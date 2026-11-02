package com.android.argosapp.view.fragment

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.argosapp.R
import com.android.argosapp.databinding.FragmentChatbotBinding
import com.android.argosapp.view.activity.HomeActivity
import com.android.argosapp.viewmodel.ChatbotPage
import com.android.argosapp.viewmodel.ChatbotUiState
import com.android.argosapp.viewmodel.ChatbotViewModel
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class ChatbotFragment : Fragment(R.layout.fragment_chatbot) {
    private var _binding: FragmentChatbotBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val viewModel: ChatbotViewModel by viewModels()
    private val conversationBack = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = viewModel.startNewConversation()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentChatbotBinding.bind(view)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, conversationBack)
        binding.chatbotBack.setOnClickListener {
            if (conversationBack.isEnabled) viewModel.startNewConversation()
            else (activity as? HomeActivity)?.navigateToHome()
        }
        binding.chatbotNewTab.setOnClickListener { viewModel.startNewConversation() }
        binding.chatbotHistoryTab.setOnClickListener { viewModel.showHistory() }
        binding.chatbotRetry.setOnClickListener { viewModel.retryOrDismissError() }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.state.collect(::render) }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun render(state: ChatbotUiState) {
        val history = state.page == ChatbotPage.HISTORY
        conversationBack.isEnabled = state.page == ChatbotPage.CONVERSATION
        binding.chatbotTitle.text = if (conversationBack.isEnabled) {
            state.pendingSessionTitle ?: state.sessions.firstOrNull { it.id == state.currentSessionId }?.title
                ?: getString(R.string.chatbot_title)
        } else getString(R.string.chatbot_title)
        binding.chatbotTabs.isVisible = true
        binding.chatbotNewTab.isEnabled = !state.isSending
        binding.chatbotHistoryTab.isEnabled = !state.isSending
        binding.chatbotLoading.isVisible = state.isLoading
        binding.chatbotErrorCard.isVisible = state.errorMessage != null
        binding.chatbotError.text = state.errorMessage.orEmpty()
        binding.chatbotRetry.setText(if (history || state.userKey.isBlank() || state.canRetryPendingRecovery || state.page == ChatbotPage.CONVERSATION && state.currentSessionId != null) R.string.chatbot_retry else R.string.chatbot_dismiss_error)
        if (state.canContinueAfterInterruption) binding.chatbotRetry.setText(R.string.chatbot_continue_after_review)
        styleTab(binding.chatbotNewTab, binding.chatbotNewTabLabel, state.page == ChatbotPage.NEW_SESSION)
        styleTab(binding.chatbotHistoryTab, binding.chatbotHistoryTabLabel, history)
        val existing = childFragmentManager.findFragmentById(R.id.chatbotPageContainer)
        val matches = when (state.page) {
            ChatbotPage.NEW_SESSION -> existing is ChatNewConversationFragment
            ChatbotPage.HISTORY -> existing is ChatHistoryFragment
            ChatbotPage.CONVERSATION -> existing is ChatConversationFragment
        }
        if (!childFragmentManager.isStateSaved && !matches) {
            val fragment = when (state.page) {
                ChatbotPage.NEW_SESSION -> ChatNewConversationFragment()
                ChatbotPage.HISTORY -> ChatHistoryFragment()
                ChatbotPage.CONVERSATION -> ChatConversationFragment()
            }
            childFragmentManager.beginTransaction()
                .replace(R.id.chatbotPageContainer, fragment)
                .commitNow()
        }
    }

    private fun styleTab(card: MaterialCardView, label: TextView, selected: Boolean) {
        val green = ContextCompat.getColor(requireContext(), R.color.argos_green)
        val background = ContextCompat.getColor(requireContext(), R.color.argos_background)
        card.setCardBackgroundColor(if (selected) green else background)
        card.strokeColor = green
        card.strokeWidth = if (selected) 0 else resources.displayMetrics.density.toInt().coerceAtLeast(1)
        label.setTextColor(if (selected) background else green)
    }
}
