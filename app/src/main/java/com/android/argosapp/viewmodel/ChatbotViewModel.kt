package com.android.argosapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.argosapp.ArgosApplication
import com.android.argosapp.R
import com.android.argosapp.data.cache.storage.UserStorage
import com.android.argosapp.data.repository.AiAnswerInputDto
import com.android.argosapp.data.repository.AiInteractionInputDto
import com.android.argosapp.data.repository.InterruptedInteractionException
import com.android.argosapp.data.repository.EncryptedPendingInteractionStore
import com.android.argosapp.data.repository.PendingInteraction
import com.android.argosapp.data.repository.PendingInteractionRunner
import com.android.argosapp.data.repository.PendingInteractionStore
import com.android.argosapp.data.repository.AiChatApiRepository
import com.android.argosapp.data.repository.FailedInteractionException
import com.android.argosapp.data.repository.AiSessionDto
import com.android.argosapp.data.repository.toEpochMillis
import com.android.argosapp.data.repository.toMessages
import com.android.argosapp.model.ChatMessage
import com.android.argosapp.model.ChatMessageKind
import com.android.argosapp.model.ChatSender
import com.android.argosapp.model.ChatSession
import com.android.argosapp.model.latestUnansweredPrompt
import com.android.argosapp.model.restoreAfterFailedAnswer
import com.android.argosapp.util.session.ChatbotOnboardingStorage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.FirebaseNetworkException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import retrofit2.HttpException
import java.io.IOException
import java.util.concurrent.TimeoutException
import java.util.UUID

enum class ChatbotPage { NEW_SESSION, CONVERSATION, HISTORY }

data class ChatDraft(val sessionId: String?, val text: String)

private class RejectedInteractionException(val pending: PendingInteraction) : Exception(
    if (pending.interrupted || pending.operationId != null) "Não foi possível consultar a interação; o resultado é incerto. Consulte o histórico antes de continuar."
    else when (pending.rejectionCode) {
        401 -> "Sua sessão do ChatTobby expirou. Entre novamente para continuar."
        403 -> "Sua conta não tem acesso a esta conversa."
        404 -> "Esta conversa não está mais disponível."
        413, 422 -> "A mensagem ou resposta é inválida ou muito longa. Revise os dados e tente novamente."
        else -> "A interação foi recusada. Revise os dados e tente novamente."
    },
)

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
    val answerDraftToRestore: ChatDraft? = null,
    val messagesCursor: String? = null,
    val hasEarlierMessages: Boolean = false,
    val canContinueAfterInterruption: Boolean = false,
)

class ChatbotViewModel internal constructor(
    application: Application,
    private val aiRepository: AiChatApiRepository,
    private val pendingStore: PendingInteractionStore,
    initialState: ChatbotUiState?,
) : AndroidViewModel(application) {
    constructor(application: Application) : this(
        application,
        (application as ArgosApplication).aiChatApiRepository,
        EncryptedPendingInteractionStore(application) { uid -> FirebaseAuth.getInstance().currentUser?.uid == uid },
        null,
    )

    private val userStorage = UserStorage(application)
    private val onboardingStorage = ChatbotOnboardingStorage(application)
    private val pendingRunner = PendingInteractionRunner(pendingStore) { pending, onAccepted ->
        aiRepository.interact(pending.sessionId, pending.input, pending.operationId, pending.pollAfterMs, onAccepted)
    }
    private val firebaseAuth = if (initialState == null) FirebaseAuth.getInstance() else null
    private val mutableState = MutableStateFlow(initialState ?: ChatbotUiState())
    private val composerDrafts = mutableMapOf<Pair<String, String?>, String>()
    val state: StateFlow<ChatbotUiState> = mutableState.asStateFlow()
    private var requestInFlight = false
    private var interactionJob: Job? = null
    private var accountGeneration = 0
    private var observedUid: String? = firebaseAuth?.currentUser?.uid ?: initialState?.userKey
    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        val uid = auth.currentUser?.uid
        if (uid != observedUid) {
            composerDrafts.clear()
            interactionJob?.cancel()
            interactionJob = null
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
                    startPendingRecovery(uid, generation)
                }
            }
        }
    }

    init {
        firebaseAuth?.addAuthStateListener(authStateListener)
        viewModelScope.launch {
            val uid = observedUid
            val generation = accountGeneration
            if (initialState == null) loadUser(uid, generation)
            if (uid != null && generation == accountGeneration) startPendingRecovery(uid, generation)
        }
    }

    override fun onCleared() {
        firebaseAuth?.removeAuthStateListener(authStateListener)
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
                startPendingRecovery(current.userKey, generation)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (generation != accountGeneration) return@launch
                if (error is HttpException && error.code() in setOf(403, 404)) {
                    val pending = pendingStore.get(current.userKey)
                    if (generation != accountGeneration) return@launch
                    if (pending?.interrupted == true && pending.sessionId == sessionId) {
                        val rejected = pending.copy(rejectionCode = error.code())
                        pendingStore.save(rejected)
                        if (generation != accountGeneration) return@launch
                        showRejectedInteraction(rejected)
                        mutableState.value = mutableState.value.copy(isLoading = false)
                        return@launch
                    }
                }
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

    fun consumeAnswerDraft() {
        mutableState.value = mutableState.value.copy(answerDraftToRestore = null)
    }

    fun rememberComposerDraft(userKey: String, sessionId: String?, text: String) {
        if (userKey.isNotBlank() && userKey == mutableState.value.userKey) {
            composerDrafts[userKey to sessionId] = text
        }
    }

    fun composerDraft(userKey: String, sessionId: String?): String =
        composerDrafts[userKey to sessionId].orEmpty()

    private fun rejectedDraft(sessionId: String?, text: String): ChatDraft {
        val key = mutableState.value.userKey to sessionId
        if (composerDrafts[key].isNullOrEmpty()) composerDrafts[key] = text
        return ChatDraft(sessionId, text)
    }

    fun sendText(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        val prompt = mutableState.value.messages.latestUnansweredPrompt()
        when (prompt?.inputType) {
            "free_text" -> {
                if (prompt.isExpired()) {
                    mutableState.value = mutableState.value.copy(errorMessage = "Esta pergunta expirou. Envie uma nova mensagem.")
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
            "single_choice" -> mutableState.value = mutableState.value.copy(
                errorMessage = "Escolha uma das opções exibidas.",
            )
            "confirmation" -> mutableState.value = mutableState.value.copy(
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
        val current = mutableState.value
        val prompt = current.messages.latestUnansweredPrompt(includeExpired = true)
            ?.takeIf { it.inputType == "single_choice" }
            ?: return
        if (current.isSending) return
        if (prompt.isExpired()) {
            mutableState.value = current.copy(errorMessage = "Esta pergunta expirou. Envie uma nova mensagem.")
            return
        }
        if (optionId.isBlank() || optionId !in prompt.optionIds) {
            mutableState.value = current.copy(errorMessage = "Não foi possível identificar essa opção. Reabra a conversa.")
            return
        }
        mutableState.value = current.copy(
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
        val current = mutableState.value
        val prompt = current.messages.latestUnansweredPrompt(includeExpired = true)
            ?.takeIf { it.inputType == "confirmation" }
            ?: return
        if (current.isSending) return
        if (prompt.isExpired()) {
            mutableState.value = current.copy(errorMessage = "Esta pergunta expirou. Envie uma nova mensagem.")
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
        if (requestInFlight || mutableState.value.isSending) return
        val generation = accountGeneration
        requestInFlight = true
        mutableState.value = mutableState.value.copy(isSending = true, errorMessage = null)
        interactionJob = viewModelScope.launch {
            var pendingMessageId: String? = null
            var pendingMessageSessionId: String? = null
            var saved = false
            try {
                var current = mutableState.value
                if (current.userKey.isBlank() || current.onboardingVisible) return@launch
                if (pendingStore.get(current.userKey)?.let { it.rejectionCode == null } == true) {
                    saved = true
                    mutableState.value = current.copy(answerDraftToRestore = ChatDraft(current.currentSessionId, text))
                    processPending(current.userKey, generation)
                    return@launch
                }
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
                pendingMessageId = userMessage.id
                pendingMessageSessionId = activeSessionId
                current = current.copy(
                    page = ChatbotPage.CONVERSATION,
                    messages = current.messages + userMessage,
                    isSending = true,
                    errorMessage = null,
                    composerHint = "Peça ao Tobby",
                )
                mutableState.value = current

                pendingStore.save(PendingInteraction(current.userKey, activeSessionId, makeInput()))
                saved = true
                processPending(current.userKey, generation)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (generation != accountGeneration) return@launch
                val current = mutableState.value
                val answerId = pendingMessageId
                val rejected = error is RejectedInteractionException && error.pending.operationId == null
                val interrupted = error is InterruptedInteractionException ||
                    (error is HttpException && error.code() == 409)
                val restoreQuestion = (!saved || rejected) && !interrupted && previousPrompt != null && answerId != null &&
                    current.currentSessionId == pendingMessageSessionId
                mutableState.value = current.copy(
                    isSending = true,
                    messages = if (restoreQuestion) {
                        current.messages.restoreAfterFailedAnswer(answerId!!, previousPrompt!!.id)
                    } else if (rejected && previousPrompt == null) {
                        current.messages.filterNot { it.sender == ChatSender.USER && it.id == pendingMessageId }
                    } else {
                        current.messages
                    },
                    composerHint = if (restoreQuestion) previousPrompt!!.composerHint() else current.composerHint,
                    answerDraftToRestore = if (((!saved || rejected) && !interrupted && previousPrompt?.inputType == "free_text") || (rejected && previousPrompt == null)) {
                        rejectedDraft(pendingMessageSessionId, text)
                    } else {
                        current.answerDraftToRestore
                    },
                    errorMessage = when {
                        interrupted -> getApplication<Application>().getString(R.string.chatbot_interruption_warning)
                        rejected && current.currentSessionId != pendingMessageSessionId ->
                            getApplication<Application>().getString(R.string.chatbot_rejected_other_conversation)
                        error is RejectedInteractionException -> error.message
                        !restoreQuestion -> error.toUserMessage()
                        previousPrompt!!.isExpired() -> "Esta pergunta expirou. Envie uma nova mensagem."
                        else -> "${error.toUserMessage()} Tente novamente com a mesma resposta ou reabra a conversa."
                    },
                )
            } finally {
                finishPendingRecovery(generation, mutableState.value.userKey)
            }
        }
    }

    private suspend fun startPendingRecovery(userKey: String, generation: Int) {
        if (requestInFlight || generation != accountGeneration) return
        requestInFlight = true
        mutableState.value = mutableState.value.copy(isSending = true)
        val pending = try {
            pendingStore.get(userKey)
        } catch (error: CancellationException) {
            requestInFlight = false
            throw error
        } catch (error: Exception) {
            mutableState.value = mutableState.value.copy(
                isSending = true,
                errorMessage = "Não foi possível recuperar a interação pendente.",
            )
            requestInFlight = false
            return
        }
        if (generation != accountGeneration) return
        if (pending == null || pending.rejectionCode != null) {
            requestInFlight = false
            if (pending == null) mutableState.value = mutableState.value.copy(isSending = false)
            else showRejectedInteraction(pending)
            return
        }
        if (pending.interrupted) {
            val conversationOpen = mutableState.value.currentSessionId == pending.sessionId
            if (conversationOpen) {
                pendingStore.clear(userKey)
                if (generation != accountGeneration) return
            }
            mutableState.value = mutableState.value.copy(
                isSending = !conversationOpen,
                canContinueAfterInterruption = !conversationOpen,
                errorMessage = InterruptedInteractionException().toUserMessage(),
            )
            requestInFlight = false
            return
        }
        interactionJob = viewModelScope.launch {
            try {
                processPending(pending.userKey, generation)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (generation == accountGeneration) {
                    if (error is RejectedInteractionException) showRejectedInteraction(error.pending)
                    else mutableState.value = mutableState.value.copy(errorMessage = error.toUserMessage())
                }
            } finally {
                finishPendingRecovery(generation, userKey)
            }
        }
    }

    private suspend fun finishPendingRecovery(generation: Int, userKey: String) {
        if (generation != accountGeneration) return
        val pending = try {
            pendingStore.get(userKey)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            if (generation == accountGeneration) {
                requestInFlight = false
                mutableState.value = mutableState.value.copy(isSending = true, errorMessage = "Não foi possível recuperar a interação pendente.")
            }
            return
        }
        if (generation == accountGeneration) {
            requestInFlight = false
            mutableState.value = mutableState.value.copy(
                isSending = pending?.let { it.rejectionCode == null } ?: false,
                canContinueAfterInterruption = pending?.interrupted == true && pending.rejectionCode == null,
            )
        }
    }

    fun continueAfterInterruption() {
        val current = mutableState.value
        if (requestInFlight || !current.canContinueAfterInterruption || current.userKey.isBlank()) return
        val generation = accountGeneration
        requestInFlight = true
        mutableState.value = current.copy(canContinueAfterInterruption = false)
        interactionJob = viewModelScope.launch {
            try {
                val pending = pendingStore.get(current.userKey)
                if (generation != accountGeneration) return@launch
                if (pending?.interrupted == true && pending.rejectionCode == null) {
                    pendingStore.save(pending.copy(rejectionCode = -1))
                }
                if (generation == accountGeneration) {
                    mutableState.value = mutableState.value.copy(errorMessage = "O resultado da interação continua incerto. Consulte o histórico se estiver disponível.")
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (generation == accountGeneration) {
                    mutableState.value = mutableState.value.copy(
                        canContinueAfterInterruption = true,
                        errorMessage = "Não foi possível continuar. Tente novamente.",
                    )
                }
            } finally {
                finishPendingRecovery(generation, current.userKey)
            }
        }
    }

    private suspend fun processPending(userKey: String, generation: Int) {
        while (generation == accountGeneration) {
            val pending = pendingStore.get(userKey) ?: return
            if (pending.interrupted) throw InterruptedInteractionException()
            if (pending.rejectionCode != null) throw RejectedInteractionException(pending)
            try {
                pendingRunner.run(pending)
                if (generation != accountGeneration) return
                pendingStore.clear(userKey)
                if (mutableState.value.currentSessionId == pending.sessionId) {
                    val page = try {
                        aiRepository.getMessages(pending.sessionId)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        mutableState.value = mutableState.value.copy(
                            errorMessage = "Interação concluída. Reabra a conversa para atualizar o histórico.",
                        )
                        return
                    }
                    if (generation != accountGeneration || mutableState.value.currentSessionId != pending.sessionId) return
                    val messages = page.toMessages(userKey)
                    mutableState.value = mutableState.value.copy(
                        messages = messages,
                        messagesCursor = page.nextCursor,
                        hasEarlierMessages = page.hasMore,
                        composerHint = messages.lastOrNull()?.composerHint() ?: "Peça ao Tobby",
                        errorMessage = null,
                    )
                }
                return
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (generation != accountGeneration) return
                if (error is InterruptedInteractionException) throw error
                val rejectionCode = when {
                    error is FailedInteractionException -> -1
                    error is HttpException && error.code() in 400..499 && error.code() !in setOf(408, 429) -> error.code()
                    else -> null
                }
                if (rejectionCode != null) {
                    val rejected = (pendingStore.get(userKey) ?: pending).copy(rejectionCode = rejectionCode)
                    pendingStore.save(rejected)
                    throw RejectedInteractionException(rejected)
                }
                if (error !is IOException && error !is TimeoutException && error !is FirebaseNetworkException &&
                    !(error is HttpException && (error.code() in setOf(408, 429) || error.code() in 500..599))) throw error
                mutableState.value = mutableState.value.copy(
                    errorMessage = "Aguardando conexão para recuperar a interação.",
                )
                delay(10_000)
            }
        }
    }

    private fun showRejectedInteraction(pending: PendingInteraction) {
        val text = if (pending.operationId == null && !pending.interrupted) pending.input.message ?: pending.input.answer?.text else null
        mutableState.value = mutableState.value.copy(
            isSending = requestInFlight,
            canContinueAfterInterruption = false,
            answerDraftToRestore = text?.let { rejectedDraft(pending.sessionId, it) },
            errorMessage = RejectedInteractionException(pending).message,
        )
    }

    private fun AiSessionDto.toModel(userKey: String): ChatSession {
        val timestamp = toEpochMillis()
        return ChatSession(sessionId, userKey, timestamp, timestamp)
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

    private fun Exception.toUserMessage(): String = if (this is InterruptedInteractionException) {
        getApplication<Application>().getString(R.string.chatbot_interruption_warning)
    } else when ((this as? HttpException)?.code()) {
        401 -> "Sua sessão do ChatTobby expirou. Entre novamente para continuar."
        403 -> "Sua conta não tem fazendas autorizadas para usar o ChatTobby."
        404 -> "Esta conversa não está mais disponível."
        503 -> "O serviço do ChatTobby está temporariamente indisponível. Tente novamente."
        else -> "Não foi possível concluir a operação do ChatTobby. Verifique sua conexão e tente novamente."
    }
}
