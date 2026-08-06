package com.Sunset.REN.GitHub.ui.pages

import androidx.compose.runtime.Composable
import com.Sunset.REN.GitHub.data.github.html.RepositoryWebhookItem
import com.Sunset.REN.GitHub.ui.layout.PageSchema
import com.Sunset.REN.GitHub.ui.layout.RowSchema
import com.Sunset.REN.GitHub.ui.layout.cell
import com.Sunset.REN.GitHub.ui.layout.row
import com.Sunset.REN.GitHub.ui.render.renderPage
import com.Sunset.REN.GitHub.ui.repo.RepositoryWebhooksUiState
import com.Sunset.REN.GitHub.ui.schema.ButtonComponent
import com.Sunset.REN.GitHub.ui.schema.ButtonKind
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

/**
 * 仓库 Webhooks 页垂直切片（组 B：仓库设置/管理）。
 *
 * 渲染结构对齐 RepositoryWebhooksScreen：
 * - 初始加载 → Loading；错误 → Error + 重试；
 * - Content：HeaderCard → inline 消息（pending/error）→ SummaryCard
 *   （owner/repo + 管理员/只读）→ 刷新 + 新增按钮行 → 空态 EmptyCard 或
 *   WebhookCard 列表（启用·停用徽章 + URL/事件/内容类型·SSL/最近响应 + Ping/删除按钮行）。
 * 壳：Hidden + showBack（独立全屏页）。
 * 路由前缀：webhooks.refresh / create / ping.{id} / delete.{id} / retry / shell.back。
 * 新增 Webhook CreateDialog 与删除确认 Dialog 由调用端承载。
 */
object RepositoryWebhooksPage {

    fun schemaFor(
        state: RepositoryWebhooksUiState,
        onRefresh: () -> Unit = {},
        onCreateWebhook: () -> Unit = {},
        onPingWebhook: (RepositoryWebhookItem) -> Unit = {},
        onDeleteWebhook: (RepositoryWebhookItem) -> Unit = {},
    ): PageSchema {
        val rows = buildList<RowSchema> {
            when {
                state.isInitialLoad -> add(
                    row(
                        cell(
                            StateComponent(
                                id = "webhooks.loading",
                                kind = StateKind.Loading,
                                message = "正在加载 Webhooks…",
                            ),
                        ),
                    ),
                )

                state.errorMessage != null && state.snapshot == null -> add(
                    row(
                        cell(
                            StateComponent(
                                id = "webhooks.error",
                                kind = StateKind.Error,
                                message = "Webhooks 暂时不可用",
                                detail = state.errorMessage,
                                retryAction = "webhooks.retry",
                            ),
                        ),
                    ),
                )

                state.snapshot != null -> {
                    val snapshot = state.snapshot
                    // —— HeaderCard ——
                    add(
                        row(
                            cell(
                                SectionHeaderComponent(
                                    id = "webhooks.header",
                                    title = "Webhooks",
                                    subtitle = "向外部服务发送仓库事件通知，适合 CI、部署、机器人和审计集成。",
                                ),
                            ),
                        ),
                    )
                    // —— inline 消息 ——
                    val inlineMessage = when {
                        state.isSaving -> state.pendingMessage.orEmpty()
                        !state.errorMessage.isNullOrBlank() -> state.errorMessage.orEmpty()
                        else -> state.pendingMessage?.takeIf { !state.isSaving }.orEmpty()
                    }
                    if (inlineMessage.isNotBlank()) {
                        add(
                            row(
                                cell(
                                    TextComponent(
                                        id = "webhooks.inline",
                                        text = inlineMessage,
                                        style = TextStyle.Body,
                                        color = if (!state.errorMessage.isNullOrBlank()) TextColor.Danger else TextColor.Secondary,
                                    ),
                                ),
                            ),
                        )
                    }
                    // —— SummaryCard ——
                    add(
                        row(
                            cell(
                                SectionHeaderComponent(
                                    id = "webhooks.summary",
                                    title = "${snapshot.owner}/${snapshot.repo}",
                                    subtitle = if (snapshot.canAdmin) "管理员权限 · 可管理 Webhook" else "只读模式 · 无管理员权限",
                                ),
                            ),
                        ),
                    )
                    // —— 刷新 + 新增按钮行 ——
                    val canEdit = snapshot.canAdmin && !state.isSaving
                    add(
                        row(
                            cell(
                                ButtonComponent(
                                    id = "webhooks.refresh",
                                    text = "刷新",
                                    kind = ButtonKind.Secondary,
                                    action = "webhooks.refresh",
                                ),
                                span = 6,
                            ),
                            cell(
                                ButtonComponent(
                                    id = "webhooks.create",
                                    text = "新增 Webhook",
                                    kind = ButtonKind.Primary,
                                    enabled = canEdit,
                                    action = "webhooks.create",
                                ),
                                span = 6,
                            ),
                        ),
                    )
                    // —— 列表 / 空态 ——
                    if (snapshot.hooks.isEmpty()) {
                        add(
                            row(
                                cell(
                                    TextComponent(
                                        id = "webhooks.empty",
                                        text = "暂无 Webhook",
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
                                        id = "webhooks.empty.hint",
                                        text = "点击“新增 Webhook”配置 Payload URL 和事件列表。",
                                        style = TextStyle.Caption,
                                        color = TextColor.Muted,
                                    ),
                                ),
                            ),
                        )
                    } else {
                        snapshot.hooks.forEach { hook ->
                            add(
                                row(
                                    cell(
                                        TextComponent(
                                            id = "webhooks.item.${hook.id}.badge",
                                            text = if (hook.active) "启用" else "停用",
                                            style = TextStyle.Meta,
                                            color = if (hook.active) TextColor.Accent else TextColor.Muted,
                                        ),
                                    ),
                                ),
                            )
                            add(
                                row(
                                    cell(
                                        SectionHeaderComponent(
                                            id = "webhooks.item.${hook.id}",
                                            title = "#${hook.id}  ${hook.url.ifBlank { "未返回 Payload URL" }}",
                                        ),
                                    ),
                                ),
                            )
                            add(
                                row(
                                    cell(
                                        TextComponent(
                                            id = "webhooks.item.${hook.id}.events",
                                            text = "事件：${hook.events.joinToString().ifBlank { "未返回" }}",
                                            style = TextStyle.Meta,
                                            color = TextColor.Secondary,
                                        ),
                                    ),
                                ),
                            )
                            add(
                                row(
                                    cell(
                                        TextComponent(
                                            id = "webhooks.item.${hook.id}.content",
                                            text = "内容类型：${hook.contentType.ifBlank { "json" }} · SSL：${if (hook.insecureSsl) "允许不安全 SSL" else "验证 SSL"}",
                                            style = TextStyle.Meta,
                                            color = TextColor.Secondary,
                                        ),
                                    ),
                                ),
                            )
                            add(
                                row(
                                    cell(
                                        TextComponent(
                                            id = "webhooks.item.${hook.id}.response",
                                            text = "最近响应：${listOf(hook.lastResponseCode, hook.lastResponseStatus, hook.lastResponseMessage).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "暂无" }}",
                                            style = TextStyle.Caption,
                                            color = TextColor.Muted,
                                        ),
                                    ),
                                ),
                            )
                            add(
                                row(
                                    cell(
                                        ButtonComponent(
                                            id = "webhooks.item.${hook.id}.ping",
                                            text = "Ping",
                                            kind = ButtonKind.Secondary,
                                            enabled = canEdit,
                                            action = "webhooks.ping.${hook.id}",
                                        ),
                                        span = 6,
                                    ),
                                    cell(
                                        ButtonComponent(
                                            id = "webhooks.item.${hook.id}.delete",
                                            text = "删除",
                                            kind = ButtonKind.Secondary,
                                            enabled = canEdit,
                                            action = "webhooks.delete.${hook.id}",
                                        ),
                                        span = 6,
                                    ),
                                ),
                            )
                            add(row(cell(SpacerComponent(id = "webhooks.item.${hook.id}.spacer", heightDp = 10))))
                        }
                    }
                }
            }
        }
        return PageSchema(id = "webhooks", columns = 12, scrollable = true, rows = rows)
    }

    /** Webhooks 页壳状态：Hidden + 返回（独立全屏页）。 */
    fun shellState(): ShellState = ShellState(
        title = "Webhooks",
        showBack = true,
        backAction = "shell.back",
        navBarMode = NavBarMode.Hidden,
        contentKey = "webhooks",
    )
}

/**
 * 仓库 Webhooks 页入口：壳 + 三分支 schema。
 * 新增 Webhook CreateDialog 与删除确认 Dialog 由调用端承载。
 */
@Composable
fun RepositoryWebhooksPageContent(
    state: RepositoryWebhooksUiState,
    onRefresh: () -> Unit = {},
    onCreateWebhook: () -> Unit = {},
    onPingWebhook: (RepositoryWebhookItem) -> Unit = {},
    onDeleteWebhook: (RepositoryWebhookItem) -> Unit = {},
    onRetry: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    val handleAction: (String) -> Unit = { action ->
        when {
            action == "shell.back" -> onBack()
            action == "webhooks.retry" -> onRetry()
            action == "webhooks.refresh" -> onRefresh()
            action == "webhooks.create" -> onCreateWebhook()
            action.startsWith("webhooks.ping.") -> {
                val id = action.removePrefix("webhooks.ping.").toLongOrNull()
                state.snapshot?.hooks?.firstOrNull { it.id == id }?.let(onPingWebhook)
            }
            action.startsWith("webhooks.delete.") -> {
                val id = action.removePrefix("webhooks.delete.").toLongOrNull()
                state.snapshot?.hooks?.firstOrNull { it.id == id }?.let(onDeleteWebhook)
            }
        }
    }
    AppShell(state = RepositoryWebhooksPage.shellState(), onAction = handleAction) {
        RepositoryWebhooksPage.schemaFor(state, onRefresh, onCreateWebhook, onPingWebhook, onDeleteWebhook).renderPage(handleAction)
    }
}