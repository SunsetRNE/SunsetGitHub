package com.Sunset.REN.GitHub.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.Sunset.REN.GitHub.data.github.GitHubRepositoryApiGateway
import com.Sunset.REN.GitHub.data.local.EncryptedSharedPreferencesTokenStore
import com.Sunset.REN.GitHub.data.local.SharedPreferencesCurrentAccountStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min

/**
 * 全局搜索 ViewModel。
 *
 * 输入关键词时只更新类型建议，不发请求；用户点选搜索类型后才真正调用对应的 GitHub Search API。
 * 支持仓库 / 用户 / Issue / 代码四种类型，采用替换式翻页：
 * - 翻到「已访问过」的页直接走页缓存，不重复请求，从而缓解 GitHub 搜索接口（尤其代码搜索）的限流。
 * - GitHub 搜索结果硬上限 1000 条，翻页范围据此封顶。
 */
class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val currentAccountStore = SharedPreferencesCurrentAccountStore(application)
    private val tokenStore = EncryptedSharedPreferencesTokenStore(application)

    private val _searchState = MutableLiveData<SearchUiState>(SearchUiState.Idle)
    val searchState: LiveData<SearchUiState> = _searchState

    private var isLoading = false

    /** 页缓存：key = "type|query|page"，避免重复请求同一页。 */
    private val pageCache = mutableMapOf<String, SearchPageResult>()

    /** 输入变化：空则回到空闲态，否则展示类型建议。不触发网络请求。 */
    fun onQueryChanged(rawQuery: String) {
        val query = rawQuery.trim()
        if (query.isEmpty()) {
            _searchState.value = SearchUiState.Idle
            return
        }
        _searchState.value = SearchUiState.TypeSuggestion(
            query = query,
            types = SearchType.entries
        )
    }

    /** 选定搜索类型并发起搜索（定位到第 1 页）。仅 [SearchType.isAvailable] 为 true 的类型可执行。 */
    fun search(rawQuery: String, type: SearchType) {
        val query = rawQuery.trim()
        if (query.isEmpty()) {
            _searchState.value = SearchUiState.Idle
            return
        }
        if (!type.isAvailable) return
        goToPage(query, type, FirstPage)
    }

    fun retry() {
        when (val state = _searchState.value) {
            is SearchUiState.Error -> goToPage(state.query, state.type, FirstPage)
            is SearchUiState.Empty -> goToPage(state.query, state.type, FirstPage)
            else -> Unit
        }
    }

    fun prevPage() {
        val state = _searchState.value as? SearchUiState.Content ?: return
        if (!state.hasPrevPage) return
        goToPage(state.query, state.type, state.currentPage - 1)
    }

    fun nextPage() {
        val state = _searchState.value as? SearchUiState.Content ?: return
        if (!state.hasNextPage) return
        goToPage(state.query, state.type, state.currentPage + 1)
    }

    /**
     * 跳转到指定页：命中页缓存则直接出结果（不请求），否则发请求并缓存。
     */
    private fun goToPage(query: String, type: SearchType, page: Int) {
        if (isLoading) return
        val cached = pageCache[cacheKey(query, type, page)]
        if (cached != null) {
            _searchState.value = toContentState(query, type, page, cached)
            return
        }
        val previousContent = _searchState.value as? SearchUiState.Content
        viewModelScope.launch {
            isLoading = true
            _searchState.value = SearchUiState.Loading(query, type, previousContent)
            try {
                val token = loadTokenOrNull() ?: run {
                    _searchState.value = SearchUiState.SignedOut
                    return@launch
                }
                val result = fetchPage(token, query, type, page)
                if (result.items.isEmpty() && page == FirstPage) {
                    _searchState.value = SearchUiState.Empty(query, type)
                } else {
                    pageCache[cacheKey(query, type, page)] = result
                    _searchState.value = toContentState(query, type, page, result)
                }
            } catch (exception: Exception) {
                _searchState.value = SearchUiState.Error(
                    query = query,
                    type = type,
                    message = exception.message ?: "搜索失败"
                )
            } finally {
                isLoading = false
            }
        }
    }

    private fun toContentState(
        query: String,
        type: SearchType,
        page: Int,
        result: SearchPageResult
    ): SearchUiState.Content {
        // GitHub 搜索硬上限 1000 条，可翻页数据据此封顶。
        val cappedCount = min(result.totalCount, SearchResultHardLimit)
        val totalPages = ((cappedCount + PageSize - 1) / PageSize).coerceAtLeast(1)
        return SearchUiState.Content(
            query = query,
            type = type,
            items = result.items,
            totalCount = result.totalCount,
            incompleteResults = result.incompleteResults,
            currentPage = page,
            totalPages = totalPages,
            hasPrevPage = page > FirstPage,
            hasNextPage = page < totalPages && result.items.size >= PageSize
        )
    }

    private suspend fun fetchPage(
        token: String,
        query: String,
        type: SearchType,
        page: Int
    ): SearchPageResult {
        return withContext(Dispatchers.IO) {
            val gateway = GitHubRepositoryApiGateway(token)
            when (type) {
                SearchType.Repositories -> {
                    val result = gateway.searchRepositories(query, page, PageSize)
                    SearchPageResult(
                        items = result.repositories.map { SearchResultItem.Repo(it) },
                        totalCount = result.totalCount,
                        incompleteResults = result.incompleteResults
                    )
                }
                SearchType.Users -> {
                    val result = gateway.searchUsers(query, page, PageSize)
                    SearchPageResult(
                        items = result.users.map { SearchResultItem.User(it) },
                        totalCount = result.totalCount,
                        incompleteResults = result.incompleteResults
                    )
                }
                SearchType.Issues -> {
                    val result = gateway.searchIssues(query, page, PageSize)
                    SearchPageResult(
                        items = result.issues.map { SearchResultItem.Issue(it) },
                        totalCount = result.totalCount,
                        incompleteResults = result.incompleteResults
                    )
                }
                SearchType.Code -> {
                    val result = gateway.searchCode(query, page, PageSize)
                    SearchPageResult(
                        items = result.items.map { SearchResultItem.Code(it) },
                        totalCount = result.totalCount,
                        incompleteResults = result.incompleteResults
                    )
                }
            }
        }
    }

    private fun cacheKey(query: String, type: SearchType, page: Int): String {
        return "${type.name}|$query|$page"
    }

    private suspend fun loadTokenOrNull(): String? {
        val account = withContext(Dispatchers.IO) {
            currentAccountStore.getCurrentAccount()
        } ?: return null
        return withContext(Dispatchers.IO) {
            tokenStore.getAccessToken(account.id)
        }?.takeIf { it.isNotBlank() }
    }

    /** 统一的一页搜索结果，屏蔽不同类型分页对象的字段差异。 */
    private data class SearchPageResult(
        val items: List<SearchResultItem>,
        val totalCount: Int,
        val incompleteResults: Boolean
    )

    private companion object {
        const val FirstPage = 1
        const val PageSize = 30
        const val SearchResultHardLimit = 1000
    }
}