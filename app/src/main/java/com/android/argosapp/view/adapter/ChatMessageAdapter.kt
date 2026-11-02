package com.android.argosapp.view.adapter

import android.graphics.Typeface
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.argosapp.R
import com.android.argosapp.databinding.ItemChatAssistantBinding
import com.android.argosapp.databinding.ItemChatOptionBinding
import com.android.argosapp.databinding.ItemChatUserBinding
import com.android.argosapp.model.ChatMessage
import com.android.argosapp.model.ChatMessageKind
import com.android.argosapp.model.ChatResponseState
import com.android.argosapp.model.ChatSender

class ChatMessageAdapter(
    private val onOption: (String, String) -> Unit,
    private val onConfirm: (Boolean) -> Unit,
) : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(DIFF) {
    var activePromptId: String? = null
        set(value) {
            if (field == value) return
            val old = currentList.indexOfFirst { it.id == field }
            field = value
            if (old >= 0) notifyItemChanged(old)
            val next = currentList.indexOfFirst { it.id == value }
            if (next >= 0) notifyItemChanged(next)
        }

    override fun getItemViewType(position: Int) = if (getItem(position).sender == ChatSender.USER) 0 else 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == 0) {
            val binding = ItemChatUserBinding.inflate(inflater, parent, false)
            val card = binding.messageCard
            card.shapeAppearanceModel = card.shapeAppearanceModel.toBuilder().setBottomRightCornerSize(0f).build()
            UserHolder(binding)
        } else {
            val binding = ItemChatAssistantBinding.inflate(inflater, parent, false)
            val card = binding.messageCard
            card.shapeAppearanceModel = card.shapeAppearanceModel.toBuilder().setBottomLeftCornerSize(0f).build()
            AssistantHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is UserHolder -> with(holder.binding) {
                messageText.isVisible = !message.redacted
                messageRedacted.isVisible = message.redacted
                messageText.text = message.text
            }
            is AssistantHolder -> holder.bind(message, message.id == activePromptId)
        }
    }

    private class UserHolder(val binding: ItemChatUserBinding) : RecyclerView.ViewHolder(binding.root)

    private inner class AssistantHolder(private val binding: ItemChatAssistantBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage, interactive: Boolean) = with(binding) {
            val context = root.context
            val isConfirmation = message.kind == ChatMessageKind.CONFIRMATION
            val isPrompt = message.kind == ChatMessageKind.SINGLE_CHOICE || message.kind == ChatMessageKind.FREE_TEXT_PROMPT
            messageHeading.isVisible = isConfirmation || isPrompt
            messageHeading.text = context.getString(
                if (isConfirmation) R.string.chatbot_confirmation_heading else R.string.chatbot_question_heading,
            )
            messageText.isVisible = !message.redacted
            messageRedacted.isVisible = message.redacted
            messageText.text = message.text
            messageText.typeface = Typeface.create(context.resources.getFont(R.font.dm_sans), if (isConfirmation || isPrompt) Typeface.BOLD else Typeface.NORMAL)
            messageDescription.isVisible = interactive && (isConfirmation || message.kind == ChatMessageKind.SINGLE_CHOICE)
            messageDescription.setText(if (isConfirmation) R.string.chatbot_confirmation_description else R.string.chatbot_choice_description)
            val expired = message.questionExpiresAt?.let { it <= System.currentTimeMillis() } == true
            messageExpiration.isVisible = expired || message.kind == ChatMessageKind.SINGLE_CHOICE && interactive
            messageExpiration.setText(if (expired) R.string.chatbot_question_expired else R.string.chatbot_choice_expiration)
            messagePartial.isVisible = message.responseState == ChatResponseState.PARTIAL
            messageOptions.removeAllViews()
            fun addOption(label: String, selected: Boolean, enabled: Boolean, centered: Boolean = false, click: () -> Unit) {
                val option = ItemChatOptionBinding.inflate(LayoutInflater.from(context), messageOptions, false)
                option.optionText.text = label
                option.optionText.gravity = if (centered) Gravity.CENTER else Gravity.START
                option.root.setCardBackgroundColor(ContextCompat.getColor(context, if (selected) R.color.argos_green else R.color.argos_eye_slash_background))
                option.optionText.setTextColor(ContextCompat.getColor(context, if (selected) R.color.argos_background else R.color.argos_green))
                option.root.isClickable = enabled
                option.root.isFocusable = enabled
                option.root.isSelected = selected
                option.root.setOnClickListener(if (enabled) android.view.View.OnClickListener { click() } else null)
                messageOptions.addView(option.root)
            }
            if (!message.redacted && message.kind == ChatMessageKind.SINGLE_CHOICE) {
                message.options.forEachIndexed { index, label ->
                    val id = message.optionIds.getOrNull(index).orEmpty()
                    addOption(label, id.isNotBlank() && id == message.selectedOptionId, interactive && !expired && id.isNotBlank()) { onOption(label, id) }
                }
            }
            if (!message.redacted && isConfirmation && interactive && !expired) {
                addOption(context.getString(R.string.chatbot_confirmation_confirm), true, true, true) { onConfirm(true) }
                addOption(context.getString(R.string.chatbot_confirmation_cancel), false, true, true) { onConfirm(false) }
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ChatMessage>() {
            override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage) = oldItem == newItem
        }
    }
}
