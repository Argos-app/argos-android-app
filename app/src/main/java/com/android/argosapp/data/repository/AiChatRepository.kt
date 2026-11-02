package com.android.argosapp.data.repository

import com.android.argosapp.data.dto.AiInteractionAcceptedDto
import com.android.argosapp.data.dto.AiInteractionInputDto
import com.android.argosapp.data.dto.AiInteractionResultDto
import com.android.argosapp.data.dto.AiSessionDto
import com.android.argosapp.data.dto.AiSessionMessagesPageDto
import java.util.UUID

interface AiChatRepository {
    suspend fun createSession(userKey: String): AiSessionDto
    suspend fun listSessions(userKey: String): List<AiSessionDto>
    suspend fun getMessages(userKey: String, sessionId: String, cursor: String? = null): AiSessionMessagesPageDto
    suspend fun interact(
        userKey: String,
        sessionId: String,
        input: AiInteractionInputDto,
        operationId: String? = null,
        pollAfterMs: Long = 1000,
        onAccepted: suspend (AiInteractionAcceptedDto) -> Unit = {},
    ): AiInteractionResultDto
    suspend fun clearCachedToken()
}

interface FirebaseIdTokenProvider {
    fun currentFirebaseUid(): String?
    suspend fun getCurrentIdToken(forceRefresh: Boolean = false): String
}

internal fun AiInteractionInputDto.validateContract() {
    when (kind) {
        "message" -> {
            require(message != null && message.length <= MAX_TEXT_LENGTH) { "A mensagem deve ter até 8.000 caracteres." }
            val messageId = requireNotNull(clientMessageId?.takeIf(String::isNotBlank)) { "A mensagem exige client_message_id." }
            UUID.fromString(messageId)
        }
        "answer" -> {
            require(!interactionId.isNullOrBlank() && !questionId.isNullOrBlank()) { "A resposta exige os identificadores da pergunta." }
            val value = requireNotNull(answer) { "A resposta estruturada é obrigatória." }
            when (value.kind) {
                "text" -> require(value.text != null && value.text.length <= MAX_TEXT_LENGTH) { "A resposta deve ter até 8.000 caracteres." }
                "choice" -> require(!value.optionId.isNullOrBlank()) { "A opção selecionada é obrigatória." }
                "confirmation" -> require(value.confirm != null) { "A confirmação é obrigatória." }
                else -> error("Tipo de resposta não suportado.")
            }
        }
        else -> error("Tipo de interação não suportado.")
    }
}

private const val MAX_TEXT_LENGTH = 8000
