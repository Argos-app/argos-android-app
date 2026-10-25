package com.android.argosapp.data.repository

import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class AiChatApiRepositoryTest {
    @Test
    fun exchangesFirebaseTokenAndUsesAiJwtForSessionCalls() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(jsonResponse("""{"access_token":"ai-jwt"}"""))
            server.enqueue(jsonResponse("""{"session_id":"session-1","farm_ids":["farm-1"],"created_at":"2026-10-24T12:00:00Z"}""", 201))
            val firebase = FakeFirebaseTokens()
            val repository = AiChatApiRepository(firebase, server.url("/").toString())

            val session = repository.createSession()

            assertEquals("session-1", session.sessionId)
            val authRequest = server.takeRequest()
            assertEquals("/v1/auth/firebase", authRequest.path)
            assertEquals("Bearer firebase-id-token", authRequest.getHeader("Authorization"))
            val createSession = server.takeRequest()
            assertEquals("/v1/sessions", createSession.path)
            assertEquals("Bearer ai-jwt", createSession.getHeader("Authorization"))
            assertEquals(listOf(false), firebase.refreshRequests)
        }
    }

    @Test
    fun renewsOnceAfterProtectedRouteReturnsUnauthorized() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(jsonResponse("""{"access_token":"old-ai-jwt"}"""))
            server.enqueue(jsonResponse("{}", 401))
            server.enqueue(jsonResponse("""{"access_token":"new-ai-jwt"}"""))
            server.enqueue(jsonResponse("""{"session_id":"session-1","farm_ids":[],"created_at":"2026-10-24T12:00:00Z"}""", 201))
            val firebase = FakeFirebaseTokens()
            val repository = AiChatApiRepository(firebase, server.url("/").toString())

            repository.createSession()

            assertEquals(listOf(false, true), firebase.refreshRequests)
            val firstExchange = server.takeRequest()
            assertEquals("/v1/auth/firebase", firstExchange.path)
            val rejectedCall = server.takeRequest()
            assertEquals("Bearer old-ai-jwt", rejectedCall.getHeader("Authorization"))
            val secondExchange = server.takeRequest()
            assertEquals("/v1/auth/firebase", secondExchange.path)
            assertEquals("Bearer refreshed-firebase-id-token", secondExchange.getHeader("Authorization"))
            val retriedCall = server.takeRequest()
            assertEquals("Bearer new-ai-jwt", retriedCall.getHeader("Authorization"))
            assertEquals(5, server.requestCount)
        }
    }

    @Test
    fun answerRequestUsesStructuredApiFields() {
        val json = Gson().toJson(
            AiInteractionInputDto(
                kind = "answer",
                interactionId = "interaction-1",
                questionId = "question-1",
                answer = AiAnswerInputDto(kind = "choice", optionId = "lot-1"),
            ),
        )

        assertEquals("answer", Gson().fromJson(json, Map::class.java)["kind"])
        assertEquals("interaction-1", Gson().fromJson(json, Map::class.java)["interaction_id"])
        assertEquals("question-1", Gson().fromJson(json, Map::class.java)["question_id"])
        assertFalse(json.contains("client_message_id"))
        assertFalse(json.contains("farm_ids"))
    }

    @Test
    fun doesNotReuseAccessTokenAfterFirebaseUserChangesOrSessionIsCleared() = runBlocking {
        MockWebServer().use { server ->
            repeat(3) { index ->
                server.enqueue(jsonResponse("""{"access_token":"ai-jwt-$index"}"""))
                server.enqueue(jsonResponse("""{"session_id":"session-$index","farm_ids":[],"created_at":"2026-10-24T12:00:00Z"}""", 201))
            }
            val firebase = FakeFirebaseTokens()
            val repository = AiChatApiRepository(firebase, server.url("/").toString())

            repository.createSession()
            firebase.uid = "firebase-user-2"
            repository.createSession()
            repository.clearCachedToken()
            repository.createSession()

            assertEquals(3, firebase.refreshRequests.size)
            assertEquals(6, server.requestCount)
        }
    }

    @Test
    fun mapsHistoryQuestionIdsOptionsAndDegradationStatus() {
        val message = AiSessionMessagesPageDto(
            sessionId = "session-1",
            items = listOf(
                AiSessionMessageDto(
                    messageId = "message-1",
                    interactionId = "interaction-1",
                    sequence = 3,
                    role = "assistant",
                    kind = "clarification_question",
                    content = "Escolha o lote",
                    createdAt = "2026-10-24T12:00:00Z",
                    questionId = "question-1",
                    response = AiResponseDto(
                        kind = "question",
                        questionId = "question-1",
                        inputType = "single_choice",
                        prompt = "Escolha o lote",
                        options = listOf(AiQuestionOptionDto("lot-1", "Lote 1")),
                        expiresAt = "2026-10-24T12:05:00Z",
                    ),
                    status = "degraded",
                    degradationReasons = listOf("prediction_failed"),
                ),
            ),
            hasMore = false,
        ).toMessages("firebase-uid")

        assertEquals("Lote 1", message.single().options.single())
        assertEquals("lot-1", message.single().optionIds.single())
        assertEquals("question-1", message.single().questionId)
        assertEquals("interaction-1", message.single().apiInteractionId)
        assertEquals("single_choice", message.single().inputType)
        assertNotNull(message.single().questionExpiresAt)
        assertEquals(listOf("prediction_failed"), message.single().degradationReasons)
    }

    private fun jsonResponse(body: String, status: Int = 200) = MockResponse()
        .setResponseCode(status)
        .addHeader("Content-Type", "application/json")
        .setBody(body)

    private class FakeFirebaseTokens : FirebaseIdTokenProvider {
        val refreshRequests = mutableListOf<Boolean>()
        var uid = "firebase-uid"
        override fun currentFirebaseUid(): String = uid
        override suspend fun getCurrentIdToken(forceRefresh: Boolean): String {
            refreshRequests += forceRefresh
            return if (forceRefresh) "refreshed-firebase-id-token" else "firebase-id-token"
        }
    }
}
