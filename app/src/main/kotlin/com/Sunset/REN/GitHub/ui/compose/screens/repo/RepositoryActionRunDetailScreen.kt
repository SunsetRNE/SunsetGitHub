package com.Sunset.REN.GitHub.ui.compose.screens.repo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.repo.RepositoryActionArtifact
import com.Sunset.REN.GitHub.domain.repo.RepositoryActionRunDetail
import com.Sunset.REN.GitHub.domain.repo.RepositoryActionRunLogPreview
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens
import com.Sunset.REN.GitHub.ui.compose.components.SunsetCard
import com.Sunset.REN.GitHub.ui.compose.components.SunsetEmptyState
import com.Sunset.REN.GitHub.ui.compose.components.SunsetLoadingState
import com.Sunset.REN.GitHub.ui.compose.components.SunsetPrimaryButton
import com.Sunset.REN.GitHub.ui.compose.components.SunsetSecondaryButton
import com.Sunset.REN.GitHub.ui.repo.RepositoryActionRunDetailUiState
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Composable
fun RepositoryActionRunDetailScreen(
    state: RepositoryActionRunDetailUiState,
    onRetry: () -> Unit,
    onOpenActions: () -> Unit,
    onOpenRun: (String) -> Unit,
    onRefreshLogs: () -> Unit,
    onDownloadLogs: () -> Unit,
    onDownloadArtifact: (RepositoryActionArtifact) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    val actionRun = state.actionRun

    when {
        state.isLoading && actionRun == null -> SunsetLoadingState(
            modifier = modifier.fillMaxSize().background(colors.canvas),
            message = stringResource(R.string.repository_action_run_detail_loading)
        )
        actionRun == null && !state.errorMessage.isNullOrBlank() -> SunsetEmptyState(
            title = state.errorMessage,
            modifier = modifier.fillMaxSize().background(colors.canvas),
            action = { SunsetPrimaryButton(stringResource(R.string.repository_action_run_detail_retry), onRetry) }
        )
        actionRun == null && !state.unavailableMessage.isNullOrBlank() -> SunsetEmptyState(
            title = state.unavailableMessage,
            modifier = modifier.fillMaxSize().background(colors.canvas),
            action = {
                if (state.actionsHtmlUrl != null) {
                    SunsetSecondaryButton(stringResource(R.string.repository_action_run_detail_open_in_github), onOpenActions)
                }
            }
        )
        actionRun == null -> SunsetEmptyState(
            title = stringResource(R.string.repository_action_run_detail_missing_destination),
            modifier = modifier.fillMaxSize().background(colors.canvas)
        )
        else -> ActionRunDetailContent(
            state = state,
            actionRun = actionRun,
            onRetry = onRetry,
            onOpenActions = onOpenActions,
            onOpenRun = onOpenRun,
            onRefreshLogs = onRefreshLogs,
            onDownloadLogs = onDownloadLogs,
            onDownloadArtifact = onDownloadArtifact,
            modifier = modifier
        )
    }
}

@Composable
private fun ActionRunDetailContent(
    state: RepositoryActionRunDetailUiState,
    actionRun: RepositoryActionRunDetail,
    onRetry: () -> Unit,
    onOpenActions: () -> Unit,
    onOpenRun: (String) -> Unit,
    onRefreshLogs: () -> Unit,
    onDownloadLogs: () -> Unit,
    onDownloadArtifact: (RepositoryActionArtifact) -> Unit,
    modifier: Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    LazyColumn(
        modifier = modifier.fillMaxSize().background(colors.canvas).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ActionRunStatusMessage(state, onRetry, onOpenActions) }
        item { ActionRunHeroCard(actionRun, state) }
        item { ActionRunWorkflowCard(actionRun) }
        item { ActionRunLogsCard(state, onRefreshLogs, onDownloadLogs) }
        item { ActionRunArtifactsCard(state, onDownloadArtifact) }
        actionRun.htmlUrl?.takeIf { it.isNotBlank() }?.let { htmlUrl ->
            item {
                SunsetSecondaryButton(
                    text = stringResource(R.string.repository_action_run_detail_open_in_github),
                    onClick = { onOpenRun(htmlUrl) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun ActionRunStatusMessage(
    state: RepositoryActionRunDetailUiState,
    onRetry: () -> Unit,
    onOpenActions: () -> Unit
) {
    val message = when {
        state.isRefreshing -> stringResource(R.string.repository_action_run_detail_refreshing)
        !state.errorMessage.isNullOrBlank() -> state.errorMessage
        !state.unavailableMessage.isNullOrBlank() -> state.unavailableMessage
        state.refreshedAtMillis > 0L -> stringResource(
            R.string.repository_action_run_detail_cached_at,
            state.refreshedAtMillis.formatEpochMillis()
        )
        else -> null
    } ?: return

    SunsetCard(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Text(message, color = SunsetGitHubThemeTokens.colors.textSecondary, style = MaterialTheme.typography.bodySmall)
        if (!state.errorMessage.isNullOrBlank()) {
            SunsetPrimaryButton(
                text = stringResource(R.string.repository_action_run_detail_retry),
                onClick = onRetry,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
        if (!state.unavailableMessage.isNullOrBlank() && state.actionsHtmlUrl != null) {
            SunsetSecondaryButton(
                text = stringResource(R.string.repository_action_run_detail_open_in_github),
                onClick = onOpenActions,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

@Composable
private fun ActionRunHeroCard(actionRun: RepositoryActionRunDetail, state: RepositoryActionRunDetailUiState) {
    val colors = SunsetGitHubThemeTokens.colors
    val title = actionRun.name.ifBlank { stringResource(R.string.repository_action_run_detail_unknown_title) }
    SunsetCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = actionRun.runNumber?.let { "$title #$it" } ?: title,
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = actionRun.headerMeta(),
                    modifier = Modifier.padding(top = 6.dp),
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = actionRun.statusBadgeText(),
                color = actionRun.statusColor(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = stringResource(R.string.repository_action_run_detail_status_label, actionRun.displayState.localizedStatus()),
            modifier = Modifier.padding(top = 14.dp),
            color = colors.textPrimary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = actionRun.runSummary(),
            modifier = Modifier.padding(top = 6.dp),
            color = colors.textSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
        Row(modifier = Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionRunStat(
                label = stringResource(R.string.repository_action_run_detail_total_duration, actionRun.durationText()),
                value = actionRun.displayState.localizedStatus(),
                modifier = Modifier.weight(1f)
            )
            ActionRunStat(
                label = stringResource(R.string.repository_action_run_detail_assets_title),
                value = if (state.isLoadingArtifacts) "-" else state.artifacts.size.toString(),
                modifier = Modifier.weight(1f)
            )
        }
        actionRun.timeText()?.let { timeText ->
            Text(
                text = timeText,
                modifier = Modifier.padding(top = 12.dp),
                color = colors.textMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ActionRunStat(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = SunsetGitHubThemeTokens.colors
    SunsetCard(modifier = modifier) {
        Text(value, color = colors.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, modifier = Modifier.padding(top = 4.dp), color = colors.textMuted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ActionRunWorkflowCard(actionRun: RepositoryActionRunDetail) {
    val colors = SunsetGitHubThemeTokens.colors
    SunsetCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = actionRun.path?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
                ?: actionRun.workflowName?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.repository_action_run_detail_unknown_title),
            color = colors.textPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = stringResource(R.string.repository_action_run_detail_workflow_event_label, actionRun.event.localizedEvent()),
            modifier = Modifier.padding(top = 6.dp),
            color = colors.textSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
        actionRun.path?.takeIf { it.isNotBlank() }?.let { path ->
            Text(path, modifier = Modifier.padding(top = 8.dp), color = colors.textMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ActionRunLogsCard(
    state: RepositoryActionRunDetailUiState,
    onRefreshLogs: () -> Unit,
    onDownloadLogs: () -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    val preview = state.logPreview
    SunsetCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.repository_action_run_detail_logs_title), color = colors.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(preview.logMeta(), modifier = Modifier.padding(top = 4.dp), color = colors.textMuted, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                text = stringResource(R.string.repository_action_run_detail_logs_refresh),
                modifier = Modifier.clickable(enabled = !state.isLoadingLogs, onClick = onRefreshLogs).padding(8.dp),
                color = if (state.isLoadingLogs) colors.textMuted else colors.accent,
                style = MaterialTheme.typography.labelLarge
            )
        }
        val stateText = when {
            state.isLoadingLogs -> stringResource(R.string.repository_action_run_detail_logs_loading)
            !state.logsErrorMessage.isNullOrBlank() -> state.logsErrorMessage
            preview == null -> stringResource(R.string.repository_action_run_detail_logs_empty)
            else -> null
        }
        stateText?.let {
            Text(it, modifier = Modifier.padding(top = 10.dp), color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
        }
        preview?.let {
            Text(
                text = it.text,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                color = colors.textPrimary,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                maxLines = 18,
                overflow = TextOverflow.Ellipsis
            )
        }
        SunsetSecondaryButton(
            text = stringResource(R.string.repository_action_run_detail_logs_download),
            onClick = onDownloadLogs,
            enabled = state.actionRun?.logsUrl?.isNotBlank() == true,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        )
    }
}

@Composable
private fun ActionRunArtifactsCard(
    state: RepositoryActionRunDetailUiState,
    onDownloadArtifact: (RepositoryActionArtifact) -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    SunsetCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.repository_action_run_detail_artifacts_count, state.artifacts.size),
            color = colors.textPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        val stateText = when {
            state.isLoadingArtifacts -> stringResource(R.string.repository_action_run_detail_assets_loading)
            !state.artifactsErrorMessage.isNullOrBlank() -> state.artifactsErrorMessage
            state.artifacts.isEmpty() -> stringResource(R.string.repository_action_run_detail_assets_empty)
            else -> null
        }
        stateText?.let {
            Text(it, modifier = Modifier.padding(top = 8.dp), color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
        }
        state.artifacts.forEach { artifact ->
            ArtifactRow(artifact = artifact, onDownloadArtifact = onDownloadArtifact)
        }
    }
}

@Composable
private fun ArtifactRow(artifact: RepositoryActionArtifact, onDownloadArtifact: (RepositoryActionArtifact) -> Unit) {
    val colors = SunsetGitHubThemeTokens.colors
    val enabled = !artifact.expired && !artifact.archiveDownloadUrl.isNullOrBlank()
    Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(artifact.name, color = colors.textPrimary, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(artifact.sizeInBytes.formatBytes(), color = colors.textMuted, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            text = if (artifact.expired) stringResource(R.string.repository_action_run_detail_asset_expired) else stringResource(R.string.repository_action_run_detail_asset_download),
            modifier = Modifier.clickable(enabled = enabled) { onDownloadArtifact(artifact) }.padding(8.dp),
            color = if (enabled) colors.accent else colors.textMuted,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun RepositoryActionRunLogPreview?.logMeta(): String {
    val preview = this ?: return ""
    if (preview.truncated) {
        return stringResource(R.string.repository_action_run_detail_logs_meta_truncated, preview.fileCount.toString())
    }
    return stringResource(R.string.repository_action_run_detail_logs_meta, preview.fileCount.toString(), "OK")
}

@Composable
private fun String.localizedStatus(): String = when (lowercase(Locale.US)) {
    "success" -> stringResource(R.string.repository_action_run_status_success)
    "failure" -> stringResource(R.string.repository_action_run_status_failure)
    "cancelled" -> stringResource(R.string.repository_action_run_status_cancelled)
    "timed_out" -> stringResource(R.string.repository_action_run_status_timed_out)
    "in_progress" -> stringResource(R.string.repository_action_run_status_in_progress)
    "queued" -> stringResource(R.string.repository_action_run_status_queued)
    "waiting" -> stringResource(R.string.repository_action_run_status_waiting)
    "requested" -> stringResource(R.string.repository_action_run_status_requested)
    else -> ifBlank { stringResource(R.string.repository_action_run_status_unknown) }
}

@Composable
private fun String?.localizedEvent(): String = when (orEmpty().lowercase(Locale.US)) {
    "push" -> stringResource(R.string.repository_action_run_event_push)
    "pull_request" -> stringResource(R.string.repository_action_run_event_pull_request)
    "schedule" -> stringResource(R.string.repository_action_run_event_schedule)
    "workflow_dispatch" -> stringResource(R.string.repository_action_run_event_workflow_dispatch)
    "" -> "-"
    else -> orEmpty()
}

private fun RepositoryActionRunDetail.headerMeta(): String = listOfNotNull(
    event?.takeIf { it.isNotBlank() },
    actorLogin?.withAtPrefix(),
    headSha?.shortSha(),
    headBranch?.takeIf { it.isNotBlank() }
).joinToString(" · ").ifBlank { "暂无运行信息" }

private fun RepositoryActionRunDetail.runSummary(): String = listOfNotNull(
    (triggeringActorLogin ?: actorLogin)?.withAtPrefix(),
    headSha?.shortSha(),
    headBranch?.takeIf { it.isNotBlank() }
).joinToString(" · ").ifBlank { "暂无运行信息" }

private fun RepositoryActionRunDetail.statusBadgeText(): String = when (displayState.lowercase(Locale.US)) {
    "success" -> "OK"
    "failure", "cancelled", "timed_out" -> "!"
    "in_progress", "queued", "waiting", "requested" -> "..."
    else -> "?"
}

@Composable
private fun RepositoryActionRunDetail.statusColor() = when (displayState.lowercase(Locale.US)) {
    "success" -> SunsetGitHubThemeTokens.colors.success
    "failure", "cancelled", "timed_out" -> SunsetGitHubThemeTokens.colors.danger
    "in_progress", "queued", "waiting", "requested" -> SunsetGitHubThemeTokens.colors.attention
    else -> SunsetGitHubThemeTokens.colors.textSecondary
}

private fun RepositoryActionRunDetail.durationText(): String {
    val startedAt = runStartedAt?.parseGithubTimestamp() ?: return "-"
    val endedAt = updatedAt?.parseGithubTimestamp() ?: return "-"
    val totalSeconds = ((endedAt - startedAt) / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return when {
        hours > 0L -> "${hours}h ${minutes}m ${seconds}s"
        minutes > 0L -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}

@Composable
private fun RepositoryActionRunDetail.timeText(): String? = listOfNotNull(
    runStartedAt?.let { stringResource(R.string.repository_action_run_detail_started_at, it.displayTimestamp()) },
    updatedAt?.let { stringResource(R.string.repository_action_run_detail_updated_at, it.displayTimestamp()) }
).joinToString("\n").takeIf { it.isNotBlank() }

private fun String.parseGithubTimestamp(): Long? = runCatching { GithubTimestampFormat.parse(this)?.time }.getOrNull()
private fun String.displayTimestamp(): String = replace("T", " ").removeSuffix("Z")
private fun Long.formatEpochMillis(): String = runCatching { DisplayTimestampFormat.format(java.util.Date(this)) }.getOrDefault("-")
private fun String.shortSha(): String? = takeIf { it.isNotBlank() }?.take(7)
private fun String.withAtPrefix(): String = if (startsWith("@")) this else "@$this"

private fun Long.formatBytes(): String {
    if (this <= 0L) return "0 B"
    val units = listOf("B", "KB", "MB", "GB")
    var value = toDouble()
    var unitIndex = 0
    while (value >= 1024.0 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex++
    }
    return if (unitIndex == 0) {
        "${value.toLong()} ${units[unitIndex]}"
    } else {
        String.format(Locale.US, "%.1f %s", value, units[unitIndex])
    }
}

private val GithubTimestampFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("UTC")
}
private val DisplayTimestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
