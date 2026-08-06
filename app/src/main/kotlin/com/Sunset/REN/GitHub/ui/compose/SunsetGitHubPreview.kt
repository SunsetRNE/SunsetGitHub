package com.Sunset.REN.GitHub.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.Sunset.REN.GitHub.ui.compose.components.SunsetCard
import com.Sunset.REN.GitHub.ui.compose.components.SunsetEmptyState
import com.Sunset.REN.GitHub.ui.compose.components.SunsetErrorState
import com.Sunset.REN.GitHub.ui.compose.components.SunsetPrimaryButton
import com.Sunset.REN.GitHub.ui.compose.components.SunsetSecondaryButton
import com.Sunset.REN.GitHub.ui.compose.components.SunsetSectionHeader

@Preview(name = "SunsetGitHub Compose Foundation - Light", showBackground = true)
@Composable
private fun SunsetGitHubFoundationLightPreview() {
    SunsetGitHubFoundationPreviewContent(darkTheme = false)
}

@Preview(name = "SunsetGitHub Compose Foundation - Dark", showBackground = true)
@Composable
private fun SunsetGitHubFoundationDarkPreview() {
    SunsetGitHubFoundationPreviewContent(darkTheme = true)
}

@Composable
private fun SunsetGitHubFoundationPreviewContent(darkTheme: Boolean) {
    SunsetGitHubTheme(darkTheme = darkTheme) {
        val colors = SunsetGitHubThemeTokens.colors
        val spacing = SunsetGitHubThemeTokens.spacing
        Surface(color = colors.canvas) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.lg),
                verticalArrangement = Arrangement.spacedBy(spacing.lg)
            ) {
                SunsetSectionHeader(
                    title = "Compose foundation",
                    subtitle = "Shared theme and starter components for migrated screens."
                )
                SunsetCard {
                    Text(text = "Card content inherits the GitHub/Primer-inspired surface.")
                }
                SunsetPrimaryButton(text = "Primary action", onClick = {})
                SunsetSecondaryButton(text = "Secondary action", onClick = {})
                SunsetEmptyState(
                    title = "No repositories yet",
                    description = "This preview validates empty state spacing and typography."
                )
                SunsetErrorState(
                    title = "Unable to load repositories",
                    message = "This preview validates the shared recoverable error state."
                ) {
                    SunsetSecondaryButton(text = "Retry", onClick = {})
                }
            }
        }
    }
}