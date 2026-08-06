package com.Sunset.REN.GitHub.ui.compose.screens.repo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.data.github.html.RepositorySettingsSnapshot
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens
import com.Sunset.REN.GitHub.ui.compose.components.SunsetCard
import com.Sunset.REN.GitHub.ui.repo.RepositoryDangerZoneUiState

@Composable
fun RepositoryDangerZoneScreen(
    state: RepositoryDangerZoneUiState,
    onRetry: () -> Unit,
    onArchiveClick: (archive: Boolean) -> Unit,
    onTransferClick: (fullName: String) -> Unit,
    onDeleteClick: (fullName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    Surface(
        modifier = modifier.fillMaxSize(),
        color = colors.canvas
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            when {
                state.isInitialLoad -> RepositorySettingsStateCard(
                    title = stringResource(R.string.repository_danger_zone_title),
                    message = stringResource(R.string.repository_danger_zone_loading),
                    showProgress = true,
                    retryText = stringResource(R.string.repository_danger_zone_retry),
                    onRetry = null
                )
                state.errorMessage != null && state.snapshot == null -> RepositorySettingsStateCard(
                    title = stringResource(
                        if (state.isDeleted) {
                            R.string.repository_danger_zone_deleted_title
                        } else {
                            R.string.repository_danger_zone_unavailable_title
                        }
                    ),
                    message = state.errorMessage,
                    showProgress = false,
                    retryText = stringResource(R.string.repository_danger_zone_retry),
                    onRetry = if (state.isDeleted) null else onRetry
                )
                state.snapshot != null -> RepositoryDangerZoneContent(
                    snapshot = state.snapshot,
                    inlineMessage = when {
                        state.isSaving -> state.pendingMessage.orEmpty()
                        !state.errorMessage.isNullOrBlank() -> state.errorMessage
                        else -> ""
                    },
                    isSaving = state.isSaving,
                    onArchiveClick = onArchiveClick,
                    onTransferClick = onTransferClick,
                    onDeleteClick = onDeleteClick
                )
            }
        }
    }
}

// Loading and error state card is shared by repository settings screens.

@Composable
private fun RepositoryDangerZoneContent(
    snapshot: RepositorySettingsSnapshot,
    inlineMessage: String,
    isSaving: Boolean,
    onArchiveClick: (archive: Boolean) -> Unit,
    onTransferClick: (fullName: String) -> Unit,
    onDeleteClick: (fullName: String) -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RepositorySettingsInlineMessageCard(inlineMessage)
        SunsetCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.repository_danger_zone_repository_title, snapshot.fullName),
                color = colors.danger,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = stringResource(
                    R.string.repository_danger_zone_summary,
                    stringResource(
                        if (snapshot.archived) {
                            R.string.repository_danger_zone_archived
                        } else {
                            R.string.repository_danger_zone_not_archived
                        }
                    ),
                    stringResource(
                        if (snapshot.canAdmin) {
                            R.string.repository_danger_zone_admin_allowed
                        } else {
                            R.string.repository_danger_zone_read_only
                        }
                    )
                ),
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        SunsetCard(modifier = Modifier.fillMaxWidth()) {
            DangerOperationSection(
                title = stringResource(R.string.repository_danger_zone_archive_section),
                description = stringResource(R.string.repository_danger_zone_archive_description),
                actionTitle = stringResource(
                    if (snapshot.archived) {
                        R.string.repository_danger_zone_unarchive_action
                    } else {
                        R.string.repository_danger_zone_archive_action
                    }
                ),
                actionDetail = stringResource(
                    if (snapshot.archived) {
                        R.string.repository_danger_zone_unarchive_action_detail
                    } else {
                        R.string.repository_danger_zone_archive_action_detail
                    }
                ),
                danger = !snapshot.archived,
                enabled = snapshot.canAdmin && !isSaving,
                onClick = { onArchiveClick(!snapshot.archived) }
            )
            DangerOperationSection(
                modifier = Modifier.padding(top = 18.dp),
                title = stringResource(R.string.repository_danger_zone_transfer_section),
                description = stringResource(R.string.repository_danger_zone_transfer_description),
                actionTitle = stringResource(R.string.repository_danger_zone_transfer_action),
                actionDetail = stringResource(R.string.repository_danger_zone_transfer_detail, snapshot.fullName),
                danger = true,
                enabled = snapshot.canAdmin && !isSaving,
                onClick = { onTransferClick(snapshot.fullName) }
            )
            DangerOperationSection(
                modifier = Modifier.padding(top = 18.dp),
                title = stringResource(R.string.repository_danger_zone_delete_section),
                description = stringResource(R.string.repository_danger_zone_delete_description),
                actionTitle = stringResource(R.string.repository_danger_zone_delete_action),
                actionDetail = stringResource(R.string.repository_danger_zone_delete_detail, snapshot.fullName),
                danger = true,
                enabled = snapshot.canAdmin && !isSaving,
                onClick = { onDeleteClick(snapshot.fullName) }
            )
        }
    }
}

@Composable
private fun DangerOperationSection(
    title: String,
    description: String,
    actionTitle: String,
    actionDetail: String,
    danger: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = colors.textPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            modifier = Modifier.padding(top = 6.dp),
            text = description,
            color = colors.textSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = actionTitle,
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    modifier = Modifier.padding(top = 2.dp),
                    text = actionDetail,
                    color = colors.textMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            OutlinedButton(
                modifier = Modifier.padding(start = 12.dp),
                enabled = enabled,
                onClick = onClick,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (danger) colors.danger else colors.accent
                )
            ) {
                Text(text = actionTitle)
            }
        }
    }
}
