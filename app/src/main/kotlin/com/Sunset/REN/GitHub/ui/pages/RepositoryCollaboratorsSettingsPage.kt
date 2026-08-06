package com.Sunset.REN.GitHub.ui.pages

import androidx.compose.runtime.Composable
import com.Sunset.REN.GitHub.ui.layout.PageSchema
import com.Sunset.REN.GitHub.ui.layout.RowSchema
import com.Sunset.REN.GitHub.ui.layout.cell
import com.Sunset.REN.GitHub.ui.layout.row
import com.Sunset.REN.GitHub.ui.render.renderPage
import com.Sunset.REN.GitHub.ui.repo.RepositoryCollaboratorsSettingsUiState
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

/**
 * 仓库协作者设置页垂直切片（组 B：仓库设置/管理）。
 *
 * 渲染结构对齐 RepositoryCollaboratorsSettingsScreen（三分支）：
 * - 初始加载 → Loading；错误（snapshot 为空）→ Error+重试；snapshot → Content；
 * - Content：inline 消息（保存/错误）+ 概览卡（总数/管理员/写权限/待邀请/权限五指标）+
 *   协作者列表卡（login·权限 + htmlUrl，选中 ✓）+ 待邀请卡（displayName·权限 + 提示）+
 *   高级卡（邀请协作者/更改权限/移除协作者三动作，danger 语义）。
 * 壳：Hidden + showBack（独立全屏页）。
 * 路由前缀：collaborators.select.{login} / invite / change_permission / remove / cancel_invitation.{index} / retry / shell.back。
 * 邀请/改权限/移除/取消邀请 Dialog 由调用端承载。
 */
object RepositoryCollaboratorsSettingsPage {

    /** 协作者行标题（login · 权限）。 */
    private fun collaboratorTitle(login: String, permissionLabel: String): String = "$login · $permissionLabel"

    /** 邀请行标题（displayName · 权限）。 */
    private fun invitationTitle(displayName: String, permissionLabel: String): String = "$displayName · $permissionLabel"

    fun schemaFor(
        state: RepositoryCollaboratorsSettingsUiState,
        onCancelInvitation: (Int) -> Unit = {},
    ): PageSchema {
        val rows = buildList<RowSchema> {
            when {
                state.isInitialLoad -> add(
                    row(
                        cell(
                            StateComponent(
                                id = "collaborators.loading",
                                kind = StateKind.Loading,
                                message = "正在加载协作者设置…",
                            ),
                        ),
                    ),
                )

                state.errorMessage != null && state.snapshot == null -> add(
                    row(
                        cell(
                            StateComponent(
                                id = "collaborators.error",
                                kind = StateKind.Error,
                                message = "加载协作者设置失败",
                                detail = state.errorMessage,
                                retryAction = "collaborators.retry",
                            ),
                        ),
                    ),
                )

                state.snapshot != null -> {
                    val snapshot = state.snapshot
                    // —— inline 消息 ——
                    val inlineMessage = when {
                        state.isSaving -> state.pendingMessage.orEmpty()
                        !state.errorMessage.isNullOrBlank() -> state.errorMessage.orEmpty()
                        else -> ""
                    }
                    if (inlineMessage.isNotBlank()) {
                        add(
                            row(
                                cell(
                                    TextComponent(
                                        id = "collaborators.inline",
                                        text = inlineMessage,
                                        style = TextStyle.Body,
                                        color = if (!state.errorMessage.isNullOrBlank()) TextColor.Danger else TextColor.Secondary,
                                    ),
                                ),
                            ),
                        )
                    }
                    // —— 概览卡 ——
                    add(
                        row(
                            cell(
                                SectionHeaderComponent(
                                    id = "collaborators.overview",
                                    title = "协作者",
                                    subtitle = "共 ${snapshot.collaborators.size} 名协作者 · ${snapshot.adminCount} 管理员 · ${snapshot.writeLikeCount} 写权限 · ${snapshot.invitations.size} 待邀请",
                                ),
                            ),
                        ),
                    )
                    add(
                        row(
                            cell(
                                TextComponent(
                                    id = "collaborators.metric_total",
                                    text = "协作者总数：${snapshot.collaborators.size}",
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
                                    id = "collaborators.metric_admin",
                                    text = "管理员：${snapshot.adminCount}",
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
                                    id = "collaborators.metric_write",
                                    text = "写权限：${snapshot.writeLikeCount}",
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
                                    id = "collaborators.metric_invitations",
                                    text = "待邀请：${snapshot.invitations.size}",
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
                                    id = "collaborators.metric_permission",
                                    text = if (snapshot.canAdmin) "权限：可编辑" else "权限：只读",
                                    style = TextStyle.Meta,
                                    color = TextColor.Secondary,
                                ),
                            ),
                        ),
                    )
                    val canEdit = snapshot.canAdmin && !state.isSaving
                    add(row(cell(SpacerComponent(id = "collaborators.spacer.list", heightDp = 8))))
                    // —— 协作者列表卡 ——
                    add(
                        row(
                            cell(
                                SectionHeaderComponent(
                                    id = "collaborators.list_header",
                                    title = "协作者列表",
                                ),
                            ),
                        ),
                    )
                    if (state.collaborators.isEmpty()) {
                        add(
                            row(
                                cell(
                                    TextComponent(
                                        id = "collaborators.empty",
                                        text = "暂无协作者。",
                                        style = TextStyle.Body,
                                        color = TextColor.Secondary,
                                    ),
                                ),
                            ),
                        )
                    } else {
                        state.collaborators.forEach { row ->
                            add(
                                row(
                                    cell(
                                        ItemComponent(
                                            id = "collaborators.collaborator.${row.login}",
                                            title = collaboratorTitle(row.login, row.permissionLabel),
                                            subtitle = row.htmlUrl.ifBlank { row.permission.apiValue },
                                            badge = if (row.login == state.selectedLogin) "✓" else null,
                                            badgeColor = TextColor.Success,
                                            action = "collaborators.select.${row.login}",
                                        ),
                                    ),
                                ),
                            )
                        }
                    }
                    add(row(cell(SpacerComponent(id = "collaborators.spacer.invitations", heightDp = 8))))
                    // —— 待邀请卡 ——
                    add(
                        row(
                            cell(
                                SectionHeaderComponent(
                                    id = "collaborators.invitations_header",
                                    title = "待处理邀请",
                                ),
                            ),
                        ),
                    )
                    if (state.invitations.isEmpty()) {
                        add(
                            row(
                                cell(
                                    TextComponent(
                                        id = "collaborators.invitations_empty",
                                        text = "暂无待处理邀请。",
                                        style = TextStyle.Body,
                                        color = TextColor.Secondary,
                                    ),
                                ),
                            ),
                        )
                    } else {
                        state.invitations.forEachIndexed { index, invitation ->
                            add(
                                row(
                                    cell(
                                        ItemComponent(
                                            id = "collaborators.invitation.$index",
                                            title = invitationTitle(invitation.displayName, invitation.permissionLabel),
                                            subtitle = if (canEdit) "点击取消邀请" else "只读模式 · 无管理员权限",
                                            trailing = if (canEdit) "取消" else null,
                                            action = if (canEdit) "collaborators.cancel_invitation.$index" else "",
                                        ),
                                    ),
                                ),
                            )
                        }
                    }
                    add(row(cell(SpacerComponent(id = "collaborators.spacer.advanced", heightDp = 8))))
                    // —— 高级卡 ——
                    add(
                        row(
                            cell(
                                SectionHeaderComponent(
                                    id = "collaborators.advanced_header",
                                    title = "高级操作",
                                ),
                            ),
                        ),
                    )
                    val selectedLogin = state.selectedLogin.orEmpty()
                    add(
                        row(
                            cell(
                                TextComponent(
                                    id = "collaborators.action_invite",
                                    text = "邀请协作者",
                                    style = TextStyle.Body,
                                    color = if (canEdit) TextColor.Accent else TextColor.Muted,
                                    action = if (canEdit) "collaborators.invite" else "",
                                ),
                            ),
                        ),
                    )
                    add(
                        row(
                            cell(
                                TextComponent(
                                    id = "collaborators.action_change_permission",
                                    text = "更改选中协作者权限",
                                    style = TextStyle.Body,
                                    color = if (canEdit && selectedLogin.isNotBlank()) TextColor.Accent else TextColor.Muted,
                                    action = if (canEdit && selectedLogin.isNotBlank()) "collaborators.change_permission" else "",
                                ),
                            ),
                        ),
                    )
                    add(
                        row(
                            cell(
                                TextComponent(
                                    id = "collaborators.action_remove",
                                    text = "移除选中协作者",
                                    style = TextStyle.Body,
                                    color = if (canEdit && selectedLogin.isNotBlank()) TextColor.Danger else TextColor.Muted,
                                    action = if (canEdit && selectedLogin.isNotBlank()) "collaborators.remove" else "",
                                ),
                            ),
                        ),
                    )
                }
            }
        }
        return PageSchema(id = "collaborators", columns = 12, scrollable = true, rows = rows)
    }

    /** 协作者设置页壳状态：Hidden + 返回（独立全屏页）。 */
    fun shellState(): ShellState = ShellState(
        title = "协作者",
        showBack = true,
        backAction = "shell.back",
        navBarMode = NavBarMode.Hidden,
        contentKey = "collaborators",
    )
}

/**
 * 仓库协作者设置页入口：壳 + 三分支 schema。
 * 邀请/改权限/移除/取消邀请 Dialog 由调用端承载（selectedLogin 由调用端持有）。
 */
@Composable
fun RepositoryCollaboratorsSettingsPageContent(
    state: RepositoryCollaboratorsSettingsUiState,
    onSelectCollaborator: (String) -> Unit = {},
    onInvite: () -> Unit = {},
    onChangePermission: (String) -> Unit = {},
    onRemoveCollaborator: (String) -> Unit = {},
    onCancelInvitation: (Int) -> Unit = {},
    onRetry: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    val handleAction: (String) -> Unit = { action ->
        when {
            action == "shell.back" -> onBack()
            action == "collaborators.retry" -> onRetry()
            action == "collaborators.invite" -> onInvite()
            action == "collaborators.change_permission" -> state.selectedLogin?.let(onChangePermission)
            action == "collaborators.remove" -> state.selectedLogin?.let(onRemoveCollaborator)
            action.startsWith("collaborators.select.") -> onSelectCollaborator(action.removePrefix("collaborators.select."))
            action.startsWith("collaborators.cancel_invitation.") -> {
                val index = action.removePrefix("collaborators.cancel_invitation.").toIntOrNull()
                if (index != null) onCancelInvitation(index)
            }
        }
    }
    AppShell(state = RepositoryCollaboratorsSettingsPage.shellState(), onAction = handleAction) {
        RepositoryCollaboratorsSettingsPage.schemaFor(state, onCancelInvitation).renderPage(handleAction)
    }
}