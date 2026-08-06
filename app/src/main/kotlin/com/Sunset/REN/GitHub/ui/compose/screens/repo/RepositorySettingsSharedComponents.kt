package com.Sunset.REN.GitHub.ui.compose.screens.repo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens
import com.Sunset.REN.GitHub.ui.compose.components.SunsetCard
import com.Sunset.REN.GitHub.ui.compose.components.SunsetSecondaryButton

@Composable
fun RepositorySettingsSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    content: @Composable () -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    SunsetCard(modifier = modifier.fillMaxWidth()) {
        Text(text = title, color = colors.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (!description.isNullOrBlank()) {
            Text(
                modifier = Modifier.padding(top = 6.dp),
                text = description,
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Column(modifier = Modifier.padding(top = 8.dp)) { content() }
    }
}

@Composable
fun RepositorySettingsStateCard(
    title: String,
    message: String,
    showProgress: Boolean,
    retryText: String,
    onRetry: (() -> Unit)? = null
) {
    val colors = SunsetGitHubThemeTokens.colors
    SunsetCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (showProgress) {
                CircularProgressIndicator(color = colors.accent)
                Spacer(modifier = Modifier.height(10.dp))
            }
            Text(text = title, color = colors.textPrimary, style = MaterialTheme.typography.headlineSmall)
            Text(modifier = Modifier.padding(top = 6.dp), text = message, color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
            if (onRetry != null) {
                SunsetSecondaryButton(modifier = Modifier.padding(top = 14.dp), text = retryText, onClick = onRetry)
            }
        }
    }
}

@Composable
fun RepositorySettingsInlineMessageCard(message: String, modifier: Modifier = Modifier) {
    if (message.isBlank()) return
    SunsetCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = message,
            color = SunsetGitHubThemeTokens.colors.textSecondary,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun RepositorySettingsSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onCheckedChange: () -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onCheckedChange)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = colors.textPrimary, style = MaterialTheme.typography.bodyMedium)
            Text(text = description, color = colors.textMuted, style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            modifier = Modifier
                .padding(start = 12.dp)
                .semantics { this.contentDescription = contentDescription },
            checked = checked,
            enabled = enabled,
            onCheckedChange = { onCheckedChange() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = colors.accent,
                uncheckedThumbColor = colors.textMuted,
                uncheckedTrackColor = colors.chipBackground
            )
        )
    }
}

@Composable
fun RepositorySettingsMetricRow(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = SunsetGitHubThemeTokens.colors
    Row(modifier = modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(modifier = Modifier.weight(1f), text = label, color = colors.textMuted, style = MaterialTheme.typography.bodySmall)
        Text(
            modifier = Modifier.weight(1.2f),
            text = value,
            color = colors.textSecondary,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun RepositorySettingsActionRow(
    text: String,
    selected: Boolean = false,
    enabled: Boolean = true,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    OutlinedButton(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (danger) colors.danger else colors.accent),
        onClick = onClick
    ) {
        Text(text = (if (selected) "✓ " else "○ ") + text)
    }
}

@Composable
fun RepositorySettingsSelectableRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp)
    ) {
        Text(
            text = (if (selected) "✓ " else "") + title,
            color = if (selected) colors.accent else colors.textPrimary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
        if (subtitle.isNotBlank()) {
            Text(text = subtitle, color = colors.textMuted, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
