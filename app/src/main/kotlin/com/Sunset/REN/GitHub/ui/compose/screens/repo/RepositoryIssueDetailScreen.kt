package com.Sunset.REN.GitHub.ui.compose.screens.repo

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.repo.RepositoryIssueComment
import com.Sunset.REN.GitHub.domain.repo.RepositoryIssueDetail
import com.Sunset.REN.GitHub.domain.repo.RepositoryIssueLabel
import com.Sunset.REN.GitHub.domain.repo.RepositoryLabel
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens
import com.Sunset.REN.GitHub.ui.compose.components.SunsetCard
import com.Sunset.REN.GitHub.ui.compose.components.SunsetPrimaryButton
import com.Sunset.REN.GitHub.ui.compose.components.SunsetSecondaryButton
import com.Sunset.REN.GitHub.ui.repo.IssueLabelDisplayNames
import com.Sunset.REN.GitHub.ui.repo.RepositoryIssueDetailUiState
import com.Sunset.REN.GitHub.ui.repo.RepositoryIssuesUiState

sealed interface RepositoryIssueDetailDialogState {
    data class EditComment(
        val commentId: Long,
        val body: String
    ) : RepositoryIssueDetailDialogState

    data class LabelsPicker(
        val labels: List<RepositoryLabel>,
        val selectedLabels: List<String>
    ) : RepositoryIssueDetailDialogState

    data class DeleteComment(
        val commentId: Long
    ) : RepositoryIssueDetailDialogState
}

@Composable
fun RepositoryIssueDetailScreen(
    state: RepositoryIssueDetailUiState,
    commentDraft: String,
    dialogState: RepositoryIssueDetailDialogState?,
    onCommentDraftChange: (String) -> Unit,
    onRetry: () -> Unit,
    onLoadMoreComments: () -> Unit,
    onToggleIssueState: () -> Unit,
    onShowLabelsPicker: () -> Unit,
    onCreateComment: () -> Unit,
    onShowEditComment: (RepositoryIssueComment) -> Unit,
    onShowDeleteComment: (RepositoryIssueComment) -> Unit,
    onDismissDialog: () -> Unit,
    onCommentSave: (Long, String) -> Unit,
    onLabelsSave: (List<String>) -> Unit,
    onDeleteComment: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    Box(modifier = modifier.fillMaxSize().background(colors.canvas)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                state.isInitialLoad -> item { StateCard(text = stringResource(R.string.repository_issue_detail_loading)) }
                state.errorMessage != null && state.issue == null -> item {
                    StateCard(
                        text = stringResource(R.string.repository_issue_detail_failed, state.errorMessage),
                        actionText = stringResource(R.string.repository_issues_retry),
                        onAction = onRetry
                    )
                }
                state.issue != null -> {
                    item {
                        IssueHeadCard(
                            state = state,
                            onToggleIssueState = onToggleIssueState,
                            onShowLabelsPicker = onShowLabelsPicker
                        )
                    }
                    item {
                        Text(
                            text = stringResource(R.string.repository_issue_detail_comments_title),
                            color = colors.textPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (state.comments.isEmpty()) {
                        item { StateCard(text = stringResource(R.string.repository_issue_detail_no_comments)) }
                    } else {
                        items(state.comments, key = { it.id }) { comment ->
                            CommentCard(
                                comment = comment,
                                canManage = state.canManageComment(comment.authorLogin),
                                onEdit = { onShowEditComment(comment) },
                                onDelete = { onShowDeleteComment(comment) }
                            )
                        }
                    }
                    if (state.hasMoreComments && state.comments.isNotEmpty()) {
                        item {
                            SunsetSecondaryButton(
                                text = if (state.isLoadingMoreComments) {
                                    stringResource(R.string.repository_issue_detail_loading_more_comments)
                                } else {
                                    stringResource(R.string.repository_issue_detail_load_more_comments)
                                },
                                onClick = onLoadMoreComments,
                                enabled = !state.isLoadingMoreComments,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    if (state.isSignedIn) {
                        item {
                            CommentComposer(
                                draft = commentDraft,
                                isMutating = state.isMutating,
                                onDraftChange = onCommentDraftChange,
                                onSend = onCreateComment
                            )
                        }
                    }
                }
            }
        }
        RepositoryIssueDetailDialogHost(
            dialogState = dialogState,
            onDismiss = onDismissDialog,
            onCommentSave = onCommentSave,
            onLabelsSave = onLabelsSave,
            onDeleteComment = onDeleteComment
        )
    }
}

@Composable
private fun StateCard(text: String, actionText: String? = null, onAction: (() -> Unit)? = null) {
    val colors = SunsetGitHubThemeTokens.colors
    SunsetCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = text, color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
            if (actionText != null && onAction != null) {
                SunsetSecondaryButton(text = actionText, onClick = onAction)
            }
        }
    }
}

@Composable
private fun IssueHeadCard(
    state: RepositoryIssueDetailUiState,
    onToggleIssueState: () -> Unit,
    onShowLabelsPicker: () -> Unit
) {
    val issue = state.issue ?: return
    val colors = SunsetGitHubThemeTokens.colors
    SunsetCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.repository_issue_detail_title, issue.number, issue.title),
                color = colors.textPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                IssueStateChip(issue.state)
                issue.labels.take(MaxLabels).forEach { label -> IssueLabelChip(label) }
            }
            Text(text = issueMeta(issue), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = issue.body.takeIf { it.isNotBlank() } ?: stringResource(R.string.repository_issue_detail_no_body),
                    modifier = Modifier.padding(12.dp),
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (state.canToggleState || state.canEditLabels) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (state.canToggleState) {
                        SunsetSecondaryButton(
                            text = if (issue.state == RepositoryIssuesUiState.ClosedState) {
                                stringResource(R.string.repository_issue_detail_reopen)
                            } else {
                                stringResource(R.string.repository_issue_detail_close)
                            },
                            onClick = onToggleIssueState,
                            enabled = !state.isMutating
                        )
                    }
                    if (state.canEditLabels) {
                        SunsetSecondaryButton(
                            text = stringResource(R.string.repository_issue_detail_edit_labels),
                            onClick = onShowLabelsPicker,
                            enabled = !state.isMutating
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentCard(comment: RepositoryIssueComment, canManage: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    val colors = SunsetGitHubThemeTokens.colors
    SunsetCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = commentMeta(comment), modifier = Modifier.weight(1f), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                if (canManage) {
                    TextButton(onClick = onEdit) { Text(stringResource(R.string.repository_issue_detail_comment_edit)) }
                    TextButton(onClick = onDelete) { Text(stringResource(R.string.repository_issue_detail_comment_delete)) }
                }
            }
            Text(
                text = comment.body,
                color = colors.textPrimary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun CommentComposer(
    draft: String,
    isMutating: Boolean,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit
) {
    SunsetCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.End) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.repository_issue_detail_comment_hint)) },
                minLines = 3,
                enabled = !isMutating
            )
            SunsetPrimaryButton(
                text = if (isMutating) stringResource(R.string.repository_issue_detail_sending_comment) else stringResource(R.string.repository_issue_detail_send_comment),
                onClick = onSend,
                enabled = !isMutating
            )
        }
    }
}

@Composable
private fun IssueStateChip(state: String) {
    val colors = SunsetGitHubThemeTokens.colors
    val isClosed = state == RepositoryIssuesUiState.ClosedState
    AssistChip(
        onClick = {},
        label = { Text(if (isClosed) stringResource(R.string.repository_issues_state_closed) else stringResource(R.string.repository_issues_state_open)) },
        colors = AssistChipDefaults.assistChipColors(containerColor = colors.accentSoft, labelColor = colors.accent),
        border = BorderStroke(1.dp, colors.accentSoftBorder)
    )
}

@Composable
private fun IssueLabelChip(label: RepositoryIssueLabel) {
    val backgroundColor = parseGitHubLabelColor(label.color) ?: Color(0xFFEAECEF)
    val textColor = readableLabelTextColor(backgroundColor)
    AssistChip(
        onClick = {},
        label = { Text(IssueLabelDisplayNames.displayName(label.name)) },
        colors = AssistChipDefaults.assistChipColors(containerColor = backgroundColor, labelColor = textColor),
        border = null
    )
}

@Composable
private fun issueMeta(issue: RepositoryIssueDetail): String {
    val date = issue.createdAt?.let { formatIssueDate(it) }
    return if (date != null) {
        stringResource(R.string.repository_issue_detail_meta, issue.authorLogin, date, issue.commentCount)
    } else {
        stringResource(R.string.repository_issue_detail_meta_no_date, issue.authorLogin, issue.commentCount)
    }
}

@Composable
private fun commentMeta(comment: RepositoryIssueComment): String {
    val date = comment.createdAt?.let { formatIssueDate(it) }
    return if (date != null) {
        stringResource(R.string.repository_issue_detail_comment_meta, comment.authorLogin, date)
    } else {
        stringResource(R.string.repository_issue_detail_comment_meta_no_date, comment.authorLogin)
    }
}

@Composable
private fun RepositoryIssueDetailDialogHost(
    dialogState: RepositoryIssueDetailDialogState?,
    onDismiss: () -> Unit,
    onCommentSave: (Long, String) -> Unit,
    onLabelsSave: (List<String>) -> Unit,
    onDeleteComment: (Long) -> Unit
) {
    when (dialogState) {
        null -> Unit
        is RepositoryIssueDetailDialogState.EditComment -> EditCommentDialog(dialogState, onDismiss, onCommentSave)
        is RepositoryIssueDetailDialogState.LabelsPicker -> LabelsPickerDialog(dialogState, onDismiss, onLabelsSave)
        is RepositoryIssueDetailDialogState.DeleteComment -> DeleteCommentDialog(dialogState, onDismiss, onDeleteComment)
    }
}

@Composable
private fun EditCommentDialog(
    state: RepositoryIssueDetailDialogState.EditComment,
    onDismiss: () -> Unit,
    onSave: (Long, String) -> Unit
) {
    var body by remember(state) { mutableStateOf(state.body) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.repository_issue_detail_edit_comment_title)) },
        text = {
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        },
        confirmButton = { TextButton(onClick = { onSave(state.commentId, body.trim()) }) { Text(text = stringResource(R.string.repository_issue_detail_dialog_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.repository_issue_detail_dialog_cancel)) } }
    )
}

@Composable
private fun DeleteCommentDialog(
    state: RepositoryIssueDetailDialogState.DeleteComment,
    onDismiss: () -> Unit,
    onDelete: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.repository_issue_detail_delete_comment_title)) },
        text = { Text(text = stringResource(R.string.repository_issue_detail_delete_comment_message)) },
        confirmButton = { TextButton(onClick = { onDelete(state.commentId) }) { Text(text = stringResource(R.string.repository_issue_detail_dialog_delete)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.repository_issue_detail_dialog_cancel)) } }
    )
}

@Composable
private fun LabelsPickerDialog(
    state: RepositoryIssueDetailDialogState.LabelsPicker,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    val selected = remember(state) { mutableStateListOf<String>().apply { addAll(state.selectedLabels) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.repository_issue_detail_labels_dialog_title)) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.labels.forEach { label ->
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
        confirmButton = { TextButton(onClick = { onSave(selected.toList()) }) { Text(text = stringResource(R.string.repository_issue_detail_dialog_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.repository_issue_detail_dialog_cancel)) } }
    )
}

private fun parseGitHubLabelColor(rawColor: String): Color? {
    val normalized = rawColor.trim().removePrefix("#")
    if (normalized.length != 6) return null
    return runCatching { Color(AndroidColor.parseColor("#$normalized")) }.getOrNull()
}

private fun readableLabelTextColor(backgroundColor: Color): Color {
    val red = backgroundColor.red
    val green = backgroundColor.green
    val blue = backgroundColor.blue
    val luminance = 0.299f * red + 0.587f * green + 0.114f * blue
    return if (luminance > 0.55f) Color(0xFF24292F) else Color.White
}

private fun formatIssueDate(raw: String): String = raw.substringBefore('T').takeIf { it.isNotBlank() } ?: raw

private const val MaxLabels = 6
