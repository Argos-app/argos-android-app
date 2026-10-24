package com.android.argosapp.data.cache.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.android.argosapp.model.ChatSession

@Entity(
    tableName = "chat_sessions",
    indices = [Index(value = ["userKey", "updatedAt"], name = "index_chat_sessions_userKey_updatedAt")],
)
data class ChatSessionEntity(
    @PrimaryKey val id: String,
    val userKey: String,
    val createdAt: Long,
    val updatedAt: Long,
) {
    fun toModel() = ChatSession(id, userKey, createdAt, updatedAt)
}
