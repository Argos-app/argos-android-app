package com.android.argosapp.data.cache.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.android.argosapp.data.cache.entity.ChatMessageEntity
import com.android.argosapp.data.cache.entity.ChatSessionEntity

@Dao
interface ChatSessionDao {
    @Query("SELECT * FROM chat_sessions WHERE userKey = :userKey ORDER BY updatedAt DESC")
    suspend fun getSessions(userKey: String): List<ChatSessionEntity>

    @Query("SELECT * FROM chat_sessions WHERE id = :sessionId AND userKey = :userKey LIMIT 1")
    suspend fun getSession(sessionId: String, userKey: String): ChatSessionEntity?

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId AND userKey = :userKey ORDER BY position")
    suspend fun getMessages(sessionId: String, userKey: String): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(session: ChatSessionEntity)

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM chat_messages WHERE sessionId = :sessionId AND userKey = :userKey")
    suspend fun nextPosition(sessionId: String, userKey: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMessage(message: ChatMessageEntity)

    @Query("UPDATE chat_sessions SET updatedAt = :updatedAt WHERE id = :sessionId AND userKey = :userKey")
    suspend fun touchSession(sessionId: String, userKey: String, updatedAt: Long)

    @Transaction
    suspend fun appendMessage(message: ChatMessageEntity): ChatMessageEntity {
        val position = nextPosition(message.sessionId, message.userKey)
        val stored = message.copy(position = position)
        saveMessage(stored)
        touchSession(message.sessionId, message.userKey, message.createdAt)
        return stored
    }

    @Transaction
    suspend fun updateMessage(message: ChatMessageEntity) {
        saveMessage(message)
        touchSession(message.sessionId, message.userKey, message.createdAt)
    }
}
