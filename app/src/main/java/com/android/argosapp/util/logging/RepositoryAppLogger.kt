package com.android.argosapp.util.logging

import android.os.SystemClock
import kotlinx.coroutines.CancellationException

/** Logger especializado que mantém uma tag pesquisável comum para todos os repositórios. */
class RepositoryAppLogger(
    private val repositoryName: String,
) : AppLogger() {
    fun debug(
        event: String,
        vararg fields: Pair<String, Any?>,
    ) = writeDebug(
        AppLogger.REPOSITORY_TAG,
        event,
        withRepository(fields),
    )

    fun info(
        event: String,
        vararg fields: Pair<String, Any?>,
    ) = writeInfo(
        AppLogger.REPOSITORY_TAG,
        event,
        withRepository(fields),
    )

    fun error(
        event: String,
        throwable: Throwable? = null,
        vararg fields: Pair<String, Any?>,
    ) = writeError(
        AppLogger.REPOSITORY_TAG,
        event,
        throwable,
        withRepository(fields),
    )

    private fun withRepository(
        fields: Array<out Pair<String, Any?>>,
    ): Array<out Pair<String, Any?>> =
        arrayOf<Pair<String, Any?>>("repository" to repositoryName, *fields)

    suspend fun <T> runSuspending(
        operation: String,
        startFields: Array<out Pair<String, Any?>> = emptyArray(),
        successFields: (T) -> Array<out Pair<String, Any?>> = { emptyArray() },
        failureFields: (Throwable) -> Array<out Pair<String, Any?>> = { error ->
            arrayOf("error_type" to error.javaClass.simpleName)
        },
        call: suspend () -> T,
    ): T {
        val startedAt = SystemClock.elapsedRealtime()
        debug("${operation}_started", *startFields)

        return try {
            call().also { result ->
                info(
                    "${operation}_succeeded",
                    "duration_ms" to (SystemClock.elapsedRealtime() - startedAt),
                    *successFields(result),
                )
            }
        } catch (error: CancellationException) {
            debug(
                "${operation}_cancelled",
                "duration_ms" to (SystemClock.elapsedRealtime() - startedAt),
            )
            throw error
        } catch (error: Throwable) {
            this.error(
                "${operation}_failed",
                error,
                "duration_ms" to (SystemClock.elapsedRealtime() - startedAt),
                *failureFields(error),
            )
            throw error
        }
    }

    fun <T> runSynchronous(
        operation: String,
        startFields: Array<out Pair<String, Any?>> = emptyArray(),
        successFields: (T) -> Array<out Pair<String, Any?>> = { emptyArray() },
        failureFields: (Throwable) -> Array<out Pair<String, Any?>> = { error ->
            arrayOf("error_type" to error.javaClass.simpleName)
        },
        call: () -> T,
    ): T {
        val startedAt = SystemClock.elapsedRealtime()
        debug("${operation}_started", *startFields)

        return try {
            call().also { result ->
                info(
                    "${operation}_succeeded",
                    "duration_ms" to (SystemClock.elapsedRealtime() - startedAt),
                    *successFields(result),
                )
            }
        } catch (error: Throwable) {
            this.error(
                "${operation}_failed",
                error,
                "duration_ms" to (SystemClock.elapsedRealtime() - startedAt),
                *failureFields(error),
            )
            throw error
        }
    }
}
