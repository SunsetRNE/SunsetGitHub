package com.Sunset.REN.GitHub.ui.filemanager

import android.app.Dialog
import android.content.Context
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.filemanager.ArchiveFormatResolver
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerEntry
import com.Sunset.REN.GitHub.domain.filemanager.FileSizeFormatter
import com.Sunset.REN.GitHub.domain.filemanager.OperationSafety
import java.text.DateFormat
import java.util.Date

/**
 * APK fact panel plus install/signature entry placeholders.
 *
 * This deliberately stays factual: it describes install strategy and validation
 * slots without performing privileged operations or applying a global risk stamp.
 */
object ApkInstallAndSignaturePanel {
    fun show(
        context: Context,
        entry: FileManagerEntry,
        typeName: String,
        onCopyPath: () -> Unit
    ): Dialog {
        val size = entry.sizeBytes?.let { FileSizeFormatter.format(it) }
            ?: context.getString(R.string.local_file_manager_properties_unknown)
        val modified = entry.modifiedAtMillis?.let { millis ->
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))
        } ?: context.getString(R.string.local_file_manager_properties_unknown)
        val archiveSupport = if (ArchiveFormatResolver.resolve(entry.name)?.supportsExtraction == true) {
            "可浏览/可解压"
        } else {
            "仅基础信息"
        }
        return LocalFileManagerDialogScaffold.showPlainScrollable(
            context = context,
            title = "Apk信息",
            message = OperationSafety.apkInstallAndSignatureFacts(
                entry = entry,
                typeName = typeName,
                size = size,
                modified = modified,
                archiveSupport = archiveSupport
            ),
            positiveText = android.R.string.ok,
            neutralText = R.string.local_file_manager_copy_path,
            onNeutral = onCopyPath
        )
    }
}