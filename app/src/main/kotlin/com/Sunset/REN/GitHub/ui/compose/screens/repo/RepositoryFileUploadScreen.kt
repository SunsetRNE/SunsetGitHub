package com.Sunset.REN.GitHub.ui.compose.screens.repo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.filemanager.FileSizeFormatter
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens
import com.Sunset.REN.GitHub.ui.compose.components.SunsetCard
import com.Sunset.REN.GitHub.ui.compose.components.SunsetPrimaryButton
import com.Sunset.REN.GitHub.ui.compose.components.SunsetSecondaryButton
import com.Sunset.REN.GitHub.ui.repo.RepositoryFileUploadUiState

sealed interface RepositoryFileUploadDialogState {
    data class TargetPathPicker(val options: List<String>) : RepositoryFileUploadDialogState
    data class Conflict(val dialogKey: String, val targetPath: String) : RepositoryFileUploadDialogState
}

@Composable
fun RepositoryFileUploadScreen(
    state: RepositoryFileUploadUiState,
    repositoryContext: String,
    targetPath: String,
    commitMessage: String,
    dialogState: RepositoryFileUploadDialogState?,
    onTargetPathChange: (String) -> Unit,
    onCommitMessageChange: (String) -> Unit,
    onShowTargetPathPicker: () -> Unit,
    onSubmit: () -> Unit,
    onDismissDialog: () -> Unit,
    onTargetPathSelected: (String) -> Unit,
    onOverwriteConflict: () -> Unit,
    onRenameConflict: (RepositoryFileUploadDialogState.Conflict) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        UploadStatusCard(
            repositoryContext = repositoryContext,
            sourceText = buildSourceText(state),
            stateText = buildStateText(state)
        )
        UploadFormCard(
            state = state,
            targetPath = targetPath,
            commitMessage = commitMessage,
            onTargetPathChange = onTargetPathChange,
            onCommitMessageChange = onCommitMessageChange,
            onShowTargetPathPicker = onShowTargetPathPicker,
            onSubmit = onSubmit
        )
    }
    RepositoryFileUploadDialogHost(
        dialogState = dialogState,
        displayName = state.displayName,
        onDismiss = onDismissDialog,
        onTargetPathSelected = onTargetPathSelected,
        onOverwriteConflict = onOverwriteConflict,
        onRenameConflict = onRenameConflict
    )
}

@Composable
private fun UploadStatusCard(repositoryContext: String, sourceText: String, stateText: String) {
    val colors = SunsetGitHubThemeTokens.colors
    SunsetCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(repositoryContext, color = colors.textPrimary, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(sourceText, color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
            Text(stateText, color = colors.textPrimary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun UploadFormCard(
    state: RepositoryFileUploadUiState,
    targetPath: String,
    commitMessage: String,
    onTargetPathChange: (String) -> Unit,
    onCommitMessageChange: (String) -> Unit,
    onShowTargetPathPicker: () -> Unit,
    onSubmit: () -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    SunsetCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.repository_file_upload_submit_file),
                color = colors.textPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = colors.surface,
                border = BorderStroke(1.dp, colors.border),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.repository_file_upload_target_path),
                        color = colors.textSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = targetPath,
                            onValueChange = onTargetPathChange,
                            modifier = Modifier.weight(1f),
                            enabled = !state.isSubmitting,
                            singleLine = true,
                            label = { Text(stringResource(R.string.repository_file_upload_target_path_hint)) }
                        )
                        SunsetSecondaryButton(
                            text = "…",
                            onClick = onShowTargetPathPicker,
                            enabled = !state.isSubmitting,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
            OutlinedTextField(
                value = commitMessage,
                onValueChange = onCommitMessageChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSubmitting,
                singleLine = true,
                label = { Text(stringResource(R.string.repository_file_edit_commit_message_hint)) }
            )
            SunsetPrimaryButton(
                text = if (state.isSubmitting) stringResource(R.string.repository_file_upload_submitting) else stringResource(R.string.repository_file_upload_submit),
                onClick = onSubmit,
                enabled = state.canSubmit && !state.submitSuccess,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun RepositoryFileUploadDialogHost(
    dialogState: RepositoryFileUploadDialogState?,
    displayName: String,
    onDismiss: () -> Unit,
    onTargetPathSelected: (String) -> Unit,
    onOverwriteConflict: () -> Unit,
    onRenameConflict: (RepositoryFileUploadDialogState.Conflict) -> Unit
) {
    when (dialogState) {
        null -> Unit
        is RepositoryFileUploadDialogState.TargetPathPicker -> TargetPathPickerDialog(
            options = dialogState.options,
            displayName = displayName,
            onDismiss = onDismiss,
            onSelected = onTargetPathSelected
        )
        is RepositoryFileUploadDialogState.Conflict -> ConflictDialog(
            state = dialogState,
            onDismiss = onDismiss,
            onOverwrite = onOverwriteConflict,
            onRename = onRenameConflict
        )
    }
}

@Composable
private fun TargetPathPickerDialog(
    options: List<String>,
    displayName: String,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.repository_file_upload_target_path_picker_title)) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = pluralStringResource(
                        R.plurals.repository_file_upload_target_path_picker_candidate_count,
                        options.size,
                        options.size
                    ),
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
                options.forEach { path ->
                    val isRoot = path == "/"
                    val fileName = displayName.substringAfterLast('/').ifBlank { displayName }
                    val directory = path.trim('/')
                    val targetPath = if (directory.isBlank()) fileName else "$directory/$fileName"
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { onSelected(path) },
                        color = colors.surface,
                        border = BorderStroke(1.dp, colors.border),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isRoot) stringResource(R.string.repository_file_upload_target_path_picker_root) else path,
                                color = colors.textPrimary,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isRoot) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = stringResource(R.string.repository_file_upload_target_path_picker_upload_as, targetPath),
                                color = colors.textSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.repository_file_upload_target_path_picker_close)) }
        }
    )
}

@Composable
private fun ConflictDialog(
    state: RepositoryFileUploadDialogState.Conflict,
    onDismiss: () -> Unit,
    onOverwrite: () -> Unit,
    onRename: (RepositoryFileUploadDialogState.Conflict) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.repository_file_conflict_title)) },
        text = { Text(text = stringResource(R.string.repository_file_conflict_message, state.targetPath)) },
        confirmButton = {
            TextButton(onClick = onOverwrite) { Text(text = stringResource(R.string.repository_file_conflict_overwrite)) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.repository_file_conflict_cancel)) }
                TextButton(onClick = { onRename(state) }) { Text(text = stringResource(R.string.repository_file_conflict_rename)) }
            }
        }
    )
}

@Composable
private fun buildSourceText(state: RepositoryFileUploadUiState): String {
    val sourceName = state.displayName.ifBlank { state.sourceUri }
    val sourceLabel = state.sourceSizeBytes?.let { sizeBytes ->
        stringResource(R.string.repository_file_upload_source_with_size, sourceName, FileSizeFormatter.format(sizeBytes))
    } ?: sourceName
    return stringResource(R.string.repository_file_upload_source, sourceLabel)
}

@Composable
private fun buildStateText(state: RepositoryFileUploadUiState): String {
    return when {
        state.isSubmitting -> stringResource(R.string.repository_file_upload_submitting)
        !state.errorMessage.isNullOrBlank() -> stringResource(R.string.repository_file_upload_failed, state.errorMessage)
        state.targetPath.trim().isBlank() -> stringResource(R.string.repository_file_upload_missing_target_path)
        state.sourceUri.isNotBlank() -> stringResource(R.string.repository_file_upload_ready)
        else -> stringResource(R.string.repository_file_upload_preparing)
    }
}