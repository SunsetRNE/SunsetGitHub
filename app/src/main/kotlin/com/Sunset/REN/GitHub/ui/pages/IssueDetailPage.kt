package com.Sunset.REN.GitHub.ui.pages

import androidx.compose.runtime.Composable
import com.Sunset.REN.GitHub.domain.repo.RepositoryIssueComment
import com.Sunset.REN.GitHub.ui.layout.PageSchema
import com.Sunset.REN.GitHub.ui.layout.cell
import com.Sunset.REN.GitHub.ui.layout.row
import com.Sunset.REN.GitHub.ui.render.renderPage
import com.Sunset.REN.GitHub.ui.repo.RepositoryIssueDetailUiState
import com.Sunset.REN.GitHub.ui.repo.RepositoryIssuesUiState
import com.Sunset.REN.GitHub.ui.repo.RepositorySection
import com.Sunset.REN.GitHub.ui.schema.ButtonComponent
import com.Sunset.REN.GitHub.ui.schema.ButtonKind
import com.Sunset.REN.GitHub.ui.schema.FieldComponent
import com.Sunset.REN.GitHub.ui.schema.IconId
import com.Sunset.REN.GitHub.ui.schema.ItemAction
import com.Sunset.REN.GitHub.ui.schema.ItemComponent
import com.Sunset.REN.GitHub.ui.schema.ListComponent
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
 * Issue 详情页（Issue Detail）垂直切片（步骤 5：列表→详情链路）。
 *
 * 与 IssuesPage 共用 RepositorySections 壳（保持仓库上下文，选中 Issues 分区）。
 * 渲染结构对齐 RepositoryIssueDetailScreen：
 * - 状态分支：isInitialLoad → Loading；error && 空 → Error+重试；issue → 详情；
 * - Issue 头：标题 + 状态徽章 + 标签 + meta + 正文 + 切换状态/编辑标签（权限字段驱动）；
 * - 评论列表（可管理评论带删除行内动作）+ 加载更多 + 登录后评论输入（草稿受控）。
 * 路由前缀：issue_detail.retry / load_more / toggle_state / labels / send_comment /
 * delete_comment.{id} / repo.section.* / shell.back。
 * 注：编辑评论/标签选择等 AlertDialog 由调用端承载（组件库不引入 Dialog）。
 */
object IssueDetailPage {

    private fun stateLabel(state: String): String = when (state) {
        RepositoryIssuesUiState.ClosedState -> "已关闭"
        else -> "开启中"
    }

    private fun formatDate(raw: String?): String? = raw?.substringBefore('T')?.takeIf { it.isNotBlank() }

    /** 状态 → 页面 schema。 */
    fun schemaFor(
        state: RepositoryIssueDetailUiState,
        commentDraft: String = "",
        onCommentDraftChange: ((String) -> Unit)? = null,
    ): PageSchema {
        val issue = state.issue
        val rows = buildList<com.Sunset.REN.GitHub.ui.layout.RowSchema> {
            when {
                state.isInitialLoad -> add(
                    row(
                        cell(
                            StateComponent(
                                id = "issue_detail.loading",
                                kind = StateKind.Loading,
                                message = "正在加载 Issue…",
                            ),
                        ),
                    ),
                )

                state.errorMessage != null && issue == null -> add(
                    row(
                        cell(
                            StateComponent(
                                id = "issue_detail.error",
                                kind = StateKind.Error,
                                message = "加载 Issue 失败",
                                detail = state.errorMessage,
                                retryAction = "issue_detail.retry",
                            ),
                        ),
                    ),
                )

                issue != null -> {
                    // —— Issue 头 ——
                    add(
                        row(
                            cell(
                                SectionHeaderComponent(
                                    id = "issue_detail.header",
                                    title = "#${issue.number} ${issue.title}",
                                    subtitle = "${state.owner}/${state.repo}",
                                ),
                            ),
                        ),
                    )
                    add(
                        row(
                            cell(
                                ItemComponent(
                                    id = "issue_detail.head",
                                    title = issue.title,
                                    subtitle = buildList {
                                        add(issue.authorLogin)
                                        formatDate(issue.createdAt)?.let(::add)
                                        add("${issue.commentCount} 条评论")
                                    }.joinToString(" · "),
                                    meta = issue.labels.take(6).map { it.name },
                                    icon = IconId.Issue,
                                    badge = stateLabel(issue.state),
                                ),
                            ),
                        ),
                    )
                    add(
                        row(
                            cell(
                                TextComponent(
                                    id = "issue_detail.body",
                                    text = issue.body.takeIf { it.isNotBlank() } ?: "暂无内容描述。",
                                    style = TextStyle.Body,
                                    color = TextColor.Primary,
                                ),
                            ),
                        ),
                    )
                    if (state.canToggleState || state.canEditLabels) {
                        val toggleText = if (issue.state == RepositoryIssuesUiState.ClosedState) "重新开启" else "关闭"
                        if (state.canToggleState && state.canEditLabels) {
                            add(
                                row(
                                    cell(
                                        ButtonComponent(
                                            id = "issue_detail.toggle_state",
                                            text = toggleText,
                                            kind = ButtonKind.Secondary,
                                            enabled = !state.isMutating,
                                            action = "issue_detail.toggle_state",
                                        ),
                                        span = 6,
                                    ),
                                    cell(
                                        ButtonComponent(
                                            id = "issue_detail.labels",
                                            text = "编辑标签",
                                            kind = ButtonKind.Secondary,
                                            enabled = !state.isMutating,
                                            action = "issue_detail.labels",
                                        ),
                                        span = 6,
                                    ),
                                ),
                            )
                        } else if (state.canToggleState) {
                            add(
                                row(
                                    cell(
                                        ButtonComponent(
                                            id = "issue_detail.toggle_state",
                                            text = toggleText,
                                            kind = ButtonKind.Secondary,
                                            enabled = !state.isMutating,
                                            action = "issue_detail.toggle_state",
                                        ),
                                    ),
                                ),
                            )
                        } else {
                            add(
                                row(
                                    cell(
                                        ButtonComponent(
                                            id = "issue_detail.labels",
                                            text = "编辑标签",
                                            kind = ButtonKind.Secondary,
                                            enabled = !state.isMutating,
                                            action = "issue_detail.labels",
                                        ),
                                    ),
                                ),
                            )
                        }
                    }

                    add(row(cell(SpacerComponent(id = "issue_detail.spacer.comments", heightDp = 8))))

                    // —— 评论 ——
                    add(
                        row(
                            cell(
                                SectionHeaderComponent(
                                    id = "issue_detail.comments_header",
                                    title = "评论",
                                    subtitle = "${state.comments.size} 条",
                                ),
                            ),
                        ),
                    )
                    if (state.comments.isEmpty()) {
                        add(
                            row(
                                cell(
                                    StateComponent(
                                        id = "issue_detail.comments_empty",
                                        kind = StateKind.Empty,
                                        message = "暂无评论",
                                    ),
                                ),
                            ),
                        )
                    } else {
                        add(
                            row(
                                cell(
                                    ListComponent(
                                        id = "issue_detail.comments",
                                        items = state.comments.map { comment ->
                                            commentFor(
                                                comment,
                                                canManage = state.canManageComment(comment.authorLogin),
                                            )
                                        },
                                    ),
                                ),
                            ),
                        )
                    }
                    if (state.hasMoreComments && state.comments.isNotEmpty()) {
                        add(
                            row(
                                cell(
                                    ButtonComponent(
                                        id = "issue_detail.load_more",
                                        text = if (state.isLoadingMoreComments) "加载中…" else "加载更多评论",
                                        kind = ButtonKind.Secondary,
                                        enabled = !state.isLoadingMoreComments,
                                        action = "issue_detail.load_more",
                                    ),
                                ),
                            ),
                        )
                    }
                    if (state.isSignedIn) {
                        add(
                            row(
                                cell(
                                    FieldComponent(
                                        id = "issue_detail.comment_field",
                                        value = commentDraft,
                                        hint = "写下评论…",
                                        singleLine = false,
                                        onChange = onCommentDraftChange,
                                    ),
                                ),
                            ),
                        )
                        add(
                            row(
                                cell(
                                    ButtonComponent(
                                        id = "issue_detail.send_comment",
                                        text = if (state.isMutating) "发送中…" else "发送评论",
                                        kind = ButtonKind.Primary,
                                        enabled = !state.isMutating && commentDraft.isNotBlank(),
                                        action = "issue_detail.send_comment",
                                    ),
                                ),
                            ),
                        )
                    }
                }
            }
        }
        return PageSchema(
            id = "issue_detail",
            columns = 12,
            scrollable = true,
            rows = rows,
        )
    }

    /** 评论 → 列表条目（可管理评论带删除行内动作）。 */
    private fun commentFor(comment: RepositoryIssueComment, canManage: Boolean): ItemComponent {
        return ItemComponent(
            id = "issue_detail.comment.${comment.id}",
            title = buildList {
                add(comment.authorLogin)
                formatDate(comment.createdAt)?.let(::add)
            }.joinToString(" · "),
            description = comment.body,
            actions = if (canManage) {
                listOf(
                    ItemAction(
                        id = "issue_detail.delete.${comment.id}",
                        icon = IconId.Close,
                        contentDescription = "删除评论",
                        action = "issue_detail.delete_comment.${comment.id}",
                    ),
                )
            } else {
                emptyList()
            },
        )
    }

    /** Issue 详情壳状态：保持仓库上下文（RepositorySections 分段导航）。 */
    fun shellState(
        fullName: String,
        number: Int,
        sections: List<RepositorySection>,
    ): ShellState = ShellState(
        title = if (number > 0) "#$number" else fullName.ifBlank { "Issue" },
        showBack = true,
        backAction = "shell.back",
        navBarMode = NavBarMode.RepositorySections,
        navItems = sections.map(RepositoryDetailPage::sectionNavItem),
        selectedNavId = RepositorySection.Issues.storageKey,
        contentKey = "issue_detail",
    )
}

/**
 * Issue 详情页垂直切片入口：壳 + 状态驱动 schema。
 * 分段导航与 IssuesPage/仓库详情共用（RepositoryDetailPage.sectionNavItem）。
 */
@Composable
fun IssueDetailPageContent(
    state: RepositoryIssueDetailUiState,
    sections: List<RepositorySection>,
    commentDraft: String = "",
    onCommentDraftChange: (String) -> Unit = {},
    onRetry: () -> Unit = {},
    onLoadMoreComments: () -> Unit = {},
    onToggleIssueState: () -> Unit = {},
    onShowLabelsPicker: () -> Unit = {},
    onCreateComment: () -> Unit = {},
    onDeleteComment: (Long) -> Unit = {},
    onOpenSection: (RepositorySection) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val fullName = if (state.owner.isNotBlank() && state.repo.isNotBlank()) {
        "${state.owner}/${state.repo}"
    } else {
        ""
    }
    val handleAction: (String) -> Unit = { action ->
        when {
            action == "shell.back" -> onBack()
            action == "issue_detail.retry" -> onRetry()
            action == "issue_detail.load_more" -> onLoadMoreComments()
            action == "issue_detail.toggle_state" -> onToggleIssueState()
            action == "issue_detail.labels" -> onShowLabelsPicker()
            action == "issue_detail.send_comment" -> onCreateComment()
            action.startsWith("issue_detail.delete_comment.") -> {
                action.removePrefix("issue_detail.delete_comment.").toLongOrNull()?.let(onDeleteComment)
            }
            action.startsWith("repo.section.") -> {
                val key = action.removePrefix("repo.section.")
                RepositorySection.fromStorageKey(key)?.let(onOpenSection)
            }
        }
    }
    AppShell(state = IssueDetailPage.shellState(fullName, state.number, sections), onAction = handleAction) {
        IssueDetailPage.schemaFor(state, commentDraft, onCommentDraftChange)
            .renderPage(handleAction)
    }
}