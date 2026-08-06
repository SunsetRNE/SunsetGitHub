package com.Sunset.REN.GitHub.ui.compose.screens.repo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.repo.RepositorySecurityAlert
import com.Sunset.REN.GitHub.domain.repo.RepositorySecurityProbe
import com.Sunset.REN.GitHub.domain.repo.RepositorySecurityProbeStatus
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens
import com.Sunset.REN.GitHub.ui.compose.components.SunsetCard
import com.Sunset.REN.GitHub.ui.compose.components.SunsetLoadingState
import com.Sunset.REN.GitHub.ui.compose.components.SunsetPrimaryButton
import com.Sunset.REN.GitHub.ui.compose.components.SunsetSecondaryButton
import com.Sunset.REN.GitHub.ui.repo.RepositorySecurityUiState

@Composable
fun RepositorySecurityScreen(
    state: RepositorySecurityUiState,
    selectedPanelKey: String,
    onRetry: () -> Unit,
    onSelectPanel: (String) -> Unit,
    onSelectAlertType: (String) -> Unit,
    onSelectAlertState: (String?) -> Unit,
    onLoadMoreAlerts: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onOpenAlert: (RepositorySecurityAlert) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Box(Modifier.padding(top = 4.dp)) }
        when {
            state.isInitialLoad -> item { SunsetLoadingState(message = stringResource(R.string.repository_security_alerts_loading)) }
            state.errorMessage != null && state.summary == null -> item {
                SunsetCard(Modifier.fillMaxWidth()) {
                    Text(state.errorMessage, color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                    SunsetPrimaryButton(
                        text = stringResource(R.string.repository_security_retry),
                        onClick = onRetry,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
            state.summary != null -> {
                item { SecuritySummaryCard(state) }
                item {
                    SecurityEntryPointsCard(
                        probes = state.summary.probes,
                        selectedPanelKey = selectedPanelKey,
                        onSelectPanel = onSelectPanel
                    )
                }
                item {
                    SecuritySelectedDetailCard(
                        state = state,
                        selectedPanelKey = selectedPanelKey,
                        onSelectPanel = onSelectPanel,
                        onOpenUrl = onOpenUrl
                    )
                }
                if (selectedPanelKey in AlertPanelKeys) {
                    item {
                        SecurityAlertFilters(
                            state = state,
                            onSelectAlertType = onSelectAlertType,
                            onSelectAlertState = onSelectAlertState
                        )
                    }
                    item {
                        SecurityAlertsCard(
                            state = state,
                            onOpenAlert = onOpenAlert,
                            onLoadMoreAlerts = onLoadMoreAlerts
                        )
                    }
                }
                if (state.summary.notices.isNotEmpty()) {
                    item { SecurityNoticesCard(state.summary.notices) }
                }
            }
        }
        item { Box(Modifier.padding(bottom = 20.dp)) }
    }
}

@Composable
private fun SecuritySummaryCard(state: RepositorySecurityUiState) {
    val colors = SunsetGitHubThemeTokens.colors
    val summary = state.summary ?: return
    val setupCount = summary.unavailableCount
    val readyCount = summary.availableCount
    val totalFeatures = summary.probes.size.coerceAtLeast(1)
    val openAlerts = state.alerts.count { it.state.equals(RepositorySecurityUiState.AlertStateOpen, ignoreCase = true) }
    SunsetCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            StatusSymbol(isReady = setupCount == 0, needsAttention = setupCount > 0)
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(
                    stringResource(R.string.repository_security_overview_title),
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (setupCount > 0) stringResource(R.string.repository_security_overview_attention_description) else stringResource(R.string.repository_security_overview_clean_description),
                    modifier = Modifier.padding(top = 4.dp),
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = if (setupCount > 0) stringResource(R.string.repository_security_badge_attention) else stringResource(R.string.repository_security_badge_clean),
                color = statusColor(isReady = setupCount == 0, needsAttention = setupCount > 0),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            SecurityStat(openAlerts.toString(), stringResource(R.string.repository_security_stat_open_alerts), Modifier.weight(1f))
            SecurityStat(stringResource(R.string.repository_security_stat_ready_value, readyCount, totalFeatures), stringResource(R.string.repository_security_stat_ready), Modifier.weight(1f))
            SecurityStat(setupCount.toString(), stringResource(R.string.repository_security_stat_setup), Modifier.weight(1f))
        }
    }
}

@Composable
private fun SecurityStat(value: String, label: String, modifier: Modifier = Modifier) {
    val colors = SunsetGitHubThemeTokens.colors
    Column(modifier.padding(horizontal = 6.dp)) {
        Text(value, color = colors.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, color = colors.textMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SecurityEntryPointsCard(
    probes: List<RepositorySecurityProbe>,
    selectedPanelKey: String,
    onSelectPanel: (String) -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.repository_security_entry_points_title),
                modifier = Modifier.weight(1f),
                color = colors.textPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(stringResource(R.string.repository_security_entry_points_caption), color = colors.textMuted, style = MaterialTheme.typography.labelSmall)
        }
        SunsetCard(Modifier.fillMaxWidth()) {
            SecurityNavRow("⌂", stringResource(R.string.repository_security_overview_title), stringResource(R.string.repository_security_overview_detail_title), OverviewPanelKey, selectedPanelKey, onSelectPanel)
            val sorted = probes.sortedWith(compareBy<RepositorySecurityProbe> { it.status.riskRank() }.thenBy { it.title.ifBlank { it.key } })
            val findings = sorted.filter { it.securityPanelKey() != SecurityPolicyPanelKey }
            val reporting = sorted.filter { it.securityPanelKey() == SecurityPolicyPanelKey }
            if (findings.isNotEmpty()) {
                SectionLabel(stringResource(R.string.repository_security_group_findings))
                findings.forEach { probe -> ProbeRow(probe, selectedPanelKey, onSelectPanel) }
            }
            if (reporting.isNotEmpty()) {
                SectionLabel(stringResource(R.string.repository_security_group_reporting))
                reporting.forEach { probe -> ProbeRow(probe, selectedPanelKey, onSelectPanel) }
                SecurityNavRow("↗", stringResource(R.string.repository_security_advisories_title), stringResource(R.string.repository_security_advisories_description), SecurityAdvisoriesPanelKey, selectedPanelKey, onSelectPanel)
                SecurityNavRow("↗", stringResource(R.string.repository_security_private_reporting_title), stringResource(R.string.repository_security_private_reporting_description), PrivateReportingPanelKey, selectedPanelKey, onSelectPanel)
            }
            if (probes.isEmpty()) {
                Text(stringResource(R.string.repository_security_overview_empty), color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun SectionLabel(label: String) {
    val colors = SunsetGitHubThemeTokens.colors
    Text(label, modifier = Modifier.padding(top = 10.dp, bottom = 2.dp), color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
}

@Composable
private fun ProbeRow(probe: RepositorySecurityProbe, selectedPanelKey: String, onSelectPanel: (String) -> Unit) {
    SecurityNavRow(
        symbol = if (probe.isReady()) "✓" else "!",
        title = probe.displayTitle(),
        description = probe.humanReadableSummary(),
        panelKey = probe.securityPanelKey(),
        selectedPanelKey = selectedPanelKey,
        onSelectPanel = onSelectPanel,
        status = probe.status
    )
}

@Composable
private fun SecurityNavRow(
    symbol: String,
    title: String,
    description: String,
    panelKey: String,
    selectedPanelKey: String,
    onSelectPanel: (String) -> Unit,
    status: RepositorySecurityProbeStatus? = null
) {
    val colors = SunsetGitHubThemeTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable { onSelectPanel(panelKey) }
            .background(if (panelKey == selectedPanelKey) colors.chipBackground else Color.Transparent)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusSymbol(
            text = symbol,
            isReady = status == null || status == RepositorySecurityProbeStatus.Available || status == RepositorySecurityProbeStatus.Empty,
            needsAttention = status == RepositorySecurityProbeStatus.Disabled,
            isDanger = status == RepositorySecurityProbeStatus.Error || status == RepositorySecurityProbeStatus.Inaccessible
        )
        Column(Modifier.weight(1f).padding(start = 10.dp, end = 8.dp)) {
            Text(title, color = colors.textPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(description, color = colors.textSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Text("›", color = colors.textMuted, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun SecuritySelectedDetailCard(
    state: RepositorySecurityUiState,
    selectedPanelKey: String,
    onSelectPanel: (String) -> Unit,
    onOpenUrl: (String) -> Unit
) {
    val probes = state.summary?.probes.orEmpty()
    val selectedProbe = probes.firstOrNull { it.securityPanelKey() == selectedPanelKey }
    val owner = state.owner
    val repo = state.repo
    SunsetCard(Modifier.fillMaxWidth()) {
        when (selectedPanelKey) {
            OverviewPanelKey -> OverviewDetail(probes, onSelectPanel)
            RepositorySecurityUiState.AlertTypeDependabot -> DetailBlock(stringResource(R.string.repository_security_filter_dependabot), selectedProbe?.humanReadableSummary() ?: stringResource(R.string.repository_security_dependabot_description), stringResource(R.string.repository_security_dependabot_next_step), stringResource(R.string.repository_security_action_open_dependabot), repositoryUrl(owner, repo, "security/dependabot"), onOpenUrl)
            RepositorySecurityUiState.AlertTypeCodeScanning -> DetailBlock(stringResource(R.string.repository_security_code_scanning_setup_title), selectedProbe?.humanReadableSummary() ?: stringResource(R.string.repository_security_code_scanning_description), stringResource(R.string.repository_security_code_scanning_next_step), stringResource(R.string.repository_security_action_setup_codeql), repositoryUrl(owner, repo, "security/code-scanning/setup"), onOpenUrl)
            RepositorySecurityUiState.AlertTypeSecretScanning -> DetailBlock(stringResource(R.string.repository_security_secret_scanning_empty_title), selectedProbe?.humanReadableSummary() ?: stringResource(R.string.repository_security_secret_scanning_description), null, stringResource(R.string.repository_security_action_open_settings), repositoryUrl(owner, repo, "settings/security_analysis"), onOpenUrl)
            SecurityPolicyPanelKey -> DetailBlock(stringResource(R.string.repository_security_policy_setup_title), selectedProbe?.humanReadableSummary() ?: stringResource(R.string.repository_security_policy_description), stringResource(R.string.repository_security_policy_next_step), stringResource(R.string.repository_security_action_create_policy), repositoryUrl(owner, repo, "security/policy"), onOpenUrl)
            SecurityAdvisoriesPanelKey -> DetailBlock(stringResource(R.string.repository_security_advisories_title), stringResource(R.string.repository_security_advisories_description), stringResource(R.string.repository_security_external_workflow_hint), stringResource(R.string.repository_security_action_open_advisories), repositoryUrl(owner, repo, "security/advisories"), onOpenUrl)
            PrivateReportingPanelKey -> DetailBlock(stringResource(R.string.repository_security_private_reporting_title), stringResource(R.string.repository_security_private_reporting_description), stringResource(R.string.repository_security_external_workflow_hint), stringResource(R.string.repository_security_action_open_settings), repositoryUrl(owner, repo, "settings/security_analysis"), onOpenUrl)
            else -> DetailText(selectedProbe?.title ?: stringResource(R.string.title_repository_security), selectedProbe?.humanReadableSummary() ?: stringResource(R.string.repository_security_overview_empty))
        }
    }
}

@Composable
private fun OverviewDetail(probes: List<RepositorySecurityProbe>, onSelectPanel: (String) -> Unit) {
    val actionable = probes.filter { it.status == RepositorySecurityProbeStatus.Disabled || it.status == RepositorySecurityProbeStatus.Inaccessible || it.status == RepositorySecurityProbeStatus.Error }
    DetailText(
        title = stringResource(R.string.repository_security_overview_detail_title),
        body = if (actionable.isEmpty()) stringResource(R.string.repository_security_overview_detail_clean) else stringResource(R.string.repository_security_overview_detail_attention)
    )
    actionable.sortedWith(compareBy<RepositorySecurityProbe> { it.status.riskRank() }.thenBy { it.title.ifBlank { it.key } }).forEach { probe ->
        SecurityNavRow(if (probe.isReady()) "✓" else "!", probe.displayTitle(), probe.nextActionText(), probe.securityPanelKey(), "", onSelectPanel, probe.status)
    }
}

@Composable
private fun DetailBlock(title: String, description: String, nextStep: String?, action: String, url: String, onOpenUrl: (String) -> Unit) {
    DetailText(title, description)
    nextStep?.let { Text(it, modifier = Modifier.padding(top = 6.dp), color = SunsetGitHubThemeTokens.colors.textSecondary, style = MaterialTheme.typography.bodyMedium) }
    SunsetSecondaryButton(text = action, onClick = { onOpenUrl(url) }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
}

@Composable
private fun DetailText(title: String, body: String) {
    val colors = SunsetGitHubThemeTokens.colors
    Text(title, color = colors.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Text(body, modifier = Modifier.padding(top = 6.dp), color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun SecurityAlertFilters(state: RepositorySecurityUiState, onSelectAlertType: (String) -> Unit, onSelectAlertState: (String?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChipText(stringResource(R.string.repository_security_filter_dependabot), state.alertFilter.alertType == RepositorySecurityUiState.AlertTypeDependabot) { onSelectAlertType(RepositorySecurityUiState.AlertTypeDependabot) }
            FilterChipText(stringResource(R.string.repository_security_filter_code_scanning), state.alertFilter.alertType == RepositorySecurityUiState.AlertTypeCodeScanning) { onSelectAlertType(RepositorySecurityUiState.AlertTypeCodeScanning) }
            FilterChipText(stringResource(R.string.repository_security_filter_secret_scanning), state.alertFilter.alertType == RepositorySecurityUiState.AlertTypeSecretScanning) { onSelectAlertType(RepositorySecurityUiState.AlertTypeSecretScanning) }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            alertStateOptions(state.alertFilter.alertType).forEach { option ->
                FilterChipText(option.label, state.alertFilter.alertState == option.state) { onSelectAlertState(option.state) }
            }
        }
    }
}

@Composable
private fun FilterChipText(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = SunsetGitHubThemeTokens.colors
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) colors.accent.copy(alpha = 0.14f) else colors.chipBackground,
            labelColor = if (selected) colors.accent else colors.textSecondary
        )
    )
}

@Composable
private fun SecurityAlertsCard(state: RepositorySecurityUiState, onOpenAlert: (RepositorySecurityAlert) -> Unit, onLoadMoreAlerts: () -> Unit) {
    val colors = SunsetGitHubThemeTokens.colors
    SunsetCard(Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.repository_security_alerts_title), color = colors.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        when {
            state.isLoadingAlerts && state.alerts.isEmpty() && !state.isShowingStaleAlerts -> Text(stringResource(R.string.repository_security_alerts_loading), modifier = Modifier.padding(top = 8.dp), color = colors.textSecondary)
            state.alertsErrorMessage != null && state.alerts.isEmpty() -> Text(state.alertsErrorMessage.compactApiDetail(), modifier = Modifier.padding(top = 8.dp), color = colors.textSecondary)
            state.isAlertsEmpty -> Text(stringResource(R.string.repository_security_alerts_empty), modifier = Modifier.padding(top = 8.dp), color = colors.textSecondary)
            else -> {
                if (state.isShowingStaleAlerts) Text(stringResource(R.string.repository_security_alerts_loading), modifier = Modifier.padding(top = 8.dp), color = colors.textSecondary)
                state.alerts.forEach { alert -> AlertRow(alert, onOpenAlert) }
                state.alertsErrorMessage?.let { Text(it.compactApiDetail(), modifier = Modifier.padding(top = 8.dp), color = colors.textSecondary) }
            }
        }
        if (state.hasMoreAlerts && state.alerts.isNotEmpty()) {
            SunsetSecondaryButton(
                text = if (state.isLoadingMoreAlerts) stringResource(R.string.repository_security_alerts_loading_more) else stringResource(R.string.repository_security_alerts_load_more),
                onClick = onLoadMoreAlerts,
                enabled = !state.isLoadingMoreAlerts,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            )
        }
    }
}

@Composable
private fun AlertRow(alert: RepositorySecurityAlert, onOpenAlert: (RepositorySecurityAlert) -> Unit) {
    val colors = SunsetGitHubThemeTokens.colors
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onOpenAlert(alert) }.padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val normalized = listOf(alert.state, alert.severity.orEmpty()).joinToString(" ").lowercase()
        StatusSymbol(
            text = if (listOf("fixed", "resolved", "closed").any { it in normalized }) "✓" else "!",
            isReady = listOf("fixed", "resolved", "closed").any { it in normalized },
            needsAttention = false,
            isDanger = listOf("open", "critical", "high").any { it in normalized }
        )
        Column(Modifier.weight(1f).padding(start = 10.dp, end = 8.dp)) {
            Text(alert.title.ifBlank { stringResource(R.string.repository_security_alert_detail_unknown_title) }, color = colors.textPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(alert.metaText(), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text("›", color = colors.textMuted, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun SecurityNoticesCard(notices: List<String>) {
    val colors = SunsetGitHubThemeTokens.colors
    SunsetCard(Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.repository_security_notices_title), color = colors.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        notices.forEach { notice -> Text("• ${notice.displaySecurityNotice()}", modifier = Modifier.padding(top = 6.dp), color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
private fun StatusSymbol(text: String = "!", isReady: Boolean, needsAttention: Boolean, isDanger: Boolean = false) {
    val color = statusColor(isReady, needsAttention, isDanger)
    Surface(
        modifier = Modifier.size(24.dp),
        shape = CircleShape,
        color = statusSoftColor(isReady, needsAttention, isDanger),
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun statusColor(isReady: Boolean, needsAttention: Boolean, isDanger: Boolean = false): Color {
    val colors = SunsetGitHubThemeTokens.colors
    return when {
        isReady -> colors.success
        isDanger -> colors.danger
        needsAttention -> colors.attention
        else -> colors.textMuted
    }
}

@Composable
private fun statusSoftColor(isReady: Boolean, needsAttention: Boolean, isDanger: Boolean = false): Color {
    val colors = SunsetGitHubThemeTokens.colors
    return when {
        isReady -> colors.success.copy(alpha = 0.12f)
        isDanger -> colors.danger.copy(alpha = 0.12f)
        needsAttention -> colors.attention.copy(alpha = 0.12f)
        else -> colors.chipBackground
    }
}

private data class AlertStateOption(val label: String, val state: String?)

@Composable
private fun alertStateOptions(alertType: String): List<AlertStateOption> {
    val all = AlertStateOption(stringResource(R.string.repository_security_filter_state_all), null)
    val open = AlertStateOption(stringResource(R.string.repository_security_filter_state_open), RepositorySecurityUiState.AlertStateOpen)
    return when (alertType) {
        RepositorySecurityUiState.AlertTypeSecretScanning -> listOf(all, open, AlertStateOption(stringResource(R.string.repository_security_filter_state_resolved), RepositorySecurityUiState.AlertStateResolved))
        RepositorySecurityUiState.AlertTypeDependabot -> listOf(
            all,
            open,
            AlertStateOption(stringResource(R.string.repository_security_filter_state_fixed), RepositorySecurityUiState.AlertStateFixed),
            AlertStateOption(stringResource(R.string.repository_security_filter_state_dismissed), RepositorySecurityUiState.AlertStateDismissed),
            AlertStateOption(stringResource(R.string.repository_security_filter_state_auto_dismissed), RepositorySecurityUiState.AlertStateAutoDismissed)
        )
        else -> listOf(
            all,
            open,
            AlertStateOption(stringResource(R.string.repository_security_filter_state_fixed), RepositorySecurityUiState.AlertStateFixed),
            AlertStateOption(stringResource(R.string.repository_security_filter_state_dismissed), RepositorySecurityUiState.AlertStateDismissed)
        )
    }
}

private fun repositoryUrl(owner: String, repo: String, path: String): String = "https://github.com/$owner/$repo/${path.trimStart('/')}"

private fun RepositorySecurityProbe.securityPanelKey(): String {
    val normalized = listOf(key, title).joinToString(" ").lowercase()
    return when {
        "dependabot" in normalized -> RepositorySecurityUiState.AlertTypeDependabot
        "code" in normalized && "scanning" in normalized -> RepositorySecurityUiState.AlertTypeCodeScanning
        "secret" in normalized && "scanning" in normalized -> RepositorySecurityUiState.AlertTypeSecretScanning
        "policy" in normalized || "security.md" in normalized -> SecurityPolicyPanelKey
        else -> key.ifBlank { title }
    }
}

@Composable
private fun RepositorySecurityProbe.displayTitle(): String = when (securityPanelKey()) {
    RepositorySecurityUiState.AlertTypeDependabot -> stringResource(R.string.repository_security_entry_dependabot)
    RepositorySecurityUiState.AlertTypeCodeScanning -> stringResource(R.string.repository_security_entry_code_scanning)
    RepositorySecurityUiState.AlertTypeSecretScanning -> stringResource(R.string.repository_security_entry_secret_scanning)
    SecurityPolicyPanelKey -> stringResource(R.string.repository_security_policy_title)
    else -> title.ifBlank { stringResource(R.string.title_repository_security) }
}

@Composable
private fun RepositorySecurityProbe.nextActionText(): String = when (securityPanelKey()) {
    RepositorySecurityUiState.AlertTypeDependabot -> stringResource(R.string.repository_security_dependabot_next_action)
    RepositorySecurityUiState.AlertTypeCodeScanning -> stringResource(R.string.repository_security_code_scanning_next_action)
    RepositorySecurityUiState.AlertTypeSecretScanning -> stringResource(R.string.repository_security_secret_scanning_next_action)
    SecurityPolicyPanelKey -> stringResource(R.string.repository_security_policy_next_action)
    else -> humanReadableSummary()
}

@Composable
private fun RepositorySecurityProbe.humanReadableSummary(): String {
    if (status == RepositorySecurityProbeStatus.Available) return value?.takeIf { it.isNotBlank() } ?: stringResource(R.string.repository_security_feature_ready)
    if (status == RepositorySecurityProbeStatus.Empty) return value?.takeIf { it.isNotBlank() } ?: stringResource(R.string.repository_security_feature_empty)
    val source = listOfNotNull(value, description, detail).joinToString(" ").lowercase()
    return when {
        "403" in source || "permission" in source || "disabled" in source -> stringResource(R.string.repository_security_feature_access_or_setup)
        "404" in source || "not found" in source || "no analysis" in source -> stringResource(R.string.repository_security_feature_no_data)
        else -> stringResource(R.string.repository_security_feature_unavailable)
    }
}

private fun RepositorySecurityProbe.isReady(): Boolean = status == RepositorySecurityProbeStatus.Available || status == RepositorySecurityProbeStatus.Empty

private fun RepositorySecurityProbeStatus.riskRank(): Int = when (this) {
    RepositorySecurityProbeStatus.Error -> 0
    RepositorySecurityProbeStatus.Inaccessible -> 1
    RepositorySecurityProbeStatus.Disabled -> 2
    RepositorySecurityProbeStatus.Empty -> 3
    RepositorySecurityProbeStatus.Available -> 4
}

@Composable
private fun String.displaySecurityNotice(): String {
    val normalized = lowercase()
    return when {
        "rest api" in normalized || "github rest" in normalized -> stringResource(R.string.repository_security_notice_read_only)
        "token scope" in normalized || "管理员权限" in this -> stringResource(R.string.repository_security_notice_permissions)
        else -> this
    }
}

@Composable
private fun String.compactApiDetail(): String {
    val lower = lowercase()
    return when {
        "403" in lower -> stringResource(R.string.repository_security_detail_compact_403)
        "404" in lower -> stringResource(R.string.repository_security_detail_compact_404)
        else -> lineSequence().firstOrNull { it.isNotBlank() }.orEmpty()
    }
}

@Composable
private fun RepositorySecurityAlert.metaText(): String {
    val stateText = listOfNotNull(state.takeIf { it.isNotBlank() }, severity?.takeIf { it.isNotBlank() }).joinToString(" · ")
    return stringResource(R.string.repository_security_alert_meta, source, stateText.ifBlank { "unknown" })
}

private const val OverviewPanelKey = "security_overview"
private const val SecurityPolicyPanelKey = "security_policy"
private const val SecurityAdvisoriesPanelKey = "security_advisories"
private const val PrivateReportingPanelKey = "private_vulnerability_reporting"
private val AlertPanelKeys = setOf(
    RepositorySecurityUiState.AlertTypeDependabot,
    RepositorySecurityUiState.AlertTypeCodeScanning,
    RepositorySecurityUiState.AlertTypeSecretScanning
)
