package com.Sunset.REN.GitHub.domain.filemanager.operation

import com.Sunset.REN.GitHub.domain.filemanager.FileManagerEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow

class LegacyFileOperationRunner {
    fun <T> runSingle(
        title: String,
        message: String,
        execute: suspend () -> T,
        summarize: (T) -> String
    ): Flow<FileOperationEvent> = channelFlow {
        send(FileOperationEvent.Started(title))
        if (message.isNotBlank()) {
            send(FileOperationEvent.Progress(0L, null, message))
        }
        try {
            val result = execute()
            currentCoroutineContext().ensureActive()
            send(FileOperationEvent.Completed(summarize(result)))
        } catch (cancelled: CancellationException) {
            send(FileOperationEvent.Cancelled)
            throw cancelled
        } catch (error: Throwable) {
            send(FileOperationEvent.Failed(error.message ?: "文件操作失败", error))
        }
    }

    fun <T> runEntryBatch(
        title: String,
        entries: List<FileManagerEntry>,
        executeBatch: suspend (entries: List<FileManagerEntry>, onProgress: (completed: Int, total: Int) -> Unit) -> T,
        summarize: (T) -> String
    ): Flow<FileOperationEvent> = channelFlow {
        if (entries.isEmpty()) {
            send(FileOperationEvent.Failed("没有可处理的条目。"))
            return@channelFlow
        }
        send(FileOperationEvent.Started(title))
        try {
            val result = executeBatch(entries) { completed, total ->
                trySend(FileOperationEvent.Progress(completed.toLong(), total.toLong(), "$completed/$total"))
            }
            currentCoroutineContext().ensureActive()
            send(FileOperationEvent.Completed(summarize(result)))
        } catch (cancelled: CancellationException) {
            send(FileOperationEvent.Cancelled)
            throw cancelled
        } catch (error: Throwable) {
            send(FileOperationEvent.Failed(error.message ?: "文件操作失败", error))
        }
    }
}
