package com.Sunset.REN.GitHub.ui.filemanager

import android.content.Context
import com.Sunset.REN.GitHub.R

/** Scrollable failure report dialog for batch and single operation errors. */
object OperationFailureReportDialog {
    fun show(
        context: Context,
        title: String,
        summary: String,
        failureLines: List<String>,
        onCopyReport: (body: String) -> Unit
    ) {
        val body = buildReportBody(summary, failureLines)
        LocalFileManagerDialogScaffold.showPlainScrollable(
            context = context,
            title = title,
            message = body,
            positiveText = android.R.string.ok,
            neutralText = R.string.local_file_manager_copy_path,
            onNeutral = { onCopyReport(body) }
        )
    }

    fun buildReportBody(summary: String, failureLines: List<String>): String {
        return buildString {
            append(summary)
            if (failureLines.isNotEmpty()) {
                append("\n\n")
                append(failureLines.joinToString("\n\n"))
            }
        }
    }
}