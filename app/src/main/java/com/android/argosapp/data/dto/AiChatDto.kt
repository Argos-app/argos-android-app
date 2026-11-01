package com.android.argosapp.data.dto

import com.google.gson.annotations.SerializedName

data class AiSessionDto(
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("farm_ids") val farmIds: List<String>,
    @SerializedName("created_at") val createdAt: String,
)

data class AiQuestionOptionDto(
    @SerializedName("option_id") val optionId: String,
    val label: String,
)

data class AiResponseDto(
    val kind: String,
    val text: String? = null,
    @SerializedName("question_id") val questionId: String? = null,
    @SerializedName("input_type") val inputType: String? = null,
    val prompt: String? = null,
    @SerializedName("reason_code") val reasonCode: String? = null,
    val options: List<AiQuestionOptionDto>? = null,
    @SerializedName("expires_at") val expiresAt: String? = null,
)

data class AiInteractionResultDto(
    @SerializedName("schema_version") val schemaVersion: String,
    val status: String,
    @SerializedName("degradation_reasons") val degradationReasons: List<String>? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("interaction_id") val interactionId: String,
    @SerializedName("public_request_id") val publicRequestId: String,
    val response: AiResponseDto,
)

data class AiAnswerDto(
    val kind: String,
    val text: String? = null,
    @SerializedName("option_id") val optionId: String? = null,
    val confirm: Boolean? = null,
)

data class AiSessionMessageDto(
    @SerializedName("message_id") val messageId: String,
    @SerializedName("interaction_id") val interactionId: String,
    val sequence: Int,
    val role: String,
    val kind: String,
    val content: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("question_id") val questionId: String? = null,
    val answer: AiAnswerDto? = null,
    val response: AiResponseDto? = null,
    val redacted: Boolean = false,
    val status: String? = null,
    @SerializedName("degradation_reasons") val degradationReasons: List<String>? = null,
)

data class AiSessionMessagesPageDto(
    @SerializedName("session_id") val sessionId: String,
    val items: List<AiSessionMessageDto>,
    @SerializedName("next_cursor") val nextCursor: String? = null,
    @SerializedName("has_more") val hasMore: Boolean,
)

data class AiTokenDto(@SerializedName("access_token") val accessToken: String)

data class AiInteractionAcceptedDto(
    @SerializedName("operation_id") val operationId: String,
    @SerializedName("poll_after_ms") val pollAfterMs: Long = 1000,
)

data class AiInteractionOperationDto(
    @SerializedName("operation_id") val operationId: String,
    val status: String,
    val result: AiInteractionResultDto? = null,
)

data class AiAnswerInputDto(
    val kind: String,
    val text: String? = null,
    @SerializedName("option_id") val optionId: String? = null,
    val confirm: Boolean? = null,
)

data class AiInteractionInputDto(
    val kind: String,
    @SerializedName("client_message_id") val clientMessageId: String? = null,
    val message: String? = null,
    @SerializedName("interaction_id") val interactionId: String? = null,
    @SerializedName("question_id") val questionId: String? = null,
    val answer: AiAnswerInputDto? = null,
)

