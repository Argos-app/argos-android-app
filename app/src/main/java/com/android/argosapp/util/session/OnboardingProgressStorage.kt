package com.android.argosapp.util.session

import android.content.Context

/** Tracks an unfinished first-access onboarding so it can resume after the app is closed. */
class OnboardingProgressStorage(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun isPending(userEmail: String): Boolean = preferences.getBoolean(pendingKey(userEmail), false)

    fun pendingStep(userEmail: String): Int =
        preferences.getInt(stepKey(userEmail), FIRST_STEP).coerceIn(FIRST_STEP, LAST_STEP)

    fun markPending(userEmail: String) {
        preferences.edit()
            .putBoolean(pendingKey(userEmail), true)
            .putInt(stepKey(userEmail), FIRST_STEP)
            .apply()
    }

    fun saveStep(userEmail: String, step: Int) {
        preferences.edit()
            .putInt(stepKey(userEmail), step.coerceIn(FIRST_STEP, LAST_STEP))
            .apply()
    }

    fun markCompleted(userEmail: String) {
        preferences.edit()
            .remove(pendingKey(userEmail))
            .remove(stepKey(userEmail))
            .apply()
    }

    private fun pendingKey(userEmail: String): String = "pending_${normalizedEmail(userEmail)}"

    private fun stepKey(userEmail: String): String = "step_${normalizedEmail(userEmail)}"

    private fun normalizedEmail(userEmail: String): String = userEmail.trim().lowercase()

    private companion object {
        const val PREFERENCES_NAME = "onboarding_progress"
        const val FIRST_STEP = 0
        const val LAST_STEP = 2
    }
}

