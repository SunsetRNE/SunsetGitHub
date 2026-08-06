package com.Sunset.REN.GitHub.ui.filemanager.preview

import android.content.Context
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.filemanager.ApkPreview
import com.Sunset.REN.GitHub.domain.filemanager.DexFilePreview
import com.Sunset.REN.GitHub.domain.filemanager.FileSizeFormatter

object ApkPreviewTextFormatter {
    fun format(context: Context, preview: ApkPreview, fallbackSizeBytes: Long): String {
        return buildString {
            appendLine(context.getString(R.string.local_file_preview_apk_summary_title))
            appendLine(context.getString(R.string.local_file_preview_archive_name, preview.displayName))
            appendLine(context.getString(R.string.local_file_preview_archive_size, FileSizeFormatter.format(preview.sizeBytes ?: fallbackSizeBytes.coerceAtLeast(0L))))
            appendLine(context.getString(R.string.local_file_preview_archive_entry_count, preview.entryCount))
            appendLine(context.getString(R.string.local_file_preview_apk_manifest, yesNo(context, preview.hasManifest)))
            appendLine(context.getString(R.string.local_file_preview_apk_classes_dex, yesNo(context, preview.hasClassesDex), preview.dexCount))
            appendLine(context.getString(R.string.local_file_preview_apk_resources_arsc, yesNo(context, preview.hasResourcesArsc)))
            appendLine(context.getString(R.string.local_file_preview_apk_native_architectures, preview.nativeArchitectures.joinToString().ifBlank { context.getString(R.string.local_file_preview_none) }))
            appendLine(context.getString(R.string.local_file_preview_apk_certificates, preview.certificateEntries.joinToString().ifBlank { context.getString(R.string.local_file_preview_none) }))
            appendLine()
            appendLine(context.getString(R.string.local_file_preview_apk_dex_title))
            if (preview.dexFiles.isEmpty() && preview.dexParseFailures.isEmpty()) {
                appendLine(context.getString(R.string.local_file_preview_none))
            } else {
                preview.dexFiles.forEach { dex ->
                    appendDexPreview(context, dex)
                }
                preview.dexParseFailures.forEach { failure ->
                    appendLine(context.getString(R.string.local_file_preview_apk_dex_parse_failed, failure))
                }
            }
            appendLine()
            appendLine(context.getString(R.string.local_file_preview_archive_entries_title))
            if (preview.entries.isEmpty()) {
                appendLine(context.getString(R.string.local_file_preview_archive_empty))
            } else {
                preview.entries.forEach { entry ->
                    val marker = if (entry.isDirectory) "[D]" else "[F]"
                    val sizeText = entry.sizeBytes?.let { FileSizeFormatter.format(it) }.orEmpty()
                    appendLine(listOf(marker, entry.name, sizeText).filter { it.isNotBlank() }.joinToString("  "))
                }
                if (preview.truncated) {
                    appendLine(context.getString(R.string.local_file_preview_archive_truncated))
                }
            }
        }
    }

    private fun StringBuilder.appendDexPreview(context: Context, dex: DexFilePreview) {
        appendLine(context.getString(R.string.local_file_preview_apk_dex_file, dex.name))
        appendLine(
            context.getString(
                R.string.local_file_preview_apk_dex_header_stats,
                dex.version,
                FileSizeFormatter.format(dex.fileSizeBytes),
                dex.headerSizeBytes,
                dex.endianTag
            )
        )
        appendLine(
            context.getString(
                R.string.local_file_preview_apk_dex_id_stats,
                dex.stringCount,
                dex.typeCount,
                dex.protoCount,
                dex.fieldCount,
                dex.methodCount,
                dex.classCount
            )
        )
    }

    private fun yesNo(context: Context, value: Boolean): String {
        return context.getString(if (value) R.string.local_file_manager_properties_yes else R.string.local_file_manager_properties_no)
    }
}
