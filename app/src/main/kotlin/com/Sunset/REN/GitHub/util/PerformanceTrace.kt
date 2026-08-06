package com.Sunset.REN.GitHub.util

import android.os.SystemClock

/**
 * Lightweight performance trace helper for page loading hot paths.
 *
 * It writes structured duration logs through [AppLogger], so traces are visible both in logcat
 * and the app log file without adding a runtime dependency.
 */
object PerformanceTrace {

    private const val Tag = "PerformanceTrace"

    inline fun <T> measure(name: String, crossinline metadata: () -> String = { "" }, block: () -> T): T {
        val startMillis = SystemClock.elapsedRealtime()
        return try {
            block()
        } finally {
            log(name, startMillis, metadata())
        }
    }

    suspend inline fun <T> measureSuspend(
        name: String,
        crossinline metadata: () -> String = { "" },
        crossinline block: suspend () -> T
    ): T {
        val startMillis = SystemClock.elapsedRealtime()
        return try {
            block()
        } finally {
            log(name, startMillis, metadata())
        }
    }

    fun mark(name: String, metadata: String = "") {
        AppLogger.d(Tag, buildMessage(name = name, durationMillis = null, metadata = metadata))
    }

    fun log(name: String, startMillis: Long, metadata: String = "") {
        val durationMillis = SystemClock.elapsedRealtime() - startMillis
        AppLogger.d(Tag, buildMessage(name = name, durationMillis = durationMillis, metadata = metadata))
    }

    private fun buildMessage(name: String, durationMillis: Long?, metadata: String): String {
        return buildString {
            append(name)
            durationMillis?.let {
                append(" durationMs=")
                append(it)
            }
            if (metadata.isNotBlank()) {
                append(' ')
                append(metadata)
            }
        }
    }
}
