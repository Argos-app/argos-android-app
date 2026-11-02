package com.android.argosapp.view.fragment

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.argosapp.R
import com.android.argosapp.databinding.FragmentChatConversationBinding
import com.android.argosapp.model.ChatMessageKind
import com.android.argosapp.model.latestUnansweredPrompt
import com.android.argosapp.view.adapter.ChatMessageAdapter
import com.android.argosapp.viewmodel.ChatbotPage
import com.android.argosapp.viewmodel.ChatbotUiState
import com.android.argosapp.viewmodel.ChatbotViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import android.text.InputFilter

class ChatConversationFragment : Fragment(R.layout.fragment_chat_conversation) {
    private var _binding: FragmentChatConversationBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val viewModel: ChatbotViewModel by viewModels({ requireParentFragment() })
    private val messageAdapter by lazy { ChatMessageAdapter(viewModel::selectOption, viewModel::confirmSend) }
    private var anchorId: String? = null
    private var anchorOffset = 0
    private var displayedSessionId: String? = null
    private val locale = Locale("pt", "BR")
    private val expirationHandler = Handler(Looper.getMainLooper())
    private var expirationTask: Runnable? = null
    private var lastRenderedState: ChatbotUiState? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentChatConversationBinding.bind(view)
        binding.chatbotMessages.layoutManager = LinearLayoutManager(requireContext())
        binding.chatbotMessages.adapter = messageAdapter
        binding.chatbotInput.filters = arrayOf(InputFilter.LengthFilter(MAX_CHAT_TEXT_LENGTH))
        binding.chatbotInput.setText(viewModel.draft)
        binding.chatbotInput.setSelection(binding.chatbotInput.length())
        binding.chatbotInput.doAfterTextChanged { viewModel.draft = it?.toString().orEmpty() }
        binding.chatbotSend.setOnClickListener { sendComposerMessage() }
        binding.chatbotLoadEarlier.setOnClickListener {
            val manager = binding.chatbotMessages.layoutManager as LinearLayoutManager
            val position = manager.findFirstVisibleItemPosition()
            anchorId = messageAdapter.currentList.getOrNull(position)?.id
            anchorOffset = manager.findViewByPosition(position)?.top ?: 0
            viewModel.loadEarlierMessages()
        }
        binding.chatbotInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND || event?.let { it.keyCode == KeyEvent.KEYCODE_ENTER && !it.isShiftPressed } == true) {
                if (binding.chatbotSend.isEnabled) sendComposerMessage()
                true
            } else false
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.state.collect(::render) }
        }
    }

    override fun onDestroyView() {
        binding.chatbotMessages.adapter = null
        expirationTask?.let(expirationHandler::removeCallbacks)
        expirationTask = null
        lastRenderedState = null
        displayedSessionId = null
        _binding = null
        super.onDestroyView()
    }

    private fun sendComposerMessage() {
        val message = binding.chatbotInput.text?.toString().orEmpty().trim()
        if (message.isBlank()) return
        binding.chatbotInput.text?.clear()
        (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
            ?.hideSoftInputFromWindow(binding.chatbotInput.windowToken, 0)
        viewModel.sendText(message)
    }

    private fun render(state: ChatbotUiState) {
        lastRenderedState = state
        scheduleExpirationRefresh(state)
        val conversation = state.page == ChatbotPage.CONVERSATION
        binding.chatbotSending.isVisible = state.isSending
        binding.chatbotSending.text = if (state.isSending) getString(R.string.chatbot_sending) else ""
        binding.chatbotInput.hint = state.composerHint
        state.answerDraftToRestore?.let { draft ->
            if (binding.chatbotInput.text.isNullOrEmpty()) {
                binding.chatbotInput.setText(draft)
                binding.chatbotInput.setSelection(draft.length)
            }
            viewModel.consumeAnswerDraft()
        }
        binding.chatbotInput.isEnabled = true
        binding.chatbotSend.isEnabled = !state.isSending && !state.isLoading && !state.isRecoveryBlocked && state.userKey.isNotBlank()
        binding.chatbotSend.alpha = if (binding.chatbotSend.isEnabled) 1f else 0.45f
        binding.chatbotLoadEarlier.isVisible = state.hasEarlierMessages
        binding.chatbotLoadEarlier.isEnabled = !state.isLoading
        val session = state.sessions.firstOrNull { it.id == state.currentSessionId }
        binding.chatbotSessionLabel.isVisible = session != null
        binding.chatbotSessionLabel.text = session?.let {
            getString(R.string.chatbot_current_session, sessionDate(it.createdAt).replace(" • ", ", "))
        }.orEmpty()
        val activeId = state.messages.latestUnansweredPrompt()
            ?.takeIf { !state.isSending && it.kind in setOf(ChatMessageKind.SINGLE_CHOICE, ChatMessageKind.CONFIRMATION) }?.id
        messageAdapter.activePromptId = activeId
        if (conversation && !state.isLoading) {
            val oldList = messageAdapter.currentList
            val changedSession = displayedSessionId != state.currentSessionId
            val newLastMessage = oldList.lastOrNull()?.id != state.messages.lastOrNull()?.id
            val wasAtBottom = !binding.chatbotMessages.canScrollVertically(1)
            val savedAnchor = anchorId
            val savedOffset = anchorOffset
            displayedSessionId = state.currentSessionId
            messageAdapter.submitList(state.messages) {
                val currentBinding = _binding ?: return@submitList
                val manager = currentBinding.chatbotMessages.layoutManager as? LinearLayoutManager ?: return@submitList
                val position = savedAnchor?.let { id -> messageAdapter.currentList.indexOfFirst { it.id == id } } ?: -1
                if (position >= 0) manager.scrollToPositionWithOffset(position, savedOffset)
                else if ((changedSession || (newLastMessage && wasAtBottom)) && messageAdapter.itemCount > 0) {
                    manager.scrollToPosition(messageAdapter.itemCount - 1)
                }
                if (anchorId == savedAnchor) anchorId = null
            }
        } else if (!conversation) {
            displayedSessionId = null
            anchorId = null
        }
        val lastMessage = state.messages.lastOrNull()
        val expired = lastMessage?.questionExpiresAt?.let { it <= System.currentTimeMillis() } == true
        binding.chatbotHelper.text = when (state.page) {
            ChatbotPage.HISTORY -> getString(R.string.chatbot_history_helper)
            ChatbotPage.CONVERSATION -> when {
                expired -> getString(R.string.chatbot_question_expired)
                lastMessage?.kind == ChatMessageKind.SINGLE_CHOICE -> getString(R.string.chatbot_choice_helper)
                lastMessage?.kind == ChatMessageKind.CONFIRMATION -> getString(R.string.chatbot_confirmation_helper)
                else -> getString(R.string.chatbot_continue_helper)
            }
            ChatbotPage.NEW_SESSION -> getString(R.string.chatbot_new_session_helper)
        }
    }

    private fun scheduleExpirationRefresh(state: ChatbotUiState) {
        expirationTask?.let(expirationHandler::removeCallbacks)
        expirationTask = null
        val now = System.currentTimeMillis()
        val nextExpiration = state.messages.mapNotNull { it.questionExpiresAt }
            .filter { it > now }
            .minOrNull() ?: return
        expirationTask = Runnable { lastRenderedState?.let(::render) }.also {
            expirationHandler.postDelayed(it, (nextExpiration - now).coerceAtLeast(1L))
        }
    }

    private fun sessionDate(timestamp: Long): String {
        val dateTime = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
        val time = dateTime.format(DateTimeFormatter.ofPattern("HH:mm", locale))
        return when (dateTime.toLocalDate()) {
            LocalDate.now() -> getString(R.string.chatbot_session_date_today, time)
            LocalDate.now().minusDays(1) -> getString(R.string.chatbot_session_date_yesterday, time)
            else -> dateTime.format(DateTimeFormatter.ofPattern("d 'de' MMMM", locale)) + " • $time"
        }
    }

    private companion object { const val MAX_CHAT_TEXT_LENGTH = 8000 }
}
