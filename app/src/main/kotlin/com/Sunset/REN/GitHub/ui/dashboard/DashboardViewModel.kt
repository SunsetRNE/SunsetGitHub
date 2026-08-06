package com.Sunset.REN.GitHub.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.Sunset.REN.GitHub.data.github.GitHubRepositoryApiGateway
import com.Sunset.REN.GitHub.data.local.EncryptedSharedPreferencesTokenStore
import com.Sunset.REN.GitHub.data.local.RepositoryCacheRepository
import com.Sunset.REN.GitHub.data.local.RepositoryLocalStateStore
import com.Sunset.REN.GitHub.data.local.SharedPreferencesCurrentAccountStore
import com.Sunset.REN.GitHub.domain.auth.GitHubAccount
import com.Sunset.REN.GitHub.domain.repo.GitHubRepository
import com.Sunset.REN.GitHub.ui.repo.RepositoriesCacheEntry
import com.Sunset.REN.GitHub.ui.repo.RepositoriesUiState
import com.Sunset.REN.GitHub.util.PerformanceTrace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val currentAccountStore = SharedPreferencesCurrentAccountStore(application)
    private val tokenStore = EncryptedSharedPreferencesTokenStore(application)
    private val repositoryCacheRepository = RepositoryCacheRepository(application)
    private val repositoryLocalStateStore = RepositoryLocalStateStore(application)
    private var isRefreshingRepositories = false

    private val _repositoriesState = MutableLiveData<RepositoriesUiState>(RepositoriesUiState.Loading)
    val repositoriesState: LiveData<RepositoriesUiState> = _repositoriesState

    init {
        refreshRepositories()
    }

    fun refreshRepositories(forceRefresh: Boolean = false) {
        if (isRefreshingRepositories) return
        viewModelScope.launch {
            isRefreshingRepositories = true
            var shownCachedState = false
            try {
                val session = loadSessionOrNull() ?: run {
                    showSignedOutState()
                    return@launch
                }
                if (!forceRefresh) {
                    val cachedEntry = withContext(Dispatchers.IO) {
                        repositoryCacheRepository.getRepositoryList(session.account.id)
                    }
                    cachedEntry?.let { entry ->
                        publishRepositoriesContent(
                            session = session,
                            repositories = entry.repositories,
                            currentPage = entry.currentPage,
                            canLoadMore = entry.canLoadMore,
                            isRefreshingFromCache = true,
                            refreshedAtMillis = entry.refreshedAtMillis,
                            isRefreshing = true
                        )
                        shownCachedState = true
                    } ?: run {
                        _repositoriesState.value = RepositoriesUiState.Loading
                    }
                } else {
                    val contentState = _repositoriesState.value as? RepositoriesUiState.Content
                    if (contentState != null && contentState.repositories.isNotEmpty()) {
                        _repositoriesState.value = contentState.copy(
                            isRefreshingFromCache = true,
                            isRefreshing = true,
                            loadMoreError = null
                        )
                    }
                }
                val firstPageRepositories = enrichRepositoriesWithLanguages(
                    token = session.token,
                    repositories = loadRepositoriesPage(session.token, FirstPage)
                )
                val repositories = loadRepositoriesPageWithPinned(session, firstPageRepositories)
                val refreshedAtMillis = System.currentTimeMillis()
                publishRepositoriesContent(
                    session = session,
                    repositories = repositories,
                    currentPage = FirstPage,
                    canLoadMore = firstPageRepositories.size == PageSize,
                    refreshedAtMillis = refreshedAtMillis,
                    persistCache = true
                )
            } catch (exception: Exception) {
                val message = exception.message ?: "加载仓库列表失败"
                val currentState = _repositoriesState.value as? RepositoriesUiState.Content
                _repositoriesState.value = if (shownCachedState && currentState != null) {
                    currentState.copy(
                        isRefreshingFromCache = false,
                        isRefreshing = false,
                        loadMoreError = message
                    )
                } else {
                    RepositoriesUiState.Error(message)
                }
            } finally {
                isRefreshingRepositories = false
            }
        }
    }

    fun retryRepositories() {
        refreshRepositories(forceRefresh = true)
    }

    fun loadMoreRepositories() {
        val currentState = _repositoriesState.value as? RepositoriesUiState.Content ?: return
        if (!currentState.canLoadMore || currentState.isLoadingMore) return

        viewModelScope.launch {
            val nextPage = currentState.currentPage + 1
            try {
                _repositoriesState.value = currentState.copy(
                    isLoadingMore = true,
                    loadMoreError = null
                )
                val session = loadSessionOrNull() ?: run {
                    showSignedOutState()
                    return@launch
                }
                val repositories = enrichRepositoriesWithLanguages(
                    token = session.token,
                    repositories = loadRepositoriesPage(session.token, nextPage)
                )
                val allRepositories = mergeRepositories(currentState.repositories, repositories)
                val refreshedAtMillis = System.currentTimeMillis()
                publishRepositoriesContent(
                    session = session,
                    repositories = allRepositories,
                    currentPage = nextPage,
                    canLoadMore = repositories.size == PageSize,
                    refreshedAtMillis = refreshedAtMillis,
                    persistCache = true
                )
            } catch (exception: Exception) {
                _repositoriesState.value = currentState.copy(
                    isLoadingMore = false,
                    loadMoreError = exception.message ?: "加载更多仓库失败"
                )
            }
        }
    }

    fun togglePinned(repositoryFullName: String) {
        toggleRepositoryLocalState(repositoryFullName) { accountId, fullName ->
            repositoryLocalStateStore.togglePinned(accountId, fullName)
        }
    }

    fun toggleFavorite(repositoryFullName: String) {
        toggleRepositoryLocalState(repositoryFullName) { accountId, fullName ->
            repositoryLocalStateStore.toggleFavorite(accountId, fullName)
        }
    }

    private fun toggleRepositoryLocalState(
        repositoryFullName: String,
        toggle: (Long, String) -> Unit
    ) {
        if (repositoryFullName.isBlank()) return
        val currentState = _repositoriesState.value as? RepositoriesUiState.Content ?: return
        viewModelScope.launch {
            val account = loadCurrentAccountOrNull() ?: run {
                showSignedOutState()
                return@launch
            }
            withContext(Dispatchers.IO) {
                toggle(account.id, repositoryFullName)
            }
            _repositoriesState.value = currentState.copy(
                repositoryLocalStates = loadRepositoryLocalStates(account.id, currentState.repositories)
            )
        }
    }

    private suspend fun loadSessionOrNull(): GitHubSession? {
        val account = loadCurrentAccountOrNull() ?: return null
        val token = withContext(Dispatchers.IO) {
            tokenStore.getAccessToken(account.id)
        }?.takeIf { it.isNotBlank() } ?: return null
        return GitHubSession(account, token)
    }

    private suspend fun loadCurrentAccountOrNull(): GitHubAccount? {
        return withContext(Dispatchers.IO) {
            currentAccountStore.getCurrentAccount()
        }
    }

    private fun showSignedOutState() {
        _repositoriesState.value = RepositoriesUiState.SignedOut
    }

    private suspend fun loadRepositoriesPage(token: String, page: Int): List<GitHubRepository> {
        return PerformanceTrace.measureSuspend(
            name = "dashboard.repositories.page",
            metadata = { "page=$page perPage=$PageSize" }
        ) {
            withContext(Dispatchers.IO) {
                GitHubRepositoryApiGateway(token)
                    .listCurrentUserRepositories(page = page, perPage = PageSize)
            }
        }
    }

    private suspend fun enrichRepositoriesWithLanguages(
        token: String,
        repositories: List<GitHubRepository>
    ): List<GitHubRepository> {
        if (repositories.isEmpty()) return repositories
        return PerformanceTrace.measureSuspend(
            name = "dashboard.repositories.languages",
            metadata = { "count=${repositories.size}" }
        ) {
            withContext(Dispatchers.IO) {
                coroutineScope {
                    val gateway = GitHubRepositoryApiGateway(token)
                    repositories.map { repository ->
                        async {
                            if (repository.languages.isNotEmpty()) {
                                repository
                            } else {
                                val languages = runCatching {
                                    gateway.listLanguages(repository.ownerLogin, repository.name)
                                }.getOrDefault(emptyList())
                                if (languages.isEmpty()) repository else repository.copy(languages = languages)
                            }
                        }
                    }.awaitAll()
                }
            }
        }
    }

    private suspend fun loadRepositoriesPageWithPinned(
        session: GitHubSession,
        pageRepositories: List<GitHubRepository>
    ): List<GitHubRepository> {
        val pinnedRepositories = loadPinnedRepositories(session, pageRepositories)
        return mergeRepositories(pinnedRepositories, pageRepositories)
    }

    private suspend fun loadPinnedRepositories(
        session: GitHubSession,
        existingRepositories: List<GitHubRepository>
    ): List<GitHubRepository> {
        val existingNames = existingRepositories.mapTo(mutableSetOf()) { repository -> repository.fullName }
        val pinnedRepositoryNames = withContext(Dispatchers.IO) {
            repositoryLocalStateStore.getPinnedRepositoryFullNames(session.account.id)
        }.filterNot { fullName -> fullName in existingNames }
        if (pinnedRepositoryNames.isEmpty()) return emptyList()

        return PerformanceTrace.measureSuspend(
            name = "dashboard.repositories.pinned",
            metadata = { "count=${pinnedRepositoryNames.size}" }
        ) {
            withContext(Dispatchers.IO) {
                val gateway = GitHubRepositoryApiGateway(session.token)
                pinnedRepositoryNames.mapNotNull { fullName ->
                    val (owner, repo) = splitRepositoryFullName(fullName) ?: return@mapNotNull null
                    runCatching {
                        gateway.getRepository(owner = owner, repo = repo, includeLanguages = false)
                    }.getOrNull()
                }
            }
        }
    }

    private fun splitRepositoryFullName(fullName: String): Pair<String, String>? {
        val separatorIndex = fullName.indexOf('/')
        if (separatorIndex <= 0 || separatorIndex == fullName.lastIndex) return null
        return fullName.substring(0, separatorIndex) to fullName.substring(separatorIndex + 1)
    }

    private fun mergeRepositories(
        first: List<GitHubRepository>,
        second: List<GitHubRepository>
    ): List<GitHubRepository> {
        val seen = LinkedHashSet<String>()
        return (first + second).filter { repository -> seen.add(repository.fullName) }
    }

    private suspend fun publishRepositoriesContent(
        session: GitHubSession,
        repositories: List<GitHubRepository>,
        currentPage: Int,
        canLoadMore: Boolean,
        isRefreshingFromCache: Boolean = false,
        isRefreshing: Boolean = false,
        refreshedAtMillis: Long? = null,
        persistCache: Boolean = false
    ) {
        if (repositories.isEmpty()) {
            _repositoriesState.value = RepositoriesUiState.Empty
            return
        }

        val localStates = withContext(Dispatchers.IO) {
            repositoryLocalStateStore.getRepositoryStates(session.account.id, repositories)
        }
        _repositoriesState.value = RepositoriesUiState.Content(
            repositories = repositories,
            repositoryLocalStates = localStates,
            currentAccountLogin = session.account.login,
            currentPage = currentPage,
            canLoadMore = canLoadMore,
            isRefreshingFromCache = isRefreshingFromCache,
            isRefreshing = isRefreshing,
            refreshedAtMillis = refreshedAtMillis
        )

        if (persistCache && refreshedAtMillis != null) {
            val refreshedEntry = RepositoriesCacheEntry(
                repositories = repositories,
                currentPage = currentPage,
                canLoadMore = canLoadMore,
                refreshedAtMillis = refreshedAtMillis
            )
            withContext(Dispatchers.IO) {
                repositoryCacheRepository.cacheRepositoryList(session.account.id, refreshedEntry)
            }
        }
    }

    private suspend fun buildRepositoriesContent(
        accountId: Long,
        currentAccountLogin: String,
        repositories: List<GitHubRepository>,
        currentPage: Int,
        canLoadMore: Boolean,
        isRefreshingFromCache: Boolean = false,
        refreshedAtMillis: Long? = null
    ): RepositoriesUiState {
        return if (repositories.isEmpty()) {
            RepositoriesUiState.Empty
        } else {
            RepositoriesUiState.Content(
                repositories = repositories,
                repositoryLocalStates = loadRepositoryLocalStates(accountId, repositories),
                currentAccountLogin = currentAccountLogin,
                currentPage = currentPage,
                canLoadMore = canLoadMore,
                isRefreshingFromCache = isRefreshingFromCache,
                refreshedAtMillis = refreshedAtMillis
            )
        }
    }

    private suspend fun loadRepositoryLocalStates(
        accountId: Long,
        repositories: List<GitHubRepository>
    ) = withContext(Dispatchers.IO) {
        repositoryLocalStateStore.getRepositoryStates(accountId, repositories)
    }

    private data class GitHubSession(
        val account: GitHubAccount,
        val token: String
    )

    private companion object {
        const val FirstPage = 1
        const val PageSize = 30
    }
}