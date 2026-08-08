package com.Sunset.REN.GitHub.ui.pages

import androidx.compose.runtime.Composable
import com.Sunset.REN.GitHub.domain.repo.GitHubRepository
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
import com.Sunset.REN.GitHub.ui.schema.ItemComponent
import com.Sunset.REN.GitHub.ui.schema.ListComponent
import com.Sunset.REN.GitHub.ui.schema.SectionHeaderComponent
import com.Sunset.REN.GitHub.ui.schema.SpacerComponent
import com.Sunset.REN.GitHub.ui.schema.StateComponent
import com.Sunset.REN.GitHub.ui.schema.StateKind
import com.Sunset.REN.GitHub.ui.schema.TextColor
import com.Sunset.REN.GitHub.ui.schema.TextComponent
import com.Sunset.REN.GitHub.ui.schema.TextStyle
import com.Sunset.REN.GitHub.ui.shell.AppShell
import com.Sunset.REN.GitHub.ui.shell.NavBarMode
import com.Sunset.REN.GitHub.ui.shell.ShellMenuItem
import com.Sunset.REN.GitHub.ui.shell.ShellState
import com.Sunset.REN.GitHub.ui.shell.shellNavItem

/**
 * 主页垂直切片（UI_SHELL_REDESIGN.md §7 步骤 2）。
 *
 * 页面 = 纯 schema 声明：所有 UI 组件化、模块化、只解析固定字段，
 * 坐标由 [PageSchema] 行×列网格固定，渲染判断完全由字段驱动。
 * 本文件不包含任何布局实现代码。
 *
 * 状态 → schema 映射（2026-08：从静态占位重绘为动态页，接 Dashboard 真实数据）：
 * - Loading → StateComponent(Loading)
 * - SignedOut → StateComponent(Empty) + 登录引导
 * - Empty → StateComponent(Empty)
 * - Error → StateComponent(Error) + 重试（home.refresh）
 * - Content → 最近仓库列表（真实数据，最多 5 条）+ 查看全部
 */
object HomePage {

    /** 状态 → 页面 schema。 */
    fun schemaFor(
        state: RepositoriesUiState,
        query: String = "",
        onQueryChange: ((String) -> Unit)? = null,
    ): PageSchema {
        val rows = buildList {
            // —— 欢迎区 ——
            add(
                row(
                    cell(
                        TextComponent(
                            id = "home.welcome",
                            text = "欢迎回来",
                            style = TextStyle.Title,
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        TextComponent(
                            id = "home.subtitle",
                            text = "查看仓库、动态与通知的聚合入口",
                            style = TextStyle.Meta,
                            color = TextColor.Muted,
                        ),
                    ),
                ),
            )
            add(row(cell(SpacerComponent(id = "home.spacer.top", heightDp = 8))))

            // —— 搜索框（纯展示入口，搜索能力在仓库/搜索页） ——
            add(
                row(
                    cell(
                        FieldComponent(
                            id = "home.search",
                            value = query,
                            hint = "搜索仓库…",
                            keyboard = FieldKeyboard.Text,
                            onChange = onQueryChange,
                        ),
                    ),
                ),
            )

            // —— 最近仓库区（状态驱动） ——
            when (state) {
                RepositoriesUiState.Loading -> {
                    add(
                        row(
                            cell(
                                StateComponent(
                                    id = "home.state",
                                    kind = StateKind.Loading,
                                    message = "正在加载仓库…",
                                ),
                            ),
                        ),
                    )
                }

                RepositoriesUiState.SignedOut -> {
                    add(
                        row(
                            cell(
                                StateComponent(
                                    id = "home.state",
                                    kind = StateKind.Empty,
                                    message = "尚未登录",
                                    detail = "登录后可查看最近仓库与动态。",
                                ),
                            ),
                        ),
                    )
                }

                RepositoriesUiState.Empty -> {
                    add(
                        row(
                            cell(
                                StateComponent(
                                    id = "home.state",
                                    kind = StateKind.Empty,
                                    message = "暂无仓库",
                                    detail = "新建或导入仓库后会显示在这里。",
                                ),
                            ),
                        ),
                    )
                }

                is RepositoriesUiState.Error -> {
                    add(
                        row(
                            cell(
                                StateComponent(
                                    id = "home.state",
                                    kind = StateKind.Error,
                                    message = "加载仓库失败",
                                    detail = state.message,
                                    retryAction = "home.refresh",
                                ),
                            ),
                        ),
                    )
                }

                is RepositoriesUiState.Content -> {
                    if (state.repositories.isEmpty()) {
                        add(
                            row(
                                cell(
                                    StateComponent(
                                        id = "home.state",
                                        kind = StateKind.Empty,
                                        message = "暂无仓库",
                                        detail = "新建或导入仓库后会显示在这里。",
                                    ),
                                ),
                            ),
                        )
                    } else {
                        add(
                            row(
                                cell(
                                    SectionHeaderComponent(
                                        id = "home.recent.header",
                                        title = "最近仓库",
                                        actionText = "查看全部",
                                        action = "nav.dashboard",
                                    ),
                                ),
                            ),
                        )
                        add(
                            row(
                                cell(
                                    ListComponent(
                                        id = "home.recent.list",
                                        items = state.repositories.take(MaxRecentRepositories).map(::itemFor),
                                    ),
                                ),
                            ),
                        )
                    }
                }
            }

            add(row(cell(SpacerComponent(id = "home.spacer.bottom", heightDp = 8))))

            // —— 快捷操作 ——
            add(
                row(
                    cell(
                        ButtonComponent(
                            id = "home.create",
                            text = "新建仓库",
                            kind = ButtonKind.Primary,
                            icon = IconId.Star,
                            action = "home.create_repository",
                        ),
                        span = 6,
                    ),
                    cell(
                        ButtonComponent(
                            id = "home.refresh",
                            text = "刷新",
                            kind = ButtonKind.Secondary,
                            icon = IconId.Refresh,
                            action = "home.refresh",
                        ),
                        span = 6,
                    ),
                ),
            )
        }
        return PageSchema(
            id = "home",
            columns = 12,
            scrollable = true,
            rows = rows,
        )
    }

    /** 仓库 → 首页列表条目（字段对齐 DashboardPage.itemFor 与 Rust RepositorySummary）。 */
    private fun itemFor(repository: GitHubRepository): ItemComponent {
        return ItemComponent(
            id = "home.recent.item.${repository.id}",
            title = repository.name,
            subtitle = buildOwnerLine(repository),
            description = repository.description?.takeIf { it.isNotBlank() },
            meta = buildList {
                repository.language?.let(::add)
                if (repository.stargazersCount > 0) add("★ ${repository.stargazersCount}")
                if (repository.forksCount > 0) add("Fork ${repository.forksCount}")
            },
            icon = IconId.Code,
            action = "repo.open.${repository.fullName}",
        )
    }

    private fun buildOwnerLine(repository: GitHubRepository): String =
        buildString {
            append(repository.ownerLogin)
            repository.language?.let { append(" · $it") }
            if (repository.isPrivate) append(" · 私有")
        }

    private const val MaxRecentRepositories = 5

    /** 主页壳状态（主 Tab，选中 home；右上角头像入口 → 个人主页）。 */
    fun shellState(avatarUrl: String? = null): ShellState = ShellState(
        title = "SunsetGitHub",
        navBarMode = NavBarMode.Main,
        menuItems = listOf(
            ShellMenuItem(id = "home.avatar", icon = IconId.Person, action = "nav.profile", avatarUrl = avatarUrl),
        ),
        navItems = listOf(
            shellNavItem(id = "home", label = "主页", icon = IconId.Home),
            shellNavItem(id = "dashboard", label = "仓库", icon = IconId.Folder),
            shellNavItem(id = "notifications", label = "通知", icon = IconId.Bell),
            shellNavItem(id = "settings", label = "设置", icon = IconId.Settings, action = "nav.settings"),
        ),
        selectedNavId = "home",
        contentKey = "home",
    )
}

/**
 * 主页垂直切片入口：壳 + 页面 schema。
 * 调试入口可直接挂载（如 UiDebug 页）；正式路由由 ShellHost 以真实状态调用 schemaFor。
 */
@Composable
fun HomePageContent(
    state: ShellState = HomePage.shellState(),
    onAction: (String) -> Unit = {},
) {
    AppShell(state = state, onAction = onAction) {
        HomePage.schemaFor(RepositoriesUiState.Loading).renderPage(onAction)
    }
}