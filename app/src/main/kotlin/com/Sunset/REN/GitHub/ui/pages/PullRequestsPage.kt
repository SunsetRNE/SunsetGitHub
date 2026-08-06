package com.Sunset.REN.GitHub.ui.pages

import androidx.compose.runtime.Composable
import com.Sunset.REN.GitHub.domain.repo.RepositoryPullRequest
import com.Sunset.REN.GitHub.ui.layout.PageSchema
import com.Sunset.REN.GitHub.ui.layout.cell
import com.Sunset.REN.GitHub.ui.layout.row
import com.Sunset.REN.GitHub.ui.render.renderPage
import com.Sunset.REN.GitHub.ui.repo.RepositoryPullRequestsUiState
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
 * 拉取请求列表页（Pull Requests）垂直切片（步骤 5：高频页面迁移）。
 *
 * 与 IssuesPage 同构，验证 DropdownMenuComponent 多态筛选（open/closed/all）。
 * 状态 → schema 映射（渲染判断由字段驱动）：
 * - isInitialLoad → 骨架列表；error && 空 → Error+重试；空 → Empty；
 * - 列表（draft 标记、合并状态徽章）+ 加载更多 + 行尾错误提示。
 */
object PullRequestsPage {

    fun schemaFor(
        state: RepositoryPullRequestsUiState,
        isStateMenuExpanded: Boolean = false,
    ): PageSchema {
        val rows = buildList {
            add(
                row(
                    cell(
                        SectionHeaderComponent(
                            id = "prs.header",
                            title = "${state.owner}/${state.repo}",
                            subtitle = "${state.pullRequests.size} 个拉取请求 · ${state.state}",
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        DropdownMenuComponent(
                            id = "prs.state_menu",
                            triggerIcon = IconId.PullRequest,
                            triggerContentDescription = "筛选拉取请求状态",
                            items = listOf(
                                MenuItemComponent(
                                    label = "开启中",
                                    selected = state.state == RepositoryPullRequestsUiState.OpenState,
                                    action = "prs.state.open",
                                ),
                                MenuItemComponent(
                                    label = "已关闭",
                                    selected = state.state == RepositoryPullRequestsUiState.ClosedState,
                                    action = "prs.state.closed",
                                ),
                                MenuItemComponent(
                                    label = "全部",
                                    selected = state.state == RepositoryPullRequestsUiState.AllState,
                                    action = "prs.state.all",
                                ),
                            ),
                            expanded = isStateMenuExpanded,
                            toggleAction = "prs.menu.toggle",
                            dismissAction = "prs.menu.dismiss",
                        ),
                        span = 3,
                    ),
                ),
            )
            add(row(cell(SpacerComponent(id = "prs.spacer.top", heightDp = 4))))

            when {
                state.isInitialLoad -> add(
                    row(
                        cell(
                            SkeletonComponent(
                                id = "prs.skeleton",
                                rows = 5,
                                compact = true,
                            ),
                        ),
                    ),
                )

                !state.errorMessage.isNullOrBlank() && state.pullRequests.isEmpty() -> add(
                    row(
                        cell(
                            StateComponent(
                                id = "prs.error",
                                kind = StateKind.Error,
                                message = "加载拉取请求失败",
                                detail = state.errorMessage,
                                retryAction = "prs.retry",
                            ),
                        ),
                    ),
                )

                state.pullRequests.isEmpty() -> add(
                    row(
                        cell(
                            StateComponent(
                                id = "prs.empty",
                                kind = StateKind.Empty,
                                message = "没有拉取请求",
                                detail = "当前筛选条件下没有可显示的拉取请求。",
                            ),
                        ),
                    ),
                )

                else -> {
                    add(
                        row(
                            cell(
                                ListComponent(
                                    id = "prs.list",
                                    items = state.pullRequests.map { pr -> prFor(pr) },
                                ),
                            ),
                        ),
                    )
                    state.errorMessage?.takeIf { state.pullRequests.isNotEmpty() }?.let { message ->
                        add(
                            row(
                                cell(
                                    TextComponent(
                                        id = "prs.stale_error",
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
                                        id = "prs.load_more",
                                        text = if (state.isLoadingMore) "加载中…" else "加载更多拉取请求",
                                        kind = ButtonKind.Secondary,
                                        enabled = !state.isLoadingMore,
                                        icon = IconId.Cloud,
                                        action = "prs.load_more",
                                    ),
                                ),
                            ),
                        )
                    }
                }
            }
        }
        return PageSchema(
            id = "pull_requests",
            columns = 12,
            scrollable = true,
            rows = rows,
        )
    }

    /** 拉取请求 → 列表条目（字段命名对齐 Rust RepositoryPullRequest 模型）。 */
    private fun prFor(pr: RepositoryPullRequest): ItemComponent {
        val stateBadge = when {
            pr.isMerged -> "已合并"
            pr.state == RepositoryPullRequestsUiState.ClosedState -> "已关闭"
            else -> "开启中"
        }
        return ItemComponent(
            id = "prs.item.${pr.number}",
            title = pr.title,
            subtitle = buildList {
                add("#${pr.number}")
                add("by ${pr.authorLogin}")
                add("${pr.baseRef} ← ${pr.headRef}")
                if (pr.commentCount > 0) add("${pr.commentCount} 评论")
            }.joinToString(" · "),
            icon = when {
                pr.isMerged -> IconId.Check
                pr.draft -> IconId.Eye
                else -> IconId.PullRequest
            },
            badge = if (pr.draft) "草稿" else stateBadge,
            trailing = (pr.mergedAt ?: pr.updatedAt ?: pr.createdAt)?.take(10)?.takeIf { it.length == 10 },
            action = "prs.open.${pr.number}",
        )
    }

    /** 拉取请求页壳状态：保持仓库上下文（RepositorySections 分段导航）。 */
    fun shellState(
        fullName: String,
        sections: List<RepositorySection>,
    ): ShellState = ShellState(
        title = fullName.ifBlank { "拉取请求" },
        showBack = true,
        backAction = "shell.back",
        navBarMode = NavBarMode.RepositorySections,
        navItems = sections.map(RepositoryDetailPage::sectionNavItem),
        selectedNavId = RepositorySection.PullRequests.storageKey,
        contentKey = "pull_requests",
    )
}

/**
 * 拉取请求页垂直切片入口：壳 + 状态驱动 schema。
 * 分段导航与仓库详情共用（RepositoryDetailPage.sectionNavItem），切换无漂移。
 */
@Composable
fun PullRequestsPageContent(
    state: RepositoryPullRequestsUiState,
    sections: List<RepositorySection>,
    isStateMenuExpanded: Boolean = false,
    onOpenPullRequest: (Int) -> Unit = {},
    onLoadFirstPage: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    onStateSelected: (String) -> Unit = {},
    onToggleStateMenu: () -> Unit = {},
    onDismissStateMenu: () -> Unit = {},
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
            action == "prs.retry" -> onLoadFirstPage()
            action == "prs.load_more" -> onLoadMore()
            action == "prs.menu.toggle" -> onToggleStateMenu()
            action == "prs.menu.dismiss" -> onDismissStateMenu()
            action == "prs.state.open" -> onStateSelected(RepositoryPullRequestsUiState.OpenState)
            action == "prs.state.closed" -> onStateSelected(RepositoryPullRequestsUiState.ClosedState)
            action == "prs.state.all" -> onStateSelected(RepositoryPullRequestsUiState.AllState)
            action.startsWith("prs.open.") -> {
                action.removePrefix("prs.open.").toIntOrNull()?.let(onOpenPullRequest)
            }
            action.startsWith("repo.section.") -> {
                val key = action.removePrefix("repo.section.")
                RepositorySection.fromStorageKey(key)?.let(onOpenSection)
            }
        }
    }
    AppShell(
        state = PullRequestsPage.shellState(fullName, sections),
        onAction = handleAction,
    ) {
        PullRequestsPage.schemaFor(state, isStateMenuExpanded).renderPage(handleAction)
    }
}