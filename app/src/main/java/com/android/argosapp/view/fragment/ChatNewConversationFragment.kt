package com.android.argosapp.view.fragment

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.text.InputFilter
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.argosapp.R
import com.android.argosapp.databinding.FragmentChatNewConversationBinding
import com.android.argosapp.viewmodel.ChatbotViewModel
import kotlinx.coroutines.launch

class ChatNewConversationFragment : Fragment(R.layout.fragment_chat_new_conversation) {
    private var _binding: FragmentChatNewConversationBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val viewModel: ChatbotViewModel by viewModels({ requireParentFragment() })

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentChatNewConversationBinding.bind(view)
        binding.chatbotInput.filters = arrayOf(InputFilter.LengthFilter(MAX_CHAT_TEXT_LENGTH))
        binding.chatbotInput.setText(viewModel.draft)
        binding.chatbotInput.setSelection(binding.chatbotInput.length())
        binding.chatbotInput.doAfterTextChanged { viewModel.draft = it?.toString().orEmpty() }
        binding.chatbotSend.setOnClickListener { sendMessage() }
        binding.suggestHerd.setOnClickListener { viewModel.sendText(getString(R.string.chatbot_suggestion_herd)) }
        binding.suggestConsumption.setOnClickListener { viewModel.sendText(getString(R.string.chatbot_suggestion_consumption)) }
        binding.suggestMedicine.setOnClickListener { viewModel.sendText(getString(R.string.chatbot_suggestion_medication)) }
        binding.chatbotInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND || event?.let { it.keyCode == KeyEvent.KEYCODE_ENTER && !it.isShiftPressed } == true) {
                if (binding.chatbotSend.isEnabled) sendMessage()
                true
            } else false
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    binding.chatbotSending.isVisible = state.isSending
                    binding.chatbotInput.hint = state.composerHint
                    binding.chatbotSend.isEnabled = !state.isSending && !state.isLoading && state.userKey.isNotBlank()
                    binding.chatbotSend.alpha = if (binding.chatbotSend.isEnabled) 1f else 0.45f
                    binding.suggestHerd.isEnabled = binding.chatbotSend.isEnabled
                    binding.suggestConsumption.isEnabled = binding.chatbotSend.isEnabled
                    binding.suggestMedicine.isEnabled = binding.chatbotSend.isEnabled
                }
            }
        }
    }

    private fun sendMessage() {
        val message = binding.chatbotInput.text?.toString().orEmpty().trim()
        if (message.isBlank()) return
        binding.chatbotInput.text?.clear()
        (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
            ?.hideSoftInputFromWindow(binding.chatbotInput.windowToken, 0)
        viewModel.sendText(message)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private companion object { const val MAX_CHAT_TEXT_LENGTH = 8000 }
}
