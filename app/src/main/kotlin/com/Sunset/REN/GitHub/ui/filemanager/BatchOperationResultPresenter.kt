package com.Sunset.REN.GitHub.ui.filemanager

import android.content.Context
import android.widget.Toast
import com.Sunset.REN.GitHub.R

/** Presents batch operation results as success toasts or detailed failure reports. */
object BatchOperationResultPresenter {
    fun showDelete(
        context: Context,
        result: BatchDeleteResult,
        onCopyReport: (title: String, body: String) -> Unit
    ) {
        if (result.failures.isEmpty()) {
            Toast.makeText(
                context,
                context.resources.getQuantityString(R.plurals.local_file_manager_batch_delete_success, result.successCount, result.successCount),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val summary = context.getString(R.string.local_file_manager_batch_delete_partial_failed, result.successCount, result.failures.size)
        showFailures(
            context = context,
            title = context.getString(R.string.local_file_manager_batch_delete_failures_title),
            summary = summary,
            failureLines = result.failures.map { failure ->
                context.getString(R.string.local_file_manager_batch_delete_failure_item, failure.entry.name, failure.message)
            },
            onCopyReport = onCopyReport
        )
    }

    fun showCopy(
        context: Context,
        result: BatchCopyResult,
        onCopyReport: (title: String, body: String) -> Unit
    ) {
        if (result.failures.isEmpty()) {
            Toast.makeText(
                context,
                context.resources.getQuantityString(R.plurals.local_file_manager_batch_copy_success, result.successCount, result.successCount),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val summary = context.getString(R.string.local_file_manager_batch_copy_partial_failed, result.successCount, result.failures.size)
        showFailures(
            context = context,
            title = context.getString(R.string.local_file_manager_batch_copy_failures_title),
            summary = summary,
            failureLines = result.failures.map { failure ->
                context.getString(R.string.local_file_manager_batch_copy_failure_item, failure.entry.name, failure.message)
            },
            onCopyReport = onCopyReport
        )
    }

    fun showMove(
        context: Context,
        result: BatchMoveResult,
        onCopyReport: (title: String, body: String) -> Unit
    ) {
        if (result.failures.isEmpty()) {
            Toast.makeText(
                context,
                context.resources.getQuantityString(R.plurals.local_file_manager_batch_move_success, result.successCount, result.successCount),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val summary = context.getString(R.string.local_file_manager_batch_move_partial_failed, result.successCount, result.failures.size)
        showFailures(
            context = context,
            title = context.getString(R.string.local_file_manager_batch_move_failures_title),
            summary = summary,
            failureLines = result.failures.map { failure ->
                context.getString(R.string.local_file_manager_batch_move_failure_item, failure.entry.name, failure.message)
            },
            onCopyReport = onCopyReport
        )
    }

    fun showRestore(
        context: Context,
        result: BatchRestoreResult,
        onCopyReport: (title: String, body: String) -> Unit
    ) {
        if (result.failures.isEmpty()) {
            Toast.makeText(
                context,
                context.resources.getQuantityString(R.plurals.local_file_manager_batch_restore_success, result.successCount, result.successCount),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val summary = context.getString(R.string.local_file_manager_batch_restore_partial_failed, result.successCount, result.failures.size)
        showFailures(
            context = context,
            title = context.getString(R.string.local_file_manager_batch_restore_failures_title),
            summary = summary,
            failureLines = result.failures.map { failure ->
                context.getString(R.string.local_file_manager_batch_restore_failure_item, failure.entry.name, failure.message)
            },
            onCopyReport = onCopyReport
        )
    }

    fun showTextExport(
        context: Context,
        result: BatchTextExportResult,
        onCopyReport: (title: String, body: String) -> Unit
    ) {
        if (result.failures.isEmpty()) {
            Toast.makeText(
                context,
                context.resources.getQuantityString(R.plurals.local_file_manager_batch_text_export_success, result.successCount, result.successCount),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val summary = context.getString(R.string.local_file_manager_batch_text_export_partial_failed, result.successCount, result.failures.size)
        showFailures(
            context = context,
            title = context.getString(R.string.local_file_manager_batch_text_export_failures_title),
            summary = summary,
            failureLines = result.failures.map { failure ->
                context.getString(R.string.local_file_manager_batch_text_export_failure_item, failure.entry.name, failure.message)
            },
            onCopyReport = onCopyReport
        )
    }

    fun showUnzip(
        context: Context,
        result: BatchUnzipResult,
        onCopyReport: (title: String, body: String) -> Unit
    ) {
        if (result.failures.isEmpty()) {
            Toast.makeText(
                context,
                context.resources.getQuantityString(R.plurals.local_file_manager_batch_unzip_success, result.successCount, result.successCount),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        showFailures(
            context = context,
            title = context.getString(R.string.local_file_manager_unzip_failed),
            summary = context.getString(R.string.local_file_manager_batch_unzip_partial_failed, result.successCount, result.failures.size),
            failureLines = result.failures.map { failure -> "${failure.entry.name}\n${failure.message}" },
            onCopyReport = onCopyReport
        )
    }

    fun showZip(
        context: Context,
        result: BatchZipResult,
        onCopyReport: (title: String, body: String) -> Unit
    ) {
        if (result.failures.isEmpty()) {
            Toast.makeText(
                context,
                context.getString(R.string.local_file_manager_batch_zip_success, result.archiveName),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val summary = context.getString(
            R.string.local_file_manager_batch_zip_partial_failed,
            result.successCount,
            result.failures.size,
            result.archiveName.ifBlank { context.getString(R.string.local_file_manager_batch_zip_default_name) }
        )
        showFailures(
            context = context,
            title = context.getString(R.string.local_file_manager_batch_zip_failures_title),
            summary = summary,
            failureLines = result.failures.map { failure ->
                context.getString(R.string.local_file_manager_batch_zip_failure_item, failure.entry.name, failure.message)
            },
            onCopyReport = onCopyReport
        )
    }

    fun showRename(
        context: Context,
        result: BatchRenameResult,
        onCopyReport: (title: String, body: String) -> Unit
    ) {
        if (result.failures.isEmpty()) {
            Toast.makeText(
                context,
                context.resources.getQuantityString(R.plurals.local_file_manager_batch_rename_success, result.successCount, result.successCount),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val summary = context.getString(R.string.local_file_manager_batch_rename_partial_failed, result.successCount, result.failures.size)
        showFailures(
            context = context,
            title = context.getString(R.string.local_file_manager_batch_rename_failures_title),
            summary = summary,
            failureLines = result.failures.map { failure ->
                context.getString(R.string.local_file_manager_batch_rename_failure_item, failure.entry.name, failure.message)
            },
            onCopyReport = onCopyReport
        )
    }

    fun showSeparateZip(
        context: Context,
        result: BatchSeparateZipResult,
        onCopyReport: (title: String, body: String) -> Unit
    ) {
        if (result.failures.isEmpty()) {
            Toast.makeText(
                context,
                context.resources.getQuantityString(R.plurals.local_file_manager_batch_zip_each_success, result.successCount, result.successCount),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val summary = context.getString(R.string.local_file_manager_batch_zip_each_partial_failed, result.successCount, result.failures.size)
        showFailures(
            context = context,
            title = context.getString(R.string.local_file_manager_batch_zip_failures_title),
            summary = summary,
            failureLines = result.failures.map { failure ->
                context.getString(R.string.local_file_manager_batch_zip_failure_item, failure.entry.name, failure.message)
            },
            onCopyReport = onCopyReport
        )
    }

    private fun showFailures(
        context: Context,
        title: String,
        summary: String,
        failureLines: List<String>,
        onCopyReport: (title: String, body: String) -> Unit
    ) {
        OperationFailureReportDialog.show(
            context = context,
            title = title,
            summary = summary,
            failureLines = failureLines,
            onCopyReport = { body -> onCopyReport(title, body) }
        )
    }
}