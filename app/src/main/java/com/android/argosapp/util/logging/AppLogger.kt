package com.android.argosapp.util.logging

import timber.log.Timber

open class AppLogger {
    internal fun writeDebug(
        tag: String,
        event: String,
        fields: Array<out Pair<String, Any?>>,
    ) {
        Timber.tag(tag).d(format(event, fields))
    }

    internal fun writeInfo(
        tag: String,
        event: String,
        fields: Array<out Pair<String, Any?>>,
    ) {
        Timber.tag(tag).i(format(event, fields))
    }

    internal fun writeError(
        tag: String,
        event: String,
        throwable: Throwable?,
        fields: Array<out Pair<String, Any?>>,
    ) {
        Timber.tag(tag).e(throwable, format(event, fields))
    }

    companion object {
        const val AUTH_TAG = "ARGOS_AUTH"
        const val FIRST_ACCESS_TAG = "ARGOS_FIRST"
        const val RESET_TAG = "ARGOS_RESET"
        const val SESSION_TAG = "ARGOS_SESSION"
        const val REPOSITORY_TAG = "REPOSITORY_OPERATIONS"

        private val defaultLogger = AppLogger()

        fun debug(
            tag: String,
            event: String,
            vararg fields: Pair<String, Any?>,
        ) = defaultLogger.writeDebug(tag, event, fields)

        fun info(
            tag: String,
            event: String,
            vararg fields: Pair<String, Any?>,
        ) = defaultLogger.writeInfo(tag, event, fields)

        fun error(
            tag: String,
            event: String,
            throwable: Throwable? = null,
            vararg fields: Pair<String, Any?>,
        ) = defaultLogger.writeError(tag, event, throwable, fields)

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
}
