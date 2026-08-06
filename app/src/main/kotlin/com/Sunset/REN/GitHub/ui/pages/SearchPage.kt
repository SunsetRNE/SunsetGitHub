package com.Sunset.REN.GitHub.ui.pages

import androidx.compose.runtime.Composable
import com.Sunset.REN.GitHub.domain.repo.GitHubCodeSearchItem
import com.Sunset.REN.GitHub.domain.repo.GitHubIssueSearchItem
import com.Sunset.REN.GitHub.domain.repo.GitHubRepository
import com.Sunset.REN.GitHub.domain.repo.GitHubUserSearchItem
import com.Sunset.REN.GitHub.ui.layout.PageSchema
import com.Sunset.REN.GitHub.ui.layout.cell
import com.Sunset.REN.GitHub.ui.layout.row
import com.Sunset.REN.GitHub.ui.render.renderPage
import com.Sunset.REN.GitHub.ui.search.SearchResultItem
import com.Sunset.REN.GitHub.ui.search.SearchType
import com.Sunset.REN.GitHub.ui.search.SearchUiState
import com.Sunset.REN.GitHub.ui.schema.ButtonComponent
import com.Sunset.REN.GitHub.ui.schema.ButtonKind
import com.Sunset.REN.GitHub.ui.schema.FieldComponent
import com.Sunset.REN.GitHub.ui.schema.IconId
import com.Sunset.REN.GitHub.ui.schema.ItemComponent
import com.Sunset.REN.GitHub.ui.schema.ListComponent
import com.Sunset.REN.GitHub.ui.schema.SectionHeaderComponent
import com.Sunset.REN.GitHub.ui.schema.SkeletonComponent
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
 * 全局搜索页（Search）垂直切片（步骤 5：DialogFragment 类页面迁移）。
 *
 * 原 SearchFragment 为全屏对话框（覆盖式），壳映射为 Hidden 导航 + 返回关闭：
 * - 状态全态映射（7 态）：Idle / SignedOut / TypeSuggestion / Loading / Content / Empty / Error；
 * - 结果项 4 分发：Repo（语言·★·⑂）/ User（类型）/ Issue（#编号·状态·作者）/ Code（仓库·路径）；
 * - 分页：上一页 / 第 X 页 / 下一页（三 cell 一行）。
 * 路由前缀：search.submit / search.type.* / search.retry / search.prev|next /
 * search.open.repo|user|issue|code.* / search.dismiss。
 */
object SearchPage {

    /** 状态 → 页面 schema。 */
    fun schemaFor(
        state: SearchUiState,
        query: String = "",
        onQueryChange: ((String) -> Unit)? = null,
    ): PageSchema {
        val rows = buildList<com.Sunset.REN.GitHub.ui.layout.RowSchema> {
            add(
                row(
                    cell(
                        SectionHeaderComponent(
                            id = "search.header",
                            title = "搜索",
                            subtitle = "仓库 / 用户 / Issue / 代码",
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        FieldComponent(
                            id = "search.field",
                            value = query,
                            hint = "输入关键词搜索",
                            onChange = onQueryChange,
                        ),
                        span = 9,
                    ),
                    cell(
                        ButtonComponent(
                            id = "search.submit",
                            text = "搜索",
                            kind = ButtonKind.Primary,
                            enabled = query.isNotBlank(),
                            action = "search.submit",
                        ),
                        span = 3,
                    ),
                ),
            )
            add(row(cell(SpacerComponent(id = "search.spacer.top", heightDp = 4))))

            when (state) {
                SearchUiState.Idle -> add(
                    row(
                        cell(
                            TextComponent(
                                id = "search.idle",
                                text = "输入关键词开始搜索",
                                style = TextStyle.Meta,
                                color = TextColor.Muted,
                            ),
                        ),
                    ),
                )

                SearchUiState.SignedOut -> add(
                    row(
                        cell(
                            StateComponent(
                                id = "search.signed_out",
                                kind = StateKind.Empty,
                                message = "尚未登录",
                                detail = "登录后可使用全局搜索。",
                            ),
                        ),
                    ),
                )

                is SearchUiState.TypeSuggestion -> {
                    add(
                        row(
                            cell(
                                ListComponent(
                                    id = "search.suggestions",
                                    items = buildList {
                                        add(
                                            ItemComponent(
                                                id = "search.suggest.query",
                                                title = "搜索此关键词",
                                                subtitle = state.query,
                                                icon = IconId.Search,
                                                action = "search.type.Repositories",
                                            ),
                                        )
                                        state.types.forEach { type ->
                                            add(
                                                ItemComponent(
                                                    id = "search.suggest.${type.name}",
                                                    title = if (type.isAvailable) {
                                                        "可直接搜索：${type.displayName}"
                                                    } else {
                                                        "暂无搜索建议"
                                                    },
                                                    subtitle = type.name,
                                                    icon = IconId.Search,
                                                    action = "search.type.${type.name}",
                                                ),
                                            )
                                        }
                                    },
                                ),
                            ),
                        ),
                    )
                }

                is SearchUiState.Loading -> add(
                    row(
                        cell(
                            SkeletonComponent(
                                id = "search.skeleton",
                                rows = 5,
                                compact = true,
                            ),
                        ),
                    ),
                )

                is SearchUiState.Content -> {
                    val countText = buildResultCountText(state)
                    add(
                        row(
                            cell(
                                TextComponent(
                                    id = "search.count",
                                    text = countText,
                                    style = TextStyle.Body,
                                    color = TextColor.Secondary,
                                ),
                            ),
                        ),
                    )
                    add(
                        row(
                            cell(
                                ListComponent(
                                    id = "search.results",
                                    items = state.items.map { resultFor(it) },
                                ),
                            ),
                        ),
                    )
                    if (state.totalPages > 1 || state.hasPrevPage || state.hasNextPage) {
                        add(
                            row(
                                cell(
                                    ButtonComponent(
                                        id = "search.prev",
                                        text = "上一页",
                                        kind = ButtonKind.Secondary,
                                        enabled = state.hasPrevPage,
                                        action = "search.prev",
                                    ),
                                    span = 3,
                                ),
                                cell(
                                    TextComponent(
                                        id = "search.page",
                                        text = "第 ${state.currentPage} 页",
                                        style = TextStyle.Body,
                                        color = TextColor.Secondary,
                                    ),
                                    span = 6,
                                ),
                                cell(
                                    ButtonComponent(
                                        id = "search.next",
                                        text = "下一页",
                                        kind = ButtonKind.Secondary,
                                        enabled = state.hasNextPage,
                                        action = "search.next",
                                    ),
                                    span = 3,
                                ),
                            ),
                        )
                    }
                }

                is SearchUiState.Empty -> add(
                    row(
                        cell(
                            StateComponent(
                                id = "search.empty",
                                kind = StateKind.Empty,
                                message = "没有找到匹配结果",
                                detail = "换一个关键词，或切换到其他搜索类型重试。",
                                retryAction = "search.retry",
                            ),
                        ),
                    ),
                )

                is SearchUiState.Error -> add(
                    row(
                        cell(
                            StateComponent(
                                id = "search.error",
                                kind = StateKind.Error,
                                message = "搜索失败",
                                detail = state.message,
                                retryAction = "search.retry",
                            ),
                        ),
                    ),
                )
            }
        }
        return PageSchema(
            id = "search",
            columns = 12,
            scrollable = true,
            rows = rows,
        )
    }

    /** 统一结果项 → 列表条目（按类型分发渲染）。 */
    private fun resultFor(item: SearchResultItem): ItemComponent = when (item) {
        is SearchResultItem.Repo -> repoFor(item.repository)
        is SearchResultItem.User -> userFor(item.user)
        is SearchResultItem.Issue -> issueFor(item.issue)
        is SearchResultItem.Code -> codeFor(item.code)
    }

    private fun repoFor(repository: GitHubRepository): ItemComponent {
        val language = repository.languages.firstOrNull()?.name ?: repository.language
        return ItemComponent(
            id = "search.result.repo.${repository.fullName}",
            title = repository.fullName,
            description = repository.description?.takeIf { it.isNotBlank() },
            meta = listOfNotNull(
                language?.takeIf { it.isNotBlank() },
                "★ ${repository.stargazersCount}",
                "⑂ ${repository.forksCount}",
            ),
            icon = IconId.Folder,
            action = "search.open.repo.${repository.fullName}",
        )
    }

    private fun userFor(user: GitHubUserSearchItem): ItemComponent {
        val type = if (user.type?.lowercase() == "organization") "组织" else "用户"
        return ItemComponent(
            id = "search.result.user.${user.login}",
            title = user.login,
            subtitle = type,
            icon = IconId.Person,
            action = "search.open.user.${user.login}",
        )
    }

    private fun issueFor(issue: GitHubIssueSearchItem): ItemComponent {
        val kind = if (issue.isPullRequest) "拉取请求" else "议题"
        return ItemComponent(
            id = "search.result.issue.${issue.repositoryOwner}.${issue.repositoryName}.${issue.number}",
            title = issue.title,
            subtitle = buildList {
                add("${issue.repositoryFullName} #${issue.number}")
                add(kind)
                add(issue.state)
                add(issue.authorLogin)
            }.joinToString(" · "),
            icon = if (issue.isPullRequest) IconId.PullRequest else IconId.Issue,
            action = "search.open.issue.${issue.repositoryOwner}.${issue.repositoryName}.${issue.number}",
        )
    }

    private fun codeFor(code: GitHubCodeSearchItem): ItemComponent {
        return ItemComponent(
            id = "search.result.code.${code.repositoryFullName}.${code.path}",
            title = code.name,
            subtitle = "${code.repositoryFullName} · ${code.path}",
            icon = IconId.Code,
            action = "search.open.code.${code.repositoryOwner}.${code.repositoryName}.${code.path}",
        )
    }

    private fun buildResultCountText(state: SearchUiState.Content): String {
        val base = "找到 ${state.totalCount} 条结果"
        return if (state.totalCount > 1000) "$base · 显示前 1000 条结果" else base
    }

    /** 搜索页壳状态：全屏对话框语义（Hidden 导航 + 返回关闭）。 */
    fun shellState(): ShellState = ShellState(
        title = "搜索",
        showBack = true,
        backAction = "search.dismiss",
        navBarMode = NavBarMode.Hidden,
        contentKey = "search",
    )
}

/**
 * 搜索页垂直切片入口：壳 + 状态驱动 schema。
 * 原 DialogFragment 覆盖式语义由 Hidden 导航 + 返回关闭承载。
 */
@Composable
fun SearchPageContent(
    state: SearchUiState,
    query: String = "",
    onQueryChange: (String) -> Unit = {},
    onSearch: (String, SearchType) -> Unit = { _, _ -> },
    onDismiss: () -> Unit = {},
    onRetry: () -> Unit = {},
    onPrevPage: () -> Unit = {},
    onNextPage: () -> Unit = {},
    onOpenRepository: (GitHubRepository) -> Unit = {},
    onOpenUser: (GitHubUserSearchItem) -> Unit = {},
    onOpenIssue: (GitHubIssueSearchItem) -> Unit = {},
    onOpenCode: (GitHubCodeSearchItem) -> Unit = {},
) {
    val content = state as? SearchUiState.Content
    val handleAction: (String) -> Unit = { action ->
        when {
            action == "search.dismiss" -> onDismiss()
            action == "search.submit" -> onSearch(query, SearchType.Repositories)
            action == "search.retry" -> onRetry()
            action == "search.prev" -> onPrevPage()
            action == "search.next" -> onNextPage()
            action.startsWith("search.type.") -> {
                val typeName = action.removePrefix("search.type.")
                SearchType.entries.firstOrNull { it.name == typeName }?.let { type ->
                    onSearch(query, type)
                }
            }
            action.startsWith("search.open.repo.") -> {
                val fullName = action.removePrefix("search.open.repo.")
                content?.items?.mapNotNull { it as? SearchResultItem.Repo }
                    ?.firstOrNull { it.repository.fullName == fullName }
                    ?.let { onOpenRepository(it.repository) }
            }
            action.startsWith("search.open.user.") -> {
                val login = action.removePrefix("search.open.user.")
                content?.items?.mapNotNull { it as? SearchResultItem.User }
                    ?.firstOrNull { it.user.login == login }
                    ?.let { onOpenUser(it.user) }
            }
            action.startsWith("search.open.issue.") -> {
                val segments = action.removePrefix("search.open.issue.").split(".")
                if (segments.size >= 3) {
                    val number = segments[2].toIntOrNull() ?: -1
                    content?.items?.mapNotNull { it as? SearchResultItem.Issue }
                        ?.firstOrNull {
                            it.issue.repositoryOwner == segments[0] &&
                                it.issue.repositoryName == segments[1] &&
                                it.issue.number == number
                        }
                        ?.let { onOpenIssue(it.issue) }
                }
            }
            action.startsWith("search.open.code.") -> {
                val segments = action.removePrefix("search.open.code.").split(".")
                if (segments.size >= 3) {
                    val path = segments.drop(2).joinToString(".")
                    content?.items?.mapNotNull { it as? SearchResultItem.Code }
                        ?.firstOrNull {
                            it.code.repositoryOwner == segments[0] &&
                                it.code.repositoryName == segments[1] &&
                                it.code.path == path
                        }
                        ?.let { onOpenCode(it.code) }
                }
            }
        }
    }
    AppShell(state = SearchPage.shellState(), onAction = handleAction) {
        SearchPage.schemaFor(state, query = query, onQueryChange = onQueryChange)
            .renderPage(handleAction)
    }
}