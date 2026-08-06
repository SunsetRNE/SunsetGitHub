package com.Sunset.REN.GitHub.ui.compose.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens

/**
 * Shared card pattern for migrated settings-like Compose screens.
 *
 * Keep page-specific business rows in the screen package, but route common spacing,
 * surface, title and description treatment through this component so future Compose
 * migration does not duplicate small styling decisions per page.
 */
@Composable
fun SunsetSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    SunsetCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = SunsetGitHubThemeTokens.spacing.lg),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp)
    ) {
        Text(
            text = title,
            color = colors.textPrimary,
            style = MaterialTheme.typography.titleMedium
        )
        if (!description.isNullOrBlank()) {
            Text(
                modifier = Modifier.padding(top = 6.dp),
                text = description,
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        content()
    }
}

@Composable
fun SunsetSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = SunsetGitHubThemeTokens.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (enabled) colors.textPrimary else colors.textMuted,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                modifier = Modifier.padding(top = 2.dp),
                text = description,
                color = colors.textMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Switch(
            modifier = Modifier.padding(start = 12.dp),
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = colors.accent,
                uncheckedThumbColor = colors.textMuted,
                uncheckedTrackColor = colors.subtleBackground,
                uncheckedBorderColor = colors.border,
                disabledCheckedThumbColor = colors.textMuted,
                disabledCheckedTrackColor = colors.subtleBackground,
                disabledUncheckedThumbColor = colors.textMuted,
                disabledUncheckedTrackColor = colors.subtleBackground
            )
        )
    }
}

@Composable
fun SunsetDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.fillMaxWidth(),
        thickness = DividerDefaults.Thickness,
        color = SunsetGitHubThemeTokens.colors.border
    )
}
