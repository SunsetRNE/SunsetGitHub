package com.Sunset.REN.GitHub.ui.pages

import androidx.compose.runtime.Composable
import com.Sunset.REN.GitHub.ui.layout.PageSchema
import com.Sunset.REN.GitHub.ui.layout.RowSchema
import com.Sunset.REN.GitHub.ui.layout.cell
import com.Sunset.REN.GitHub.ui.layout.row
import com.Sunset.REN.GitHub.ui.render.renderPage
import com.Sunset.REN.GitHub.ui.repo.RepositoryFileUploadUiState
import com.Sunset.REN.GitHub.ui.schema.ButtonComponent
import com.Sunset.REN.GitHub.ui.schema.ButtonKind
import com.Sunset.REN.GitHub.ui.schema.FieldComponent
import com.Sunset.REN.GitHub.ui.schema.SectionHeaderComponent
import com.Sunset.REN.GitHub.ui.schema.SpacerComponent
import com.Sunset.REN.GitHub.ui.schema.TextColor
import com.Sunset.REN.GitHub.ui.schema.TextComponent
import com.Sunset.REN.GitHub.ui.schema.TextStyle
import com.Sunset.REN.GitHub.ui.shell.AppShell
import com.Sunset.REN.GitHub.ui.shell.NavBarMode
import com.Sunset.REN.GitHub.ui.shell.ShellState
import java.util.Locale

/**
 * 仓库文件上传页垂直切片（组 A：仓库写入/文件流）。
 *
 * 渲染结构对齐 RepositoryFileUploadScreen（双卡）：
 * - 状态卡：repositoryContext（SectionHeader 标题）+ 来源（displayName·大小）+
 *   状态文本五态纯函数（submitting/failed/missing_target/ready/preparing）；
 * - 表单卡：目标路径输入（Field + 目录选择按钮）→ 提交信息输入 → 提交按钮
 *   （提交中三态 + canSubmit/submitSuccess 驱动 enabled）。
 * 壳：Hidden + showBack（独立全屏页）。
 * 路由前缀：file_upload.submit / file_upload.pick_target_path / shell.back。
 * 目标路径选择 Dialog（TargetPathPicker/Conflict）由调用端承载。
 */
object RepositoryFileUploadPage {

    /** 来源文本（原版 buildSourceText：displayName · 大小）。 */
    private fun sourceText(state: RepositoryFileUploadUiState): String {
        val sourceName = state.displayName.ifBlank { state.sourceUri }
        return if (sourceName.isBlank()) {
            "尚未选择文件"
        } else {
            val size = state.sourceSizeBytes?.let { formatSize(it) }
            if (size != null) "$sourceName · $size" else sourceName
        }
    }

    /** 状态文本五态纯函数（原版 buildStateText）。 */
    private fun stateText(state: RepositoryFileUploadUiState): String = when {
        state.isSubmitting -> "正在上传…"
        !state.errorMessage.isNullOrBlank() -> "上传失败：${state.errorMessage}"
        state.targetPath.trim().isBlank() -> "请填写目标路径。"
        state.sourceUri.isNotBlank() -> "文件已准备好。"
        else -> "正在准备上传…"
    }

    /** 大小格式化（B/K/M/G）。 */
    private fun formatSize(sizeBytes: Long): String {
        if (sizeBytes < 1024L) return "${sizeBytes}B"
        val kb = sizeBytes / 1024.0
        if (kb < 1024.0) return String.format(Locale.US, "%.2fK", kb)
        val mb = kb / 1024.0
        if (mb < 1024.0) return String.format(Locale.US, "%.2fM", mb)
        return String.format(Locale.US, "%.2fG", mb / 1024.0)
    }

    fun schemaFor(
        state: RepositoryFileUploadUiState,
        repositoryContext: String,
        targetPath: String,
        commitMessage: String,
        onTargetPathChange: (String) -> Unit = {},
        onCommitMessageChange: (String) -> Unit = {},
    ): PageSchema {
        val rows = buildList<RowSchema> {
            // —— 状态卡 ——
            add(
                row(
                    cell(
                        SectionHeaderComponent(
                            id = "file_upload.status_header",
                            title = repositoryContext.ifBlank { "上传文件" },
                            subtitle = "来源：${sourceText(state)}",
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        TextComponent(
                            id = "file_upload.status",
                            text = stateText(state),
                            style = TextStyle.Body,
                            color = if (!state.errorMessage.isNullOrBlank()) TextColor.Danger else TextColor.Primary,
                        ),
                    ),
                ),
            )
            add(row(cell(SpacerComponent(id = "file_upload.spacer.form", heightDp = 8))))
            // —— 表单卡 ——
            add(
                row(
                    cell(
                        TextComponent(
                            id = "file_upload.form_title",
                            text = "提交文件",
                            style = TextStyle.Subtitle,
                            color = TextColor.Primary,
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        TextComponent(
                            id = "file_upload.target_label",
                            text = "目标路径",
                            style = TextStyle.Meta,
                            color = TextColor.Secondary,
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        FieldComponent(
                            id = "file_upload.target_path",
                            value = targetPath,
                            hint = "目标路径，例如 docs/readme.md",
                            singleLine = true,
                            enabled = !state.isSubmitting,
                            onChange = onTargetPathChange,
                        ),
                        span = 9,
                    ),
                    cell(
                        ButtonComponent(
                            id = "file_upload.pick_target_path",
                            text = "…",
                            kind = ButtonKind.Secondary,
                            enabled = !state.isSubmitting,
                            action = "file_upload.pick_target_path",
                        ),
                        span = 3,
                    ),
                ),
            )
            add(
                row(
                    cell(
                        FieldComponent(
                            id = "file_upload.commit_message",
                            value = commitMessage,
                            hint = "提交说明",
                            singleLine = true,
                            enabled = !state.isSubmitting,
                            onChange = onCommitMessageChange,
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        ButtonComponent(
                            id = "file_upload.submit",
                            text = if (state.isSubmitting) "正在上传…" else "上传文件",
                            kind = ButtonKind.Primary,
                            enabled = state.canSubmit && !state.submitSuccess,
                            action = "file_upload.submit",
                        ),
                    ),
                ),
            )
        }
        return PageSchema(id = "file_upload", columns = 12, scrollable = true, rows = rows)
    }

    /** 文件上传页壳状态：Hidden + 返回（独立全屏页）。 */
    fun shellState(): ShellState = ShellState(
        title = "上传文件",
        showBack = true,
        backAction = "shell.back",
        navBarMode = NavBarMode.Hidden,
        contentKey = "file_upload",
    )
}

/**
 * 仓库文件上传页入口：壳 + 状态驱动 schema。
 * 目录选择 Dialog（TargetPathPicker/Conflict）与提交由调用端承载。
 */
@Composable
fun RepositoryFileUploadPageContent(
    state: RepositoryFileUploadUiState,
    repositoryContext: String,
    targetPath: String,
    commitMessage: String,
    onTargetPathChange: (String) -> Unit = {},
    onCommitMessageChange: (String) -> Unit = {},
    onShowTargetPathPicker: () -> Unit = {},
    onSubmit: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    val handleAction: (String) -> Unit = { action ->
        when (action) {
            "shell.back" -> onBack()
            "file_upload.submit" -> onSubmit()
            "file_upload.pick_target_path" -> onShowTargetPathPicker()
        }
    }
    AppShell(state = RepositoryFileUploadPage.shellState(), onAction = handleAction) {
        RepositoryFileUploadPage.schemaFor(
            state, repositoryContext, targetPath, commitMessage,
            onTargetPathChange, onCommitMessageChange,
        ).renderPage(handleAction)
    }
}