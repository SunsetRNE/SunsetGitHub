package com.Sunset.REN.GitHub.ui.pages

import androidx.compose.runtime.Composable
import com.Sunset.REN.GitHub.domain.profile.GitHubContributionCalendar
import com.Sunset.REN.GitHub.domain.profile.GitHubUserProfile
import com.Sunset.REN.GitHub.domain.repo.GitHubRepository
import com.Sunset.REN.GitHub.ui.layout.PageSchema
import com.Sunset.REN.GitHub.ui.layout.cell
import com.Sunset.REN.GitHub.ui.layout.row
import com.Sunset.REN.GitHub.ui.render.renderPage
import com.Sunset.REN.GitHub.ui.profile.ProfileLanguageSummary
import com.Sunset.REN.GitHub.ui.profile.ProfileUiState
import com.Sunset.REN.GitHub.ui.schema.ButtonComponent
import com.Sunset.REN.GitHub.ui.schema.ButtonKind
import com.Sunset.REN.GitHub.ui.schema.IconId
import com.Sunset.REN.GitHub.ui.schema.ImageComponent
import com.Sunset.REN.GitHub.ui.schema.ImageSource
import com.Sunset.REN.GitHub.ui.schema.ItemComponent
import com.Sunset.REN.GitHub.ui.schema.LanguageBarComponent
import com.Sunset.REN.GitHub.ui.schema.LanguageSegment
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
import com.Sunset.REN.GitHub.ui.shell.shellNavItem

/**
 * 个人资料页（Profile）垂直切片（步骤 5：高频页面迁移，主 Tab 收官）。
 *
 * 与原 ProfileScreen 渲染结构对齐（5 区块）：
 * - Hero：远程头像 + 名字 + @login + bio + 资料 meta + 指标行 + 刷新状态 + GitHub 打开按钮；
 * - 概览：仓库统计 meta + 主语言；
 * - 语言分布：LanguageBarComponent 分布条 + 语言明细列表（top6）；
 * - 个人贡献：年度总数 + 分类统计 + 参与仓库；
 * - 个人仓库：分区标题 + 仓库列表（语言色条嵌套）。
 * 状态全态映射：Loading → 骨架；SignedOut/Error → StateComponent；Content → 五区块。
 */
object ProfilePage {

    /** 状态 → 页面 schema（渲染判断由字段驱动）。 */
    fun schemaFor(state: ProfileUiState): PageSchema {
        val rows = buildList<com.Sunset.REN.GitHub.ui.layout.RowSchema> {
            when (state) {
                ProfileUiState.Loading -> add(
                    row(
                        cell(
                            SkeletonComponent(
                                id = "profile.skeleton",
                                rows = 4,
                                compact = true,
                            ),
                        ),
                    ),
                )

                ProfileUiState.SignedOut -> add(
                    row(
                        cell(
                            StateComponent(
                                id = "profile.signed_out",
                                kind = StateKind.Empty,
                                message = "尚未登录",
                                detail = "登录后可查看个人资料。",
                            ),
                        ),
                    ),
                )

                is ProfileUiState.Error -> add(
                    row(
                        cell(
                            StateComponent(
                                id = "profile.error",
                                kind = StateKind.Error,
                                message = "个人资料加载失败",
                                detail = state.message,
                                retryAction = "profile.retry",
                            ),
                        ),
                    ),
                )

                is ProfileUiState.Content -> contentRows(state)
            }
        }
        return PageSchema(
            id = "profile",
            columns = 12,
            scrollable = true,
            rows = rows,
        )
    }

    /** Content 态五区块：Hero / 概览 / 语言分布 / 个人贡献 / 个人仓库。 */
    private fun MutableList<com.Sunset.REN.GitHub.ui.layout.RowSchema>.contentRows(
        state: ProfileUiState.Content,
    ) {
        val profile = state.profile
        add(
            row(
                cell(
                    SectionHeaderComponent(
                        id = "profile.header",
                        title = "我的",
                        subtitle = "@${profile.login}",
                    ),
                ),
            ),
        )

        // —— Hero：头像 + 名字 + @login + bio ——
        add(
            row(
                cell(
                    ImageComponent(
                        id = "profile.avatar",
                        source = profile.avatarUrl
                            ?.takeIf { it.isNotBlank() }
                            ?.let { ImageSource.Remote(it) }
                            ?: ImageSource.Icon(IconId.Person),
                        sizeDp = 66,
                    ),
                    span = 3,
                ),
                cell(
                    TextComponent(
                        id = "profile.name",
                        text = profile.name?.takeIf { it.isNotBlank() } ?: profile.login,
                        style = TextStyle.Title,
                        color = TextColor.Primary,
                        maxLines = 1,
                        ellipsis = true,
                    ),
                    span = 9,
                ),
            ),
        )
        add(
            row(
                cell(
                    TextComponent(
                        id = "profile.login",
                        text = "@${profile.login}",
                        style = TextStyle.Body,
                        color = TextColor.Secondary,
                    ),
                    span = 9,
                ),
            ),
        )
        add(
            row(
                cell(
                    TextComponent(
                        id = "profile.bio",
                        text = profile.bio?.takeIf { it.isNotBlank() } ?: "暂无个人简介",
                        style = TextStyle.Body,
                        color = TextColor.Primary,
                    ),
                ),
            ),
        )

        // —— Hero meta：公司/地点/博客/邮箱/𝕏 ——
        val profileMeta = buildProfileMeta(profile)
        if (profileMeta.isNotBlank()) {
            add(
                row(
                    cell(
                        TextComponent(
                            id = "profile.meta",
                            text = profileMeta,
                            style = TextStyle.Body,
                            color = TextColor.Secondary,
                            maxLines = 3,
                            ellipsis = true,
                        ),
                    ),
                ),
            )
        }

        // —— 指标行：粉丝/关注/仓库/Gists ——
        add(
            row(
                cell(
                    TextComponent(
                        id = "profile.metrics",
                        text = buildList {
                            add("粉丝 ${compactCount(profile.followers)}")
                            add("关注 ${compactCount(profile.following)}")
                            add("仓库 ${compactCount(profile.publicRepos)}")
                            add("Gists ${compactCount(profile.publicGists)}")
                        }.joinToString(" · "),
                        style = TextStyle.Body,
                        color = TextColor.Secondary,
                    ),
                ),
            ),
        )

        // —— 刷新状态行 ——
        val status = when {
            state.refreshError != null -> "刷新失败：${state.refreshError}"
            state.isRefreshingFromCache -> "正在刷新缓存…"
            else -> "资料已保存"
        }
        add(
            row(
                cell(
                    TextComponent(
                        id = "profile.status",
                        text = status,
                        style = TextStyle.Meta,
                        color = if (state.refreshError != null) TextColor.Danger else TextColor.Muted,
                    ),
                ),
            ),
        )

        // —— GitHub 打开按钮 ——
        if (profile.htmlUrl.isNotBlank()) {
            add(
                row(
                    cell(
                        ButtonComponent(
                            id = "profile.open_github",
                            text = "在 GitHub 打开",
                            kind = ButtonKind.Primary,
                            action = "profile.open_github",
                        ),
                    ),
                ),
            )
        }

        add(row(cell(SpacerComponent(id = "profile.spacer.overview", heightDp = 8))))

        // —— 概览 ——
        add(
            row(
                cell(
                    SectionHeaderComponent(
                        id = "profile.overview_header",
                        title = "概览",
                    ),
                ),
            ),
        )
        add(
            row(
                cell(
                    TextComponent(
                        id = "profile.overview",
                        text = buildList {
                            add("源仓库 ${compactCount(state.sourceRepositoryCount)}")
                            add("Fork ${compactCount(state.forkRepositoryCount)}")
                            add("已归档 ${compactCount(state.archivedRepositoryCount)}")
                            add("Stars ${compactCount(state.totalStars)}")
                            add("Forks ${compactCount(state.totalForks)}")
                            add("打开的议题 ${compactCount(state.totalOpenIssues)}")
                        }.joinToString(" · "),
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
                        id = "profile.primary_language",
                        text = "主语言：${state.primaryLanguage ?: "暂无语言统计"}",
                        style = TextStyle.Body,
                        color = TextColor.Secondary,
                    ),
                ),
            ),
        )

        add(row(cell(SpacerComponent(id = "profile.spacer.lang", heightDp = 8))))

        // —— 语言分布 ——
        add(
            row(
                cell(
                    SectionHeaderComponent(
                        id = "profile.lang_header",
                        title = "语言分布",
                    ),
                ),
            ),
        )
        if (state.languageSummaries.isEmpty()) {
            add(
                row(
                    cell(
                        TextComponent(
                            id = "profile.lang_empty",
                            text = "暂无语言统计",
                            style = TextStyle.Body,
                            color = TextColor.Secondary,
                        ),
                    ),
                ),
            )
        } else {
            add(
                row(
                    cell(
                        LanguageBarComponent(
                            id = "profile.lang_bar",
                            segments = state.languageSummaries.take(6).map { language ->
                                LanguageSegment(
                                    name = language.name,
                                    percentage = language.percentage.coerceIn(0, 100).toFloat(),
                                )
                            },
                        ),
                    ),
                ),
            )
            add(
                row(
                    cell(
                        ListComponent(
                            id = "profile.lang_list",
                            items = state.languageSummaries.take(6).map { languageFor(it) },
                        ),
                    ),
                ),
            )
        }

        add(row(cell(SpacerComponent(id = "profile.spacer.contrib", heightDp = 8))))

        // —— 个人贡献 ——
        add(
            row(
                cell(
                    SectionHeaderComponent(
                        id = "profile.contrib_header",
                        title = "个人贡献",
                    ),
                ),
            ),
        )
        contributionRows(state)

        add(row(cell(SpacerComponent(id = "profile.spacer.repos", heightDp = 8))))

        // —— 个人仓库 ——
        add(
            row(
                cell(
                    SectionHeaderComponent(
                        id = "profile.repos_header",
                        title = "个人仓库",
                        subtitle = repositoryListSummary(state),
                    ),
                ),
            ),
        )
        if (state.profileRepositories.isEmpty()) {
            add(
                row(
                    cell(
                        StateComponent(
                            id = "profile.repos_empty",
                            kind = StateKind.Empty,
                            message = "暂无可展示仓库",
                        ),
                    ),
                ),
            )
        } else {
            add(
                row(
                    cell(
                        ListComponent(
                            id = "profile.repos_list",
                            items = state.profileRepositories.map { repositoryFor(it) },
                        ),
                    ),
                ),
            )
        }
    }

    /** 个人贡献区块：总数 + 分类统计 + 参与仓库。 */
    private fun MutableList<com.Sunset.REN.GitHub.ui.layout.RowSchema>.contributionRows(
        state: ProfileUiState.Content,
    ) {
        val calendar: GitHubContributionCalendar? = state.contributionCalendar
        val error: String? = state.contributionError
        when {
            calendar != null -> {
                add(
                    row(
                        cell(
                            TextComponent(
                                id = "profile.contrib_total",
                                text = "${calendar.year ?: "过去一年"} 有 ${compactCount(calendar.totalContributions)} 次贡献",
                                style = TextStyle.Title,
                                color = TextColor.Primary,
                            ),
                        ),
                    ),
                )
                val overview = calendar.overview
                if (overview != null) {
                    add(
                        row(
                            cell(
                                TextComponent(
                                    id = "profile.contrib_metrics",
                                    text = buildList {
                                        add("提交 ${compactCount(overview.commitCount)}")
                                        add("议题 ${compactCount(overview.issueCount)}")
                                        add("PR ${compactCount(overview.pullRequestCount)}")
                                        add("代码审查 ${compactCount(overview.pullRequestReviewCount)}")
                                    }.joinToString(" · "),
                                    style = TextStyle.Body,
                                    color = TextColor.Secondary,
                                ),
                            ),
                        ),
                    )
                    val repos = overview.repositoryNames.take(4).joinToString().ifBlank { "暂无活动概览" }
                    add(
                        row(
                            cell(
                                TextComponent(
                                    id = "profile.contrib_repos",
                                    text = "参与了 $repos",
                                    style = TextStyle.Body,
                                    color = TextColor.Muted,
                                    maxLines = 2,
                                    ellipsis = true,
                                ),
                            ),
                        ),
                    )
                }
            }

            error != null -> add(
                row(
                    cell(
                        TextComponent(
                            id = "profile.contrib_error",
                            text = "贡献数据加载失败：$error",
                            style = TextStyle.Body,
                            color = TextColor.Secondary,
                        ),
                    ),
                ),
            )

            else -> add(
                row(
                    cell(
                        TextComponent(
                            id = "profile.contrib_empty",
                            text = "${state.profile.login} 在此期间暂无活动。",
                            style = TextStyle.Body,
                            color = TextColor.Secondary,
                        ),
                    ),
                ),
            )
        }
    }

    /** 语言摘要 → 列表条目。 */
    private fun languageFor(language: ProfileLanguageSummary): ItemComponent {
        return ItemComponent(
            id = "profile.lang.${language.name}",
            title = language.name,
            meta = listOf("${language.percentage}%", "${language.repositoryCount} 个仓库"),
            trailing = "${language.percentage}%",
        )
    }

    /** 仓库 → 列表条目（对齐 Dashboard.itemFor 与 Rust RepositorySummary）。 */
    private fun repositoryFor(repository: GitHubRepository): ItemComponent {
        return ItemComponent(
            id = "profile.repo.${repository.fullName}",
            title = repository.name.ifBlank { repository.fullName },
            description = repository.description?.takeIf { it.isNotBlank() },
            meta = repositoryMeta(repository),
            languageBar = LanguageBarComponent(
                id = "profile.repo.lang.${repository.fullName}",
                segments = repository.languages.map { language ->
                    LanguageSegment(name = language.name, percentage = language.percentage.toFloat())
                },
                fallback = repository.language,
            ),
            icon = if (repository.archived) IconId.Archive else IconId.Folder,
            badge = repositoryStatusLabel(repository),
            trailing = repository.updatedAt?.take(10)?.takeIf { it.length == 10 },
            action = "profile.repo.open.${repository.fullName}",
        )
    }

    private fun repositoryMeta(repository: GitHubRepository): List<String> = buildList {
        primaryLanguageName(repository)?.let(::add)
        repository.ownerLogin.takeIf { it.isNotBlank() }?.let(::add)
        repository.defaultBranch.takeIf { it.isNotBlank() }?.let(::add)
        add("★ ${compactCount(repository.stargazersCount)}")
        if (repository.forksCount > 0) add("Fork ${compactCount(repository.forksCount)}")
        if (repository.openIssuesCount > 0) add("Issue ${compactCount(repository.openIssuesCount)}")
    }

    private fun repositoryStatusLabel(repository: GitHubRepository): String? = when {
        repository.archived -> "Archived"
        repository.fork -> "Fork"
        repository.isPrivate -> "Private"
        else -> null
    }

    private fun primaryLanguageName(repository: GitHubRepository): String? {
        return repository.languages.firstOrNull()?.name?.takeIf { it.isNotBlank() }
            ?: repository.language?.takeIf { it.isNotBlank() }
    }

    private fun repositoryListSummary(state: ProfileUiState.Content): String = buildList {
        add("最近更新 ${state.profileRepositories.size} 个公开仓库")
        if (state.sourceRepositoryCount > 0) add("原创 ${state.sourceRepositoryCount}")
        if (state.forkRepositoryCount > 0) add("Fork ${state.forkRepositoryCount}")
        if (state.archivedRepositoryCount > 0) add("归档 ${state.archivedRepositoryCount}")
    }.joinToString(" · ")

    private fun buildProfileMeta(profile: GitHubUserProfile): String {
        return listOfNotNull(
            profile.company?.takeIf { it.isNotBlank() }?.let { "🏢 $it" },
            profile.location?.takeIf { it.isNotBlank() }?.let { "📍 $it" },
            profile.blog?.takeIf { it.isNotBlank() }?.let { "🔗 $it" },
            profile.email?.takeIf { it.isNotBlank() }?.let { "✉️ $it" },
            profile.twitterUsername?.takeIf { it.isNotBlank() }?.let { "𝕏 @$it" },
        ).joinToString(" · ")
    }

    private fun compactCount(count: Int): String = when {
        count >= 1_000_000 -> "${count / 1_000_000}M"
        count >= 1_000 -> "${count / 1_000}K"
        else -> count.toString()
    }

    /** 个人资料页默认壳状态：主 Tab 导航（选中"我的"）。 */
    fun shellState(): ShellState = ShellState(
        title = "我的",
        navBarMode = NavBarMode.Main,
        navItems = listOf(
            shellNavItem(id = "home", label = "主页", icon = IconId.Home),
            shellNavItem(id = "dashboard", label = "仓库", icon = IconId.Folder),
            shellNavItem(id = "notifications", label = "通知", icon = IconId.Bell),
            shellNavItem(id = "settings", label = "设置", icon = IconId.Settings, action = "nav.settings"),
        ),
        selectedNavId = "home",
        contentKey = "profile",
    )
}

/**
 * 个人资料页垂直切片入口：壳 + 状态驱动 schema。
 * 同一路由同时服务壳（主导航切换）与页面组件（重试/打开 GitHub/打开仓库）。
 */
@Composable
fun ProfilePageContent(
    state: ProfileUiState,
    onRetry: () -> Unit = {},
    onOpenGitHub: (String) -> Unit = {},
    onOpenRepository: (GitHubRepository) -> Unit = {},
    onOpenHome: () -> Unit = {},
    onOpenDashboard: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
) {
    val profile = (state as? ProfileUiState.Content)?.profile
    val repositories = (state as? ProfileUiState.Content)?.profileRepositories.orEmpty()
    val handleAction: (String) -> Unit = { action ->
        when {
            action == "profile.retry" -> onRetry()
            action == "profile.open_github" -> profile?.htmlUrl?.let(onOpenGitHub)
            action == "nav.home" -> onOpenHome()
            action == "nav.dashboard" -> onOpenDashboard()
            action == "nav.notifications" -> onOpenNotifications()
            action.startsWith("profile.repo.open.") -> {
                val fullName = action.removePrefix("profile.repo.open.")
                repositories.firstOrNull { it.fullName == fullName }?.let(onOpenRepository)
            }
        }
    }
    AppShell(state = ProfilePage.shellState(), onAction = handleAction) {
        ProfilePage.schemaFor(state).renderPage(handleAction)
    }
}