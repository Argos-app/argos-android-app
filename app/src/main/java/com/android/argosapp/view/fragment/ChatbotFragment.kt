package com.android.argosapp.view.fragment

import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.argosapp.R
import com.android.argosapp.databinding.FragmentChatbotBinding
import com.android.argosapp.model.ChatMessage
import com.android.argosapp.model.ChatMessageKind
import com.android.argosapp.model.ChatResponseState
import com.android.argosapp.model.ChatSender
import com.android.argosapp.model.latestUnansweredPrompt
import com.android.argosapp.view.activity.HomeActivity
import com.android.argosapp.viewmodel.ChatbotPage
import com.android.argosapp.viewmodel.ChatbotUiState
import com.android.argosapp.viewmodel.ChatbotViewModel
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class ChatbotFragment : Fragment(R.layout.fragment_chatbot) {
    private var _binding: FragmentChatbotBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val viewModel: ChatbotViewModel by viewModels()
    private val locale = Locale("pt", "BR")
    private var pendingScrollAnchorId: String? = null
    private var pendingScrollAnchorOffset = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentChatbotBinding.bind(view)

        binding.chatbotBack.setOnClickListener { (activity as? HomeActivity)?.navigateToHome() }
        binding.chatbotOnboardingBack.setOnClickListener { (activity as? HomeActivity)?.navigateToHome() }
        binding.chatbotOnboardingCta.setOnClickListener { viewModel.completeOnboarding() }
        binding.chatbotNewTab.setOnClickListener { viewModel.startNewConversation() }
        binding.chatbotHistoryTab.setOnClickListener { viewModel.showHistory() }
        binding.chatbotSend.setOnClickListener { sendComposerMessage() }
        binding.chatbotInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                event?.let { it.keyCode == KeyEvent.KEYCODE_ENTER && !it.isShiftPressed } == true
            ) {
                sendComposerMessage()
                true
            } else {
                false
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
    }

    override fun onDestroyView() {
        (activity as? HomeActivity)?.setChatbotOnboardingVisible(false)
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
        val loadingEarlierMessages = state.isLoading &&
            state.page == ChatbotPage.CONVERSATION &&
            state.currentSessionId != null &&
            state.messages.isNotEmpty() &&
            state.hasEarlierMessages
        if (loadingEarlierMessages && pendingScrollAnchorId == null) {
            captureVisibleMessageAnchor()
        }
        val restoreScrollAnchor = !state.isLoading &&
            state.page == ChatbotPage.CONVERSATION &&
            pendingScrollAnchorId != null
        val anchorId = pendingScrollAnchorId
        val anchorOffset = pendingScrollAnchorOffset

        binding.chatbotOnboarding.isVisible = state.onboardingVisible
        binding.chatbotMain.isVisible = !state.onboardingVisible
        (activity as? HomeActivity)?.setChatbotOnboardingVisible(state.onboardingVisible || state.isLoading)

        binding.chatbotError.isVisible = state.errorMessage != null
        binding.chatbotError.text = state.errorMessage.orEmpty()
        binding.chatbotInput.hint = state.composerHint
        state.answerDraftToRestore?.let { draft ->
            if (binding.chatbotInput.text.isNullOrEmpty()) {
                binding.chatbotInput.setText(draft)
                binding.chatbotInput.setSelection(draft.length)
            }
            viewModel.consumeAnswerDraft()
        }
        binding.chatbotInput.isEnabled = !state.isSending && !state.isLoading
        binding.chatbotSend.isEnabled = !state.isSending && !state.isLoading
        binding.chatbotSend.alpha = if (state.isSending || state.isLoading) 0.45f else 1f

        val historySelected = state.page == ChatbotPage.HISTORY
        styleTab(
            card = binding.chatbotNewTab,
            label = binding.chatbotNewTabLabel,
            selected = !historySelected,
        )
        styleTab(
            card = binding.chatbotHistoryTab,
            label = binding.chatbotHistoryTabLabel,
            selected = historySelected,
        )

        binding.chatbotContent.removeAllViews()
        when {
            state.isLoading -> addText(getString(R.string.chatbot_loading), 14f, color(R.color.argos_text_secondary), Gravity.CENTER)
            state.errorMessage != null && state.userKey.isBlank() ->
                addText(state.errorMessage, 14f, color(R.color.argos_text_secondary), Gravity.CENTER)
            state.page == ChatbotPage.HISTORY -> renderHistory(state)
            state.page == ChatbotPage.CONVERSATION -> renderConversation(state)
            else -> renderNewSession()
        }
        val lastMessage = state.messages.lastOrNull()
        val questionExpired = lastMessage?.questionExpiresAt?.let { it <= System.currentTimeMillis() } == true
        binding.chatbotHelper.text = when (state.page) {
            ChatbotPage.HISTORY -> getString(R.string.chatbot_history_helper)
            ChatbotPage.CONVERSATION -> when {
                questionExpired -> getString(R.string.chatbot_question_expired)
                lastMessage?.kind == ChatMessageKind.SINGLE_CHOICE -> getString(R.string.chatbot_choice_helper)
                lastMessage?.kind == ChatMessageKind.CONFIRMATION -> getString(R.string.chatbot_confirmation_helper)
                else -> getString(R.string.chatbot_continue_helper)
            }
            ChatbotPage.NEW_SESSION -> getString(R.string.chatbot_new_session_helper)
        }
        binding.chatbotContentScroll.post {
            val anchor = if (restoreScrollAnchor) {
                (0 until binding.chatbotContent.childCount)
                    .asSequence()
                    .map(binding.chatbotContent::getChildAt)
                    .firstOrNull { it.tag == anchorId }
            } else {
                null
            }
            if (anchor != null) {
                binding.chatbotContentScroll.scrollTo(0, anchor.top - anchorOffset)
            } else {
                binding.chatbotContentScroll.fullScroll(View.FOCUS_DOWN)
            }
            if (restoreScrollAnchor || (!state.isLoading && state.page != ChatbotPage.CONVERSATION)) {
                pendingScrollAnchorId = null
            }
        }
    }

    private fun captureVisibleMessageAnchor() {
        val scrollTop = binding.chatbotContentScroll.scrollY
        val scrollBottom = scrollTop + binding.chatbotContentScroll.height
        val firstVisibleMessage = (0 until binding.chatbotContent.childCount)
            .asSequence()
            .map(binding.chatbotContent::getChildAt)
            .firstOrNull { child ->
                child.tag is String && child.bottom > scrollTop && child.top < scrollBottom
            }
        pendingScrollAnchorId = firstVisibleMessage?.tag as? String
        if (firstVisibleMessage != null) {
            pendingScrollAnchorOffset = firstVisibleMessage.top - scrollTop
        }
    }

    private fun styleTab(card: MaterialCardView, label: TextView, selected: Boolean) {
        card.setCardBackgroundColor(color(if (selected) R.color.argos_green else R.color.argos_background))
        card.strokeColor = color(if (selected) R.color.argos_green else R.color.argos_green)
        card.strokeWidth = dp(if (selected) 0 else 1)
        label.setTextColor(color(if (selected) R.color.argos_background else R.color.argos_green))
    }

    private fun renderNewSession() {
        addSessionLabel(null)
        val avatar = FrameLayout(requireContext()).apply {
            background = rounded(color(R.color.argos_divider), radius = 42f)
            layoutParams = columnParams(width = 84, height = 84, top = 26, bottom = 14).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
        }
        avatar.addView(ImageView(requireContext()).apply {
            setImageResource(R.drawable.tobby_home)
            contentDescription = getString(R.string.home_mascot_description)
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = FrameLayout.LayoutParams(dp(70), dp(70), Gravity.CENTER)
        })
        binding.chatbotContent.addView(avatar)
        addText(getString(R.string.chatbot_welcome_heading), 21f, color(R.color.argos_green), Gravity.CENTER, bold = true, top = 0)
        addText(getString(R.string.chatbot_welcome_subheading), 14f, color(R.color.argos_green), Gravity.CENTER, top = 12)
        addText(getString(R.string.chatbot_suggestions_heading), 13f, color(R.color.argos_green), bold = true, top = 30, bottom = 14)

        listOf(
            getString(R.string.chatbot_suggestion_herd),
            getString(R.string.chatbot_suggestion_consumption),
            getString(R.string.chatbot_suggestion_medication),
        )
            .forEach { suggestion ->
                addActionCard(suggestion, top = 0, bottom = 12) { viewModel.sendText(suggestion) }
            }
    }

    private fun renderHistory(state: ChatbotUiState) {
        addText(getString(R.string.chatbot_sessions_title), 21f, color(R.color.argos_green), bold = true, top = 16, bottom = 12)
        addText(
            getString(R.string.chatbot_sessions_subtitle),
            14f,
            color(R.color.argos_green),
            bottom = 16,
        )
        state.sessions.forEach { session ->
            val card = MaterialCardView(requireContext()).apply {
                radius = dp(18).toFloat()
                setCardBackgroundColor(color(R.color.argos_divider))
                cardElevation = 0f
                strokeWidth = 0
                isClickable = true
                isFocusable = true
                layoutParams = columnParams(top = 0, bottom = 14)
                setOnClickListener { viewModel.openSession(session.id) }
            }
            val content = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(14), dp(16), dp(14))
            }
            content.addView(textView(sessionDate(session.createdAt), 13f, color(R.color.argos_green), bold = true))
            content.addView(textView(getString(R.string.chatbot_open_conversation), 14f, color(R.color.argos_green)).apply {
                setPadding(0, dp(12), 0, 0)
            })
            card.addView(content)
            binding.chatbotContent.addView(card)
        }
        if (state.sessions.isEmpty()) {
            addText(getString(R.string.chatbot_empty_history), 14f, color(R.color.argos_text_secondary), top = 8)
        }
    }

    private fun renderConversation(state: ChatbotUiState) {
        val currentSession = state.sessions.firstOrNull { it.id == state.currentSessionId }
        addSessionLabel(currentSession?.createdAt)
        addText(
            getString(R.string.chatbot_load_earlier_messages),
            12f,
            color(R.color.argos_green),
            Gravity.CENTER,
            top = 12,
            bottom = 14,
        ).apply {
            isClickable = true
            setOnClickListener { viewModel.loadEarlierMessages() }
        }.isVisible = state.hasEarlierMessages

        val activePromptId = state.messages.latestUnansweredPrompt()
            ?.takeIf { !state.isSending }
            ?.takeIf { it.kind == ChatMessageKind.SINGLE_CHOICE || it.kind == ChatMessageKind.CONFIRMATION }
            ?.id
        state.messages.forEach { message ->
            if (message.sender == ChatSender.USER) renderUserMessage(message)
            else renderAssistantMessage(message, interactive = message.id == activePromptId)
        }
    }

    private fun renderUserMessage(message: ChatMessage) {
        val row = LinearLayout(requireContext()).apply {
            gravity = Gravity.END
            layoutParams = columnParams(bottom = 14)
            tag = message.id
        }
        val bubble = textView(message.text, 13f, color(R.color.argos_background)).apply {
            background = rounded(color(R.color.argos_green), radius = 20f)
            setPadding(dp(16), dp(15), dp(16), dp(15))
        }
        row.addView(bubble, LinearLayout.LayoutParams(dp(310).coerceAtMost(resources.displayMetrics.widthPixels - dp(90)), -2))
        binding.chatbotContent.addView(row)
    }

    private fun renderAssistantMessage(message: ChatMessage, interactive: Boolean) {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM
            layoutParams = columnParams(bottom = 14)
            tag = message.id
        }
        val avatar = FrameLayout(requireContext()).apply {
            background = rounded(color(R.color.argos_divider), radius = 18f)
            layoutParams = LinearLayout.LayoutParams(dp(38), dp(38)).apply { marginEnd = dp(10) }
        }
        avatar.addView(ImageView(requireContext()).apply {
            setImageResource(R.drawable.tobby_home)
            contentDescription = null
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = FrameLayout.LayoutParams(dp(32), dp(32), Gravity.CENTER)
        })
        row.addView(avatar)

        val card = MaterialCardView(requireContext()).apply {
            radius = dp(18).toFloat()
            setCardBackgroundColor(color(R.color.argos_divider))
            cardElevation = 0f
            strokeWidth = 0
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }
        val body = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(15), dp(16), dp(15))
        }
        if (message.kind == ChatMessageKind.CONFIRMATION) {
            body.addView(textView(getString(R.string.chatbot_confirmation_heading), 11f, color(R.color.argos_text_secondary)))
            body.addView(textView(message.text, 14f, color(R.color.argos_green), bold = true).apply {
                setPadding(0, dp(10), 0, 0)
            })
            if (interactive) {
                body.addView(textView(getString(R.string.chatbot_confirmation_description), 13f, color(R.color.argos_green)).apply {
                    setPadding(0, dp(10), 0, 0)
                })
            }
        } else {
            val paragraphs = message.text.split("\n\n")
            paragraphs.forEachIndexed { index, paragraph ->
                val isPromptHeading = message.kind in setOf(ChatMessageKind.SINGLE_CHOICE, ChatMessageKind.FREE_TEXT_PROMPT) && index == 0
                body.addView(
                    textView(
                        paragraph,
                        if (isPromptHeading) 11f else 14f,
                        color(if (isPromptHeading) R.color.argos_text_secondary else R.color.argos_green),
                        bold = message.kind in setOf(ChatMessageKind.SINGLE_CHOICE, ChatMessageKind.FREE_TEXT_PROMPT) && index == 1,
                    ).apply {
                        if (index > 0) setPadding(0, dp(10), 0, 0)
                    },
                )
            }
        }

        if (message.kind == ChatMessageKind.SINGLE_CHOICE) {
            message.options.forEachIndexed { optionIndex, option ->
                val optionId = message.optionIds.getOrNull(optionIndex).orEmpty()
                val canSelect = interactive && optionId.isNotBlank()
                val isSelected = optionId.isNotBlank() && optionId == message.selectedOptionId
                val select = { viewModel.selectOption(option, optionId) }
                val optionCard = MaterialCardView(requireContext()).apply {
                    radius = dp(16).toFloat()
                    setCardBackgroundColor(color(if (isSelected) R.color.argos_green else R.color.argos_background))
                    cardElevation = 0f
                    strokeColor = color(if (isSelected) R.color.argos_green else R.color.argos_divider)
                    strokeWidth = dp(1)
                    isClickable = canSelect
                    isFocusable = canSelect
                    this.isSelected = isSelected
                    layoutParams = columnParams(top = 10)
                    if (canSelect) setOnClickListener { select() }
                }
                optionCard.addView(textView(
                    option,
                    13f,
                    color(if (isSelected) R.color.argos_background else R.color.argos_green),
                ).apply {
                    setPadding(dp(12), dp(12), dp(12), dp(12))
                    if (canSelect) {
                        isClickable = true
                        setOnClickListener { select() }
                    }
                })
                body.addView(optionCard)
            }
            if (interactive) {
                body.addView(textView(getString(R.string.chatbot_choice_expiration), 11f, color(R.color.argos_text_secondary)).apply {
                    setPadding(0, dp(12), 0, 0)
                })
            }
        }

        if (message.kind == ChatMessageKind.CONFIRMATION && interactive) {
            addPromptButton(body, getString(R.string.chatbot_confirmation_confirm), filled = true) { viewModel.confirmSend(true) }
            addPromptButton(body, getString(R.string.chatbot_confirmation_cancel), filled = false) { viewModel.confirmSend(false) }
        }

        card.addView(body)
        row.addView(card)
        binding.chatbotContent.addView(row)

        if (message.responseState == ChatResponseState.PARTIAL) {
            val partialCard = MaterialCardView(requireContext()).apply {
                radius = dp(18).toFloat()
                setCardBackgroundColor(color(R.color.argos_divider))
                cardElevation = 0f
                strokeWidth = 0
                layoutParams = columnParams(top = -4, bottom = 14)
            }
            val partialBody = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(15), dp(16), dp(15))
            }
            partialBody.addView(textView(getString(R.string.chatbot_partial_answer), 13f, color(R.color.argos_green), bold = true))
            partialBody.addView(textView(
                getString(R.string.chatbot_partial_unavailable),
                13f,
                color(R.color.argos_green),
            ).apply { setPadding(0, dp(10), 0, 0) })
            partialCard.addView(partialBody)
            binding.chatbotContent.addView(partialCard)
        }
    }

    private fun addPromptButton(
        parent: LinearLayout,
        label: String,
        filled: Boolean,
        onClick: () -> Unit,
    ) {
        val button = MaterialCardView(requireContext()).apply {
            radius = dp(18).toFloat()
            setCardBackgroundColor(color(if (filled) R.color.argos_green else R.color.argos_background))
            cardElevation = 0f
            strokeColor = color(R.color.argos_green)
            strokeWidth = dp(if (filled) 0 else 1)
            isClickable = true
            isFocusable = true
            layoutParams = columnParams(top = 10)
            setOnClickListener { onClick() }
        }
        button.addView(textView(label, 13f, color(if (filled) R.color.argos_background else R.color.argos_green), Gravity.CENTER, bold = true).apply {
            setPadding(dp(10), dp(12), dp(10), dp(12))
        })
        parent.addView(button)
    }

    private fun addActionCard(label: String, top: Int = 0, bottom: Int = 0, onClick: () -> Unit) {
        val card = MaterialCardView(requireContext()).apply {
            radius = dp(16).toFloat()
            setCardBackgroundColor(color(R.color.argos_background))
            cardElevation = 0f
            strokeColor = color(R.color.argos_divider)
            strokeWidth = dp(1)
            isClickable = true
            isFocusable = true
            layoutParams = columnParams(top = top, bottom = bottom)
            setOnClickListener { onClick() }
        }
        card.addView(textView(label, 13f, color(R.color.argos_green)).apply {
            setPadding(dp(12), dp(14), dp(12), dp(14))
        })
        binding.chatbotContent.addView(card)
    }

    private fun addSessionLabel(timestamp: Long?) {
        val label = timestamp?.let { getString(R.string.chatbot_current_session, sessionDate(it).replace(" • ", ", ")) }
            ?: getString(R.string.chatbot_new_session_label)
        addText(label, 11f, color(R.color.argos_text_secondary), top = 8)
    }

    private fun addText(
        value: CharSequence,
        size: Float,
        textColor: Int,
        gravity: Int = Gravity.START,
        bold: Boolean = false,
        top: Int = 0,
        bottom: Int = 0,
    ): TextView = textView(value, size, textColor, gravity, bold).also { text ->
        binding.chatbotContent.addView(text, columnParams(top = top, bottom = bottom))
    }

    private fun textView(
        value: CharSequence,
        size: Float,
        textColor: Int,
        gravity: Int = Gravity.START,
        bold: Boolean = false,
    ) = TextView(requireContext()).apply {
        text = value
        textSize = size
        setTextColor(textColor)
        this.gravity = gravity
        typeface = Typeface.create(resources.getFont(R.font.dm_sans), if (bold) Typeface.BOLD else Typeface.NORMAL)
        includeFontPadding = true
        setLineSpacing(0f, 1.08f)
    }

    private fun sessionDate(timestamp: Long): String {
        val date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
        val today = java.time.LocalDate.now()
        val time = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("HH:mm", locale))
        return when (date) {
            today -> getString(R.string.chatbot_session_date_today, time)
            today.minusDays(1) -> getString(R.string.chatbot_session_date_yesterday, time)
            else -> date.format(DateTimeFormatter.ofPattern("d 'de' MMMM", locale)) + " • $time"
        }
    }

    private fun columnParams(
        width: Int = ViewGroup.LayoutParams.MATCH_PARENT,
        height: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
        start: Int = 0,
        top: Int = 0,
        end: Int = 0,
        bottom: Int = 0,
    ) = LinearLayout.LayoutParams(width, height).apply {
        marginStart = dp(start)
        topMargin = dp(top)
        marginEnd = dp(end)
        bottomMargin = dp(bottom)
    }

    private fun rounded(fill: Int, radius: Float, stroke: Int? = null) = GradientDrawable().apply {
        setColor(fill)
        cornerRadius = dp(radius.toInt()).toFloat()
        if (stroke != null) setStroke(dp(1), stroke)
    }

    private fun color(resource: Int) = ContextCompat.getColor(requireContext(), resource)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
