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
    val selectedOptionId: String? = null,
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

/** Returns the latest unanswered structured prompt, ignoring status cards after it. */
fun List<ChatMessage>.latestUnansweredPrompt(
    nowMillis: Long = System.currentTimeMillis(),
    includeExpired: Boolean = false,
): ChatMessage? {
    val lastUserMessage = indexOfLast { it.sender == ChatSender.USER }
    return drop(lastUserMessage + 1).lastOrNull {
        it.sender == ChatSender.ASSISTANT &&
            it.inputType in setOf("free_text", "single_choice", "confirmation")
    }?.takeIf { includeExpired || it.questionExpiresAt == null || it.questionExpiresAt > nowMillis }
}

/** Removes the optimistic answer and restores the question after a failed request. */
fun List<ChatMessage>.restoreAfterFailedAnswer(answerId: String, questionMessageId: String): List<ChatMessage> =
    filterNot { it.id == answerId }.map { message ->
        if (message.id == questionMessageId) message.copy(selectedOptionId = null) else message
    }
