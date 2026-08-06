package com.Sunset.REN.GitHub.ui.compose.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.repo.GitHubRepository
import com.Sunset.REN.GitHub.domain.repo.RepositoryLanguage
import com.Sunset.REN.GitHub.domain.repo.RepositoryLocalState
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens
import com.Sunset.REN.GitHub.ui.compose.components.SunsetEmptyState
import com.Sunset.REN.GitHub.ui.compose.components.SunsetErrorState
import com.Sunset.REN.GitHub.ui.compose.components.SunsetPrimaryButton
import com.Sunset.REN.GitHub.ui.repo.RepositoriesUiState

@Composable
fun DashboardScreen(
    state: RepositoriesUiState,
    onOpenRepository: (GitHubRepository) -> Unit,
    onTogglePinned: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenHome: () -> Unit,
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp = 0.dp
) {
    val colors = SunsetGitHubThemeTokens.colors
    var query by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(DashboardSortMode.Default) }
    var showSortMenu by remember { mutableStateOf(false) }
    val content = state as? RepositoriesUiState.Content
    val visibleRepositories = content?.repositories
        ?.filter { repository -> repository.matches(query) }
        ?.sortedForDashboard(sortMode, content.repositoryLocalStates)
        .orEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompactRepositorySearchField(
                query = query,
                onQueryChange = { query = it },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(colors.surface, RoundedCornerShape(12.dp))
                        .border(1.dp, colors.border, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        enabled = visibleRepositories.size > 1,
                        onClick = { showSortMenu = true }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_menu_24),
                            contentDescription = "排序仓库",
                            modifier = Modifier.size(22.dp),
                            tint = if (visibleRepositories.size > 1) colors.textPrimary else colors.textMuted
                        )
                    }
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                    shape = RoundedCornerShape(12.dp),
                    containerColor = colors.surface,
                    tonalElevation = 0.dp,
                    shadowElevation = 4.dp,
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    DashboardSortMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(mode.labelResId),
                                    color = colors.textPrimary
                                )
                            },
                            onClick = {
                                sortMode = mode
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
        }

        when (state) {
            RepositoriesUiState.Loading -> DashboardSkeletonList(
                modifier = Modifier.weight(1f),
                bottomContentPadding = bottomContentPadding
            )
            RepositoriesUiState.SignedOut -> SunsetEmptyState(
                modifier = Modifier.weight(1f),
                title = "尚未登录",
                description = "请先完成 GitHub 登录后再查看仓库列表。",
                action = { SunsetPrimaryButton("前往首页", onOpenHome) }
            )
            RepositoriesUiState.Empty -> SunsetEmptyState(
                modifier = Modifier.weight(1f),
                title = "暂无仓库",
                description = "当前账号暂时没有可显示的仓库。",
                action = { SunsetPrimaryButton(stringResource(R.string.dashboard_refresh_repositories), onRefresh) }
            )
            is RepositoriesUiState.Error -> SunsetErrorState(
                modifier = Modifier.weight(1f),
                title = "加载仓库失败",
                message = state.message,
                action = {
                    SunsetPrimaryButton(
                        text = stringResource(R.string.dashboard_refresh_repositories),
                        onClick = onRefresh
                    )
                }
            )
            is RepositoriesUiState.Content -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = bottomContentPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (state.isRefreshing && visibleRepositories.isEmpty()) {
                        items(DashboardSkeletonItemCount) { index ->
                            RepositoryDashboardSkeletonCard(compact = index >= 3)
                        }
                    }
                    if (visibleRepositories.isEmpty() && !state.isRefreshing) {
                        item {
                            SunsetEmptyState(
                                title = "没有匹配的仓库",
                                description = "可尝试换一个关键词，或加载更多仓库后继续筛选。"
                            )
                        }
                    } else if (visibleRepositories.isNotEmpty()) {
                        items(visibleRepositories, key = { it.id }) { repository ->
                            RepositoryDashboardCard(
                                repository = repository,
                                localState = state.repositoryLocalStates[repository.fullName] ?: RepositoryLocalState(),
                                onOpen = { onOpenRepository(repository) },
                                onTogglePinned = { onTogglePinned(repository.fullName) },
                                onToggleFavorite = { onToggleFavorite(repository.fullName) }
                            )
                        }
                    }
                    state.loadMoreError?.let { message ->
                        item { Text("加载更多失败：$message", color = colors.danger, style = MaterialTheme.typography.bodySmall) }
                    }
                    if (state.isLoadingMore) {
                        item { LoadMoreSkeletonButton() }
                    } else if (state.canLoadMore) {
                        item {
                            SunsetPrimaryButton(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                text = stringResource(R.string.dashboard_load_more_repositories),
                                enabled = true,
                                onClick = onLoadMore
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardSkeletonList(
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp = 0.dp
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = bottomContentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(DashboardSkeletonItemCount) { index ->
            RepositoryDashboardSkeletonCard(compact = index >= 3)
        }
        item { LoadMoreSkeletonButton() }
    }
}

@Composable
private fun RepositoryDashboardSkeletonCard(
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, colors.divider),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 11.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    SkeletonBlock(widthFraction = if (compact) 0.62f else 0.82f, height = 18.dp)
                    Spacer(Modifier.height(7.dp))
                    SkeletonBlock(widthFraction = if (compact) 0.5f else 0.68f, height = 12.dp)
                }
                Row(
                    modifier = Modifier.padding(start = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    SkeletonIconButton()
                    SkeletonIconButton()
                }
            }
            if (!compact) {
                Spacer(Modifier.height(8.dp))
                SkeletonBlock(widthFraction = 0.92f, height = 13.dp)
            }
            Spacer(Modifier.height(8.dp))
            SkeletonBlock(widthFraction = 1f, height = 3.dp)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                SkeletonDot()
                SkeletonBlock(widthFraction = 0.16f, height = 11.dp)
                SkeletonBlock(widthFraction = 0.12f, height = 11.dp)
                SkeletonBlock(widthFraction = 0.14f, height = 11.dp)
                Spacer(Modifier.weight(1f))
                SkeletonBlock(widthFraction = 0.18f, height = 11.dp)
            }
        }
    }
}

@Composable
private fun LoadMoreSkeletonButton(modifier: Modifier = Modifier) {
    val colors = SunsetGitHubThemeTokens.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .height(42.dp)
            .background(colors.chipBackground, RoundedCornerShape(12.dp))
            .border(1.dp, colors.border, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        SkeletonBlock(widthFraction = 0.34f, height = 12.dp)
    }
}

@Composable
private fun SkeletonIconButton() {
    val colors = SunsetGitHubThemeTokens.colors
    Box(
        modifier = Modifier
            .size(30.dp)
            .background(colors.chipBackground, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        SkeletonBlock(widthFraction = 0.46f, height = 13.dp)
    }
}

@Composable
private fun SkeletonDot() {
    val colors = SunsetGitHubThemeTokens.colors
    Box(
        modifier = Modifier
            .size(6.dp)
            .background(colors.border, CircleShape)
    )
}

@Composable
private fun SkeletonBlock(
    widthFraction: Float,
    height: Dp,
    modifier: Modifier = Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction.coerceIn(0.05f, 1f))
            .height(height)
            .background(colors.chipBackground, RoundedCornerShape(99.dp))
    )
}

@Composable
private fun CompactRepositorySearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    Box(
        modifier = modifier
            .height(40.dp)
            .background(colors.surface, RoundedCornerShape(12.dp))
            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = if (query.isNotBlank()) 28.dp else 0.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.textPrimary),
            decorationBox = { innerTextField ->
                if (query.isBlank()) {
                    Text(
                        text = stringResource(R.string.dashboard_search_hint),
                        color = colors.textMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                innerTextField()
            }
        )
        if (query.isNotBlank()) {
            Text(
                text = "×",
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(28.dp)
                    .clickable { onQueryChange("") },
                color = colors.textMuted,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun RepositoryDashboardCard(
    repository: GitHubRepository,
    localState: RepositoryLocalState,
    onOpen: () -> Unit,
    onTogglePinned: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, colors.divider),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .clickable(onClick = onOpen)
                .padding(horizontal = 11.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = repository.name,
                        color = colors.textPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildRepositoryOwnerLine(repository),
                        modifier = Modifier.padding(top = 3.dp),
                        color = colors.textMuted,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    modifier = Modifier.padding(start = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    CompactRepositoryActionButton(
                        iconResId = if (localState.isPinned) R.drawable.ic_pin_filled_24 else R.drawable.ic_pin_outline_24,
                        contentDescription = if (localState.isPinned) "取消置顶仓库" else "置顶仓库",
                        isActive = localState.isPinned,
                        activeColor = colors.accent,
                        onClick = onTogglePinned
                    )
                    CompactRepositoryActionButton(
                        iconResId = if (localState.isFavorite) R.drawable.ic_star_filled_24 else R.drawable.ic_star_outline_24,
                        contentDescription = if (localState.isFavorite) "取消收藏仓库" else "收藏仓库",
                        isActive = localState.isFavorite,
                        activeColor = colors.attention,
                        onClick = onToggleFavorite
                    )
                }
            }

            repository.description?.takeIf { it.isNotBlank() }?.let { description ->
                Text(
                    text = description,
                    modifier = Modifier.padding(top = 6.dp),
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            RepositoryLanguageBar(
                languages = repository.languages,
                fallbackLanguage = repository.language,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )

            RepositoryMetaRow(
                repository = repository,
                modifier = Modifier.padding(top = 7.dp)
            )
        }
    }
}

@Composable
private fun CompactRepositoryActionButton(
    iconResId: Int,
    contentDescription: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    IconButton(
        modifier = Modifier.size(30.dp),
        onClick = onClick
    ) {
        Icon(
            painter = painterResource(iconResId),
            contentDescription = contentDescription,
            modifier = Modifier.size(21.dp),
            tint = if (isActive) activeColor else colors.textMuted
        )
    }
}

@Composable
private fun RepositoryMetaRow(
    repository: GitHubRepository,
    modifier: Modifier = Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        repository.primaryLanguageName()?.let { language ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(languageColor(language), CircleShape)
                )
                Text(
                    text = language,
                    modifier = Modifier.padding(start = 4.dp),
                    color = colors.textMuted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        buildRepositoryCountMeta(repository).forEach { item ->
            Text(
                text = item,
                color = colors.textMuted,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
        Spacer(Modifier.weight(1f))
        repository.updatedAt?.takeIf { it.isNotBlank() }?.let { updatedAt ->
            Text(
                text = updatedAt.toRepositoryDateLabel(),
                color = colors.textMuted,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun RepositoryLanguageBar(
    languages: List<RepositoryLanguage>,
    fallbackLanguage: String?,
    modifier: Modifier = Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    val segments = languageBarSegments(languages, fallbackLanguage)
    if (segments.isEmpty()) return
    Row(
        modifier = modifier
            .height(3.dp)
            .background(colors.chipBackground, RoundedCornerShape(99.dp))
            .border(0.dp, Color.Transparent, RoundedCornerShape(99.dp))
    ) {
        segments.forEach { segment ->
            Box(
                modifier = Modifier
                    .weight(segment.weight)
                    .fillMaxHeight()
                    .background(segment.color)
            )
        }
    }
}

private fun buildRepositoryOwnerLine(repository: GitHubRepository): String = buildList {
    add(repository.ownerLogin)
    add(if (repository.isPrivate) "private" else "public")
    if (repository.archived) add("archived")
    if (repository.fork) add("fork")
    repository.defaultBranch.takeIf { it.isNotBlank() }?.let(::add)
}.joinToString(" · ")

private fun buildRepositoryCountMeta(repository: GitHubRepository): List<String> = buildList {
    if (repository.stargazersCount > 0) add("★ ${repository.stargazersCount}")
    if (repository.forksCount > 0) add("Fork ${repository.forksCount}")
    if (repository.openIssuesCount > 0) add("Issue ${repository.openIssuesCount}")
}

private data class LanguageBarSegment(
    val weight: Float,
    val color: Color
)

private fun languageBarSegments(
    languages: List<RepositoryLanguage>,
    fallbackLanguage: String?
): List<LanguageBarSegment> {
    val knownLanguages = languages
        .filter { language -> language.percentage > 0 }
        .sortedByDescending { language -> language.percentage }
        .take(4)
    if (knownLanguages.isNotEmpty()) {
        return knownLanguages.map { language ->
            LanguageBarSegment(
                weight = language.percentage.coerceAtLeast(1).toFloat(),
                color = languageColor(language.name)
            )
        }
    }
    return fallbackLanguage
        ?.takeIf { it.isNotBlank() }
        ?.let { listOf(LanguageBarSegment(weight = 1f, color = languageColor(it))) }
        .orEmpty()
}

private fun GitHubRepository.primaryLanguageName(): String? {
    return languages
        .maxByOrNull { it.percentage }
        ?.name
        ?.takeIf { it.isNotBlank() }
        ?: language?.takeIf { it.isNotBlank() }
}

private fun languageColor(language: String): Color {
    return when (language.trim().lowercase()) {
        "kotlin" -> Color(0xFFA97BFF)
        "java" -> Color(0xFFB07219)
        "python" -> Color(0xFF3572A5)
        "shell", "bash" -> Color(0xFF89E051)
        "javascript" -> Color(0xFFF1E05A)
        "typescript" -> Color(0xFF3178C6)
        "html" -> Color(0xFFE34C26)
        "css" -> Color(0xFF563D7C)
        "c" -> Color(0xFF555555)
        "c++", "cpp" -> Color(0xFFF34B7D)
        "go" -> Color(0xFF00ADD8)
        "rust" -> Color(0xFFDEA584)
        "ruby" -> Color(0xFF701516)
        "swift" -> Color(0xFFF05138)
        else -> Color(0xFF8C959F)
    }
}

private fun String.toRepositoryDateLabel(): String {
    return take(10).takeIf { it.length == 10 } ?: this
}

private fun GitHubRepository.matches(query: String): Boolean {
    val normalized = query.trim()
    if (normalized.isBlank()) return true
    return fullName.contains(normalized, true) ||
        name.contains(normalized, true) ||
        ownerLogin.contains(normalized, true) ||
        description.orEmpty().contains(normalized, true) ||
        language.orEmpty().contains(normalized, true) ||
        defaultBranch.contains(normalized, true) ||
        languages.any { it.name.contains(normalized, true) }
}

private fun List<GitHubRepository>.sortedForDashboard(
    mode: DashboardSortMode,
    localStates: Map<String, RepositoryLocalState>
): List<GitHubRepository> {
    val sorted = when (mode) {
        DashboardSortMode.Default -> this
        DashboardSortMode.Stars -> sortedByDescending { it.stargazersCount }
        DashboardSortMode.Watchers -> sortedByDescending { it.watchersCount }
        DashboardSortMode.Forks -> sortedByDescending { it.forksCount }
        DashboardSortMode.Issues -> sortedByDescending { it.openIssuesCount }
        DashboardSortMode.Name -> sortedBy { it.fullName.lowercase() }
    }
    return sorted.sortedByDescending { localStates[it.fullName]?.isPinned == true }
}

private const val DashboardSkeletonItemCount = 5

private enum class DashboardSortMode(@androidx.annotation.StringRes val labelResId: Int) {
    Default(R.string.dashboard_sort_mode_default),
    Stars(R.string.dashboard_sort_mode_stars),
    Watchers(R.string.dashboard_sort_mode_watchers),
    Forks(R.string.dashboard_sort_mode_forks),
    Issues(R.string.dashboard_sort_mode_issues),
    Name(R.string.dashboard_sort_mode_name)
}
