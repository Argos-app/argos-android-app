package com.android.argosapp.model

enum class ChatSender { USER, ASSISTANT }

enum class ChatMessageKind {
    TEXT,
    SINGLE_CHOICE,
    CONFIRMATION,
    FREE_TEXT_PROMPT,
}

enum class ChatResponseState { PARTIAL, COMPLETED }

data class ChatMessage(
    val id: String,
    val sessionId: String,
    val userKey: String,
    val position: Int,
    val sender: ChatSender,
    val text: String,
    val kind: ChatMessageKind = ChatMessageKind.TEXT,
    val options: List<String> = emptyList(),
    val responseState: ChatResponseState? = null,
    val createdAt: Long,
    val apiInteractionId: String? = null,
    val questionId: String? = null,
    val inputType: String? = null,
    val optionIds: List<String> = emptyList(),
    val degradationReasons: List<String> = emptyList(),
    val redacted: Boolean = false,
    val questionExpiresAt: Long? = null,
)

data class ChatRequest(
    val text: String,
    val previousPrompt: ChatMessageKind? = null,
    val confirmed: Boolean? = null,
)

sealed interface ChatReply {
    data class Prompt(
        val text: String,
        val kind: ChatMessageKind,
        val options: List<String> = emptyList(),
    ) : ChatReply

    data class Answer(val text: String, val state: ChatResponseState) : ChatReply
}
