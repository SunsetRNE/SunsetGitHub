package com.Sunset.REN.GitHub.ui.compose.screens.repo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.data.github.html.RepositoryActionsCacheItem
import com.Sunset.REN.GitHub.data.github.html.RepositoryActionsSettingsSnapshot
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens
import com.Sunset.REN.GitHub.ui.compose.components.SunsetCard
import com.Sunset.REN.GitHub.ui.repo.RepositoryActionsSettingsMetric
import com.Sunset.REN.GitHub.ui.repo.RepositoryActionsSettingsUiState
import com.Sunset.REN.GitHub.ui.repo.toAllowedActionsText
import com.Sunset.REN.GitHub.ui.repo.toReadableBytes
import com.Sunset.REN.GitHub.ui.repo.toWorkflowPermissionText

sealed interface RepositoryActionsSettingsDialogState {
    data class ConfirmActionsEnabled(val enabled: Boolean) : RepositoryActionsSettingsDialogState
    data object ConfirmWorkflowWrite : RepositoryActionsSettingsDialogState
    data class Retention(val currentDays: Int?) : RepositoryActionsSettingsDialogState
    data class CacheAction(val cache: RepositoryActionsCacheItem) : RepositoryActionsSettingsDialogState
    data object DeleteCacheByKey : RepositoryActionsSettingsDialogState
    data class SelectedPatterns(
        val githubOwned: Boolean,
        val verified: Boolean,
        val patterns: List<String>
    ) : RepositoryActionsSettingsDialogState
    data class SecretEditor(val name: String) : RepositoryActionsSettingsDialogState
    data class VariableEditor(val name: String, val value: String) : RepositoryActionsSettingsDialogState
    data class SecretAction(val name: String) : RepositoryActionsSettingsDialogState
    data class VariableAction(val name: String, val value: String) : RepositoryActionsSettingsDialogState
    data class DeleteSecret(val name: String) : RepositoryActionsSettingsDialogState
    data class DeleteVariable(val name: String) : RepositoryActionsSettingsDialogState
}

@Composable
fun RepositoryActionsSettingsScreen(
    state: RepositoryActionsSettingsUiState,
    onRetry: () -> Unit,
    onSetActionsEnabled: (Boolean) -> Unit,
    onSetAllowedActions: (String) -> Unit,
    onSetWorkflowPermission: (String) -> Unit,
    onToggleWorkflowPrApproval: () -> Unit,
    onEditRetention: (Int?) -> Unit,
    onRefreshSecretsVariables: () -> Unit,
    onSecretClick: (String) -> Unit,
    onVariableClick: (String, String) -> Unit,
    onUpsertSecret: () -> Unit,
    onUpsertVariable: () -> Unit,
    onRefreshCaches: () -> Unit,
    onDeleteCachesByKey: () -> Unit,
    onCacheClick: (RepositoryActionsCacheItem) -> Unit,
    onToggleGithubOwnedSelectedActions: () -> Unit,
    onToggleVerifiedSelectedActions: () -> Unit,
    onEditSelectedPatterns: () -> Unit,
    dialogState: RepositoryActionsSettingsDialogState?,
    onDismissDialog: () -> Unit,
    onConfirmActionsEnabled: (Boolean) -> Unit,
    onConfirmWorkflowWrite: () -> Unit,
    onSaveRetention: (String) -> Unit,
    onDeleteCache: (RepositoryActionsCacheItem) -> Unit,
    onDeleteCachesByKeyConfirmed: (String, String) -> Unit,
    onSaveSelectedPatterns: (Boolean, Boolean, String) -> Unit,
    onSaveSecret: (String, String) -> Unit,
    onSaveVariable: (String, String) -> Unit,
    onRequestSecretEdit: (String) -> Unit,
    onRequestSecretDelete: (String) -> Unit,
    onRequestVariableEdit: (String, String) -> Unit,
    onRequestVariableDelete: (String) -> Unit,
    onDeleteSecret: (String) -> Unit,
    onDeleteVariable: (String) -> Unit,
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                state.isInitialLoad -> RepositorySettingsStateCard(
                    title = stringResource(R.string.repository_actions_settings_loading_title),
                    message = stringResource(R.string.repository_actions_settings_loading),
                    showProgress = true,
                    retryText = stringResource(R.string.repository_actions_retry),
                    onRetry = null
                )
                state.errorMessage != null && state.snapshot == null -> RepositorySettingsStateCard(
                    title = stringResource(R.string.repository_actions_settings_error_title),
                    message = state.errorMessage,
                    showProgress = false,
                    retryText = stringResource(R.string.repository_actions_retry),
                    onRetry = onRetry
                )
                state.snapshot != null -> ActionsContent(
                    snapshot = state.snapshot,
                    metrics = state.metrics,
                    state = state,
                    inlineMessage = when {
                        state.isSaving -> state.pendingMessage.orEmpty()
                        !state.errorMessage.isNullOrBlank() -> state.errorMessage
                        else -> ""
                    },
                    onSetActionsEnabled = onSetActionsEnabled,
                    onSetAllowedActions = onSetAllowedActions,
                    onSetWorkflowPermission = onSetWorkflowPermission,
                    onToggleWorkflowPrApproval = onToggleWorkflowPrApproval,
                    onEditRetention = onEditRetention,
                    onRefreshSecretsVariables = onRefreshSecretsVariables,
                    onSecretClick = onSecretClick,
                    onVariableClick = onVariableClick,
                    onUpsertSecret = onUpsertSecret,
                    onUpsertVariable = onUpsertVariable,
                    onRefreshCaches = onRefreshCaches,
                    onDeleteCachesByKey = onDeleteCachesByKey,
                    onCacheClick = onCacheClick,
                    onToggleGithubOwnedSelectedActions = onToggleGithubOwnedSelectedActions,
                    onToggleVerifiedSelectedActions = onToggleVerifiedSelectedActions,
                    onEditSelectedPatterns = onEditSelectedPatterns
                )
            }
        }
    }
    RepositoryActionsSettingsDialogHost(
        dialogState = dialogState,
        onDismiss = onDismissDialog,
        onConfirmActionsEnabled = onConfirmActionsEnabled,
        onConfirmWorkflowWrite = onConfirmWorkflowWrite,
        onSaveRetention = onSaveRetention,
        onDeleteCache = onDeleteCache,
        onDeleteCachesByKeyConfirmed = onDeleteCachesByKeyConfirmed,
        onSaveSelectedPatterns = onSaveSelectedPatterns,
        onSaveSecret = onSaveSecret,
        onSaveVariable = onSaveVariable,
        onRequestSecretEdit = onRequestSecretEdit,
        onRequestSecretDelete = onRequestSecretDelete,
        onRequestVariableEdit = onRequestVariableEdit,
        onRequestVariableDelete = onRequestVariableDelete,
        onDeleteSecret = onDeleteSecret,
        onDeleteVariable = onDeleteVariable
    )
}

// Loading and error state card is shared by repository settings screens.

@Composable
private fun ActionsContent(
    snapshot: RepositoryActionsSettingsSnapshot,
    metrics: List<RepositoryActionsSettingsMetric>,
    state: RepositoryActionsSettingsUiState,
    inlineMessage: String,
    onSetActionsEnabled: (Boolean) -> Unit,
    onSetAllowedActions: (String) -> Unit,
    onSetWorkflowPermission: (String) -> Unit,
    onToggleWorkflowPrApproval: () -> Unit,
    onEditRetention: (Int?) -> Unit,
    onRefreshSecretsVariables: () -> Unit,
    onSecretClick: (String) -> Unit,
    onVariableClick: (String, String) -> Unit,
    onUpsertSecret: () -> Unit,
    onUpsertVariable: () -> Unit,
    onRefreshCaches: () -> Unit,
    onDeleteCachesByKey: () -> Unit,
    onCacheClick: (RepositoryActionsCacheItem) -> Unit,
    onToggleGithubOwnedSelectedActions: () -> Unit,
    onToggleVerifiedSelectedActions: () -> Unit,
    onEditSelectedPatterns: () -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    val enabled = snapshot.actionsPermissions?.enabled == true
    val canEdit = snapshot.canAdmin && !state.isSaving
    RepositorySettingsInlineMessageCard(inlineMessage)
    SunsetCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (enabled) {
                stringResource(R.string.repository_actions_settings_enabled)
            } else {
                stringResource(R.string.repository_actions_settings_disabled)
            },
            color = if (enabled) colors.accent else colors.danger,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = stringResource(
                R.string.repository_actions_settings_summary,
                if (enabled) stringResource(R.string.repository_settings_toggle_on) else stringResource(R.string.repository_settings_toggle_off),
                snapshot.workflowPermissions?.defaultWorkflowPermissions.toWorkflowPermissionText(),
                if (snapshot.canAdmin) stringResource(R.string.repository_settings_toggle_editable) else stringResource(R.string.repository_settings_toggle_readonly)
            ),
            color = colors.textSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
    RepositorySettingsSectionCard(title = stringResource(R.string.repository_actions_settings_section_overview)) {
        metrics.forEach { RepositorySettingsMetricRow(it.label, it.value) }
    }
    RepositorySettingsSectionCard(title = stringResource(R.string.repository_actions_settings_section_permissions), description = stringResource(R.string.repository_actions_settings_permissions_description)) {
        RepositorySettingsActionRow(stringResource(R.string.repository_actions_settings_enable_all), selected = enabled, enabled = canEdit && !enabled) { onSetActionsEnabled(true) }
        RepositorySettingsActionRow(stringResource(R.string.repository_actions_settings_disable_all), selected = !enabled, enabled = canEdit && enabled) { onSetActionsEnabled(false) }
        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = stringResource(R.string.repository_actions_settings_allowed_actions, snapshot.actionsPermissions?.allowedActions.toAllowedActionsText()),
            color = colors.textMuted,
            style = MaterialTheme.typography.bodySmall
        )
        val allowed = snapshot.actionsPermissions?.allowedActions ?: "unknown"
        RepositorySettingsActionRow(stringResource(R.string.repository_actions_settings_allow_all_workflows), selected = allowed == "all", enabled = canEdit && allowed != "all") { onSetAllowedActions("all") }
        RepositorySettingsActionRow(stringResource(R.string.repository_actions_settings_allow_local_workflows), selected = allowed == "local_only", enabled = canEdit && allowed != "local_only") { onSetAllowedActions("local_only") }
        RepositorySettingsActionRow(stringResource(R.string.repository_actions_settings_allow_selected_workflows), selected = allowed == "selected", enabled = canEdit && allowed != "selected") { onSetAllowedActions("selected") }
        snapshot.selectedActions?.let { selected ->
            Text(
                modifier = Modifier.padding(top = 12.dp),
                text = stringResource(R.string.repository_actions_settings_selected_actions_section),
                color = colors.textPrimary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            RepositorySettingsActionRow(stringResource(R.string.repository_actions_settings_allow_github_owned), selected = selected.githubOwnedAllowed, enabled = canEdit, onClick = onToggleGithubOwnedSelectedActions)
            RepositorySettingsActionRow(stringResource(R.string.repository_actions_settings_allow_verified_marketplace), selected = selected.verifiedAllowed, enabled = canEdit, onClick = onToggleVerifiedSelectedActions)
            Text(
                modifier = Modifier.padding(top = 6.dp),
                text = stringResource(
                    R.string.repository_actions_settings_allowed_patterns,
                    selected.patternsAllowed.joinToString().ifBlank {
                        stringResource(R.string.repository_actions_settings_allowed_patterns_empty)
                    }
                ),
                color = colors.textMuted,
                style = MaterialTheme.typography.bodySmall
            )
            RepositorySettingsActionRow(stringResource(R.string.repository_actions_settings_edit_allowed_patterns), selected = false, enabled = canEdit, onClick = onEditSelectedPatterns)
        }
    }
    RepositorySettingsSectionCard(title = stringResource(R.string.repository_actions_settings_section_workflow), description = stringResource(R.string.repository_actions_settings_workflow_description)) {
        val current = snapshot.workflowPermissions?.defaultWorkflowPermissions ?: "read"
        RepositorySettingsActionRow(stringResource(R.string.repository_actions_settings_workflow_read), selected = current == "read", enabled = canEdit && current != "read") { onSetWorkflowPermission("read") }
        RepositorySettingsActionRow(stringResource(R.string.repository_actions_settings_workflow_write), selected = current == "write", enabled = canEdit && current != "write") { onSetWorkflowPermission("write") }
        RepositorySettingsActionRow(stringResource(R.string.repository_actions_settings_workflow_pr_approval), selected = snapshot.workflowPermissions?.canApprovePullRequestReviews == true, enabled = canEdit, onClick = onToggleWorkflowPrApproval)
    }
    RepositorySettingsSectionCard(
        title = stringResource(R.string.repository_actions_settings_retention_section),
        description = stringResource(
            R.string.repository_actions_settings_retention_description,
            snapshot.retentionDays?.let {
                stringResource(R.string.repository_actions_settings_retention_days, it)
            } ?: stringResource(R.string.repository_actions_settings_unreadable)
        )
    ) {
        RepositorySettingsActionRow(stringResource(R.string.repository_actions_settings_edit_retention), selected = false, enabled = canEdit) { onEditRetention(snapshot.retentionDays) }
    }
    RepositorySettingsSectionCard(title = stringResource(R.string.repository_actions_settings_section_storage)) {
        RepositorySettingsMetricRow(stringResource(R.string.repository_actions_settings_secret_metric), snapshot.secretsCount?.toString() ?: stringResource(R.string.repository_actions_settings_inaccessible))
        RepositorySettingsMetricRow(stringResource(R.string.repository_actions_settings_variable_metric), snapshot.variablesCount?.toString() ?: stringResource(R.string.repository_actions_settings_inaccessible))
        RepositorySettingsActionRow(stringResource(R.string.repository_actions_settings_refresh_secrets_variables), selected = false, enabled = !state.isSaving, onClick = onRefreshSecretsVariables)
        state.secrets.forEach { secret ->
            RepositorySettingsActionRow(stringResource(R.string.repository_actions_settings_secret_row, secret.name, secret.updatedAt), selected = false, enabled = canEdit) { onSecretClick(secret.name) }
        }
        state.variables.forEach { variable ->
            RepositorySettingsActionRow(stringResource(R.string.repository_actions_settings_variable_row, variable.name, variable.value), selected = false, enabled = canEdit) { onVariableClick(variable.name, variable.value) }
        }
        RepositorySettingsActionRow(stringResource(R.string.repository_actions_settings_upsert_secret), selected = false, enabled = canEdit, onClick = onUpsertSecret)
        RepositorySettingsActionRow(stringResource(R.string.repository_actions_settings_upsert_variable), selected = false, enabled = canEdit, onClick = onUpsertVariable)
        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = stringResource(R.string.repository_actions_settings_storage_note),
            color = colors.textMuted,
            style = MaterialTheme.typography.bodySmall
        )
    }
    RepositorySettingsSectionCard(title = stringResource(R.string.repository_actions_settings_cache_section)) {
        RepositorySettingsMetricRow(
            stringResource(R.string.repository_actions_settings_current_usage),
            snapshot.cacheUsage?.let {
                stringResource(R.string.repository_actions_settings_cache_usage, it.activeCachesCount, it.activeCachesSizeInBytes.toReadableBytes())
            } ?: stringResource(R.string.repository_actions_settings_unreadable)
        )
        RepositorySettingsActionRow(stringResource(R.string.repository_actions_settings_refresh_caches), selected = false, enabled = !state.isSaving, onClick = onRefreshCaches)
        RepositorySettingsActionRow(stringResource(R.string.repository_actions_settings_delete_caches_by_key), selected = false, enabled = canEdit, onClick = onDeleteCachesByKey)
        if (state.caches.isEmpty()) {
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = stringResource(R.string.repository_actions_settings_caches_empty),
                color = colors.textMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }
        state.caches.forEach { cache ->
            RepositorySettingsActionRow(
                text = stringResource(R.string.repository_actions_settings_cache_row, cache.key, cache.ref, cache.sizeInBytes.toReadableBytes()),
                selected = false,
                enabled = canEdit
            ) { onCacheClick(cache) }
        }
    }
}




@Composable
private fun RepositoryActionsSettingsDialogHost(
    dialogState: RepositoryActionsSettingsDialogState?,
    onDismiss: () -> Unit,
    onConfirmActionsEnabled: (Boolean) -> Unit,
    onConfirmWorkflowWrite: () -> Unit,
    onSaveRetention: (String) -> Unit,
    onDeleteCache: (RepositoryActionsCacheItem) -> Unit,
    onDeleteCachesByKeyConfirmed: (String, String) -> Unit,
    onSaveSelectedPatterns: (Boolean, Boolean, String) -> Unit,
    onSaveSecret: (String, String) -> Unit,
    onSaveVariable: (String, String) -> Unit,
    onRequestSecretEdit: (String) -> Unit,
    onRequestSecretDelete: (String) -> Unit,
    onRequestVariableEdit: (String, String) -> Unit,
    onRequestVariableDelete: (String) -> Unit,
    onDeleteSecret: (String) -> Unit,
    onDeleteVariable: (String) -> Unit
) {
    when (dialogState) {
        null -> Unit
        is RepositoryActionsSettingsDialogState.ConfirmActionsEnabled -> ConfirmDialog(
            title = stringResource(if (dialogState.enabled) R.string.repository_actions_settings_enable_all else R.string.repository_actions_settings_disable_all),
            message = stringResource(if (dialogState.enabled) R.string.repository_actions_settings_enable_confirm else R.string.repository_actions_settings_disable_confirm),
            confirmText = stringResource(R.string.repository_settings_save_action),
            onDismiss = onDismiss,
            onConfirm = { onConfirmActionsEnabled(dialogState.enabled) }
        )
        RepositoryActionsSettingsDialogState.ConfirmWorkflowWrite -> ConfirmDialog(
            title = stringResource(R.string.repository_actions_settings_workflow_write_confirm_title),
            message = stringResource(R.string.repository_actions_settings_workflow_write_confirm),
            confirmText = stringResource(R.string.repository_settings_save_action),
            onDismiss = onDismiss,
            onConfirm = onConfirmWorkflowWrite
        )
        is RepositoryActionsSettingsDialogState.Retention -> RetentionDialog(
            currentDays = dialogState.currentDays,
            onDismiss = onDismiss,
            onSave = onSaveRetention
        )
        is RepositoryActionsSettingsDialogState.CacheAction -> ConfirmDialog(
            title = stringResource(R.string.repository_actions_settings_cache_title),
            message = stringResource(
                R.string.repository_actions_settings_cache_detail_message,
                dialogState.cache.key,
                dialogState.cache.ref,
                dialogState.cache.sizeInBytes.toReadableBytes(),
                dialogState.cache.lastAccessedAt
            ),
            confirmText = stringResource(R.string.repository_actions_settings_delete),
            onDismiss = onDismiss,
            onConfirm = { onDeleteCache(dialogState.cache) }
        )
        RepositoryActionsSettingsDialogState.DeleteCacheByKey -> DeleteCacheByKeyDialog(
            onDismiss = onDismiss,
            onDelete = onDeleteCachesByKeyConfirmed
        )
        is RepositoryActionsSettingsDialogState.SelectedPatterns -> SelectedPatternsDialog(
            githubOwned = dialogState.githubOwned,
            verified = dialogState.verified,
            patterns = dialogState.patterns,
            onDismiss = onDismiss,
            onSave = onSaveSelectedPatterns
        )
        is RepositoryActionsSettingsDialogState.SecretEditor -> SecretEditorDialog(
            name = dialogState.name,
            onDismiss = onDismiss,
            onSave = onSaveSecret
        )
        is RepositoryActionsSettingsDialogState.VariableEditor -> VariableEditorDialog(
            name = dialogState.name,
            value = dialogState.value,
            onDismiss = onDismiss,
            onSave = onSaveVariable
        )
        is RepositoryActionsSettingsDialogState.SecretAction -> ActionChoiceDialog(
            title = stringResource(R.string.repository_actions_settings_secret_action_title, dialogState.name),
            firstActionText = stringResource(R.string.repository_actions_settings_secret_update_value),
            secondActionText = stringResource(R.string.repository_actions_settings_delete),
            onDismiss = onDismiss,
            onFirstAction = { onRequestSecretEdit(dialogState.name) },
            onSecondAction = { onRequestSecretDelete(dialogState.name) }
        )
        is RepositoryActionsSettingsDialogState.VariableAction -> ActionChoiceDialog(
            title = stringResource(R.string.repository_actions_settings_variable_action_title, dialogState.name),
            firstActionText = stringResource(R.string.repository_actions_settings_permission_edit_variable),
            secondActionText = stringResource(R.string.repository_actions_settings_delete),
            onDismiss = onDismiss,
            onFirstAction = { onRequestVariableEdit(dialogState.name, dialogState.value) },
            onSecondAction = { onRequestVariableDelete(dialogState.name) }
        )
        is RepositoryActionsSettingsDialogState.DeleteSecret -> ConfirmDialog(
            title = stringResource(R.string.repository_actions_settings_secret_delete_confirm_title),
            message = stringResource(R.string.repository_actions_settings_secret_delete_confirm_message, dialogState.name),
            confirmText = stringResource(android.R.string.ok),
            onDismiss = onDismiss,
            onConfirm = { onDeleteSecret(dialogState.name) }
        )
        is RepositoryActionsSettingsDialogState.DeleteVariable -> ConfirmDialog(
            title = stringResource(R.string.repository_actions_settings_variable_delete_confirm_title),
            message = stringResource(R.string.repository_actions_settings_variable_delete_confirm_message, dialogState.name),
            confirmText = stringResource(android.R.string.ok),
            onDismiss = onDismiss,
            onConfirm = { onDeleteVariable(dialogState.name) }
        )
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(text = confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(android.R.string.cancel)) }
        }
    )
}

@Composable
private fun RetentionDialog(currentDays: Int?, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var days by remember(currentDays) { mutableStateOf(currentDays?.toString().orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.repository_actions_settings_retention_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = stringResource(R.string.repository_actions_settings_retention_message))
                OutlinedTextField(
                    value = days,
                    onValueChange = { days = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(text = stringResource(R.string.repository_actions_settings_retention_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(days) }) { Text(text = stringResource(R.string.repository_settings_save_action)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(android.R.string.cancel)) }
        }
    )
}

@Composable
private fun DeleteCacheByKeyDialog(onDismiss: () -> Unit, onDelete: (String, String) -> Unit) {
    var key by remember { mutableStateOf("") }
    var ref by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.repository_actions_settings_delete_cache_by_key_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = stringResource(R.string.repository_actions_settings_delete_cache_by_key_message))
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(text = stringResource(R.string.repository_actions_settings_key_hint)) }
                )
                OutlinedTextField(
                    value = ref,
                    onValueChange = { ref = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(text = stringResource(R.string.repository_actions_settings_ref_hint)) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onDelete(key, ref) }) { Text(text = stringResource(R.string.repository_actions_settings_delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(android.R.string.cancel)) }
        }
    )
}

@Composable
private fun SelectedPatternsDialog(
    githubOwned: Boolean,
    verified: Boolean,
    patterns: List<String>,
    onDismiss: () -> Unit,
    onSave: (Boolean, Boolean, String) -> Unit
) {
    var patternsText by remember(patterns) { mutableStateOf(patterns.joinToString("\n")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.repository_actions_settings_edit_patterns_title)) },
        text = {
            OutlinedTextField(
                value = patternsText,
                onValueChange = { patternsText = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                label = { Text(text = stringResource(R.string.repository_actions_settings_pattern_hint)) }
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(githubOwned, verified, patternsText) }) { Text(text = stringResource(R.string.repository_settings_save_action)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(android.R.string.cancel)) }
        }
    )
}

@Composable
private fun SecretEditorDialog(name: String, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var secretName by remember(name) { mutableStateOf(name) }
    var secretValue by remember(name) { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(if (name.isBlank()) R.string.repository_actions_settings_secret_new_title else R.string.repository_actions_settings_secret_update_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = stringResource(R.string.repository_actions_settings_secret_dialog_message))
                OutlinedTextField(
                    value = secretName,
                    onValueChange = { secretName = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(text = stringResource(R.string.repository_actions_settings_secret_name_hint)) }
                )
                OutlinedTextField(
                    value = secretValue,
                    onValueChange = { secretValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    label = { Text(text = stringResource(R.string.repository_actions_settings_secret_value_hint)) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(secretName, secretValue) }) { Text(text = stringResource(R.string.repository_settings_save_action)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(android.R.string.cancel)) }
        }
    )
}

@Composable
private fun VariableEditorDialog(name: String, value: String, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var variableName by remember(name) { mutableStateOf(name) }
    var variableValue by remember(value) { mutableStateOf(value) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(if (name.isBlank()) R.string.repository_actions_settings_variable_new_title else R.string.repository_actions_settings_variable_edit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = variableName,
                    onValueChange = { variableName = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(text = stringResource(R.string.repository_actions_settings_variable_name_hint)) }
                )
                OutlinedTextField(
                    value = variableValue,
                    onValueChange = { variableValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(R.string.repository_actions_settings_variable_value_hint)) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(variableName, variableValue) }) { Text(text = stringResource(R.string.repository_settings_save_action)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(android.R.string.cancel)) }
        }
    )
}

@Composable
private fun ActionChoiceDialog(
    title: String,
    firstActionText: String,
    secondActionText: String,
    onDismiss: () -> Unit,
    onFirstAction: () -> Unit,
    onSecondAction: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onFirstAction) { Text(text = firstActionText) }
                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onSecondAction) { Text(text = secondActionText) }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(android.R.string.cancel)) }
        }
    )
}