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
import com.android.argosapp.data.repository.AiChatApiRepository
import com.android.argosapp.data.repository.EncryptedPendingInteractionStore
import com.android.argosapp.data.repository.FailedInteractionException
import com.android.argosapp.data.repository.InterruptedInteractionException
import com.android.argosapp.data.repository.PendingInteraction
import com.android.argosapp.data.repository.PendingInteractionRunner
import com.android.argosapp.data.repository.PendingInteractionStore
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import retrofit2.HttpException
import com.google.firebase.FirebaseNetworkException
import java.io.IOException
import java.time.Instant
import java.util.concurrent.TimeoutException
import java.util.UUID

enum class ChatbotPage { NEW_SESSION, CONVERSATION, HISTORY }

data class ChatDraft(val sessionId: String?, val text: String)

private class RejectedInteractionException(val pending: PendingInteraction) : IllegalStateException(
    if (pending.interrupted || pending.operationId != null) "O resultado da interação é incerto. Consulte o histórico antes de continuar."
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
    val answerDraftToRestore: ChatDraft? = null,
    val messagesCursor: String? = null,
    val hasEarlierMessages: Boolean = false,
    val canContinueAfterInterruption: Boolean = false,
    val isRecoveryBlocked: Boolean = false,
    val canRetryPendingRecovery: Boolean = false,
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
    private val chatCache = ChatCacheStorage(application)
    private val pendingRunner = PendingInteractionRunner(pendingStore) { pending, onAccepted ->
        aiRepository.interact(pending.sessionId, pending.input, pending.operationId, pending.pollAfterMs, onAccepted)
    }
    private val firebaseAuth = if (initialState == null) FirebaseAuth.getInstance() else null
    private val mutableState = MutableStateFlow(initialState ?: ChatbotUiState())
    val state: StateFlow<ChatbotUiState> = mutableState.asStateFlow()
    private var currentState = initialState ?: ChatbotUiState()
        set(value) {
            field = value
            mutableState.value = value
        }
    var draft: String = ""
    private val composerDrafts = mutableMapOf<Pair<String, String?>, String>()
    private var interactionJob: Job? = null

    private var historyJob: Job? = null
    private var requestInFlight = false
    private var accountGeneration = 0
    private var conversationGeneration = 0
    private var observedUid: String? = firebaseAuth?.currentUser?.uid ?: initialState?.userKey
    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        val uid = auth.currentUser?.uid
        if (uid != observedUid) {
            observedUid = uid
            historyJob?.cancel()
            accountGeneration += 1
            draft = ""
            val generation = accountGeneration
            requestInFlight = false
            interactionJob?.cancel()
            interactionJob = null
            composerDrafts.clear()
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
        if (currentState.canRetryPendingRecovery) {
            retryPendingRecovery()
        } else if (currentState.canContinueAfterInterruption) {
            continueAfterInterruption()
        } else if (currentState.page == ChatbotPage.HISTORY && currentState.userKey.isNotBlank()) {
            showHistory()
        } else if (currentState.userKey.isBlank()) {
            currentState = currentState.copy(isLoading = true, errorMessage = null)
            viewModelScope.launch { loadUser(firebaseAuth?.currentUser?.uid) }
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

    fun rememberComposerDraft(userKey: String, sessionId: String?, text: String) {
        if (userKey.isNotBlank()) composerDrafts[userKey to sessionId] = text
    }

    fun composerDraft(userKey: String, sessionId: String?): String = composerDrafts[userKey to sessionId].orEmpty()

    private fun rejectedDraft(sessionId: String?, text: String) = ChatDraft(sessionId, text)

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
        if (current.isSending || current.isRecoveryBlocked) return
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
        if (current.isSending || current.isRecoveryBlocked) return
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
        if (requestInFlight || currentState.isSending || currentState.isRecoveryBlocked) return
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
                            answerDraftToRestore = text.takeIf { previousPrompt.inputType == "free_text" }?.let { rejectedDraft(sessionId, it) },
                            errorMessage = "Esta pergunta expirou. Envie uma nova mensagem.",
                        )
                    }
                    return@launch
                }

                if (previousPrompt != null) {
                    pendingAnswerMessageId = userMessage.id
                    pendingAnswerSessionId = activeSessionId
                }

                val existingPending = pendingStore.get(current.userKey)
                if (existingPending != null && existingPending.rejectionCode == null) {
                    currentState = currentState.copy(
                        messages = currentState.messages.filterNot { it.id == userMessage.id },
                        answerDraftToRestore = rejectedDraft(activeSessionId, text),
                    )
                    processPending(current.userKey, generation)
                    refreshSessionAfterRecovery(activeSessionId, current.userKey, generation)
                    return@launch
                }
                if (existingPending != null) pendingStore.clear(current.userKey)
                val pending = PendingInteraction(current.userKey, activeSessionId, makeInput())
                pendingStore.save(pending)
                val response = processPending(current.userKey, generation)
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
                        rejectedDraft(pendingAnswerSessionId ?: current.currentSessionId, text)
                    } else {
                        current.answerDraftToRestore
                    },
                    canContinueAfterInterruption = error is InterruptedInteractionException || error is RejectedInteractionException && error.pending.interrupted,
                    errorMessage = when {
                        error is RejectedInteractionException -> error.message
                        error is InterruptedInteractionException -> getApplication<Application>().getString(R.string.chatbot_interruption_warning)
                        !restoreQuestion -> error.toUserMessage()
                        previousPrompt!!.isExpired() -> "Esta pergunta expirou. Envie uma nova mensagem."
                        else -> "${error.toUserMessage()} Tente novamente com a mesma resposta ou reabra a conversa."
                    },
                )
            } finally {
                if (generation == accountGeneration) {
                    requestInFlight = false
                    val pending = runCatching { pendingStore.get(currentState.userKey) }.getOrNull()
                    currentState = currentState.copy(isSending = pending?.rejectionCode == null && pending != null)
                }
            }
        }
    }

    private suspend fun startPendingRecovery(userKey: String, generation: Int) {
        if (requestInFlight || generation != accountGeneration) return
        requestInFlight = true
        currentState = currentState.copy(isSending = true)
        val pending = try {
            pendingStore.get(userKey)
        } catch (error: CancellationException) {
            requestInFlight = false
            throw error
        } catch (error: Exception) {
            markPendingRecoveryUnavailable()
            return
        }
        if (generation != accountGeneration) return
        if (pending == null) {
            requestInFlight = false
            currentState = currentState.copy(isSending = false)
            return
        }
        if (pending.rejectionCode != null) {
            requestInFlight = false
            showRejectedInteraction(pending)
            return
        }
        if (pending.interrupted) {
            showInterruptedInteraction(pending)
            requestInFlight = false
            return
        }
        interactionJob = viewModelScope.launch {
            try {
                processPending(userKey, generation)
                if (generation == accountGeneration) refreshSessionAfterRecovery(pending.sessionId, userKey, generation)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (generation == accountGeneration) {
                    when (error) {
                        is RejectedInteractionException -> if (error.pending.interrupted) showInterruptedInteraction(error.pending) else showRejectedInteraction(error.pending)
                        is InterruptedInteractionException -> {
                            val interrupted = runCatching { pendingStore.get(userKey) }.getOrNull()
                            if (interrupted != null) showInterruptedInteraction(interrupted) else markPendingRecoveryUnavailable(error.toUserMessage())
                        }
                        else -> markPendingRecoveryUnavailable(error.toUserMessage())
                    }
                }
            } finally {
                finishPendingRecovery(generation, userKey)
            }
        }
    }

    private suspend fun finishPendingRecovery(generation: Int, userKey: String) {
        if (generation != accountGeneration) return
        if (currentState.canRetryPendingRecovery) {
            requestInFlight = false
            return
        }
        val pending = try {
            pendingStore.get(userKey)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            requestInFlight = false
            markPendingRecoveryUnavailable()
            return
        }
        requestInFlight = false
        currentState = currentState.copy(
            isSending = pending != null && pending.rejectionCode == null,
            canContinueAfterInterruption = pending?.interrupted == true && pending.rejectionCode == null,
        )
    }

    private fun markPendingRecoveryUnavailable(message: String = "Não foi possível recuperar a interação pendente.") {
        requestInFlight = false
        currentState = currentState.copy(
            isSending = false,
            isRecoveryBlocked = true,
            canRetryPendingRecovery = true,
            errorMessage = message,
        )
    }

    private fun showInterruptedInteraction(pending: PendingInteraction) {
        val current = currentState
        val existingText = pending.input.recoveryText()
        val draft = existingText?.takeIf { text ->
            current.currentSessionId == pending.sessionId && current.messages.none { it.sender == ChatSender.USER && it.text == text }
        }?.let { rejectedDraft(pending.sessionId, it) }
        currentState = current.copy(
            isSending = true,
            canContinueAfterInterruption = true,
            answerDraftToRestore = draft ?: current.answerDraftToRestore,
            isRecoveryBlocked = false,
            canRetryPendingRecovery = false,
            errorMessage = getApplication<Application>().getString(R.string.chatbot_interruption_warning),
        )
    }

    fun retryPendingRecovery() {
        val current = currentState
        if (requestInFlight || !current.canRetryPendingRecovery || current.userKey.isBlank()) return
        val generation = accountGeneration
        currentState = current.copy(isSending = true, isRecoveryBlocked = false, canRetryPendingRecovery = false, errorMessage = null)
        viewModelScope.launch { startPendingRecovery(current.userKey, generation) }
    }

    fun continueAfterInterruption() {
        val current = currentState
        if (requestInFlight || !current.canContinueAfterInterruption || current.userKey.isBlank()) return
        requestInFlight = true
        currentState = current.copy(canContinueAfterInterruption = false)
        viewModelScope.launch {
            try {
                val pending = pendingStore.get(current.userKey)
                if (pending?.interrupted == true && pending.rejectionCode == null) {
                    pendingStore.save(pending.copy(rejectionCode = -1))
                }
                currentState = currentState.copy(errorMessage = "O resultado da interação continua incerto. Consulte o histórico se estiver disponível.")
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                currentState = currentState.copy(canContinueAfterInterruption = true, errorMessage = "Não foi possível continuar. Tente novamente.")
            } finally {
                finishPendingRecovery(accountGeneration, current.userKey)
            }
        }
    }

    private suspend fun processPending(userKey: String, generation: Int): AiInteractionResultDto {
        while (generation == accountGeneration) {
            val pending = pendingStore.get(userKey) ?: throw IllegalStateException("A interação pendente não foi encontrada.")
            if (pending.interrupted || pending.rejectionCode != null) throw RejectedInteractionException(pending)
            try {
                val result = pendingRunner.run(pending)
                if (generation != accountGeneration) throw CancellationException()
                pendingStore.clear(userKey)
                return result
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (generation != accountGeneration) throw CancellationException()
                if (error is InterruptedInteractionException) throw RejectedInteractionException(pendingStore.get(userKey) ?: pending.copy(interrupted = true))
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
                currentState = currentState.copy(errorMessage = "Aguardando conexão para recuperar a interação.")
                delay(10_000)
            }
        }
        throw CancellationException()
    }

    private suspend fun refreshSessionAfterRecovery(sessionId: String, userKey: String, generation: Int) {
        if (currentState.currentSessionId != sessionId) return
        try {
            val page = aiRepository.getMessages(sessionId)
            if (generation != accountGeneration || currentState.currentSessionId != sessionId) return
            val messages = page.toMessages(userKey)
            currentState = currentState.copy(
                messages = messages,
                messagesCursor = page.nextCursor,
                hasEarlierMessages = page.hasMore,
                composerHint = messages.lastOrNull()?.composerHint() ?: "Peça ao Tobby",
                errorMessage = null,
            )
            writeCache { chatCache.savePage(userKey, sessionId, messages, page.nextCursor, page.hasMore, replace = true) }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            currentState = currentState.copy(errorMessage = "Interação concluída. Reabra a conversa para atualizar o histórico.")
        }
    }

    private fun showRejectedInteraction(pending: PendingInteraction) {
        if (pending.interrupted) {
            showInterruptedInteraction(pending)
            return
        }
        val text = if (pending.operationId == null && !pending.interrupted) pending.input.message ?: pending.input.answer?.text else null
        if (text != null) rememberComposerDraft(pending.userKey, pending.sessionId, text)
        currentState = currentState.copy(
            isSending = requestInFlight,
            canContinueAfterInterruption = false,
            answerDraftToRestore = text?.let { rejectedDraft(pending.sessionId, it) },
            errorMessage = RejectedInteractionException(pending).message,
        )
    }

    private fun AiInteractionInputDto.recoveryText(): String? = when (kind) {
        "message" -> message
        "answer" -> when (answer?.kind) {
            "text" -> answer.text
            "confirmation" -> if (answer.confirm == true) "Confirmar" else "Cancelar"
            else -> null
        }
        else -> null
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

    private fun Exception.toUserMessage(): String = when {
        this is InterruptedInteractionException -> getApplication<Application>().getString(R.string.chatbot_interruption_warning)
        this is RejectedInteractionException -> message.orEmpty()
        else -> when ((this as? HttpException)?.code()) {
        401 -> "Sua sessão do ChatTobby expirou. Entre novamente para continuar."
        403 -> "Sua conta não tem fazendas autorizadas para usar o ChatTobby."
        404 -> "Esta conversa não está mais disponível."
        503 -> "O serviço do ChatTobby está temporariamente indisponível. Tente novamente."
        else -> "Não foi possível concluir a operação do ChatTobby. Verifique sua conexão e tente novamente."
        }
    }
}
