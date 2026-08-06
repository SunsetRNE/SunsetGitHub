package com.Sunset.REN.GitHub.ui.pages

import androidx.compose.runtime.Composable
import com.Sunset.REN.GitHub.ui.layout.PageSchema
import com.Sunset.REN.GitHub.ui.layout.cell
import com.Sunset.REN.GitHub.ui.layout.row
import com.Sunset.REN.GitHub.ui.render.renderPage
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
import com.Sunset.REN.GitHub.ui.shell.ShellState
import com.Sunset.REN.GitHub.ui.shell.shellNavItem

/**
 * 主页垂直切片（UI_SHELL_REDESIGN.md §7 步骤 2）。
 *
 * 页面 = 纯 schema 声明：所有 UI 组件化、模块化、只解析固定字段，
 * 坐标由 [PageSchema] 行×列网格固定，渲染判断完全由字段驱动。
 * 本文件不包含任何布局实现代码。
 */
object HomePage {

    /** 主页 schema：纯数据，可序列化（阶段 6 可由 Rust 核心直接下发）。 */
    val schema: PageSchema = PageSchema(
        id = "home",
        columns = 12,
        scrollable = true,
        rows = listOf(
            row(
                cell(
                    TextComponent(
                        id = "home.welcome",
                        text = "欢迎回来",
                        style = TextStyle.Title,
                    ),
                ),
            ),
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
            row(
                cell(SpacerComponent(id = "home.spacer.top", heightDp = 8)),
            ),
            row(
                cell(
                    FieldComponent(
                        id = "home.search",
                        value = "",
                        hint = "搜索仓库…",
                        keyboard = FieldKeyboard.Text,
                    ),
                ),
            ),
            row(
                cell(
                    SectionHeaderComponent(
                        id = "home.recent.header",
                        title = "最近仓库",
                        actionText = "查看全部",
                        action = "home.open_all",
                    ),
                ),
            ),
            row(
                cell(
                    ListComponent(
                        id = "home.recent.list",
                        items = listOf(
                            ItemComponent(
                                id = "home.recent.item.1",
                                title = "SunsetGitHub",
                                subtitle = "SunsetRNE · Kotlin",
                                icon = IconId.Code,
                                badge = "3★",
                                trailing = "2 小时前",
                                action = "repo.open.SunsetRNE/SunsetGitHub",
                            ),
                            ItemComponent(
                                id = "home.recent.item.2",
                                title = "sunset-core",
                                subtitle = "SunsetRNE · Rust",
                                icon = IconId.Cloud,
                                badge = "12★",
                                trailing = "昨天",
                                action = "repo.open.SunsetRNE/sunset-core",
                            ),
                        ),
                    ),
                ),
            ),
            row(
                cell(SpacerComponent(id = "home.spacer.mid", heightDp = 8)),
            ),
            row(
                cell(
                    StateComponent(
                        id = "home.state",
                        kind = StateKind.Loading,
                        message = "正在同步动态…",
                    ),
                ),
            ),
            row(
                cell(SpacerComponent(id = "home.spacer.bottom", heightDp = 8)),
            ),
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
        ),
    )

    /** 主页默认壳状态（垂直切片演示用）。 */
    fun shellState(): ShellState = ShellState(
        title = "SunsetGitHub",
        navBarMode = NavBarMode.Main,
        navItems = listOf(
            shellNavItem(id = "home", label = "主页", icon = IconId.Home),
            shellNavItem(id = "dashboard", label = "仓库", icon = IconId.Folder),
            shellNavItem(id = "notifications", label = "通知", icon = IconId.Bell),
            shellNavItem(id = "profile", label = "我的", icon = IconId.Person),
        ),
        selectedNavId = "home",
        contentKey = "home",
    )
}

/**
 * 主页垂直切片入口：壳 + 页面 schema。
 * 调试入口可直接挂载（如 UiDebug 页），迁移时替换为真实数据源。
 */
@Composable
fun HomePageContent(
    state: ShellState = HomePage.shellState(),
    onAction: (String) -> Unit = {},
) {
    AppShell(state = state, onAction = onAction) {
        HomePage.schema.renderPage(onAction)
    }
}