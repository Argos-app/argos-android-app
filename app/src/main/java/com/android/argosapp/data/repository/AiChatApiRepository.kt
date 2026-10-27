package com.android.argosapp.data.repository

import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.time.Instant
import java.util.concurrent.TimeoutException
import java.util.UUID

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

private interface AiChatApi {
    @POST("v1/auth/firebase")
    suspend fun exchangeFirebaseToken(
        @Header("Authorization") authorization: String,
    ): Response<AiTokenDto>

    @POST("v1/sessions")
    suspend fun createSession(@Header("Authorization") authorization: String): Response<AiSessionDto>

    @GET("v1/sessions")
    suspend fun listSessions(@Header("Authorization") authorization: String): Response<List<AiSessionDto>>

    @GET("v1/sessions/{session_id}/messages")
    suspend fun getMessages(
        @Header("Authorization") authorization: String,
        @Path("session_id") sessionId: String,
        @Query("limit") limit: Int = 100,
        @Query("cursor") cursor: String? = null,
    ): Response<AiSessionMessagesPageDto>

    @POST("v1/sessions/{session_id}/interactions/async")
    suspend fun startInteraction(
        @Header("Authorization") authorization: String,
        @Path("session_id") sessionId: String,
        @Body input: AiInteractionInputDto,
    ): Response<AiInteractionAcceptedDto>

    @GET("v1/sessions/{session_id}/interactions/{operation_id}")
    suspend fun getInteractionOperation(
        @Header("Authorization") authorization: String,
        @Path("session_id") sessionId: String,
        @Path("operation_id") operationId: String,
    ): Response<AiInteractionOperationDto>
}

/** Cliente isolado da API de IA; o JWT próprio fica somente em memória. */
class AiChatApiRepository(
    private val firebaseRepository: FirebaseIdTokenProvider,
    baseUrl: String,
) {
    private val api = Retrofit.Builder()
        .baseUrl(baseUrl.ensureTrailingSlash())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(AiChatApi::class.java)

    private val tokenMutex = Mutex()
    private var cachedUid: String? = null
    private var cachedAccessToken: String? = null

    suspend fun createSession(): AiSessionDto = authorized { api.createSession(it).requiredBody() }

    suspend fun listSessions(): List<AiSessionDto> = authorized { api.listSessions(it).requiredBody() }

    suspend fun getMessages(sessionId: String, cursor: String? = null): AiSessionMessagesPageDto =
        authorized { api.getMessages(it, sessionId, cursor = cursor).requiredBody() }

    suspend fun interact(sessionId: String, input: AiInteractionInputDto): AiInteractionResultDto {
        val accepted = authorized { api.startInteraction(it, sessionId, input).requiredBody() }
        return withTimeoutOrNull(MAX_INTERACTION_WAIT_MS) {
            var pollDelayMs = accepted.pollAfterMs.coerceIn(MIN_POLL_DELAY_MS, MAX_POLL_DELAY_MS)
            while (true) {
                delay(pollDelayMs)
                val operation = authorized {
                    api.getInteractionOperation(it, sessionId, accepted.operationId).requiredBody()
                }
                when (operation.status) {
                    "completed", "degraded" -> return@withTimeoutOrNull requireNotNull(operation.result) {
                        "A API de IA concluiu a interação sem retornar o resultado."
                    }
                    "failed" -> {
                        operation.result?.let { return@withTimeoutOrNull it }
                        throw IllegalStateException("Não foi possível concluir a interação com a IA.")
                    }
                    "interrupted" -> throw IllegalStateException(
                        "A interação foi interrompida e precisa ser enviada novamente.",
                    )
                    "accepted", "running" -> {
                        pollDelayMs = (pollDelayMs * 2).coerceAtMost(MAX_POLL_DELAY_MS)
                    }
                    else -> throw IllegalStateException("Status desconhecido da interação com a IA.")
                }
            }
            @Suppress("UNREACHABLE_CODE")
            error("unreachable")
        } ?: throw TimeoutException("A resposta da IA demorou mais que o limite esperado.")
    }

    suspend fun clearCachedToken() = tokenMutex.withLock {
        cachedUid = null
        cachedAccessToken = null
    }

    private suspend fun <T> authorized(request: suspend (String) -> T): T {
        val uid = requireNotNull(firebaseRepository.currentFirebaseUid()) {
            "A sessão do Firebase não está ativa."
        }
        val token = accessToken(uid, forceRefresh = false)
        return try {
            requestForCurrentUser(uid, token, request)
        } catch (error: HttpException) {
            if (error.code() != 401) throw error
            val renewed = accessToken(uid, forceRefresh = true, rejectedToken = token)
            requestForCurrentUser(uid, renewed, request)
        }
    }

    private suspend fun <T> requestForCurrentUser(
        uid: String,
        token: String,
        request: suspend (String) -> T,
    ): T {
        check(firebaseRepository.currentFirebaseUid() == uid) {
            "A conta Firebase foi alterada antes da chamada da API de IA."
        }
        val result = request("Bearer $token")
        check(firebaseRepository.currentFirebaseUid() == uid) {
            "A conta Firebase foi alterada durante a chamada da API de IA."
        }
        return result
    }

    private suspend fun accessToken(
        uid: String,
        forceRefresh: Boolean,
        rejectedToken: String? = null,
    ): String = tokenMutex.withLock {
        if (cachedUid != uid) {
            cachedUid = uid
            cachedAccessToken = null
        }
        if (forceRefresh && !cachedAccessToken.isNullOrBlank() && cachedAccessToken != rejectedToken) {
            return@withLock cachedAccessToken!!
        }
        if (!forceRefresh) cachedAccessToken?.let { return@withLock it }
        if (forceRefresh) cachedAccessToken = null

        val firebaseIdToken = firebaseRepository.getCurrentIdToken(forceRefresh)
        val response = api.exchangeFirebaseToken("Bearer $firebaseIdToken").requiredBody()
        check(firebaseRepository.currentFirebaseUid() == uid) {
            "A conta Firebase foi alterada durante a autenticação da IA."
        }
        response.accessToken.also {
            cachedUid = uid
            cachedAccessToken = it
        }
    }

    private fun String.ensureTrailingSlash(): String = if (endsWith('/')) this else "$this/"

    private fun <T> Response<T>.requiredBody(): T {
        if (!isSuccessful) throw HttpException(this)
        return requireNotNull(body()) { "A API de IA retornou uma resposta vazia." }
    }

    companion object {
        private const val MIN_POLL_DELAY_MS = 250L
        private const val MAX_POLL_DELAY_MS = 5000L
        private const val MAX_INTERACTION_WAIT_MS = 6 * 60 * 1000L

        fun newClient(firebaseRepository: FirebaseRepository): AiChatApiRepository =
            AiChatApiRepository(firebaseRepository, com.android.argosapp.BuildConfig.ARGOS_AI_BASE_URL)

        fun newClientMessageId(): String = UUID.randomUUID().toString()
    }
}

fun AiSessionDto.toEpochMillis(): Long = Instant.parse(createdAt).toEpochMilli()

fun AiSessionMessagesPageDto.toMessages(userKey: String): List<com.android.argosapp.model.ChatMessage> {
    val optionLabelsByQuestion = items.mapNotNull { item ->
        val response = item.response?.takeIf { it.kind == "question" } ?: return@mapNotNull null
        val questionId = response.questionId ?: item.questionId ?: return@mapNotNull null
        questionId to response.options.orEmpty().associate { it.optionId to it.label }
    }.toMap()

    return items.map { item ->
        val response = item.response
        val inputType = response?.inputType
        val kind = when (inputType) {
            "single_choice" -> com.android.argosapp.model.ChatMessageKind.SINGLE_CHOICE
            "confirmation" -> com.android.argosapp.model.ChatMessageKind.CONFIRMATION
            "free_text" -> com.android.argosapp.model.ChatMessageKind.FREE_TEXT_PROMPT
            else -> com.android.argosapp.model.ChatMessageKind.TEXT
        }
        val isUserMessage = item.role == "user"
        val answer = item.answer
        val answerText = when {
            answer?.kind == "text" -> answer.text ?: item.content
            answer?.kind == "choice" -> answer.optionId
                ?.let { optionId -> item.questionId?.let(optionLabelsByQuestion::get)?.get(optionId) }
                ?: "Opção selecionada"
            answer?.kind == "confirmation" -> when (answer.confirm) {
                true -> "Confirmar"
                false -> "Cancelar"
                null -> item.content
            }
            else -> item.content
        }

        com.android.argosapp.model.ChatMessage(
            id = item.messageId,
            sessionId = sessionId,
            userKey = userKey,
            position = item.sequence,
            sender = if (isUserMessage) com.android.argosapp.model.ChatSender.USER else com.android.argosapp.model.ChatSender.ASSISTANT,
            text = if (isUserMessage) answerText else if (response?.kind == "question") response.prompt ?: item.content else item.content,
            kind = kind,
            options = response?.options.orEmpty().map(AiQuestionOptionDto::label),
            responseState = when (item.status) {
                "degraded" -> com.android.argosapp.model.ChatResponseState.PARTIAL
                "completed" -> com.android.argosapp.model.ChatResponseState.COMPLETED
                else -> null
            },
            createdAt = Instant.parse(item.createdAt).toEpochMilli(),
            apiInteractionId = item.interactionId,
            questionId = item.questionId ?: response?.questionId,
            inputType = inputType,
            optionIds = response?.options.orEmpty().map(AiQuestionOptionDto::optionId),
            degradationReasons = item.degradationReasons.orEmpty(),
            redacted = item.redacted,
            questionExpiresAt = response?.expiresAt?.let { Instant.parse(it).toEpochMilli() },
        )
    }
}
