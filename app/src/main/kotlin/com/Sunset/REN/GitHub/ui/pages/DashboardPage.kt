package com.Sunset.REN.GitHub.ui.pages

import androidx.compose.runtime.Composable
import com.Sunset.REN.GitHub.domain.repo.GitHubRepository
import com.Sunset.REN.GitHub.domain.repo.RepositoryLocalState
import com.Sunset.REN.GitHub.ui.layout.PageSchema
import com.Sunset.REN.GitHub.ui.layout.cell
import com.Sunset.REN.GitHub.ui.layout.row
import com.Sunset.REN.GitHub.ui.render.renderPage
import com.Sunset.REN.GitHub.ui.repo.RepositoriesUiState
import com.Sunset.REN.GitHub.ui.schema.ButtonComponent
import com.Sunset.REN.GitHub.ui.schema.ButtonKind
import com.Sunset.REN.GitHub.ui.schema.FieldComponent
import com.Sunset.REN.GitHub.ui.schema.FieldKeyboard
import com.Sunset.REN.GitHub.ui.schema.IconId
import com.Sunset.REN.GitHub.ui.schema.ItemAction
import com.Sunset.REN.GitHub.ui.schema.ItemComponent
import com.Sunset.REN.GitHub.ui.schema.LanguageBarComponent
import com.Sunset.REN.GitHub.ui.schema.LanguageSegment
import com.Sunset.REN.GitHub.ui.schema.ListComponent
import com.Sunset.REN.GitHub.ui.schema.SpacerComponent
import com.Sunset.REN.GitHub.ui.schema.StateComponent
import com.Sunset.REN.GitHub.ui.schema.StateKind
import com.Sunset.REN.GitHub.ui.schema.TextColor
import com.Sunset.REN.GitHub.ui.schema.TextComponent
import com.Sunset.REN.GitHub.ui.schema.TextStyle
import com.Sunset.REN.GitHub.ui.shell.AppShell
import com.Sunset.REN.GitHub.ui.shell.NavBarMode
import com.Sunset.REN.GitHub.ui.shell.ShellState
import com.Sunset.REN.GitHub.ui.shell.shellNavItem

/**
 * 仓库列表页（Dashboard）垂直切片（UI_SHELL_REDESIGN.md §7 步骤 3）。
 *
 * 状态 → schema 映射（渲染判断由字段驱动）：
 * - Loading → StateComponent(Loading)
 * - SignedOut/Empty → StateComponent(Empty) + 引导按钮
 * - Error → StateComponent(Error) + 重试
 * - Content → 搜索框 + 仓库列表 + 加载更多
 *
 * 条目字段与现有 [RepositoryDashboardCard] 对齐（title/subtitle/description/meta/trailing），
 * 字段命名与 Rust `RepositorySummary` 模型对齐（阶段 6 直接映射）。
 * 本文件不包含任何布局实现代码。
 */
object DashboardPage {

    /** 状态 → 页面 schema。 */
    fun schemaFor(
        state: RepositoriesUiState,
        query: String = "",
        onQueryChange: ((String) -> Unit)? = null,
    ): PageSchema {
        val rows = buildList {
            when (state) {
                RepositoriesUiState.Loading -> {
                    add(
                        row(
                            cell(StateComponent(id = "dashboard.loading", kind = StateKind.Loading, message = "正在加载仓库列表…")),
                        ),
                    )
                }

                RepositoriesUiState.SignedOut -> {
                    add(
                        row(
                            cell(
                                StateComponent(
                                    id = "dashboard.signed_out",
                                    kind = StateKind.Empty,
                                    message = "尚未登录",
                                    detail = "请先完成 GitHub 登录后再查看仓库列表。",
                                ),
                            ),
                        ),
                    )
                    add(actionRow("dashboard.signed_out.action", "前往首页", IconId.Home, "nav.home"))
                }

                RepositoriesUiState.Empty -> {
                    add(
                        row(
                            cell(
                                StateComponent(
                                    id = "dashboard.empty",
                                    kind = StateKind.Empty,
                                    message = "暂无仓库",
                                    detail = "当前账号暂时没有可显示的仓库。",
                                ),
                            ),
                        ),
                    )
                    add(actionRow("dashboard.empty.action", "刷新", IconId.Refresh, "dashboard.refresh"))
                }

                is RepositoriesUiState.Error -> {
                    add(
                        row(
                            cell(
                                StateComponent(
                                    id = "dashboard.error",
                                    kind = StateKind.Error,
                                    message = "加载仓库失败",
                                    detail = state.message,
                                    retryAction = "dashboard.refresh",
                                ),
                            ),
                        ),
                    )
                    add(actionRow("dashboard.error.action", "重试", IconId.Refresh, "dashboard.refresh"))
                }

                is RepositoriesUiState.Content -> {
                    // 搜索行：搜索框 + 排序按钮（排序菜单为浮层组件，阶段 6 支持）
                    add(
                        row(
                            cell(
                                FieldComponent(
                                    id = "dashboard.search",
                                    value = query,
                                    hint = "搜索仓库…",
                                    onChange = onQueryChange,
                                ),
                                span = 9,
                            ),
                            cell(
                                ButtonComponent(
                                    id = "dashboard.sort",
                                    text = "排序",
                                    kind = ButtonKind.Secondary,
                                    icon = IconId.Sort,
                                    enabled = state.repositories.size > 1,
                                    action = "dashboard.sort",
                                ),
                                span = 3,
                            ),
                        ),
                    )
                    add(row(cell(SpacerComponent(id = "dashboard.spacer.top", heightDp = 4))))

                    if (state.repositories.isEmpty()) {
                        add(
                            row(
                                cell(
                                    StateComponent(
                                        id = "dashboard.empty_list",
                                        kind = StateKind.Empty,
                                        message = "没有匹配的仓库",
                                        detail = "可尝试换一个关键词，或加载更多仓库后继续筛选。",
                                    ),
                                ),
                            ),
                        )
                    } else {
                        add(
                            row(
                                cell(
                                    ListComponent(
                                        id = "dashboard.list",
                                        items = state.repositories.map { repository ->
                                            itemFor(
                                                repository = repository,
                                                localState = state.repositoryLocalStates[repository.fullName]
                                                    ?: RepositoryLocalState(),
                                            )
                                        },
                                    ),
                                ),
                            ),
                        )
                    }

                    state.loadMoreError?.let { message ->
                        add(
                            row(
                                cell(
                                    TextComponent(
                                        id = "dashboard.load_more_error",
                                        text = "加载更多失败：$message",
                                        style = TextStyle.Meta,
                                        color = TextColor.Danger,
                                    ),
                                ),
                            ),
                        )
                    }
                    if (state.canLoadMore) {
                        add(
                            row(
                                cell(
                                    ButtonComponent(
                                        id = "dashboard.load_more",
                                        text = if (state.isLoadingMore) "加载中…" else "加载更多仓库",
                                        kind = ButtonKind.Secondary,
                                        enabled = !state.isLoadingMore,
                                        icon = IconId.Cloud,
                                        action = "dashboard.load_more",
                                    ),
                                ),
                            ),
                        )
                    }
                }
            }
        }
        return PageSchema(
            id = "dashboard",
            columns = 12,
            scrollable = true,
            rows = rows,
        )
    }

    /** 仓库 → 列表条目（字段对齐现有 RepositoryDashboardCard 与 Rust RepositorySummary）。 */
    private fun itemFor(repository: GitHubRepository, localState: RepositoryLocalState): ItemComponent {
        return ItemComponent(
            id = "dashboard.item.${repository.id}",
            title = repository.name,
            subtitle = buildOwnerLine(repository),
            description = repository.description?.takeIf { it.isNotBlank() },
            meta = buildList {
                primaryLanguageName(repository)?.let(::add)
                if (repository.stargazersCount > 0) add("★ ${repository.stargazersCount}")
                if (repository.forksCount > 0) add("Fork ${repository.forksCount}")
                if (repository.openIssuesCount > 0) add("Issue ${repository.openIssuesCount}")
            },
            languageBar = LanguageBarComponent(
                id = "dashboard.lang.${repository.id}",
                segments = repository.languages.map { language ->
                    LanguageSegment(name = language.name, percentage = language.percentage.toFloat())
                },
                fallback = repository.language,
            ),
            icon = if (repository.archived) IconId.Archive else IconId.Folder,
            badge = if (localState.isPinned) "置顶" else null,
            trailing = repository.updatedAt?.take(10)?.takeIf { it.length == 10 },
            actions = listOf(
                ItemAction(
                    id = "dashboard.action.pin.${repository.fullName}",
                    icon = IconId.Pin,
                    contentDescription = if (localState.isPinned) "取消置顶" else "置顶",
                    active = localState.isPinned,
                    action = "dashboard.pin.${repository.fullName}",
                ),
                ItemAction(
                    id = "dashboard.action.star.${repository.fullName}",
                    icon = IconId.Star,
                    contentDescription = if (localState.isFavorite) "取消收藏" else "收藏",
                    active = localState.isFavorite,
                    action = "dashboard.star.${repository.fullName}",
                ),
            ),
            action = "repo.open.${repository.fullName}",
        )
    }

    private fun buildOwnerLine(repository: GitHubRepository): String = buildList {
        add(repository.ownerLogin)
        add(if (repository.isPrivate) "private" else "public")
        if (repository.archived) add("archived")
        if (repository.fork) add("fork")
        repository.defaultBranch.takeIf { it.isNotBlank() }?.let(::add)
    }.joinToString(" · ")

    private fun primaryLanguageName(repository: GitHubRepository): String? {
        return repository.languages.maxByOrNull { it.percentage }?.name?.takeIf { it.isNotBlank() }
            ?: repository.language?.takeIf { it.isNotBlank() }
    }

    private fun actionRow(
        id: String,
        text: String,
        icon: IconId,
        action: String,
    ) = row(
        cell(
            ButtonComponent(
                id = id,
                text = text,
                kind = ButtonKind.Primary,
                icon = icon,
                action = action,
            ),
            span = 6,
        ),
    )

    /** 仓库列表页默认壳状态。 */
    fun shellState(): ShellState = ShellState(
        title = "仓库",
        navBarMode = NavBarMode.Main,
        navItems = listOf(
            shellNavItem(id = "home", label = "主页", icon = IconId.Home),
            shellNavItem(id = "dashboard", label = "仓库", icon = IconId.Folder),
            shellNavItem(id = "notifications", label = "通知", icon = IconId.Bell),
            shellNavItem(id = "settings", label = "设置", icon = IconId.Settings, action = "nav.settings"),
        ),
        selectedNavId = "dashboard",
        contentKey = "dashboard",
    )
}

/**
 * 仓库列表页垂直切片入口：壳 + 状态驱动 schema。
 * 调试入口可直接挂载；替换旧壳时保留现有 Fragment 导航契约即可。
 */
@Composable
fun DashboardPageContent(
    state: RepositoriesUiState,
    onOpenRepository: (GitHubRepository) -> Unit = {},
    onTogglePinned: (String) -> Unit = {},
    onToggleFavorite: (String) -> Unit = {},
    onRefresh: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    onOpenHome: () -> Unit = {},
    query: String = "",
    onQueryChange: ((String) -> Unit)? = null,
) {
    val repositories = (state as? RepositoriesUiState.Content)?.repositories.orEmpty()
    // 同一路由同时服务于壳（顶栏/导航栏）与页面组件（schema action）
    val handleAction: (String) -> Unit = { action ->
        when {
            action == "dashboard.refresh" -> onRefresh()
            action == "dashboard.load_more" -> onLoadMore()
            action == "nav.home" -> onOpenHome()
            // 排序菜单为浮层组件（阶段 6），当前仅占位路由
            action == "dashboard.sort" -> Unit
            action.startsWith("dashboard.pin.") -> onTogglePinned(action.removePrefix("dashboard.pin."))
            action.startsWith("dashboard.star.") -> onToggleFavorite(action.removePrefix("dashboard.star."))
            action.startsWith("repo.open.") -> {
                val fullName = action.removePrefix("repo.open.")
                repositories.firstOrNull { it.fullName == fullName }?.let(onOpenRepository)
            }
        }
    }
    AppShell(state = DashboardPage.shellState(), onAction = handleAction) {
        DashboardPage.schemaFor(state, query = query, onQueryChange = onQueryChange)
            .renderPage(onAction = handleAction)
    }
}