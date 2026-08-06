package com.Sunset.REN.GitHub.ui.compose.screens.repo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.repo.RepositorySecurityAlert
import com.Sunset.REN.GitHub.domain.repo.RepositorySecurityAlertDetailGroup
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens
import com.Sunset.REN.GitHub.ui.compose.components.SunsetCard
import com.Sunset.REN.GitHub.ui.compose.components.SunsetPrimaryButton
import com.Sunset.REN.GitHub.ui.compose.components.SunsetSecondaryButton
import com.Sunset.REN.GitHub.ui.repo.RepositorySecurityAlertDetailUiState

@Composable
fun RepositorySecurityAlertDetailScreen(
    state: RepositorySecurityAlertDetailUiState,
    initialAlert: RepositorySecurityAlert,
    onRetry: () -> Unit,
    onOpenInGithub: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    val alert = state.alert ?: initialAlert
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (state.isLoading) {
            item {
                Text(
                    text = stringResource(R.string.repository_security_alert_detail_loading),
                    modifier = Modifier.padding(top = 16.dp),
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        state.errorMessage?.let { message ->
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = message,
                        color = colors.danger,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    SunsetPrimaryButton(
                        text = stringResource(R.string.repository_security_alert_detail_retry),
                        onClick = onRetry
                    )
                }
            }
        }
        item {
            SecurityAlertCard(alert, onOpenInGithub)
        }
    }
}

@Composable
private fun SecurityAlertCard(
    alert: RepositorySecurityAlert,
    onOpenInGithub: (String) -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    val meta = listOf(alert.source, alert.state, alert.severity.orEmpty())
        .filter { it.isNotBlank() }
        .joinToString(" · ")
        .ifBlank { stringResource(R.string.repository_security_alert_detail_unknown_meta) }

    SunsetCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = alert.title.ifBlank { stringResource(R.string.repository_security_alert_detail_unknown_title) },
            color = colors.textPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = meta,
            modifier = Modifier.padding(top = 8.dp),
            color = colors.textSecondary,
            style = MaterialTheme.typography.bodySmall
        )
        alert.createdAt?.takeIf { it.isNotBlank() }?.let { createdAt ->
            Text(
                text = createdAt,
                modifier = Modifier.padding(top = 6.dp),
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (alert.detailGroups.isEmpty() && alert.details.isEmpty()) {
            Text(
                text = stringResource(R.string.repository_security_alert_detail_details_title),
                modifier = Modifier.padding(top = 16.dp),
                color = colors.textPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        } else if (alert.detailGroups.isNotEmpty()) {
            alert.detailGroups.forEach { group ->
                SecurityAlertDetailGroup(group)
            }
        } else {
            alert.details.forEach { detail ->
                Text(
                    text = "• $detail",
                    modifier = Modifier.padding(top = 8.dp),
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        alert.htmlUrl?.takeIf { it.isNotBlank() }?.let { url ->
            SunsetSecondaryButton(
                text = stringResource(R.string.repository_security_alert_detail_open_in_github),
                onClick = { onOpenInGithub(url) },
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
private fun SecurityAlertDetailGroup(group: RepositorySecurityAlertDetailGroup) {
    val colors = SunsetGitHubThemeTokens.colors
    SunsetCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)
    ) {
        Text(
            text = group.title,
            color = colors.textPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        group.items.forEach { item ->
            Text(
                text = "• $item",
                modifier = Modifier.padding(top = 4.dp),
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}