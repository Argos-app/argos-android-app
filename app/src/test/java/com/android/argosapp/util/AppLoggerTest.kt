package com.android.argosapp.util

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLoggerTest {
    @Test
    fun formatKeepsEventAndNonSensitiveFields() {
        val result = AppLogger.format(
            event = "login_started",
            fields = arrayOf("method" to "password"),
        )

        assertEquals("event=login_started method=password", result)
    }

    @Test
    fun formatRemovesSensitiveFields() {
        val result = AppLogger.format(
            event = "login_started",
            fields = arrayOf(
                "method" to "password",
                "password" to "secret",
                "idToken" to "token",
            ),
        )

        assertEquals("event=login_started method=password", result)
    }
}
