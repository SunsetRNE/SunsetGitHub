package com.Sunset.REN.GitHub.ui.pages

import androidx.compose.runtime.Composable
import com.Sunset.REN.GitHub.domain.repo.RepositoryActionRun
import com.Sunset.REN.GitHub.ui.layout.PageSchema
import com.Sunset.REN.GitHub.ui.layout.RowSchema
import com.Sunset.REN.GitHub.ui.layout.cell
import com.Sunset.REN.GitHub.ui.layout.row
import com.Sunset.REN.GitHub.ui.render.renderPage
import com.Sunset.REN.GitHub.ui.repo.RepositoryActionsUiState
import com.Sunset.REN.GitHub.ui.repo.RepositorySection
import com.Sunset.REN.GitHub.ui.schema.ButtonComponent
import com.Sunset.REN.GitHub.ui.schema.ButtonKind
import com.Sunset.REN.GitHub.ui.schema.DropdownMenuComponent
import com.Sunset.REN.GitHub.ui.schema.IconId
import com.Sunset.REN.GitHub.ui.schema.ItemComponent
import com.Sunset.REN.GitHub.ui.schema.MenuItemComponent
import com.Sunset.REN.GitHub.ui.schema.StateComponent
import com.Sunset.REN.GitHub.ui.schema.StateKind
import com.Sunset.REN.GitHub.ui.schema.TextColor
import com.Sunset.REN.GitHub.ui.schema.TextComponent
import com.Sunset.REN.GitHub.ui.schema.TextStyle
import com.Sunset.REN.GitHub.ui.shell.AppShell
import com.Sunset.REN.GitHub.ui.shell.NavBarMode
import com.Sunset.REN.GitHub.ui.shell.ShellMenuItem
import com.Sunset.REN.GitHub.ui.shell.ShellState

/**
 * Actions（仓库工作流运行列表）页面垂直切片（步骤 5：仓库分段子页）。
 *
 * 渲染结构对齐 RepositoryActionsScreen：
 * - 状态筛选：DropdownMenuComponent（全部/排队中/运行中/已完成，expanded 受控）；
 * - 状态分支：isInitialLoad && !stale → Loading；error && 空 → Error+重试；
 *   unavailable && 空 → Error + 打开 GitHub；isEmpty → Empty；
 * - 列表：stale 提示 + 行尾错误 + Run 卡（名称/meta（event·状态·分支·sha7）/创建时间/
 *   状态徽章按结论着色 Success/Danger/Accent）+ 加载更多；
 * - 壳菜单：工作流筛选入口（调用端承载原 WorkflowDialog，组件库不引入 Dialog）。
 * 路由前缀：actions.status.* / menu.toggle|dismiss / retry / load_more / open_github /
 * open_run.{id} / workflows / repo.section.* / shell.back。
 */
object ActionsPage {

    /** 运行状态 → 文案（原版 localizeStatus，10 态纯函数）。 */
    fun localizeStatus(status: String): String = when (status.lowercase()) {
        "success" -> "成功"
        "failure" -> "失败"
        "cancelled" -> "已取消"
        "timed_out" -> "已超时"
        "in_progress" -> "运行中"
        "queued" -> "排队中"
        "waiting" -> "等待中"
        "requested" -> "已请求"
        "completed" -> "已完成"
        else -> status.ifBlank { "未知" }
    }

    /** 状态 → 徽章色（原版 actionRunStatusColor）。 */
    private fun statusColor(status: String): TextColor = when (status.lowercase()) {
        "success", "completed" -> TextColor.Success
        "failure", "cancelled", "timed_out" -> TextColor.Danger
        else -> TextColor.Accent
    }

    /** 筛选值 → 文案。 */
    private fun statusLabel(status: String?): String = when (status) {
        RepositoryActionsUiState.StatusQueued -> "排队中"
        RepositoryActionsUiState.StatusInProgress -> "运行中"
        RepositoryActionsUiState.StatusCompleted -> "已完成"
        else -> "全部"
    }

    /** run meta：event · 状态 · 分支 · sha7（原版 actionRunMeta）。 */
    private fun runMeta(run: RepositoryActionRun): String = listOfNotNull(
        run.event.takeIf { it.isNotBlank() }?.replace('_', ' '),
        localizeStatus(run.displayState),
        run.headBranch?.takeIf { it.isNotBlank() },
        run.headSha?.takeIf { it.isNotBlank() }?.take(7),
    ).joinToString(" · ")

    /** 创建时间：T → 空格，去 Z（原版 createdAt 格式化）。 */
    private fun formatCreatedAt(raw: String?): String? =
        raw?.takeIf { it.isNotBlank() }?.replace("T", " ")?.removeSuffix("Z")

    /** 状态 → 页面 schema。 */
    fun schemaFor(
        state: RepositoryActionsUiState,
        menuExpanded: Boolean = false,
    ): PageSchema {
        val rows = buildList<RowSchema> {
            // —— 状态筛选（DropdownMenu 受控） ——
            add(
                row(
                    cell(
                        DropdownMenuComponent(
                            id = "actions.filter",
                            triggerIcon = IconId.Sort,
                            triggerContentDescription = "状态筛选：${statusLabel(state.status)}",
                            expanded = menuExpanded,
                            toggleAction = "actions.menu.toggle",
                            dismissAction = "actions.menu.dismiss",
                            items = listOf(
                                MenuItemComponent(
                                    label = "全部",
                                    selected = state.status == RepositoryActionsUiState.StatusAll,
                                    action = "actions.status.all",
                                ),
                                MenuItemComponent(
                                    label = "排队中",
                                    selected = state.status == RepositoryActionsUiState.StatusQueued,
                                    action = "actions.status.queued",
                                ),
                                MenuItemComponent(
                                    label = "运行中",
                                    selected = state.status == RepositoryActionsUiState.StatusInProgress,
                                    action = "actions.status.in_progress",
                                ),
                                MenuItemComponent(
                                    label = "已完成",
                                    selected = state.status == RepositoryActionsUiState.StatusCompleted,
                                    action = "actions.status.completed",
                                ),
                            ),
                        ),
                    ),
                ),
            )

            when {
                state.isInitialLoad && !state.isShowingStaleContent -> add(
                    row(
                        cell(
                            StateComponent(
                                id = "actions.loading",
                                kind = StateKind.Loading,
                                message = "正在加载运行记录…",
                            ),
                        ),
                    ),
                )

                state.errorMessage != null && state.workflowRuns.isEmpty() && state.workflows.isEmpty() -> add(
                    row(
                        cell(
                            StateComponent(
                                id = "actions.error",
                                kind = StateKind.Error,
                                message = "加载运行记录失败",
                                detail = state.errorMessage,
                                retryAction = "actions.retry",
                            ),
                        ),
                    ),
                )

                state.unavailableMessage != null && state.workflowRuns.isEmpty() -> {
                    add(
                        row(
                            cell(
                                StateComponent(
                                    id = "actions.unavailable",
                                    kind = StateKind.Error,
                                    message = "Actions 不可用",
                                    detail = state.unavailableMessage,
                                ),
                            ),
                        ),
                    )
                    state.actionsHtmlUrl?.let {
                        add(
                            row(
                                cell(
                                    ButtonComponent(
                                        id = "actions.open_github",
                                        text = "在 GitHub 中打开",
                                        kind = ButtonKind.Secondary,
                                        action = "actions.open_github",
                                    ),
                                ),
                            ),
                        )
                    }
                }

                state.isEmpty -> add(
                    row(
                        cell(
                            StateComponent(
                                id = "actions.empty",
                                kind = StateKind.Empty,
                                message = "暂无运行记录",
                            ),
                        ),
                    ),
                )

                else -> {
                    // —— stale 提示 + 行尾错误 ——
                    if (state.isShowingStaleContent) {
                        add(
                            row(
                                cell(
                                    TextComponent(
                                        id = "actions.stale",
                                        text = "正在加载运行记录…",
                                        style = TextStyle.Meta,
                                        color = TextColor.Secondary,
                                    ),
                                ),
                            ),
                        )
                    }
                    state.errorMessage?.takeIf { state.workflowRuns.isNotEmpty() }?.let { message ->
                        add(
                            row(
                                cell(
                                    TextComponent(
                                        id = "actions.stale_error",
                                        text = message,
                                        style = TextStyle.Meta,
                                        color = TextColor.Danger,
                                    ),
                                ),
                            ),
                        )
                    }

                    // —— Run 卡列表 ——
                    state.workflowRuns.forEach { run ->
                        add(
                            row(
                                cell(
                                    runFor(run),
                                ),
                            ),
                        )
                    }

                    // —— 加载更多 ——
                    if (state.hasMoreRuns) {
                        add(
                            row(
                                cell(
                                    ButtonComponent(
                                        id = "actions.load_more",
                                        text = if (state.isLoadingMore) "正在加载运行记录…" else "加载更多运行记录",
                                        kind = ButtonKind.Primary,
                                        enabled = !state.isLoadingMore,
                                        action = "actions.load_more",
                                    ),
                                ),
                            ),
                        )
                    }
                }
            }
        }
        return PageSchema(
            id = "actions",
            columns = 12,
            scrollable = true,
            rows = rows,
        )
    }

    /** run → 列表条目（状态徽章按结论着色，整卡点击打开详情）。 */
    private fun runFor(run: RepositoryActionRun): ItemComponent {
        return ItemComponent(
            id = "actions.run.${run.id}",
            title = run.name,
            badge = localizeStatus(run.displayState),
            badgeColor = statusColor(run.displayState),
            description = runMeta(run),
            meta = listOfNotNull(formatCreatedAt(run.createdAt)),
            action = "actions.open_run.${run.id}",
        )
    }

    /** Actions 壳状态：RepositorySections + 工作流筛选壳菜单。 */
    fun shellState(
        sections: List<RepositorySection>,
        title: String = "Actions",
    ): ShellState = ShellState(
        title = title,
        showBack = true,
        backAction = "shell.back",
        menuItems = listOf(
            ShellMenuItem(
                id = "actions.workflows",
                icon = IconId.Sort,
                action = "actions.workflows",
            ),
        ),
        navBarMode = NavBarMode.RepositorySections,
        navItems = sections.map(RepositoryDetailPage::sectionNavItem),
        selectedNavId = RepositorySection.Actions.storageKey,
        contentKey = "actions",
    )
}

/**
 * Actions 页面垂直切片入口：壳 + 状态驱动 schema。
 * 工作流筛选/分发对话框由调用端承载（menuExpanded 为页面内状态筛选的受控展开）。
 */
@Composable
fun ActionsPageContent(
    state: RepositoryActionsUiState,
    sections: List<RepositorySection>,
    menuExpanded: Boolean = false,
    onMenuToggle: () -> Unit = {},
    onMenuDismiss: () -> Unit = {},
    onStatusSelected: (String?) -> Unit = {},
    onRetry: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    onOpenActions: () -> Unit = {},
    onOpenRun: (Long) -> Unit = {},
    onOpenWorkflows: () -> Unit = {},
    onOpenSection: (RepositorySection) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val handleAction: (String) -> Unit = { action ->
        when {
            action == "shell.back" -> onBack()
            action == "actions.menu.toggle" -> onMenuToggle()
            action == "actions.menu.dismiss" -> onMenuDismiss()
            action == "actions.retry" -> onRetry()
            action == "actions.load_more" -> onLoadMore()
            action == "actions.open_github" -> onOpenActions()
            action == "actions.workflows" -> onOpenWorkflows()
            action.startsWith("actions.status.") -> {
                val key = action.removePrefix("actions.status.")
                val status = when (key) {
                    "all" -> RepositoryActionsUiState.StatusAll
                    "queued" -> RepositoryActionsUiState.StatusQueued
                    "in_progress" -> RepositoryActionsUiState.StatusInProgress
                    "completed" -> RepositoryActionsUiState.StatusCompleted
                    else -> null
                }
                onStatusSelected(status)
            }
            action.startsWith("actions.open_run.") -> {
                action.removePrefix("actions.open_run.").toLongOrNull()?.let(onOpenRun)
            }
            action.startsWith("repo.section.") -> {
                val key = action.removePrefix("repo.section.")
                RepositorySection.fromStorageKey(key)?.let(onOpenSection)
            }
        }
    }
    AppShell(state = ActionsPage.shellState(sections), onAction = handleAction) {
        ActionsPage.schemaFor(state, menuExpanded).renderPage(handleAction)
    }
}