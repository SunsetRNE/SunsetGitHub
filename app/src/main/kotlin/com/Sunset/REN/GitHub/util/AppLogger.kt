package com.Sunset.REN.GitHub.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {

    private const val DefaultTag = "SunsetGitHub"
    private const val LogsDirectoryName = "logs"
    private const val CurrentLogFileName = "app.log"
    private const val PreviousLogFileName = "app.old.log"
    private const val MaxLogFileBytes = 512 * 1024L
    private const val MaxReadableLogChars = 120_000

    private val lock = Any()
    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private var logsDirectory: File? = null

    fun initialize(context: Context) {
        synchronized(lock) {
            logsDirectory = File(context.filesDir, LogsDirectoryName).also { directory ->
                if (!directory.exists()) directory.mkdirs()
            }
        }
        i("AppLogger", "logger initialized")
    }

    fun d(tag: String = DefaultTag, message: String) {
        Log.d(tag, message)
        write("D", tag, message, null)
    }

    fun i(tag: String = DefaultTag, message: String) {
        Log.i(tag, message)
        write("I", tag, message, null)
    }

    fun w(tag: String = DefaultTag, message: String, throwable: Throwable? = null) {
        Log.w(tag, message, throwable)
        write("W", tag, message, throwable)
    }

    fun e(tag: String = DefaultTag, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        write("E", tag, message, throwable)
    }

    fun readLogText(maxChars: Int = MaxReadableLogChars): String {
        val directory = logsDirectory ?: return ""
        return synchronized(lock) {
            listOf(PreviousLogFileName, CurrentLogFileName)
                .map { fileName -> File(directory, fileName) }
                .filter { file -> file.exists() }
                .joinToString(separator = "") { file -> file.readText() }
                .takeLast(maxChars)
        }
    }

    private fun write(level: String, tag: String, message: String, throwable: Throwable?) {
        val directory = logsDirectory ?: return
        synchronized(lock) {
            runCatching {
                if (!directory.exists()) directory.mkdirs()
                val currentLog = File(directory, CurrentLogFileName)
                rotateIfNeeded(currentLog)
                currentLog.appendText(buildLine(level, tag, message, throwable))
            }.onFailure { error ->
                Log.w(DefaultTag, "Failed to write app log", error)
            }
        }
    }

    private fun rotateIfNeeded(currentLog: File) {
        if (!currentLog.exists() || currentLog.length() <= MaxLogFileBytes) return
        val previousLog = File(currentLog.parentFile, PreviousLogFileName)
        if (previousLog.exists()) previousLog.delete()
        currentLog.renameTo(previousLog)
    }

    private fun buildLine(level: String, tag: String, message: String, throwable: Throwable?): String {
        val timestamp = timestampFormat.format(Date())
        return buildString {
            append(timestamp)
            append(' ')
            append(level)
            append('/')
            append(tag)
            append(": ")
            append(message)
            append('\n')
            throwable?.let { error ->
                append(error.stackTraceText())
                append('\n')
            }
        }
    }

    private fun Throwable.stackTraceText(): String {
        val writer = StringWriter()
        printStackTrace(PrintWriter(writer))
        return writer.toString()
    }
}
