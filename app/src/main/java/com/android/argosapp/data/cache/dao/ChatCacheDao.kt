package com.android.argosapp.data.cache.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.android.argosapp.data.cache.entity.ChatMessageEntity
import com.android.argosapp.data.cache.entity.ChatMessagePageEntity
import com.android.argosapp.data.cache.entity.ChatSessionEntity

@Dao
interface ChatCacheDao {
    @Query("SELECT * FROM chat_sessions WHERE userKey = :userKey ORDER BY updatedAt DESC")
    suspend fun sessions(userKey: String): List<ChatSessionEntity>

    @Query("SELECT * FROM chat_messages WHERE userKey = :userKey AND sessionId = :sessionId ORDER BY position")
    suspend fun messages(userKey: String, sessionId: String): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_message_pages WHERE userKey = :userKey AND sessionId = :sessionId LIMIT 1")
    suspend fun page(userKey: String, sessionId: String): ChatMessagePageEntity?

    @Query("DELETE FROM chat_sessions WHERE userKey = :userKey")
    suspend fun deleteSessions(userKey: String)

    @Query("DELETE FROM chat_messages WHERE userKey = :userKey AND sessionId = :sessionId")
    suspend fun deleteMessages(userKey: String, sessionId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<ChatSessionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: ChatMessagePageEntity)

    @Query("DELETE FROM chat_sessions")
    suspend fun clearSessions()

    @Query("DELETE FROM chat_messages")
    suspend fun clearMessages()

    @Query("DELETE FROM chat_message_pages")
    suspend fun clearPages()

    @Transaction
    suspend fun clear() {
        clearSessions()
        clearMessages()
        clearPages()
    }

    @Transaction
    suspend fun replaceSessions(userKey: String, sessions: List<ChatSessionEntity>) {
        deleteSessions(userKey)
        insertSessions(sessions)
    }

    @Transaction
    suspend fun savePage(page: ChatMessagePageEntity, messages: List<ChatMessageEntity>, replace: Boolean) {
        if (replace) deleteMessages(page.userKey, page.sessionId)
        insertMessages(messages)
        insertPage(page)
    }
}
