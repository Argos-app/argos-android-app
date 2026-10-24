package com.android.argosapp.data.cache.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.android.argosapp.model.ChatMessage
import com.android.argosapp.model.ChatMessageKind
import com.android.argosapp.model.ChatResponseState
import com.android.argosapp.model.ChatSender
import org.json.JSONArray

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["sessionId", "position"], name = "index_chat_messages_sessionId_position"),
        Index(value = ["userKey", "sessionId"], name = "index_chat_messages_userKey_sessionId"),
    ],
)
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val userKey: String,
    val position: Int,
    val sender: String,
    val text: String,
    val kind: String,
    val optionsJson: String,
    val responseState: String?,
    val createdAt: Long,
) {
    fun toModel() = ChatMessage(
        id = id,
        sessionId = sessionId,
        userKey = userKey,
        position = position,
        sender = ChatSender.valueOf(sender),
        text = text,
        kind = ChatMessageKind.valueOf(kind),
        options = runCatching {
            val json = JSONArray(optionsJson)
            List(json.length()) { index -> json.getString(index) }
        }.getOrDefault(emptyList()),
        responseState = responseState?.let(ChatResponseState::valueOf),
        createdAt = createdAt,
    )

    companion object {
        fun fromModel(message: ChatMessage) = ChatMessageEntity(
            id = message.id,
            sessionId = message.sessionId,
            userKey = message.userKey,
            position = message.position,
            sender = message.sender.name,
            text = message.text,
            kind = message.kind.name,
            optionsJson = JSONArray(message.options).toString(),
            responseState = message.responseState?.name,
            createdAt = message.createdAt,
        )
    }
}
