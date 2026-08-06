package com.Sunset.REN.GitHub.ui.compose.screens.repo

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.repo.RepositoryIssue
import com.Sunset.REN.GitHub.domain.repo.RepositoryIssueLabel
import com.Sunset.REN.GitHub.domain.repo.RepositoryLabel
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens
import com.Sunset.REN.GitHub.ui.compose.components.SunsetCard
import com.Sunset.REN.GitHub.ui.compose.components.SunsetLoadingState
import com.Sunset.REN.GitHub.ui.compose.components.SunsetPrimaryButton
import com.Sunset.REN.GitHub.ui.compose.components.SunsetSecondaryButton
import com.Sunset.REN.GitHub.ui.repo.IssueLabelDisplayNames
import com.Sunset.REN.GitHub.ui.repo.RepositoryIssuesUiState

sealed interface RepositoryIssuesDialogState {
    data class CreatorFilter(
        val currentUserLogin: String,
        val selectedCreator: String?
    ) : RepositoryIssuesDialogState

    data class LabelsFilter(
        val labels: List<RepositoryLabel>,
        val selectedLabels: List<String>
    ) : RepositoryIssuesDialogState
}

@Composable
fun RepositoryIssuesScreen(
    state: RepositoryIssuesUiState,
    isLabelFilterExpanded: Boolean,
    dialogState: RepositoryIssuesDialogState?,
    onOpenState: () -> Unit,
    onClosedState: () -> Unit,
    onToggleLabelsExpanded: () -> Unit,
    onSelectLabels: (List<String>) -> Unit,
    onShowCreatorFilter: () -> Unit,
    onDismissDialog: () -> Unit,
    onCreatorSelected: (String?) -> Unit,
    onShowLabelsDialog: () -> Unit,
    onLoadFirstPage: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenIssue: (Int) -> Unit,
    onCreateIssue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    Box(modifier = modifier.fillMaxSize().background(colors.canvas)) {
        Column(Modifier.fillMaxSize()) {
            IssueFilters(
                state = state,
                isLabelFilterExpanded = isLabelFilterExpanded,
                onOpenState = onOpenState,
                onClosedState = onClosedState,
                onToggleLabelsExpanded = onToggleLabelsExpanded,
                onSelectLabels = onSelectLabels,
                onShowCreatorFilter = onShowCreatorFilter,
                onShowLabelsDialog = onShowLabelsDialog
            )
            IssueContent(
                state = state,
                onLoadFirstPage = onLoadFirstPage,
                onLoadMore = onLoadMore,
                onOpenIssue = onOpenIssue,
                modifier = Modifier.weight(1f)
            )
        }
        val showCreateIssueFab = state.owner.isNotBlank() && state.repo.isNotBlank() && (state.canCreateIssue || state.isInitialLoad)
        if (showCreateIssueFab) {
            FloatingActionButton(
                onClick = onCreateIssue,
                modifier = Modifier.align(Alignment.BottomEnd).padding(18.dp),
                containerColor = colors.success,
                contentColor = Color.White
            ) { Text("+", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        }
        RepositoryIssuesDialogHost(
            dialogState = dialogState,
            onDismiss = onDismissDialog,
            onCreatorSelected = onCreatorSelected,
            onLabelsSelected = onSelectLabels,
            onClearLabels = { onSelectLabels(emptyList()) }
        )
    }
}

@Composable
private fun IssueFilters(
    state: RepositoryIssuesUiState,
    isLabelFilterExpanded: Boolean,
    onOpenState: () -> Unit,
    onClosedState: () -> Unit,
    onToggleLabelsExpanded: () -> Unit,
    onSelectLabels: (List<String>) -> Unit,
    onShowCreatorFilter: () -> Unit,
    onShowLabelsDialog: () -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    Column(
        modifier = Modifier.fillMaxWidth().background(colors.surface).padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IssueFilterChip(stringResource(R.string.repository_issues_filter_open), state.state == RepositoryIssuesUiState.OpenState, onOpenState)
            IssueFilterChip(buildCreatorFilterLabel(state), state.selectedCreator != null, onShowCreatorFilter)
            IssueFilterChip(buildLabelsFilterLabel(state, isLabelFilterExpanded), state.selectedLabels.isNotEmpty(), onToggleLabelsExpanded)
            IssueFilterChip(stringResource(R.string.repository_issues_filter_closed), state.state == RepositoryIssuesUiState.ClosedState, onClosedState)
        }
        if (isLabelFilterExpanded) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.availableLabels.isEmpty()) {
                    IssueFilterChip(stringResource(R.string.repository_issues_filter_labels_empty), selected = false, onClick = onShowLabelsDialog)
                } else {
                    state.availableLabels.forEach { label ->
                        val selected = state.selectedLabels.contains(label.name)
                        IssueFilterChip(IssueLabelDisplayNames.displayName(label.name), selected) {
                            onSelectLabels(toggleLabel(state.selectedLabels, label.name))
                        }
                    }
                    if (state.selectedLabels.isNotEmpty()) {
                        IssueFilterChip(stringResource(R.string.repository_issues_filter_clear), selected = false) { onSelectLabels(emptyList()) }
                    }
                }
            }
        }
    }
}

@Composable
private fun IssueFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = SunsetGitHubThemeTokens.colors
    AssistChip(
        onClick = onClick,
        label = { Text(label, fontWeight = FontWeight.Bold) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) colors.accent.copy(alpha = 0.14f) else colors.surface,
            labelColor = if (selected) colors.accent else colors.textSecondary
        ),
        border = BorderStroke(1.dp, if (selected) colors.accent.copy(alpha = 0.25f) else colors.border)
    )
}

@Composable
private fun IssueContent(
    state: RepositoryIssuesUiState,
    onLoadFirstPage: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenIssue: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    val isError = !state.errorMessage.isNullOrBlank()
    val hasItems = state.issues.isNotEmpty()
    val showSkeleton = state.isInitialLoad && !state.isShowingStaleContent
    val showEmpty = !showSkeleton && !state.isInitialLoad && !isError && !hasItems

    LazyColumn(
        modifier = modifier.fillMaxWidth().background(colors.surface),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        if (state.isShowingStaleContent) {
            item { StateText(stringResource(R.string.repository_issues_loading_more)) }
        }
        if (isError && !hasItems) {
            item {
                SunsetCard(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(stringResource(R.string.repository_issues_failed, state.errorMessage.orEmpty()), color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                    SunsetPrimaryButton(text = stringResource(R.string.repository_issues_retry), onClick = onLoadFirstPage, modifier = Modifier.padding(top = 12.dp))
                }
            }
        } else if (showSkeleton) {
            item { SunsetLoadingState(message = stringResource(R.string.repository_issues_loading_more), modifier = Modifier.fillMaxWidth().padding(top = 36.dp)) }
        } else if (showEmpty) {
            item {
                SunsetCard(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(stringResource(R.string.repository_issues_empty), color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        items(state.issues, key = { it.number }) { issue ->
            IssueRow(issue = issue, onOpenIssue = onOpenIssue)
        }
        if (state.hasMore && hasItems) {
            item {
                SunsetSecondaryButton(
                    text = if (state.isLoadingMore) stringResource(R.string.repository_issues_loading_more) else stringResource(R.string.repository_issues_load_more),
                    onClick = onLoadMore,
                    enabled = !state.isLoadingMore,
                    modifier = Modifier.fillMaxWidth().padding(12.dp)
                )
            }
        }
        item { Box(Modifier.padding(bottom = 20.dp)) }
    }
}

@Composable
private fun StateText(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        color = SunsetGitHubThemeTokens.colors.textSecondary,
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun IssueRow(issue: RepositoryIssue, onOpenIssue: (Int) -> Unit) {
    val colors = SunsetGitHubThemeTokens.colors
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onOpenIssue(issue.number) }.padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = if (issue.labels.isNotEmpty()) "🙏" else "⋯",
            modifier = Modifier.size(34.dp),
            color = colors.textPrimary,
            style = MaterialTheme.typography.titleLarge
        )
        Column(Modifier.weight(1f).padding(start = 14.dp, end = 10.dp)) {
            Text(buildIssueCategory(issue), color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
            Text(issue.title, modifier = Modifier.padding(top = 3.dp), color = colors.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (issue.labels.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    issue.labels.take(MaxVisibleIssueLabels).forEach { label -> IssueLabelChip(label) }
                    val hidden = issue.labels.size - MaxVisibleIssueLabels
                    if (hidden > 0) SmallPill("+$hidden")
                }
            }
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallPill("↑ ${maxOf(1, issue.labels.size)}")
                if (issue.commentCount > 0) SmallPill("▢ ${issue.commentCount}")
                if (issue.labels.isNotEmpty()) SmallPill("✓ ${issue.labels.size}", success = true)
            }
        }
        Text(formatIssueMonth(issue.createdAt), color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun IssueLabelChip(label: RepositoryIssueLabel) {
    val background = parseGitHubLabelColor(label.color) ?: SunsetGitHubThemeTokens.colors.chipBackground
    Surface(shape = MaterialTheme.shapes.small, color = background) {
        Text(
            text = IssueLabelDisplayNames.displayName(label.name),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            color = readableLabelTextColor(background),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SmallPill(text: String, success: Boolean = false) {
    val colors = SunsetGitHubThemeTokens.colors
    Surface(
        shape = CircleShape,
        color = if (success) colors.success.copy(alpha = 0.12f) else colors.surface,
        border = BorderStroke(1.dp, if (success) colors.success.copy(alpha = 0.18f) else colors.border)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            color = if (success) colors.success else colors.textSecondary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RepositoryIssuesDialogHost(
    dialogState: RepositoryIssuesDialogState?,
    onDismiss: () -> Unit,
    onCreatorSelected: (String?) -> Unit,
    onLabelsSelected: (List<String>) -> Unit,
    onClearLabels: () -> Unit
) {
    when (dialogState) {
        null -> Unit
        is RepositoryIssuesDialogState.CreatorFilter -> CreatorFilterDialog(dialogState, onDismiss, onCreatorSelected)
        is RepositoryIssuesDialogState.LabelsFilter -> LabelsFilterDialog(dialogState, onDismiss, onLabelsSelected, onClearLabels)
    }
}

@Composable
private fun CreatorFilterDialog(
    state: RepositoryIssuesDialogState.CreatorFilter,
    onDismiss: () -> Unit,
    onCreatorSelected: (String?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.repository_issues_filter_creator_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CreatorFilterOption(stringResource(R.string.repository_issues_filter_creator_all), state.selectedCreator != state.currentUserLogin) { onCreatorSelected(null) }
                CreatorFilterOption(stringResource(R.string.repository_issues_filter_creator_me), state.selectedCreator == state.currentUserLogin) { onCreatorSelected(state.currentUserLogin) }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.repository_issue_detail_dialog_cancel)) } }
    )
}

@Composable
private fun CreatorFilterOption(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick)
        TextButton(onClick = onClick) { Text(text = text) }
    }
}

@Composable
private fun LabelsFilterDialog(
    state: RepositoryIssuesDialogState.LabelsFilter,
    onDismiss: () -> Unit,
    onLabelsSelected: (List<String>) -> Unit,
    onClearLabels: () -> Unit
) {
    val selected = remember(state) { mutableStateListOf<String>().apply { addAll(state.selectedLabels) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.repository_issues_filter_labels_title)) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.labels, key = { it.name }) { label ->
                    val checked = selected.contains(label.name)
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    if (!selected.contains(label.name)) selected.add(label.name)
                                } else {
                                    selected.remove(label.name)
                                }
                            }
                        )
                        Text(text = IssueLabelDisplayNames.displayName(label.name))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onLabelsSelected(selected.toList()) }) { Text(text = stringResource(R.string.repository_issue_detail_dialog_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.repository_issue_detail_dialog_cancel)) } },
        icon = { TextButton(onClick = onClearLabels) { Text(text = stringResource(R.string.repository_issues_filter_clear)) } }
    )
}

@Composable
private fun buildCreatorFilterLabel(state: RepositoryIssuesUiState): String = if (state.selectedCreator != null) {
    stringResource(R.string.repository_issues_filter_creator_me)
} else {
    stringResource(R.string.repository_issues_filter_creator)
}

@Composable
private fun buildLabelsFilterLabel(state: RepositoryIssuesUiState, expanded: Boolean): String = when (state.selectedLabels.size) {
    0 -> if (expanded) stringResource(R.string.repository_issues_filter_labels_collapse) else stringResource(R.string.repository_issues_filter_labels)
    1 -> IssueLabelDisplayNames.displayName(state.selectedLabels.first())
    else -> stringResource(R.string.repository_issues_filter_labels_count, state.selectedLabels.size)
}

private fun toggleLabel(current: List<String>, label: String): List<String> {
    val normalized = current.filterNot { it == label }
    return if (normalized.size == current.size) normalized + label else normalized
}

@Composable
private fun buildIssueCategory(issue: RepositoryIssue): String = if (issue.labels.isNotEmpty()) {
    IssueLabelDisplayNames.displayName(issue.labels.first().name)
} else {
    stringResource(R.string.title_repository_issues)
}

private fun formatIssueMonth(raw: String?): String {
    val month = raw?.substringBefore('T')?.split('-')?.getOrNull(1)?.trimStart('0')?.takeIf { it.isNotBlank() }
    return if (month != null) "${month}月" else ""
}

private fun parseGitHubLabelColor(rawColor: String): Color? {
    val normalized = rawColor.trim().removePrefix("#")
    if (normalized.length != 6) return null
    val parsed = runCatching { AndroidColor.parseColor("#$normalized") }.getOrNull() ?: return null
    return Color(parsed)
}

private fun readableLabelTextColor(backgroundColor: Color): Color {
    val luminance = (0.299f * backgroundColor.red + 0.587f * backgroundColor.green + 0.114f * backgroundColor.blue)
    return if (luminance > 0.55f) Color(0xFF24292F) else Color.White
}

private const val MaxVisibleIssueLabels = 3