package com.android.argosapp.util

import timber.log.Timber

object AppLogger {
    const val AUTH_TAG = "ARGOS_AUTH"
    const val LOGIN_TAG = "ARGOS_LOGIN"
    const val FIRST_ACCESS_TAG = "ARGOS_FIRST"
    const val RESET_TAG = "ARGOS_RESET"
    const val SESSION_TAG = "ARGOS_SESSION"

    fun debug(
        tag: String,
        event: String,
        vararg fields: Pair<String, Any?>,
    ) {
        Timber.tag(tag).d(format(event, fields))
    }

    fun info(
        tag: String,
        event: String,
        vararg fields: Pair<String, Any?>,
    ) {
        Timber.tag(tag).i(format(event, fields))
    }

    fun error(
        tag: String,
        event: String,
        throwable: Throwable? = null,
        vararg fields: Pair<String, Any?>,
    ) {
        Timber.tag(tag).e(throwable, format(event, fields))
    }

    internal fun format(
        event: String,
        fields: Array<out Pair<String, Any?>>,
    ): String = buildString {
        append("event=").append(event)
        fields.forEach { (key, value) ->
            if (key !in SENSITIVE_KEYS && value != null) {
                append(' ').append(key).append('=').append(value)
            }
        }
    }

    private val SENSITIVE_KEYS = setOf(
        "password",
        "firebaseIdToken",
        "idToken",
        "bearerToken",
        "oobCode",
        "resetCode",
        "email",
    )
}
