package com.Sunset.REN.GitHub.ui.filemanager

import android.app.Dialog
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.filemanager.BookmarkSettings
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerListOptions
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerOperationSettings
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerSearchOptions
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerSortMode
import com.Sunset.REN.GitHub.ui.common.LegacyDialogActions
import com.Sunset.REN.GitHub.ui.common.LegacyDialogSurface
import com.Sunset.REN.GitHub.ui.common.LegacyDialogTitle
import com.Sunset.REN.GitHub.ui.common.showComposeDialog
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens

object LocalFileManagerSettingsComposeDialogs {
    fun showListDisplaySettings(
        context: Context,
        settings: FileManagerListOptions,
        onSave: (FileManagerListOptions) -> Unit
    ) {
        showComposeDialog(context) { dismiss ->
            var useShortYear by remember { mutableStateOf(settings.useShortYear) }
            var showSeconds by remember { mutableStateOf(settings.showSeconds) }
            var showPermissions by remember { mutableStateOf(settings.showPermissions) }
            BooleanSettingsContent(
                title = context.getString(R.string.local_file_manager_list_display_settings_title),
                rows = listOf(
                    BooleanSettingRowState(
                        title = context.getString(R.string.local_file_manager_list_display_short_year_title),
                        summary = context.getString(R.string.local_file_manager_list_display_short_year_summary),
                        checked = useShortYear,
                        onCheckedChange = { useShortYear = it }
                    ),
                    BooleanSettingRowState(
                        title = context.getString(R.string.local_file_manager_list_display_seconds_title),
                        summary = context.getString(R.string.local_file_manager_list_display_seconds_summary),
                        checked = showSeconds,
                        onCheckedChange = { showSeconds = it }
                    ),
                    BooleanSettingRowState(
                        title = context.getString(R.string.local_file_manager_list_display_permissions_title),
                        summary = context.getString(R.string.local_file_manager_list_display_permissions_summary),
                        checked = showPermissions,
                        onCheckedChange = { showPermissions = it }
                    )
                ),
                cancelText = context.getString(android.R.string.cancel),
                okText = context.getString(android.R.string.ok),
                onCancel = dismiss,
                onOk = {
                    dismiss()
                    onSave(
                        settings.copy(
                            useShortYear = useShortYear,
                            showSeconds = showSeconds,
                            showPermissions = showPermissions
                        )
                    )
                }
            )
        }.setCanceledOnTouchOutside(true)
    }

    fun showOperationSettings(
        context: Context,
        settings: FileManagerOperationSettings,
        onSave: (FileManagerOperationSettings) -> Unit
    ) {
        showComposeDialog(context) { dismiss ->
            var backupBeforeTextSave by remember { mutableStateOf(settings.backupBeforeTextSave) }
            var preserveModifiedTimeOnCopy by remember { mutableStateOf(settings.preserveModifiedTimeOnCopy) }
            var preserveModifiedTimeOnExtract by remember { mutableStateOf(settings.preserveModifiedTimeOnExtract) }
            BooleanSettingsContent(
                title = context.getString(R.string.local_file_manager_operation_settings_title),
                rows = listOf(
                    BooleanSettingRowState(
                        title = context.getString(R.string.local_file_manager_operation_backup_before_save_title),
                        summary = context.getString(R.string.local_file_manager_operation_backup_before_save_summary),
                        checked = backupBeforeTextSave,
                        onCheckedChange = { backupBeforeTextSave = it }
                    ),
                    BooleanSettingRowState(
                        title = context.getString(R.string.local_file_manager_operation_preserve_copy_time_title),
                        summary = context.getString(R.string.local_file_manager_operation_preserve_copy_time_summary),
                        checked = preserveModifiedTimeOnCopy,
                        onCheckedChange = { preserveModifiedTimeOnCopy = it }
                    ),
                    BooleanSettingRowState(
                        title = context.getString(R.string.local_file_manager_operation_preserve_extract_time_title),
                        summary = context.getString(R.string.local_file_manager_operation_preserve_extract_time_summary),
                        checked = preserveModifiedTimeOnExtract,
                        onCheckedChange = { preserveModifiedTimeOnExtract = it }
                    )
                ),
                cancelText = context.getString(android.R.string.cancel),
                okText = context.getString(android.R.string.ok),
                onCancel = dismiss,
                onOk = {
                    dismiss()
                    onSave(
                        FileManagerOperationSettings(
                            backupBeforeTextSave = backupBeforeTextSave,
                            preserveModifiedTimeOnCopy = preserveModifiedTimeOnCopy,
                            preserveModifiedTimeOnExtract = preserveModifiedTimeOnExtract
                        )
                    )
                }
            )
        }.setCanceledOnTouchOutside(true)
    }

    fun showBookmarkSettings(
        context: Context,
        settings: BookmarkSettings,
        onSave: (BookmarkSettings) -> Unit
    ) {
        showComposeDialog(context) { dismiss ->
            var showInSidebar by remember { mutableStateOf(settings.showInSidebar) }
            var addToTop by remember { mutableStateOf(settings.addToTop) }
            var swipePositionAware by remember { mutableStateOf(settings.swipePositionAware) }
            BooleanSettingsContent(
                title = context.getString(R.string.local_file_manager_bookmark_settings_title),
                rows = listOf(
                    BooleanSettingRowState(
                        title = context.getString(R.string.local_file_manager_bookmark_show_in_sidebar_title),
                        summary = context.getString(R.string.local_file_manager_bookmark_show_in_sidebar_summary),
                        checked = showInSidebar,
                        onCheckedChange = { showInSidebar = it }
                    ),
                    BooleanSettingRowState(
                        title = context.getString(R.string.local_file_manager_bookmark_add_to_top_title),
                        summary = context.getString(R.string.local_file_manager_bookmark_add_to_top_summary),
                        checked = addToTop,
                        onCheckedChange = { addToTop = it }
                    ),
                    BooleanSettingRowState(
                        title = context.getString(R.string.local_file_manager_bookmark_swipe_position_title),
                        summary = context.getString(R.string.local_file_manager_bookmark_swipe_position_summary),
                        checked = swipePositionAware,
                        onCheckedChange = { swipePositionAware = it }
                    )
                ),
                cancelText = context.getString(android.R.string.cancel),
                okText = context.getString(android.R.string.ok),
                onCancel = dismiss,
                onOk = {
                    dismiss()
                    onSave(
                        settings.copy(
                            showInSidebar = showInSidebar,
                            addToTop = addToTop,
                            swipePositionAware = swipePositionAware
                        )
                    )
                }
            )
        }.setCanceledOnTouchOutside(true)
    }

    fun showPathJumpDialog(
        context: Context,
        initialPath: String,
        normalize: (String) -> String,
        onSubmit: (String) -> Unit
    ): PathJumpDialogHandle {
        val errorState = mutableStateOf<String?>(null)
        lateinit var dialog: Dialog
        dialog = showComposeDialog(context) { dismiss ->
            var path by remember { mutableStateOf(initialPath) }
            fun submit() {
                val normalized = normalize(path)
                if (normalized.isBlank()) {
                    errorState.value = context.getString(R.string.local_file_manager_search_query_required)
                    return
                }
                onSubmit(normalized)
            }
            PathJumpContent(
                title = context.getString(R.string.local_file_manager_jump_title),
                path = path,
                error = errorState.value,
                onPathChange = {
                    path = it
                    errorState.value = null
                },
                onCancel = dismiss,
                onSubmit = ::submit
            )
        }
        dialog.setCanceledOnTouchOutside(true)
        return PathJumpDialogHandle(
            dialog = dialog,
            setError = { errorState.value = it },
            dismiss = { if (dialog.isShowing) dialog.dismiss() }
        )
    }

    fun showHiddenFilesOptions(
        context: Context,
        options: FileManagerListOptions,
        onlyHidden: Boolean,
        hasSelection: Boolean,
        onSave: (showSystemHidden: Boolean, showManualHidden: Boolean, onlyHidden: Boolean) -> Unit,
        onHideSelected: () -> Unit,
        onEditManual: () -> Unit
    ) {
        showComposeDialog(context) { dismiss ->
            var showSystemHidden by remember { mutableStateOf(options.showHiddenFiles) }
            var showManualHidden by remember { mutableStateOf(options.showManualHiddenFiles) }
            var onlyHiddenFiles by remember { mutableStateOf(onlyHidden && (options.showHiddenFiles || options.showManualHiddenFiles)) }
            val onlyHiddenEnabled = showSystemHidden || showManualHidden
            if (!onlyHiddenEnabled && onlyHiddenFiles) onlyHiddenFiles = false
            LegacyDialogSurface {
                LegacyDialogTitle(context.getString(R.string.local_file_manager_menu_hidden_files))
                Spacer(modifier = Modifier.height(12.dp))
                BooleanSettingRow(
                    BooleanSettingRowState(
                        title = context.getString(R.string.local_file_manager_hidden_show_system),
                        summary = "",
                        checked = showSystemHidden,
                        onCheckedChange = { showSystemHidden = it }
                    )
                )
                BooleanSettingRow(
                    BooleanSettingRowState(
                        title = context.getString(R.string.local_file_manager_hidden_show_manual),
                        summary = "",
                        checked = showManualHidden,
                        onCheckedChange = { showManualHidden = it }
                    )
                )
                BooleanSettingRow(
                    BooleanSettingRowState(
                        title = context.getString(R.string.local_file_manager_hidden_only_hidden),
                        summary = "",
                        checked = onlyHiddenFiles,
                        enabled = onlyHiddenEnabled,
                        onCheckedChange = { if (onlyHiddenEnabled) onlyHiddenFiles = it }
                    )
                )
                DialogActionRow(
                    text = context.getString(R.string.local_file_manager_hidden_hide_selected),
                    enabled = hasSelection,
                    onClick = {
                        dismiss()
                        onHideSelected()
                    }
                )
                DialogActionRow(
                    text = context.getString(R.string.local_file_manager_hidden_edit_manual),
                    onClick = {
                        dismiss()
                        onEditManual()
                    }
                )
                LegacyDialogActions(
                    negativeText = context.getString(android.R.string.cancel),
                    positiveText = context.getString(android.R.string.ok),
                    onNegative = dismiss,
                    onPositive = {
                        dismiss()
                        onSave(showSystemHidden, showManualHidden, onlyHiddenFiles)
                    }
                )
            }
        }.setCanceledOnTouchOutside(true)
    }

    fun showSearchDialog(
        context: Context,
        options: FileManagerSearchOptions,
        includeHiddenFiles: Boolean,
        onSubmit: (FileManagerSearchOptions) -> Unit
    ) {
        showComposeDialog(context) { dismiss ->
            var query by remember { mutableStateOf(options.query) }
            var includeSubdirectories by remember { mutableStateOf(options.includeSubdirectories) }
            var includeFiles by remember { mutableStateOf(options.includeFiles) }
            var includeDirectories by remember { mutableStateOf(options.includeDirectories) }
            var caseSensitive by remember { mutableStateOf(options.caseSensitive) }
            fun submit() {
                dismiss()
                onSubmit(
                    options.copy(
                        query = query,
                        includeSubdirectories = includeSubdirectories,
                        includeFiles = includeFiles,
                        includeDirectories = includeDirectories,
                        caseSensitive = caseSensitive,
                        includeHiddenFiles = includeHiddenFiles
                    )
                )
            }
            SearchContent(
                title = context.getString(R.string.local_file_manager_search_hint),
                query = query,
                onQueryChange = { query = it },
                rows = listOf(
                    BooleanSettingRowState("包含子文件夹", "", includeSubdirectories, { includeSubdirectories = it }),
                    BooleanSettingRowState("包含文件", "", includeFiles, { includeFiles = it }),
                    BooleanSettingRowState("包含文件夹", "", includeDirectories, { includeDirectories = it }),
                    BooleanSettingRowState("区分大小写", "", caseSensitive, { caseSensitive = it })
                ),
                positiveText = context.getString(if (options.includeSubdirectories) R.string.local_file_manager_search_recursive else android.R.string.search_go),
                onCancel = dismiss,
                onSubmit = ::submit
            )
        }.setCanceledOnTouchOutside(true)
    }

    fun showFilterDialog(
        context: Context,
        options: FileManagerSearchOptions,
        initialQuery: String,
        extensionQuery: String,
        useWildcard: Boolean,
        useRegex: Boolean,
        includeHiddenFiles: Boolean,
        onSubmit: (options: FileManagerSearchOptions, extensionQuery: String, useWildcard: Boolean, useRegex: Boolean) -> Boolean,
        onClear: () -> Unit
    ) {
        showComposeDialog(context) { dismiss ->
            var query by remember { mutableStateOf(initialQuery) }
            var extension by remember { mutableStateOf(extensionQuery) }
            var includeFiles by remember { mutableStateOf(options.includeFiles) }
            var includeDirectories by remember { mutableStateOf(options.includeDirectories) }
            var caseSensitive by remember { mutableStateOf(options.caseSensitive) }
            var wildcard by remember { mutableStateOf(useWildcard) }
            var regex by remember { mutableStateOf(useRegex) }
            fun submit() {
                val handled = onSubmit(
                    options.copy(
                        query = query,
                        includeSubdirectories = false,
                        includeFiles = includeFiles,
                        includeDirectories = includeDirectories,
                        caseSensitive = caseSensitive,
                        includeHiddenFiles = includeHiddenFiles
                    ),
                    extension.trim(),
                    wildcard && !regex,
                    regex
                )
                if (handled) dismiss()
            }
            FilterContent(
                query = query,
                extension = extension,
                onQueryChange = { query = it },
                onExtensionChange = { extension = it },
                rows = listOf(
                    BooleanSettingRowState("包含文件", "", includeFiles, { includeFiles = it }),
                    BooleanSettingRowState("包含文件夹", "", includeDirectories, { includeDirectories = it }),
                    BooleanSettingRowState("区分大小写", "", caseSensitive, { caseSensitive = it }),
                    BooleanSettingRowState("使用通配符（* / ?）", "", wildcard && !regex, { wildcard = it; if (it) regex = false }),
                    BooleanSettingRowState("使用正则表达式", "", regex, { regex = it; if (it) wildcard = false })
                ),
                onCancel = dismiss,
                onClear = {
                    dismiss()
                    onClear()
                },
                onSubmit = ::submit
            )
        }.setCanceledOnTouchOutside(true)
    }

    fun showSortDialog(
        context: Context,
        title: String,
        selectedOptions: FileManagerListOptions,
        directoryOnly: Boolean,
        onManage: () -> Unit,
        onSave: (FileManagerListOptions, Boolean) -> Unit
    ) {
        showComposeDialog(context) { dismiss ->
            var sortMode by remember { mutableStateOf(selectedOptions.sortMode) }
            var reverse by remember { mutableStateOf(selectedOptions.reverse) }
            var applyDirectoryOnly by remember { mutableStateOf(directoryOnly) }
            val sortModes = listOf(
                FileManagerSortMode.Name to "按名称",
                FileManagerSortMode.Time to "按日期",
                FileManagerSortMode.Size to "按大小",
                FileManagerSortMode.Type to "按类型"
            )
            LegacyDialogSurface {
                LegacyDialogTitle(title)
                Spacer(modifier = Modifier.height(12.dp))
                sortModes.forEach { (mode, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { sortMode = mode }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = sortMode == mode, onClick = { sortMode = mode })
                        Text(
                            text = label,
                            modifier = Modifier.padding(start = 8.dp),
                            color = SunsetGitHubThemeTokens.colors.textPrimary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                BooleanSettingRow(BooleanSettingRowState("逆向排序", "", reverse, { reverse = it }))
                BooleanSettingRow(BooleanSettingRowState("仅应用于此文件夹", "", applyDirectoryOnly, { applyDirectoryOnly = it }))
                LegacyDialogActions(
                    negativeText = context.getString(android.R.string.cancel),
                    neutralText = "管理",
                    positiveText = context.getString(android.R.string.ok),
                    onNegative = dismiss,
                    onNeutral = {
                        dismiss()
                        onManage()
                    },
                    onPositive = {
                        dismiss()
                        onSave(selectedOptions.copy(sortMode = sortMode, reverse = reverse), applyDirectoryOnly)
                    }
                )
            }
        }.setCanceledOnTouchOutside(true)
    }
}

data class PathJumpDialogHandle(
    val dialog: Dialog,
    val setError: (String) -> Unit,
    val dismiss: () -> Unit
)

private data class BooleanSettingRowState(
    val title: String,
    val summary: String,
    val checked: Boolean,
    val onCheckedChange: (Boolean) -> Unit,
    val enabled: Boolean = true
)

@Composable
private fun PathJumpContent(
    title: String,
    path: String,
    error: String?,
    onPathChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSubmit: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    LegacyDialogSurface {
        LegacyDialogTitle(title)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = path,
            onValueChange = onPathChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            isError = error != null,
            supportingText = { error?.let { Text(it) } },
            minLines = 1,
            maxLines = 5,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { onSubmit() })
        )
        LegacyDialogActions(
            negativeText = "取消",
            positiveText = "确定",
            onNegative = onCancel,
            onPositive = onSubmit
        )
    }
}

@Composable
private fun SearchContent(
    title: String,
    query: String,
    onQueryChange: (String) -> Unit,
    rows: List<BooleanSettingRowState>,
    positiveText: String,
    onCancel: () -> Unit,
    onSubmit: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    LegacyDialogSurface {
        LegacyDialogTitle(title)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() })
        )
        Spacer(modifier = Modifier.height(8.dp))
        rows.forEach { BooleanSettingRow(it) }
        LegacyDialogActions(
            negativeText = "取消",
            positiveText = positiveText,
            onNegative = onCancel,
            onPositive = onSubmit
        )
    }
}

@Composable
private fun FilterContent(
    query: String,
    extension: String,
    onQueryChange: (String) -> Unit,
    onExtensionChange: (String) -> Unit,
    rows: List<BooleanSettingRowState>,
    onCancel: () -> Unit,
    onClear: () -> Unit,
    onSubmit: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    LegacyDialogSurface {
        LegacyDialogTitle("过滤")
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            label = { Text("输入名称过滤当前列表") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() })
        )
        OutlinedTextField(
            value = extension,
            onValueChange = onExtensionChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            label = { Text("扩展名，例如 kt / .xml / jpg,png") },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        rows.forEach { BooleanSettingRow(it) }
        LegacyDialogActions(
            negativeText = "取消",
            neutralText = "清除过滤",
            positiveText = "过滤",
            onNegative = onCancel,
            onNeutral = onClear,
            onPositive = onSubmit
        )
    }
}

@Composable
private fun DialogActionRow(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp),
        color = if (enabled) colors.textPrimary else colors.textSecondary.copy(alpha = 0.45f),
        style = MaterialTheme.typography.bodyLarge
    )
}

@Composable
private fun BooleanSettingsContent(
    title: String,
    rows: List<BooleanSettingRowState>,
    cancelText: String,
    okText: String,
    onCancel: () -> Unit,
    onOk: () -> Unit
) {
    LegacyDialogSurface {
        LegacyDialogTitle(title)
        Spacer(modifier = Modifier.height(12.dp))
        Column {
            rows.forEach { row ->
                BooleanSettingRow(row)
            }
        }
        LegacyDialogActions(
            negativeText = cancelText,
            positiveText = okText,
            onNegative = onCancel,
            onPositive = onOk
        )
    }
}

@Composable
private fun BooleanSettingRow(row: BooleanSettingRowState) {
    val colors = SunsetGitHubThemeTokens.colors
    val contentAlpha = if (row.enabled) 1f else 0.45f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = row.enabled) { row.onCheckedChange(!row.checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.title,
                color = colors.textPrimary.copy(alpha = contentAlpha),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            if (row.summary.isNotBlank()) {
                Text(
                    text = row.summary,
                    modifier = Modifier.padding(top = 2.dp),
                    color = colors.textSecondary.copy(alpha = contentAlpha),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Checkbox(
            checked = row.checked,
            enabled = row.enabled,
            onCheckedChange = row.onCheckedChange,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}
