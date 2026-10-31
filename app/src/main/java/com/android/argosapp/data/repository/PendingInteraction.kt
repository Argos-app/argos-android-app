package com.android.argosapp.data.repository

import android.content.Context
import com.android.argosapp.util.session.SharedPreferencesCipher
import com.google.gson.Gson
import retrofit2.HttpException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class PendingInteraction(
    val userKey: String,
    val sessionId: String,
    val input: AiInteractionInputDto,
    val operationId: String? = null,
    val pollAfterMs: Long = 1000,
    val interrupted: Boolean = false,
    val rejectionCode: Int? = null,
)

interface PendingInteractionStore {
    suspend fun get(userKey: String): PendingInteraction?
    suspend fun save(pending: PendingInteraction)
    suspend fun clear(userKey: String)
}

class EncryptedPendingInteractionStore(
    context: Context,
    private val canSave: (String) -> Boolean = { true },
) : PendingInteractionStore {
    private val preferences = context.getSharedPreferences("chatbot_pending_interactions", Context.MODE_PRIVATE)
    private val cipher = SharedPreferencesCipher("chatbot_pending_interactions")
    private val gson = Gson()

    override suspend fun get(userKey: String): PendingInteraction? = withContext(Dispatchers.IO) {
        val encrypted = preferences.getString(userKey, null) ?: return@withContext null
        val json = checkNotNull(cipher.decrypt(encrypted)) { "Não foi possível recuperar a interação pendente." }
        checkNotNull(gson.fromJson(json, PendingInteraction::class.java))
            .also { check(it.userKey == userKey) }
    }

    override suspend fun save(pending: PendingInteraction) = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            check(canSave(pending.userKey)) { "A conta da interação não está mais autenticada." }
            check(preferences.edit().putString(pending.userKey, cipher.encrypt(gson.toJson(pending))).commit()) {
                "Não foi possível guardar a interação pendente."
            }
        }
    }

    override suspend fun clear(userKey: String) = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            check(preferences.edit().remove(userKey).commit()) { "Não foi possível limpar a interação pendente." }
        }
    }

    private companion object {
        val writeMutex = Mutex()
    }
}

class PendingInteractionRunner(
    private val store: PendingInteractionStore,
    private val interact: suspend (PendingInteraction, suspend (AiInteractionAcceptedDto) -> Unit) -> AiInteractionResultDto,
) {
    suspend fun run(pending: PendingInteraction): AiInteractionResultDto {
        if (pending.interrupted) throw InterruptedInteractionException()
        try {
            return interact(pending) { accepted ->
                store.save(pending.copy(operationId = accepted.operationId, pollAfterMs = accepted.pollAfterMs))
            }
        } catch (error: Exception) {
            if (error is InterruptedInteractionException || error is HttpException && error.code() == 409) {
                store.save((store.get(pending.userKey) ?: pending).copy(interrupted = true))
                throw InterruptedInteractionException()
            }
            throw error
        }
    }
}
