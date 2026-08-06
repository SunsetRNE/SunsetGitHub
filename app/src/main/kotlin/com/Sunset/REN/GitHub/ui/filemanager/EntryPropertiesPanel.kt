package com.Sunset.REN.GitHub.ui.filemanager

import android.content.Context
import com.Sunset.REN.GitHub.R

/** Single-entry property panel. */
object EntryPropertiesPanel {
    fun show(
        context: Context,
        title: String,
        message: String,
        onCopyPath: () -> Unit
    ) {
        LocalFileManagerDialogScaffold.showPlainScrollable(
            context = context,
            title = title,
            message = message,
            positiveText = android.R.string.ok,
            neutralText = R.string.local_file_manager_copy_path,
            onNeutral = onCopyPath
        )
    }
}