package com.android.argosapp.data.repository

import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.util.concurrent.TimeoutException

class PendingInteractionTest {
    private class MemoryStore : PendingInteractionStore {
        val entries = mutableMapOf<String, PendingInteraction>()
        override suspend fun get(userKey: String) = entries[userKey]
        override suspend fun save(pending: PendingInteraction) { entries[pending.userKey] = pending }
        override suspend fun clear(userKey: String) { entries.remove(userKey) }
    }

    private val pending = PendingInteraction(
        "user-1", "session-1", AiInteractionInputDto("message", "message-1", "Olá"),
    )

    private fun result() = AiInteractionResultDto(
        "1", "completed", sessionId = "session-1", interactionId = "op-1",
        publicRequestId = "request-1", response = AiResponseDto("answer", text = "OK"),
    )

    @Test
    fun savesOperationBeforePollingAndResumesAfterAppClosure() = runBlocking {
        val store = MemoryStore().apply { save(pending) }
        val first = PendingInteractionRunner(store) { _, accepted ->
            accepted(AiInteractionAcceptedDto("op-1", 250))
            throw CancellationException("app closed")
        }
        try { first.run(pending) } catch (_: CancellationException) { }
        assertEquals("op-1", store.get("user-1")?.operationId)
        val reopened = PendingInteractionRunner(store) { restored, _ ->
            assertEquals("op-1", restored.operationId)
            assertEquals(250L, restored.pollAfterMs)
            result()
        }
        assertEquals("OK", reopened.run(store.get("user-1")!!).response.text)
        assertNull(store.get("other-user"))
    }

    @Test
    fun retriesLostAcceptanceWithIdenticalMessageAndStructuredAnswer() = runBlocking {
        val inputs = listOf(
            pending.input,
            AiInteractionInputDto("answer", interactionId = "interaction-1", questionId = "question-1",
                answer = AiAnswerInputDto("confirmation", confirm = true)),
            AiInteractionInputDto("answer", interactionId = "interaction-1", questionId = "question-1",
                answer = AiAnswerInputDto("choice", optionId = "option-1")),
            AiInteractionInputDto("answer", interactionId = "interaction-1", questionId = "question-1",
                answer = AiAnswerInputDto("text", text = "resposta")),
        )
        for (input in inputs) {
            val store = MemoryStore().apply { save(pending.copy(input = input)) }
            val initialJson = Gson().toJson(store.get("user-1")!!.input)
            val lostResponse = PendingInteractionRunner(store) { _, _ -> throw IOException("connection lost") }
            try { lostResponse.run(store.get("user-1")!!) } catch (_: IOException) { }
            assertNull(store.get("user-1")!!.operationId)
            val retry = PendingInteractionRunner(store) { restored, accepted ->
                assertEquals(initialJson, Gson().toJson(restored.input))
                accepted(AiInteractionAcceptedDto("op-1"))
                result()
            }
            retry.run(store.get("user-1")!!)
            assertEquals("op-1", store.get("user-1")!!.operationId)
        }
    }

    @Test
    fun timeoutAndConnectionLossKeepAcceptedOperationForGet() = runBlocking {
        for (failure in listOf(TimeoutException(), IOException())) {
            val store = MemoryStore().apply { save(pending) }
            val runner = PendingInteractionRunner(store) { _, accepted ->
                accepted(AiInteractionAcceptedDto("op-1"))
                throw failure
            }
            try { runner.run(pending) } catch (_: Exception) { }
            assertEquals("op-1", store.get("user-1")!!.operationId)
        }
    }

    @Test
    fun conflictStopsRecoveryAndRecordsInterruption() = runBlocking {
        val store = MemoryStore().apply { save(pending) }
        var calls = 0
        val runner = PendingInteractionRunner(store) { _, _ ->
            calls++
            throw HttpException(Response.error<Any>(409, "{}".toResponseBody("application/json".toMediaType())))
        }
        assertThrows(InterruptedInteractionException::class.java) { runBlocking { runner.run(pending) } }
        assertEquals(1, calls)
        assertEquals(true, store.get("user-1")!!.interrupted)
        assertThrows(InterruptedInteractionException::class.java) { runBlocking { runner.run(store.get("user-1")!!) } }
        assertEquals(1, calls)
    }

    @Test
    fun interruptedOperationStopsRecovery() = runBlocking {
        val store = MemoryStore().apply { save(pending.copy(operationId = "op-1")) }
        val runner = PendingInteractionRunner(store) { _, _ -> throw InterruptedInteractionException() }
        assertThrows(InterruptedInteractionException::class.java) { runBlocking { runner.run(store.get("user-1")!!) } }
        assertEquals(true, store.get("user-1")!!.interrupted)
    }

    @Test
    fun acknowledgedInterruptionCannotBeResubmitted() = runBlocking {
        val acknowledged = pending.copy(operationId = "op-1", interrupted = true, rejectionCode = -1)
        val store = MemoryStore().apply { save(acknowledged) }
        var calls = 0
        val runner = PendingInteractionRunner(store) { _, _ -> calls++; result() }

        assertThrows(InterruptedInteractionException::class.java) { runBlocking { runner.run(acknowledged) } }
        assertEquals(0, calls)
        assertEquals(acknowledged, store.get("user-1"))
    }
}
