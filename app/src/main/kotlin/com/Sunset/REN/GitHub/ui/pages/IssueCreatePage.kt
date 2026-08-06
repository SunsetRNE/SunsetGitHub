package com.Sunset.REN.GitHub.ui.pages

import androidx.compose.runtime.Composable
import com.Sunset.REN.GitHub.ui.layout.PageSchema
import com.Sunset.REN.GitHub.ui.layout.RowSchema
import com.Sunset.REN.GitHub.ui.layout.cell
import com.Sunset.REN.GitHub.ui.layout.row
import com.Sunset.REN.GitHub.ui.render.renderPage
import com.Sunset.REN.GitHub.ui.repo.IssueLabelDisplayNames
import com.Sunset.REN.GitHub.ui.repo.RepositoryIssueCreateUiState
import com.Sunset.REN.GitHub.ui.repo.RepositorySection
import com.Sunset.REN.GitHub.ui.schema.ButtonComponent
import com.Sunset.REN.GitHub.ui.schema.ButtonKind
import com.Sunset.REN.GitHub.ui.schema.FieldComponent
import com.Sunset.REN.GitHub.ui.schema.ItemComponent
import com.Sunset.REN.GitHub.ui.schema.SpacerComponent
import com.Sunset.REN.GitHub.ui.schema.TextColor
import com.Sunset.REN.GitHub.ui.schema.TextComponent
import com.Sunset.REN.GitHub.ui.schema.TextStyle
import com.Sunset.REN.GitHub.ui.shell.AppShell
import com.Sunset.REN.GitHub.ui.shell.NavBarMode
import com.Sunset.REN.GitHub.ui.shell.ShellState

/**
 * 新建议题页（Issue Create）垂直切片（任务 2：仓库写入流表单页）。
 *
 * 与 IssuesPage 共用 RepositorySections 壳（选中 Issues 分区）。
 * 渲染结构对齐 RepositoryIssueCreateScreen：
 * - 上下文条（owner/repo）+ 新建议题卡（标题计数 + 标题输入 + 标签区 + 正文输入）；
 * - 标签区：加载中/空/未选择/已选择 N 个 四态 + 标签行列表（badge ✓ 表示选中，点击切换）；
 * - 错误信息（danger）+ 发布议题按钮（isSubmitting 禁用）。
 * 壳：RepositorySections + showBack。
 * 路由前缀：issue_create.submit / issue_create.label.toggle.{index} / shell.back。
 * 草稿 title/body 由调用端持有（rememberSaveable），title 上限 256 在回调内截断。
 */

/** 新建议题页。 */
object IssueCreatePage {

    /** 标签区状态行文案（四态纯函数）。 */
    private fun labelsStatus(state: RepositoryIssueCreateUiState): String = when {
        state.isLoadingLabels -> "正在加载标签…"
        state.availableLabels.isEmpty() -> "暂无可用标签"
        state.selectedLabels.isEmpty() -> "未选择标签"
        else -> "已选择 ${state.selectedLabels.size} 个标签"
    }

    fun schemaFor(
        state: RepositoryIssueCreateUiState,
        draftTitle: String,
        draftBody: String,
        titleError: String?,
        onDraftChanged: (String, String) -> Unit = { _, _ -> },
    ): PageSchema {
        val rows = buildList<RowSchema> {
            // —— 上下文条 ——
            add(
                row(
                    cell(
                        TextComponent(
                            id = "issue_create.context",
                            text = if (state.owner.isBlank() || state.repo.isBlank()) {
                                "新建议题"
                            } else {
                                "# ${state.owner}/${state.repo} · Issue"
                            },
                            style = TextStyle.Body,
                            color = TextColor.Primary,
                        ),
                    ),
                ),
            )
            add(row(cell(SpacerComponent(id = "issue_create.spacer.card", heightDp = 8))))
            // —— 标题行：分区标题 + 计数 ——
            add(
                row(
                    cell(
                        TextComponent(
                            id = "issue_create.section",
                            text = "新建议题",
                            style = TextStyle.Section,
                            color = TextColor.Primary,
                        ),
                        span = 8,
                    ),
                    cell(
                        TextComponent(
                            id = "issue_create.count",
                            text = "${draftTitle.length} / 256",
                            style = TextStyle.Caption,
                            color = TextColor.Muted,
                        ),
                        span = 4,
                    ),
                ),
            )
            // —— 标题输入 ——
            add(
                row(
                    cell(
                        FieldComponent(
                            id = "issue_create.title",
                            value = draftTitle,
                            hint = "议题标题",
                            singleLine = true,
                            enabled = !state.isSubmitting,
                            isError = titleError != null,
                            supportingText = titleError,
                            onChange = { newTitle -> onDraftChanged(newTitle.take(256), draftBody) },
                        ),
                    ),
                ),
            )
            add(row(cell(SpacerComponent(id = "issue_create.spacer.labels", heightDp = 8))))
            // —— 标签区 ——
            add(
                row(
                    cell(
                        TextComponent(
                            id = "issue_create.labels.section",
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
                        TextComponent(
                            id = "issue_create.labels.status",
                            text = labelsStatus(state),
                            style = TextStyle.Caption,
                            color = TextColor.Muted,
                        ),
                    ),
                ),
            )
            if (state.availableLabels.isNotEmpty()) {
                state.availableLabels.forEachIndexed { index, label ->
                    add(
                        row(
                            cell(
                                ItemComponent(
                                    id = "issue_create.label.toggle.$index",
                                    title = IssueLabelDisplayNames.displayName(label.name),
                                    badge = if (state.selectedLabels.contains(label.name)) "✓" else null,
                                    badgeColor = TextColor.Accent,
                                    action = "issue_create.label.toggle.$index",
                                ),
                            ),
                        ),
                    )
                }
            }
            if (!state.labelErrorMessage.isNullOrBlank()) {
                add(
                    row(
                        cell(
                            TextComponent(
                                id = "issue_create.labels.error",
                                text = state.labelErrorMessage.orEmpty(),
                                style = TextStyle.Caption,
                                color = TextColor.Danger,
                            ),
                        ),
                    ),
                )
            }
            add(row(cell(SpacerComponent(id = "issue_create.spacer.body", heightDp = 8))))
            // —— 正文输入 ——
            add(
                row(
                    cell(
                        FieldComponent(
                            id = "issue_create.body",
                            value = draftBody,
                            hint = "描述问题、需求或上下文",
                            singleLine = false,
                            enabled = !state.isSubmitting,
                            supportingText = "Markdown",
                            onChange = { newBody -> onDraftChanged(draftTitle, newBody) },
                        ),
                    ),
                ),
            )
            // —— 错误信息 ——
            if (!state.errorMessage.isNullOrBlank()) {
                add(
                    row(
                        cell(
                            TextComponent(
                                id = "issue_create.error",
                                text = "创建议题失败：${state.errorMessage.orEmpty()}",
                                style = TextStyle.Caption,
                                color = TextColor.Danger,
                            ),
                        ),
                    ),
                )
            }
            add(row(cell(SpacerComponent(id = "issue_create.spacer.submit", heightDp = 8))))
            add(
                row(
                    cell(
                        ButtonComponent(
                            id = "issue_create.submit",
                            text = if (state.isSubmitting) "正在发布…" else "发布议题",
                            kind = ButtonKind.Primary,
                            enabled = !state.isSubmitting,
                            action = "issue_create.submit",
                        ),
                    ),
                ),
            )
        }
        return PageSchema(id = "issue_create", columns = 12, scrollable = true, rows = rows)
    }

    /** Issue 创建页壳状态：保持仓库上下文（RepositorySections 分段导航，选中 Issues）。 */
    fun shellState(
        fullName: String,
        sections: List<RepositorySection>,
    ): ShellState = ShellState(
        title = "新建议题",
        showBack = true,
        backAction = "shell.back",
        navBarMode = NavBarMode.RepositorySections,
        navItems = sections.map(RepositoryDetailPage::sectionNavItem),
        selectedNavId = RepositorySection.Issues.storageKey,
        contentKey = "issue_create",
    ).let { if (fullName.isBlank()) it else it.copy(title = fullName) }
}

/**
 * 新建议题页入口：壳 + 表单 schema。
 * 草稿 title/body 由调用端持有；onDraftChanged 合并回调（title 截断 256 由调用端保证）；
 * 标签切换与提交通过 onAction 路由；createdIssueNumber 跳转由调用端承载。
 */
@Composable
fun IssueCreatePageContent(
    state: RepositoryIssueCreateUiState,
    draftTitle: String,
    draftBody: String,
    titleError: String?,
    sections: List<RepositorySection> = emptyList(),
    onDraftChanged: (String, String) -> Unit = { _, _ -> },
    onLabelsChanged: (List<String>) -> Unit = {},
    onSubmit: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    val handleAction: (String) -> Unit = { action ->
        when {
            action == "shell.back" -> onBack()
            action == "issue_create.submit" -> onSubmit()
            action.startsWith("issue_create.label.toggle.") -> {
                val index = action.removePrefix("issue_create.label.toggle.").toIntOrNull()
                if (index != null) {
                    val label = state.availableLabels.getOrNull(index)
                    if (label != null) {
                        val next = if (state.selectedLabels.contains(label.name)) {
                            state.selectedLabels - label.name
                        } else {
                            state.selectedLabels + label.name
                        }
                        onLabelsChanged(next)
                    }
                }
            }
        }
    }
    AppShell(state = IssueCreatePage.shellState(state.fullName(), sections), onAction = handleAction) {
        IssueCreatePage.schemaFor(state, draftTitle, draftBody, titleError, onDraftChanged).renderPage(handleAction)
    }
}

/** UiState 便捷扩展：owner/repo 组合名。 */
private fun RepositoryIssueCreateUiState.fullName(): String =
    if (owner.isBlank() || repo.isBlank()) "" else "$owner/$repo"