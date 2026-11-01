package com.android.argosapp.model

data class ChatSession(
    val id: String,
    val userKey: String,
    val createdAt: Long,
    val updatedAt: Long,
    val title: String? = null,
)

fun chatSessionTitle(prompt: String): String =
    prompt.trim().split(Regex("\\s+")).take(3).joinToString(" ")
