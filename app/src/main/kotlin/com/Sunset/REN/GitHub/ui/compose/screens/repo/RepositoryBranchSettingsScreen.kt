package com.Sunset.REN.GitHub.ui.compose.screens.repo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.data.github.html.RepositoryBranchProtectionSnapshot
import com.Sunset.REN.GitHub.data.github.html.RepositoryBranchSettingsSnapshot
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens
import com.Sunset.REN.GitHub.ui.repo.RepositoryBranchSettingsRow
import com.Sunset.REN.GitHub.ui.repo.RepositoryBranchSettingsUiState
import com.Sunset.REN.GitHub.ui.repo.toBranchProtectionSummary

@Composable
fun RepositoryBranchSettingsScreen(
    state: RepositoryBranchSettingsUiState,
    onRetry: () -> Unit,
    onSelectBranch: (String) -> Unit,
    onEditProtection: (String) -> Unit,
    onDeleteProtection: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    Surface(modifier = modifier.fillMaxSize(), color = colors.canvas) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                state.isInitialLoad -> RepositorySettingsStateCard(
                    title = stringResource(R.string.repository_branch_settings_loading_title),
                    message = stringResource(R.string.repository_branch_settings_loading),
                    showProgress = true,
                    retryText = stringResource(R.string.repository_actions_retry)
                )
                state.errorMessage != null && state.snapshot == null -> RepositorySettingsStateCard(
                    title = stringResource(R.string.repository_branch_settings_error_title),
                    message = state.errorMessage,
                    showProgress = false,
                    retryText = stringResource(R.string.repository_actions_retry),
                    onRetry = onRetry
                )
                state.snapshot != null -> BranchSettingsContent(
                    state = state,
                    snapshot = state.snapshot,
                    onSelectBranch = onSelectBranch,
                    onEditProtection = onEditProtection,
                    onDeleteProtection = onDeleteProtection
                )
            }
        }
    }
}

@Composable
private fun BranchSettingsContent(
    state: RepositoryBranchSettingsUiState,
    snapshot: RepositoryBranchSettingsSnapshot,
    onSelectBranch: (String) -> Unit,
    onEditProtection: (String) -> Unit,
    onDeleteProtection: (String) -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    val inlineMessage = when {
        state.isLoadingProtection -> stringResource(R.string.repository_branch_settings_loading_protection)
        state.isSaving -> state.pendingMessage.orEmpty()
        !state.errorMessage.isNullOrBlank() -> state.errorMessage.orEmpty()
        else -> ""
    }
    RepositorySettingsInlineMessageCard(inlineMessage)
    RepositorySettingsSectionCard(
        title = stringResource(R.string.title_repository_branch_settings),
        description = stringResource(
            R.string.repository_branch_settings_summary,
            snapshot.defaultBranch,
            snapshot.branches.size,
            snapshot.protectedBranchCount
        )
    ) {
        RepositorySettingsMetricRow(stringResource(R.string.repository_branch_settings_metric_default_branch), snapshot.defaultBranch)
        RepositorySettingsMetricRow(stringResource(R.string.repository_branch_settings_metric_branch_count), snapshot.branches.size.toString())
        RepositorySettingsMetricRow(stringResource(R.string.repository_branch_settings_metric_protected_count), snapshot.protectedBranchCount.toString())
        RepositorySettingsMetricRow(
            stringResource(R.string.repository_branch_settings_metric_permission),
            if (snapshot.canAdmin) stringResource(R.string.repository_settings_toggle_editable) else stringResource(R.string.repository_settings_toggle_readonly)
        )
        if (snapshot.hasMoreProtectionDetailsThanLoaded) {
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = stringResource(R.string.repository_branch_settings_partial_details),
                color = colors.textMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
    RepositorySettingsSectionCard(title = stringResource(R.string.repository_branch_settings_section_branches)) {
        if (state.branches.isEmpty()) {
            Text(text = stringResource(R.string.repository_branch_settings_empty), color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
        } else {
            state.branches.forEach { row ->
                BranchRow(row = row, selected = row.name == state.selectedBranch, onClick = { onSelectBranch(row.name) })
            }
        }
    }
    RepositorySettingsSectionCard(title = stringResource(R.string.repository_branch_settings_section_selected)) {
        val selectedBranch = state.selectedBranch
        if (selectedBranch.isNullOrBlank()) {
            Text(text = stringResource(R.string.repository_branch_settings_select_hint), color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
        } else {
            RepositorySettingsMetricRow(stringResource(R.string.repository_branch_settings_metric_branch), selectedBranch)
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = state.selectedProtection?.toBranchProtectionSummary()
                    ?: stringResource(R.string.repository_branch_settings_no_protection_detail),
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            val canEdit = snapshot.canAdmin && !state.isSaving
            val hasProtection = state.selectedProtection != null || state.branches.firstOrNull { it.name == selectedBranch }?.isProtected == true
            RepositorySettingsActionRow(
                text = stringResource(R.string.repository_branch_settings_enable_pr_review),
                enabled = canEdit,
                onClick = { onEditProtection(selectedBranch) }
            )
            RepositorySettingsActionRow(
                text = stringResource(R.string.repository_branch_settings_delete_protection),
                enabled = canEdit && hasProtection,
                danger = true,
                onClick = { onDeleteProtection(selectedBranch) }
            )
        }
    }
}

@Composable
private fun BranchRow(row: RepositoryBranchSettingsRow, selected: Boolean, onClick: () -> Unit) {
    val status = if (row.isProtected) stringResource(R.string.repository_branch_settings_protected) else stringResource(R.string.repository_branch_settings_unprotected)
    val defaultBadge = if (row.isDefault) " · ${stringResource(R.string.repository_branch_settings_default_badge)}" else ""
    RepositorySettingsSelectableRow(
        title = "${row.name}$defaultBadge · $status",
        subtitle = "${row.sha.ifBlank { "-" }} · ${row.protectionSummary}",
        selected = selected,
        enabled = true,
        onClick = onClick
    )
}
