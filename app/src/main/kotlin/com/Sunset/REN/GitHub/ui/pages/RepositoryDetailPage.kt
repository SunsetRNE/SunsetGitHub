package com.Sunset.REN.GitHub.ui.pages

import androidx.compose.runtime.Composable
import com.Sunset.REN.GitHub.domain.repo.RepositoryContentItem
import com.Sunset.REN.GitHub.ui.layout.PageSchema
import com.Sunset.REN.GitHub.ui.layout.cell
import com.Sunset.REN.GitHub.ui.layout.row
import com.Sunset.REN.GitHub.ui.render.renderPage
import com.Sunset.REN.GitHub.ui.repo.RepositoryDetailUiState
import com.Sunset.REN.GitHub.ui.repo.RepositorySection
import com.Sunset.REN.GitHub.ui.schema.IconId
import com.Sunset.REN.GitHub.ui.schema.ItemComponent
import com.Sunset.REN.GitHub.ui.schema.ListComponent
import com.Sunset.REN.GitHub.ui.schema.SpacerComponent
import com.Sunset.REN.GitHub.ui.schema.StateComponent
import com.Sunset.REN.GitHub.ui.schema.StateKind
import com.Sunset.REN.GitHub.ui.schema.TextColor
import com.Sunset.REN.GitHub.ui.schema.TextComponent
import com.Sunset.REN.GitHub.ui.schema.TextStyle
import com.Sunset.REN.GitHub.ui.shell.AppShell
import com.Sunset.REN.GitHub.ui.shell.NavBarMode
import com.Sunset.REN.GitHub.ui.shell.ShellNavItem
import com.Sunset.REN.GitHub.ui.shell.ShellState

/**
 * 仓库详情垂直切片（UI_SHELL_REDESIGN.md §7 步骤 4）。
 *
 * 核心验证点：**分段导航由壳状态驱动，不再事后修补**。
 * - [RepositorySection] → [ShellNavItem] 固定字段映射（label/icon/action），
 *   壳按 [ShellState.navBarMode] = RepositorySections 直接渲染，约束唯一；
 * - 当前分区内容渲染进壳内容区，非 Code 分区标记"待迁移"（步骤 5 逐页迁移）；
 * - 字段命名对齐 Rust `RepositoryContentItem`/`RepositorySummary` 模型。
 * 本文件不包含任何布局实现代码。
 */
object RepositoryDetailPage {

    /** 仓库详情 → 页面 schema。 */
    fun schemaFor(
        state: RepositoryDetailUiState,
        selectedSection: RepositorySection = RepositorySection.Code,
    ): PageSchema {
        val rows = buildList {
            when (state) {
                RepositoryDetailUiState.Loading -> {
                    add(
                        row(
                            cell(
                                StateComponent(
                                    id = "repo.loading",
                                    kind = StateKind.Loading,
                                    message = "正在加载仓库…",
                                ),
                            ),
                        ),
                    )
                }

                RepositoryDetailUiState.SignedOut -> {
                    add(
                        row(
                            cell(
                                StateComponent(
                                    id = "repo.signed_out",
                                    kind = StateKind.Empty,
                                    message = "尚未登录",
                                    detail = "请先完成 GitHub 登录后再查看仓库内容。",
                                ),
                            ),
                        ),
                    )
                }

                is RepositoryDetailUiState.Error -> {
                    add(
                        row(
                            cell(
                                StateComponent(
                                    id = "repo.error",
                                    kind = StateKind.Error,
                                    message = "加载仓库失败",
                                    detail = state.message,
                                    retryAction = "repo.retry_contents",
                                ),
                            ),
                        ),
                    )
                }

                is RepositoryDetailUiState.Content -> {
                    if (selectedSection != RepositorySection.Code) {
                        // 非 Code 分区：导航已由壳驱动，内容随步骤 5 逐页迁移
                        add(
                            row(
                                cell(
                                    StateComponent(
                                        id = "repo.section.${selectedSection.storageKey}",
                                        kind = StateKind.Empty,
                                        message = "「${sectionLabel(selectedSection)}」分区待迁移",
                                        detail = "分区导航已由壳状态驱动，内容将随逐页迁移接入。",
                                    ),
                                ),
                            ),
                        )
                    } else {
                        add(
                            row(
                                cell(
                                    TextComponent(
                                        id = "repo.path",
                                        text = if (state.currentPath.isBlank()) "/" else state.currentPath,
                                        style = TextStyle.Code,
                                        color = TextColor.Secondary,
                                    ),
                                ),
                            ),
                        )
                        add(row(cell(SpacerComponent(id = "repo.spacer.top", heightDp = 4))))

                        when {
                            state.contentsError != null -> add(
                                row(
                                    cell(
                                        StateComponent(
                                            id = "repo.contents_error",
                                            kind = StateKind.Error,
                                            message = "加载文件列表失败",
                                            detail = state.contentsError,
                                            retryAction = "repo.retry_contents",
                                        ),
                                    ),
                                ),
                            )

                            state.isContentsLoading && state.contents.isEmpty() -> add(
                                row(
                                    cell(
                                        StateComponent(
                                            id = "repo.contents_loading",
                                            kind = StateKind.Loading,
                                            message = "正在加载文件列表…",
                                        ),
                                    ),
                                ),
                            )

                            state.contents.isEmpty() -> add(
                                row(
                                    cell(
                                        StateComponent(
                                            id = "repo.contents_empty",
                                            kind = StateKind.Empty,
                                            message = "空目录",
                                            detail = "当前路径下没有可显示的文件。",
                                        ),
                                    ),
                                ),
                            )

                            else -> add(
                                row(
                                    cell(
                                        ListComponent(
                                            id = "repo.contents",
                                            items = state.contents.map { item -> itemFor(item) },
                                        ),
                                    ),
                                ),
                            )
                        }
                    }
                }
            }
        }
        return PageSchema(
            id = "repo.detail.${selectedSection.storageKey}",
            columns = 12,
            scrollable = true,
            rows = rows,
        )
    }

    /** 仓库内容条目 → 列表条目（字段对齐现有 RepositoryDetailScreen 与 Rust 模型）。 */
    private fun itemFor(item: RepositoryContentItem): ItemComponent = when (item) {
        is RepositoryContentItem.Directory -> ItemComponent(
            id = "repo.item.dir.${item.path}",
            title = item.name,
            subtitle = item.path,
            icon = IconId.Folder,
            action = "repo.dir.${item.path}",
        )

        is RepositoryContentItem.File -> ItemComponent(
            id = "repo.item.file.${item.path}",
            title = item.name,
            subtitle = formatSize(item.sizeBytes),
            icon = IconId.File,
            trailing = item.sizeBytes.takeIf { it > 0 }?.let(::formatSize),
            action = "repo.file.${item.path}",
        )

        is RepositoryContentItem.Unsupported -> ItemComponent(
            id = "repo.item.unsupported.${item.path}",
            title = item.name,
            subtitle = item.reason,
            icon = IconId.Warning,
        )
    }

    /** 分区 → 壳导航条目（固定字段映射，与 RepositorySection 顺序解耦）。 */
    fun sectionNavItem(section: RepositorySection): ShellNavItem = ShellNavItem(
        id = section.storageKey,
        label = sectionLabel(section),
        icon = sectionIcon(section),
        action = "repo.section.${section.storageKey}",
    )

    /** 仓库详情壳状态：RepositorySections 模式由分区列表驱动。 */
    fun shellState(
        fullName: String,
        sections: List<RepositorySection>,
        selectedSection: RepositorySection,
    ): ShellState = ShellState(
        title = fullName.ifBlank { "仓库详情" },
        showBack = true,
        backAction = "shell.back",
        navBarMode = NavBarMode.RepositorySections,
        navItems = sections.map(::sectionNavItem),
        selectedNavId = selectedSection.storageKey,
        contentKey = "repo.${selectedSection.storageKey}",
    )

    private fun sectionIcon(section: RepositorySection): IconId = when (section) {
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

    private fun sectionLabel(section: RepositorySection): String = when (section) {
        RepositorySection.Code -> "代码"
        RepositorySection.Issues -> "问题"
        RepositorySection.PullRequests -> "拉取请求"
        RepositorySection.Actions -> "动作"
        RepositorySection.Projects -> "项目"
        RepositorySection.SecurityQuality -> "安全"
        RepositorySection.Insights -> "洞察"
        RepositorySection.Wiki -> "Wiki"
        RepositorySection.Agents -> "代理"
        RepositorySection.Settings -> "设置"
        RepositorySection.Fork -> "复刻"
        RepositorySection.More -> "更多"
    }

    private fun formatSize(bytes: Long): String = when {
        bytes >= 1024L * 1024 * 1024 -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
        bytes >= 1024L * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
        bytes >= 1024L -> String.format("%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}

/**
 * 仓库详情垂直切片入口：壳（RepositorySections 导航）+ 分区内容 schema。
 * 分段导航切换不再打补丁——壳按 ShellState 重建导航栏，内容区按分区重建 schema。
 */
@Composable
fun RepositoryDetailPageContent(
    state: RepositoryDetailUiState,
    sections: List<RepositorySection>,
    selectedSection: RepositorySection = RepositorySection.Code,
    onOpenDirectory: (String) -> Unit = {},
    onOpenFile: (String) -> Unit = {},
    onRetryContents: () -> Unit = {},
    onSectionSelected: (RepositorySection) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val fullName = (state as? RepositoryDetailUiState.Content)?.repository?.fullName.orEmpty()
    val handleAction: (String) -> Unit = { action ->
        when {
            action == "shell.back" -> onBack()
            action == "repo.retry_contents" -> onRetryContents()
            action.startsWith("repo.section.") -> {
                val key = action.removePrefix("repo.section.")
                RepositorySection.fromStorageKey(key)?.let(onSectionSelected)
            }
            action.startsWith("repo.dir.") -> onOpenDirectory(action.removePrefix("repo.dir."))
            action.startsWith("repo.file.") -> onOpenFile(action.removePrefix("repo.file."))
        }
    }
    AppShell(
        state = RepositoryDetailPage.shellState(fullName, sections, selectedSection),
        onAction = handleAction,
    ) {
        RepositoryDetailPage.schemaFor(state, selectedSection).renderPage(handleAction)
    }
}