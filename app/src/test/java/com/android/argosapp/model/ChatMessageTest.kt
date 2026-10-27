package com.android.argosapp.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatMessageTest {
    @Test
    fun keepsPromptActiveWhenPartialStatusFollowsIt() {
        val question = message(
            "question",
            ChatSender.ASSISTANT,
            "Para qual destinatário?",
            inputType = "free_text",
        )
        val result = listOf(
            message("request", ChatSender.USER, "Prepare o relatório"),
            question,
            message("partial", ChatSender.ASSISTANT, "Resposta parcial"),
        ).latestUnansweredPrompt()

        assertEquals(question, result)
    }

    @Test
    fun doesNotReuseQuestionAfterUserHasAnswered() {
        val result = listOf(
            message("request", ChatSender.USER, "Prepare o relatório"),
            message(
                "question",
                ChatSender.ASSISTANT,
                "Para qual destinatário?",
                inputType = "free_text",
            ),
            message("answer", ChatSender.USER, "Matheus Fazan"),
            message("partial", ChatSender.ASSISTANT, "Resposta parcial"),
        ).latestUnansweredPrompt()

        assertNull(result)
    }

    @Test
    fun expiredQuestionsDoNotBlockNewMessages() {
        for (inputType in listOf("free_text", "single_choice", "confirmation")) {
            val question = message("question", ChatSender.ASSISTANT, "Pergunta", inputType)
                .copy(questionExpiresAt = 1000)
            assertNull(listOf(question).latestUnansweredPrompt(nowMillis = 1000))
            assertEquals(question, listOf(question).latestUnansweredPrompt(nowMillis = 999))
            assertEquals(question, listOf(question).latestUnansweredPrompt(nowMillis = 1000, includeExpired = true))
        }
    }

    @Test
    fun expiredLatestQuestionDoesNotReactivateAnOlderQuestion() {
        val older = message("older", ChatSender.ASSISTANT, "Antiga", "free_text")
        val expired = message("expired", ChatSender.ASSISTANT, "Recente", "confirmation")
            .copy(questionExpiresAt = 1000)
        assertNull(listOf(older, expired).latestUnansweredPrompt(nowMillis = 1000))
    }

    @Test
    fun failedStructuredAnswerRestoresTheQuestionAndClearsSelection() {
        for (inputType in listOf("free_text", "single_choice", "confirmation")) {
            val question = message("question", ChatSender.ASSISTANT, "Pergunta", inputType)
                .copy(selectedOptionId = "option-1", questionExpiresAt = 2000)
            val answer = message("answer", ChatSender.USER, "Resposta")
            val restored = listOf(question, answer).restoreAfterFailedAnswer("answer", "question")

            assertEquals(1, restored.size)
            assertNull(restored.single().selectedOptionId)
            assertEquals("question", restored.latestUnansweredPrompt(nowMillis = 1000)?.id)
        }
    }

    @Test
    fun rollbackKeepsOtherMessagesIntact() {
        val unrelated = message("other", ChatSender.USER, "Outra mensagem")
        val question = message("question", ChatSender.ASSISTANT, "Pergunta", "free_text")
        val answer = message("answer", ChatSender.USER, "Resposta")
        assertEquals(
            listOf(unrelated, question),
            listOf(unrelated, question, answer).restoreAfterFailedAnswer("answer", "question"),
        )
    }

    private fun message(
        id: String,
        sender: ChatSender,
        text: String,
        inputType: String? = null,
    ) = ChatMessage(
        id = id,
        sessionId = "session",
        userKey = "user",
        position = 0,
        sender = sender,
        text = text,
        createdAt = 0,
        inputType = inputType,
    )
}