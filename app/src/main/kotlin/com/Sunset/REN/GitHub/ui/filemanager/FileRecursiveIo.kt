package com.Sunset.REN.GitHub.ui.filemanager

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.io.FileAlreadyExistsException

suspend fun InputStream.copyToCancellable(out: OutputStream, bufferSize: Int = DEFAULT_BUFFER_SIZE): Long {
    var bytesCopied = 0L
    val buffer = ByteArray(bufferSize)
    while (true) {
        currentCoroutineContext().ensureActive()
        val bytes = read(buffer)
        if (bytes < 0) break
        out.write(buffer, 0, bytes)
        bytesCopied += bytes.toLong()
    }
    return bytesCopied
}

suspend fun File.copyToCancellable(target: File, overwrite: Boolean = false) {
    currentCoroutineContext().ensureActive()
    if (target.exists()) {
        if (!overwrite) throw FileAlreadyExistsException(this, target, "Target already exists")
        if (!target.delete()) throw FileAlreadyExistsException(this, target, "Cannot overwrite target")
    }
    target.parentFile?.mkdirs()
    inputStream().use { input -> target.outputStream().use { output -> input.copyToCancellable(output) } }
}

suspend fun File.copyRecursivelyCancellable(target: File) {
    currentCoroutineContext().ensureActive()
    if (!exists()) throw NoSuchFileException(this)
    if (isDirectory) {
        if (target.exists() && !target.isDirectory) throw FileAlreadyExistsException(this, target, "Target is not a directory")
        if (!target.exists() && !target.mkdirs()) throw IOException("Cannot create directory: ${target.absolutePath}")
        listFiles().orEmpty().forEach { child ->
            currentCoroutineContext().ensureActive()
            child.copyRecursivelyCancellable(File(target, child.name))
        }
    } else {
        copyToCancellable(target, overwrite = false)
    }
}

suspend fun File.deleteRecursivelyCancellable() {
    currentCoroutineContext().ensureActive()
    if (isDirectory) {
        listFiles().orEmpty().forEach { child ->
            currentCoroutineContext().ensureActive()
            child.deleteRecursivelyCancellable()
        }
    }
    if (exists() && !delete()) throw IOException("Cannot delete: $absolutePath")
}