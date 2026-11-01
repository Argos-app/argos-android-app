package com.android.argosapp.data.repository

import com.android.argosapp.data.dto.*
import com.android.argosapp.data.cache.storage.AiTokenStorage
import com.android.argosapp.util.logging.RepositoryAppLogger
import org.json.JSONObject
import org.json.JSONArray
import org.json.JSONTokener
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


class AiChatApiRepository(
    private val firebaseRepository: FirebaseIdTokenProvider,
    baseUrl: String,
    private val tokenStorage: AiTokenStorage,
) {
    private val api = Retrofit.Builder()
        .baseUrl(baseUrl.ensureTrailingSlash())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(AiChatApi::class.java)

    private val logger = RepositoryAppLogger("AiChatApiRepository")
    private val tokenMutex = Mutex()

    suspend fun createSession(): AiSessionDto = logger.runSuspending(
        operation = "ai_create_session",
        startFields = arrayOf("method" to "POST", "endpoint" to "v1/sessions"),
        failureFields = ::httpFailureFields,
    ) { authorized { api.createSession(it).requiredBody() } }

    suspend fun listSessions(): List<AiSessionDto> = logger.runSuspending(
        operation = "ai_list_sessions",
        startFields = arrayOf("method" to "GET", "endpoint" to "v1/sessions"),
        successFields = { arrayOf("count" to it.size) },
        failureFields = ::httpFailureFields,
    ) { authorized { api.listSessions(it).requiredBody() } }

    suspend fun getMessages(sessionId: String, cursor: String? = null): AiSessionMessagesPageDto =
        logger.runSuspending(
            operation = "ai_get_messages",
            startFields = arrayOf("method" to "GET", "endpoint" to "v1/sessions/{session_id}/messages", "has_cursor" to (cursor != null)),
            successFields = { arrayOf("count" to it.items.size, "has_more" to it.hasMore) },
            failureFields = ::httpFailureFields,
        ) { authorized { api.getMessages(it, sessionId, cursor = cursor).requiredBody() } }

    suspend fun interact(sessionId: String, input: AiInteractionInputDto): AiInteractionResultDto = logger.runSuspending(
        operation = "ai_interact",
        startFields = arrayOf("method" to "POST", "endpoint" to "v1/sessions/{session_id}/interactions/async", "kind" to input.kind),
        successFields = { arrayOf("status" to it.status) },
        failureFields = ::httpFailureFields,
    ) {
        val accepted = authorized { api.startInteraction(it, sessionId, input).requiredBody() }
        withTimeoutOrNull(MAX_INTERACTION_WAIT_MS) {
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
        tokenStorage.clearAiToken()
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
            logger.info("ai_token_rejected", *httpFailureFields(error))
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
        val cached = tokenStorage.getAiToken(uid)
        if (forceRefresh && cached != null && cached != rejectedToken) return@withLock cached
        if (!forceRefresh && cached != null) return@withLock cached
        if (forceRefresh) tokenStorage.clearAiToken()

        val firebaseIdToken = firebaseRepository.getCurrentIdToken(forceRefresh)
        val response = logger.runSuspending(
            operation = "ai_authenticate",
            startFields = arrayOf("method" to "POST", "endpoint" to "v1/auth/firebase", "force_refresh" to forceRefresh),
            failureFields = ::httpFailureFields,
        ) { api.exchangeFirebaseToken("Bearer $firebaseIdToken").requiredBody() }
        check(firebaseRepository.currentFirebaseUid() == uid) {
            "A conta Firebase foi alterada durante a autenticação da IA."
        }
        response.accessToken.also {
            tokenStorage.saveAiToken(uid, it)
        }
    }

    private fun String.ensureTrailingSlash(): String = if (endsWith('/')) this else "$this/"

    private fun httpFailureFields(error: Throwable): Array<out Pair<String, Any?>> {
        val response = (error as? HttpException)?.response()
        val body = runCatching { response?.errorBody()?.source()?.peek()?.readUtf8() }.getOrNull()
        return arrayOf(
            "http_status" to response?.code(),
            "response_message" to (body?.let(::parseAiErrorMessage) ?: response?.message()),
            "error_type" to error.javaClass.simpleName,
        )
    }

    private fun <T> Response<T>.requiredBody(): T {
        if (!isSuccessful) throw HttpException(this)
        return requireNotNull(body()) { "A API de IA retornou uma resposta vazia." }
    }

    companion object {
        private const val MIN_POLL_DELAY_MS = 250L
        private const val MAX_POLL_DELAY_MS = 5000L
        private const val MAX_INTERACTION_WAIT_MS = 6 * 60 * 1000L

        fun newClient(firebaseRepository: FirebaseRepository, tokenStorage: TokenStorage): AiChatApiRepository =
            AiChatApiRepository(firebaseRepository, com.android.argosapp.BuildConfig.ARGOS_AI_BASE_URL, tokenStorage)

        fun newClientMessageId(): String = UUID.randomUUID().toString()
    }
}

internal fun parseAiErrorMessage(body: String): String? {
    fun findMessage(value: Any?): String? = when (value) {
        is JSONObject -> listOf("message", "detail", "error", "error_description", "title", "description", "msg")
            .firstNotNullOfOrNull { key -> findMessage(value.opt(key)) }
        is JSONArray -> (0 until value.length()).firstNotNullOfOrNull { findMessage(value.opt(it)) }
        is String -> value.trim().takeIf(String::isNotEmpty)
        else -> null
    }
    return runCatching { findMessage(JSONTokener(body).nextValue()) }
        .getOrNull()?.replace(Regex("[\\r\\n\\t]+"), " ")?.take(500)
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
