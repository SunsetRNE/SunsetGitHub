package com.Sunset.REN.GitHub.ui.compose.screens.repo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens
import com.Sunset.REN.GitHub.ui.compose.components.SunsetCard
import com.Sunset.REN.GitHub.ui.compose.components.SunsetPrimaryButton
import com.Sunset.REN.GitHub.ui.compose.components.SunsetSecondaryButton
import com.Sunset.REN.GitHub.ui.compose.components.TextEditorHostView
import com.Sunset.REN.GitHub.ui.editor.TextEditorHost
import com.Sunset.REN.GitHub.ui.repo.RepositoryFileEditUiState

@Composable
fun RepositoryFileEditScreen(
    state: RepositoryFileEditUiState,
    owner: String,
    repo: String,
    previewMode: Boolean,
    targetPathDraft: String,
    commitMessageDraft: String,
    editorHost: TextEditorHost,
    onTargetPathChange: (String) -> Unit,
    onCommitMessageChange: (String) -> Unit,
    onEnterEditMode: () -> Unit,
    onSubmit: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onFocusEditor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas)
            .padding(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        RepositoryFileEditHeader(
            state = state,
            owner = owner,
            repo = repo,
            previewMode = previewMode,
            onEnterEditMode = onEnterEditMode,
            onCopy = onCopy,
            onFocusEditor = onFocusEditor
        )
        if (previewMode) {
            PreviewHintCard(state = state, onCopy = onCopy, onEnterEditMode = onEnterEditMode)
        }
        if (state.isCreateMode) {
            OutlinedTextField(
                value = targetPathDraft,
                onValueChange = onTargetPathChange,
                enabled = !previewMode && !state.isSubmitting && !state.isDeleting,
                label = { Text("目标路径") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        state.errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
            SunsetCard(Modifier.fillMaxWidth()) {
                Text(message, color = colors.danger, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (state.isLoading) {
            SunsetCard(Modifier.fillMaxWidth()) {
                Text("正在加载文件内容…", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
            }
        }
        SunsetCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            TextEditorHostView(
                host = editorHost,
                modifier = Modifier.fillMaxSize()
            )
        }
        if (!previewMode) {
            OutlinedTextField(
                value = commitMessageDraft,
                onValueChange = onCommitMessageChange,
                enabled = !state.isSubmitting && !state.isDeleting,
                label = { Text("提交信息") },
                minLines = 1,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (!previewMode) {
            RepositoryFileEditActions(
                state = state,
                previewMode = previewMode,
                onSubmit = onSubmit,
                onDelete = onDelete,
                onCopy = onCopy,
                onFocusEditor = onFocusEditor
            )
        }
    }
}

@Composable
private fun RepositoryFileEditHeader(
    state: RepositoryFileEditUiState,
    owner: String,
    repo: String,
    previewMode: Boolean,
    onEnterEditMode: () -> Unit,
    onCopy: () -> Unit,
    onFocusEditor: () -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    SunsetCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (state.isCreateMode) "新建文件" else if (previewMode) "文件预览" else "编辑文件",
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    if (state.hasUnsavedChanges) "未保存" else "已同步",
                    color = if (state.hasUnsavedChanges) colors.attention else colors.success,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Text("$owner/$repo", color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
            Text(
                state.filePath.ifBlank { state.fileName.ifBlank { "未命名文件" } },
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                buildMetadata(state),
                color = colors.textSecondary,
                style = MaterialTheme.typography.labelSmall
            )
            if (!previewMode) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SunsetSecondaryButton("复制内容", onClick = onCopy, enabled = state.content.isNotEmpty())
                    SunsetSecondaryButton("聚焦编辑器", onClick = onFocusEditor)
                }
            }
        }
    }
}

@Composable
private fun PreviewHintCard(
    state: RepositoryFileEditUiState,
    onCopy: () -> Unit,
    onEnterEditMode: () -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    SunsetCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "只读预览",
                color = colors.textPrimary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "内容已在下方按源码方式加载。需要修改时再进入编辑模式。",
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodySmall
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SunsetSecondaryButton("复制", onClick = onCopy, enabled = state.content.isNotEmpty())
                if (state.originalSha.isNotBlank()) {
                    SunsetPrimaryButton("进入编辑", onClick = onEnterEditMode)
                }
            }
        }
    }
}

@Composable
private fun RepositoryFileEditActions(
    state: RepositoryFileEditUiState,
    previewMode: Boolean,
    onSubmit: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onFocusEditor: () -> Unit
) {
    SunsetCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SunsetSecondaryButton("复制", onClick = onCopy, enabled = state.content.isNotEmpty())
                SunsetSecondaryButton("聚焦", onClick = onFocusEditor)
                if (!previewMode && !state.isCreateMode && state.originalSha.isNotBlank()) {
                    SunsetSecondaryButton("删除文件", onClick = onDelete, enabled = !state.isSubmitting && !state.isDeleting)
                }
                if (!previewMode) {
                    SunsetPrimaryButton(
                        if (state.isSubmitting) "提交中…" else "提交",
                        onClick = onSubmit,
                        enabled = state.canSubmit && !state.isSubmitting && !state.isDeleting
                    )
                }
            }
            Text(
                if (previewMode) "预览模式下内容只读；可进入编辑模式修改。" else "提交前请确认路径、内容和提交信息。",
                color = SunsetGitHubThemeTokens.colors.textSecondary,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

private fun buildMetadata(state: RepositoryFileEditUiState): String {
    val lines = if (state.content.isBlank()) 0 else state.content.count { it == '\n' } + 1
    return "${state.content.length} 字符 · $lines 行" + if (state.isLoading) " · 加载中" else ""
}
