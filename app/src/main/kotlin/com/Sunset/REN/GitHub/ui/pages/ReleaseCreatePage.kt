package com.Sunset.REN.GitHub.ui.pages

import androidx.compose.runtime.Composable
import com.Sunset.REN.GitHub.ui.layout.PageSchema
import com.Sunset.REN.GitHub.ui.layout.RowSchema
import com.Sunset.REN.GitHub.ui.layout.cell
import com.Sunset.REN.GitHub.ui.layout.row
import com.Sunset.REN.GitHub.ui.render.renderPage
import com.Sunset.REN.GitHub.ui.repo.RepositoryReleaseCreateUiState
import com.Sunset.REN.GitHub.ui.schema.ButtonComponent
import com.Sunset.REN.GitHub.ui.schema.ButtonKind
import com.Sunset.REN.GitHub.ui.schema.FieldComponent
import com.Sunset.REN.GitHub.ui.schema.IconId
import com.Sunset.REN.GitHub.ui.schema.ItemAction
import com.Sunset.REN.GitHub.ui.schema.ItemComponent
import com.Sunset.REN.GitHub.ui.schema.SpacerComponent
import com.Sunset.REN.GitHub.ui.schema.SwitchComponent
import com.Sunset.REN.GitHub.ui.schema.TextColor
import com.Sunset.REN.GitHub.ui.schema.TextComponent
import com.Sunset.REN.GitHub.ui.schema.TextStyle
import com.Sunset.REN.GitHub.ui.shell.AppShell
import com.Sunset.REN.GitHub.ui.shell.NavBarMode
import com.Sunset.REN.GitHub.ui.shell.ShellState
import java.util.Locale

/**
 * 新建发布页（Release Create）垂直切片（任务 2：仓库写入流表单页）。
 *
 * 与 ReleasesPage 共用 RepositorySections 壳（选中 Releases 分区）。
 * 渲染结构对齐 RepositoryReleaseCreateScreen（六卡片）：
 * - 头卡（标题 + owner/repo + 上一个标签三态）；
 * - 发布类型卡（预览/草稿/正式发布三开关，互斥逻辑由调用端承载）；
 * - 标签卡（标签名/发布名称/发布说明三输入 + Markdown 快捷按钮行）；
 * - 发布目标卡（分支三态 + 分支行列表，badge ✓ 选中 + default/protected/ready meta）；
 * - 附件卡（空提示/草稿行 + 添加附件）；
 * - 状态卡（error/status/subtitle 消息 + 提交按钮三态文本）。
 * 壳：RepositorySections + showBack。
 * 路由前缀：release_create.type.* / release_create.md.* / release_create.branch.{index} /
 * release_create.add_asset / release_create.remove_asset.{index} / release_create.submit / shell.back。
 * 附件选择（FileSourcePickerDialog/本地缓存）由调用端承载。
 */

/** 新建发布页。 */
object ReleaseCreatePage {

    /** 上一个标签文案（三态纯函数）。 */
    private fun previousTagText(state: RepositoryReleaseCreateUiState): String = when {
        state.isLoadingPreviousTag -> "正在加载上一个标签…"
        !state.previousTagName.isNullOrBlank() -> "上一个标签：${state.previousTagName}"
        else -> "暂无历史标签"
    }

    /** 发布目标文案（三态纯函数）。 */
    private fun branchText(state: RepositoryReleaseCreateUiState): String = when {
        state.isLoadingBranches -> "正在加载分支…"
        state.selectedBranchName.isNotBlank() -> state.selectedBranchName
        else -> "使用默认分支"
    }

    /** 分支 meta 行（default/protected/ready 三态片段）。 */
    private fun branchMeta(isDefault: Boolean, isProtected: Boolean): String = listOfNotNull(
        "default".takeIf { isDefault },
        "protected".takeIf { isProtected },
        "ready",
    ).joinToString(" · ")

    /** 附件大小格式化。 */
    private fun formatFileSize(bytes: Long): String {
        if (bytes < 1024L) return "$bytes B"
        val units = listOf("KiB", "MiB", "GiB")
        var value = bytes.toDouble() / 1024.0
        var unitIndex = 0
        while (value >= 1024.0 && unitIndex < units.lastIndex) {
            value /= 1024.0
            unitIndex++
        }
        return "%.1f %s".format(Locale.US, value, units[unitIndex])
    }

    /** 状态卡消息（error/status/subtitle 三态纯函数）。 */
    private fun statusMessage(state: RepositoryReleaseCreateUiState): String = when {
        !state.errorMessage.isNullOrBlank() -> "创建发布失败：${state.errorMessage}"
        !state.statusMessage.isNullOrBlank() -> state.statusMessage.orEmpty()
        else -> "为当前仓库创建 Release。"
    }

    fun schemaFor(
        state: RepositoryReleaseCreateUiState,
        tagName: String,
        releaseName: String,
        body: String,
        prerelease: Boolean,
        draft: Boolean,
        makeLatest: Boolean,
        tagError: String?,
        onTagNameChange: (String) -> Unit = {},
        onReleaseNameChange: (String) -> Unit = {},
        onBodyChange: (String) -> Unit = {},
    ): PageSchema {
        val rows = buildList<RowSchema> {
            // —— 头卡 ——
            add(
                row(
                    cell(
                        TextComponent(
                            id = "release_create.title",
                            text = "新建发布",
                            style = TextStyle.Section,
                            color = TextColor.Primary,
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        TextComponent(
                            id = "release_create.repo",
                            text = if (state.owner.isBlank() || state.repo.isBlank()) {
                                "为当前仓库创建 Release。"
                            } else {
                                "${state.owner}/${state.repo}"
                            },
                            style = TextStyle.Body,
                            color = TextColor.Secondary,
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        TextComponent(
                            id = "release_create.previous_tag",
                            text = previousTagText(state),
                            style = TextStyle.Meta,
                            color = TextColor.Muted,
                        ),
                    ),
                ),
            )
            add(row(cell(SpacerComponent(id = "release_create.spacer.type", heightDp = 8))))
            // —— 发布类型卡 ——
            add(
                row(
                    cell(
                        TextComponent(
                            id = "release_create.type_section",
                            text = "发布类型",
                            style = TextStyle.Subtitle,
                            color = TextColor.Primary,
                        ),
                    ),
                ),
            )
            add(row(cell(SwitchComponent(id = "release_create.type.prerelease", title = "预览", checked = prerelease, action = "release_create.type.prerelease"))))
            add(row(cell(SwitchComponent(id = "release_create.type.draft", title = "草稿", checked = draft, action = "release_create.type.draft"))))
            add(row(cell(SwitchComponent(id = "release_create.type.release", title = "正式发布", checked = makeLatest, action = "release_create.type.release"))))
            add(row(cell(SpacerComponent(id = "release_create.spacer.fields", heightDp = 8))))
            // —— 标签/名称/正文卡 ——
            add(
                row(
                    cell(
                        TextComponent(
                            id = "release_create.tag_section",
                            text = "标签",
                            style = TextStyle.Subtitle,
                            color = TextColor.Primary,
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        FieldComponent(
                            id = "release_create.tag",
                            value = tagName,
                            hint = "标签名，例如 v1.0.0",
                            singleLine = true,
                            enabled = !state.isSubmitting,
                            isError = tagError != null,
                            supportingText = tagError,
                            onChange = onTagNameChange,
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        FieldComponent(
                            id = "release_create.name",
                            value = releaseName,
                            hint = "发布名称",
                            singleLine = true,
                            enabled = !state.isSubmitting,
                            onChange = onReleaseNameChange,
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        FieldComponent(
                            id = "release_create.body",
                            value = body,
                            hint = "发布说明",
                            singleLine = false,
                            enabled = !state.isSubmitting,
                            onChange = onBodyChange,
                        ),
                    ),
                ),
            )
            // —— Markdown 快捷按钮行 ——
            add(
                row(
                    cell(
                        ButtonComponent(
                            id = "release_create.md.bold",
                            text = "Markdown",
                            kind = ButtonKind.Secondary,
                            enabled = !state.isSubmitting,
                            action = "release_create.md.bold",
                        ),
                        span = 4,
                    ),
                    cell(
                        ButtonComponent(
                            id = "release_create.md.link",
                            text = "链接",
                            kind = ButtonKind.Secondary,
                            enabled = !state.isSubmitting,
                            action = "release_create.md.link",
                        ),
                        span = 4,
                    ),
                    cell(
                        ButtonComponent(
                            id = "release_create.md.list",
                            text = "列表",
                            kind = ButtonKind.Secondary,
                            enabled = !state.isSubmitting,
                            action = "release_create.md.list",
                        ),
                        span = 4,
                    ),
                ),
            )
            add(row(cell(SpacerComponent(id = "release_create.spacer.target", heightDp = 8))))
            // —— 发布目标卡 ——
            add(
                row(
                    cell(
                        TextComponent(
                            id = "release_create.target_section",
                            text = "发布目标",
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
                            id = "release_create.target",
                            text = branchText(state),
                            style = TextStyle.Body,
                            color = TextColor.Secondary,
                        ),
                    ),
                ),
            )
            if (!state.branchErrorMessage.isNullOrBlank()) {
                add(
                    row(
                        cell(
                            TextComponent(
                                id = "release_create.target_error",
                                text = state.branchErrorMessage,
                                style = TextStyle.Meta,
                                color = TextColor.Danger,
                            ),
                        ),
                    ),
                )
            }
            state.branches.take(5).forEachIndexed { index, branch ->
                add(
                    row(
                        cell(
                            ItemComponent(
                                id = "release_create.branch.$index",
                                title = branch.name,
                                meta = listOf(branchMeta(branch.isDefault, branch.isProtected)),
                                badge = if (branch.name == state.selectedBranchName) "✓" else null,
                                badgeColor = TextColor.Success,
                                action = "release_create.branch.$index",
                            ),
                        ),
                    ),
                )
            }
            add(row(cell(SpacerComponent(id = "release_create.spacer.assets", heightDp = 8))))
            // —— 附件卡 ——
            add(
                row(
                    cell(
                        TextComponent(
                            id = "release_create.assets_section",
                            text = "附件",
                            style = TextStyle.Subtitle,
                            color = TextColor.Primary,
                        ),
                    ),
                ),
            )
            if (state.assets.isEmpty()) {
                add(
                    row(
                        cell(
                            TextComponent(
                                id = "release_create.assets_empty",
                                text = "暂无可下载产物",
                                style = TextStyle.Body,
                                color = TextColor.Secondary,
                            ),
                        ),
                    ),
                )
            } else {
                state.assets.forEachIndexed { index, asset ->
                    add(
                        row(
                            cell(
                                ItemComponent(
                                    id = "release_create.asset.$index",
                                    title = asset.fileName,
                                    meta = listOf(formatFileSize(asset.sizeBytes), asset.mimeType),
                                    actions = listOf(
                                        ItemAction(
                                            id = "remove",
                                            icon = IconId.Close,
                                            contentDescription = "移除附件",
                                            action = "release_create.remove_asset.$index",
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    )
                }
            }
            add(
                row(
                    cell(
                        ButtonComponent(
                            id = "release_create.add_asset",
                            text = "添加附件",
                            kind = ButtonKind.Secondary,
                            enabled = !state.isSubmitting,
                            action = "release_create.add_asset",
                        ),
                    ),
                ),
            )
            add(row(cell(SpacerComponent(id = "release_create.spacer.status", heightDp = 8))))
            // —— 状态卡 ——
            add(
                row(
                    cell(
                        TextComponent(
                            id = "release_create.status",
                            text = statusMessage(state),
                            style = TextStyle.Body,
                            color = if (state.errorMessage != null) TextColor.Danger else TextColor.Secondary,
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        ButtonComponent(
                            id = "release_create.submit",
                            text = when {
                                state.isSubmitting -> "正在发布…"
                                draft -> "保存为草稿"
                                else -> "发布"
                            },
                            kind = ButtonKind.Primary,
                            enabled = !state.isSubmitting,
                            action = "release_create.submit",
                        ),
                    ),
                ),
            )
        }
        return PageSchema(id = "release_create", columns = 12, scrollable = true, rows = rows)
    }

    /** 发布创建页壳状态：次级页（Hidden + 返回），与 ReleasesPage 一致。 */
    fun shellState(
        fullName: String,
    ): ShellState = ShellState(
        title = "新建发布",
        showBack = true,
        backAction = "shell.back",
        navBarMode = NavBarMode.Hidden,
        contentKey = "release_create",
    ).let { if (fullName.isBlank()) it else it.copy(title = fullName) }
}

/**
 * 新建发布页入口：壳 + 表单 schema。
 * 草稿字段（tagName/releaseName/body/类型开关）由调用端持有；类型互斥与附件选择由调用端承载；
 * 创建成功（createdTagName）跳转由调用端承载。
 */
@Composable
fun ReleaseCreatePageContent(
    state: RepositoryReleaseCreateUiState,
    tagName: String,
    releaseName: String,
    body: String,
    prerelease: Boolean,
    draft: Boolean,
    makeLatest: Boolean,
    tagError: String?,
    onTagNameChange: (String) -> Unit = {},
    onReleaseNameChange: (String) -> Unit = {},
    onBodyChange: (String) -> Unit = {},
    onPrereleaseChange: (Boolean) -> Unit = {},
    onDraftChange: (Boolean) -> Unit = {},
    onMakeLatestChange: (Boolean) -> Unit = {},
    onSelectBranch: (String) -> Unit = {},
    onAddAsset: () -> Unit = {},
    onRemoveAsset: (Int) -> Unit = {},
    onSubmit: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    val handleAction: (String) -> Unit = { action ->
        when {
            action == "shell.back" -> onBack()
            action == "release_create.submit" -> onSubmit()
            action == "release_create.add_asset" -> onAddAsset()
            action == "release_create.type.prerelease" -> onPrereleaseChange(!prerelease)
            action == "release_create.type.draft" -> onDraftChange(!draft)
            action == "release_create.type.release" -> onMakeLatestChange(!makeLatest)
            action == "release_create.md.bold" -> onBodyChange(body + "\n\n**加粗文本**")
            action == "release_create.md.link" -> onBodyChange(body + "\n[链接文本](https://)")
            action == "release_create.md.list" -> onBodyChange(body + "\n- 列表项")
            action.startsWith("release_create.branch.") -> {
                val index = action.removePrefix("release_create.branch.").toIntOrNull()
                if (index != null) {
                    state.branches.getOrNull(index)?.let { branch -> onSelectBranch(branch.name) }
                }
            }
            action.startsWith("release_create.remove_asset.") -> {
                val index = action.removePrefix("release_create.remove_asset.").toIntOrNull()
                if (index != null) onRemoveAsset(index)
            }
        }
    }
    AppShell(state = ReleaseCreatePage.shellState(state.fullName()), onAction = handleAction) {
        ReleaseCreatePage.schemaFor(
            state, tagName, releaseName, body, prerelease, draft, makeLatest, tagError,
            onTagNameChange, onReleaseNameChange, onBodyChange,
        ).renderPage(handleAction)
    }
}

/** UiState 便捷扩展：owner/repo 组合名。 */
private fun RepositoryReleaseCreateUiState.fullName(): String =
    if (owner.isBlank() || repo.isBlank()) "" else "$owner/$repo"