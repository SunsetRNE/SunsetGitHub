package com.Sunset.REN.GitHub.ui.filemanager.controller

import android.content.Context
import com.Sunset.REN.GitHub.ui.filemanager.RootAdvancedActionDialog
import com.Sunset.REN.GitHub.ui.filemanager.RootOperationSafetyDialog

/** Owns Root-related UI safety surfaces so Fragment does not duplicate privileged-action policy. */
class FileManagerRootActionController(
    private val contextProvider: () -> Context,
    private val activePathProvider: () -> String,
    private val copyReport: (title: String, body: String) -> Unit
) {
    fun showAdvancedActionUnavailable(actionTitle: String, path: String = activePathProvider()) {
        RootAdvancedActionDialog.showUnavailable(
            context = contextProvider(),
            actionTitle = actionTitle,
            path = path,
            onCopyReport = { report -> copyReport(actionTitle, report) }
        )
    }

    fun showWriteBlocked(actionTitle: String, path: String) {
        RootOperationSafetyDialog.showBlockedWrite(
            context = contextProvider(),
            actionTitle = actionTitle,
            path = path,
            onCopyReport = { report -> copyReport(actionTitle, report) }
        )
    }
}
