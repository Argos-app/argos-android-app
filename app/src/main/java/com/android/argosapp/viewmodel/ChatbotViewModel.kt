package com.android.argosapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.argosapp.ArgosApplication
import com.android.argosapp.data.cache.storage.UserStorage
import com.android.argosapp.data.cache.storage.ChatCacheStorage
import com.android.argosapp.util.logging.AppLogger
import com.android.argosapp.data.dto.AiAnswerInputDto
import com.android.argosapp.data.dto.AiInteractionInputDto
import com.android.argosapp.data.dto.AiInteractionResultDto
import com.android.argosapp.data.dto.AiSessionDto
import com.android.argosapp.data.repository.toEpochMillis
import com.android.argosapp.data.repository.toMessages
import com.android.argosapp.model.ChatMessage
import com.android.argosapp.model.ChatMessageKind
import com.android.argosapp.model.ChatResponseState
import com.android.argosapp.model.ChatSender
import com.android.argosapp.model.ChatSession
import com.android.argosapp.model.chatSessionTitle
import com.android.argosapp.model.latestUnansweredPrompt
import com.android.argosapp.model.restoreAfterFailedAnswer
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.time.Instant
import java.util.UUID

enum class ChatbotPage { NEW_SESSION, CONVERSATION, HISTORY }

data class ChatbotUiState(
    val isLoading: Boolean = true,
    val page: ChatbotPage = ChatbotPage.NEW_SESSION,
    val userName: String = "",
    val userKey: String = "",
    val currentSessionId: String? = null,
    val pendingSessionTitle: String? = null,
    val sessions: List<ChatSession> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val composerHint: String = "Peça ao Tobby",
    val isSending: Boolean = false,
    val errorMessage: String? = null,
    val answerDraftToRestore: String? = null,
    val messagesCursor: String? = null,
    val hasEarlierMessages: Boolean = false,
)

class ChatbotViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ArgosApplication
    private val userStorage = UserStorage(application)
    private val chatCache = ChatCacheStorage(application)
    private val aiRepository = app.aiChatApiRepository
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val mutableState = MutableSharedFlow<ChatbotUiState>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val state = mutableState.asSharedFlow()
    private var currentState = ChatbotUiState()
        set(value) {
            field = value
            mutableState.tryEmit(value)
        }
    var draft: String = ""

    private var historyJob: Job? = null
    private var requestInFlight = false
    private var accountGeneration = 0
    private var conversationGeneration = 0
    private var observedUid: String? = firebaseAuth.currentUser?.uid
    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        val uid = auth.currentUser?.uid
        if (uid != observedUid) {
            observedUid = uid
            historyJob?.cancel()
            accountGeneration += 1
            draft = ""
            val generation = accountGeneration
            requestInFlight = false
            currentState = ChatbotUiState(isLoading = uid != null, userKey = uid.orEmpty())
            viewModelScope.launch {
                aiRepository.clearCachedToken()
                if (uid == null) {
                    currentState = ChatbotUiState(
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
        mutableState.tryEmit(currentState)
        firebaseAuth.addAuthStateListener(authStateListener)
        viewModelScope.launch { loadUser(observedUid) }
    }

    override fun onCleared() {
        firebaseAuth.removeAuthStateListener(authStateListener)
        super.onCleared()
    }

    private suspend fun loadUser(uid: String?, generation: Int = accountGeneration) {
        if (uid.isNullOrBlank()) {
            currentState = ChatbotUiState(
                isLoading = false,
                errorMessage = "Entre na sua conta para usar o ChatTobby.",
            )
            return
        }
        try {
            val user = userStorage.getUser()
            if (generation != accountGeneration) return
            currentState = ChatbotUiState(
                isLoading = false,
                userName = user?.userName.orEmpty(),
                userKey = uid,
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            if (generation != accountGeneration) return
            currentState = ChatbotUiState(
                isLoading = false,
                userKey = uid,
                errorMessage = error.toUserMessage(),
            )
        }
    }

    fun retryOrDismissError() {
        if (currentState.page == ChatbotPage.HISTORY && currentState.userKey.isNotBlank()) {
            showHistory()
        } else if (currentState.userKey.isBlank()) {
            currentState = currentState.copy(isLoading = true, errorMessage = null)
            viewModelScope.launch { loadUser(firebaseAuth.currentUser?.uid) }
        } else {
            currentState = currentState.copy(errorMessage = null)
        }
    }

    fun showHistory() {
        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            val current = currentState
            if (current.userKey.isBlank()) return@launch
            val generation = accountGeneration
            currentState = current.copy(
                isLoading = true,
                page = ChatbotPage.HISTORY,
                currentSessionId = null,
                messages = emptyList(),
                messagesCursor = null,
                hasEarlierMessages = false,
                errorMessage = null,
            )
            try {
                val cached = readCache { chatCache.sessions(current.userKey) }.orEmpty()
                coroutineContext.ensureActive()
                if (generation != accountGeneration) return@launch
                currentState = currentState.copy(sessions = cached)
                val titles = cached.associate { it.id to it.title }
                val sessions = aiRepository.listSessions().map { it.toModel(current.userKey).copy(title = titles[it.sessionId]) }
                coroutineContext.ensureActive()
                if (generation != accountGeneration) return@launch
                currentState = currentState.copy(isLoading = false, sessions = sessions)
                writeCache { chatCache.replaceSessions(current.userKey, sessions) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                coroutineContext.ensureActive()
                if (generation != accountGeneration) return@launch
                currentState = currentState.copy(isLoading = false, errorMessage = error.toUserMessage())
            }
        }
    }

    fun startNewConversation() {
        conversationGeneration += 1
        historyJob?.cancel()
        currentState = currentState.copy(
            page = ChatbotPage.NEW_SESSION,
            pendingSessionTitle = null,
            isLoading = false,
            currentSessionId = null,
            messages = emptyList(),
            composerHint = "Peça ao Tobby",
            messagesCursor = null,
            hasEarlierMessages = false,
            errorMessage = null,
        )
    }

    fun openSession(sessionId: String) {
        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            val current = currentState
            if (current.userKey.isBlank()) return@launch
            val generation = accountGeneration
            currentState = current.copy(isLoading = true, errorMessage = null)
            try {
                val cachedPage = readCache { chatCache.page(current.userKey, sessionId) }
                val cachedMessages = if (cachedPage != null) readCache { chatCache.messages(current.userKey, sessionId) } else null
                coroutineContext.ensureActive()
                if (generation != accountGeneration) return@launch
                if (cachedPage != null && cachedMessages != null) {
                    currentState = currentState.copy(
                        page = ChatbotPage.CONVERSATION, currentSessionId = sessionId,
                        messages = cachedMessages, messagesCursor = cachedPage.nextCursor,
                        hasEarlierMessages = cachedPage.hasMore,
                        composerHint = cachedMessages.lastOrNull()?.composerHint() ?: "Peça ao Tobby",
                    )
                }
                val page = aiRepository.getMessages(sessionId)
                coroutineContext.ensureActive()
                if (generation != accountGeneration) return@launch
                val messages = page.toMessages(current.userKey)
                currentState = currentState.copy(
                    isLoading = false,
                    page = ChatbotPage.CONVERSATION,
                    currentSessionId = sessionId,
                    messages = messages,
                    composerHint = messages.lastOrNull()?.composerHint() ?: "Peça ao Tobby",
                    messagesCursor = page.nextCursor,
                    hasEarlierMessages = page.hasMore,
                )
                writeCache { chatCache.savePage(current.userKey, sessionId, messages, page.nextCursor, page.hasMore, replace = true) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                coroutineContext.ensureActive()
                if (generation != accountGeneration) return@launch
                currentState = currentState.copy(isLoading = false, errorMessage = error.toUserMessage())
            }
        }
    }

    fun loadEarlierMessages() {
        val current = currentState
        val sessionId = current.currentSessionId ?: return
        val cursor = current.messagesCursor ?: return
        if (!current.hasEarlierMessages || current.isLoading) return
        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            val generation = accountGeneration
            currentState = current.copy(isLoading = true, errorMessage = null)
            try {
                val page = aiRepository.getMessages(sessionId, cursor)
                coroutineContext.ensureActive()
                if (generation != accountGeneration) return@launch
                val messages = page.toMessages(current.userKey)
                currentState = currentState.copy(
                    isLoading = false,
                    messages = messages + currentState.messages,
                    messagesCursor = page.nextCursor,
                    hasEarlierMessages = page.hasMore,
                )
                writeCache { chatCache.savePage(current.userKey, sessionId, messages, page.nextCursor, page.hasMore, replace = false) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                coroutineContext.ensureActive()
                if (generation != accountGeneration) return@launch
                currentState = currentState.copy(isLoading = false, errorMessage = error.toUserMessage())
            }
        }
    }

    fun consumeAnswerDraft() {
        currentState = currentState.copy(answerDraftToRestore = null)
    }

    fun sendText(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        val prompt = currentState.messages.latestUnansweredPrompt()
        when (prompt?.inputType) {
            "free_text" -> {
                if (prompt.isExpired()) {
                    currentState = currentState.copy(errorMessage = "Esta pergunta expirou. Envie uma nova mensagem.")
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
            "single_choice" -> currentState = currentState.copy(
                errorMessage = "Escolha uma das opções exibidas.",
            )
            "confirmation" -> currentState = currentState.copy(
                errorMessage = "Use os botões para confirmar ou cancelar.",
            )
            else -> send(trimmed, null) {
                AiInteractionInputDto(
                    kind = "message",
                    clientMessageId = UUID.randomUUID().toString(),
                    message = trimmed,
                )
            }
        }
    }

    fun selectOption(option: String, optionId: String) {
        val current = currentState
        val prompt = current.messages.latestUnansweredPrompt(includeExpired = true)
            ?.takeIf { it.inputType == "single_choice" }
            ?: return
        if (current.isSending) return
        if (prompt.isExpired()) {
            currentState = current.copy(errorMessage = "Esta pergunta expirou. Envie uma nova mensagem.")
            return
        }
        if (optionId.isBlank() || optionId !in prompt.optionIds) {
            currentState = current.copy(errorMessage = "Não foi possível identificar essa opção. Reabra a conversa.")
            return
        }
        currentState = current.copy(
            messages = current.messages.map { message ->
                if (message.id == prompt.id) message.copy(selectedOptionId = optionId) else message
            },
        )
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
        val current = currentState
        val prompt = current.messages.latestUnansweredPrompt(includeExpired = true)
            ?.takeIf { it.inputType == "confirmation" }
            ?: return
        if (current.isSending) return
        if (prompt.isExpired()) {
            currentState = current.copy(errorMessage = "Esta pergunta expirou. Envie uma nova mensagem.")
            return
        }
        send(if (confirmed) "Confirmar" else "Cancelar", prompt) {
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
        if (requestInFlight || currentState.isSending) return
        val generation = accountGeneration
        val screenGeneration = conversationGeneration
        requestInFlight = true
        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            sessionId = currentState.currentSessionId.orEmpty(),
            userKey = currentState.userKey,
            position = (currentState.messages.lastOrNull()?.position ?: 0) + 1,
            sender = ChatSender.USER,
            text = text,
            createdAt = System.currentTimeMillis(),
            apiInteractionId = previousPrompt?.apiInteractionId,
        )
        val sendState = currentState.copy(
            page = ChatbotPage.CONVERSATION,
            messages = currentState.messages + userMessage,
            composerHint = "Peça ao Tobby",
            pendingSessionTitle = if (currentState.currentSessionId == null) chatSessionTitle(text) else null,
            isSending = true,
            errorMessage = null,
        )
        currentState = sendState
        viewModelScope.launch {
            var pendingAnswerMessageId: String? = null
            var pendingAnswerSessionId: String? = null
            try {
                var current = sendState
                if (current.userKey.isBlank()) return@launch
                var sessionId = current.currentSessionId
                if (sessionId == null) {
                    val session = aiRepository.createSession().toModel(current.userKey).copy(title = chatSessionTitle(text))
                    if (generation != accountGeneration) return@launch
                    sessionId = session.id
                    current = current.copy(
                        page = ChatbotPage.CONVERSATION,
                        currentSessionId = session.id,
                        pendingSessionTitle = null,
                        messages = current.messages.map { if (it.id == userMessage.id) it.copy(sessionId = session.id) else it },
                        sessions = listOf(session) + current.sessions,
                    )
                    if (screenGeneration == conversationGeneration) currentState = current
                    writeCache { chatCache.saveSession(session) }
                }
                val activeSessionId = requireNotNull(sessionId)
                if (previousPrompt?.isExpired() == true) {
                    if (screenGeneration == conversationGeneration) {
                        currentState = current.copy(
                            isSending = false,
                            messages = current.messages.restoreAfterFailedAnswer(userMessage.id, previousPrompt.id),
                            answerDraftToRestore = text.takeIf { previousPrompt.inputType == "free_text" },
                            errorMessage = "Esta pergunta expirou. Envie uma nova mensagem.",
                        )
                    }
                    return@launch
                }

                if (previousPrompt != null) {
                    pendingAnswerMessageId = userMessage.id
                    pendingAnswerSessionId = activeSessionId
                }

                val response = aiRepository.interact(activeSessionId, makeInput())
                if (generation != accountGeneration) return@launch
                val assistantMessage = response.toAssistantMessage(current.userKey, (current.messages.lastOrNull()?.position ?: 0) + 1)
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
                if (screenGeneration == conversationGeneration) currentState = current
                writeCache {
                    chatCache.saveSession(current.sessions.first())
                    chatCache.savePage(current.userKey, activeSessionId, current.messages, current.messagesCursor, current.hasEarlierMessages, replace = true)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (generation != accountGeneration) return@launch
                if (screenGeneration != conversationGeneration) {
                    currentState = currentState.copy(errorMessage = error.toUserMessage())
                    return@launch
                }
                val current = currentState
                val answerId = pendingAnswerMessageId
                val restoreQuestion = previousPrompt != null && answerId != null &&
                    current.currentSessionId == pendingAnswerSessionId
                currentState = current.copy(
                    isSending = false,
                    messages = when {
                        restoreQuestion -> current.messages.restoreAfterFailedAnswer(answerId!!, previousPrompt!!.id)
                        current.currentSessionId == null -> current.messages.filterNot { it.id == userMessage.id }
                        else -> current.messages
                    },
                    composerHint = if (restoreQuestion) previousPrompt!!.composerHint() else current.composerHint,
                    answerDraftToRestore = if (current.currentSessionId == null || (restoreQuestion && previousPrompt!!.inputType == "free_text")) {
                        text
                    } else {
                        current.answerDraftToRestore
                    },
                    errorMessage = when {
                        !restoreQuestion -> error.toUserMessage()
                        previousPrompt!!.isExpired() -> "Esta pergunta expirou. Envie uma nova mensagem."
                        else -> "${error.toUserMessage()} Tente novamente com a mesma resposta ou reabra a conversa."
                    },
                )
            } finally {
                if (generation == accountGeneration) {
                    requestInFlight = false
                    currentState = currentState.copy(isSending = false)
                }
            }
        }
    }

    private suspend fun <T> readCache(action: suspend () -> T): T? = try {
        action()
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        AppLogger.error(AppLogger.REPOSITORY_TAG, "chat_cache_read_failed", error)
        null
    }

    private suspend fun writeCache(action: suspend () -> Unit) {
        try {
            action()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            AppLogger.error(AppLogger.REPOSITORY_TAG, "chat_cache_write_failed", error)
        }
    }

    private fun AiSessionDto.toModel(userKey: String): ChatSession {
        val timestamp = toEpochMillis()
        return ChatSession(sessionId, userKey, timestamp, timestamp)
    }

    private fun AiInteractionResultDto.toAssistantMessage(userKey: String, position: Int): ChatMessage {
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
            position = position,
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
