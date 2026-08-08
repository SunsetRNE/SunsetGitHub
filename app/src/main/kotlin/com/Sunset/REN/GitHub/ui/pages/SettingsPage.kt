package com.Sunset.REN.GitHub.ui.pages

import androidx.compose.runtime.Composable
import com.Sunset.REN.GitHub.ui.layout.PageSchema
import com.Sunset.REN.GitHub.ui.layout.cell
import com.Sunset.REN.GitHub.ui.layout.row
import com.Sunset.REN.GitHub.ui.render.renderPage
import com.Sunset.REN.GitHub.ui.repo.RepositorySection
import com.Sunset.REN.GitHub.ui.schema.ButtonComponent
import com.Sunset.REN.GitHub.ui.schema.ButtonKind
import com.Sunset.REN.GitHub.ui.schema.IconId
import com.Sunset.REN.GitHub.ui.schema.ItemAction
import com.Sunset.REN.GitHub.ui.schema.ItemComponent
import com.Sunset.REN.GitHub.ui.schema.ListComponent
import com.Sunset.REN.GitHub.ui.schema.SectionHeaderComponent
import com.Sunset.REN.GitHub.ui.schema.SpacerComponent
import com.Sunset.REN.GitHub.ui.schema.SwitchComponent
import com.Sunset.REN.GitHub.ui.schema.TextColor
import com.Sunset.REN.GitHub.ui.schema.TextComponent
import com.Sunset.REN.GitHub.ui.schema.TextStyle
import com.Sunset.REN.GitHub.ui.shell.AppShell
import com.Sunset.REN.GitHub.ui.shell.NavBarMode
import com.Sunset.REN.GitHub.ui.shell.ShellState
import com.Sunset.REN.GitHub.ui.shell.shellNavItem

/**
 * 设置页（Settings）垂直切片（步骤 5：首个次级页面）。
 *
 * 验证壳能力组合：showBack=true + NavBarMode.Hidden（设置页无底导航）。
 * 与原 SettingsScreen 渲染结构对齐（4 分区）：
 * - 账号：打开账号页面；
 * - 外观：浮动导航/代码编辑器/UI 渲染诊断开关（SwitchComponent 受控）+ 应用日志 + 配色说明；
 * - 仓库导航：分区排序列表（↑↓ 行内动作）；
 * - 工作区操作：步骤 1/2 + 同步/终端按钮 + 命令说明。
 * 文案与 strings_formal.xml settings_* 值一致（纯函数，无 Context 依赖）。
 */
object SettingsPage {

    /** 分区 → 中文标签（对齐 strings_formal.xml repository_section_*）。 */
    fun sectionLabel(section: RepositorySection): String = when (section) {
        RepositorySection.Code -> "代码"
        RepositorySection.Issues -> "议题"
        RepositorySection.PullRequests -> "拉取请求"
        RepositorySection.Actions -> "工作流"
        RepositorySection.Projects -> "项目"
        RepositorySection.SecurityQuality -> "安全"
        RepositorySection.Insights -> "洞察"
        RepositorySection.Wiki -> "Wiki"
        RepositorySection.Agents -> "代理"
        RepositorySection.Settings -> "设置"
        RepositorySection.Fork -> "Fork"
        RepositorySection.More -> "更多"
    }

    /** 分区 → 图标（与 RepositorySection.navigationIconResId 对齐）。 */
    fun sectionIcon(section: RepositorySection): IconId = when (section) {
        RepositorySection.Code -> IconId.Code
        RepositorySection.Issues -> IconId.Issue
        RepositorySection.PullRequests -> IconId.PullRequest
        RepositorySection.Actions -> IconId.Refresh
        RepositorySection.Projects -> IconId.Home
        RepositorySection.SecurityQuality -> IconId.Eye
        RepositorySection.Insights -> IconId.Home
        RepositorySection.Wiki -> IconId.File
        RepositorySection.Agents -> IconId.Person
        RepositorySection.Settings -> IconId.Settings
        RepositorySection.Fork -> IconId.Fork
        RepositorySection.More -> IconId.Sort
    }

    /** 设置页 schema（无状态分支——设置项固定存在）。 */
    fun schemaFor(
        floatingNavigationEnabled: Boolean,
        soraEditorEnabled: Boolean,
        uiDebugOverlayEnabled: Boolean,
        showUiDebugOverlaySetting: Boolean,
        repositorySectionOrder: List<RepositorySection>,
    ): PageSchema {
        val rows = buildList<com.Sunset.REN.GitHub.ui.layout.RowSchema> {
            add(
                row(
                    cell(
                        SectionHeaderComponent(
                            id = "settings.header",
                            title = "设置",
                            subtitle = "应用偏好与仓库导航",
                        ),
                    ),
                ),
            )

            // —— 账号 ——
            add(
                row(
                    cell(
                        SectionHeaderComponent(
                            id = "settings.account_header",
                            title = "账号",
                            subtitle = "管理登录账号与访问凭据。",
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        ButtonComponent(
                            id = "settings.open_account",
                            text = "打开账号页面",
                            kind = ButtonKind.Primary,
                            icon = IconId.Person,
                            action = "settings.open_account",
                        ),
                    ),
                ),
            )

            add(row(cell(SpacerComponent(id = "settings.spacer.theme", heightDp = 8))))

            // —— 外观 ——
            add(
                row(
                    cell(
                        SectionHeaderComponent(
                            id = "settings.theme_header",
                            title = "外观",
                            subtitle = "调整界面主题和导航显示方式。",
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        SwitchComponent(
                            id = "settings.switch.floating_nav",
                            title = "浮动导航栏",
                            description = "使用浮动样式显示底部导航栏。",
                            checked = floatingNavigationEnabled,
                            action = "settings.toggle.floating_nav",
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        SwitchComponent(
                            id = "settings.switch.sora_editor",
                            title = "代码编辑器",
                            description = "使用 Sora Editor 预览和编辑代码文件。",
                            checked = soraEditorEnabled,
                            action = "settings.toggle.sora_editor",
                        ),
                    ),
                ),
            )
            if (showUiDebugOverlaySetting) {
                add(
                    row(
                        cell(
                            SwitchComponent(
                                id = "settings.switch.ui_debug_overlay",
                                title = "UI 渲染诊断",
                                description = "显示当前页面、Fragment、屏幕参数与系统栏 inset，仅 Debug 构建可用。",
                                checked = uiDebugOverlayEnabled,
                                action = "settings.toggle.ui_debug_overlay",
                            ),
                        ),
                    ),
                )
                add(
                    row(
                        cell(
                            ButtonComponent(
                                id = "settings.open_app_log",
                                text = "查看应用日志",
                                kind = ButtonKind.Secondary,
                                action = "settings.open_app_log",
                            ),
                        ),
                    ),
                )
                add(
                    row(
                        cell(
                            ButtonComponent(
                                id = "settings.open_rust_core",
                                text = "Rust 核心自检",
                                kind = ButtonKind.Secondary,
                                action = "settings.open_rust_core",
                            ),
                        ),
                    ),
                )
            }
            add(
                row(
                    cell(
                        TextComponent(
                            id = "settings.color_theme_title",
                            text = "配色主题",
                            style = TextStyle.Body,
                            color = TextColor.Primary,
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        TextComponent(
                            id = "settings.color_theme_value",
                            text = "黑白高对比",
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
                            id = "settings.color_theme_boundary",
                            text = "显示卡片边框",
                            style = TextStyle.Meta,
                            color = TextColor.Muted,
                        ),
                    ),
                ),
            )

            add(row(cell(SpacerComponent(id = "settings.spacer.sections", heightDp = 8))))

            // —— 仓库导航 ——
            add(
                row(
                    cell(
                        SectionHeaderComponent(
                            id = "settings.sections_header",
                            title = "仓库导航",
                            subtitle = "配置仓库详情页底部导航项目。",
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        ListComponent(
                            id = "settings.sections_list",
                            items = repositorySectionOrder.mapIndexed { index, section ->
                                sectionOrderFor(section, index, repositorySectionOrder.size)
                            },
                        ),
                    ),
                ),
            )

            add(row(cell(SpacerComponent(id = "settings.spacer.workspace", heightDp = 8))))

            // —— 工作区操作 ——
            add(
                row(
                    cell(
                        SectionHeaderComponent(
                            id = "settings.workspace_header",
                            title = "工作区操作",
                            subtitle = "先创建或导入工作区，再进入终端检查文件、绑定远端、执行 dry-run 或同步。",
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        TextComponent(
                            id = "settings.workspace_step_1",
                            text = "1. 创建或导入工作区文件",
                            style = TextStyle.Body,
                            color = TextColor.Secondary,
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        ButtonComponent(
                            id = "settings.open_sync",
                            text = "打开工作区同步",
                            kind = ButtonKind.Secondary,
                            icon = IconId.Cloud,
                            action = "settings.open_sync",
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        TextComponent(
                            id = "settings.workspace_step_2",
                            text = "2. 检查文件、远端绑定与同步计划",
                            style = TextStyle.Body,
                            color = TextColor.Secondary,
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        ButtonComponent(
                            id = "settings.open_terminal",
                            text = "打开工作区终端",
                            kind = ButtonKind.Primary,
                            icon = IconId.Terminal,
                            action = "settings.open_terminal",
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        TextComponent(
                            id = "settings.workspace_terminal_desc",
                            text = "终端支持 help、status、cat、remote、dry-run、sync 等工作区命令。",
                            style = TextStyle.Meta,
                            color = TextColor.Muted,
                        ),
                    ),
                ),
            )
        }
        return PageSchema(
            id = "settings",
            columns = 12,
            scrollable = true,
            rows = rows,
        )
    }

    /** 仓库分区排序行：图标 + 名称 + ↑↓ 行内动作。 */
    private fun sectionOrderFor(section: RepositorySection, index: Int, total: Int): ItemComponent {
        return ItemComponent(
            id = "settings.section.${section.storageKey}",
            title = sectionLabel(section),
            icon = sectionIcon(section),
            actions = listOf(
                ItemAction(
                    id = "settings.section.up.${section.storageKey}",
                    icon = IconId.ArrowUp,
                    contentDescription = "上移分区",
                    active = index > 0,
                    action = "settings.section.up.${section.storageKey}",
                ),
                ItemAction(
                    id = "settings.section.down.${section.storageKey}",
                    icon = IconId.ArrowDown,
                    contentDescription = "下移分区",
                    active = index < total - 1,
                    action = "settings.section.down.${section.storageKey}",
                ),
            ),
        )
    }

    /** 设置页壳状态：次级页面（返回 + 无底导航）。 */
    fun shellState(): ShellState = ShellState(
        title = "设置",
        navBarMode = NavBarMode.Main,
        navItems = listOf(
            shellNavItem(id = "home", label = "主页", icon = IconId.Home),
            shellNavItem(id = "dashboard", label = "仓库", icon = IconId.Folder),
            shellNavItem(id = "notifications", label = "通知", icon = IconId.Bell),
            shellNavItem(id = "settings", label = "设置", icon = IconId.Settings, action = "nav.settings"),
        ),
        selectedNavId = "settings",
        contentKey = "settings",
    )
}

/**
 * 设置页垂直切片入口：壳 + schema。
 * 首次验证 showBack + NavBarMode.Hidden 组合（次级页面无底导航）。
 */
@Composable
fun SettingsPageContent(
    floatingNavigationEnabled: Boolean = false,
    soraEditorEnabled: Boolean = false,
    uiDebugOverlayEnabled: Boolean = false,
    showUiDebugOverlaySetting: Boolean = false,
    repositorySectionOrder: List<RepositorySection> = emptyList(),
    onFloatingNavigationChange: (Boolean) -> Unit = {},
    onSoraEditorChange: (Boolean) -> Unit = {},
    onUiDebugOverlayChange: (Boolean) -> Unit = {},
    onRepositorySectionOrderChange: (List<RepositorySection>) -> Unit = {},
    onOpenAccountPage: () -> Unit = {},
    onOpenWorkspaceSync: () -> Unit = {},
    onOpenWorkspaceTerminal: () -> Unit = {},
    onOpenAppLog: () -> Unit = {},
    onOpenRustCore: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    val handleAction: (String) -> Unit = { action ->
        when {
            action == "shell.back" -> onBack()
            action == "settings.open_account" -> onOpenAccountPage()
            action == "settings.open_sync" -> onOpenWorkspaceSync()
            action == "settings.open_terminal" -> onOpenWorkspaceTerminal()
            action == "settings.open_app_log" -> onOpenAppLog()
            action == "settings.open_rust_core" -> onOpenRustCore()
            action == "settings.toggle.floating_nav" -> onFloatingNavigationChange(!floatingNavigationEnabled)
            action == "settings.toggle.sora_editor" -> onSoraEditorChange(!soraEditorEnabled)
            action == "settings.toggle.ui_debug_overlay" -> onUiDebugOverlayChange(!uiDebugOverlayEnabled)
            action.startsWith("settings.section.up.") -> {
                val key = action.removePrefix("settings.section.up.")
                val index = repositorySectionOrder.indexOfFirst { it.storageKey == key }
                if (index > 0) {
                    onRepositorySectionOrderChange(
                        repositorySectionOrder.move(index, index - 1),
                    )
                }
            }
            action.startsWith("settings.section.down.") -> {
                val key = action.removePrefix("settings.section.down.")
                val index = repositorySectionOrder.indexOfFirst { it.storageKey == key }
                if (index in 0 until repositorySectionOrder.lastIndex) {
                    onRepositorySectionOrderChange(
                        repositorySectionOrder.move(index, index + 1),
                    )
                }
            }
        }
    }
    AppShell(state = SettingsPage.shellState(), onAction = handleAction) {
        SettingsPage.schemaFor(
            floatingNavigationEnabled = floatingNavigationEnabled,
            soraEditorEnabled = soraEditorEnabled,
            uiDebugOverlayEnabled = uiDebugOverlayEnabled,
            showUiDebugOverlaySetting = showUiDebugOverlaySetting,
            repositorySectionOrder = repositorySectionOrder,
        ).renderPage(handleAction)
    }
}

private fun <T> List<T>.move(fromIndex: Int, toIndex: Int): List<T> {
    if (fromIndex !in indices || toIndex !in indices || fromIndex == toIndex) return this
    return toMutableList().apply {
        val item = removeAt(fromIndex)
        add(toIndex, item)
    }
}