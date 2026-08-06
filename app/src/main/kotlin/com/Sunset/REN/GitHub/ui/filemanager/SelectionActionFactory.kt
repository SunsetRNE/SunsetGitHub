package com.Sunset.REN.GitHub.ui.filemanager

import android.content.Context
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.filemanager.ArchiveFormatResolver
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerEntry
import com.Sunset.REN.GitHub.domain.filemanager.capability.FileActionVisibilityPolicy

/** Builds selected-entry action lists while keeping Fragment callbacks explicit. */
object SelectionActionFactory {
    fun build(
        context: Context,
        entries: List<FileManagerEntry>,
        onOpenEntry: (FileManagerEntry) -> Unit,
        onUnzipEntry: (FileManagerEntry) -> Unit,
        onUnzipEntriesToTarget: (List<FileManagerEntry>) -> Unit,
        onShowProperties: (FileManagerEntry) -> Unit,
        onCopyPath: (FileManagerEntry) -> Unit,
        onRename: (FileManagerEntry) -> Unit,
        onCopy: () -> Unit,
        onCopyTo: () -> Unit,
        onZip: () -> Unit,
        onConvertToText: () -> Unit,
        onMoveUp: () -> Unit,
        onMoveTo: () -> Unit,
        onDelete: () -> Unit
    ): List<SelectionActionItem> {
        val actions = mutableListOf<SelectionActionItem>()
        val capabilities = FileActionVisibilityPolicy.selectionCapabilities(entries)
        fun add(label: String, onClick: () -> Unit) {
            actions += SelectionActionItem(label, onClick)
        }
        if (entries.size == 1) {
            val entry = entries.first()
            add(context.getString(R.string.local_file_manager_entry_access_content)) { onOpenEntry(entry) }
            if (ArchiveFormatResolver.resolve(entry.name)?.supportsExtraction == true) {
                add(context.getString(R.string.local_file_manager_unzip)) { onUnzipEntry(entry) }
                add(context.getString(R.string.local_file_manager_unzip_to_target)) { onUnzipEntriesToTarget(entries) }
            }
            add(context.getString(R.string.local_file_manager_entry_properties)) { onShowProperties(entry) }
            add(context.getString(R.string.local_file_manager_copy_path)) { onCopyPath(entry) }
            if (entry.capabilities.canRename) {
                add(context.getString(R.string.local_file_manager_rename)) { onRename(entry) }
            }
        }
        if (capabilities.canCopy) {
            add(context.getString(R.string.local_file_manager_selection_copy), onCopy)
            add(context.getString(R.string.local_file_manager_copy_to_target), onCopyTo)
        }
        if (capabilities.canCompress) add(context.getString(R.string.local_file_manager_selection_zip), onZip)
        if (capabilities.canPreview) add(context.getString(R.string.local_file_manager_selection_convert_to_txt), onConvertToText)
        if (capabilities.canExtract) add(context.getString(R.string.local_file_manager_unzip_to_target)) { onUnzipEntriesToTarget(entries) }
        if (capabilities.canMove) {
            add(context.getString(R.string.local_file_manager_selection_move_up), onMoveUp)
            add(context.getString(R.string.local_file_manager_move_to_target), onMoveTo)
        }
        if (capabilities.canDelete) add(context.getString(R.string.local_file_manager_delete), onDelete)
        return actions
    }
}