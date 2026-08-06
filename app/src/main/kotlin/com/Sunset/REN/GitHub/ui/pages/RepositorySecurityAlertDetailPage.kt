package com.Sunset.REN.GitHub.ui.pages

import androidx.compose.runtime.Composable
import com.Sunset.REN.GitHub.domain.repo.RepositorySecurityAlert
import com.Sunset.REN.GitHub.ui.layout.PageSchema
import com.Sunset.REN.GitHub.ui.layout.RowSchema
import com.Sunset.REN.GitHub.ui.layout.cell
import com.Sunset.REN.GitHub.ui.layout.row
import com.Sunset.REN.GitHub.ui.render.renderPage
import com.Sunset.REN.GitHub.ui.repo.RepositorySecurityAlertDetailUiState
import com.Sunset.REN.GitHub.ui.schema.ButtonComponent
import com.Sunset.REN.GitHub.ui.schema.ButtonKind
import com.Sunset.REN.GitHub.ui.schema.SectionHeaderComponent
import com.Sunset.REN.GitHub.ui.schema.SpacerComponent
import com.Sunset.REN.GitHub.ui.schema.TextColor
import com.Sunset.REN.GitHub.ui.schema.TextComponent
import com.Sunset.REN.GitHub.ui.schema.TextStyle
import com.Sunset.REN.GitHub.ui.shell.AppShell
import com.Sunset.REN.GitHub.ui.shell.NavBarMode
import com.Sunset.REN.GitHub.ui.shell.ShellState

/**
 * 仓库安全告警详情页垂直切片（组 B：仓库设置/管理）。
 *
 * 渲染结构对齐 RepositorySecurityAlertDetailScreen：
 * - alert = state.alert ?: initialAlert（initialAlert 由列表页参数承载，加载中兜底显示）；
 * - loading → 加载提示；errorMessage → 错误 + 重试；
 * - SecurityAlertCard：标题（空→兜底）/ meta（source·state·severity 连接）/ createdAt /
 *   detailGroups 分组（组标题 + “• item”逐行）或 details 逐行 / 在 GitHub 打开按钮。
 * 壳：Hidden + showBack（独立全屏页）。
 * 路由前缀：security_alert.retry / open_in_github / shell.back。
 */
object RepositorySecurityAlertDetailPage {

    fun schemaFor(
        state: RepositorySecurityAlertDetailUiState,
        initialAlert: RepositorySecurityAlert,
        onOpenInGithub: (String) -> Unit = {},
    ): PageSchema {
        val alert = state.alert ?: initialAlert
        val rows = buildList<RowSchema> {
            if (state.isLoading) {
                add(
                    row(
                        cell(
                            TextComponent(
                                id = "security_alert.loading",
                                text = "正在加载告警详情…",
                                style = TextStyle.Body,
                                color = TextColor.Secondary,
                            ),
                        ),
                    ),
                )
            }
            state.errorMessage?.let { message ->
                add(
                    row(
                        cell(
                            TextComponent(
                                id = "security_alert.error",
                                text = message,
                                style = TextStyle.Body,
                                color = TextColor.Danger,
                            ),
                        ),
                    ),
                )
                add(
                    row(
                        cell(
                            ButtonComponent(
                                id = "security_alert.retry",
                                text = "重试",
                                kind = ButtonKind.Primary,
                                action = "security_alert.retry",
                            ),
                        ),
                    ),
                )
            }
            // —— SecurityAlertCard ——
            add(
                row(
                    cell(
                        SectionHeaderComponent(
                            id = "security_alert.card",
                            title = alert.title.ifBlank { "未知告警" },
                        ),
                    ),
                ),
            )
            val meta = listOf(alert.source, alert.state, alert.severity.orEmpty())
                .filter { it.isNotBlank() }
                .joinToString(" · ")
                .ifBlank { "未知元信息" }
            add(
                row(
                    cell(
                        TextComponent(
                            id = "security_alert.meta",
                            text = meta,
                            style = TextStyle.Meta,
                            color = TextColor.Secondary,
                        ),
                    ),
                ),
            )
            alert.createdAt?.takeIf { it.isNotBlank() }?.let { createdAt ->
                add(
                    row(
                        cell(
                            TextComponent(
                                id = "security_alert.created_at",
                                text = createdAt,
                                style = TextStyle.Meta,
                                color = TextColor.Secondary,
                            ),
                        ),
                    ),
                )
            }
            add(row(cell(SpacerComponent(id = "security_alert.spacer.details", heightDp = 6))))
            if (alert.detailGroups.isEmpty() && alert.details.isEmpty()) {
                add(
                    row(
                        cell(
                            TextComponent(
                                id = "security_alert.details_title",
                                text = "详情",
                                style = TextStyle.Section,
                                color = TextColor.Primary,
                            ),
                        ),
                    ),
                )
            } else if (alert.detailGroups.isNotEmpty()) {
                alert.detailGroups.forEachIndexed { groupIndex, group ->
                    add(
                        row(
                            cell(
                                SectionHeaderComponent(
                                    id = "security_alert.group.$groupIndex",
                                    title = group.title,
                                ),
                            ),
                        ),
                    )
                    group.items.forEachIndexed { itemIndex, item ->
                        add(
                            row(
                                cell(
                                    TextComponent(
                                        id = "security_alert.group.$groupIndex.item.$itemIndex",
                                        text = "• $item",
                                        style = TextStyle.Body,
                                        color = TextColor.Secondary,
                                    ),
                                ),
                            ),
                        )
                    }
                }
            } else {
                alert.details.forEachIndexed { index, detail ->
                    add(
                        row(
                            cell(
                                TextComponent(
                                    id = "security_alert.detail.$index",
                                    text = "• $detail",
                                    style = TextStyle.Body,
                                    color = TextColor.Secondary,
                                ),
                            ),
                        ),
                    )
                }
            }
            alert.htmlUrl?.takeIf { it.isNotBlank() }?.let { url ->
                add(row(cell(SpacerComponent(id = "security_alert.spacer.open", heightDp = 6))))
                add(
                    row(
                        cell(
                            ButtonComponent(
                                id = "security_alert.open",
                                text = "在 GitHub 打开",
                                kind = ButtonKind.Secondary,
                                action = "security_alert.open_in_github",
                            ),
                        ),
                    ),
                )
            }
        }
        return PageSchema(id = "security_alert_detail", columns = 12, scrollable = true, rows = rows)
    }

    /** 安全告警详情页壳状态：Hidden + 返回（独立全屏页）。 */
    fun shellState(): ShellState = ShellState(
        title = "安全告警",
        showBack = true,
        backAction = "shell.back",
        navBarMode = NavBarMode.Hidden,
        contentKey = "security_alert_detail",
    )
}

/**
 * 仓库安全告警详情页入口：壳 + 详情 schema。
 * 在 GitHub 打开由调用端启动外部浏览器。
 */
@Composable
fun RepositorySecurityAlertDetailPageContent(
    state: RepositorySecurityAlertDetailUiState,
    initialAlert: RepositorySecurityAlert,
    onRetry: () -> Unit = {},
    onOpenInGithub: (String) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val alert = state.alert ?: initialAlert
    val handleAction: (String) -> Unit = { action ->
        when (action) {
            "shell.back" -> onBack()
            "security_alert.retry" -> onRetry()
            "security_alert.open_in_github" -> alert.htmlUrl?.takeIf { it.isNotBlank() }?.let(onOpenInGithub)
        }
    }
    AppShell(state = RepositorySecurityAlertDetailPage.shellState(), onAction = handleAction) {
        RepositorySecurityAlertDetailPage.schemaFor(state, initialAlert, onOpenInGithub).renderPage(handleAction)
    }
}