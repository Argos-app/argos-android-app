package com.android.argosapp.data.cache.storage

import android.content.Context
import com.android.argosapp.data.cache.dao.ChatSessionDao
import com.android.argosapp.data.cache.db.ArgosCacheDatabase
import com.android.argosapp.data.cache.entity.ChatMessageEntity
import com.android.argosapp.data.cache.entity.ChatSessionEntity
import com.android.argosapp.model.ChatMessage
import com.android.argosapp.model.ChatSession
import java.util.UUID

class ChatHistoryStorage(context: Context) {
    private val dao: ChatSessionDao = ArgosCacheDatabase.getInstance(context).chatSessionDao()

    suspend fun createSession(userKey: String, now: Long = System.currentTimeMillis()): ChatSession {
        val session = ChatSessionEntity(
            id = UUID.randomUUID().toString(),
            userKey = userKey,
            createdAt = now,
            updatedAt = now,
        )
        dao.insertSession(session)
        return session.toModel()
    }

    suspend fun getSessions(userKey: String): List<ChatSession> =
        dao.getSessions(userKey).map(ChatSessionEntity::toModel)

    suspend fun getMessages(sessionId: String, userKey: String): List<ChatMessage> =
        dao.getMessages(sessionId, userKey).map(ChatMessageEntity::toModel)

    suspend fun appendMessage(message: ChatMessage): ChatMessage {
        return dao.appendMessage(ChatMessageEntity.fromModel(message)).toModel()
    }

    suspend fun updateMessage(message: ChatMessage) {
        dao.updateMessage(ChatMessageEntity.fromModel(message))
    }
}
