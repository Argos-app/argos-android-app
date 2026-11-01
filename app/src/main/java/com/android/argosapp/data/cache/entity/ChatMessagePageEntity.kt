package com.android.argosapp.data.cache.entity

import androidx.room3.Entity

@Entity(tableName = "chat_message_pages", primaryKeys = ["userKey", "sessionId"])
data class ChatMessagePageEntity(
    val userKey: String,
    val sessionId: String,
    val nextCursor: String?,
    val hasMore: Boolean,
)
