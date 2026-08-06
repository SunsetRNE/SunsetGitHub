package com.Sunset.REN.GitHub.ui.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.Sunset.REN.GitHub.ui.compose.components.TextEditorHostView
import com.Sunset.REN.GitHub.ui.editor.TextEditorHost
import com.Sunset.REN.GitHub.ui.layout.PageSchema
import com.Sunset.REN.GitHub.ui.layout.RowSchema
import com.Sunset.REN.GitHub.ui.layout.cell
import com.Sunset.REN.GitHub.ui.layout.row
import com.Sunset.REN.GitHub.ui.render.renderPage
import com.Sunset.REN.GitHub.ui.repo.RepositoryFileEditUiState
import com.Sunset.REN.GitHub.ui.schema.ButtonComponent
import com.Sunset.REN.GitHub.ui.schema.ButtonKind
import com.Sunset.REN.GitHub.ui.schema.FieldComponent
import com.Sunset.REN.GitHub.ui.schema.SpacerComponent
import com.Sunset.REN.GitHub.ui.schema.TextColor
import com.Sunset.REN.GitHub.ui.schema.TextComponent
import com.Sunset.REN.GitHub.ui.schema.TextStyle
import com.Sunset.REN.GitHub.ui.shell.AppShell
import com.Sunset.REN.GitHub.ui.shell.NavBarMode
import com.Sunset.REN.GitHub.ui.shell.ShellState

/**
 * 仓库文件编辑页垂直切片（组 A：仓库写入/文件流）。
 *
 * 渲染结构对齐 RepositoryFileEditScreen，采用**混合布局**：
 * - 顶部 schema 段：标题（新建/预览/编辑三态）+ 未保存/已同步徽章 + owner/repo +
 *   路径 + 字符·行数元信息 + 复制/聚焦按钮 + 预览提示卡 + 目标路径（新建模式）+
 *   error/loading 消息；
 * - 中部：原生 Sora 编辑器宿主（TextEditorHostView，weight 1f 弹性填充，
 *   编辑器自带内部滚动）；
 * - 底部 schema 段：提交信息输入 + 操作行（复制/聚焦/删除/提交）+ 提示文字。
 * 壳：Hidden + showBack（独立全屏页）。
 * 路由前缀：file_edit.submit / delete / copy / focus_editor / enter_edit / shell.back。
 * 提交/删除/放弃/冲突四类 Dialog、未保存返回确认（OnBackPressedCallback）、
 * 编辑器引擎解析（Sora/SoraFallback）与语言模式映射均由调用端承载。
 */
object RepositoryFileEditPage {

    /** 文件路径显示（空态兜底）。 */
    private fun pathText(state: RepositoryFileEditUiState): String =
        state.filePath.ifBlank { state.fileName.ifBlank { "未命名文件" } }

    /** 字符·行数元信息（原版 buildMetadata）。 */
    private fun metadataText(state: RepositoryFileEditUiState): String {
        val lines = if (state.content.isBlank()) 0 else state.content.count { it == '\n' } + 1
        return "${state.content.length} 字符 · $lines 行" + if (state.isLoading) " · 加载中" else ""
    }

    /** 标题三态（新建/预览/编辑）。 */
    private fun titleText(state: RepositoryFileEditUiState, previewMode: Boolean): String = when {
        state.isCreateMode -> "新建文件"
        previewMode -> "文件预览"
        else -> "编辑文件"
    }

    /** 顶部 schema：标题/元信息/路径/预览提示/目标路径/状态消息。 */
    fun topSchema(
        state: RepositoryFileEditUiState,
        owner: String,
        repo: String,
        previewMode: Boolean,
        targetPathDraft: String,
        onTargetPathChange: (String) -> Unit = {},
    ): PageSchema {
        val rows = buildList<RowSchema> {
            // —— 标题 + 未保存/已同步徽章 ——
            add(
                row(
                    cell(
                        TextComponent(
                            id = "file_edit.title",
                            text = titleText(state, previewMode),
                            style = TextStyle.Section,
                            color = TextColor.Primary,
                        ),
                        span = 9,
                    ),
                    cell(
                        TextComponent(
                            id = "file_edit.sync_badge",
                            text = if (state.hasUnsavedChanges) "未保存" else "已同步",
                            style = TextStyle.Caption,
                            color = if (state.hasUnsavedChanges) TextColor.Accent else TextColor.Success,
                        ),
                        span = 3,
                    ),
                ),
            )
            add(
                row(
                    cell(
                        TextComponent(
                            id = "file_edit.repo",
                            text = "$owner/$repo",
                            style = TextStyle.Meta,
                            color = TextColor.Secondary,
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        TextComponent(
                            id = "file_edit.path",
                            text = pathText(state),
                            style = TextStyle.Meta,
                            color = TextColor.Secondary,
                            maxLines = 2,
                            ellipsis = true,
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        TextComponent(
                            id = "file_edit.metadata",
                            text = metadataText(state),
                            style = TextStyle.Caption,
                            color = TextColor.Secondary,
                        ),
                    ),
                ),
            )
            if (!previewMode) {
                // —— 复制/聚焦按钮行 ——
                add(
                    row(
                        cell(
                            ButtonComponent(
                                id = "file_edit.copy",
                                text = "复制内容",
                                kind = ButtonKind.Secondary,
                                enabled = state.content.isNotEmpty(),
                                action = "file_edit.copy",
                            ),
                            span = 6,
                        ),
                        cell(
                            ButtonComponent(
                                id = "file_edit.focus_editor",
                                text = "聚焦编辑器",
                                kind = ButtonKind.Secondary,
                                action = "file_edit.focus_editor",
                            ),
                            span = 6,
                        ),
                    ),
                )
            } else {
                // —— 预览提示卡 ——
                add(
                    row(
                        cell(
                            TextComponent(
                                id = "file_edit.preview_title",
                                text = "只读预览",
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
                                id = "file_edit.preview_desc",
                                text = "内容已在下方按源码方式加载。需要修改时再进入编辑模式。",
                                style = TextStyle.Meta,
                                color = TextColor.Secondary,
                            ),
                        ),
                    ),
                )
                add(
                    row(
                        cell(
                            ButtonComponent(
                                id = "file_edit.preview_copy",
                                text = "复制",
                                kind = ButtonKind.Secondary,
                                enabled = state.content.isNotEmpty(),
                                action = "file_edit.copy",
                            ),
                            span = 6,
                        ),
                        cell(
                            ButtonComponent(
                                id = "file_edit.enter_edit",
                                text = "进入编辑",
                                kind = ButtonKind.Primary,
                                enabled = state.originalSha.isNotBlank(),
                                action = "file_edit.enter_edit",
                            ),
                            span = 6,
                        ),
                    ),
                )
            }
            if (state.isCreateMode) {
                // —— 目标路径（新建模式） ——
                add(row(cell(SpacerComponent(id = "file_edit.spacer.path", heightDp = 8))))
                add(
                    row(
                        cell(
                            FieldComponent(
                                id = "file_edit.target_path",
                                value = targetPathDraft,
                                hint = "目标路径",
                                singleLine = true,
                                enabled = !previewMode && !state.isSubmitting && !state.isDeleting,
                                onChange = onTargetPathChange,
                            ),
                        ),
                    ),
                )
            }
            // —— 错误消息 ——
            state.errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
                add(
                    row(
                        cell(
                            TextComponent(
                                id = "file_edit.error",
                                text = message,
                                style = TextStyle.Body,
                                color = TextColor.Danger,
                            ),
                        ),
                    ),
                )
            }
            // —— 加载消息 ——
            if (state.isLoading) {
                add(
                    row(
                        cell(
                            TextComponent(
                                id = "file_edit.loading",
                                text = "正在加载文件内容…",
                                style = TextStyle.Meta,
                                color = TextColor.Secondary,
                            ),
                        ),
                    ),
                )
            }
        }
        return PageSchema(id = "file_edit_top", columns = 12, scrollable = false, rows = rows)
    }

    /** 底部 schema：提交信息 + 操作行（非预览模式）。 */
    fun bottomSchema(
        state: RepositoryFileEditUiState,
        previewMode: Boolean,
        commitMessageDraft: String,
        onCommitMessageChange: (String) -> Unit = {},
    ): PageSchema {
        if (previewMode) return PageSchema(id = "file_edit_bottom", columns = 12, scrollable = false, rows = emptyList())
        val rows = buildList<RowSchema> {
            add(
                row(
                    cell(
                        FieldComponent(
                            id = "file_edit.commit_message",
                            value = commitMessageDraft,
                            hint = "提交说明",
                            singleLine = false,
                            enabled = !state.isSubmitting && !state.isDeleting,
                            onChange = onCommitMessageChange,
                        ),
                    ),
                ),
            )
            // —— 操作行：复制/聚焦/删除/提交 ——
            add(
                row(
                    cell(
                        ButtonComponent(
                            id = "file_edit.actions_copy",
                            text = "复制",
                            kind = ButtonKind.Secondary,
                            enabled = state.content.isNotEmpty(),
                            action = "file_edit.copy",
                        ),
                        span = 3,
                    ),
                    cell(
                        ButtonComponent(
                            id = "file_edit.actions_focus",
                            text = "聚焦",
                            kind = ButtonKind.Secondary,
                            action = "file_edit.focus_editor",
                        ),
                        span = 3,
                    ),
                    cell(
                        ButtonComponent(
                            id = "file_edit.delete",
                            text = "删除文件",
                            kind = ButtonKind.Secondary,
                            enabled = !state.isCreateMode && state.originalSha.isNotBlank() && !state.isSubmitting && !state.isDeleting,
                            action = "file_edit.delete",
                        ),
                        span = 3,
                    ),
                    cell(
                        ButtonComponent(
                            id = "file_edit.submit",
                            text = if (state.isSubmitting) "提交中…" else "提交",
                            kind = ButtonKind.Primary,
                            enabled = state.canSubmit && !state.isSubmitting && !state.isDeleting,
                            action = "file_edit.submit",
                        ),
                        span = 3,
                    ),
                ),
            )
            add(
                row(
                    cell(
                        TextComponent(
                            id = "file_edit.hint",
                            text = "提交前请确认路径、内容和提交信息。",
                            style = TextStyle.Caption,
                            color = TextColor.Muted,
                        ),
                    ),
                ),
            )
        }
        return PageSchema(id = "file_edit_bottom", columns = 12, scrollable = false, rows = rows)
    }

    /** 文件编辑页壳状态：Hidden + 返回（独立全屏页）。 */
    fun shellState(state: RepositoryFileEditUiState, previewMode: Boolean): ShellState = ShellState(
        title = titleText(state, previewMode),
        showBack = true,
        backAction = "shell.back",
        navBarMode = NavBarMode.Hidden,
        contentKey = "file_edit",
    )
}

/**
 * 仓库文件编辑页入口：壳 + 混合布局（schema + 原生编辑器）。
 * 编辑器宿主由调用端创建并传入；提交/删除/放弃/冲突 Dialog 与未保存返回确认由调用端承载。
 */
@Composable
fun RepositoryFileEditPageContent(
    state: RepositoryFileEditUiState,
    owner: String,
    repo: String,
    previewMode: Boolean,
    targetPathDraft: String,
    commitMessageDraft: String,
    editorHost: TextEditorHost,
    onTargetPathChange: (String) -> Unit = {},
    onCommitMessageChange: (String) -> Unit = {},
    onEnterEditMode: () -> Unit = {},
    onSubmit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onCopy: () -> Unit = {},
    onFocusEditor: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    val handleAction: (String) -> Unit = { action ->
        when (action) {
            "shell.back" -> onBack()
            "file_edit.submit" -> onSubmit()
            "file_edit.delete" -> onDelete()
            "file_edit.copy" -> onCopy()
            "file_edit.focus_editor" -> onFocusEditor()
            "file_edit.enter_edit" -> onEnterEditMode()
        }
    }
    AppShell(state = RepositoryFileEditPage.shellState(state, previewMode), onAction = handleAction) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部 schema（非滚动、不填满，高度由内容决定）
            RepositoryFileEditPage.topSchema(state, owner, repo, previewMode, targetPathDraft, onTargetPathChange)
                .renderPage(handleAction, fillMaxSize = false)
            // 中部原生编辑器：弹性填充（内部滚动由编辑器承载）
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                TextEditorHostView(host = editorHost, modifier = Modifier.fillMaxSize())
            }
            // 底部 schema（提交信息 + 操作行）
            RepositoryFileEditPage.bottomSchema(state, previewMode, commitMessageDraft, onCommitMessageChange)
                .renderPage(handleAction, fillMaxSize = false)
        }
    }
}
