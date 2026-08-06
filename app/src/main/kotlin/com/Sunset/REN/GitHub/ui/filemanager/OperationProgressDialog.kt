package com.Sunset.REN.GitHub.ui.filemanager

import android.content.Context
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.ui.common.ComposeProgressDialog
import com.Sunset.REN.GitHub.ui.common.showComposeProgressDialog

/** Shared progress dialog used by local-file-manager batch and blocking operations. */
class OperationProgressDialog private constructor(
    private val dialog: ComposeProgressDialog,
    private val context: Context
) {
    fun update(title: String, completedCount: Int, totalCount: Int) {
        val safeTotal = totalCount.coerceAtLeast(1)
        val safeCompleted = completedCount.coerceIn(0, safeTotal)
        dialog.update(
            title = title,
            message = context.getString(R.string.local_file_manager_batch_operation_processing_progress, safeCompleted, totalCount),
            completedCount = safeCompleted,
            totalCount = safeTotal
        )
    }

    fun dismiss() {
        dialog.dismiss()
    }

    companion object {
        fun showDeterminate(
            context: Context,
            title: String,
            totalCount: Int,
            onCancel: (() -> Unit)? = null
        ): OperationProgressDialog {
            val progressDialog = showComposeProgressDialog(
                context = context,
                title = title,
                message = context.getString(R.string.local_file_manager_batch_operation_preparing, totalCount.coerceAtLeast(0)),
                totalCount = totalCount.coerceAtLeast(1),
                onCancel = onCancel
            )
            return OperationProgressDialog(progressDialog, context)
        }

        fun showIndeterminate(
            context: Context,
            title: String,
            message: String,
            onCancel: (() -> Unit)? = null
        ): OperationProgressDialog {
            val progressDialog = showComposeProgressDialog(
                context = context,
                title = title,
                message = message,
                onCancel = onCancel
            )
            return OperationProgressDialog(progressDialog, context)
        }
    }
}