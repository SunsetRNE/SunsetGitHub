package com.Sunset.REN.GitHub.ui.filemanager

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerEntry
import com.Sunset.REN.GitHub.domain.filemanager.OperationSafety
import com.Sunset.REN.GitHub.domain.filemanager.RecycleBinSettings
import com.Sunset.REN.GitHub.ui.common.showComposeDialog
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens

/**
 * Plain confirmation dialogs for file-manager write/destructive operations.
 *
 * This keeps operation copy and button wiring out of LocalFileManagerFragment;
 * actual execution still stays with the Fragment/ViewModel boundary.
 */
object FileOperationConfirmDialog {
    fun showUnzipEntry(
        context: Context,
        entry: FileManagerEntry,
        formatName: String,
        onConfirm: () -> Unit
    ) {
        show(
            context = context,
            title = context.getString(R.string.local_file_manager_unzip),
            message = OperationSafety.archiveExtractionPlan(entry.name, formatName),
            positiveText = R.string.local_file_manager_unzip,
            onConfirm = onConfirm
        )
    }

    fun showUnzipToTarget(
        context: Context,
        targetPath: String,
        archiveCount: Int,
        onConfirm: () -> Unit
    ) {
        show(
            context = context,
            title = context.getString(R.string.local_file_manager_unzip_to_target),
            message = OperationSafety.multiArchiveExtractionPlan(targetPath, archiveCount),
            positiveText = R.string.local_file_manager_unzip,
            onConfirm = onConfirm
        )
    }

    fun showZipSelected(
        context: Context,
        directoryPath: String,
        entryCount: Int,
        onConfirm: () -> Unit
    ) {
        show(
            context = context,
            title = context.getString(R.string.local_file_manager_selection_zip),
            message = OperationSafety.zipPlan(directoryPath, entryCount),
            positiveText = R.string.local_file_manager_selection_zip,
            onConfirm = onConfirm
        )
    }

    fun showTextExport(
        context: Context,
        directoryPath: String,
        entryCount: Int,
        onConfirm: () -> Unit
    ) {
        show(
            context = context,
            title = context.getString(R.string.local_file_manager_selection_convert_to_txt),
            message = OperationSafety.textExportPlan(directoryPath, entryCount),
            positiveText = R.string.local_file_manager_selection_convert_to_txt,
            onConfirm = onConfirm
        )
    }

    fun showTextExportWithDirectorySkip(
        context: Context,
        directoryCount: Int,
        onConfirm: () -> Unit
    ) {
        show(
            context = context,
            title = context.getString(R.string.local_file_manager_selection_convert_to_txt),
            message = OperationSafety.textExportDirectorySkipPlan(directoryCount),
            positiveText = R.string.local_file_manager_selection_convert_to_txt,
            onConfirm = onConfirm
        )
    }

    fun showDeleteSelected(
        context: Context,
        entries: List<FileManagerEntry>,
        recycleBinSettings: RecycleBinSettings,
        onConfirm: (moveToRecycleBin: Boolean) -> Unit
    ) {
        val title = if (entries.size == 1) {
            context.getString(R.string.local_file_manager_delete_confirm_title)
        } else {
            context.getString(R.string.local_file_manager_batch_delete_confirm_title)
        }
        val baseMessage = if (entries.size == 1) {
            context.getString(R.string.local_file_manager_delete_confirm_message, entries.first().name)
        } else {
            context.resources.getQuantityString(
                R.plurals.local_file_manager_batch_delete_confirm_message,
                entries.size,
                entries.size
            )
        }
        showComposeDialog(context) { dismiss ->
            DeleteConfirmContent(
                title = title,
                message = baseMessage,
                recycleBinSettings = recycleBinSettings,
                cancelText = context.getString(android.R.string.cancel),
                confirmText = context.getString(R.string.local_file_manager_delete),
                moveToRecycleBinText = context.getString(R.string.local_file_manager_delete_move_to_recycle_bin),
                permanentWarningText = context.getString(R.string.local_file_manager_delete_warning_permanent_without_recycle_bin),
                disabledWarningText = context.getString(R.string.local_file_manager_delete_warning_recycle_bin_disabled),
                onCancel = dismiss,
                onConfirm = { moveToRecycleBin ->
                    dismiss()
                    onConfirm(moveToRecycleBin)
                }
            )
        }.setCanceledOnTouchOutside(true)
    }

    fun showTransfer(
        context: Context,
        title: String,
        targetPath: String,
        entryCount: Int,
        @StringRes positiveText: Int,
        onConfirm: () -> Unit
    ) {
        show(
            context = context,
            title = title,
            message = OperationSafety.transferPlan(targetPath, entryCount),
            positiveText = positiveText,
            onConfirm = onConfirm
        )
    }

    fun showMoveToParent(
        context: Context,
        entryCount: Int,
        onConfirm: () -> Unit
    ) {
        show(
            context = context,
            title = context.getString(R.string.local_file_manager_batch_move_confirm_title),
            message = context.resources.getQuantityString(
                R.plurals.local_file_manager_batch_move_confirm_message,
                entryCount,
                entryCount
            ),
            positiveText = R.string.local_file_manager_selection_move_up,
            onConfirm = onConfirm
        )
    }

    private fun show(
        context: Context,
        title: String,
        message: String,
        @StringRes positiveText: Int,
        onConfirm: () -> Unit
    ) {
        showComposeDialog(context) { dismiss ->
            ConfirmContent(
                title = title,
                message = message,
                cancelText = context.getString(android.R.string.cancel),
                confirmText = context.getString(positiveText),
                onCancel = dismiss,
                onConfirm = {
                    dismiss()
                    onConfirm()
                }
            )
        }.setCanceledOnTouchOutside(true)
    }
}

@Composable
private fun ConfirmContent(
    title: String,
    message: String,
    cancelText: String,
    confirmText: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    DialogSurface {
        Text(
            text = title,
            color = SunsetGitHubThemeTokens.colors.textPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            color = SunsetGitHubThemeTokens.colors.textSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
        DialogActions(
            cancelText = cancelText,
            confirmText = confirmText,
            onCancel = onCancel,
            onConfirm = onConfirm
        )
    }
}

@Composable
private fun DeleteConfirmContent(
    title: String,
    message: String,
    recycleBinSettings: RecycleBinSettings,
    cancelText: String,
    confirmText: String,
    moveToRecycleBinText: String,
    permanentWarningText: String,
    disabledWarningText: String,
    onCancel: () -> Unit,
    onConfirm: (moveToRecycleBin: Boolean) -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    var moveToRecycleBin by remember {
        mutableStateOf(recycleBinSettings.enabled && recycleBinSettings.defaultMoveToRecycleBin)
    }
    val showWarning = !recycleBinSettings.enabled || (recycleBinSettings.showDeletionWarning && !moveToRecycleBin)
    DialogSurface {
        Text(
            text = title,
            color = colors.textPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            color = colors.textSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
        if (recycleBinSettings.enabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = moveToRecycleBin,
                    onCheckedChange = { moveToRecycleBin = it }
                )
                Text(
                    text = moveToRecycleBinText,
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        if (showWarning) {
            Text(
                text = if (recycleBinSettings.enabled) permanentWarningText else disabledWarningText,
                modifier = Modifier.padding(top = 10.dp),
                color = colors.danger,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
        }
        DialogActions(
            cancelText = cancelText,
            confirmText = confirmText,
            onCancel = onCancel,
            onConfirm = { onConfirm(recycleBinSettings.enabled && moveToRecycleBin) }
        )
    }
}

@Composable
private fun DialogSurface(content: @Composable ColumnScope.() -> Unit) {
    val colors = SunsetGitHubThemeTokens.colors
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        color = colors.surface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 8.dp,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content = content
        )
    }
}

@Composable
private fun DialogActions(
    cancelText: String,
    confirmText: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = onCancel) {
            Text(cancelText)
        }
        TextButton(onClick = onConfirm) {
            Text(confirmText)
        }
    }
}