package com.Sunset.REN.GitHub.ui.pages

import androidx.compose.runtime.Composable
import com.Sunset.REN.GitHub.ui.layout.PageSchema
import com.Sunset.REN.GitHub.ui.layout.RowSchema
import com.Sunset.REN.GitHub.ui.layout.cell
import com.Sunset.REN.GitHub.ui.layout.row
import com.Sunset.REN.GitHub.ui.render.renderPage
import com.Sunset.REN.GitHub.ui.repo.RepositoryDangerZoneUiState
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
 * 仓库危险区页垂直切片（组 B：仓库设置/管理）。
 *
 * 渲染结构对齐 RepositoryDangerZoneScreen（三分支）：
 * - 初始加载 → Loading；错误 → Error（已删除/不可用双标题，已删除无重试）；
 * - Content：inline 消息 + 仓库卡（fullName 危险色标题 + 归档状态/管理员权限摘要）+
 *   三个危险操作区块（归档/取消归档 → 转移 → 删除，各自标题+描述+按钮，danger 语义）。
 * 壳：Hidden + showBack（独立全屏页）。
 * 路由前缀：danger_zone.archive / transfer / delete / retry / shell.back。
 * 归档/转移/删除确认 Dialog 由调用端承载。
 */
object RepositoryDangerZonePage {

    fun schemaFor(
        state: RepositoryDangerZoneUiState,
        onArchiveClick: (Boolean) -> Unit = { _ -> },
    ): PageSchema {
        val rows = buildList<RowSchema> {
            when {
                state.isInitialLoad -> add(
                    row(
                        cell(
                            StateComponent(
                                id = "danger_zone.loading",
                                kind = StateKind.Loading,
                                message = "正在加载危险区…",
                            ),
                        ),
                    ),
                )

                state.errorMessage != null && state.snapshot == null -> add(
                    row(
                        cell(
                            StateComponent(
                                id = "danger_zone.error",
                                kind = StateKind.Error,
                                message = if (state.isDeleted) "仓库已删除" else "危险区暂时不可用",
                                detail = state.errorMessage,
                                retryAction = if (state.isDeleted) "" else "danger_zone.retry",
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
                                        id = "danger_zone.inline",
                                        text = inlineMessage,
                                        style = TextStyle.Body,
                                        color = if (!state.errorMessage.isNullOrBlank()) TextColor.Danger else TextColor.Secondary,
                                    ),
                                ),
                            ),
                        )
                    }
                    // —— 仓库卡 ——
                    add(
                        row(
                            cell(
                                SectionHeaderComponent(
                                    id = "danger_zone.repo_header",
                                    title = snapshot.fullName,
                                ),
                            ),
                        ),
                    )
                    add(
                        row(
                            cell(
                                TextComponent(
                                    id = "danger_zone.repo_status",
                                    text = if (snapshot.archived) "仓库已归档" else "仓库未归档",
                                    style = TextStyle.Body,
                                    color = if (snapshot.archived) TextColor.Accent else TextColor.Success,
                                ),
                            ),
                        ),
                    )
                    add(
                        row(
                            cell(
                                TextComponent(
                                    id = "danger_zone.repo_permission",
                                    text = if (snapshot.canAdmin) "管理员权限 · 可执行危险操作" else "只读模式 · 无管理员权限",
                                    style = TextStyle.Meta,
                                    color = TextColor.Secondary,
                                ),
                            ),
                        ),
                    )
                    add(row(cell(SpacerComponent(id = "danger_zone.spacer.actions", heightDp = 8))))
                    val canEdit = snapshot.canAdmin && !state.isSaving
                    // —— 归档/取消归档 ——
                    add(
                        row(
                            cell(
                                SectionHeaderComponent(
                                    id = "danger_zone.archive_section",
                                    title = if (snapshot.archived) "取消归档" else "归档仓库",
                                    subtitle = if (snapshot.archived) {
                                        "取消归档后仓库恢复可读可写，他人可再次访问。"
                                    } else {
                                        "归档后仓库变为只读，他人仍可浏览，但无法提交。"
                                    },
                                ),
                            ),
                        ),
                    )
                    add(
                        row(
                            cell(
                                ButtonComponent(
                                    id = "danger_zone.archive_action",
                                    text = if (snapshot.archived) "取消归档" else "归档仓库",
                                    kind = if (snapshot.archived) ButtonKind.Secondary else ButtonKind.Danger,
                                    enabled = canEdit,
                                    action = "danger_zone.archive",
                                ),
                                span = 6,
                            ),
                            cell(
                                TextComponent(
                                    id = "danger_zone.archive_detail",
                                    text = if (snapshot.archived) "操作后仓库恢复可写状态" else "操作后仓库变为只读",
                                    style = TextStyle.Caption,
                                    color = TextColor.Muted,
                                ),
                                span = 6,
                            ),
                        ),
                    )
                    add(row(cell(SpacerComponent(id = "danger_zone.spacer.transfer", heightDp = 12))))
                    // —— 转移 ——
                    add(
                        row(
                            cell(
                                SectionHeaderComponent(
                                    id = "danger_zone.transfer_section",
                                    title = "转移仓库",
                                    subtitle = "将仓库转移到其他账号或组织，转移后保留历史记录。",
                                ),
                            ),
                        ),
                    )
                    add(
                        row(
                            cell(
                                ButtonComponent(
                                    id = "danger_zone.transfer_action",
                                    text = "转移仓库",
                                    kind = ButtonKind.Danger,
                                    enabled = canEdit,
                                    action = "danger_zone.transfer",
                                ),
                                span = 6,
                            ),
                            cell(
                                TextComponent(
                                    id = "danger_zone.transfer_detail",
                                    text = "转移目标：$snapshot.fullName",
                                    style = TextStyle.Caption,
                                    color = TextColor.Muted,
                                ),
                                span = 6,
                            ),
                        ),
                    )
                    add(row(cell(SpacerComponent(id = "danger_zone.spacer.delete", heightDp = 12))))
                    // —— 删除 ——
                    add(
                        row(
                            cell(
                                SectionHeaderComponent(
                                    id = "danger_zone.delete_section",
                                    title = "删除仓库",
                                    subtitle = "永久删除该仓库及其所有分支、Issue、PR 与附件，此操作不可撤销。",
                                ),
                            ),
                        ),
                    )
                    add(
                        row(
                            cell(
                                ButtonComponent(
                                    id = "danger_zone.delete_action",
                                    text = "删除仓库",
                                    kind = ButtonKind.Danger,
                                    enabled = canEdit,
                                    action = "danger_zone.delete",
                                ),
                                span = 6,
                            ),
                            cell(
                                TextComponent(
                                    id = "danger_zone.delete_detail",
                                    text = "删除目标：$snapshot.fullName",
                                    style = TextStyle.Caption,
                                    color = TextColor.Muted,
                                ),
                                span = 6,
                            ),
                        ),
                    )
                }
            }
        }
        return PageSchema(id = "danger_zone", columns = 12, scrollable = true, rows = rows)
    }

    /** 危险区页壳状态：Hidden + 返回（独立全屏页）。 */
    fun shellState(): ShellState = ShellState(
        title = "危险区",
        showBack = true,
        backAction = "shell.back",
        navBarMode = NavBarMode.Hidden,
        contentKey = "danger_zone",
    )
}

/**
 * 仓库危险区页入口：壳 + 三分支 schema。
 * 归档/转移/删除确认 Dialog（含二次确认）由调用端承载。
 */
@Composable
fun RepositoryDangerZonePageContent(
    state: RepositoryDangerZoneUiState,
    onArchiveClick: (Boolean) -> Unit = { _ -> },
    onTransferClick: (String) -> Unit = {},
    onDeleteClick: (String) -> Unit = {},
    onRetry: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    val handleAction: (String) -> Unit = { action ->
        when (action) {
            "shell.back" -> onBack()
            "danger_zone.retry" -> onRetry()
            "danger_zone.archive" -> state.snapshot?.let { onArchiveClick(!it.archived) }
            "danger_zone.transfer" -> state.snapshot?.let { onTransferClick(it.fullName) }
            "danger_zone.delete" -> state.snapshot?.let { onDeleteClick(it.fullName) }
        }
    }
    AppShell(state = RepositoryDangerZonePage.shellState(), onAction = handleAction) {
        RepositoryDangerZonePage.schemaFor(state, onArchiveClick).renderPage(handleAction)
    }
}