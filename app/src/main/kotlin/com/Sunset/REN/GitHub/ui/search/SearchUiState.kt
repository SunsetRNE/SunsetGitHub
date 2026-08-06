package com.Sunset.REN.GitHub.ui.search

import com.Sunset.REN.GitHub.domain.repo.GitHubCodeSearchItem
import com.Sunset.REN.GitHub.domain.repo.GitHubIssueSearchItem
import com.Sunset.REN.GitHub.domain.repo.GitHubRepository
import com.Sunset.REN.GitHub.domain.repo.GitHubUserSearchItem

/**
 * 全局搜索页 UI 状态。
 *
 * 交互流程：进入页面为 [Idle] → 输入关键词出现 [TypeSuggestion]（选择搜索类型）→ 选定类型后
 * 进入 [Loading]/[Content]/[Empty]/[Error]。支持仓库、用户、Issue 三种搜索类型。
 */
sealed class SearchUiState {

    /** 初始空闲态：尚未输入任何关键词。 */
    data object Idle : SearchUiState()

    /** 未登录：搜索 GitHub 需要有效 token。 */
    data object SignedOut : SearchUiState()

    /**
     * 输入中：根据当前关键词展示可选的搜索类型建议，点击某一类型才真正发起搜索。
     */
    data class TypeSuggestion(
        val query: String,
        val types: List<SearchType>
    ) : SearchUiState()

    /** 搜索请求进行中。 */
    data class Loading(
        val query: String,
        val type: SearchType,
        val previousContent: Content? = null
    ) : SearchUiState()

    /** 有结果。[items] 为当前页的统一结果项，按 [type] 渲染对应样式。翻页为替换式。 */
    data class Content(
        val query: String,
        val type: SearchType,
        val items: List<SearchResultItem>,
        val totalCount: Int,
        val incompleteResults: Boolean,
        val currentPage: Int,
        val totalPages: Int,
        val hasPrevPage: Boolean,
        val hasNextPage: Boolean
    ) : SearchUiState()

    /** 无结果。 */
    data class Empty(
        val query: String,
        val type: SearchType
    ) : SearchUiState()

    /** 出错。 */
    data class Error(
        val query: String,
        val type: SearchType,
        val message: String
    ) : SearchUiState()
}

/** 统一搜索结果项：按搜索类型承载不同的领域对象，渲染层据此分发。 */
sealed class SearchResultItem {
    data class Repo(val repository: GitHubRepository) : SearchResultItem()
    data class User(val user: GitHubUserSearchItem) : SearchResultItem()
    data class Issue(val issue: GitHubIssueSearchItem) : SearchResultItem()
    data class Code(val code: GitHubCodeSearchItem) : SearchResultItem()
}

/**
 * 搜索类型。仓库 / 用户 / Issue / 代码均已支持。
 *
 * 代码搜索（`/search/code`）速率限制较严（约 10 次/分钟）且仅索引部分仓库默认分支，按需使用。
 */
enum class SearchType(val displayName: String, val isAvailable: Boolean) {
    Repositories("仓库", isAvailable = true),
    Users("用户", isAvailable = true),
    Issues("Issue", isAvailable = true),
    Code("代码", isAvailable = true);

    companion object {
        /** 当前可在应用内执行的搜索类型。 */
        val AvailableTypes: List<SearchType> = entries.filter { it.isAvailable }
    }
}