package com.android.argosapp.view.fragment

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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

class ChatConversationFragment : Fragment(R.layout.fragment_chat_conversation) {
    private var _binding: FragmentChatConversationBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val viewModel: ChatbotViewModel by activityViewModels()
    private val messageAdapter by lazy { ChatMessageAdapter(viewModel::selectOption, viewModel::confirmSend) }
    private var anchorId: String? = null
    private var anchorOffset = 0
    private var displayedSessionId: String? = null
    private var composerKey: Pair<String, String?>? = null
    private val locale = Locale("pt", "BR")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentChatConversationBinding.bind(view)
        binding.chatbotMessages.layoutManager = LinearLayoutManager(requireContext())
        binding.chatbotMessages.adapter = messageAdapter
        binding.chatbotInput.setText(viewModel.draft)
        binding.chatbotInput.setSelection(binding.chatbotInput.length())
        binding.chatbotInput.doAfterTextChanged {
            val text = it?.toString().orEmpty()
            viewModel.draft = text
            composerKey?.let { (userKey, sessionId) -> viewModel.rememberComposerDraft(userKey, sessionId, text) }
        }
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
        val conversation = state.page == ChatbotPage.CONVERSATION
        binding.chatbotSending.isVisible = state.isSending
        binding.chatbotInput.hint = state.composerHint
        state.answerDraftToRestore?.let { restored ->
            if (restored.sessionId == state.currentSessionId && binding.chatbotInput.text.isNullOrEmpty()) {
                binding.chatbotInput.setText(restored.text)
                binding.chatbotInput.setSelection(restored.text.length)
            }
            viewModel.rememberComposerDraft(state.userKey, restored.sessionId, restored.text)
            viewModel.consumeAnswerDraft()
        }
        binding.chatbotInput.isEnabled = true
        val activeComposerKey = state.userKey to state.currentSessionId
        if (composerKey != activeComposerKey) {
            composerKey = activeComposerKey
            val restored = viewModel.composerDraft(state.userKey, state.currentSessionId)
            if (binding.chatbotInput.text?.toString() != restored) binding.chatbotInput.setText(restored)
            binding.chatbotInput.setSelection(binding.chatbotInput.length())
        }
        binding.chatbotSend.isEnabled = !state.isSending && !state.isLoading && state.userKey.isNotBlank()
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

    private fun sessionDate(timestamp: Long): String {
        val dateTime = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
        val time = dateTime.format(DateTimeFormatter.ofPattern("HH:mm", locale))
        return when (dateTime.toLocalDate()) {
            LocalDate.now() -> getString(R.string.chatbot_session_date_today, time)
            LocalDate.now().minusDays(1) -> getString(R.string.chatbot_session_date_yesterday, time)
            else -> dateTime.format(DateTimeFormatter.ofPattern("d 'de' MMMM", locale)) + " • $time"
        }
    }
}
