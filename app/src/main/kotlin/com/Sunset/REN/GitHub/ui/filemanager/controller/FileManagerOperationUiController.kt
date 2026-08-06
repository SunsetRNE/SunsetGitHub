package com.Sunset.REN.GitHub.ui.filemanager.controller

import android.content.Context
import android.widget.Toast
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerEntry
import com.Sunset.REN.GitHub.domain.filemanager.operation.FileOperationEvent
import com.Sunset.REN.GitHub.domain.filemanager.operation.LegacyFileOperationRunner
import com.Sunset.REN.GitHub.ui.filemanager.OperationProgressDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/** Bridges legacy ViewModel operations into the new operation-event UI model. */
class FileManagerOperationUiController(
    private val contextProvider: () -> Context,
    private val coroutineScopeProvider: () -> CoroutineScope,
    private val legacyOperationRunner: LegacyFileOperationRunner = LegacyFileOperationRunner(),
    private val showDeterminateProgress: (title: String, totalCount: Int, onCancel: (() -> Unit)?) -> OperationProgressDialog,
    private val showIndeterminateProgress: (title: String, message: String, onCancel: (() -> Unit)?) -> OperationProgressDialog
) {
    private val runningJobs = mutableSetOf<Job>()

    fun <T> runSingle(
        title: String,
        message: String,
        execute: suspend () -> T,
        summarize: (T) -> String,
        onSuccess: (T) -> Unit,
        onFailure: (String) -> Unit
    ) {
        var job: Job? = null
        val progress = showIndeterminateProgress(title, message) { job?.cancel() }
        job = coroutineScopeProvider().launch {
            var result: T? = null
            legacyOperationRunner.runSingle(
                title = title,
                message = message,
                execute = { execute().also { result = it } },
                summarize = summarize
            ).collect { event ->
                when (event) {
                    is FileOperationEvent.Started -> Unit
                    is FileOperationEvent.Progress -> Unit
                    is FileOperationEvent.Completed -> {
                        progress.dismiss()
                        result?.let(onSuccess)
                    }
                    is FileOperationEvent.Failed -> {
                        progress.dismiss()
                        onFailure(event.message)
                    }
                    FileOperationEvent.Cancelled -> progress.dismiss()
                    is FileOperationEvent.ConflictDetected -> Unit
                }
            }
        }
        track(job)
    }

    fun <T> runEntryBatch(
        title: String,
        entries: List<FileManagerEntry>,
        executeBatch: suspend (entries: List<FileManagerEntry>, onProgress: (completed: Int, total: Int) -> Unit) -> T,
        summarize: (T) -> String,
        onCompleted: (T) -> Unit,
        onFailed: (String) -> Unit = { message -> Toast.makeText(contextProvider(), message, Toast.LENGTH_SHORT).show() }
    ) {
        var job: Job? = null
        val progress = showDeterminateProgress(title, entries.size) { job?.cancel() }
        job = coroutineScopeProvider().launch {
            var batchResult: T? = null
            legacyOperationRunner.runEntryBatch(
                title = title,
                entries = entries,
                executeBatch = { batch, onProgress ->
                    executeBatch(batch, onProgress).also { batchResult = it }
                },
                summarize = summarize
            ).collect { event ->
                when (event) {
                    is FileOperationEvent.Started -> Unit
                    is FileOperationEvent.Progress -> {
                        val total = event.total?.toInt() ?: entries.size
                        progress.update(title, event.current.toInt(), total)
                    }
                    is FileOperationEvent.Completed -> {
                        progress.dismiss()
                        batchResult?.let(onCompleted)
                    }
                    is FileOperationEvent.Failed -> {
                        progress.dismiss()
                        onFailed(event.message)
                    }
                    FileOperationEvent.Cancelled -> progress.dismiss()
                    is FileOperationEvent.ConflictDetected -> Unit
                }
            }
        }
        track(job)
    }

    fun cancelAll() {
        runningJobs.toList().forEach { it.cancel() }
        runningJobs.clear()
    }

    private fun track(job: Job) {
        runningJobs += job
        job.invokeOnCompletion { runningJobs -= job }
    }
}
