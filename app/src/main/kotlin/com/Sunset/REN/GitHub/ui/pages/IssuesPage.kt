package com.Sunset.REN.GitHub.ui.pages

import androidx.compose.runtime.Composable
import com.Sunset.REN.GitHub.domain.repo.RepositoryIssue
import com.Sunset.REN.GitHub.ui.layout.PageSchema
import com.Sunset.REN.GitHub.ui.layout.cell
import com.Sunset.REN.GitHub.ui.layout.row
import com.Sunset.REN.GitHub.ui.render.renderPage
import com.Sunset.REN.GitHub.ui.repo.RepositoryIssuesUiState
import com.Sunset.REN.GitHub.ui.repo.RepositorySection
import com.Sunset.REN.GitHub.ui.schema.ButtonComponent
import com.Sunset.REN.GitHub.ui.schema.ButtonKind
import com.Sunset.REN.GitHub.ui.schema.DropdownMenuComponent
import com.Sunset.REN.GitHub.ui.schema.IconId
import com.Sunset.REN.GitHub.ui.schema.ItemComponent
import com.Sunset.REN.GitHub.ui.schema.ListComponent
import com.Sunset.REN.GitHub.ui.schema.MenuItemComponent
import com.Sunset.REN.GitHub.ui.schema.SectionHeaderComponent
import com.Sunset.REN.GitHub.ui.schema.SkeletonComponent
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
 * 问题列表页（Issues）垂直切片（步骤 5：高频页面迁移）。
 *
 * 验证新增组件：SkeletonComponent（首载骨架）、DropdownMenuComponent（open/closed 切换）。
 * 状态 → schema 映射（渲染判断由字段驱动）：
 * - isInitialLoad → 骨架列表；error && 空 → Error+重试；空 → Empty；
 * - 列表 + 加载更多 + 行尾错误提示；canCreateIssue → 新建入口。
 */
object IssuesPage {

    fun schemaFor(
        state: RepositoryIssuesUiState,
        isStateMenuExpanded: Boolean = false,
    ): PageSchema {
        val rows = buildList {
            add(
                row(
                    cell(
                        SectionHeaderComponent(
                            id = "issues.header",
                            title = "${state.owner}/${state.repo}",
                            subtitle = "${state.issues.size} 个问题 · ${state.state}",
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        DropdownMenuComponent(
                            id = "issues.state_menu",
                            triggerIcon = IconId.Sort,
                            triggerContentDescription = "筛选问题状态",
                            items = listOf(
                                MenuItemComponent(
                                    label = "开启中",
                                    selected = state.state == RepositoryIssuesUiState.OpenState,
                                    action = "issues.state.open",
                                ),
                                MenuItemComponent(
                                    label = "已关闭",
                                    selected = state.state == RepositoryIssuesUiState.ClosedState,
                                    action = "issues.state.closed",
                                ),
                            ),
                            expanded = isStateMenuExpanded,
                            toggleAction = "issues.menu.toggle",
                            dismissAction = "issues.menu.dismiss",
                        ),
                        span = 2,
                    ),
                    cell(
                        ButtonComponent(
                            id = "issues.create",
                            text = "新建",
                            kind = ButtonKind.Primary,
                            icon = IconId.Issue,
                            enabled = state.canCreateIssue,
                            action = "issues.create",
                        ),
                        span = 10,
                    ),
                ),
            )
            add(row(cell(SpacerComponent(id = "issues.spacer.top", heightDp = 4))))

            when {
                state.isInitialLoad -> add(
                    row(
                        cell(
                            SkeletonComponent(
                                id = "issues.skeleton",
                                rows = 5,
                                compact = false,
                            ),
                        ),
                    ),
                )

                !state.errorMessage.isNullOrBlank() && state.issues.isEmpty() -> add(
                    row(
                        cell(
                            StateComponent(
                                id = "issues.error",
                                kind = StateKind.Error,
                                message = "加载问题列表失败",
                                detail = state.errorMessage,
                                retryAction = "issues.retry",
                            ),
                        ),
                    ),
                )

                state.issues.isEmpty() -> add(
                    row(
                        cell(
                            StateComponent(
                                id = "issues.empty",
                                kind = StateKind.Empty,
                                message = "没有问题",
                                detail = "当前筛选条件下没有可显示的问题。",
                            ),
                        ),
                    ),
                )

                else -> {
                    add(
                        row(
                            cell(
                                ListComponent(
                                    id = "issues.list",
                                    items = state.issues.map { issue -> issueFor(issue) },
                                ),
                            ),
                        ),
                    )
                    state.errorMessage?.takeIf { state.issues.isNotEmpty() }?.let { message ->
                        add(
                            row(
                                cell(
                                    TextComponent(
                                        id = "issues.stale_error",
                                        text = "刷新失败：$message",
                                        style = TextStyle.Meta,
                                        color = TextColor.Danger,
                                    ),
                                ),
                            ),
                        )
                    }
                    if (state.hasMore) {
                        add(
                            row(
                                cell(
                                    ButtonComponent(
                                        id = "issues.load_more",
                                        text = if (state.isLoadingMore) "加载中…" else "加载更多问题",
                                        kind = ButtonKind.Secondary,
                                        enabled = !state.isLoadingMore,
                                        icon = IconId.Cloud,
                                        action = "issues.load_more",
                                    ),
                                ),
                            ),
                        )
                    }
                }
            }
        }
        return PageSchema(
            id = "issues",
            columns = 12,
            scrollable = true,
            rows = rows,
        )
    }

    /** 问题 → 列表条目（字段命名对齐 Rust RepositoryIssue 模型）。 */
    private fun issueFor(issue: RepositoryIssue): ItemComponent {
        val isClosed = issue.state == RepositoryIssuesUiState.ClosedState
        return ItemComponent(
            id = "issues.item.${issue.number}",
            title = issue.title,
            subtitle = buildList {
                add("#${issue.number}")
                add("by ${issue.authorLogin}")
                if (issue.commentCount > 0) add("${issue.commentCount} 评论")
            }.joinToString(" · "),
            meta = issue.labels.map { it.name },
            icon = if (isClosed) IconId.Check else IconId.Issue,
            badge = if (isClosed) "已关闭" else "开启中",
            trailing = issue.createdAt?.take(10)?.takeIf { it.length == 10 },
            action = "issues.open.${issue.number}",
        )
    }

    /** 问题列表页壳状态：保持仓库上下文（RepositorySections 分段导航）。 */
    fun shellState(
        fullName: String,
        sections: List<RepositorySection>,
    ): ShellState = ShellState(
        title = fullName.ifBlank { "问题" },
        showBack = true,
        backAction = "shell.back",
        navBarMode = NavBarMode.RepositorySections,
        navItems = sections.map(RepositoryDetailPage::sectionNavItem),
        selectedNavId = RepositorySection.Issues.storageKey,
        contentKey = "issues",
    )
}

/**
 * 问题列表页垂直切片入口：壳 + 状态驱动 schema。
 * 分段导航与仓库详情共用（RepositoryDetailPage.sectionNavItem），切换无漂移。
 */
@Composable
fun IssuesPageContent(
    state: RepositoryIssuesUiState,
    sections: List<RepositorySection>,
    isStateMenuExpanded: Boolean = false,
    onOpenIssue: (Int) -> Unit = {},
    onLoadFirstPage: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    onStateSelected: (String) -> Unit = {},
    onToggleStateMenu: () -> Unit = {},
    onDismissStateMenu: () -> Unit = {},
    onCreateIssue: () -> Unit = {},
    onOpenSection: (RepositorySection) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val fullName = if (state.owner.isNotBlank() && state.repo.isNotBlank()) {
        "${state.owner}/${state.repo}"
    } else {
        ""
    }
    val handleAction: (String) -> Unit = { action ->
        when {
            action == "shell.back" -> onBack()
            action == "issues.retry" -> onLoadFirstPage()
            action == "issues.load_more" -> onLoadMore()
            action == "issues.create" -> onCreateIssue()
            action == "issues.menu.toggle" -> onToggleStateMenu()
            action == "issues.menu.dismiss" -> onDismissStateMenu()
            action == "issues.state.open" -> onStateSelected(RepositoryIssuesUiState.OpenState)
            action == "issues.state.closed" -> onStateSelected(RepositoryIssuesUiState.ClosedState)
            action.startsWith("issues.open.") -> {
                action.removePrefix("issues.open.").toIntOrNull()?.let(onOpenIssue)
            }
            action.startsWith("repo.section.") -> {
                val key = action.removePrefix("repo.section.")
                RepositorySection.fromStorageKey(key)?.let(onOpenSection)
            }
        }
    }
    AppShell(
        state = IssuesPage.shellState(fullName, sections),
        onAction = handleAction,
    ) {
        IssuesPage.schemaFor(state, isStateMenuExpanded).renderPage(handleAction)
    }
}