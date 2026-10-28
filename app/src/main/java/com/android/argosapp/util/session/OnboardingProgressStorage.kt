package com.android.argosapp.util.session

import android.content.Context

/** Tracks an unfinished first-access onboarding so it can resume after the app is closed. */
class OnboardingProgressStorage(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun isPending(userEmail: String): Boolean = preferences.getBoolean(key(userEmail), false)

    fun markPending(userEmail: String) {
        preferences.edit().putBoolean(key(userEmail), true).apply()
    }

    fun markCompleted(userEmail: String) {
        preferences.edit().remove(key(userEmail)).apply()
    }

    private fun key(userEmail: String): String = "pending_${userEmail.trim().lowercase()}"

    private companion object {
        const val PREFERENCES_NAME = "onboarding_progress"
    }
}
