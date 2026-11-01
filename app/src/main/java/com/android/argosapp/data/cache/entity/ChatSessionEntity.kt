package com.android.argosapp.data.cache.entity

import androidx.room3.Embedded
import androidx.room3.Entity
import com.android.argosapp.model.ChatSession

@Entity(tableName = "chat_sessions", primaryKeys = ["userKey", "id"])
data class ChatSessionEntity(@Embedded val session: ChatSession)
