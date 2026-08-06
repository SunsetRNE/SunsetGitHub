package com.Sunset.REN.GitHub.ui.filemanager

import android.content.Context
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.filemanager.FileEntryType
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerEntry
import com.Sunset.REN.GitHub.domain.filemanager.FileSizeFormatter
import java.text.DateFormat
import java.util.Date

/** Formats user-visible single-entry property facts for file-manager panels. */
object FileEntryPropertiesFormatter {
    fun format(context: Context, entry: FileManagerEntry): String {
        val yes = context.getString(R.string.local_file_manager_properties_yes)
        val no = context.getString(R.string.local_file_manager_properties_no)
        val typeName = typeDisplayName(context, entry)
        val size = if (entry.type == FileEntryType.Directory) {
            context.getString(R.string.local_file_manager_properties_size_directory)
        } else {
            entry.sizeBytes?.let { sizeBytes ->
                "${FileSizeFormatter.format(sizeBytes)} (${context.getString(R.string.local_file_manager_properties_size_bytes, sizeBytes)})"
            } ?: context.getString(R.string.local_file_manager_properties_unknown)
        }
        val modifiedAt = entry.modifiedAtMillis?.let { millis ->
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))
        } ?: context.getString(R.string.local_file_manager_properties_unknown)
        val lines = mutableListOf(
            context.getString(R.string.local_file_manager_properties_path, entry.displayPath),
            context.getString(R.string.local_file_manager_properties_type, typeName),
            context.getString(R.string.local_file_manager_properties_size, size),
            context.getString(R.string.local_file_manager_properties_modified, modifiedAt),
            context.getString(R.string.local_file_manager_properties_readable, if (entry.capabilities.canRead) yes else no),
            context.getString(R.string.local_file_manager_properties_writable, if (entry.capabilities.canWrite) yes else no),
            context.getString(R.string.local_file_manager_properties_uploadable, if (entry.capabilities.canUpload) yes else no),
            context.getString(R.string.local_file_manager_properties_access_content, if (entry.capabilities.canAccessContent) yes else no),
            context.getString(R.string.local_file_manager_properties_editable, if (entry.capabilities.canEditAsText) yes else no),
            context.getString(R.string.local_file_manager_properties_compilable, if (entry.compileCapability != null) yes else no)
        )
        entry.compileCapability?.let { capability ->
            lines += context.getString(
                R.string.local_file_manager_properties_compile_target,
                capability.language,
                capability.mode.name,
                capability.toolHint
            )
        }
        return lines.joinToString("\n")
    }

    fun typeDisplayName(context: Context, entry: FileManagerEntry): String {
        val resId = when (entry.type) {
            FileEntryType.Parent -> R.string.local_file_preview_type_directory
            FileEntryType.Directory -> R.string.local_file_preview_type_directory
            FileEntryType.Text -> R.string.local_file_preview_type_text
            FileEntryType.Markdown -> R.string.local_file_preview_type_markdown
            FileEntryType.Code -> R.string.local_file_preview_type_code
            FileEntryType.Image -> R.string.local_file_preview_type_image
            FileEntryType.Archive -> R.string.local_file_preview_type_archive
            FileEntryType.Apk -> R.string.local_file_preview_type_apk
            FileEntryType.Binary -> R.string.local_file_preview_type_binary
            FileEntryType.Unknown -> R.string.local_file_preview_type_unknown
        }
        return context.getString(resId)
    }
}