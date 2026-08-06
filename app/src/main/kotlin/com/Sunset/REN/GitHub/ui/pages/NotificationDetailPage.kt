package com.Sunset.REN.GitHub.ui.pages

import androidx.compose.runtime.Composable
import com.Sunset.REN.GitHub.ui.layout.PageSchema
import com.Sunset.REN.GitHub.ui.layout.RowSchema
import com.Sunset.REN.GitHub.ui.layout.cell
import com.Sunset.REN.GitHub.ui.layout.row
import com.Sunset.REN.GitHub.ui.render.renderPage
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
 * 通知详情页（Notification Detail）垂直切片（任务 3）。
 *
 * 渲染结构对齐 NotificationDetailFragment 内嵌 Screen：
 * - 头部（仓库名 Muted + 主题标题 Section）；
 * - 信息卡（类型/原因/更新时间三行）；
 * - 快捷操作区（标记已读[unread 驱动 enabled]/完成 + 订阅/取消订阅 span6+6）；
 * - 打开相关链接区（主题/最新评论/仓库，各自 URL 非空驱动 enabled）。
 * 本地化复用 NotificationsPage.localizeSubjectType/localizeReason 纯函数。
 * 壳：Hidden + showBack（通知次级页）。
 * 路由前缀：notification_detail.mark_read / done / subscribe / unsubscribe /
 * open_subject / open_latest / open_repo / shell.back。
 * 内部链接跳转（GitHubInternalLinkParser）与 Snackbar 提示由调用端承载。
 */

/** 通知详情页。 */
object NotificationDetailPage {

    fun schemaFor(
        repositoryFullName: String,
        subjectTitle: String,
        subjectType: String,
        reason: String,
        unread: Boolean,
        updatedAt: String,
        htmlUrl: String,
        repositoryHtmlUrl: String,
        latestCommentHtmlUrl: String,
    ): PageSchema {
        val rows = buildList<RowSchema> {
            // —— 头部 ——
            add(
                row(
                    cell(
                        TextComponent(
                            id = "notification_detail.repo",
                            text = repositoryFullName.ifBlank { "通知详情" },
                            style = TextStyle.Caption,
                            color = TextColor.Muted,
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        TextComponent(
                            id = "notification_detail.title",
                            text = subjectTitle.ifBlank { "（无标题）" },
                            style = TextStyle.Section,
                            color = TextColor.Primary,
                        ),
                    ),
                ),
            )
            add(row(cell(SpacerComponent(id = "notification_detail.spacer.info", heightDp = 8))))
            // —— 信息卡 ——
            add(
                row(
                    cell(
                        TextComponent(
                            id = "notification_detail.type",
                            text = "通知类型：${NotificationsPage.localizeSubjectType(subjectType)}",
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
                            id = "notification_detail.reason",
                            text = "通知原因：${NotificationsPage.localizeReason(reason)}",
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
                            id = "notification_detail.updated",
                            text = "已更新：${updatedAt.ifBlank { "未知时间" }}",
                            style = TextStyle.Body,
                            color = TextColor.Secondary,
                        ),
                    ),
                ),
            )
            add(row(cell(SpacerComponent(id = "notification_detail.spacer.actions", heightDp = 8))))
            // —— 快捷操作区 ——
            add(
                row(
                    cell(
                        SectionHeaderComponent(
                            id = "notification_detail.quick_actions",
                            title = "快捷操作",
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        ButtonComponent(
                            id = "notification_detail.mark_read",
                            text = "通知操作标记已读",
                            kind = ButtonKind.Primary,
                            enabled = unread,
                            action = "notification_detail.mark_read",
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        ButtonComponent(
                            id = "notification_detail.done",
                            text = "通知操作完成",
                            kind = ButtonKind.Primary,
                            action = "notification_detail.done",
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        ButtonComponent(
                            id = "notification_detail.subscribe",
                            text = "通知操作订阅",
                            kind = ButtonKind.Secondary,
                            action = "notification_detail.subscribe",
                        ),
                        span = 6,
                    ),
                    cell(
                        ButtonComponent(
                            id = "notification_detail.unsubscribe",
                            text = "通知操作取消订阅",
                            kind = ButtonKind.Secondary,
                            action = "notification_detail.unsubscribe",
                        ),
                        span = 6,
                    ),
                ),
            )
            add(row(cell(SpacerComponent(id = "notification_detail.spacer.links", heightDp = 8))))
            // —— 打开相关链接区 ——
            add(
                row(
                    cell(
                        SectionHeaderComponent(
                            id = "notification_detail.open_links",
                            title = "打开相关链接",
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        ButtonComponent(
                            id = "notification_detail.open_subject",
                            text = "通知操作打开主题",
                            kind = ButtonKind.Primary,
                            enabled = htmlUrl.isNotBlank(),
                            action = "notification_detail.open_subject",
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        ButtonComponent(
                            id = "notification_detail.open_latest",
                            text = "通知操作打开最新评论",
                            kind = ButtonKind.Primary,
                            enabled = latestCommentHtmlUrl.isNotBlank(),
                            action = "notification_detail.open_latest",
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        ButtonComponent(
                            id = "notification_detail.open_repo",
                            text = "通知操作打开仓库",
                            kind = ButtonKind.Primary,
                            enabled = repositoryHtmlUrl.isNotBlank(),
                            action = "notification_detail.open_repo",
                        ),
                    ),
                ),
            )
        }
        return PageSchema(id = "notification_detail", columns = 12, scrollable = true, rows = rows)
    }

    fun shellState(): ShellState = ShellState(
        title = "通知详情",
        showBack = true,
        backAction = "shell.back",
        navBarMode = NavBarMode.Hidden,
        contentKey = "notification_detail",
    )
}

/** 通知详情页入口：壳 + 详情 schema。内部链接跳转与 Snackbar 由调用端承载。 */
@Composable
fun NotificationDetailPageContent(
    repositoryFullName: String = "",
    subjectTitle: String = "",
    subjectType: String = "",
    reason: String = "",
    unread: Boolean = false,
    updatedAt: String = "",
    htmlUrl: String = "",
    repositoryHtmlUrl: String = "",
    latestCommentHtmlUrl: String = "",
    onMarkRead: () -> Unit = {},
    onDone: () -> Unit = {},
    onSubscribe: () -> Unit = {},
    onUnsubscribe: () -> Unit = {},
    onOpenSubject: () -> Unit = {},
    onOpenLatest: () -> Unit = {},
    onOpenRepo: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    val handleAction: (String) -> Unit = { action ->
        when (action) {
            "shell.back" -> onBack()
            "notification_detail.mark_read" -> onMarkRead()
            "notification_detail.done" -> onDone()
            "notification_detail.subscribe" -> onSubscribe()
            "notification_detail.unsubscribe" -> onUnsubscribe()
            "notification_detail.open_subject" -> onOpenSubject()
            "notification_detail.open_latest" -> onOpenLatest()
            "notification_detail.open_repo" -> onOpenRepo()
        }
    }
    AppShell(state = NotificationDetailPage.shellState(), onAction = handleAction) {
        NotificationDetailPage.schemaFor(
            repositoryFullName, subjectTitle, subjectType, reason, unread, updatedAt,
            htmlUrl, repositoryHtmlUrl, latestCommentHtmlUrl,
        ).renderPage(handleAction)
    }
}