package com.Sunset.REN.GitHub.ui.compose.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.Sunset.REN.GitHub.domain.repo.GitHubCodeSearchItem
import com.Sunset.REN.GitHub.domain.repo.GitHubIssueSearchItem
import com.Sunset.REN.GitHub.domain.repo.GitHubRepository
import com.Sunset.REN.GitHub.domain.repo.GitHubUserSearchItem
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens
import com.Sunset.REN.GitHub.ui.compose.components.SunsetCard
import com.Sunset.REN.GitHub.ui.compose.components.SunsetSecondaryButton
import com.Sunset.REN.GitHub.ui.search.SearchResultItem
import com.Sunset.REN.GitHub.ui.search.SearchType
import com.Sunset.REN.GitHub.ui.search.SearchUiState

@Composable
fun SearchScreen(
    query: String,
    state: SearchUiState,
    onQueryChange: (String) -> Unit,
    onSearch: (String, SearchType) -> Unit,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
    onOpenRepository: (GitHubRepository) -> Unit,
    onOpenUser: (GitHubUserSearchItem) -> Unit,
    onOpenIssue: (GitHubIssueSearchItem) -> Unit,
    onOpenCode: (GitHubCodeSearchItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    Box(modifier = modifier.fillMaxSize().background(Color(0x66000000))) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(color = colors.surface, shadowElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onDismiss) { Text("‹", style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary) }
                    OutlinedTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("搜索") },
                        trailingIcon = {
                            if (query.isNotBlank()) {
                                TextButton(onClick = { onQueryChange("") }) { Text("×") }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearch(query, SearchType.Repositories) })
                    )
                }
            }
            when (state) {
                SearchUiState.Idle -> Spacer(Modifier.weight(1f))
                SearchUiState.SignedOut -> SearchBodyMessage("登录后可使用全局搜索。")
                is SearchUiState.TypeSuggestion -> SuggestionsCard(state, onSearch)
                is SearchUiState.Loading -> {
                    state.previousContent?.let {
                        SearchResultsContent(it, isLoading = true, onPrevPage, onNextPage, onOpenRepository, onOpenUser, onOpenIssue, onOpenCode)
                    } ?: SearchBodyMessage("正在搜索…")
                }
                is SearchUiState.Content -> SearchResultsContent(state, false, onPrevPage, onNextPage, onOpenRepository, onOpenUser, onOpenIssue, onOpenCode)
                is SearchUiState.Empty -> SearchBodyMessage("没有找到匹配结果", actionLabel = "重试", onAction = onRetry)
                is SearchUiState.Error -> SearchBodyMessage("搜索失败：${state.message}", actionLabel = "重试", onAction = onRetry)
            }
        }
    }
}

@Composable
private fun SuggestionsCard(state: SearchUiState.TypeSuggestion, onSearch: (String, SearchType) -> Unit) {
    val colors = SunsetGitHubThemeTokens.colors
    SunsetCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            SearchRow(
                title = "搜索此关键词",
                subtitle = state.query,
                enabled = true,
                onClick = { onSearch(state.query, SearchType.Repositories) }
            )
            state.types.forEach { type ->
                SearchRow(
                    title = if (type.isAvailable) "可直接搜索：${type.displayName}" else "暂无搜索建议",
                    subtitle = type.name,
                    enabled = type.isAvailable,
                    onClick = { onSearch(state.query, type) }
                )
            }
        }
    }
}

@Composable
private fun SearchBodyMessage(message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    val colors = SunsetGitHubThemeTokens.colors
    Column(
        modifier = Modifier.fillMaxSize().background(colors.canvas).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(message, color = colors.textPrimary, style = MaterialTheme.typography.bodyLarge)
        if (actionLabel != null && onAction != null) {
            SunsetSecondaryButton(text = actionLabel, onClick = onAction, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SearchResultsContent(
    state: SearchUiState.Content,
    isLoading: Boolean,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
    onOpenRepository: (GitHubRepository) -> Unit,
    onOpenUser: (GitHubUserSearchItem) -> Unit,
    onOpenIssue: (GitHubIssueSearchItem) -> Unit,
    onOpenCode: (GitHubCodeSearchItem) -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.canvas),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = buildResultCountText(state) + if (isLoading) " · 正在搜索…" else "",
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
        items(state.items) { item ->
            when (item) {
                is SearchResultItem.Repo -> RepositoryResultCard(item.repository) { onOpenRepository(item.repository) }
                is SearchResultItem.User -> UserResultCard(item.user) { onOpenUser(item.user) }
                is SearchResultItem.Issue -> IssueResultCard(item.issue) { onOpenIssue(item.issue) }
                is SearchResultItem.Code -> CodeResultCard(item.code) { onOpenCode(item.code) }
            }
        }
        if (state.totalPages > 1 || state.hasPrevPage || state.hasNextPage) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    SunsetSecondaryButton("上一页", onClick = onPrevPage, enabled = state.hasPrevPage)
                    Text("第 ${state.currentPage} 页", modifier = Modifier.weight(1f), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                    SunsetSecondaryButton("下一页", onClick = onNextPage, enabled = state.hasNextPage)
                }
            }
        }
    }
}

@Composable
private fun RepositoryResultCard(repository: GitHubRepository, onClick: () -> Unit) {
    val language = repository.languages.firstOrNull()?.name ?: repository.language
    SearchRow(
        title = repository.fullName,
        subtitle = listOfNotNull(
            repository.description?.takeIf { it.isNotBlank() },
            language,
            "★ ${repository.stargazersCount}",
            "⑂ ${repository.forksCount}"
        ).joinToString(" · "),
        enabled = true,
        onClick = onClick
    )
}

@Composable
private fun UserResultCard(user: GitHubUserSearchItem, onClick: () -> Unit) {
    val type = if (user.type?.lowercase() == "organization") "组织" else "用户"
    SearchRow(title = user.login, subtitle = type, enabled = true, onClick = onClick)
}

@Composable
private fun IssueResultCard(issue: GitHubIssueSearchItem, onClick: () -> Unit) {
    val kind = if (issue.isPullRequest) "拉取请求" else "议题"
    SearchRow(
        title = issue.title,
        subtitle = "${issue.repositoryFullName} #${issue.number} · $kind · ${issue.state} · ${issue.authorLogin}",
        enabled = true,
        onClick = onClick
    )
}

@Composable
private fun CodeResultCard(code: GitHubCodeSearchItem, onClick: () -> Unit) {
    SearchRow(title = code.name, subtitle = "${code.repositoryFullName} · ${code.path}", enabled = true, onClick = onClick)
}

@Composable
private fun SearchRow(title: String, subtitle: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = SunsetGitHubThemeTokens.colors
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick),
        color = colors.surface,
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = colors.textPrimary.copy(alpha = if (enabled) 1f else 0.45f), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotBlank()) {
                Text(subtitle, color = colors.textSecondary.copy(alpha = if (enabled) 1f else 0.45f), style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private fun buildResultCountText(state: SearchUiState.Content): String {
    val base = "找到 ${state.totalCount} 条结果"
    return if (state.totalCount > 1000) "$base · 显示前 1000 条结果" else base
}