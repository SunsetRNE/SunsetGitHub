package com.Sunset.REN.GitHub.ui.pages

import androidx.compose.runtime.Composable
import com.Sunset.REN.GitHub.ui.layout.PageSchema
import com.Sunset.REN.GitHub.ui.layout.RowSchema
import com.Sunset.REN.GitHub.ui.layout.cell
import com.Sunset.REN.GitHub.ui.layout.row
import com.Sunset.REN.GitHub.ui.render.renderPage
import com.Sunset.REN.GitHub.ui.repo.RepositoryBranchSettingsRow
import com.Sunset.REN.GitHub.ui.repo.RepositoryBranchSettingsUiState
import com.Sunset.REN.GitHub.ui.repo.toBranchProtectionSummary
import com.Sunset.REN.GitHub.ui.schema.ItemComponent
import com.Sunset.REN.GitHub.ui.schema.SectionHeaderComponent
import com.Sunset.REN.GitHub.ui.schema.SpacerComponent
import com.Sunset.REN.GitHub.ui.schema.StateComponent
import com.Sunset.REN.GitHub.ui.schema.StateKind
import com.Sunset.REN.GitHub.ui.schema.TextColor
import com.Sunset.REN.GitHub.ui.schema.TextComponent
import com.Sunset.REN.GitHub.ui.schema.TextStyle
import com.Sunset.REN.GitHub.ui.shell.AppShell
import com.Sunset.REN.GitHub.ui.shell.NavBarMode
import com.Sunset.REN.GitHub.ui.shell.ShellState

/**
 * 仓库分支设置页垂直切片（组 B：仓库设置/管理）。
 *
 * 渲染结构对齐 RepositoryBranchSettingsScreen（三分支）：
 * - 初始加载 → Loading；错误（snapshot 为空）→ Error+重试；snapshot → Content；
 * - Content：inline 消息（加载保护/保存/错误三态）+ 概览卡（默认分支/分支数/保护数/
 *   权限四指标 + 部分详情提示）+ 分支列表卡（名称·默认·保护状态 + sha·保护摘要，选中 ✓）+
 *   选中分支卡（保护摘要 + 启用 PR 审查/删除保护两动作）。
 * 壳：Hidden + showBack（独立全屏页）。
 * 路由前缀：branch_settings.select.{name} / edit_protection / delete_protection / retry / shell.back。
 * 保护规则编辑/删除 Dialog 由调用端承载。
 */
object RepositoryBranchSettingsPage {

    /** 分支行标题（默认徽章 + 保护状态）。 */
    private fun branchTitle(row: RepositoryBranchSettingsRow): String = buildString {
        append(row.name)
        if (row.isDefault) append(" · 默认")
        append(if (row.isProtected) " · 已保护" else " · 未保护")
    }

    /** 分支行副标题（sha · 保护摘要）。 */
    private fun branchSubtitle(row: RepositoryBranchSettingsRow): String =
        "${row.sha.ifBlank { "-" }} · ${row.protectionSummary}"

    fun schemaFor(
        state: RepositoryBranchSettingsUiState,
        onSelectBranch: (String) -> Unit = {},
    ): PageSchema {
        val rows = buildList<RowSchema> {
            when {
                state.isInitialLoad -> add(
                    row(
                        cell(
                            StateComponent(
                                id = "branch_settings.loading",
                                kind = StateKind.Loading,
                                message = "正在加载分支设置…",
                            ),
                        ),
                    ),
                )

                state.errorMessage != null && state.snapshot == null -> add(
                    row(
                        cell(
                            StateComponent(
                                id = "branch_settings.error",
                                kind = StateKind.Error,
                                message = "加载分支设置失败",
                                detail = state.errorMessage,
                                retryAction = "branch_settings.retry",
                            ),
                        ),
                    ),
                )

                state.snapshot != null -> {
                    val snapshot = state.snapshot
                    // —— inline 消息 ——
                    val inlineMessage = when {
                        state.isLoadingProtection -> "正在加载保护规则…"
                        state.isSaving -> state.pendingMessage.orEmpty()
                        !state.errorMessage.isNullOrBlank() -> state.errorMessage.orEmpty()
                        else -> ""
                    }
                    if (inlineMessage.isNotBlank()) {
                        add(
                            row(
                                cell(
                                    TextComponent(
                                        id = "branch_settings.inline",
                                        text = inlineMessage,
                                        style = TextStyle.Body,
                                        color = if (!state.errorMessage.isNullOrBlank()) TextColor.Danger else TextColor.Secondary,
                                    ),
                                ),
                            ),
                        )
                    }
                    // —— 概览卡 ——
                    add(
                        row(
                            cell(
                                SectionHeaderComponent(
                                    id = "branch_settings.overview",
                                    title = "分支设置",
                                    subtitle = "默认分支 ${snapshot.defaultBranch} · 共 ${snapshot.branches.size} 个分支 · 已保护 ${snapshot.protectedBranchCount} 个",
                                ),
                            ),
                        ),
                    )
                    add(
                        row(
                            cell(
                                TextComponent(
                                    id = "branch_settings.metric_default",
                                    text = "默认分支：${snapshot.defaultBranch}",
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
                                    id = "branch_settings.metric_count",
                                    text = "分支数量：${snapshot.branches.size}",
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
                                    id = "branch_settings.metric_protected",
                                    text = "已保护数量：${snapshot.protectedBranchCount}",
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
                                    id = "branch_settings.metric_permission",
                                    text = if (snapshot.canAdmin) "权限：可编辑" else "权限：只读",
                                    style = TextStyle.Meta,
                                    color = TextColor.Secondary,
                                ),
                            ),
                        ),
                    )
                    if (snapshot.hasMoreProtectionDetailsThanLoaded) {
                        add(
                            row(
                                cell(
                                    TextComponent(
                                        id = "branch_settings.partial",
                                        text = "部分保护规则详情未加载，可在 GitHub 网页端查看完整配置。",
                                        style = TextStyle.Caption,
                                        color = TextColor.Muted,
                                    ),
                                ),
                            ),
                        )
                    }
                    add(row(cell(SpacerComponent(id = "branch_settings.spacer.list", heightDp = 8))))
                    // —— 分支列表卡 ——
                    add(
                        row(
                            cell(
                                SectionHeaderComponent(
                                    id = "branch_settings.list_header",
                                    title = "分支",
                                ),
                            ),
                        ),
                    )
                    if (state.branches.isEmpty()) {
                        add(
                            row(
                                cell(
                                    TextComponent(
                                        id = "branch_settings.empty",
                                        text = "暂无分支。",
                                        style = TextStyle.Body,
                                        color = TextColor.Secondary,
                                    ),
                                ),
                            ),
                        )
                    } else {
                        state.branches.forEach { row ->
                            add(
                                row(
                                    cell(
                                        ItemComponent(
                                            id = "branch_settings.branch.${row.name}",
                                            title = branchTitle(row),
                                            subtitle = branchSubtitle(row),
                                            badge = if (row.name == state.selectedBranch) "✓" else null,
                                            badgeColor = TextColor.Success,
                                            action = "branch_settings.select.${row.name}",
                                        ),
                                    ),
                                ),
                            )
                        }
                    }
                    add(row(cell(SpacerComponent(id = "branch_settings.spacer.selected", heightDp = 8))))
                    // —— 选中分支卡 ——
                    add(
                        row(
                            cell(
                                SectionHeaderComponent(
                                    id = "branch_settings.selected_header",
                                    title = "选中分支",
                                ),
                            ),
                        ),
                    )
                    val selectedBranch = state.selectedBranch
                    if (selectedBranch.isNullOrBlank()) {
                        add(
                            row(
                                cell(
                                    TextComponent(
                                        id = "branch_settings.select_hint",
                                        text = "从上方列表选择一个分支查看保护规则。",
                                        style = TextStyle.Body,
                                        color = TextColor.Secondary,
                                    ),
                                ),
                            ),
                        )
                    } else {
                        add(
                            row(
                                cell(
                                    TextComponent(
                                        id = "branch_settings.selected_name",
                                        text = "分支：$selectedBranch",
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
                                        id = "branch_settings.selected_protection",
                                        text = state.selectedProtection?.toBranchProtectionSummary() ?: "暂无保护规则详情",
                                        style = TextStyle.Body,
                                        color = TextColor.Secondary,
                                    ),
                                ),
                            ),
                        )
                        val canEdit = snapshot.canAdmin && !state.isSaving
                        val hasProtection = state.selectedProtection != null ||
                            state.branches.firstOrNull { it.name == selectedBranch }?.isProtected == true
                        add(
                            row(
                                cell(
                                    TextComponent(
                                        id = "branch_settings.action_edit",
                                        text = "启用 PR 审查 / 编辑保护规则",
                                        style = TextStyle.Body,
                                        color = if (canEdit) TextColor.Accent else TextColor.Muted,
                                        action = if (canEdit) "branch_settings.edit_protection" else "",
                                    ),
                                ),
                            ),
                        )
                        add(
                            row(
                                cell(
                                    TextComponent(
                                        id = "branch_settings.action_delete",
                                        text = "删除保护规则",
                                        style = TextStyle.Body,
                                        color = if (canEdit && hasProtection) TextColor.Danger else TextColor.Muted,
                                        action = if (canEdit && hasProtection) "branch_settings.delete_protection" else "",
                                    ),
                                ),
                            ),
                        )
                    }
                }
            }
        }
        return PageSchema(id = "branch_settings", columns = 12, scrollable = true, rows = rows)
    }

    /** 分支设置页壳状态：Hidden + 返回（独立全屏页）。 */
    fun shellState(): ShellState = ShellState(
        title = "分支设置",
        showBack = true,
        backAction = "shell.back",
        navBarMode = NavBarMode.Hidden,
        contentKey = "branch_settings",
    )
}

/**
 * 仓库分支设置页入口：壳 + 三分支 schema。
 * 保护规则编辑/删除 Dialog 由调用端承载（selectedBranch 由调用端持有）。
 */
@Composable
fun RepositoryBranchSettingsPageContent(
    state: RepositoryBranchSettingsUiState,
    onSelectBranch: (String) -> Unit = {},
    onEditProtection: (String) -> Unit = {},
    onDeleteProtection: (String) -> Unit = {},
    onRetry: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    val handleAction: (String) -> Unit = { action ->
        when {
            action == "shell.back" -> onBack()
            action == "branch_settings.retry" -> onRetry()
            action == "branch_settings.edit_protection" -> state.selectedBranch?.let(onEditProtection)
            action == "branch_settings.delete_protection" -> state.selectedBranch?.let(onDeleteProtection)
            action.startsWith("branch_settings.select.") -> onSelectBranch(action.removePrefix("branch_settings.select."))
        }
    }
    AppShell(state = RepositoryBranchSettingsPage.shellState(), onAction = handleAction) {
        RepositoryBranchSettingsPage.schemaFor(state, onSelectBranch).renderPage(handleAction)
    }
}