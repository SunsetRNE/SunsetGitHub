package com.Sunset.REN.GitHub.ui.pages

import androidx.compose.runtime.Composable
import com.Sunset.REN.GitHub.data.github.html.RepositoryHtmlSectionStatus
import com.Sunset.REN.GitHub.ui.layout.PageSchema
import com.Sunset.REN.GitHub.ui.layout.RowSchema
import com.Sunset.REN.GitHub.ui.layout.cell
import com.Sunset.REN.GitHub.ui.layout.row
import com.Sunset.REN.GitHub.ui.render.renderPage
import com.Sunset.REN.GitHub.ui.repo.RepositorySection
import com.Sunset.REN.GitHub.ui.repo.RepositorySectionNativeStubUiState
import com.Sunset.REN.GitHub.ui.schema.ButtonComponent
import com.Sunset.REN.GitHub.ui.schema.ButtonKind
import com.Sunset.REN.GitHub.ui.schema.ItemComponent
import com.Sunset.REN.GitHub.ui.schema.SectionHeaderComponent
import com.Sunset.REN.GitHub.ui.schema.StateComponent
import com.Sunset.REN.GitHub.ui.schema.StateKind
import com.Sunset.REN.GitHub.ui.schema.TextColor
import com.Sunset.REN.GitHub.ui.schema.TextComponent
import com.Sunset.REN.GitHub.ui.schema.TextStyle
import com.Sunset.REN.GitHub.ui.shell.AppShell
import com.Sunset.REN.GitHub.ui.shell.NavBarMode
import com.Sunset.REN.GitHub.ui.shell.ShellState

/**
 * HTML 摘要分区页（步骤 5：仓库分段子页族）。
 *
 * 通用垂直切片，服务六个共用 RepositorySectionNativeStubScreen 的分区：
 * Wiki / Projects / Insights / Agents / SecurityQuality / 仓库 Settings。
 * 渲染结构对齐 RepositorySectionNativeStubScreen：
 * - 头卡：分区标题 + 仓库标签 + 状态行（error 时 Danger 色）+ 描述
 *   （加载中/错误/摘要描述/兜底描述）+ 按钮行（error 时重试 + 打开 GitHub，否则仅打开）；
 * - 加载：isLoading && summary == null → Loading；
 * - 摘要卡：标题 + 状态徽章 + 源 URL；
 * - 指标卡：指标数据列表（空 → 占位）；
 * - 提示卡：提示与建议（空 → 占位）；
 * - 调试卡：debug 输出（monospace，点击复制）。
 * 路由前缀：stub.retry / open_github / copy_debug / repo.section.{key} / shell.back。
 */
object RepositorySectionStubPage {

    /** 状态 → 文案（原版 displayText，本地化纯函数）。 */
    private fun statusDisplay(status: RepositoryHtmlSectionStatus): String = when (status) {
        RepositoryHtmlSectionStatus.Available -> "可打开"
        RepositoryHtmlSectionStatus.Empty -> "该栏目当前没有可显示的内容"
        RepositoryHtmlSectionStatus.Disabled -> "未启用"
        RepositoryHtmlSectionStatus.AccessDenied -> "无权访问"
        RepositoryHtmlSectionStatus.ParsePartial -> "仅获取到部分状态"
        RepositoryHtmlSectionStatus.ParseFailed -> "状态解析失败"
    }

    /** debug 输出纯函数（原版 RepositorySectionNativeStubUiState.debugOutput）。 */
    fun debugOutput(state: RepositorySectionNativeStubUiState, initialSectionUrl: String): String {
        state.summary?.let { summary ->
            return buildString {
                appendLine("debug.repository=${state.owner}/${state.repo}")
                appendLine("debug.section=${state.sectionKey}")
                appendLine("debug.status=${summary.status}")
                appendLine("debug.sourceUrl=${summary.sourceUrl}")
                appendLine("debug.metrics.count=${summary.metrics.size}")
                summary.metrics.forEachIndexed { index, metric ->
                    appendLine("debug.metrics[$index]=${metric.label}: ${metric.value}")
                }
                appendLine("debug.notices.count=${summary.notices.size}")
                summary.notices.forEachIndexed { index, notice -> appendLine("debug.notices[$index]=$notice") }
                appendLine("debug.actions.count=${summary.actions.size}")
                summary.actions.forEachIndexed { index, action -> appendLine("debug.actions[$index]=$action") }
            }
        }
        if (state.errorMessage == null && state.sectionStatusCode == null && state.htmlPreview == null) return ""
        return buildString {
            appendLine("debug.repository=${state.owner}/${state.repo}")
            appendLine("debug.section=${state.sectionKey}")
            appendLine("debug.statusCode=${state.sectionStatusCode ?: "n/a"}")
            appendLine("debug.sourceUrl=${state.sourceUrl ?: initialSectionUrl}")
            appendLine("debug.htmlPreview=${state.htmlPreview ?: "n/a"}")
        }
    }

    /** 状态 → 页面 schema（顺序渲染与 RepositorySectionNativeStubScreen 一致）。 */
    fun schemaFor(
        state: RepositorySectionNativeStubUiState,
        sectionTitle: String,
        fallbackDescription: String,
        repositoryLabel: String,
        initialSectionUrl: String,
    ): PageSchema {
        val effectiveUrl = state.sourceUrl?.takeIf { it.isNotBlank() } ?: initialSectionUrl
        val statusText = state.summary?.status?.let(::statusDisplay)
        val description = when {
            state.isLoading -> "正在加载页面状态…"
            !state.errorMessage.isNullOrBlank() -> state.errorMessage
            state.summary != null -> state.summary.description
            else -> fallbackDescription
        }
        val debugText = debugOutput(state, initialSectionUrl)

        val rows = buildList<RowSchema> {
            // —— 头卡 ——
            add(
                row(
                    cell(
                        SectionHeaderComponent(
                            id = "stub.header",
                            title = sectionTitle,
                            subtitle = repositoryLabel,
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        TextComponent(
                            id = "stub.status",
                            text = "状态：${statusText ?: "仓库栏目"}",
                            style = TextStyle.Meta,
                            color = if (state.errorMessage != null) TextColor.Danger else TextColor.Accent,
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        TextComponent(
                            id = "stub.description",
                            text = description,
                            style = TextStyle.Body,
                            color = TextColor.Secondary,
                        ),
                    ),
                ),
            )
            // —— 按钮行：error 时 重试+打开 GitHub，否则仅打开 GitHub ——
            if (state.errorMessage != null) {
                add(
                    row(
                        cell(
                            ButtonComponent(
                                id = "stub.retry",
                                text = "重试",
                                kind = ButtonKind.Primary,
                                action = "stub.retry",
                            ),
                            span = 6,
                        ),
                        cell(
                            ButtonComponent(
                                id = "stub.open_github",
                                text = "在 GitHub 中打开",
                                kind = ButtonKind.Secondary,
                                enabled = effectiveUrl.isNotBlank(),
                                action = "stub.open_github",
                            ),
                            span = 6,
                        ),
                    ),
                )
            } else {
                add(
                    row(
                        cell(
                            ButtonComponent(
                                id = "stub.open_github",
                                text = "在 GitHub 中打开",
                                kind = ButtonKind.Secondary,
                                enabled = effectiveUrl.isNotBlank(),
                                action = "stub.open_github",
                            ),
                        ),
                    ),
                )
            }

            // —— 加载态 ——
            if (state.isLoading && state.summary == null) {
                add(
                    row(
                        cell(
                            StateComponent(
                                id = "stub.loading",
                                kind = StateKind.Loading,
                                message = "正在加载页面状态…",
                            ),
                        ),
                    ),
                )
            }

            // —— 摘要卡 ——
            state.summary?.let { summary ->
                add(
                    row(
                        cell(
                            ItemComponent(
                                id = "stub.summary",
                                title = summary.title,
                                badge = statusDisplay(summary.status),
                                meta = summary.sourceUrl.takeIf { it.isNotBlank() }?.let(::listOf) ?: emptyList(),
                            ),
                        ),
                    ),
                )

                // —— 指标卡 ——
                add(
                    row(
                        cell(
                            TextComponent(
                                id = "stub.metrics_title",
                                text = "指标数据",
                                style = TextStyle.Title,
                                color = TextColor.Primary,
                            ),
                        ),
                    ),
                )
                if (summary.metrics.isEmpty()) {
                    add(
                        row(
                            cell(
                                TextComponent(
                                    id = "stub.metrics_empty",
                                    text = "暂无指标数据",
                                    style = TextStyle.Body,
                                    color = TextColor.Secondary,
                                ),
                            ),
                        ),
                    )
                } else {
                    summary.metrics.forEachIndexed { index, metric ->
                        add(
                            row(
                                cell(
                                    TextComponent(
                                        id = "stub.metric.$index",
                                        text = "${metric.label}: ${metric.value}",
                                        style = TextStyle.Body,
                                        color = TextColor.Secondary,
                                    ),
                                ),
                            ),
                        )
                    }
                }

                // —— 提示卡 ——
                add(
                    row(
                        cell(
                            TextComponent(
                                id = "stub.notices_title",
                                text = "提示与建议",
                                style = TextStyle.Title,
                                color = TextColor.Primary,
                            ),
                        ),
                    ),
                )
                if (summary.notices.isEmpty() && summary.actions.isEmpty()) {
                    add(
                        row(
                            cell(
                                TextComponent(
                                    id = "stub.notices_empty",
                                    text = "暂无提示",
                                    style = TextStyle.Body,
                                    color = TextColor.Secondary,
                                ),
                            ),
                        ),
                    )
                } else {
                    summary.notices.forEachIndexed { index, notice ->
                        add(
                            row(
                                cell(
                                    TextComponent(
                                        id = "stub.notice.$index",
                                        text = "• $notice",
                                        style = TextStyle.Body,
                                        color = TextColor.Secondary,
                                    ),
                                ),
                            ),
                        )
                    }
                    summary.actions.forEachIndexed { index, action ->
                        add(
                            row(
                                cell(
                                    TextComponent(
                                        id = "stub.action.$index",
                                        text = "建议操作：$action",
                                        style = TextStyle.Body,
                                        color = TextColor.Accent,
                                    ),
                                ),
                            ),
                        )
                    }
                }
            }

            // —— 调试卡（点击复制） ——
            if (debugText.isNotBlank()) {
                add(
                    row(
                        cell(
                            TextComponent(
                                id = "stub.debug_title",
                                text = "复制调试信息",
                                style = TextStyle.Title,
                                color = TextColor.Primary,
                            ),
                        ),
                    ),
                )
                add(
                    row(
                        cell(
                            TextComponent(
                                id = "stub.debug",
                                text = debugText,
                                style = TextStyle.Code,
                                color = TextColor.Secondary,
                                action = "stub.copy_debug",
                            ),
                        ),
                    ),
                )
            }
        }
        return PageSchema(
            id = "repo_section_stub",
            columns = 12,
            scrollable = true,
            rows = rows,
        )
    }

    /** 壳状态：RepositorySections 分段导航 + 选中当前分区。 */
    fun shellState(
        section: RepositorySection,
        sections: List<RepositorySection>,
        sectionTitle: String,
    ): ShellState = ShellState(
        title = sectionTitle,
        showBack = true,
        backAction = "shell.back",
        navBarMode = NavBarMode.RepositorySections,
        navItems = sections.map(RepositoryDetailPage::sectionNavItem),
        selectedNavId = section.storageKey,
        contentKey = "repo_section_stub",
    )
}

/**
 * HTML 摘要分区页垂直切片入口：壳 + 状态驱动 schema。
 * 一个页面服务六个分区（Wiki/Projects/Insights/Agents/SecurityQuality/仓库 Settings）。
 */
@Composable
fun RepositorySectionStubPageContent(
    state: RepositorySectionNativeStubUiState,
    section: RepositorySection,
    sections: List<RepositorySection>,
    sectionTitle: String,
    fallbackDescription: String,
    repositoryLabel: String,
    initialSectionUrl: String,
    onRetry: () -> Unit = {},
    onOpenInGitHub: (String) -> Unit = {},
    onCopyDebug: (String) -> Unit = {},
    onOpenSection: (RepositorySection) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val effectiveUrl = state.sourceUrl?.takeIf { it.isNotBlank() } ?: initialSectionUrl
    val handleAction: (String) -> Unit = { action ->
        when {
            action == "shell.back" -> onBack()
            action == "stub.retry" -> onRetry()
            action == "stub.open_github" -> if (effectiveUrl.isNotBlank()) onOpenInGitHub(effectiveUrl)
            action == "stub.copy_debug" -> onCopyDebug(RepositorySectionStubPage.debugOutput(state, initialSectionUrl))
            action.startsWith("repo.section.") -> {
                val key = action.removePrefix("repo.section.")
                RepositorySection.fromStorageKey(key)?.let(onOpenSection)
            }
        }
    }
    AppShell(state = RepositorySectionStubPage.shellState(section, sections, sectionTitle), onAction = handleAction) {
        RepositorySectionStubPage.schemaFor(state, sectionTitle, fallbackDescription, repositoryLabel, initialSectionUrl)
            .renderPage(handleAction)
    }
}