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
import com.Sunset.REN.GitHub.data.github.html.RepositoryHtmlMetric
import com.Sunset.REN.GitHub.data.github.html.RepositoryHtmlSectionStatus
import com.Sunset.REN.GitHub.data.github.html.RepositoryHtmlSectionSummary
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens
import com.Sunset.REN.GitHub.ui.compose.components.SunsetCard
import com.Sunset.REN.GitHub.ui.compose.components.SunsetLoadingState
import com.Sunset.REN.GitHub.ui.compose.components.SunsetPrimaryButton
import com.Sunset.REN.GitHub.ui.compose.components.SunsetSecondaryButton
import com.Sunset.REN.GitHub.ui.repo.RepositorySectionNativeStubUiState

@Composable
fun RepositorySectionNativeStubScreen(
    state: RepositorySectionNativeStubUiState,
    sectionTitle: String,
    sectionFallbackDescription: String,
    repositoryLabel: String,
    initialSectionUrl: String,
    onRetry: () -> Unit,
    onOpenInGitHub: (String) -> Unit,
    onCopyDebug: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    val effectiveUrl = state.sourceUrl?.takeIf { it.isNotBlank() } ?: initialSectionUrl
    val debugText = state.debugOutput(initialSectionUrl)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HeaderCard(
                sectionTitle = sectionTitle,
                repositoryLabel = repositoryLabel,
                state = state,
                fallbackDescription = sectionFallbackDescription,
                effectiveUrl = effectiveUrl,
                onRetry = onRetry,
                onOpenInGitHub = onOpenInGitHub,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        if (state.isLoading && state.summary == null) {
            item { SunsetLoadingState(message = stringResource(R.string.repository_section_native_stub_loading)) }
        }

        state.summary?.let { summary ->
            item { SummaryCard(summary = summary) }
            item { MetricsCard(metrics = summary.metrics) }
            item { NoticesCard(notices = summary.notices, actions = summary.actions) }
        }

        if (debugText.isNotBlank()) {
            item {
                DebugCard(
                    debugText = debugText,
                    onCopyDebug = onCopyDebug,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun HeaderCard(
    sectionTitle: String,
    repositoryLabel: String,
    state: RepositorySectionNativeStubUiState,
    fallbackDescription: String,
    effectiveUrl: String,
    onRetry: () -> Unit,
    onOpenInGitHub: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    val statusText = state.summary?.status?.displayText()
    val description = when {
        state.isLoading -> stringResource(R.string.repository_section_native_stub_loading)
        !state.errorMessage.isNullOrBlank() -> state.errorMessage
        state.summary != null -> state.summary.description
        else -> fallbackDescription
    }
    SunsetCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = sectionTitle,
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
            text = stringResource(R.string.repository_section_native_stub_state, statusText ?: stringResource(R.string.repository_section_native_stub_title)),
            modifier = Modifier.padding(top = 10.dp),
            color = if (state.errorMessage != null) colors.danger else colors.accent,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = description,
            modifier = Modifier.padding(top = 8.dp),
            color = colors.textSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state.errorMessage != null) {
                SunsetPrimaryButton(
                    text = stringResource(R.string.repository_section_native_stub_retry),
                    onClick = onRetry,
                    modifier = Modifier.weight(1f)
                )
            }
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
private fun SummaryCard(summary: RepositoryHtmlSectionSummary) {
    val colors = SunsetGitHubThemeTokens.colors
    SunsetCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = summary.title,
            color = colors.textPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = summary.status.displayText(),
            modifier = Modifier.padding(top = 6.dp),
            color = colors.accent,
            style = MaterialTheme.typography.labelMedium
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
private fun MetricsCard(metrics: List<RepositoryHtmlMetric>) {
    val colors = SunsetGitHubThemeTokens.colors
    SunsetCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "指标数据",
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
private fun NoticesCard(notices: List<String>, actions: List<String>) {
    val colors = SunsetGitHubThemeTokens.colors
    SunsetCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "提示与建议",
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
                    text = "• $notice",
                    modifier = Modifier.padding(top = 8.dp),
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            actions.forEach { action ->
                Text(
                    text = "建议操作：$action",
                    modifier = Modifier.padding(top = 8.dp),
                    color = colors.accent,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun DebugCard(debugText: String, onCopyDebug: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = SunsetGitHubThemeTokens.colors
    SunsetCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.repository_section_native_stub_copy_debug_label),
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

private fun RepositorySectionNativeStubUiState.debugOutput(initialSectionUrl: String): String {
    summary?.let { summary ->
        return buildString {
            appendLine("debug.repository=$owner/$repo")
            appendLine("debug.section=$sectionKey")
            appendLine("debug.status=${summary.status}")
            appendLine("debug.sourceUrl=${summary.sourceUrl}")
            appendLine("debug.metrics.count=${summary.metrics.size}")
            summary.metrics.forEachIndexed { index, metric ->
                appendLine("debug.metrics[$index]=${metric.label}: ${metric.value}")
            }
            appendLine("debug.notices.count=${summary.notices.size}")
            summary.notices.forEachIndexed { index, notice -> appendLine("debug.notices[$index]=$notice") }
            appendLine("debug.actions.count=${summary.actions.size}")
            summary.actions.forEachIndexed { index, action -> appendLine("debug.actions[$index]=$action") }
        }
    }
    if (errorMessage == null && sectionStatusCode == null && htmlPreview == null) return ""
    return buildString {
        appendLine("debug.repository=$owner/$repo")
        appendLine("debug.section=$sectionKey")
        appendLine("debug.statusCode=${sectionStatusCode ?: "n/a"}")
        appendLine("debug.sourceUrl=${sourceUrl ?: initialSectionUrl}")
        appendLine("debug.htmlPreview=${htmlPreview ?: "n/a"}")
    }
}