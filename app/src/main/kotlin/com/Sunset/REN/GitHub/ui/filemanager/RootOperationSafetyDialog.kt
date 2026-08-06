package com.Sunset.REN.GitHub.ui.filemanager

import android.content.Context
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.filemanager.root.RootPathPolicy
import com.Sunset.REN.GitHub.ui.common.ComposeDialogAction
import com.Sunset.REN.GitHub.ui.common.showComposeMessageDialog

/**
 * Explicit second-confirmation surface for privileged Root operations.
 *
 * The current file manager still keeps Root write operations disabled, but this
 * dialog is the reusable confirmation boundary that every future chmod/chown/
 * delete/write/remount flow must pass through before execution.
 */
object RootOperationSafetyDialog {
    fun showBlockedWrite(
        context: Context,
        actionTitle: String,
        path: String,
        onCopyReport: (String) -> Unit
    ) {
        val normalizedPath = path.removePrefix("root://").ifBlank { path }
        val report = RootPathPolicy.writeOperationBlockedReport(actionTitle, normalizedPath)
        showComposeMessageDialog(
            context = context,
            title = context.getString(R.string.local_file_manager_root_safety_title),
            message = report,
            negativeText = context.getString(android.R.string.cancel),
            neutralAction = ComposeDialogAction(
                text = context.getString(R.string.local_file_manager_copy_path_label),
                onClick = { onCopyReport(report) }
            )
        )
    }
}
