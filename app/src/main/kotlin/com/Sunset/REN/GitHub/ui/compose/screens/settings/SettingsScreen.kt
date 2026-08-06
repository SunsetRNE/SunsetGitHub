package com.Sunset.REN.GitHub.ui.compose.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens
import com.Sunset.REN.GitHub.ui.compose.components.SunsetDivider
import com.Sunset.REN.GitHub.ui.compose.components.SunsetPrimaryButton
import com.Sunset.REN.GitHub.ui.compose.components.SunsetSecondaryButton
import com.Sunset.REN.GitHub.ui.compose.components.SunsetSectionCard
import com.Sunset.REN.GitHub.ui.compose.components.SunsetSwitchRow
import com.Sunset.REN.GitHub.ui.repo.RepositorySection

@Composable
fun SettingsScreen(
    floatingNavigationEnabled: Boolean,
    soraEditorEnabled: Boolean,
    uiDebugOverlayEnabled: Boolean,
    showUiDebugOverlaySetting: Boolean,
    repositorySectionOrder: List<RepositorySection>,
    onFloatingNavigationChange: (Boolean) -> Unit,
    onSoraEditorChange: (Boolean) -> Unit,
    onUiDebugOverlayChange: (Boolean) -> Unit,
    onRepositorySectionOrderChange: (List<RepositorySection>) -> Unit,
    onOpenAccountPage: () -> Unit,
    onOpenWorkspaceSync: () -> Unit,
    onOpenWorkspaceTerminal: () -> Unit,
    onOpenAppLog: () -> Unit,
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
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                color = colors.textPrimary,
                fontSize = 28.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Bold
            )
            SunsetSectionCard(
                title = stringResource(R.string.settings_account_section_title),
                description = stringResource(R.string.settings_account_section_description)
            ) {
                SunsetPrimaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    text = stringResource(R.string.settings_open_account_page),
                    onClick = onOpenAccountPage
                )
            }
            SunsetSectionCard(
                title = stringResource(R.string.settings_theme_section_title),
                description = stringResource(R.string.settings_theme_section_description)
            ) {
                SunsetSwitchRow(
                    title = stringResource(R.string.settings_floating_navigation_title),
                    description = stringResource(R.string.settings_floating_navigation_description),
                    checked = floatingNavigationEnabled,
                    onCheckedChange = onFloatingNavigationChange
                )
                SunsetDivider()
                SunsetSwitchRow(
                    title = stringResource(R.string.settings_sora_editor_title),
                    description = stringResource(R.string.settings_sora_editor_description),
                    checked = soraEditorEnabled,
                    onCheckedChange = onSoraEditorChange
                )
                if (showUiDebugOverlaySetting) {
                    SunsetDivider()
                    SunsetSwitchRow(
                        title = stringResource(R.string.settings_ui_debug_overlay_title),
                        description = stringResource(R.string.settings_ui_debug_overlay_description),
                        checked = uiDebugOverlayEnabled,
                        onCheckedChange = onUiDebugOverlayChange
                    )
                    SunsetSecondaryButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        text = "查看应用日志",
                        onClick = onOpenAppLog
                    )
                }
                SunsetDivider(modifier = Modifier.padding(top = 18.dp, bottom = 18.dp))
                Text(
                    text = stringResource(R.string.settings_color_theme_title),
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    modifier = Modifier.padding(top = 6.dp),
                    text = stringResource(R.string.settings_color_theme_black_white),
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = stringResource(R.string.settings_color_theme_boundary),
                    color = colors.textMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            SunsetSectionCard(
                title = stringResource(R.string.settings_repository_navigation_section_title),
                description = stringResource(R.string.settings_repository_navigation_section_description)
            ) {
                RepositorySectionOrderList(
                    sections = repositorySectionOrder,
                    onOrderChange = onRepositorySectionOrderChange
                )
            }
            SunsetSectionCard(
                title = stringResource(R.string.settings_workspace_section_title),
                description = stringResource(R.string.settings_workspace_section_description)
            ) {
                WorkspaceStep(number = "1", text = stringResource(R.string.settings_workspace_step_create_or_import))
                SunsetSecondaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    text = stringResource(R.string.settings_workspace_open_sync),
                    onClick = onOpenWorkspaceSync
                )
                WorkspaceStep(
                    modifier = Modifier.padding(top = 12.dp),
                    number = "2",
                    text = stringResource(R.string.settings_workspace_step_check_plan)
                )
                SunsetPrimaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    text = stringResource(R.string.settings_workspace_open_terminal),
                    onClick = onOpenWorkspaceTerminal
                )
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = stringResource(R.string.settings_workspace_terminal_description),
                    color = colors.textMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

// Shared settings card, switch row, and divider components live in ui.compose.components.

@Composable
private fun RepositorySectionOrderList(
    sections: List<RepositorySection>,
    onOrderChange: (List<RepositorySection>) -> Unit
) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        sections.forEachIndexed { index, section ->
            RepositorySectionOrderRow(
                section = section,
                isFirst = index == 0,
                isLast = index == sections.lastIndex,
                onMoveUp = {
                    if (index > 0) onOrderChange(sections.move(index, index - 1))
                },
                onMoveDown = {
                    if (index < sections.lastIndex) onOrderChange(sections.move(index, index + 1))
                }
            )
        }
    }
}

@Composable
private fun RepositorySectionOrderRow(
    section: RepositorySection,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    val moveUpContentDescription = stringResource(R.string.settings_repository_section_move_up)
    val moveDownContentDescription = stringResource(R.string.settings_repository_section_move_down)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            modifier = Modifier.size(24.dp),
            painter = painterResource(section.navigationIconResId),
            contentDescription = null,
            colorFilter = ColorFilter.tint(colors.textSecondary)
        )
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            text = stringResource(section.titleResId),
            color = colors.textPrimary,
            style = MaterialTheme.typography.bodyMedium
        )
        IconButton(
            modifier = Modifier.semantics {
                contentDescription = moveUpContentDescription
            },
            enabled = !isFirst,
            onClick = onMoveUp
        ) {
            Text(
                text = "↑",
                color = if (isFirst) colors.textMuted else colors.accent,
                fontWeight = FontWeight.Bold
            )
        }
        IconButton(
            modifier = Modifier.semantics {
                contentDescription = moveDownContentDescription
            },
            enabled = !isLast,
            onClick = onMoveDown
        ) {
            Text(
                text = "↓",
                color = if (isLast) colors.textMuted else colors.accent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun WorkspaceStep(
    number: String,
    text: String,
    modifier: Modifier = Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(colors.accentSoft),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                color = colors.accent,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            text = text,
            color = colors.textSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun <T> List<T>.move(fromIndex: Int, toIndex: Int): List<T> {
    if (fromIndex !in indices || toIndex !in indices || fromIndex == toIndex) return this
    return toMutableList().apply {
        val item = removeAt(fromIndex)
        add(toIndex, item)
    }
}
