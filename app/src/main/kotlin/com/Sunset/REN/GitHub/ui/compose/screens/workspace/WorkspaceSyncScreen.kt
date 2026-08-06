package com.Sunset.REN.GitHub.ui.compose.screens.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens
import com.Sunset.REN.GitHub.ui.compose.components.SunsetPrimaryButton
import com.Sunset.REN.GitHub.ui.compose.components.SunsetSectionCard

@Composable
fun WorkspaceSyncScreen(
    onOpenPull: () -> Unit,
    onOpenPush: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    val spacing = SunsetGitHubThemeTokens.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.lg, vertical = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        Text(
            text = stringResource(R.string.title_workspace_sync),
            color = colors.textPrimary,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.workspace_sync_description),
            color = colors.textSecondary,
            style = MaterialTheme.typography.bodyMedium
        )

        WorkspaceSyncDirectionCard(
            title = stringResource(R.string.title_workspace_pull),
            description = stringResource(R.string.workspace_sync_pull_description),
            actionText = stringResource(R.string.workspace_sync_open_pull),
            onClick = onOpenPull
        )
        WorkspaceSyncDirectionCard(
            title = stringResource(R.string.title_workspace_push),
            description = stringResource(R.string.workspace_sync_push_description),
            actionText = stringResource(R.string.workspace_sync_open_push),
            onClick = onOpenPush
        )
    }
}

@Composable
private fun WorkspaceSyncDirectionCard(
    title: String,
    description: String,
    actionText: String,
    onClick: () -> Unit
) {
    SunsetSectionCard(
        title = title,
        description = description
    ) {
        SunsetPrimaryButton(
            text = actionText,
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
        )
    }
}
