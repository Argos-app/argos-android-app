package com.android.argosapp.data.cache.entity

import androidx.room3.Embedded
import androidx.room3.Entity
import com.android.argosapp.model.ChatMessage

@Entity(tableName = "chat_messages", primaryKeys = ["userKey", "sessionId", "id"])
data class ChatMessageEntity(@Embedded val message: ChatMessage)
