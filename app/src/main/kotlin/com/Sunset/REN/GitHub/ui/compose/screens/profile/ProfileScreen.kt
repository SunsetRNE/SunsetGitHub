package com.Sunset.REN.GitHub.ui.compose.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.Sunset.REN.GitHub.domain.profile.GitHubContributionCalendar
import com.Sunset.REN.GitHub.domain.profile.GitHubUserProfile
import com.Sunset.REN.GitHub.domain.repo.GitHubRepository
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens
import com.Sunset.REN.GitHub.ui.compose.components.SunsetCard
import com.Sunset.REN.GitHub.ui.compose.components.SunsetPrimaryButton
import com.Sunset.REN.GitHub.ui.compose.components.SunsetSecondaryButton
import com.Sunset.REN.GitHub.ui.profile.ProfileLanguageSummary
import com.Sunset.REN.GitHub.ui.profile.ProfileUiState

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onRetry: () -> Unit,
    onOpenGitHub: (String) -> Unit,
    onOpenRepository: (GitHubRepository) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    LazyColumn(
        modifier = modifier.background(colors.canvas),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        when (state) {
            ProfileUiState.Loading -> item { StatePanel(message = "正在加载个人资料…") }
            ProfileUiState.SignedOut -> item { StatePanel(message = "登录后可查看个人资料。") }
            is ProfileUiState.Error -> item {
                StatePanel(
                    message = "个人资料加载失败：${state.message}",
                    actionLabel = "重试",
                    onAction = onRetry
                )
            }
            is ProfileUiState.Content -> {
                item { ProfileHero(state = state, onOpenGitHub = onOpenGitHub) }
                item { ProfileRepositoryOverview(state) }
                item { ProfileLanguageDistribution(state.languageSummaries) }
                item { ProfileContributionSummary(state.profile.login, state.contributionCalendar, state.contributionError) }
                item {
                    ProfileRepositoryListSection(
                        state = state,
                        onOpenRepository = onOpenRepository
                    )
                }
            }
        }
    }
}

@Composable
private fun StatePanel(message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    val colors = SunsetGitHubThemeTokens.colors
    SunsetCard(Modifier.fillMaxWidth()) {
        Text(message, color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
        if (actionLabel != null && onAction != null) {
            SunsetSecondaryButton(text = actionLabel, onClick = onAction, modifier = Modifier.padding(top = 12.dp))
        }
    }
}

@Composable
private fun ProfileHero(state: ProfileUiState.Content, onOpenGitHub: (String) -> Unit) {
    val colors = SunsetGitHubThemeTokens.colors
    val profile = state.profile
    HeroSurface {
        Row(verticalAlignment = Alignment.Top) {
            Avatar(login = profile.login, modifier = Modifier.size(66.dp))
            Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                Text(
                    text = profile.name?.takeIf { it.isNotBlank() } ?: profile.login,
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text("@${profile.login}", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                val bio = profile.bio?.takeIf { it.isNotBlank() } ?: "暂无个人简介"
                Text(
                    text = bio,
                    modifier = Modifier.padding(top = 12.dp),
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        ProfileMetaBlock(profile)

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompactMetric(value = compactCount(profile.followers), label = "粉丝", modifier = Modifier.weight(1f))
            CompactMetric(value = compactCount(profile.following), label = "关注", modifier = Modifier.weight(1f))
            CompactMetric(value = compactCount(profile.publicRepos), label = "仓库", modifier = Modifier.weight(1f))
            CompactMetric(value = compactCount(profile.publicGists), label = "Gists", modifier = Modifier.weight(1f))
        }

        val status = when {
            state.refreshError != null -> "刷新失败：${state.refreshError}"
            state.isRefreshingFromCache -> "正在刷新缓存…"
            else -> "资料已保存"
        }
        Text(status, color = colors.textMuted, style = MaterialTheme.typography.bodySmall)

        if (profile.htmlUrl.isNotBlank()) {
            SunsetPrimaryButton(
                text = "在 GitHub 打开",
                onClick = { onOpenGitHub(profile.htmlUrl) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun HeroSurface(content: @Composable ColumnScope.() -> Unit) {
    val colors = SunsetGitHubThemeTokens.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )
    }
}

@Composable
private fun Avatar(login: String, modifier: Modifier = Modifier) {
    val colors = SunsetGitHubThemeTokens.colors
    Box(
        modifier = modifier.clip(CircleShape).background(colors.accentSoft),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = login.take(1).uppercase(),
            color = colors.accent,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ProfileMetaBlock(profile: GitHubUserProfile) {
    val colors = SunsetGitHubThemeTokens.colors
    val meta = buildProfileMeta(profile)
    Text(
        text = meta,
        color = colors.textSecondary,
        style = MaterialTheme.typography.bodySmall,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun CompactMetric(value: String, label: String, modifier: Modifier = Modifier) {
    val colors = SunsetGitHubThemeTokens.colors
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = colors.chipBackground
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)) {
            Text(value, color = colors.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, color = colors.textSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

@Composable
private fun ProfileRepositoryOverview(state: ProfileUiState.Content) {
    SectionCard(title = "概览") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OverviewMetric("源仓库", compactCount(state.sourceRepositoryCount))
            OverviewMetric("Fork", compactCount(state.forkRepositoryCount))
            OverviewMetric("已归档", compactCount(state.archivedRepositoryCount))
            OverviewMetric("Stars", compactCount(state.totalStars))
            OverviewMetric("Forks", compactCount(state.totalForks))
            OverviewMetric("打开的议题", compactCount(state.totalOpenIssues))
        }
        Text(
            text = "主语言：${state.primaryLanguage ?: "暂无语言统计"}",
            modifier = Modifier.padding(top = 10.dp),
            color = SunsetGitHubThemeTokens.colors.textSecondary,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun OverviewMetric(label: String, value: String) {
    val colors = SunsetGitHubThemeTokens.colors
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Column(modifier = Modifier.width(92.dp).padding(horizontal = 11.dp, vertical = 10.dp)) {
            Text(value, color = colors.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, color = colors.textMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ProfileLanguageDistribution(languages: List<ProfileLanguageSummary>) {
    SectionCard(title = "语言分布") {
        if (languages.isEmpty()) {
            Text("暂无语言统计", color = SunsetGitHubThemeTokens.colors.textSecondary, style = MaterialTheme.typography.bodySmall)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                languages.take(6).forEachIndexed { index, language ->
                    LanguageRow(language = language, accent = languageAccent(index))
                }
            }
        }
    }
}

@Composable
private fun LanguageRow(language: ProfileLanguageSummary, accent: Color) {
    val colors = SunsetGitHubThemeTokens.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(language.name, modifier = Modifier.weight(1f), color = colors.textPrimary, style = MaterialTheme.typography.bodyMedium)
            Text("${language.percentage}% · ${language.repositoryCount} 个仓库", color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
        }
        LinearProgressIndicator(
            progress = { (language.percentage.coerceIn(0, 100) / 100f) },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
            color = accent,
            trackColor = colors.chipBackground
        )
    }
}

@Composable
private fun ProfileContributionSummary(login: String, calendar: GitHubContributionCalendar?, error: String?) {
    val colors = SunsetGitHubThemeTokens.colors
    val overview = calendar?.overview
    SectionCard(title = "个人贡献") {
        when {
            calendar != null -> {
                Text(
                    text = "${calendar.year ?: "过去一年"} 有 ${compactCount(calendar.totalContributions)} 次贡献",
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                if (overview != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ContributionMetric("提交", compactCount(overview.commitCount), Modifier.weight(1f))
                        ContributionMetric("议题", compactCount(overview.issueCount), Modifier.weight(1f))
                        ContributionMetric("PR", compactCount(overview.pullRequestCount), Modifier.weight(1f))
                        ContributionMetric("代码审查", compactCount(overview.pullRequestReviewCount), Modifier.weight(1f))
                    }
                    val repos = overview.repositoryNames.take(4).joinToString().ifBlank { "暂无活动概览" }
                    Text(
                        text = "参与了 $repos",
                        modifier = Modifier.padding(top = 12.dp),
                        color = colors.textSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            error != null -> Text("贡献数据加载失败：$error", color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
            else -> Text("$login 在此期间暂无活动。", color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ContributionMetric(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = SunsetGitHubThemeTokens.colors
    Column(modifier = modifier) {
        Text(value, color = colors.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, color = colors.textMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = SunsetGitHubThemeTokens.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = colors.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String? = null) {
    val colors = SunsetGitHubThemeTokens.colors
    Column(modifier = Modifier.padding(top = 2.dp, start = 2.dp, end = 2.dp)) {
        Text(title, color = colors.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (!subtitle.isNullOrBlank()) {
            Text(subtitle, modifier = Modifier.padding(top = 2.dp), color = colors.textMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}
@Composable
private fun ProfileRepositoryListSection(
    state: ProfileUiState.Content,
    onOpenRepository: (GitHubRepository) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(
            title = "个人仓库",
            subtitle = state.profileRepositoryListSummary()
        )
        if (state.profileRepositories.isEmpty()) {
            StatePanel(message = "暂无可展示仓库")
        } else {
            state.profileRepositories.forEach { repository ->
                ProfileRepositoryListCard(
                    repository = repository,
                    onClick = { onOpenRepository(repository) }
                )
            }
        }
    }
}

@Composable
private fun ProfileRepositoryListCard(repository: GitHubRepository, onClick: () -> Unit) {
    val colors = SunsetGitHubThemeTokens.colors
    val primaryLanguage = repository.primaryLanguageName()
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = colors.subtleBackground,
        border = BorderStroke(1.dp, colors.divider)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RepositoryLanguageRail(color = languageAccent(repository.languageAccentSeed()))
            Column(modifier = Modifier.weight(1f).padding(start = 9.dp, end = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = repository.name.ifBlank { repository.fullName },
                        modifier = Modifier.weight(1f),
                        color = colors.textPrimary,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    repository.profileRepositoryStatusLabel()?.let { status ->
                        Text(
                            text = status,
                            color = colors.textMuted,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }
                }
                repository.description?.takeIf { it.isNotBlank() }?.let { description ->
                    Text(
                        text = description,
                        modifier = Modifier.padding(top = 2.dp),
                        color = colors.textSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = repository.profileRepositoryMeta(primaryLanguage),
                    modifier = Modifier.padding(top = 3.dp),
                    color = colors.textMuted,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}


@Composable
private fun RepositoryLanguageRail(color: Color) {
    Box(
        modifier = Modifier
            .width(3.dp)
            .height(42.dp)
            .clip(CircleShape)
            .background(color)
    )
}

private fun ProfileUiState.Content.profileRepositoryListSummary(): String {
    return buildList {
        add("最近更新 ${profileRepositories.size} 个公开仓库")
        if (sourceRepositoryCount > 0) add("原创 $sourceRepositoryCount")
        if (forkRepositoryCount > 0) add("Fork $forkRepositoryCount")
        if (archivedRepositoryCount > 0) add("归档 $archivedRepositoryCount")
    }.joinToString(" · ")
}

private fun GitHubRepository.primaryLanguageName(): String? {
    return languages.firstOrNull()?.name ?: language?.takeIf { it.isNotBlank() }
}

private fun GitHubRepository.profileRepositoryStatusLabel(): String? {
    return when {
        archived -> "Archived"
        fork -> "Fork"
        isPrivate -> "Private"
        else -> null
    }
}

private fun GitHubRepository.languageAccentSeed(): Int {
    return primaryLanguageName()
        ?.fold(0) { acc, char -> acc + char.code }
        ?: name.length
}

private fun GitHubRepository.profileRepositoryMeta(primaryLanguage: String?): String {
    return buildList {
        primaryLanguage?.let(::add)
        ownerLogin.takeIf { it.isNotBlank() }?.let(::add)
        defaultBranch.takeIf { it.isNotBlank() }?.let(::add)
        add("★ ${compactCount(stargazersCount)}")
        if (forksCount > 0) add("Fork ${compactCount(forksCount)}")
        if (openIssuesCount > 0) add("Issue ${compactCount(openIssuesCount)}")
    }.joinToString(" · ")
}

private fun buildProfileMeta(profile: GitHubUserProfile): String {
    return listOfNotNull(
        profile.company?.takeIf { it.isNotBlank() }?.let { "🏢 $it" },
        profile.location?.takeIf { it.isNotBlank() }?.let { "📍 $it" },
        profile.blog?.takeIf { it.isNotBlank() }?.let { "🔗 $it" },
        profile.email?.takeIf { it.isNotBlank() }?.let { "✉️ $it" },
        profile.twitterUsername?.takeIf { it.isNotBlank() }?.let { "𝕏 @$it" }
    ).joinToString(" · ").ifBlank { "暂无个人资料信息" }
}

private fun compactCount(count: Int): String {
    return when {
        count >= 1_000_000 -> "${count / 1_000_000}M"
        count >= 1_000 -> "${count / 1_000}K"
        else -> count.toString()
    }
}

private fun languageAccent(index: Int): Color {
    val palette = listOf(
        Color(0xFF0969DA),
        Color(0xFF1A7F37),
        Color(0xFF8250DF),
        Color(0xFFBC4C00),
        Color(0xFFCF222E),
        Color(0xFF57606A)
    )
    return palette[index.mod(palette.size)]
}