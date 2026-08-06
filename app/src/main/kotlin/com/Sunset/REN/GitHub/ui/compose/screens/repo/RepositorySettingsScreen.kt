package com.Sunset.REN.GitHub.ui.compose.screens.repo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens
import com.Sunset.REN.GitHub.ui.compose.components.SunsetCard
import com.Sunset.REN.GitHub.ui.repo.RepositorySettingsEditableFieldKey
import com.Sunset.REN.GitHub.ui.repo.RepositorySettingsEditableItem
import com.Sunset.REN.GitHub.ui.repo.RepositorySettingsInfoItem
import com.Sunset.REN.GitHub.ui.repo.RepositorySettingsInfoKey
import com.Sunset.REN.GitHub.ui.repo.RepositorySettingsScreenState
import com.Sunset.REN.GitHub.ui.repo.RepositorySettingsStatItem
import com.Sunset.REN.GitHub.ui.repo.RepositorySettingsToggleItem
import com.Sunset.REN.GitHub.ui.repo.RepositorySettingsToggleKey
import com.Sunset.REN.GitHub.ui.repo.RepositorySettingsUiState
import com.Sunset.REN.GitHub.ui.repo.RepositorySettingsVisibility
import com.Sunset.REN.GitHub.ui.repo.RepositorySettingsVisibilityOption

@Composable
fun RepositorySettingsScreen(
    state: RepositorySettingsUiState,
    pendingVisibilitySelection: RepositorySettingsVisibility?,
    onRetry: () -> Unit,
    onVisibilitySelected: (RepositorySettingsVisibility) -> Unit,
    onEditField: (RepositorySettingsEditableItem) -> Unit,
    onToggle: (RepositorySettingsToggleItem) -> Unit,
    onOpenBranches: () -> Unit,
    onOpenCollaborators: () -> Unit,
    onOpenRulesets: () -> Unit,
    onOpenWebhooks: () -> Unit,
    onOpenDeployKeys: () -> Unit,
    onOpenActions: () -> Unit,
    onOpenDangerZone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    Surface(
        modifier = modifier.fillMaxSize(),
        color = colors.canvas
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 10.dp, top = 10.dp, end = 10.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                state.isInitialLoad -> Unit
                state.errorMessage != null && state.screen == null -> RepositorySettingsStateCard(
                    title = stringResource(R.string.repository_settings_error_title),
                    message = state.errorMessage,
                    showProgress = false,
                    retryText = stringResource(R.string.repository_settings_retry),
                    onRetry = onRetry
                )
                state.screen != null -> RepositorySettingsContent(
                    screen = state.screen,
                    state = state,
                    pendingVisibilitySelection = pendingVisibilitySelection,
                    onVisibilitySelected = onVisibilitySelected,
                    onEditField = onEditField,
                    onToggle = onToggle,
                    onOpenBranches = onOpenBranches,
                    onOpenCollaborators = onOpenCollaborators,
                    onOpenRulesets = onOpenRulesets,
                    onOpenWebhooks = onOpenWebhooks,
                    onOpenDeployKeys = onOpenDeployKeys,
                    onOpenActions = onOpenActions,
                    onOpenDangerZone = onOpenDangerZone
                )
            }
        }
    }
}
// Loading and error state card is shared by repository settings screens.

@Composable
private fun RepositorySettingsContent(
    screen: RepositorySettingsScreenState,
    state: RepositorySettingsUiState,
    pendingVisibilitySelection: RepositorySettingsVisibility?,
    onVisibilitySelected: (RepositorySettingsVisibility) -> Unit,
    onEditField: (RepositorySettingsEditableItem) -> Unit,
    onToggle: (RepositorySettingsToggleItem) -> Unit,
    onOpenBranches: () -> Unit,
    onOpenCollaborators: () -> Unit,
    onOpenRulesets: () -> Unit,
    onOpenWebhooks: () -> Unit,
    onOpenDeployKeys: () -> Unit,
    onOpenActions: () -> Unit,
    onOpenDangerZone: () -> Unit
) {
    val inlineMessage = when {
        state.isLoading -> stringResource(R.string.repository_settings_refreshing)
        state.isSaving -> state.pendingMessage.orEmpty()
        !state.errorMessage.isNullOrBlank() -> state.errorMessage.orEmpty()
        else -> ""
    }
    RepositorySettingsInlineMessageCard(inlineMessage)
    RepositorySettingsHero(screen)
    RepositorySettingsSectionCard(
        title = stringResource(R.string.repository_settings_section_basic),
        description = stringResource(R.string.repository_settings_basic_subtitle)
    ) {
        screen.basicItems.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { item -> InfoCell(item, Modifier.weight(1f)) }
                if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
    VisibilitySection(
        option = screen.visibilityOption,
        isSaving = state.isSaving,
        pendingVisibilitySelection = pendingVisibilitySelection,
        onVisibilitySelected = onVisibilitySelected
    )
    RepositorySettingsSectionCard(
        title = stringResource(R.string.repository_settings_section_editable),
        description = stringResource(R.string.repository_settings_editable_subtitle)
    ) {
        screen.editableItems.forEach { item -> EditableRow(item, state.isSaving, onEditField) }
    }
    ToggleSection(
        title = stringResource(R.string.repository_settings_section_features),
        description = stringResource(R.string.repository_settings_features_subtitle),
        items = screen.featureItems,
        isSaving = state.isSaving,
        onToggle = onToggle
    )
    ToggleSection(
        title = stringResource(R.string.repository_settings_section_merge),
        description = stringResource(R.string.repository_settings_merge_subtitle),
        items = screen.mergeItems,
        isSaving = state.isSaving,
        onToggle = onToggle
    )
    NoticesSection(screen.notices)
    AdvancedSection(
        onOpenBranches = onOpenBranches,
        onOpenCollaborators = onOpenCollaborators,
        onOpenRulesets = onOpenRulesets,
        onOpenWebhooks = onOpenWebhooks,
        onOpenDeployKeys = onOpenDeployKeys,
        onOpenActions = onOpenActions,
        onOpenDangerZone = onOpenDangerZone
    )
}

@Composable
private fun RepositorySettingsHero(screen: RepositorySettingsScreenState) {
    val colors = SunsetGitHubThemeTokens.colors
    SunsetCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(colors.accent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = screen.repo.firstOrNull()?.uppercaseChar()?.toString() ?: "R",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(
                    text = screen.repo.ifBlank { screen.fullName.substringAfter('/') },
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (screen.summary.isNotBlank()) {
                    Text(
                        modifier = Modifier.padding(top = 8.dp),
                        text = screen.summary,
                        color = colors.textSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val chips = listOf(
                screen.visibilityLabel,
                screen.defaultBranch,
                if (screen.canEdit) stringResource(R.string.repository_settings_toggle_editable) else stringResource(R.string.repository_settings_toggle_readonly),
                screen.basicItems.firstOrNull { it.key == RepositorySettingsInfoKey.Language }?.value.orEmpty(),
                screen.basicItems.firstOrNull { it.key == RepositorySettingsInfoKey.License }?.value.orEmpty()
            ).filter { it.isNotBlank() && it != stringResource(R.string.repository_settings_empty_value) }
            chips.take(3).forEachIndexed { index, chip -> SettingsChip(text = chip, primary = index == 0) }
        }
        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            screen.stats.forEach { item -> StatCell(item, Modifier.weight(1f)) }
        }
        Text(
            modifier = Modifier.padding(top = 12.dp),
            text = if (screen.canEdit) stringResource(R.string.repository_settings_admin_notice) else stringResource(R.string.repository_settings_readonly_notice),
            color = colors.textMuted,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun SettingsChip(text: String, primary: Boolean) {
    val colors = SunsetGitHubThemeTokens.colors
    Text(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(if (primary) colors.accent.copy(alpha = 0.16f) else colors.chipBackground)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        text = text,
        color = if (primary) colors.accent else colors.textSecondary,
        style = MaterialTheme.typography.bodySmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun StatCell(item: RepositorySettingsStatItem, modifier: Modifier = Modifier) {
    val colors = SunsetGitHubThemeTokens.colors
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(colors.chipBackground)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = item.value, color = colors.textPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text(text = item.label, color = colors.textMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun InfoCell(item: RepositorySettingsInfoItem, modifier: Modifier = Modifier) {
    val colors = SunsetGitHubThemeTokens.colors
    Column(
        modifier = modifier
            .padding(bottom = 8.dp)
            .clip(MaterialTheme.shapes.small)
            .background(colors.chipBackground)
            .padding(10.dp)
    ) {
        Text(text = item.label, color = colors.textMuted, style = MaterialTheme.typography.bodySmall)
        Text(
            text = item.value.ifBlank { stringResource(R.string.repository_settings_empty_value) },
            color = colors.textPrimary,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun VisibilitySection(
    option: RepositorySettingsVisibilityOption,
    isSaving: Boolean,
    pendingVisibilitySelection: RepositorySettingsVisibility?,
    onVisibilitySelected: (RepositorySettingsVisibility) -> Unit
) {
    RepositorySettingsSectionCard(
        title = stringResource(R.string.repository_settings_section_visibility),
        description = stringResource(R.string.repository_settings_visibility_subtitle)
    ) {
        val displaySelected = pendingVisibilitySelection?.takeIf { isSaving && it != option.selected } ?: option.selected
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RepositorySettingsVisibility.values().forEach { visibility ->
                val selected = visibility == displaySelected
                val enabled = option.editable && !isSaving && !selected
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    enabled = enabled,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SunsetGitHubThemeTokens.colors.accent),
                    onClick = { onVisibilitySelected(visibility) }
                ) {
                    Text(
                        text = visibility.displayName(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun EditableRow(
    item: RepositorySettingsEditableItem,
    isSaving: Boolean,
    onEditField: (RepositorySettingsEditableItem) -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    val enabled = item.editable && !isSaving
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onEditField(item) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.label, color = colors.textPrimary, style = MaterialTheme.typography.bodyMedium)
            item.helper?.takeIf { it.isNotBlank() }?.let {
                Text(text = it, color = colors.textMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
        Text(
            modifier = Modifier.padding(start = 12.dp),
            text = item.value.ifBlank { stringResource(R.string.repository_settings_empty_value) },
            color = if (enabled) colors.accent else colors.textMuted,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ToggleSection(
    title: String,
    description: String,
    items: List<RepositorySettingsToggleItem>,
    isSaving: Boolean,
    onToggle: (RepositorySettingsToggleItem) -> Unit
) {
    RepositorySettingsSectionCard(title = title, description = description) {
        items.forEach { item -> ToggleRow(item, isSaving, onToggle) }
    }
}

@Composable
private fun ToggleRow(
    item: RepositorySettingsToggleItem,
    isSaving: Boolean,
    onToggle: (RepositorySettingsToggleItem) -> Unit
) {
    val enabled = item.editable && !isSaving
    val stateText = if (item.checked) {
        stringResource(R.string.repository_settings_toggle_on)
    } else {
        stringResource(R.string.repository_settings_toggle_off)
    }
    RepositorySettingsSwitchRow(
        title = item.label,
        description = item.description,
        checked = item.checked,
        enabled = enabled,
        contentDescription = stringResource(
            R.string.repository_settings_toggle_content_description,
            item.label,
            stateText
        ),
        onCheckedChange = { onToggle(item) }
    )
}

@Composable
private fun NoticesSection(notices: List<String>) {
    RepositorySettingsSectionCard(title = stringResource(R.string.repository_settings_notices_title)) {
        if (notices.isEmpty()) {
            Text(
                text = stringResource(R.string.repository_section_native_stub_no_notices),
                color = SunsetGitHubThemeTokens.colors.textSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            notices.forEach { notice ->
                Text(
                    modifier = Modifier.padding(vertical = 3.dp),
                    text = "• $notice",
                    color = SunsetGitHubThemeTokens.colors.textSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun AdvancedSection(
    onOpenBranches: () -> Unit,
    onOpenCollaborators: () -> Unit,
    onOpenRulesets: () -> Unit,
    onOpenWebhooks: () -> Unit,
    onOpenDeployKeys: () -> Unit,
    onOpenActions: () -> Unit,
    onOpenDangerZone: () -> Unit
) {
    RepositorySettingsSectionCard(title = stringResource(R.string.repository_settings_advanced_title)) {
        AdvancedRow(stringResource(R.string.repository_settings_advanced_branches), stringResource(R.string.repository_settings_advanced_branches_description), onOpenBranches)
        AdvancedRow(stringResource(R.string.repository_settings_advanced_collaborators), stringResource(R.string.repository_settings_advanced_collaborators_description), onOpenCollaborators)
        AdvancedRow(stringResource(R.string.repository_settings_advanced_rulesets), stringResource(R.string.repository_settings_advanced_rulesets_description), onOpenRulesets)
        AdvancedRow(stringResource(R.string.repository_settings_advanced_webhooks), stringResource(R.string.repository_settings_advanced_webhooks_description), onOpenWebhooks)
        AdvancedRow(stringResource(R.string.repository_settings_advanced_deploy_keys), stringResource(R.string.repository_settings_advanced_deploy_keys_description), onOpenDeployKeys)
        AdvancedRow(stringResource(R.string.repository_settings_advanced_actions), stringResource(R.string.repository_settings_advanced_actions_description), onOpenActions)
        AdvancedRow(stringResource(R.string.repository_settings_advanced_danger), stringResource(R.string.repository_settings_advanced_danger_description), onOpenDangerZone, danger = true)
    }
}

@Composable
private fun AdvancedRow(title: String, description: String, onClick: () -> Unit, danger: Boolean = false) {
    val colors = SunsetGitHubThemeTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = if (danger) colors.danger else colors.textPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(text = description, color = colors.textMuted, style = MaterialTheme.typography.bodySmall)
        }
        Text(text = ">", color = colors.textMuted, style = MaterialTheme.typography.bodyMedium)
    }
}


@Composable
private fun RepositorySettingsVisibility.displayName(): String = when (this) {
    RepositorySettingsVisibility.Public -> stringResource(R.string.repository_settings_visibility_public)
    RepositorySettingsVisibility.Internal -> stringResource(R.string.repository_settings_visibility_internal)
    RepositorySettingsVisibility.Private -> stringResource(R.string.repository_settings_visibility_private)
}
