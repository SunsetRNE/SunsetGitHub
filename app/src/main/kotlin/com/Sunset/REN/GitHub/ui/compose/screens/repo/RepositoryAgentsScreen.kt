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
import com.Sunset.REN.GitHub.data.github.html.RepositoryAgentSession
import com.Sunset.REN.GitHub.data.github.html.RepositoryHtmlMetric
import com.Sunset.REN.GitHub.data.github.html.RepositoryHtmlSectionStatus
import com.Sunset.REN.GitHub.data.github.html.RepositoryHtmlSectionSummary
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens
import com.Sunset.REN.GitHub.ui.compose.components.SunsetCard
import com.Sunset.REN.GitHub.ui.compose.components.SunsetLoadingState
import com.Sunset.REN.GitHub.ui.compose.components.SunsetPrimaryButton
import com.Sunset.REN.GitHub.ui.compose.components.SunsetSecondaryButton
import com.Sunset.REN.GitHub.ui.repo.RepositoryAgentsUiState

@Composable
fun RepositoryAgentsScreen(
    state: RepositoryAgentsUiState,
    repositoryLabel: String,
    fallbackUrl: String,
    onRetry: () -> Unit,
    onOpenInGitHub: (String) -> Unit,
    onCopyDebug: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    val effectiveUrl = state.sourceUrl?.takeIf { it.isNotBlank() } ?: fallbackUrl
    val debugText = state.debugOutput(fallbackUrl)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AgentsHeaderCard(
                state = state,
                repositoryLabel = repositoryLabel,
                effectiveUrl = effectiveUrl,
                onRetry = onRetry,
                onOpenInGitHub = onOpenInGitHub,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        if (state.isInitialLoad) {
            item { SunsetLoadingState(message = stringResource(R.string.repository_agents_loading)) }
        }

        state.summary?.let { summary ->
            item { AgentsOverviewCard(summary = summary) }
            item { AgentsMetricsCard(metrics = summary.metrics) }
            item { AgentsNoticesCard(notices = summary.notices, actions = summary.actions) }
        }

        item { AgentsSessionsCard(sessions = state.sessions, isLoading = state.isLoading, errorMessage = state.errorMessage) }

        if (debugText.isNotBlank()) {
            item {
                AgentsDebugCard(
                    debugText = debugText,
                    onCopyDebug = onCopyDebug,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun AgentsHeaderCard(
    state: RepositoryAgentsUiState,
    repositoryLabel: String,
    effectiveUrl: String,
    onRetry: () -> Unit,
    onOpenInGitHub: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    val title = state.summary?.title ?: stringResource(R.string.repository_section_agents)
    val statusText = state.summary?.status?.displayText()
    val description = when {
        state.isLoading -> stringResource(R.string.repository_agents_loading)
        !state.errorMessage.isNullOrBlank() -> state.errorMessage
        state.summary != null -> state.summary.description
        else -> stringResource(R.string.repository_agents_actions_description)
    }

    SunsetCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = colors.textPrimary,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = repositoryLabel,
            modifier = Modifier.padding(top = 6.dp),
            color = colors.textSecondary,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = statusText ?: stringResource(R.string.repository_agents_section_overview),
            modifier = Modifier.padding(top = 10.dp),
            color = if (state.errorMessage != null) colors.danger else colors.accent,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = description.orEmpty(),
            modifier = Modifier.padding(top = 8.dp),
            color = colors.textSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
        if (state.isShowingStaleContent) {
            Text(
                text = stringResource(R.string.repository_agents_state_loading),
                modifier = Modifier.padding(top = 8.dp),
                color = colors.textMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SunsetPrimaryButton(
                text = stringResource(R.string.repository_agents_refresh),
                onClick = onRetry,
                modifier = Modifier.weight(1f),
                enabled = !state.isLoading
            )
            SunsetSecondaryButton(
                text = stringResource(R.string.repository_section_open_in_github_content_description),
                onClick = { onOpenInGitHub(effectiveUrl) },
                modifier = Modifier.weight(1f),
                enabled = effectiveUrl.isNotBlank()
            )
        }
    }
}

@Composable
private fun AgentsOverviewCard(summary: RepositoryHtmlSectionSummary) {
    val colors = SunsetGitHubThemeTokens.colors
    SunsetCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.repository_agents_section_overview),
            color = colors.textPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = summary.description,
            modifier = Modifier.padding(top = 8.dp),
            color = colors.textSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
        if (summary.sourceUrl.isNotBlank()) {
            Text(
                text = summary.sourceUrl,
                modifier = Modifier.padding(top = 8.dp),
                color = colors.textMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun AgentsMetricsCard(metrics: List<RepositoryHtmlMetric>) {
    val colors = SunsetGitHubThemeTokens.colors
    SunsetCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.repository_actions_workflow_title),
            color = colors.textPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        if (metrics.isEmpty()) {
            Text(
                text = stringResource(R.string.repository_section_native_stub_no_metrics),
                modifier = Modifier.padding(top = 8.dp),
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            metrics.forEach { metric ->
                Text(
                    text = "${metric.label}: ${metric.value}",
                    modifier = Modifier.padding(top = 8.dp),
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun AgentsNoticesCard(notices: List<String>, actions: List<String>) {
    val colors = SunsetGitHubThemeTokens.colors
    SunsetCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.repository_agents_notices_title),
            color = colors.textPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        if (notices.isEmpty() && actions.isEmpty()) {
            Text(
                text = stringResource(R.string.repository_section_native_stub_no_notices),
                modifier = Modifier.padding(top = 8.dp),
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            notices.forEach { notice ->
                Text(
                    text = "- $notice",
                    modifier = Modifier.padding(top = 8.dp),
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            actions.forEach { action ->
                Text(
                    text = action,
                    modifier = Modifier.padding(top = 8.dp),
                    color = colors.accent,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun AgentsSessionsCard(
    sessions: List<RepositoryAgentSession>,
    isLoading: Boolean,
    errorMessage: String?
) {
    val colors = SunsetGitHubThemeTokens.colors
    SunsetCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.repository_agents_sessions_title),
            color = colors.textPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        when {
            isLoading && sessions.isEmpty() -> Text(
                text = stringResource(R.string.repository_agents_state_loading),
                modifier = Modifier.padding(top = 8.dp),
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            errorMessage != null && sessions.isEmpty() -> Text(
                text = stringResource(R.string.repository_agents_error_sessions_description),
                modifier = Modifier.padding(top = 8.dp),
                color = colors.danger,
                style = MaterialTheme.typography.bodyMedium
            )
            sessions.isEmpty() -> Text(
                text = stringResource(R.string.repository_agents_state_empty),
                modifier = Modifier.padding(top = 8.dp),
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            else -> sessions.forEach { session -> AgentsSessionRow(session = session) }
        }
    }
}

@Composable
private fun AgentsSessionRow(session: RepositoryAgentSession) {
    val colors = SunsetGitHubThemeTokens.colors
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Text(
            text = session.title.ifBlank { session.id },
            color = colors.textPrimary,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = session.status.displayLabel,
            modifier = Modifier.padding(top = 4.dp),
            color = if (session.status.isCompleted) colors.textMuted else colors.accent,
            style = MaterialTheme.typography.labelMedium
        )
        val details = listOfNotNull(
            session.type?.displayLabel,
            session.agent?.displayLabel,
            session.author?.takeIf { it.isNotBlank() },
            session.branch?.takeIf { it.isNotBlank() },
            session.updatedAt?.takeIf { it.isNotBlank() }
        ).joinToString("  ")
        if (details.isNotBlank()) {
            Text(
                text = details,
                modifier = Modifier.padding(top = 4.dp),
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (session.summary.isNotBlank()) {
            Text(
                text = session.summary,
                modifier = Modifier.padding(top = 4.dp),
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AgentsDebugCard(debugText: String, onCopyDebug: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = SunsetGitHubThemeTokens.colors
    SunsetCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.repository_agents_section_diagnostics),
            color = colors.textPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = debugText,
            modifier = Modifier
                .padding(top = 8.dp)
                .clickable { onCopyDebug(debugText) },
            color = colors.textSecondary,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun RepositoryHtmlSectionStatus.displayText(): String {
    val resId = when (this) {
        RepositoryHtmlSectionStatus.Available -> R.string.repository_section_native_stub_status_available
        RepositoryHtmlSectionStatus.Empty -> R.string.repository_section_native_stub_status_empty
        RepositoryHtmlSectionStatus.Disabled -> R.string.repository_section_native_stub_status_disabled
        RepositoryHtmlSectionStatus.AccessDenied -> R.string.repository_section_native_stub_status_access_denied
        RepositoryHtmlSectionStatus.ParsePartial -> R.string.repository_section_native_stub_status_parse_partial
        RepositoryHtmlSectionStatus.ParseFailed -> R.string.repository_section_native_stub_status_parse_failed
    }
    return stringResource(resId)
}

private fun RepositoryAgentsUiState.debugOutput(fallbackUrl: String): String {
    val summary = summary
    if (summary == null && errorMessage == null && sessions.isEmpty()) return ""
    return buildString {
        appendLine("debug.repository=$owner/$repo")
        appendLine("debug.section=agents")
        appendLine("debug.sourceUrl=${sourceUrl ?: summary?.sourceUrl ?: fallbackUrl}")
        appendLine("debug.loading=$isLoading")
        appendLine("debug.stale=$isShowingStaleContent")
        appendLine("debug.experimentalHtmlParse=$isExperimentalHtmlParse")
        errorMessage?.let { appendLine("debug.error=$it") }
        summary?.let { value ->
            appendLine("debug.status=${value.status}")
            appendLine("debug.metrics.count=${value.metrics.size}")
            appendLine("debug.notices.count=${value.notices.size}")
            appendLine("debug.actions.count=${value.actions.size}")
        }
        appendLine("debug.sessions.count=${sessions.size}")
        sessions.forEachIndexed { index, session ->
            appendLine("debug.sessions[$index]=${session.id}: ${session.status.displayLabel}: ${session.title}")
        }
    }
}
