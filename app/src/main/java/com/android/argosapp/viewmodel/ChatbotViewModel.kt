package com.android.argosapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.argosapp.ArgosApplication
import com.android.argosapp.data.cache.storage.ChatHistoryStorage
import com.android.argosapp.data.cache.storage.UserStorage
import com.android.argosapp.model.ChatMessage
import com.android.argosapp.model.ChatMessageKind
import com.android.argosapp.model.ChatReply
import com.android.argosapp.model.ChatRequest
import com.android.argosapp.model.ChatResponseState
import com.android.argosapp.model.ChatSender
import com.android.argosapp.model.ChatSession
import com.android.argosapp.util.session.ChatbotOnboardingStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

enum class ChatbotPage { NEW_SESSION, CONVERSATION, HISTORY }

data class ChatbotUiState(
    val isLoading: Boolean = true,
    val onboardingVisible: Boolean = false,
    val page: ChatbotPage = ChatbotPage.NEW_SESSION,
    val userName: String = "",
    val userKey: String = "",
    val currentSessionId: String? = null,
    val sessions: List<ChatSession> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val composerHint: String = "Peça ao Tobby",
    val isSending: Boolean = false,
    val errorMessage: String? = null,
)

class ChatbotViewModel(application: Application) : AndroidViewModel(application) {
    private val userStorage = UserStorage(application)
    private val historyStorage = ChatHistoryStorage(application)
    private val onboardingStorage = ChatbotOnboardingStorage(application)
    private val apiRepository = (application as ArgosApplication).argosApiRepository

    private val mutableState = MutableStateFlow(ChatbotUiState())
    val state: StateFlow<ChatbotUiState> = mutableState.asStateFlow()
    private var requestInFlight = false

    init {
        viewModelScope.launch {
            try {
                val user = userStorage.getUser()
                val email = user?.userEmail.orEmpty().trim()
                if (email.isBlank()) {
                    mutableState.value = ChatbotUiState(
                        isLoading = false,
                        errorMessage = "Não foi possível identificar sua conta.",
                    )
                    return@launch
                }

                val userKey = email.lowercase(Locale.ROOT)
                val completed = onboardingStorage.isCompleted(email)
                mutableState.value = ChatbotUiState(
                    isLoading = false,
                    onboardingVisible = !completed,
                    userName = user?.userName.orEmpty(),
                    userKey = userKey,
                    sessions = historyStorage.getSessions(userKey),
                )
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                mutableState.value = ChatbotUiState(
                    isLoading = false,
                    errorMessage = "Não foi possível carregar o ChatTobby.",
                )
            }
        }
    }

    fun completeOnboarding() {
        val current = mutableState.value
        if (current.userKey.isBlank()) return
        onboardingStorage.markCompleted(current.userKey)
        mutableState.value = current.copy(onboardingVisible = false, page = ChatbotPage.NEW_SESSION)
    }

    fun showHistory() {
        viewModelScope.launch {
            val current = mutableState.value
            if (current.userKey.isBlank()) return@launch
            mutableState.value = current.copy(
                page = ChatbotPage.HISTORY,
                currentSessionId = null,
                messages = emptyList(),
                sessions = historyStorage.getSessions(current.userKey),
                errorMessage = null,
            )
        }
    }

    fun startNewConversation() {
        mutableState.value = mutableState.value.copy(
            page = ChatbotPage.NEW_SESSION,
            currentSessionId = null,
            messages = emptyList(),
            composerHint = "Peça ao Tobby",
            errorMessage = null,
        )
    }

    fun openSession(sessionId: String) {
        viewModelScope.launch {
            val current = mutableState.value
            if (current.userKey.isBlank()) return@launch
            val session = historyStorage.getSessions(current.userKey)
                .firstOrNull { it.id == sessionId } ?: return@launch
            val messages = historyStorage.getMessages(sessionId, current.userKey)
            mutableState.value = current.copy(
                page = ChatbotPage.CONVERSATION,
                currentSessionId = session.id,
                messages = messages,
                composerHint = messages.lastOrNull()
                    ?.takeIf { it.kind == ChatMessageKind.FREE_TEXT_PROMPT }
                    ?.let { "Digite o lote..." }
                    ?: "Peça ao Tobby",
                errorMessage = null,
            )
        }
    }

    fun loadEarlierMessages() {
        viewModelScope.launch {
            val current = mutableState.value
            val sessionId = current.currentSessionId ?: return@launch
            mutableState.value = current.copy(
                messages = historyStorage.getMessages(sessionId, current.userKey),
            )
        }
    }

    fun sendText(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        val current = mutableState.value
        if (current.messages.lastOrNull()?.let {
                it.sender == ChatSender.ASSISTANT && it.kind == ChatMessageKind.CONFIRMATION
            } == true
        ) {
            mutableState.value = current.copy(errorMessage = "Use os botões para confirmar ou cancelar o envio.")
            return
        }
        val lastPrompt = mutableState.value.messages.lastOrNull()
            ?.takeIf { it.sender == ChatSender.ASSISTANT }
            ?.kind
            ?.takeIf {
                it == ChatMessageKind.SINGLE_CHOICE ||
                    it == ChatMessageKind.CONFIRMATION ||
                    it == ChatMessageKind.FREE_TEXT_PROMPT
            }
        send(trimmed, lastPrompt)
    }

    fun selectOption(option: String) {
        val current = mutableState.value
        if (current.isSending || current.messages.lastOrNull()?.let {
                it.sender == ChatSender.ASSISTANT && it.kind == ChatMessageKind.SINGLE_CHOICE
            } != true
        ) return
        send(option, ChatMessageKind.SINGLE_CHOICE)
    }

    fun confirmSend(confirmed: Boolean) {
        val current = mutableState.value
        if (current.isSending || current.messages.lastOrNull()?.let {
                it.sender == ChatSender.ASSISTANT && it.kind == ChatMessageKind.CONFIRMATION
            } != true
        ) return
        send(
            text = if (confirmed) "Confirmar envio" else "Não enviar",
            previousPrompt = ChatMessageKind.CONFIRMATION,
            confirmed = confirmed,
        )
    }

    private fun send(
        text: String,
        previousPrompt: ChatMessageKind? = null,
        confirmed: Boolean? = null,
    ) {
        if (requestInFlight || mutableState.value.isSending) return
        requestInFlight = true
        mutableState.value = mutableState.value.copy(isSending = true)
        viewModelScope.launch {
            try {
                var current = mutableState.value
                if (current.userKey.isBlank() || current.onboardingVisible) return@launch
                var sessionId = current.currentSessionId
                if (sessionId == null) {
                    val session = historyStorage.createSession(current.userKey)
                    sessionId = session.id
                    current = current.copy(
                        page = ChatbotPage.CONVERSATION,
                        currentSessionId = session.id,
                        sessions = listOf(session) + current.sessions,
                    )
                }

                val userMessage = historyStorage.appendMessage(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        sessionId = sessionId,
                        userKey = current.userKey,
                        position = 0,
                        sender = ChatSender.USER,
                        text = text,
                        createdAt = System.currentTimeMillis(),
                    ),
                )
                current = current.copy(
                    page = ChatbotPage.CONVERSATION,
                    messages = current.messages + userMessage,
                    isSending = true,
                    errorMessage = null,
                    composerHint = "Peça ao Tobby",
                )
                mutableState.value = current

                var answerMessage: ChatMessage? = null
                apiRepository.sendChatMessage(
                    ChatRequest(text, previousPrompt, confirmed),
                ).collect { reply ->
                    val session = sessionId ?: return@collect
                    when (reply) {
                        is ChatReply.Prompt -> {
                            val assistantMessage = historyStorage.appendMessage(
                                ChatMessage(
                                    id = UUID.randomUUID().toString(),
                                    sessionId = session,
                                    userKey = current.userKey,
                                    position = 0,
                                    sender = ChatSender.ASSISTANT,
                                    text = reply.text,
                                    kind = reply.kind,
                                    options = reply.options,
                                    createdAt = System.currentTimeMillis(),
                                ),
                            )
                            current = current.copy(
                                messages = current.messages + assistantMessage,
                                composerHint = if (reply.kind == ChatMessageKind.FREE_TEXT_PROMPT) {
                                    "Digite o lote..."
                                } else {
                                    "Peça ao Tobby"
                                },
                            )
                            mutableState.value = current
                        }

                        is ChatReply.Answer -> {
                            val now = System.currentTimeMillis()
                            val existing = answerMessage
                            if (existing == null) {
                                val appended = historyStorage.appendMessage(
                                    ChatMessage(
                                        id = UUID.randomUUID().toString(),
                                        sessionId = session,
                                        userKey = current.userKey,
                                        position = 0,
                                        sender = ChatSender.ASSISTANT,
                                        text = reply.text,
                                        responseState = reply.state,
                                        createdAt = now,
                                    ),
                                )
                                answerMessage = appended
                                current = current.copy(messages = current.messages + appended)
                            } else {
                                answerMessage = existing.copy(text = reply.text, responseState = reply.state)
                                historyStorage.updateMessage(requireNotNull(answerMessage))
                                current = current.copy(
                                    messages = current.messages.map {
                                        if (it.id == existing.id) requireNotNull(answerMessage) else it
                                    },
                                )
                            }
                            mutableState.value = current
                        }
                    }
                }
                mutableState.value = mutableState.value.copy(
                    isSending = false,
                    sessions = historyStorage.getSessions(mutableState.value.userKey),
                )
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                mutableState.value = mutableState.value.copy(
                    isSending = false,
                    errorMessage = "Não foi possível enviar sua mensagem. Tente novamente.",
                )
            } finally {
                requestInFlight = false
                mutableState.value = mutableState.value.copy(isSending = false)
            }
        }
    }
}
