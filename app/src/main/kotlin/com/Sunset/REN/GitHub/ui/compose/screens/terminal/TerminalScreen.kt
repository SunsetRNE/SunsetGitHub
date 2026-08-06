package com.Sunset.REN.GitHub.ui.compose.screens.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens
import com.Sunset.REN.GitHub.ui.compose.components.SunsetPrimaryButton
import com.Sunset.REN.GitHub.ui.compose.components.SunsetSecondaryButton
import com.Sunset.REN.GitHub.ui.terminal.TerminalUiState

@Composable
fun TerminalScreen(
    state: TerminalUiState,
    commandText: String,
    onCommandTextChange: (String) -> Unit,
    onRunCommand: () -> Unit,
    onQuickHelp: () -> Unit,
    onQuickStatus: () -> Unit,
    onQuickDryRun: () -> Unit,
    onSelectWorkspace: () -> Unit,
    onOpenCommandPanel: () -> Unit,
    onManageExports: () -> Unit,
    onHistoryPrevious: () -> Unit,
    onHistoryNext: () -> Unit,
    onCopyOutput: () -> Unit,
    onExportOutput: () -> Unit,
    onShareOutput: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    val spacing = SunsetGitHubThemeTokens.spacing
    val pageScroll = rememberScrollState()
    val outputScroll = rememberScrollState()
    val isRunning = state.isCommandRunning
    val hasHistory = state.history.isNotEmpty()
    val hasOutput = state.output.isNotBlank()
    val workspace = state.selectedWorkspace
    val statusText = workspace?.let { "工作区：${it.name} · /${state.currentDirectory}" } ?: stringResource(R.string.terminal_status_disconnected)
    val outputText = state.output.ifBlank { stringResource(R.string.terminal_output_placeholder) }

    LaunchedEffect(outputText) {
        outputScroll.animateScrollTo(outputScroll.maxValue)
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = colors.canvas
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(pageScroll)
                .padding(horizontal = spacing.lg, vertical = spacing.lg),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.title_workspace_terminal),
                color = colors.textPrimary,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = statusText,
                color = colors.textMuted,
                style = MaterialTheme.typography.bodySmall
            )
            if (isRunning) {
                val progress = state.commandProgressPercent
                if (progress == null) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                val progressText = listOfNotNull(
                    progress?.let { "$it%" },
                    state.commandProgressText
                ).joinToString(" · ")
                if (progressText.isNotBlank()) {
                    Text(
                        text = progressText,
                        color = colors.textMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Text(
                text = stringResource(R.string.terminal_description),
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodyMedium
            )

            TerminalButtonRow {
                SunsetSecondaryButton(stringResource(R.string.terminal_quick_help), onQuickHelp, Modifier.weight(1f), enabled = !isRunning)
                Spacer(Modifier.width(8.dp))
                SunsetSecondaryButton(stringResource(R.string.terminal_quick_status), onQuickStatus, Modifier.weight(1f), enabled = !isRunning)
                Spacer(Modifier.width(8.dp))
                SunsetSecondaryButton(stringResource(R.string.terminal_quick_dry_run), onQuickDryRun, Modifier.weight(1f), enabled = !isRunning)
            }
            TerminalButtonRow {
                SunsetSecondaryButton(stringResource(R.string.terminal_select_workspace), onSelectWorkspace, Modifier.weight(1f), enabled = !isRunning && state.workspaces.isNotEmpty())
                Spacer(Modifier.width(8.dp))
                SunsetSecondaryButton(stringResource(R.string.terminal_command_panel), onOpenCommandPanel, Modifier.weight(1f), enabled = !isRunning)
                Spacer(Modifier.width(8.dp))
                SunsetSecondaryButton(stringResource(R.string.terminal_manage_exports), onManageExports, Modifier.weight(1f), enabled = !isRunning)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .background(colors.surface)
                    .padding(12.dp)
            ) {
                Text(
                    text = outputText,
                    color = colors.textPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(outputScroll)
                )
            }

            TerminalButtonRow {
                SunsetSecondaryButton(stringResource(R.string.terminal_history_previous), onHistoryPrevious, Modifier.weight(1f), enabled = hasHistory && !isRunning)
                Spacer(Modifier.width(8.dp))
                SunsetSecondaryButton(stringResource(R.string.terminal_history_next), onHistoryNext, Modifier.weight(1f), enabled = hasHistory && !isRunning)
                Spacer(Modifier.width(8.dp))
                SunsetSecondaryButton(stringResource(R.string.terminal_copy_output), onCopyOutput, Modifier.weight(1f), enabled = hasOutput)
                Spacer(Modifier.width(8.dp))
                SunsetSecondaryButton(stringResource(R.string.terminal_export_output), onExportOutput, Modifier.weight(1f), enabled = hasOutput)
                Spacer(Modifier.width(8.dp))
                SunsetSecondaryButton(stringResource(R.string.terminal_share_output), onShareOutput, Modifier.weight(1f), enabled = hasOutput)
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = commandText,
                    onValueChange = onCommandTextChange,
                    modifier = Modifier.weight(1f),
                    enabled = !isRunning,
                    label = { Text(stringResource(R.string.terminal_command_hint)) },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onRunCommand() })
                )
                Spacer(Modifier.width(8.dp))
                SunsetPrimaryButton(
                    text = stringResource(R.string.terminal_run_command),
                    onClick = onRunCommand,
                    modifier = Modifier.padding(top = 8.dp),
                    enabled = !isRunning
                )
            }
        }
    }
}

@Composable
private fun TerminalButtonRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        content = content
    )
}
