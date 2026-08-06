package com.Sunset.REN.GitHub.domain.filemanager.root

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

open class RootCommandRunner(
    private var suCommand: String = "su"
) {
    fun updateSuCommand(command: String) {
        suCommand = normalizeSuCommand(command)
    }

    fun quoteArg(value: String): String = "'" + value.replace("'", "'\"'\"'") + "'"

    fun buildCommand(program: String, vararg args: String): String {
        require(program.matches(Regex("[A-Za-z0-9_./-]+"))) { "Invalid root command program" }
        return buildString {
            append(program)
            args.forEach { arg ->
                append(' ')
                append(quoteArg(arg))
            }
        }
    }

    open suspend fun hasSuBinary(): Boolean = withContext(Dispatchers.IO) {
        val command = normalizeSuCommand(suCommand)
        val directCommand = File(command)
        if (command.contains('/')) {
            return@withContext directCommand.exists() && directCommand.canExecute()
        }
        val candidates = listOf("/system/bin/$command", "/system/xbin/$command", "/sbin/$command", "/vendor/bin/$command", "/su/bin/$command")
        candidates.any { File(it).exists() && File(it).canExecute() } || runCatching {
            ProcessBuilder("sh", "-c", "command -v ${shellQuote(command)}").redirectErrorStream(true).start().useProcess { process ->
                process.waitFor(1500, TimeUnit.MILLISECONDS) && process.exitValue() == 0
            }
        }.getOrDefault(false)
    }

    open suspend fun run(command: String, timeoutMillis: Long = 5000L): Result<String> = withContext(Dispatchers.IO) {
        runBlocking(command, timeoutMillis)
    }

    open fun runBlocking(command: String, timeoutMillis: Long = 5000L): Result<String> {
        return runCatching {
            val process = ProcessBuilder(normalizeSuCommand(suCommand), "-c", command).redirectErrorStream(true).start()
            val completed = process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)
            if (!completed) {
                process.destroyForcibly()
                error("Root command timed out")
            }
            val output = process.inputStream.bufferedReader().readText().trim()
            if (process.exitValue() != 0) error(output.ifBlank { "Root command failed: ${process.exitValue()}" })
            output
        }
    }

    private fun normalizeSuCommand(command: String): String {
        val normalized = command.trim().ifBlank { "su" }
        require(!normalized.any { it.isWhitespace() }) { "Root command must not contain whitespace" }
        require(normalized.matches(Regex("[A-Za-z0-9_./-]+"))) { "Invalid root command" }
        return normalized
    }

    private fun shellQuote(value: String): String = "'" + value.replace("'", "'\"'\"'") + "'"

    private inline fun <T> Process.useProcess(block: (Process) -> T): T {
        return try { block(this) } finally { destroy() }
    }
}
