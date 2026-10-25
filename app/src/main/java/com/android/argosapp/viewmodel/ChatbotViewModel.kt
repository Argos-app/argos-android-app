package com.android.argosapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.argosapp.ArgosApplication
import com.android.argosapp.data.cache.storage.UserStorage
import com.android.argosapp.data.repository.AiAnswerInputDto
import com.android.argosapp.data.repository.AiInteractionInputDto
import com.android.argosapp.data.repository.AiInteractionResultDto
import com.android.argosapp.data.repository.AiSessionDto
import com.android.argosapp.data.repository.toEpochMillis
import com.android.argosapp.data.repository.toMessages
import com.android.argosapp.model.ChatMessage
import com.android.argosapp.model.ChatMessageKind
import com.android.argosapp.model.ChatResponseState
import com.android.argosapp.model.ChatSender
import com.android.argosapp.model.ChatSession
import com.android.argosapp.util.session.ChatbotOnboardingStorage
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.time.Instant
import java.util.UUID

enum class ChatbotPage { NEW_SESSION, CONVERSATION, HISTORY }

data class ChatbotUiState(
    val isLoading: Boolean = true,
    val onboardingVisible: Boolean = false,
    val page: ChatbotPage = ChatbotPage.NEW_SESSION,
    val userName: String = "",
    val userKey: String = "",
    val onboardingKey: String = "",
    val currentSessionId: String? = null,
    val sessions: List<ChatSession> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val composerHint: String = "Peça ao Tobby",
    val isSending: Boolean = false,
    val errorMessage: String? = null,
    val messagesCursor: String? = null,
    val hasEarlierMessages: Boolean = false,
)

class ChatbotViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ArgosApplication
    private val userStorage = UserStorage(application)
    private val onboardingStorage = ChatbotOnboardingStorage(application)
    private val aiRepository = app.aiChatApiRepository
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val mutableState = MutableStateFlow(ChatbotUiState())
    val state: StateFlow<ChatbotUiState> = mutableState.asStateFlow()
    private var requestInFlight = false
    private var accountGeneration = 0
    private var observedUid: String? = firebaseAuth.currentUser?.uid
    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        val uid = auth.currentUser?.uid
        if (uid != observedUid) {
            observedUid = uid
            accountGeneration += 1
            val generation = accountGeneration
            requestInFlight = false
            mutableState.value = ChatbotUiState(isLoading = uid != null, userKey = uid.orEmpty())
            viewModelScope.launch {
                aiRepository.clearCachedToken()
                if (uid == null) {
                    mutableState.value = ChatbotUiState(
                        isLoading = false,
                        errorMessage = "Entre na sua conta para usar o ChatTobby.",
                    )
                } else {
                    loadUser(uid, generation)
                }
            }
        }
    }

    init {
        firebaseAuth.addAuthStateListener(authStateListener)
        viewModelScope.launch { loadUser(observedUid) }
    }

    override fun onCleared() {
        firebaseAuth.removeAuthStateListener(authStateListener)
        super.onCleared()
    }

    private suspend fun loadUser(uid: String?, generation: Int = accountGeneration) {
        if (uid.isNullOrBlank()) {
            mutableState.value = ChatbotUiState(
                isLoading = false,
                errorMessage = "Entre na sua conta para usar o ChatTobby.",
            )
            return
        }
        try {
            val user = userStorage.getUser()
            val onboardingKey = user?.userEmail?.trim()?.takeIf(String::isNotBlank) ?: uid
            val sessions = aiRepository.listSessions().map { it.toModel(uid) }
            if (generation != accountGeneration) return
            mutableState.value = ChatbotUiState(
                isLoading = false,
                onboardingVisible = !onboardingStorage.isCompleted(onboardingKey),
                userName = user?.userName.orEmpty(),
                userKey = uid,
                onboardingKey = onboardingKey,
                sessions = sessions,
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            if (generation != accountGeneration) return
            mutableState.value = ChatbotUiState(
                isLoading = false,
                userKey = uid,
                errorMessage = error.toUserMessage(),
            )
        }
    }

    fun completeOnboarding() {
        val current = mutableState.value
        if (current.userKey.isBlank()) return
        onboardingStorage.markCompleted(current.onboardingKey.ifBlank { current.userKey })
        mutableState.value = current.copy(onboardingVisible = false, page = ChatbotPage.NEW_SESSION)
    }

    fun showHistory() {
        viewModelScope.launch {
            val current = mutableState.value
            if (current.userKey.isBlank()) return@launch
            val generation = accountGeneration
            mutableState.value = current.copy(
                isLoading = true,
                page = ChatbotPage.HISTORY,
                currentSessionId = null,
                messages = emptyList(),
                messagesCursor = null,
                hasEarlierMessages = false,
                errorMessage = null,
            )
            try {
                val sessions = aiRepository.listSessions().map { it.toModel(current.userKey) }
                if (generation != accountGeneration) return@launch
                mutableState.value = mutableState.value.copy(isLoading = false, sessions = sessions)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (generation != accountGeneration) return@launch
                mutableState.value = mutableState.value.copy(isLoading = false, errorMessage = error.toUserMessage())
            }
        }
    }

    fun startNewConversation() {
        mutableState.value = mutableState.value.copy(
            page = ChatbotPage.NEW_SESSION,
            currentSessionId = null,
            messages = emptyList(),
            composerHint = "Peça ao Tobby",
            messagesCursor = null,
            hasEarlierMessages = false,
            errorMessage = null,
        )
    }

    fun openSession(sessionId: String) {
        viewModelScope.launch {
            val current = mutableState.value
            if (current.userKey.isBlank()) return@launch
            val generation = accountGeneration
            mutableState.value = current.copy(isLoading = true, errorMessage = null)
            try {
                val page = aiRepository.getMessages(sessionId)
                if (generation != accountGeneration) return@launch
                val messages = page.toMessages(current.userKey)
                mutableState.value = mutableState.value.copy(
                    isLoading = false,
                    page = ChatbotPage.CONVERSATION,
                    currentSessionId = sessionId,
                    messages = messages,
                    composerHint = messages.lastOrNull()?.composerHint() ?: "Peça ao Tobby",
                    messagesCursor = page.nextCursor,
                    hasEarlierMessages = page.hasMore,
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (generation != accountGeneration) return@launch
                mutableState.value = mutableState.value.copy(isLoading = false, errorMessage = error.toUserMessage())
            }
        }
    }

    fun loadEarlierMessages() {
        val current = mutableState.value
        val sessionId = current.currentSessionId ?: return
        val cursor = current.messagesCursor ?: return
        if (!current.hasEarlierMessages || current.isLoading) return
        viewModelScope.launch {
            val generation = accountGeneration
            mutableState.value = current.copy(isLoading = true, errorMessage = null)
            try {
                val page = aiRepository.getMessages(sessionId, cursor)
                if (generation != accountGeneration) return@launch
                mutableState.value = mutableState.value.copy(
                    isLoading = false,
                    messages = page.toMessages(current.userKey) + mutableState.value.messages,
                    messagesCursor = page.nextCursor,
                    hasEarlierMessages = page.hasMore,
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (generation != accountGeneration) return@launch
                mutableState.value = mutableState.value.copy(isLoading = false, errorMessage = error.toUserMessage())
            }
        }
    }

    fun sendText(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        val current = mutableState.value
        val prompt = current.messages.lastOrNull()?.takeIf { it.sender == ChatSender.ASSISTANT }
        when (prompt?.inputType) {
            "single_choice" -> mutableState.value = current.copy(errorMessage = "Escolha uma das opções exibidas.")
            "confirmation" -> mutableState.value = current.copy(errorMessage = "Use os botões para confirmar ou cancelar.")
            "free_text" -> {
                if (prompt.isExpired()) {
                    mutableState.value = current.copy(errorMessage = "Esta pergunta expirou. Envie uma nova mensagem.")
                } else {
                    send(trimmed, prompt) {
                        AiInteractionInputDto(
                            kind = "answer",
                            interactionId = prompt.requireInteractionId(),
                            questionId = prompt.requireQuestionId(),
                            answer = AiAnswerInputDto(kind = "text", text = trimmed),
                        )
                    }
                }
            }
            else -> send(trimmed, null) {
                AiInteractionInputDto(
                    kind = "message",
                    clientMessageId = UUID.randomUUID().toString(),
                    message = trimmed,
                )
            }
        }
    }

    fun selectOption(option: String) {
        val current = mutableState.value
        val prompt = current.messages.lastOrNull()?.takeIf {
            it.sender == ChatSender.ASSISTANT && it.inputType == "single_choice"
        } ?: return
        if (current.isSending) return
        if (prompt.isExpired()) {
            mutableState.value = current.copy(errorMessage = "Esta pergunta expirou. Envie uma nova mensagem.")
            return
        }
        val index = prompt.options.indexOf(option)
        val optionId = prompt.optionIds.getOrNull(index)
        if (optionId.isNullOrBlank()) {
            mutableState.value = current.copy(errorMessage = "Não foi possível identificar essa opção. Reabra a conversa.")
            return
        }
        send(option, prompt) {
            AiInteractionInputDto(
                kind = "answer",
                interactionId = prompt.requireInteractionId(),
                questionId = prompt.requireQuestionId(),
                answer = AiAnswerInputDto(kind = "choice", optionId = optionId),
            )
        }
    }

    fun confirmSend(confirmed: Boolean) {
        val current = mutableState.value
        val prompt = current.messages.lastOrNull()?.takeIf {
            it.sender == ChatSender.ASSISTANT && it.inputType == "confirmation"
        } ?: return
        if (current.isSending) return
        if (prompt.isExpired()) {
            mutableState.value = current.copy(errorMessage = "Esta pergunta expirou. Envie uma nova mensagem.")
            return
        }
        send(if (confirmed) "Confirmar envio" else "Não enviar", prompt) {
            AiInteractionInputDto(
                kind = "answer",
                interactionId = prompt.requireInteractionId(),
                questionId = prompt.requireQuestionId(),
                answer = AiAnswerInputDto(kind = "confirmation", confirm = confirmed),
            )
        }
    }

    private fun send(
        text: String,
        previousPrompt: ChatMessage?,
        makeInput: () -> AiInteractionInputDto,
    ) {
        if (requestInFlight || mutableState.value.isSending) return
        val generation = accountGeneration
        requestInFlight = true
        mutableState.value = mutableState.value.copy(isSending = true, errorMessage = null)
        viewModelScope.launch {
            try {
                var current = mutableState.value
                if (current.userKey.isBlank() || current.onboardingVisible) return@launch
                var sessionId = current.currentSessionId
                if (sessionId == null) {
                    val session = aiRepository.createSession().toModel(current.userKey)
                    sessionId = session.id
                    current = current.copy(
                        page = ChatbotPage.CONVERSATION,
                        currentSessionId = session.id,
                        sessions = listOf(session) + current.sessions,
                    )
                    mutableState.value = current
                }
                val activeSessionId = requireNotNull(sessionId)
                if (previousPrompt?.isExpired() == true) {
                    mutableState.value = current.copy(isSending = false, errorMessage = "Esta pergunta expirou. Envie uma nova mensagem.")
                    return@launch
                }

                val now = System.currentTimeMillis()
                val userMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    sessionId = activeSessionId,
                    userKey = current.userKey,
                    position = (current.messages.lastOrNull()?.position ?: 0) + 1,
                    sender = ChatSender.USER,
                    text = text,
                    createdAt = now,
                    apiInteractionId = previousPrompt?.apiInteractionId,
                )
                current = current.copy(
                    page = ChatbotPage.CONVERSATION,
                    messages = current.messages + userMessage,
                    isSending = true,
                    errorMessage = null,
                    composerHint = "Peça ao Tobby",
                )
                mutableState.value = current

                val response = aiRepository.interact(activeSessionId, makeInput())
                if (generation != accountGeneration) return@launch
                val assistantMessage = response.toAssistantMessage(current.userKey)
                current = current.copy(
                    messages = current.messages + assistantMessage,
                    isSending = false,
                    composerHint = assistantMessage.composerHint(),
                    sessions = listOf(
                        current.sessions.firstOrNull { it.id == activeSessionId }
                            ?.copy(updatedAt = assistantMessage.createdAt)
                            ?: ChatSession(activeSessionId, current.userKey, assistantMessage.createdAt, assistantMessage.createdAt),
                    ) + current.sessions.filterNot { it.id == activeSessionId },
                )
                mutableState.value = current
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (generation != accountGeneration) return@launch
                mutableState.value = mutableState.value.copy(
                    isSending = false,
                    errorMessage = error.toUserMessage(),
                )
            } finally {
                if (generation == accountGeneration) {
                    requestInFlight = false
                    mutableState.value = mutableState.value.copy(isSending = false)
                }
            }
        }
    }

    private fun AiSessionDto.toModel(userKey: String): ChatSession {
        val timestamp = toEpochMillis()
        return ChatSession(sessionId, userKey, timestamp, timestamp)
    }

    private fun AiInteractionResultDto.toAssistantMessage(userKey: String): ChatMessage {
        val content = when (response.kind) {
            "answer" -> response.text.orEmpty()
            "question" -> response.prompt.orEmpty()
            else -> "Não foi possível interpretar a resposta da IA."
        }
        val inputType = response.inputType
        val kind = when (inputType) {
            "free_text" -> ChatMessageKind.FREE_TEXT_PROMPT
            "single_choice" -> ChatMessageKind.SINGLE_CHOICE
            "confirmation" -> ChatMessageKind.CONFIRMATION
            else -> ChatMessageKind.TEXT
        }
        return ChatMessage(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            userKey = userKey,
                    position = (mutableState.value.messages.lastOrNull()?.position ?: 0) + 1,
            sender = ChatSender.ASSISTANT,
            text = content,
            kind = kind,
            options = response.options.orEmpty().map { it.label },
            responseState = if (status == "degraded") ChatResponseState.PARTIAL else ChatResponseState.COMPLETED,
            createdAt = createdAt?.let { Instant.parse(it).toEpochMilli() } ?: System.currentTimeMillis(),
            apiInteractionId = interactionId,
            questionId = response.questionId,
            inputType = inputType,
            optionIds = response.options.orEmpty().map { it.optionId },
            degradationReasons = degradationReasons.orEmpty(),
            questionExpiresAt = response.expiresAt?.let { Instant.parse(it).toEpochMilli() },
        )
    }

    private fun ChatMessage.composerHint(): String =
        if (kind == ChatMessageKind.FREE_TEXT_PROMPT && !isExpired()) "Digite sua resposta..." else "Peça ao Tobby"

    private fun ChatMessage.isExpired(): Boolean = questionExpiresAt?.let { it <= System.currentTimeMillis() } == true

    private fun ChatMessage.requireInteractionId(): String =
        requireNotNull(apiInteractionId?.takeIf(String::isNotBlank)) {
            "Não foi possível identificar a pergunta. Reabra a conversa."
        }

    private fun ChatMessage.requireQuestionId(): String =
        requireNotNull(questionId?.takeIf(String::isNotBlank)) {
            "Não foi possível identificar a pergunta. Reabra a conversa."
        }

    private fun Exception.toUserMessage(): String = when ((this as? HttpException)?.code()) {
        401 -> "Sua sessão do ChatTobby expirou. Entre novamente para continuar."
        403 -> "Sua conta não tem fazendas autorizadas para usar o ChatTobby."
        404 -> "Esta conversa não está mais disponível."
        503 -> "O serviço do ChatTobby está temporariamente indisponível. Tente novamente."
        else -> "Não foi possível concluir a operação do ChatTobby. Verifique sua conexão e tente novamente."
    }
}
