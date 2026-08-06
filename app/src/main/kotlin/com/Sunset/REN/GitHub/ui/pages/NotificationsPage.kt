package com.Sunset.REN.GitHub.ui.pages

import androidx.compose.runtime.Composable
import com.Sunset.REN.GitHub.domain.notification.GitHubNotification
import com.Sunset.REN.GitHub.ui.layout.PageSchema
import com.Sunset.REN.GitHub.ui.layout.cell
import com.Sunset.REN.GitHub.ui.layout.row
import com.Sunset.REN.GitHub.ui.render.renderPage
import com.Sunset.REN.GitHub.ui.notifications.NotificationsUiState
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
import com.Sunset.REN.GitHub.ui.shell.shellNavItem

/**
 * 通知页（Notifications）垂直切片（步骤 5：高频页面迁移）。
 *
 * 与原 NotificationsFragment 渲染结构对齐：
 * - all/unread 筛选（DropdownMenuComponent，与 Issues/PRs 状态筛选同一方法论）；
 * - 状态分支：error && 空 → Error+重试；空 → Empty；isInitialLoad → 骨架；
 *   列表（未读铃铛标记、类型徽章、原因·时间 meta）+ 加载更多。
 * 本地化映射为纯函数（subjectType 6 类 + reason 12 种），与 R.string.notification_* 值一致。
 */
object NotificationsPage {

    /** subjectType → 中文（对齐 strings_formal.xml notification_type_*）。 */
    fun localizeSubjectType(type: String): String = when (type.lowercase()) {
        "issue" -> "Issue"
        "pullrequest" -> "Pull Request"
        "release" -> "Release"
        "commit" -> "提交"
        "discussion" -> "讨论"
        "checksuite" -> "检查套件"
        else -> type.ifBlank { "通知" }
    }

    /** reason → 中文（对齐 strings_formal.xml notification_reason_*）。 */
    fun localizeReason(reason: String): String = when (reason.lowercase()) {
        "assign" -> "被分配"
        "author" -> "你创建了相关内容"
        "comment" -> "有新评论"
        "invitation" -> "收到邀请"
        "manual" -> "手动订阅"
        "mention" -> "有人提及你"
        "review_requested" -> "请求你审查"
        "security_alert" -> "安全警报"
        "state_change" -> "状态变更"
        "subscribed" -> "已订阅"
        "team_mention" -> "团队被提及"
        "ci_activity" -> "CI 状态更新"
        else -> reason.ifBlank { "未知原因" }
    }

    /** 状态 → 页面 schema（渲染判断由字段驱动）。 */
    fun schemaFor(
        state: NotificationsUiState,
        isFilterMenuExpanded: Boolean = false,
    ): PageSchema {
        val rows = buildList {
            add(
                row(
                    cell(
                        SectionHeaderComponent(
                            id = "notifications.header",
                            title = "通知",
                            subtitle = "${state.notifications.size} 条 · ${if (state.all) "全部" else "未读"}",
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        DropdownMenuComponent(
                            id = "notifications.filter_menu",
                            triggerIcon = IconId.Bell,
                            triggerContentDescription = "筛选通知范围",
                            items = listOf(
                                MenuItemComponent(
                                    label = "未读",
                                    selected = !state.all,
                                    action = "notifications.filter.unread",
                                ),
                                MenuItemComponent(
                                    label = "全部",
                                    selected = state.all,
                                    action = "notifications.filter.all",
                                ),
                            ),
                            expanded = isFilterMenuExpanded,
                            toggleAction = "notifications.menu.toggle",
                            dismissAction = "notifications.menu.dismiss",
                        ),
                        span = 3,
                    ),
                ),
            )
            add(row(cell(SpacerComponent(id = "notifications.spacer.top", heightDp = 4))))

            when {
                state.isInitialLoad -> add(
                    row(
                        cell(
                            SkeletonComponent(
                                id = "notifications.skeleton",
                                rows = 5,
                                compact = true,
                            ),
                        ),
                    ),
                )

                state.errorMessage != null && state.notifications.isEmpty() -> add(
                    row(
                        cell(
                            StateComponent(
                                id = "notifications.error",
                                kind = StateKind.Error,
                                message = "加载通知失败",
                                detail = state.errorMessage,
                                retryAction = "notifications.retry",
                            ),
                        ),
                    ),
                )

                state.isEmpty -> add(
                    row(
                        cell(
                            StateComponent(
                                id = "notifications.empty",
                                kind = StateKind.Empty,
                                message = if (state.all) "暂无通知" else "暂无未读通知",
                                detail = "新的通知会出现在这里。",
                            ),
                        ),
                    ),
                )

                else -> {
                    add(
                        row(
                            cell(
                                ListComponent(
                                    id = "notifications.list",
                                    items = state.notifications.map { notificationFor(it) },
                                ),
                            ),
                        ),
                    )
                    state.errorMessage?.takeIf { state.notifications.isNotEmpty() }?.let { message ->
                        add(
                            row(
                                cell(
                                    TextComponent(
                                        id = "notifications.stale_error",
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
                                        id = "notifications.load_more",
                                        text = if (state.isLoadingMore) "加载中…" else "加载更多通知",
                                        kind = ButtonKind.Secondary,
                                        enabled = !state.isLoadingMore,
                                        icon = IconId.Cloud,
                                        action = "notifications.load_more",
                                    ),
                                ),
                            ),
                        )
                    }
                }
            }
        }
        return PageSchema(
            id = "notifications",
            columns = 12,
            scrollable = true,
            rows = rows,
        )
    }

    /** 通知 → 列表条目（字段命名对齐 Rust GitHubNotification 模型）。 */
    private fun notificationFor(notification: GitHubNotification): ItemComponent {
        return ItemComponent(
            id = "notifications.item.${notification.id}",
            title = notification.subjectTitle,
            subtitle = notification.repositoryFullName,
            badge = localizeSubjectType(notification.subjectType),
            meta = listOfNotNull(
                localizeReason(notification.reason).takeIf { it.isNotBlank() },
                notification.updatedAt?.takeIf { it.isNotBlank() },
            ),
            icon = if (notification.unread) IconId.Bell else null,
            trailing = notification.updatedAt?.take(10)?.takeIf { it.length == 10 },
            action = "notifications.open.${notification.id}",
        )
    }

    /** 通知页默认壳状态：主 Tab 导航（选中"通知"）。 */
    fun shellState(): ShellState = ShellState(
        title = "通知",
        navBarMode = NavBarMode.Main,
        navItems = listOf(
            shellNavItem(id = "home", label = "主页", icon = IconId.Home),
            shellNavItem(id = "dashboard", label = "仓库", icon = IconId.Folder),
            shellNavItem(id = "notifications", label = "通知", icon = IconId.Bell),
            shellNavItem(id = "profile", label = "我的", icon = IconId.Person),
        ),
        selectedNavId = "notifications",
        contentKey = "notifications",
    )
}

/**
 * 通知页垂直切片入口：壳 + 状态驱动 schema。
 * 同一路由同时服务壳（主导航切换）与页面组件（筛选/重试/加载更多/打开通知）。
 */
@Composable
fun NotificationsPageContent(
    state: NotificationsUiState,
    isFilterMenuExpanded: Boolean = false,
    onFilterSelected: (Boolean) -> Unit = {},
    onRetry: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    onOpenNotification: (GitHubNotification) -> Unit = {},
    onOpenHome: () -> Unit = {},
    onOpenDashboard: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onToggleFilterMenu: () -> Unit = {},
    onDismissFilterMenu: () -> Unit = {},
) {
    val handleAction: (String) -> Unit = { action ->
        when {
            action == "notifications.retry" -> onRetry()
            action == "notifications.load_more" -> onLoadMore()
            action == "notifications.menu.toggle" -> onToggleFilterMenu()
            action == "notifications.menu.dismiss" -> onDismissFilterMenu()
            action == "notifications.filter.unread" -> onFilterSelected(false)
            action == "notifications.filter.all" -> onFilterSelected(true)
            action == "nav.home" -> onOpenHome()
            action == "nav.dashboard" -> onOpenDashboard()
            action == "nav.profile" -> onOpenProfile()
            action.startsWith("notifications.open.") -> {
                val id = action.removePrefix("notifications.open.")
                state.notifications.firstOrNull { it.id == id }?.let(onOpenNotification)
            }
        }
    }
    AppShell(state = NotificationsPage.shellState(), onAction = handleAction) {
        NotificationsPage.schemaFor(state, isFilterMenuExpanded).renderPage(handleAction)
    }
}
