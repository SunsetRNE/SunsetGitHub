package com.Sunset.REN.GitHub.ui.pages

import androidx.compose.runtime.Composable
import com.Sunset.REN.GitHub.domain.repo.RepositoryRelease
import com.Sunset.REN.GitHub.domain.repo.RepositoryReleaseAsset
import com.Sunset.REN.GitHub.ui.layout.PageSchema
import com.Sunset.REN.GitHub.ui.layout.RowSchema
import com.Sunset.REN.GitHub.ui.layout.cell
import com.Sunset.REN.GitHub.ui.layout.row
import com.Sunset.REN.GitHub.ui.render.renderPage
import com.Sunset.REN.GitHub.ui.repo.RepositoryReleasesUiState
import com.Sunset.REN.GitHub.ui.schema.ButtonComponent
import com.Sunset.REN.GitHub.ui.schema.ButtonKind
import com.Sunset.REN.GitHub.ui.schema.IconId
import com.Sunset.REN.GitHub.ui.schema.ItemAction
import com.Sunset.REN.GitHub.ui.schema.ItemComponent
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
import java.util.Locale

/** 下载目标（URL + 落盘文件名），Content 构建一次供 schema 索引与路由解析共用。 */
data class DownloadTarget(val url: String, val fileName: String)

/**
 * Releases（发布列表）页面垂直切片（步骤 5：仓库分段子页）。
 *
 * 从仓库详情"全部"进入的次级页：NavBarMode.Hidden + showBack（与 Settings 同模式）。
 * 渲染结构对齐 RepositoryReleasesScreen：
 * - 状态分支：isInitialLoad → Loading；error && 空 → Error+重试；isEmpty → Empty；
 * - 列表：头卡（标题 + owner/repo · 数量 + stale 提示 + 行尾错误）→ 发布卡序列
 *   （名称/tag/徽章（草稿/Latest/预发布）/meta/正文/在 GitHub 中打开/附件下载行/源代码 zip·tar 下载行）；
 * - 分页：hasMore → 加载更多按钮（isLoadingMore 禁用+文案切换）。
 * 路由前缀：releases.retry / load_more / open.{url} / download.{index} / shell.back。
 * 注：下载行内动作用 IconId.Download，索引基于 Content 构建的 downloads 列表（schema 与路由共享）。
 */
object ReleasesPage {

    /** 徽章：草稿 / Latest / 预发布（原版 releaseBadge，本地化纯函数）。 */
    private fun badgeFor(release: RepositoryRelease): String? = when {
        release.isDraft -> "草稿"
        release.isLatest -> "Latest"
        release.isPrerelease -> "预发布"
        else -> null
    }

    /** meta：作者发布于日期（原版 releaseMeta）。 */
    private fun releaseMeta(release: RepositoryRelease): String? {
        val date = (release.publishedAt ?: release.createdAt)?.substringBefore('T')?.takeIf { it.isNotBlank() }
        val author = release.authorLogin?.takeIf { it.isNotBlank() }
        return when {
            author != null && date != null -> "$author 发布于 $date"
            date != null -> "发布于 $date"
            else -> author
        }
    }

    /** 正文：bodySummary → body 剥离 Markdown → 占位（原版 plainReleaseBody）。 */
    private fun releaseBody(release: RepositoryRelease): String =
        release.bodySummary?.takeIf { it.isNotBlank() }
            ?: release.body?.plainReleaseBody()?.takeIf { it.isNotBlank() }
            ?: "暂无发布说明"

    /** 附件 meta：大小 · 下载次数。 */
    private fun assetMeta(asset: RepositoryReleaseAsset): String =
        "${asset.sizeBytes.toFileSize()} · ${asset.downloadCount}"

    /** 构建下载清单（顺序 = schema 中下载行顺序，供 releases.download.{index} 解析）。 */
    fun buildDownloadItems(releases: List<RepositoryRelease>, repo: String): List<DownloadTarget> {
        val items = mutableListOf<DownloadTarget>()
        releases.forEach { release ->
            release.assets.forEach { asset ->
                asset.browserDownloadUrl?.takeIf { it.isNotBlank() }?.let {
                    items += DownloadTarget(it, asset.name)
                }
            }
            val base = "${repo.ifBlank { "source" }}-${release.tagName.ifBlank { "release" }}"
            release.zipballUrl?.takeIf { it.isNotBlank() }?.let { items += DownloadTarget(it, "$base.zip") }
            release.tarballUrl?.takeIf { it.isNotBlank() }?.let { items += DownloadTarget(it, "$base.tar.gz") }
        }
        return items
    }

    /** 状态 → 页面 schema。 */
    fun schemaFor(
        state: RepositoryReleasesUiState,
        downloads: List<DownloadTarget>,
    ): PageSchema {
        val rows = buildList<RowSchema> {
            when {
                state.isInitialLoad -> add(
                    row(
                        cell(
                            StateComponent(
                                id = "releases.loading",
                                kind = StateKind.Loading,
                                message = "正在加载发布…",
                            ),
                        ),
                    ),
                )

                state.errorMessage != null && state.releases.isEmpty() -> add(
                    row(
                        cell(
                            StateComponent(
                                id = "releases.error",
                                kind = StateKind.Error,
                                message = "加载发布失败",
                                detail = state.errorMessage,
                                retryAction = "releases.retry",
                            ),
                        ),
                    ),
                )

                state.isEmpty -> add(
                    row(
                        cell(
                            StateComponent(
                                id = "releases.empty",
                                kind = StateKind.Empty,
                                message = "暂无发布",
                            ),
                        ),
                    ),
                )

                else -> {
                    // —— 头卡：标题 + 上下文 + stale 提示 + 行尾错误 ——
                    val fullName = if (state.owner.isNotBlank() || state.repo.isNotBlank()) {
                        "${state.owner}/${state.repo}"
                    } else {
                        "未知仓库"
                    }
                    add(
                        row(
                            cell(
                                SectionHeaderComponent(
                                    id = "releases.header",
                                    title = "发布",
                                    subtitle = "$fullName / ${state.releases.size}",
                                ),
                            ),
                        ),
                    )
                    if (state.isShowingStaleContent) {
                        add(
                            row(
                                cell(
                                    TextComponent(
                                        id = "releases.stale",
                                        text = "正在加载发布…",
                                        style = TextStyle.Meta,
                                        color = TextColor.Secondary,
                                    ),
                                ),
                            ),
                        )
                    }
                    state.errorMessage?.takeIf { state.releases.isNotEmpty() }?.let { message ->
                        add(
                            row(
                                cell(
                                    TextComponent(
                                        id = "releases.stale_error",
                                        text = "加载发布失败：$message",
                                        style = TextStyle.Meta,
                                        color = TextColor.Danger,
                                    ),
                                ),
                            ),
                        )
                    }

                    // —— 发布卡序列 ——
                    state.releases.forEach { release ->
                        addAll(releaseRows(release, state.repo, downloads))
                    }

                    // —— 分页 ——
                    if (state.hasMore) {
                        add(
                            row(
                                cell(
                                    ButtonComponent(
                                        id = "releases.load_more",
                                        text = if (state.isLoadingMore) "正在加载更多发布…" else "加载更多发布",
                                        kind = ButtonKind.Primary,
                                        enabled = !state.isLoadingMore,
                                        action = "releases.load_more",
                                    ),
                                ),
                            ),
                        )
                    }
                }
            }
        }
        return PageSchema(
            id = "releases",
            columns = 12,
            scrollable = true,
            rows = rows,
        )
    }

    /** 单个发布 → 行序列（头/打开按钮/附件/源代码），下载行索引指向 downloads。 */
    private fun releaseRows(
        release: RepositoryRelease,
        repo: String,
        downloads: List<DownloadTarget>,
    ): List<RowSchema> {
        val tag = release.tagName.ifBlank { release.name.ifBlank { "unknown" } }
        val rows = mutableListOf<RowSchema>()

        // —— 发布头：名称 + tag + 徽章 + meta + 正文 ——
        rows += row(
            cell(
                ItemComponent(
                    id = "releases.item.$tag",
                    title = release.name.ifBlank { release.tagName },
                    subtitle = release.tagName.ifBlank { "-" },
                    badge = badgeFor(release),
                    meta = listOfNotNull(releaseMeta(release)),
                    description = releaseBody(release),
                    action = release.htmlUrl?.takeIf { it.isNotBlank() } ?: "",
                ),
            ),
        )

        // —— 在 GitHub 中打开 ——
        release.htmlUrl?.takeIf { it.isNotBlank() }?.let { url ->
            rows += row(
                cell(
                    ButtonComponent(
                        id = "releases.open.$tag",
                        text = "在 GitHub 中打开",
                        kind = ButtonKind.Secondary,
                        action = "releases.open.$url",
                    ),
                ),
            )
        }

        // —— 附件标题 + 下载行 ——
        val assetsTitle = if (release.assets.isEmpty()) "附件" else "${release.assets.size} 个附件"
        rows += row(
            cell(
                TextComponent(
                    id = "releases.assets_title.$tag",
                    text = assetsTitle,
                    style = TextStyle.Subtitle,
                    color = TextColor.Primary,
                ),
            ),
        )
        release.assets.forEach { asset ->
            val url = asset.browserDownloadUrl
            val index = url?.takeIf { it.isNotBlank() }
                ?.let { u -> downloads.indexOfFirst { it.url == u } } ?: -1
            if (index >= 0) {
                rows += row(
                    cell(
                        ItemComponent(
                            id = "releases.asset.${asset.id}",
                            title = asset.name,
                            description = assetMeta(asset),
                            actions = listOf(
                                ItemAction(
                                    id = "releases.download.asset.${asset.id}",
                                    icon = IconId.Download,
                                    contentDescription = "下载",
                                    action = "releases.download.$index",
                                ),
                            ),
                        ),
                    ),
                )
            }
        }

        // —— 源代码 zip / tar.gz 下载行 ——
        val sourceItems = listOfNotNull(
            release.zipballUrl?.takeIf { it.isNotBlank() }?.let { "下载 zip" to it },
            release.tarballUrl?.takeIf { it.isNotBlank() }?.let { "下载 tar.gz" to it },
        )
        if (sourceItems.isNotEmpty()) {
            rows += row(
                cell(
                    TextComponent(
                        id = "releases.source_title.$tag",
                        text = "源代码",
                        style = TextStyle.Subtitle,
                        color = TextColor.Primary,
                    ),
                ),
            )
            sourceItems.forEach { (title, url) ->
                val index = downloads.indexOfFirst { it.url == url }
                if (index >= 0) {
                    rows += row(
                        cell(
                            ItemComponent(
                                id = "releases.source.$tag.${title.removePrefix("下载 ")}",
                                title = title,
                                description = release.tagName,
                                actions = listOf(
                                    ItemAction(
                                        id = "releases.download.source.$tag.${title.removePrefix("下载 ")}",
                                        icon = IconId.Download,
                                        contentDescription = "下载",
                                        action = "releases.download.$index",
                                    ),
                                ),
                            ),
                        ),
                    )
                }
            }
        }

        rows += row(cell(SpacerComponent(id = "releases.spacer.$tag", heightDp = 8)))
        return rows
    }

    /** Releases 壳状态：次级页（Hidden + 返回），标题固定"发布"。 */
    fun shellState(): ShellState = ShellState(
        title = "发布",
        showBack = true,
        backAction = "shell.back",
        navBarMode = NavBarMode.Hidden,
        contentKey = "releases",
    )

    /** 剥离 Markdown 标记的正文纯文本（原版 plainReleaseBody）。 */
    private fun String.plainReleaseBody(): String =
        replace(Regex("[`*_>#]"), "").replace(Regex("\\[([^]]+)]\\([^)]+\\)"), "$1").trim()

    /** 字节 → 可读大小（B/KB/MB/GB）。 */
    private fun Long.toFileSize(): String {
        if (this <= 0) return "0 B"
        val units = listOf("B", "KB", "MB", "GB")
        var value = toDouble()
        var index = 0
        while (value >= 1024 && index < units.lastIndex) {
            value /= 1024
            index++
        }
        return if (index == 0) "${value.toLong()} ${units[index]}" else String.format(Locale.US, "%.1f %s", value, units[index])
    }
}

/**
 * Releases 页面垂直切片入口：壳 + 状态驱动 schema。
 * 下载清单构建一次，schema 与路由共享索引（releases.download.{index}）。
 */
@Composable
fun ReleasesPageContent(
    state: RepositoryReleasesUiState,
    onRetry: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    onOpenRelease: (String) -> Unit = {},
    onDownload: (url: String, fileName: String) -> Unit = { _, _ -> },
    onBack: () -> Unit = {},
) {
    val downloads = ReleasesPage.buildDownloadItems(state.releases, state.repo)
    val handleAction: (String) -> Unit = { action ->
        when {
            action == "shell.back" -> onBack()
            action == "releases.retry" -> onRetry()
            action == "releases.load_more" -> onLoadMore()
            action.startsWith("releases.open.") -> onOpenRelease(action.removePrefix("releases.open."))
            action.startsWith("releases.download.") -> {
                action.removePrefix("releases.download.").toIntOrNull()?.let { index ->
                    downloads.getOrNull(index)?.let { onDownload(it.url, it.fileName) }
                }
            }
        }
    }
    AppShell(state = ReleasesPage.shellState(), onAction = handleAction) {
        ReleasesPage.schemaFor(state, downloads).renderPage(handleAction)
    }
}
