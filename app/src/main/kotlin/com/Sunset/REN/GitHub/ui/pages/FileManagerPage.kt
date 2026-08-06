package com.Sunset.REN.GitHub.ui.pages

import androidx.compose.runtime.Composable
import com.Sunset.REN.GitHub.domain.filemanager.FileEntryType
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerEntry
import com.Sunset.REN.GitHub.ui.layout.PageSchema
import com.Sunset.REN.GitHub.ui.layout.RowSchema
import com.Sunset.REN.GitHub.ui.layout.cell
import com.Sunset.REN.GitHub.ui.layout.row
import com.Sunset.REN.GitHub.ui.render.renderPage
import com.Sunset.REN.GitHub.ui.filemanager.LocalFileManagerUiState
import com.Sunset.REN.GitHub.ui.schema.ButtonComponent
import com.Sunset.REN.GitHub.ui.schema.ButtonKind
import com.Sunset.REN.GitHub.ui.schema.IconId
import com.Sunset.REN.GitHub.ui.schema.ItemComponent
import com.Sunset.REN.GitHub.ui.schema.SectionHeaderComponent
import com.Sunset.REN.GitHub.ui.schema.StateComponent
import com.Sunset.REN.GitHub.ui.schema.StateKind
import com.Sunset.REN.GitHub.ui.schema.TextColor
import com.Sunset.REN.GitHub.ui.schema.TextComponent
import com.Sunset.REN.GitHub.ui.schema.TextStyle
import com.Sunset.REN.GitHub.ui.shell.AppShell
import com.Sunset.REN.GitHub.ui.shell.NavBarMode
import com.Sunset.REN.GitHub.ui.shell.ShellMenuItem
import com.Sunset.REN.GitHub.ui.shell.ShellState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 本地文件管理器页面垂直切片（步骤 5：文件管理链路）。
 *
 * 聚焦 LocalFileManagerScreen 的单窗格核心形态（FileManagerPane）：
 * - 路径头：可读路径 + 文件夹/文件计数（readableDualPanePath/paneCountText 纯函数化）；
 * - 导航行：后退/前进/上级/刷新（enabled 由 canGoBack/canGoForward/canGoUp 驱动）
 *   + 新建目录/新建文件；
 * - 状态分支：isLoading && 空 → Loading；error → Error+重试；空 → Empty；列表；
 * - 条目行：类型图标（Parent→Back/Directory→Folder/Code→Code/Image→Image/Archive→Archive…）
 *   + 名称 + 权限·大小 meta + 修改时间 + 选中勾标记 + 整卡点击（打开/预览由调用端解析 id）。
 * 壳：Hidden + showBack（独立全屏页），菜单：收藏切换。
 * 路由前缀：filemanager.open.{id} / back / forward / up / refresh / retry /
 * create_dir / create_file / toggle_favorite / shell.back。
 * 注：双窗格、批量选择/传输/压缩、回收站、Root 等由调用端承载（Dialog 不引入组件库）。
 */
object FileManagerPage {

    /** 路径 → 可读显示（原版 readableDualPanePath）。 */
    private fun readablePath(path: String): String {
        if (path.isBlank()) return "本地文件"
        val normalized = path.trimEnd('/').ifBlank { path }
        return when {
            normalized == "/storage/emulated/0" -> normalized
            normalized.startsWith("/storage/emulated/0/") -> "..." + normalized.removePrefix("/storage/emulated/0")
            normalized.length > 34 -> "..." + normalized.takeLast(31)
            else -> normalized
        }
    }

    /** 计数文案：文件夹/文件（原版 paneCountText）。 */
    private fun countText(entries: List<FileManagerEntry>): String {
        val realEntries = entries.filterNot { it.type == FileEntryType.Parent }
        val folderCount = realEntries.count { it.type == FileEntryType.Directory }
        val fileCount = realEntries.size - folderCount
        return "文件夹: $folderCount  文件: $fileCount"
    }

    /** 类型 → 图标（原版 MtEntryIcon 语义映射）。 */
    private fun entryTypeIcon(type: FileEntryType): IconId = when (type) {
        FileEntryType.Parent -> IconId.Back
        FileEntryType.Directory -> IconId.Folder
        FileEntryType.Text, FileEntryType.Markdown, FileEntryType.Apk, FileEntryType.Binary, FileEntryType.Unknown -> IconId.File
        FileEntryType.Code -> IconId.Code
        FileEntryType.Image -> IconId.Image
        FileEntryType.Archive -> IconId.Archive
    }

    /** 类型 → 文案（原版 typeLabel）。 */
    private fun typeLabel(type: FileEntryType): String = when (type) {
        FileEntryType.Parent -> "上级"
        FileEntryType.Directory -> "目录"
        FileEntryType.Text -> "文本"
        FileEntryType.Markdown -> "Markdown"
        FileEntryType.Code -> "代码"
        FileEntryType.Image -> "图片"
        FileEntryType.Archive -> "压缩包"
        FileEntryType.Apk -> "APK"
        FileEntryType.Binary -> "二进制"
        FileEntryType.Unknown -> "文件"
    }

    /** 权限·大小 meta（原版 entryPermissionMeta，不含权限开关简化）。 */
    private fun entryMeta(entry: FileManagerEntry): String {
        if (entry.type == FileEntryType.Parent) return "返回上级目录"
        val size = when (entry.type) {
            FileEntryType.Directory -> null
            else -> entry.sizeBytes?.let(::formatSize)
        }
        val permission = buildString {
            append(if (entry.type == FileEntryType.Directory) 'd' else '-')
            append(if (entry.capabilities.canRead) 'r' else '-')
            append(if (entry.capabilities.canWrite) 'w' else '-')
            append("------")
        }
        return if (size != null) "$permission $size" else permission
    }

    /** 修改时间（原版 formatModifiedTime 短年格式）。 */
    private fun formatModifiedTime(millis: Long?): String {
        if (millis == null || millis <= 0L) return ""
        return SimpleDateFormat("yy-MM-dd HH:mm", Locale.getDefault()).format(Date(millis))
    }

    /** 大小（原版 formatSizeMt：B/K/M/G）。 */
    private fun formatSize(sizeBytes: Long): String {
        if (sizeBytes < 1024L) return "${sizeBytes}B"
        val kb = sizeBytes / 1024.0
        if (kb < 1024.0) return String.format(Locale.US, "%.2fK", kb)
        val mb = kb / 1024.0
        if (mb < 1024.0) return String.format(Locale.US, "%.2fM", mb)
        return String.format(Locale.US, "%.2fG", mb / 1024.0)
    }

    /** 状态 → 页面 schema。 */
    fun schemaFor(
        state: LocalFileManagerUiState,
        selectedEntryIds: Set<String> = emptySet(),
    ): PageSchema {
        val rows = buildList<RowSchema> {
            // —— 路径头 ——
            add(
                row(
                    cell(
                        SectionHeaderComponent(
                            id = "filemanager.header",
                            title = readablePath(state.currentPath.ifBlank { state.locationHint }),
                            subtitle = countText(state.entries),
                        ),
                    ),
                ),
            )

            // —— 导航行：后退/前进/上级/刷新 ——
            add(
                row(
                    cell(
                        ButtonComponent(
                            id = "filemanager.back",
                            text = "后退",
                            kind = ButtonKind.Secondary,
                            enabled = state.canGoBack,
                            action = "filemanager.back",
                        ),
                        span = 3,
                    ),
                    cell(
                        ButtonComponent(
                            id = "filemanager.forward",
                            text = "前进",
                            kind = ButtonKind.Secondary,
                            enabled = state.canGoForward,
                            action = "filemanager.forward",
                        ),
                        span = 3,
                    ),
                    cell(
                        ButtonComponent(
                            id = "filemanager.up",
                            text = "上级",
                            kind = ButtonKind.Secondary,
                            enabled = state.canGoUp,
                            action = "filemanager.up",
                        ),
                        span = 3,
                    ),
                    cell(
                        ButtonComponent(
                            id = "filemanager.refresh",
                            text = "刷新",
                            kind = ButtonKind.Secondary,
                            action = "filemanager.refresh",
                        ),
                        span = 3,
                    ),
                ),
            )

            // —— 操作行：新建目录/新建文件 ——
            add(
                row(
                    cell(
                        ButtonComponent(
                            id = "filemanager.create_dir",
                            text = "新建目录",
                            kind = ButtonKind.Secondary,
                            action = "filemanager.create_dir",
                        ),
                        span = 6,
                    ),
                    cell(
                        ButtonComponent(
                            id = "filemanager.create_file",
                            text = "新建文件",
                            kind = ButtonKind.Secondary,
                            action = "filemanager.create_file",
                        ),
                        span = 6,
                    ),
                ),
            )

            // —— 状态分支 ——
            when {
                state.isLoading && state.entries.isEmpty() -> add(
                    row(
                        cell(
                            StateComponent(
                                id = "filemanager.loading",
                                kind = StateKind.Loading,
                                message = "加载中…",
                            ),
                        ),
                    ),
                )

                state.errorMessage?.isNotBlank() == true -> add(
                    row(
                        cell(
                            StateComponent(
                                id = "filemanager.error",
                                kind = StateKind.Error,
                                message = "加载目录失败",
                                detail = state.errorMessage,
                                retryAction = "filemanager.retry",
                            ),
                        ),
                    ),
                )

                state.entries.isEmpty() -> add(
                    row(
                        cell(
                            StateComponent(
                                id = "filemanager.empty",
                                kind = StateKind.Empty,
                                message = "目录为空",
                            ),
                        ),
                    ),
                )

                else -> {
                    // —— 条目列表 ——
                    state.entries.forEach { entry ->
                        add(
                            row(
                                cell(
                                    entryFor(entry, entry.id in selectedEntryIds),
                                ),
                            ),
                        )
                    }
                }
            }
        }
        return PageSchema(
            id = "filemanager",
            columns = 12,
            scrollable = true,
            rows = rows,
        )
    }

    /** 条目 → 列表行（类型图标 + 名称 + 权限·大小 + 修改时间 + 选中勾 + 整卡点击）。 */
    private fun entryFor(entry: FileManagerEntry, selected: Boolean): ItemComponent {
        return ItemComponent(
            id = "filemanager.entry.${entry.id}",
            title = entry.name,
            subtitle = typeLabel(entry.type),
            description = entryMeta(entry),
            meta = listOfNotNull(formatModifiedTime(entry.modifiedAtMillis)),
            icon = entryTypeIcon(entry.type),
            trailing = if (selected) "✓" else null,
            action = "filemanager.open.${entry.id}",
        )
    }

    /** 文件管理器壳状态：Hidden + 返回 + 收藏切换菜单。 */
    fun shellState(title: String): ShellState = ShellState(
        title = title,
        showBack = true,
        backAction = "shell.back",
        menuItems = listOf(
            ShellMenuItem(
                id = "filemanager.favorite",
                icon = IconId.Pin,
                action = "filemanager.toggle_favorite",
            ),
        ),
        navBarMode = NavBarMode.Hidden,
        contentKey = "filemanager",
    )
}

/**
 * 文件管理器垂直切片入口：壳 + 状态驱动 schema。
 * 条目 id → 实体解析、双窗格/批量操作等由调用端承载。
 */
@Composable
fun FileManagerPageContent(
    state: LocalFileManagerUiState,
    selectedEntryIds: Set<String> = emptySet(),
    title: String = "本地文件管理",
    onOpenEntry: (String) -> Unit = {},
    onBack: () -> Unit = {},
    onForward: () -> Unit = {},
    onUp: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onRetry: () -> Unit = {},
    onCreateDirectory: () -> Unit = {},
    onCreateFile: () -> Unit = {},
    onToggleFavorite: () -> Unit = {},
    onShellBack: () -> Unit = {},
) {
    val handleAction: (String) -> Unit = { action ->
        when {
            action == "shell.back" -> onShellBack()
            action == "filemanager.back" -> onBack()
            action == "filemanager.forward" -> onForward()
            action == "filemanager.up" -> onUp()
            action == "filemanager.refresh" -> onRefresh()
            action == "filemanager.retry" -> onRetry()
            action == "filemanager.create_dir" -> onCreateDirectory()
            action == "filemanager.create_file" -> onCreateFile()
            action == "filemanager.toggle_favorite" -> onToggleFavorite()
            action.startsWith("filemanager.open.") -> onOpenEntry(action.removePrefix("filemanager.open."))
        }
    }
    AppShell(state = FileManagerPage.shellState(title), onAction = handleAction) {
        FileManagerPage.schemaFor(state, selectedEntryIds).renderPage(handleAction)
    }
}