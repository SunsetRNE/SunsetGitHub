package com.Sunset.REN.GitHub.ui.filemanager.preview

import android.content.Context
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.filemanager.ArchivePreview
import com.Sunset.REN.GitHub.domain.filemanager.FileSizeFormatter

object ArchivePreviewTextFormatter {
    fun format(context: Context, preview: ArchivePreview, fallbackSizeBytes: Long): String {
        return buildString {
            appendLine(context.getString(R.string.local_file_preview_archive_summary_title))
            appendLine(context.getString(R.string.local_file_preview_archive_name, preview.displayName))
            appendLine(context.getString(R.string.local_file_preview_archive_size, FileSizeFormatter.format(preview.sizeBytes ?: fallbackSizeBytes.coerceAtLeast(0L))))
            appendLine(context.getString(R.string.local_file_preview_archive_entry_count, preview.entryCount))
            appendLine(context.getString(R.string.local_file_preview_archive_file_count, preview.fileCount))
            appendLine(context.getString(R.string.local_file_preview_archive_directory_count, preview.directoryCount))
            appendLine()
            appendLine(context.getString(R.string.local_file_preview_archive_entries_title))
            appendLine(context.getString(R.string.local_file_preview_archive_entry_open_hint))
            if (preview.entries.isEmpty()) {
                appendLine(context.getString(R.string.local_file_preview_archive_empty))
            } else {
                preview.entries.forEach { entry ->
                    val marker = if (entry.isDirectory) "[D]" else "[F]"
                    val sizeText = entry.sizeBytes?.let { FileSizeFormatter.format(it) }.orEmpty()
                    val typeText = entry.type.name
                    val previewText = if (entry.canPreviewText) context.getString(R.string.local_file_preview_archive_entry_previewable) else ""
                    val compileText = entry.compileCapability?.let { capability ->
                        context.getString(
                            R.string.local_file_preview_compile_capability_inline,
                            capability.language,
                            capability.mode.name,
                            capability.toolHint
                        )
                    }.orEmpty()
                    appendLine(listOf(marker, entry.name, typeText, sizeText, previewText, compileText).filter { it.isNotBlank() }.joinToString("  "))
                }
                if (preview.truncated) {
                    appendLine(context.getString(R.string.local_file_preview_archive_truncated))
                }
            }
        }
    }
}