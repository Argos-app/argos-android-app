package com.android.argosapp.data.repository

import com.android.argosapp.data.cache.storage.ChatCacheStorage
import com.android.argosapp.data.dto.*
import com.android.argosapp.model.*
import java.time.Instant
import java.util.UUID

class MockedAiChatRepository(private val cache: ChatCacheStorage) : AiChatRepository {
    override suspend fun createSession(userKey: String): AiSessionDto {
        val now = Instant.now().toString()
        val session = AiSessionDto(UUID.randomUUID().toString(), emptyList(), now)
        cache.saveSession(session.toModel(userKey))
        return session
    }

    override suspend fun listSessions(userKey: String): List<AiSessionDto> =
        cache.sessions(mockKey(userKey)).map { it.toDto() }

    override suspend fun getMessages(userKey: String, sessionId: String, cursor: String?): AiSessionMessagesPageDto {
        val all = cache.messages(mockKey(userKey), sessionId)
        val end = cursor?.toIntOrNull()?.coerceIn(0, all.size) ?: all.size
        val start = (end - PAGE_SIZE).coerceAtLeast(0)
        val page = all.subList(start, end)
        return AiSessionMessagesPageDto(
            sessionId = sessionId,
            items = page.map { it.toDto() },
            nextCursor = start.takeIf { it > 0 }?.toString(),
            hasMore = start > 0,
        )
    }

    override suspend fun interact(
        userKey: String,
        sessionId: String,
        input: AiInteractionInputDto,
        operationId: String?,
        pollAfterMs: Long,
        onAccepted: suspend (AiInteractionAcceptedDto) -> Unit,
    ): AiInteractionResultDto {
        input.validateContract()
        val accepted = AiInteractionAcceptedDto(operationId ?: UUID.randomUUID().toString(), 0)
        onAccepted(accepted)
        val key = mockKey(userKey)
        val messages = cache.messages(key, sessionId).toMutableList()
        val now = System.currentTimeMillis()
        val interactionId = accepted.operationId
        input.clientMessageId?.let { clientId ->
            val existingIndex = messages.indexOfFirst { it.id == clientId }
            if (existingIndex >= 0) {
                val previousAssistant = messages.drop(existingIndex + 1).firstOrNull { it.sender == ChatSender.ASSISTANT }
                if (previousAssistant != null) return AiInteractionResultDto(
                    schemaVersion = "1", status = if (previousAssistant.responseState == ChatResponseState.PARTIAL) "degraded" else "completed",
                    sessionId = sessionId, interactionId = previousAssistant.apiInteractionId ?: interactionId,
                    publicRequestId = previousAssistant.apiInteractionId ?: interactionId, response = previousAssistant.toResponse(),
                )
            }
        }
        val userText = input.message ?: input.answer?.let {
            when (it.kind) {
                "choice" -> "Opção selecionada"
                "confirmation" -> if (it.confirm == true) "Confirmar" else "Não enviar"
                else -> it.text.orEmpty()
            }
        }.orEmpty()
        messages += ChatMessage(
            id = input.clientMessageId ?: UUID.randomUUID().toString(), sessionId = sessionId,
            userKey = key, position = (messages.lastOrNull()?.position ?: 0) + 1,
            sender = ChatSender.USER, text = userText, createdAt = now, apiInteractionId = interactionId,
            questionId = input.questionId,
        )
        val response = responseFor(input)
        val kind = when (response.inputType) {
            "single_choice" -> ChatMessageKind.SINGLE_CHOICE
            "confirmation" -> ChatMessageKind.CONFIRMATION
            "free_text" -> ChatMessageKind.FREE_TEXT_PROMPT
            else -> ChatMessageKind.TEXT
        }
        val assistant = ChatMessage(
            id = UUID.randomUUID().toString(), sessionId = sessionId, userKey = key,
            position = messages.last().position + 1, sender = ChatSender.ASSISTANT,
            text = response.text ?: response.prompt.orEmpty(), kind = kind,
            options = response.options.orEmpty().map { it.label }, responseState =
                if (response.reasonCode == "partial") ChatResponseState.PARTIAL else ChatResponseState.COMPLETED,
            createdAt = now + 1, apiInteractionId = interactionId, questionId = response.questionId,
            inputType = response.inputType, optionIds = response.options.orEmpty().map { it.optionId },
            questionExpiresAt = response.expiresAt?.let { Instant.parse(it).toEpochMilli() },
            redacted = "redigida" in input.message.orEmpty().lowercase(),
        )
        messages += assistant
        cache.savePage(key, sessionId, messages, null, false, replace = true)
        val created = Instant.ofEpochMilli(now + 1).toString()
        return AiInteractionResultDto(
            schemaVersion = "1", status = if (response.reasonCode == "partial") "degraded" else "completed",
            degradationReasons = if (response.reasonCode == "partial") listOf("mock_partial") else emptyList(),
            createdAt = created, sessionId = sessionId, interactionId = interactionId,
            publicRequestId = interactionId, response = response,
        )
    }

    override suspend fun clearCachedToken() = Unit

    private fun responseFor(input: AiInteractionInputDto): AiResponseDto {
        if (input.kind == "answer") return AiResponseDto(kind = "answer", text = when (input.answer?.kind) {
            "choice" -> "Resumo disponível para a fazenda selecionada."
            "confirmation" -> if (input.answer.confirm == true) "Relatório enviado aos responsáveis." else "O envio foi cancelado."
            else -> "Consulta recebida: ${input.answer?.text.orEmpty()}"
        })
        val text = input.message.orEmpty()
        return when {
            "redigida" in text.lowercase() -> AiResponseDto(kind = "answer", text = "Conteúdo ocultado para proteger informações sensíveis.", reasonCode = "redacted")
            "expirada" in text.lowercase() -> AiResponseDto(kind = "question", questionId = UUID.randomUUID().toString(), inputType = "free_text", prompt = "Esta pergunta de demonstração já expirou.", expiresAt = Instant.now().minusSeconds(60L).toString())
            "parcial" in text.lowercase() -> AiResponseDto(kind = "answer", text = "O lote possui 24 animais cadastrados. A previsão está indisponível.", reasonCode = "partial")
            "fazenda" in text.lowercase() || "resumo" in text.lowercase() -> AiResponseDto(
                kind = "question", questionId = UUID.randomUUID().toString(), inputType = "single_choice",
                prompt = "De qual fazenda você quer o resumo?", options = listOf("Fazenda São Bento", "Fazenda Santa Clara", "Fazenda Boa Vista", "Fazenda São José").mapIndexed { i, label -> AiQuestionOptionDto("farm-${i + 1}", label) },
                expiresAt = Instant.now().plusSeconds(900L).toString(),
            )
            "lote" in text.lowercase() -> AiResponseDto(kind = "question", questionId = UUID.randomUUID().toString(), inputType = "free_text", prompt = "Qual lote você quer consultar? Digite o nome ou código do lote.", expiresAt = Instant.now().plusSeconds(900L).toString())
            "relatório" in text.lowercase() || "relatorio" in text.lowercase() -> AiResponseDto(kind = "question", questionId = UUID.randomUUID().toString(), inputType = "confirmation", prompt = "Enviar o relatório por e-mail aos responsáveis autorizados?", expiresAt = Instant.now().plusSeconds(900L).toString())
            else -> AiResponseDto(kind = "answer", text = "Olá! Posso ajudar a consultar seu rebanho, lotes e medicamentos.")
        }
    }

    private fun mockKey(userKey: String) = "mock-data:${userKey.removePrefix("mock:")}"

    private fun AiSessionDto.toModel(userKey: String) = ChatSession(sessionId, mockKey(userKey), Instant.parse(createdAt).toEpochMilli(), Instant.parse(createdAt).toEpochMilli())
    private fun ChatSession.toDto() = AiSessionDto(id, emptyList(), Instant.ofEpochMilli(createdAt).toString())
    private fun ChatMessage.toDto() = AiSessionMessageDto(
        messageId = id, interactionId = apiInteractionId.orEmpty(), sequence = position,
        role = if (sender == ChatSender.USER) "user" else "assistant",
        kind = if (inputType == null) "message" else "question", content = text,
        createdAt = Instant.ofEpochMilli(createdAt).toString(), questionId = questionId,
        response = when {
            inputType != null -> AiResponseDto(kind = "question", questionId = questionId, inputType = inputType, prompt = text, options = options.mapIndexed { i, label -> AiQuestionOptionDto(optionIds.getOrNull(i).orEmpty(), label) }, expiresAt = questionExpiresAt?.let { Instant.ofEpochMilli(it).toString() })
            else -> null
        }, redacted = redacted,
        status = if (responseState == ChatResponseState.PARTIAL) "degraded" else "completed",
        degradationReasons = degradationReasons,
    )

    private fun ChatMessage.toResponse() = AiResponseDto(
        kind = if (inputType != null) "question" else "answer",
        text = text.takeIf { inputType == null }, questionId = questionId,
        inputType = inputType, prompt = text.takeIf { inputType != null },
        options = options.mapIndexed { i, label -> AiQuestionOptionDto(optionIds.getOrNull(i).orEmpty(), label) },
        expiresAt = questionExpiresAt?.let { Instant.ofEpochMilli(it).toString() },
    )

    private companion object { const val PAGE_SIZE = 100 }
}
