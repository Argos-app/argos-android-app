package com.android.argosapp.view.fragment

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.argosapp.R
import com.android.argosapp.databinding.FragmentChatHistoryBinding
import com.android.argosapp.view.adapter.ChatSessionAdapter
import com.android.argosapp.viewmodel.ChatbotViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class ChatHistoryFragment : Fragment(R.layout.fragment_chat_history) {
    private var _binding: FragmentChatHistoryBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val viewModel: ChatbotViewModel by activityViewModels()
    private val sessionAdapter by lazy { ChatSessionAdapter(::sessionDate, viewModel::openSession) }
    private val locale = Locale("pt", "BR")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentChatHistoryBinding.bind(view)
        binding.chatbotSessions.layoutManager = LinearLayoutManager(requireContext())
        binding.chatbotSessions.adapter = sessionAdapter
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    binding.chatbotEmptyHistory.isVisible = !state.isLoading && state.errorMessage == null && state.sessions.isEmpty()
                    sessionAdapter.submitList(state.sessions)
                }
            }
        }
    }

    override fun onDestroyView() {
        binding.chatbotSessions.adapter = null
        _binding = null
        super.onDestroyView()
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
