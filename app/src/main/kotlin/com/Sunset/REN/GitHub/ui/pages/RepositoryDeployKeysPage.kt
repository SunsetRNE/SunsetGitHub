package com.Sunset.REN.GitHub.ui.pages

import androidx.compose.runtime.Composable
import com.Sunset.REN.GitHub.data.github.html.RepositoryDeployKeyItem
import com.Sunset.REN.GitHub.ui.layout.PageSchema
import com.Sunset.REN.GitHub.ui.layout.RowSchema
import com.Sunset.REN.GitHub.ui.layout.cell
import com.Sunset.REN.GitHub.ui.layout.row
import com.Sunset.REN.GitHub.ui.render.renderPage
import com.Sunset.REN.GitHub.ui.repo.RepositoryDeployKeysUiState
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
 * 仓库部署密钥页垂直切片（组 B：仓库设置/管理）。
 *
 * 渲染结构对齐 RepositoryDeployKeysScreen：
 * - 初始加载 → Loading；错误 → Error + 重试；
 * - Content：HeaderCard → inline 消息（pending/error）→ SummaryCard
 *   （owner/repo + 管理员/只读）→ 刷新 + 新增按钮行 → 空态 EmptyCard 或
 *   DeployKeyCard 列表（只读·读写徽章 + 标题 + 验证状态·创建 + 公钥前 96 字符 + 删除按钮）。
 * 壳：Hidden + showBack（独立全屏页）。
 * 路由前缀：deploy_keys.refresh / add / delete.{id} / retry / shell.back。
 * 新增密钥 AddDialog（双确认 + 写权限二次 WRITE 确认）由调用端承载。
 */
object RepositoryDeployKeysPage {

    fun schemaFor(
        state: RepositoryDeployKeysUiState,
        onRefresh: () -> Unit = {},
        onAddKey: () -> Unit = {},
        onDeleteKey: (RepositoryDeployKeyItem) -> Unit = {},
    ): PageSchema {
        val rows = buildList<RowSchema> {
            when {
                state.isInitialLoad -> add(
                    row(
                        cell(
                            StateComponent(
                                id = "deploy_keys.loading",
                                kind = StateKind.Loading,
                                message = "正在加载部署密钥…",
                            ),
                        ),
                    ),
                )

                state.errorMessage != null && state.snapshot == null -> add(
                    row(
                        cell(
                            StateComponent(
                                id = "deploy_keys.error",
                                kind = StateKind.Error,
                                message = "部署密钥暂时不可用",
                                detail = state.errorMessage,
                                retryAction = "deploy_keys.retry",
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
                                    id = "deploy_keys.header",
                                    title = "部署密钥",
                                    subtitle = "为部署服务器或自动化系统授予仓库级 SSH 访问权限。建议优先使用只读密钥，谨慎开放写权限。",
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
                                        id = "deploy_keys.inline",
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
                                    id = "deploy_keys.summary",
                                    title = "${snapshot.owner}/${snapshot.repo}",
                                    subtitle = if (snapshot.canAdmin) "管理员权限 · 可管理部署密钥" else "只读模式 · 无管理员权限",
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
                                    id = "deploy_keys.refresh",
                                    text = "刷新",
                                    kind = ButtonKind.Secondary,
                                    action = "deploy_keys.refresh",
                                ),
                                span = 6,
                            ),
                            cell(
                                ButtonComponent(
                                    id = "deploy_keys.add",
                                    text = "新增部署密钥",
                                    kind = ButtonKind.Primary,
                                    enabled = canEdit,
                                    action = "deploy_keys.add",
                                ),
                                span = 6,
                            ),
                        ),
                    )
                    // —— 列表 / 空态 ——
                    if (snapshot.keys.isEmpty()) {
                        add(
                            row(
                                cell(
                                    TextComponent(
                                        id = "deploy_keys.empty",
                                        text = "暂无部署密钥",
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
                                        id = "deploy_keys.empty.hint",
                                        text = "点击“新增部署密钥”添加 SSH 公钥。",
                                        style = TextStyle.Caption,
                                        color = TextColor.Muted,
                                    ),
                                ),
                            ),
                        )
                    } else {
                        snapshot.keys.forEach { key ->
                            add(
                                row(
                                    cell(
                                        TextComponent(
                                            id = "deploy_keys.item.${key.id}.badge",
                                            text = if (key.readOnly) "只读" else "读写",
                                            style = TextStyle.Meta,
                                            color = if (key.readOnly) TextColor.Muted else TextColor.Danger,
                                        ),
                                    ),
                                ),
                            )
                            add(
                                row(
                                    cell(
                                        SectionHeaderComponent(
                                            id = "deploy_keys.item.${key.id}",
                                            title = key.title.ifBlank { "#${key.id}" },
                                        ),
                                    ),
                                ),
                            )
                            add(
                                row(
                                    cell(
                                        TextComponent(
                                            id = "deploy_keys.item.${key.id}.verified",
                                            text = "验证状态：${if (key.verified) "已验证" else "未验证"} · 创建：${key.createdAt.ifBlank { "未知" }}",
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
                                            id = "deploy_keys.item.${key.id}.key",
                                            text = key.key.take(96).ifBlank { "未返回公钥内容" },
                                            style = TextStyle.Code,
                                            color = TextColor.Secondary,
                                        ),
                                    ),
                                ),
                            )
                            add(
                                row(
                                    cell(
                                        ButtonComponent(
                                            id = "deploy_keys.item.${key.id}.delete",
                                            text = "删除",
                                            kind = ButtonKind.Secondary,
                                            enabled = canEdit,
                                            action = "deploy_keys.delete.${key.id}",
                                        ),
                                    ),
                                ),
                            )
                            add(row(cell(SpacerComponent(id = "deploy_keys.item.${key.id}.spacer", heightDp = 10))))
                        }
                    }
                }
            }
        }
        return PageSchema(id = "deploy_keys", columns = 12, scrollable = true, rows = rows)
    }

    /** 部署密钥页壳状态：Hidden + 返回（独立全屏页）。 */
    fun shellState(): ShellState = ShellState(
        title = "部署密钥",
        showBack = true,
        backAction = "shell.back",
        navBarMode = NavBarMode.Hidden,
        contentKey = "deploy_keys",
    )
}

/**
 * 仓库部署密钥页入口：壳 + 三分支 schema。
 * 新增密钥 AddDialog（含写权限二次 WRITE 确认）与删除确认 Dialog 由调用端承载。
 */
@Composable
fun RepositoryDeployKeysPageContent(
    state: RepositoryDeployKeysUiState,
    onRefresh: () -> Unit = {},
    onAddKey: () -> Unit = {},
    onDeleteKey: (RepositoryDeployKeyItem) -> Unit = {},
    onRetry: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    val handleAction: (String) -> Unit = { action ->
        when {
            action == "shell.back" -> onBack()
            action == "deploy_keys.retry" -> onRetry()
            action == "deploy_keys.refresh" -> onRefresh()
            action == "deploy_keys.add" -> onAddKey()
            action.startsWith("deploy_keys.delete.") -> {
                val id = action.removePrefix("deploy_keys.delete.").toLongOrNull()
                state.snapshot?.keys?.firstOrNull { it.id == id }?.let(onDeleteKey)
            }
        }
    }
    AppShell(state = RepositoryDeployKeysPage.shellState(), onAction = handleAction) {
        RepositoryDeployKeysPage.schemaFor(state, onRefresh, onAddKey, onDeleteKey).renderPage(handleAction)
    }
}