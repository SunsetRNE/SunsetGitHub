package com.Sunset.REN.GitHub.ui.filemanager

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.filemanager.root.RootAccessBackend
import com.Sunset.REN.GitHub.domain.filemanager.root.RootAccessSettings
import com.Sunset.REN.GitHub.domain.filemanager.root.RootStartupPolicy
import com.Sunset.REN.GitHub.ui.common.LegacyDialogActions
import com.Sunset.REN.GitHub.ui.common.LegacyDialogSurface
import com.Sunset.REN.GitHub.ui.common.LegacyDialogTitle
import com.Sunset.REN.GitHub.ui.common.showComposeDialog
import com.Sunset.REN.GitHub.ui.common.showComposeSingleChoiceDialog
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens

object RootAccessSettingsDialog {
    fun show(
        context: Context,
        settings: RootAccessSettings,
        onDetect: () -> Unit,
        onSave: (RootAccessSettings) -> Unit
    ) {
        showComposeDialog(context) { dismiss ->
            RootAccessSettingsContent(
                context = context,
                settings = settings,
                onDetect = {
                    dismiss()
                    onDetect()
                },
                onCancel = dismiss,
                onSave = { updatedSettings ->
                    onSave(updatedSettings)
                    Toast.makeText(context, R.string.local_file_manager_root_settings_saved, Toast.LENGTH_SHORT).show()
                    dismiss()
                }
            )
        }.setCanceledOnTouchOutside(true)
    }

    private val startupPolicies = listOf(
        RootStartupPolicy.Disabled,
        RootStartupPolicy.DetectOnly,
        RootStartupPolicy.RequestOnStartup
    )

    @Composable
    private fun RootAccessSettingsContent(
        context: Context,
        settings: RootAccessSettings,
        onDetect: () -> Unit,
        onCancel: () -> Unit,
        onSave: (RootAccessSettings) -> Unit
    ) {
        var selectedStartupPolicy by remember { mutableStateOf(settings.startupPolicy) }
        var suCommand by remember { mutableStateOf(settings.suCommand) }
        var commandError by remember { mutableStateOf(false) }
        val colors = SunsetGitHubThemeTokens.colors

        LegacyDialogSurface {
            LegacyDialogTitle(context.getString(R.string.local_file_manager_root_settings_title))
            Spacer(modifier = Modifier.height(14.dp))
            SectionTitle(context.getString(R.string.local_file_manager_root_backend_title))
            Text(
                text = context.getString(R.string.local_file_manager_root_backend_su_selected),
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            SectionSummary(context.getString(R.string.local_file_manager_root_backend_unavailable_summary))

            SectionTitle(context.getString(R.string.local_file_manager_root_su_command_title))
            OutlinedTextField(
                value = suCommand,
                onValueChange = {
                    suCommand = it
                    commandError = false
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = commandError,
                supportingText = if (commandError) {
                    { Text(context.getString(R.string.local_file_manager_root_su_command_invalid)) }
                } else {
                    null
                },
                placeholder = { Text(RootAccessSettings.DefaultSuCommand) }
            )
            SectionSummary(context.getString(R.string.local_file_manager_root_su_command_summary))

            SectionTitle(context.getString(R.string.local_file_manager_root_startup_policy_title))
            Text(
                text = context.getString(
                    R.string.local_file_manager_root_startup_policy_value,
                    startupPolicyLabel(context, selectedStartupPolicy)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        showStartupPolicyPicker(context, selectedStartupPolicy) { selected ->
                            selectedStartupPolicy = selected
                        }
                    }
                    .padding(vertical = 8.dp),
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodyMedium
            )

            LegacyDialogActions(
                negativeText = context.getString(android.R.string.cancel),
                neutralText = context.getString(R.string.local_file_manager_root_menu_detect),
                positiveText = context.getString(android.R.string.ok),
                onNegative = onCancel,
                onNeutral = onDetect,
                onPositive = {
                    val command = suCommand.trim().ifBlank { RootAccessSettings.DefaultSuCommand }
                    if (!isValidSuCommand(command)) {
                        commandError = true
                        return@LegacyDialogActions
                    }
                    onSave(
                        settings.copy(
                            backend = RootAccessBackend.Su,
                            startupPolicy = selectedStartupPolicy,
                            suCommand = command
                        )
                    )
                }
            )
        }
    }

    private fun showStartupPolicyPicker(
        context: Context,
        selected: RootStartupPolicy,
        onSelected: (RootStartupPolicy) -> Unit
    ) {
        showComposeSingleChoiceDialog(
            context = context,
            title = context.getString(R.string.local_file_manager_root_startup_policy_title),
            items = startupPolicies,
            selected = selected,
            label = { startupPolicyLabel(context, it) },
            onSelected = onSelected
        )
    }

    @Composable
    private fun SectionTitle(text: String) {
        Text(
            text = text,
            color = SunsetGitHubThemeTokens.colors.textPrimary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }

    @Composable
    private fun SectionSummary(text: String) {
        Text(
            text = text,
            modifier = Modifier.padding(bottom = 12.dp),
            color = SunsetGitHubThemeTokens.colors.textMuted,
            style = MaterialTheme.typography.bodySmall
        )
    }

    private fun startupPolicyLabel(context: Context, policy: RootStartupPolicy): String {
        return when (policy) {
            RootStartupPolicy.Disabled -> context.getString(R.string.local_file_manager_root_startup_disabled)
            RootStartupPolicy.DetectOnly -> context.getString(R.string.local_file_manager_root_startup_detect)
            RootStartupPolicy.RequestOnStartup -> context.getString(R.string.local_file_manager_root_startup_request)
        }
    }

    private fun isValidSuCommand(command: String): Boolean {
        return command.matches(Regex("[A-Za-z0-9_./-]+")) && command.none { it.isWhitespace() }
    }
}