package com.Sunset.REN.GitHub.ui.filemanager

import android.content.Context
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.filemanager.root.RootPathPolicy

/** Confirmation / explanation surface for privileged Root actions before real execution is enabled. */
object RootAdvancedActionDialog {
    fun showUnavailable(
        context: Context,
        actionTitle: String,
        path: String,
        onCopyReport: (String) -> Unit
    ) {
        val normalizedPath = path.removePrefix("root://").ifBlank { path }
        val message = RootPathPolicy.advancedActionUnavailableReport(actionTitle, normalizedPath)
        LocalFileManagerDialogScaffold.showPlainScrollable(
            context = context,
            title = actionTitle,
            message = message,
            positiveText = android.R.string.ok,
            negativeText = android.R.string.cancel,
            neutralText = R.string.local_file_manager_copy_path_label,
            onNeutral = { onCopyReport(message) }
        )
    }
}