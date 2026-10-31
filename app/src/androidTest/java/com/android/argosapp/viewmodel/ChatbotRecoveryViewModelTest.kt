package com.android.argosapp.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.argosapp.data.repository.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ChatbotRecoveryViewModelTest {
    private class MemoryStore(initial: PendingInteraction? = null) : PendingInteractionStore {
        @Volatile var pending: PendingInteraction? = initial
        @Volatile var failNextSave = false
        override suspend fun get(userKey: String) = pending?.takeIf { it.userKey == userKey }
        override suspend fun save(pending: PendingInteraction) {
            if (failNextSave) {
                failNextSave = false
                throw IllegalStateException("Falha ao salvar")
            }
            this.pending = pending
        }
        override suspend fun clear(userKey: String) { pending = null }
    }

    private val input = AiInteractionInputDto("message", "message-1", "mensagem original")
    private val initialState = ChatbotUiState(
        isLoading = false, userKey = "user-1", currentSessionId = "session-1", page = ChatbotPage.CONVERSATION,
    )
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    private fun create(
        store: ViewModelStore,
        pending: MemoryStore,
        server: MockWebServer,
        state: ChatbotUiState = initialState,
    ): ChatbotViewModel {
        val app = instrumentation.targetContext.applicationContext as Application
        val tokens = object : FirebaseIdTokenProvider {
            override fun currentFirebaseUid() = "user-1"
            override suspend fun getCurrentIdToken(forceRefresh: Boolean) = "firebase-token"
        }
        val api = AiChatApiRepository(tokens, server.url("/").toString())
        lateinit var viewModel: ChatbotViewModel
        instrumentation.runOnMainSync {
            val factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ChatbotViewModel(app, api, pending, state) as T
            }
            viewModel = ViewModelProvider(store, factory)[ChatbotViewModel::class.java]
        }
        return viewModel
    }

    private fun response(body: String, code: Int = 200) = MockResponse()
        .setResponseCode(code).addHeader("Content-Type", "application/json").setBody(body)

    private fun awaitCondition(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 20_000
        while (!condition() && System.currentTimeMillis() < deadline) Thread.sleep(50)
        assertTrue("Estado esperado não foi alcançado", condition())
    }

    private fun clear(store: ViewModelStore) = instrumentation.runOnMainSync { store.clear() }

    @Test
    fun rejectedMessageRemainsInItsOriginalConversationAfterNavigation() {
        MockWebServer().use { server ->
            server.enqueue(response("""{"access_token":"token"}"""))
            server.enqueue(response("{}", 422).setBodyDelay(1, TimeUnit.SECONDS))
            val pending = MemoryStore()
            val owner = ViewModelStore()
            try {
                val vm = create(owner, pending, server)
                instrumentation.waitForIdleSync()
                instrumentation.runOnMainSync { vm.sendText("texto recusado") }
                assertNotNull(server.takeRequest(5, TimeUnit.SECONDS))
                assertNotNull(server.takeRequest(5, TimeUnit.SECONDS))
                instrumentation.runOnMainSync { vm.startNewConversation() }
                awaitCondition { pending.pending?.rejectionCode == 422 && !vm.state.value.isSending }
                assertNull(vm.state.value.currentSessionId)
                assertEquals("session-1", vm.state.value.answerDraftToRestore?.sessionId)
                assertEquals("texto recusado", vm.composerDraft("user-1", "session-1"))
                assertEquals("", vm.composerDraft("user-1", null))
            } finally { clear(owner) }
        }
    }

    @Test
    fun recreatedViewModelResumesAcceptedOperationUsingGet() {
        MockWebServer().use { server ->
            server.enqueue(response("""{"access_token":"token"}"""))
            server.enqueue(response("""{"operation_id":"op-1","poll_after_ms":5000}""", 202))
            val pending = MemoryStore(PendingInteraction("user-1", "session-1", input))
            val firstStore = ViewModelStore()
            create(firstStore, pending, server)
            try {
                awaitCondition { pending.pending?.operationId == "op-1" }
            } finally { clear(firstStore) }

            server.enqueue(response("""{"access_token":"token"}"""))
            server.enqueue(response("""{"operation_id":"op-1","status":"completed","result":{"schema_version":"1","status":"completed","session_id":"session-1","interaction_id":"op-1","public_request_id":"request-1","response":{"kind":"answer","text":"OK"}}}"""))
            server.enqueue(response("""{"session_id":"session-1","items":[],"has_more":false}"""))
            val secondStore = ViewModelStore()
            try {
                val vm = create(secondStore, pending, server)
                awaitCondition { pending.pending == null && !vm.state.value.isSending }
                assertEquals("POST", server.takeRequest(1, TimeUnit.SECONDS)!!.method)
                assertEquals("POST", server.takeRequest(1, TimeUnit.SECONDS)!!.method)
                assertEquals("POST", server.takeRequest(1, TimeUnit.SECONDS)!!.method)
                assertEquals("/v1/sessions/session-1/interactions/op-1", server.takeRequest(1, TimeUnit.SECONDS)!!.path)
                assertEquals("/v1/sessions/session-1/messages?limit=100", server.takeRequest(1, TimeUnit.SECONDS)!!.path)
            } finally { clear(secondStore) }
        }
    }

    @Test
    fun rejectedPostRestoresDraftAndUnblocksComposerWithoutAutomaticRetry() {
        for (code in listOf(403, 404, 422)) {
            MockWebServer().use { server ->
                server.enqueue(response("""{"access_token":"token"}"""))
                server.enqueue(response("{}", code))
                val pending = MemoryStore(PendingInteraction("user-1", "session-1", input))
                val owner = ViewModelStore()
                try {
                    val vm = create(owner, pending, server)
                    awaitCondition { pending.pending?.rejectionCode == code && !vm.state.value.isSending && vm.state.value.answerDraftToRestore != null }
                    assertEquals("mensagem original", vm.state.value.answerDraftToRestore?.text)
                    assertEquals("session-1", vm.state.value.answerDraftToRestore?.sessionId)
                    assertNotNull(vm.state.value.errorMessage)
                    assertEquals(2, server.requestCount)
                    if (code == 422) {
                        server.enqueue(response("""{"operation_id":"op-2","poll_after_ms":250}""", 202))
                        server.enqueue(response("""{"operation_id":"op-2","status":"completed","result":{"schema_version":"1","status":"completed","session_id":"session-1","interaction_id":"op-2","public_request_id":"request-2","response":{"kind":"answer","text":"OK"}}}"""))
                        server.enqueue(response("""{"session_id":"session-1","items":[],"has_more":false}"""))
                        instrumentation.runOnMainSync { vm.sendText("mensagem corrigida") }
                        awaitCondition { pending.pending == null && !vm.state.value.isSending }
                        server.takeRequest(1, TimeUnit.SECONDS)
                        server.takeRequest(1, TimeUnit.SECONDS)
                        val corrected = server.takeRequest(1, TimeUnit.SECONDS)!!.body.readUtf8()
                        assertTrue(corrected.contains("mensagem corrigida"))
                        assertFalse(corrected.contains("message-1"))
                    }
                } finally { clear(owner) }
            }
        }
    }

    @Test
    fun unavailableAcceptedOperationUnblocksWithoutRestoringActionAsDraft() {
        MockWebServer().use { server ->
            server.enqueue(response("""{"access_token":"token"}"""))
            server.enqueue(response("{}", 404))
            val pending = MemoryStore(PendingInteraction("user-1", "session-1", input, operationId = "op-1", pollAfterMs = 250))
            val owner = ViewModelStore()
            try {
                val vm = create(owner, pending, server)
                awaitCondition { pending.pending?.rejectionCode == 404 && !vm.state.value.isSending }
                assertNull(vm.state.value.answerDraftToRestore)
                assertTrue(vm.state.value.errorMessage.orEmpty().contains("incerto"))
            } finally { clear(owner) }
        }
    }

    @Test
    fun failedAcceptedOperationDoesNotKeepComposerBlocked() {
        MockWebServer().use { server ->
            server.enqueue(response("""{"access_token":"token"}"""))
            server.enqueue(response("""{"operation_id":"op-1","status":"failed"}"""))
            val pending = MemoryStore(PendingInteraction("user-1", "session-1", input, operationId = "op-1", pollAfterMs = 250))
            val owner = ViewModelStore()
            try {
                val vm = create(owner, pending, server)
                awaitCondition { pending.pending?.rejectionCode == -1 && !vm.state.value.isSending }
                assertTrue(vm.state.value.errorMessage.orEmpty().contains("incerto"))
            } finally { clear(owner) }
        }
    }

    @Test
    fun unavailableInterruptedConversationCanBeAcknowledgedWithoutResending() {
        MockWebServer().use { server ->
            val pending = MemoryStore(PendingInteraction("user-1", "unavailable-session", input, operationId = "op-1", interrupted = true))
            val state = initialState.copy(currentSessionId = null, page = ChatbotPage.HISTORY)
            val owner = ViewModelStore()
            try {
                val vm = create(owner, pending, server, state)
                awaitCondition { vm.state.value.canContinueAfterInterruption }
                assertTrue(vm.state.value.isSending)
                instrumentation.runOnMainSync { vm.startNewConversation() }
                assertTrue(vm.state.value.canContinueAfterInterruption)
                instrumentation.runOnMainSync { vm.continueAfterInterruption() }
                awaitCondition { pending.pending?.rejectionCode == -1 && !vm.state.value.isSending }
                assertTrue(pending.pending!!.interrupted)
                assertEquals("op-1", pending.pending!!.operationId)
                assertFalse(vm.state.value.canContinueAfterInterruption)
                assertEquals(0, server.requestCount)
            } finally { clear(owner) }

            val reopened = ViewModelStore()
            try {
                val vm = create(reopened, pending, server, state)
                awaitCondition { !vm.state.value.isSending && vm.state.value.errorMessage != null }
                assertFalse(vm.state.value.canContinueAfterInterruption)
                assertTrue(vm.state.value.errorMessage.orEmpty().contains("incerto"))
                assertEquals(0, server.requestCount)
            } finally { clear(reopened) }
        }
    }

    @Test
    fun acknowledgementCanBeRetriedWhenSavingFails() {
        MockWebServer().use { server ->
            val pending = MemoryStore(PendingInteraction("user-1", "missing", input, interrupted = true))
            val owner = ViewModelStore()
            try {
                val vm = create(owner, pending, server, initialState.copy(currentSessionId = null))
                awaitCondition { vm.state.value.canContinueAfterInterruption }
                pending.failNextSave = true
                instrumentation.runOnMainSync { vm.continueAfterInterruption() }
                awaitCondition { vm.state.value.canContinueAfterInterruption && vm.state.value.errorMessage == "Não foi possível continuar. Tente novamente." }
                assertTrue(vm.state.value.isSending)
                assertNull(pending.pending!!.rejectionCode)
                instrumentation.runOnMainSync { vm.continueAfterInterruption() }
                awaitCondition { pending.pending?.rejectionCode == -1 && !vm.state.value.isSending }
                assertEquals(0, server.requestCount)
            } finally { clear(owner) }
        }
    }

    @Test
    fun lostPostResponseIsRetriedWithTheSameStructuredAnswer() {
        MockWebServer().use { server ->
            server.enqueue(response("""{"access_token":"token"}"""))
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
            server.enqueue(response("""{"operation_id":"op-1","poll_after_ms":250}""", 202))
            server.enqueue(response("""{"operation_id":"op-1","status":"interrupted"}"""))
            val answer = AiInteractionInputDto("answer", interactionId = "i-1", questionId = "q-1", answer = AiAnswerInputDto("confirmation", confirm = true))
            val pending = MemoryStore(PendingInteraction("user-1", "session-1", answer))
            val owner = ViewModelStore()
            try {
                val vm = create(owner, pending, server)
                awaitCondition { pending.pending?.interrupted == true && vm.state.value.canContinueAfterInterruption }
                server.takeRequest(1, TimeUnit.SECONDS)
                val first = server.takeRequest(1, TimeUnit.SECONDS)!!
                val second = server.takeRequest(1, TimeUnit.SECONDS)!!
                assertEquals(first.body.readUtf8(), second.body.readUtf8())
                assertTrue(vm.state.value.isSending)
            } finally { clear(owner) }
        }
    }
}
