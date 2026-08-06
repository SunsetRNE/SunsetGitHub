package com.Sunset.REN.GitHub.ui.filemanager

import android.content.Context
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerEntry
import com.Sunset.REN.GitHub.domain.filemanager.FileToolAction

/** Renders a Compose-first "打开方式" tool picker from legacy file-manager entry points. */
object OpenWithToolGridDialog {
    fun show(
        context: Context,
        entry: FileManagerEntry,
        tools: List<FileToolAction>,
        onToolSelected: (FileManagerEntry, FileToolAction) -> Unit
    ) {
        SelectionActionSheetDialog.show(
            context = context,
            title = context.getString(R.string.local_file_manager_open_with_title),
            actions = tools.map { tool ->
                val label = buildString {
                    append(tool.title)
                    if (tool.singleWindow) append(" •")
                    if (!tool.implemented) append(" · 未实现")
                }
                SelectionActionItem(label = label) {
                    onToolSelected(entry, tool)
                }
            }
        )
    }
}