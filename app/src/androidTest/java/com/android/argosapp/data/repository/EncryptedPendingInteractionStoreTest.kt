package com.android.argosapp.data.repository

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.runBlocking

@RunWith(AndroidJUnit4::class)
class EncryptedPendingInteractionStoreTest {
    @Test
    fun preservesEncryptedStructuredAnswerAcrossStoreInstances() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val userKey = UUID.randomUUID().toString()
        val pending = PendingInteraction(
            userKey, "session-1",
            AiInteractionInputDto("answer", interactionId = "interaction-1", questionId = "question-1",
                answer = AiAnswerInputDto("text", text = "resposta privada")),
            operationId = "op-1",
        )
        val store = EncryptedPendingInteractionStore(context)
        try {
            store.save(pending)
            val stored = context.getSharedPreferences("chatbot_pending_interactions", Context.MODE_PRIVATE)
                .getString(userKey, null).orEmpty()
            assertFalse(stored.contains("resposta privada"))
            assertEquals(pending, EncryptedPendingInteractionStore(context).get(userKey))
            assertNull(EncryptedPendingInteractionStore(context).get("other-user"))
        } finally {
            store.clear(userKey)
        }
    }

    @Test
    fun logoutRemovesOnlyItsAccountAndPreventsLateWrites() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val user = UUID.randomUUID().toString()
        val otherUser = UUID.randomUUID().toString()
        val active = AtomicBoolean(true)
        val store = EncryptedPendingInteractionStore(context) { uid -> uid != user || active.get() }
        val pending = PendingInteraction(user, "session", AiInteractionInputDto("message", "id", "conteúdo"))
        val other = pending.copy(userKey = otherUser)
        try {
            store.save(pending)
            store.save(other)
            active.set(false)
            EncryptedPendingInteractionStore(context).clear(user)
            try {
                store.save(pending)
                throw AssertionError("Uma gravação após o logout foi aceita")
            } catch (_: IllegalStateException) { }
            assertNull(store.get(user))
            assertEquals(other, store.get(otherUser))
        } finally {
            store.clear(user)
            store.clear(otherUser)
        }
    }
}
