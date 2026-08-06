package com.Sunset.REN.GitHub.ui.compose.screens.repo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.repo.RepositoryActionRun
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens
import com.Sunset.REN.GitHub.ui.compose.components.SunsetCard
import com.Sunset.REN.GitHub.ui.compose.components.SunsetEmptyState
import com.Sunset.REN.GitHub.ui.compose.components.SunsetErrorState
import com.Sunset.REN.GitHub.ui.compose.components.SunsetLoadingState
import com.Sunset.REN.GitHub.ui.compose.components.SunsetPrimaryButton
import com.Sunset.REN.GitHub.ui.compose.components.SunsetSecondaryButton
import com.Sunset.REN.GitHub.ui.repo.RepositoryActionsUiState

@Composable
fun RepositoryActionsScreen(
    state: RepositoryActionsUiState,
    onStatusSelected: (String?) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenActions: () -> Unit,
    onOpenRun: (RepositoryActionRun) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    Column(modifier = modifier.fillMaxSize().background(colors.canvas)) {
        ActionsFilterBar(state.status, onStatusSelected)
        when {
            state.isInitialLoad && !state.isShowingStaleContent -> SunsetLoadingState(
                modifier = Modifier.fillMaxSize(),
                message = stringResource(R.string.repository_actions_loading)
            )
            state.errorMessage != null && state.workflowRuns.isEmpty() && state.workflows.isEmpty() -> SunsetErrorState(
                title = stringResource(R.string.repository_actions_retry),
                message = state.errorMessage,
                modifier = Modifier.fillMaxSize(),
                action = { SunsetPrimaryButton(stringResource(R.string.repository_actions_retry), onRetry) }
            )
            state.unavailableMessage != null && state.workflowRuns.isEmpty() -> SunsetErrorState(
                title = stringResource(R.string.repository_actions_missing_destination),
                message = state.unavailableMessage,
                modifier = Modifier.fillMaxSize(),
                action = {
                    if (state.actionsHtmlUrl != null) SunsetSecondaryButton(
                        stringResource(R.string.repository_actions_open_in_github),
                        onOpenActions
                    )
                }
            )
            state.isEmpty -> SunsetEmptyState(
                title = stringResource(R.string.repository_actions_empty),
                modifier = Modifier.fillMaxSize()
            )
            else -> ActionsList(state, onLoadMore, onOpenRun, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ActionsFilterBar(selectedStatus: String?, onStatusSelected: (String?) -> Unit) {
    val filters = listOf(
        null to R.string.repository_actions_filter_all,
        RepositoryActionsUiState.StatusQueued to R.string.repository_actions_filter_queued,
        RepositoryActionsUiState.StatusInProgress to R.string.repository_actions_filter_in_progress,
        RepositoryActionsUiState.StatusCompleted to R.string.repository_actions_filter_completed
    )
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { (status, labelRes) ->
            AssistChip(
                onClick = { onStatusSelected(status) },
                label = { Text(stringResource(labelRes)) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (selectedStatus == status) SunsetGitHubThemeTokens.colors.accentSoft else SunsetGitHubThemeTokens.colors.surface,
                    labelColor = if (selectedStatus == status) SunsetGitHubThemeTokens.colors.accent else SunsetGitHubThemeTokens.colors.textSecondary
                )
            )
        }
    }
}

@Composable
private fun ActionsList(
    state: RepositoryActionsUiState,
    onLoadMore: () -> Unit,
    onOpenRun: (RepositoryActionRun) -> Unit,
    modifier: Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    LazyColumn(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (state.isShowingStaleContent) item {
            Text(stringResource(R.string.repository_actions_loading), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
        }
        state.errorMessage?.takeIf { state.workflowRuns.isNotEmpty() }?.let { message -> item {
            Text(message, color = colors.danger, style = MaterialTheme.typography.bodySmall)
        } }
        items(state.workflowRuns, key = { it.id }) { run -> ActionRunCard(run, onOpenRun) }
        if (state.hasMoreRuns) item {
            SunsetPrimaryButton(
                text = if (state.isLoadingMore) stringResource(R.string.repository_actions_loading) else stringResource(R.string.repository_actions_load_more),
                onClick = onLoadMore,
                enabled = !state.isLoadingMore,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun ActionRunCard(run: RepositoryActionRun, onOpenRun: (RepositoryActionRun) -> Unit) {
    val colors = SunsetGitHubThemeTokens.colors
    SunsetCard(modifier = Modifier.fillMaxWidth().clickable { onOpenRun(run) }) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(run.name, color = colors.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    actionRunMeta(run),
                    modifier = Modifier.padding(top = 5.dp),
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
                run.createdAt?.takeIf { it.isNotBlank() }?.let { createdAt ->
                    Text(createdAt.replace("T", " ").removeSuffix("Z"), modifier = Modifier.padding(top = 6.dp), color = colors.textMuted, style = MaterialTheme.typography.labelSmall)
                }
            }
            Text(localizeStatus(run.displayState), color = actionRunStatusColor(run.displayState), style = MaterialTheme.typography.labelMedium)
        }
    }
}

private fun actionRunMeta(run: RepositoryActionRun): String = listOfNotNull(
    run.event.takeIf { it.isNotBlank() }?.replace('_', ' '),
    localizeStatus(run.displayState),
    run.headBranch?.takeIf { it.isNotBlank() },
    run.headSha?.takeIf { it.isNotBlank() }?.take(7)
).joinToString(" · ")

@Composable
private fun actionRunStatusColor(status: String) = when (status.lowercase()) {
    "success", "completed" -> SunsetGitHubThemeTokens.colors.success
    "failure", "cancelled", "timed_out" -> SunsetGitHubThemeTokens.colors.danger
    else -> SunsetGitHubThemeTokens.colors.accent
}

private fun localizeStatus(status: String): String = when (status.lowercase()) {
    "success" -> "成功"
    "failure" -> "失败"
    "cancelled" -> "已取消"
    "timed_out" -> "已超时"
    "in_progress" -> "运行中"
    "queued" -> "排队中"
    "waiting" -> "等待中"
    "requested" -> "已请求"
    "completed" -> "已完成"
    else -> status.ifBlank { "未知" }
}