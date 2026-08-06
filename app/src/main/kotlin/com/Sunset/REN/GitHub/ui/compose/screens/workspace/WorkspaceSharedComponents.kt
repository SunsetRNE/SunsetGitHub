package com.Sunset.REN.GitHub.ui.compose.screens.workspace

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.workspace.WorkspaceProject
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens
import com.Sunset.REN.GitHub.ui.compose.components.SunsetPrimaryButton
import com.Sunset.REN.GitHub.ui.compose.components.SunsetSectionCard

@Composable
internal fun WorkspaceProjectCard(
    workspaceName: String,
    selectedWorkspace: WorkspaceProject?,
    onWorkspaceNameChange: (String) -> Unit,
    onCreateWorkspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    SunsetSectionCard(
        title = stringResource(R.string.workspace_name_hint),
        modifier = modifier
    ) {
        WorkspaceTextField(
            value = workspaceName,
            onValueChange = onWorkspaceNameChange,
            label = stringResource(R.string.workspace_name_hint),
            modifier = Modifier.padding(top = 12.dp)
        )
        SunsetPrimaryButton(
            text = stringResource(R.string.workspace_create_button),
            onClick = onCreateWorkspace,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        )
        Text(
            text = selectedWorkspace?.let { "当前工作区：${it.name}\n${it.rootPath}" }
                ?: stringResource(R.string.workspace_status_none_selected),
            color = colors.textMuted,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}

@Composable
internal fun WorkspaceTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        minLines = minLines,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
        singleLine = minLines == 1
    )
}

@Composable
internal fun WorkspaceCheckboxRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(
            text = label,
            color = colors.textSecondary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

@Composable
internal fun WorkspaceLogText(log: String, modifier: Modifier = Modifier) {
    val colors = SunsetGitHubThemeTokens.colors
    Text(
        text = log.ifBlank { stringResource(R.string.workspace_log_placeholder) },
        color = colors.textMuted,
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = SunsetGitHubThemeTokens.spacing.lg)
    )
}

@Composable
internal fun WorkspacePageScaffold(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    val spacing = SunsetGitHubThemeTokens.spacing
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg, vertical = spacing.lg)
    ) {
        Text(
            text = title,
            color = colors.textPrimary,
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = description,
            color = colors.textSecondary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        content()
    }
}