package com.Sunset.REN.GitHub.ui.filemanager

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.filemanager.RecycleBinSettings
import com.Sunset.REN.GitHub.ui.common.LegacyDialogActions
import com.Sunset.REN.GitHub.ui.common.LegacyDialogSurface
import com.Sunset.REN.GitHub.ui.common.LegacyDialogTitle
import com.Sunset.REN.GitHub.ui.common.showComposeDialog
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens

object RecycleBinSettingsDialog {
    fun show(
        context: Context,
        settings: RecycleBinSettings,
        onSave: (RecycleBinSettings) -> Unit,
        onAfterSave: () -> Unit
    ) {
        showComposeDialog(context) { dismiss ->
            RecycleBinSettingsContent(
                context = context,
                settings = settings,
                onCancel = dismiss,
                onSave = { updatedSettings ->
                    onSave(updatedSettings)
                    Toast.makeText(context, R.string.local_file_manager_recycle_bin_settings_saved, Toast.LENGTH_SHORT).show()
                    dismiss()
                    onAfterSave()
                }
            )
        }.setCanceledOnTouchOutside(true)
    }
}

@Composable
private fun RecycleBinSettingsContent(
    context: Context,
    settings: RecycleBinSettings,
    onCancel: () -> Unit,
    onSave: (RecycleBinSettings) -> Unit
) {
    var enabled by remember { mutableStateOf(settings.enabled) }
    var defaultMoveToRecycleBin by remember { mutableStateOf(settings.defaultMoveToRecycleBin) }
    var showDeletionWarning by remember { mutableStateOf(settings.showDeletionWarning) }
    var autoCleanDaysText by remember { mutableStateOf(settings.autoCleanDays.toString()) }
    var autoCleanDaysError by remember { mutableStateOf(false) }
    val colors = SunsetGitHubThemeTokens.colors

    LegacyDialogSurface {
        LegacyDialogTitle(context.getString(R.string.local_file_manager_menu_recycle_bin_settings))
        Spacer(modifier = Modifier.height(12.dp))
        CheckRow(
            text = context.getString(R.string.local_file_manager_recycle_bin_enable_feature),
            checked = enabled,
            enabled = true,
            onCheckedChange = { enabled = it }
        )
        CheckRow(
            text = context.getString(R.string.local_file_manager_recycle_bin_default_move),
            checked = defaultMoveToRecycleBin,
            enabled = enabled,
            onCheckedChange = { defaultMoveToRecycleBin = it }
        )
        CheckRow(
            text = context.getString(R.string.local_file_manager_recycle_bin_show_delete_warning),
            checked = showDeletionWarning,
            enabled = enabled,
            onCheckedChange = { showDeletionWarning = it }
        )
        Text(
            text = context.getString(R.string.local_file_manager_recycle_bin_auto_clean_title),
            modifier = Modifier.padding(top = 10.dp),
            color = colors.textSecondary,
            style = MaterialTheme.typography.bodySmall
        )
        OutlinedTextField(
            value = autoCleanDaysText,
            onValueChange = {
                autoCleanDaysText = it
                autoCleanDaysError = false
            },
            modifier = Modifier.padding(top = 4.dp),
            enabled = enabled,
            singleLine = true,
            isError = autoCleanDaysError,
            supportingText = if (autoCleanDaysError) {
                { Text(context.getString(R.string.local_file_manager_recycle_bin_auto_clean_invalid)) }
            } else {
                null
            },
            placeholder = { Text(context.getString(R.string.local_file_manager_recycle_bin_auto_clean_hint)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        LegacyDialogActions(
            negativeText = context.getString(android.R.string.cancel),
            positiveText = context.getString(android.R.string.ok),
            onNegative = onCancel,
            onPositive = {
                val autoCleanDays = autoCleanDaysText.trim().toIntOrNull()
                if (autoCleanDays == null || autoCleanDays < 0) {
                    autoCleanDaysError = true
                    return@LegacyDialogActions
                }
                onSave(
                    settings.copy(
                        enabled = enabled,
                        defaultMoveToRecycleBin = defaultMoveToRecycleBin,
                        autoCleanDays = autoCleanDays,
                        showDeletionWarning = showDeletionWarning
                    )
                )
            }
        )
    }
}

@Composable
private fun CheckRow(
    text: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
        Text(
            text = text,
            color = if (enabled) SunsetGitHubThemeTokens.colors.textPrimary else SunsetGitHubThemeTokens.colors.textMuted,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}