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
import com.Sunset.REN.GitHub.domain.repo.RepositoryPullRequest
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens
import com.Sunset.REN.GitHub.ui.compose.components.SunsetCard
import com.Sunset.REN.GitHub.ui.compose.components.SunsetEmptyState
import com.Sunset.REN.GitHub.ui.compose.components.SunsetErrorState
import com.Sunset.REN.GitHub.ui.compose.components.SunsetLoadingState
import com.Sunset.REN.GitHub.ui.compose.components.SunsetPrimaryButton
import com.Sunset.REN.GitHub.ui.repo.RepositoryPullRequestsUiState

@Composable
fun RepositoryPullRequestsScreen(
    state: RepositoryPullRequestsUiState,
    onStateSelected: (String) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenPullRequest: (RepositoryPullRequest) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    val hasItems = state.pullRequests.isNotEmpty()
    val initialError = !state.errorMessage.isNullOrBlank() && !hasItems

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas)
            .padding(horizontal = 16.dp)
    ) {
        PullRequestFilterBar(selectedState = state.state, onStateSelected = onStateSelected)
        when {
            state.isInitialLoad -> SunsetLoadingState(
                modifier = Modifier.weight(1f),
                message = "正在加载拉取请求……"
            )
            initialError -> SunsetErrorState(
                modifier = Modifier.weight(1f),
                title = "加载拉取请求失败",
                message = state.errorMessage.orEmpty(),
                action = {
                    SunsetPrimaryButton(
                        text = stringResource(R.string.repository_pull_requests_retry),
                        onClick = onRetry
                    )
                }
            )
            state.isEmpty -> SunsetEmptyState(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.repository_pull_requests_empty)
            )
            else -> PullRequestList(
                state = state,
                onLoadMore = onLoadMore,
                onOpenPullRequest = onOpenPullRequest,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PullRequestFilterBar(selectedState: String, onStateSelected: (String) -> Unit) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PullRequestFilter.entries.forEach { filter ->
            AssistChip(
                onClick = { onStateSelected(filter.state) },
                label = { Text(stringResource(filter.labelResId)) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (selectedState == filter.state) {
                        SunsetGitHubThemeTokens.colors.accentSoft
                    } else {
                        SunsetGitHubThemeTokens.colors.surface
                    },
                    labelColor = if (selectedState == filter.state) {
                        SunsetGitHubThemeTokens.colors.accent
                    } else {
                        SunsetGitHubThemeTokens.colors.textSecondary
                    }
                )
            )
        }
    }
}

@Composable
private fun PullRequestList(
    state: RepositoryPullRequestsUiState,
    onLoadMore: () -> Unit,
    onOpenPullRequest: (RepositoryPullRequest) -> Unit,
    modifier: Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (state.isShowingStaleContent) {
            item {
                Text(
                    text = stringResource(R.string.repository_pull_requests_loading_more),
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        items(state.pullRequests, key = { it.number }) { pullRequest ->
            PullRequestCard(pullRequest, state.state, onOpenPullRequest)
        }
        state.errorMessage?.takeIf { state.pullRequests.isNotEmpty() }?.let { error ->
            item {
                Text(
                    text = stringResource(R.string.repository_pull_requests_failed, error),
                    color = colors.danger,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        if (state.hasMore) {
            item {
                SunsetPrimaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    text = if (state.isLoadingMore) {
                        stringResource(R.string.repository_pull_requests_loading_more)
                    } else {
                        stringResource(R.string.repository_pull_requests_load_more)
                    },
                    enabled = !state.isLoadingMore,
                    onClick = onLoadMore
                )
            }
        }
    }
}

@Composable
private fun PullRequestCard(
    pullRequest: RepositoryPullRequest,
    selectedState: String,
    onOpenPullRequest: (RepositoryPullRequest) -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    SunsetCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenPullRequest(pullRequest) }
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = pullRequestStatusIcon(pullRequest, selectedState),
                color = pullRequestStatusColor(pullRequest, selectedState),
                style = MaterialTheme.typography.titleLarge
            )
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(
                    text = pullRequest.title,
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = pullRequestMeta(pullRequest, selectedState),
                    modifier = Modifier.padding(top = 4.dp),
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = stringResource(
                        R.string.repository_pull_requests_branch_meta,
                        pullRequest.headRef.ifBlank { "?" },
                        pullRequest.baseRef.ifBlank { "?" }
                    ),
                    modifier = Modifier.padding(top = 6.dp),
                    color = colors.textMuted,
                    style = MaterialTheme.typography.labelMedium
                )
                PullRequestBadges(pullRequest)
            }
            Text(
                text = pullRequest.updatedAt.orEmpty().substringBefore('T'),
                color = colors.textMuted,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun PullRequestBadges(pullRequest: RepositoryPullRequest) {
    val colors = SunsetGitHubThemeTokens.colors
    FlowRow(
        modifier = Modifier.padding(top = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("#${pullRequest.number}", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
        if (pullRequest.commentCount > 0) Text("💬 ${pullRequest.commentCount}", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
        if (pullRequest.draft) Text(stringResource(R.string.repository_pull_requests_draft_badge), color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
        if (pullRequest.isMerged) Text(stringResource(R.string.repository_pull_requests_merged_badge), color = colors.success, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun pullRequestMeta(pullRequest: RepositoryPullRequest, selectedState: String): String {
    val stateRes = when {
        pullRequest.isMerged -> R.string.repository_pull_requests_state_merged
        selectedState == RepositoryPullRequestsUiState.ClosedState -> R.string.repository_pull_requests_state_closed
        else -> R.string.repository_pull_requests_state_open
    }
    return stringResource(
        R.string.repository_pull_requests_meta,
        pullRequest.number,
        stringResource(stateRes),
        pullRequest.createdAt.orEmpty().substringBefore('T').ifBlank { pullRequest.authorLogin }
    )
}

@Composable
private fun pullRequestStatusColor(pullRequest: RepositoryPullRequest, selectedState: String) = when {
    pullRequest.isMerged -> SunsetGitHubThemeTokens.colors.accent
    selectedState == RepositoryPullRequestsUiState.ClosedState -> SunsetGitHubThemeTokens.colors.danger
    else -> SunsetGitHubThemeTokens.colors.success
}

private fun pullRequestStatusIcon(pullRequest: RepositoryPullRequest, selectedState: String): String = when {
    pullRequest.isMerged -> "◆"
    selectedState == RepositoryPullRequestsUiState.ClosedState -> "×"
    else -> "⑂"
}

private enum class PullRequestFilter(val state: String, @androidx.annotation.StringRes val labelResId: Int) {
    Open(RepositoryPullRequestsUiState.OpenState, R.string.repository_pull_requests_filter_open),
    Closed(RepositoryPullRequestsUiState.ClosedState, R.string.repository_pull_requests_filter_closed),
    All(RepositoryPullRequestsUiState.AllState, R.string.repository_pull_requests_filter_all)
}