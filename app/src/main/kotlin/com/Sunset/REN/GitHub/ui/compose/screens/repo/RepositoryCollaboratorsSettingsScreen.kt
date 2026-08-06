package com.Sunset.REN.GitHub.ui.compose.screens.repo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.data.github.html.RepositoryCollaboratorsSnapshot
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens
import com.Sunset.REN.GitHub.ui.repo.RepositoryCollaboratorInvitationRow
import com.Sunset.REN.GitHub.ui.repo.RepositoryCollaboratorRow
import com.Sunset.REN.GitHub.ui.repo.RepositoryCollaboratorsSettingsUiState

@Composable
fun RepositoryCollaboratorsSettingsScreen(
    state: RepositoryCollaboratorsSettingsUiState,
    onRetry: () -> Unit,
    onInvite: () -> Unit,
    onSelectCollaborator: (String) -> Unit,
    onChangePermission: (String) -> Unit,
    onRemoveCollaborator: (String) -> Unit,
    onCancelInvitation: (RepositoryCollaboratorInvitationRow) -> Unit,
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
                    title = stringResource(R.string.repository_collaborators_settings_loading_title),
                    message = stringResource(R.string.repository_collaborators_settings_loading),
                    showProgress = true,
                    retryText = stringResource(R.string.repository_actions_retry)
                )
                state.errorMessage != null && state.snapshot == null -> RepositorySettingsStateCard(
                    title = stringResource(R.string.repository_collaborators_settings_error_title),
                    message = state.errorMessage,
                    showProgress = false,
                    retryText = stringResource(R.string.repository_actions_retry),
                    onRetry = onRetry
                )
                state.snapshot != null -> CollaboratorsContent(
                    state = state,
                    snapshot = state.snapshot,
                    onInvite = onInvite,
                    onSelectCollaborator = onSelectCollaborator,
                    onChangePermission = onChangePermission,
                    onRemoveCollaborator = onRemoveCollaborator,
                    onCancelInvitation = onCancelInvitation
                )
            }
        }
    }
}

@Composable
private fun CollaboratorsContent(
    state: RepositoryCollaboratorsSettingsUiState,
    snapshot: RepositoryCollaboratorsSnapshot,
    onInvite: () -> Unit,
    onSelectCollaborator: (String) -> Unit,
    onChangePermission: (String) -> Unit,
    onRemoveCollaborator: (String) -> Unit,
    onCancelInvitation: (RepositoryCollaboratorInvitationRow) -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    val inlineMessage = when {
        state.isSaving -> state.pendingMessage.orEmpty()
        !state.errorMessage.isNullOrBlank() -> state.errorMessage.orEmpty()
        else -> ""
    }
    RepositorySettingsInlineMessageCard(inlineMessage)
    RepositorySettingsSectionCard(
        title = stringResource(R.string.title_repository_collaborators_settings),
        description = stringResource(
            R.string.repository_collaborators_settings_summary,
            snapshot.collaborators.size,
            snapshot.adminCount,
            snapshot.writeLikeCount
        )
    ) {
        RepositorySettingsMetricRow(stringResource(R.string.repository_collaborators_settings_metric_total), snapshot.collaborators.size.toString())
        RepositorySettingsMetricRow(stringResource(R.string.repository_collaborators_settings_metric_admin), snapshot.adminCount.toString())
        RepositorySettingsMetricRow(stringResource(R.string.repository_collaborators_settings_metric_write), snapshot.writeLikeCount.toString())
        RepositorySettingsMetricRow(stringResource(R.string.repository_collaborators_settings_pending_invitations), snapshot.invitations.size.toString())
        RepositorySettingsMetricRow(
            stringResource(R.string.repository_collaborators_settings_metric_permission),
            if (snapshot.canAdmin) stringResource(R.string.repository_settings_toggle_editable) else stringResource(R.string.repository_settings_toggle_readonly)
        )
    }
    val canEdit = snapshot.canAdmin && !state.isSaving
    RepositorySettingsSectionCard(title = stringResource(R.string.repository_collaborators_settings_section_list)) {
        if (state.collaborators.isEmpty()) {
            Text(text = stringResource(R.string.repository_collaborators_settings_empty), color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
        } else {
            state.collaborators.forEach { row ->
                CollaboratorRow(row = row, selected = row.login == state.selectedLogin, onClick = { onSelectCollaborator(row.login) })
            }
        }
    }
    RepositorySettingsSectionCard(title = stringResource(R.string.repository_collaborators_settings_pending_invitations)) {
        if (state.invitations.isEmpty()) {
            Text(text = stringResource(R.string.repository_collaborators_settings_pending_invitations_empty), color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
        } else {
            state.invitations.forEach { row ->
                InvitationRow(row = row, enabled = canEdit, onClick = { onCancelInvitation(row) })
            }
        }
    }
    RepositorySettingsSectionCard(title = stringResource(R.string.repository_settings_advanced_title)) {
        RepositorySettingsActionRow(
            text = stringResource(R.string.repository_collaborators_settings_invite),
            enabled = canEdit,
            onClick = onInvite
        )
        val selectedLogin = state.selectedLogin.orEmpty()
        RepositorySettingsActionRow(
            text = stringResource(R.string.repository_collaborators_settings_change_permission),
            enabled = canEdit && selectedLogin.isNotBlank(),
            onClick = { onChangePermission(selectedLogin) }
        )
        RepositorySettingsActionRow(
            text = stringResource(R.string.repository_collaborators_settings_remove),
            enabled = canEdit && selectedLogin.isNotBlank(),
            danger = true,
            onClick = { onRemoveCollaborator(selectedLogin) }
        )
    }
}

@Composable
private fun CollaboratorRow(row: RepositoryCollaboratorRow, selected: Boolean, onClick: () -> Unit) {
    RepositorySettingsSelectableRow(
        title = "${row.login} · ${row.permissionLabel}",
        subtitle = row.htmlUrl.ifBlank { row.permission.apiValue },
        selected = selected,
        onClick = onClick
    )
}

@Composable
private fun InvitationRow(row: RepositoryCollaboratorInvitationRow, enabled: Boolean, onClick: () -> Unit) {
    RepositorySettingsSelectableRow(
        title = "${row.displayName} · ${row.permissionLabel}",
        subtitle = if (enabled) {
            stringResource(R.string.repository_collaborators_settings_pending_invitations_tap_to_cancel)
        } else {
            stringResource(R.string.repository_collaborators_settings_readonly_no_admin)
        },
        selected = false,
        enabled = enabled,
        onClick = onClick
    )
}