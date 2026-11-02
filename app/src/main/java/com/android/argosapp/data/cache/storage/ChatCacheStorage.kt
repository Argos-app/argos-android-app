package com.android.argosapp.data.cache.storage

import android.content.Context
import com.android.argosapp.data.cache.db.ChatCacheDatabase
import com.android.argosapp.data.cache.entity.ChatMessageEntity
import com.android.argosapp.data.cache.entity.ChatMessagePageEntity
import com.android.argosapp.data.cache.entity.ChatSessionEntity
import com.android.argosapp.model.ChatMessage
import com.android.argosapp.model.ChatSession

class ChatCacheStorage(context: Context) {
    private val dao = ChatCacheDatabase.getInstance(context).chatCacheDao()

    suspend fun clear() = dao.clear()

    suspend fun sessions(userKey: String): List<ChatSession> = dao.sessions(userKey).map { it.session }

    suspend fun saveSession(session: ChatSession) = dao.insertSessions(listOf(ChatSessionEntity(session)))

    suspend fun replaceSessions(userKey: String, sessions: List<ChatSession>) =
        dao.replaceSessions(userKey, sessions.map(::ChatSessionEntity))

    suspend fun messages(userKey: String, sessionId: String): List<ChatMessage> =
        dao.messages(userKey, sessionId).map { it.message }

    suspend fun page(userKey: String, sessionId: String) = dao.page(userKey, sessionId)

    suspend fun savePage(userKey: String, sessionId: String, messages: List<ChatMessage>, nextCursor: String?, hasMore: Boolean, replace: Boolean) =
        dao.savePage(ChatMessagePageEntity(userKey, sessionId, nextCursor, hasMore), messages.map(::ChatMessageEntity), replace)
}
