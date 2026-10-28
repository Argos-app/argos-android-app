package com.android.argosapp.util.session

import android.content.Context
import java.util.Locale

class ChatbotOnboardingStorage(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun isCompleted(userEmail: String): Boolean = preferences.getBoolean(key(userEmail), false)

    fun markCompleted(userEmail: String) {
        preferences.edit().putBoolean(key(userEmail), true).apply()
    }

    private fun key(userEmail: String) = "completed_${userEmail.trim().lowercase(Locale.ROOT)}"

    private companion object {
        const val PREFERENCES_NAME = "chatbot_onboarding"
    }
}
