package com.Sunset.REN.GitHub.ui.repo

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

class RepositoryPullRequestsViewModel(application: Application) : AndroidViewModel(application) {

    private val currentAccountStore = SharedPreferencesCurrentAccountStore(application)
    private val tokenStore = EncryptedSharedPreferencesTokenStore(application)

    private val _pullRequestsState = MutableLiveData(RepositoryPullRequestsUiState())
    val pullRequestsState: LiveData<RepositoryPullRequestsUiState> = _pullRequestsState

    private val firstPageCache = mutableMapOf<String, RepositoryPullRequestsUiState>()

    private var accessToken: String = ""
    private var hasPrepared = false

    fun prepare(owner: String, repo: String) {
        if (hasPrepared) return
        hasPrepared = true
        _pullRequestsState.value = RepositoryPullRequestsUiState(owner = owner, repo = repo)
        loadFirstPage()
    }

    fun switchState(state: String) {
        val current = _pullRequestsState.value ?: return
        if (current.state == state) return
        _pullRequestsState.value = current.copy(
            state = state,
            isLoading = true,
            isLoadingMore = false,
            errorMessage = null,
            isShowingStaleContent = current.pullRequests.isNotEmpty()
        )
        loadFirstPage()
    }

    fun loadFirstPage() {
        val state = _pullRequestsState.value ?: return
        if (state.owner.isBlank() || state.repo.isBlank()) return
        val cacheKey = state.firstPageCacheKey()
        val cachedState = firstPageCache[cacheKey]
        if (cachedState != null && cachedState.pullRequests.isNotEmpty()) {
            _pullRequestsState.value = cachedState.copy(isLoading = true, isLoadingMore = false, errorMessage = null)
        }
        val requestState = _pullRequestsState.value ?: state
        viewModelScope.launch {
            _pullRequestsState.value = requestState.copy(
                isLoading = true,
                isLoadingMore = false,
                errorMessage = null
            )
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) {
                _pullRequestsState.value = _pullRequestsState.value?.copy(
                    isLoading = false,
                    errorMessage = NotSignedInMessage
                )
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    GitHubRepositoryApiGateway(token).listRepositoryPullRequests(
                        owner = requestState.owner,
                        repo = requestState.repo,
                        state = requestState.state,
                        page = 1,
                        perPage = PageSize
                    )
                }
            }
            _pullRequestsState.value = result.fold(
                onSuccess = { pullRequests ->
                    (_pullRequestsState.value ?: requestState).copy(
                        pullRequests = pullRequests,
                        isLoading = false,
                        isLoadingMore = false,
                        errorMessage = null,
                        hasMore = pullRequests.size >= PageSize,
                        loadedPages = 1,
                        isShowingStaleContent = false
                    ).also { refreshedState ->
                        firstPageCache[cacheKey] = refreshedState
                    }
                },
                onFailure = { error ->
                    _pullRequestsState.value?.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        errorMessage = error.message?.takeIf { it.isNotBlank() } ?: UnknownErrorMessage
                    )
                }
            )
        }
    }

    fun loadNextPage() {
        val state = _pullRequestsState.value ?: return
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return
        if (state.owner.isBlank() || state.repo.isBlank()) return
        viewModelScope.launch {
            _pullRequestsState.value = state.copy(isLoadingMore = true, errorMessage = null)
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) {
                _pullRequestsState.value = _pullRequestsState.value?.copy(
                    isLoadingMore = false,
                    errorMessage = NotSignedInMessage
                )
                return@launch
            }
            val nextPage = state.loadedPages + 1
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    GitHubRepositoryApiGateway(token).listRepositoryPullRequests(
                        owner = state.owner,
                        repo = state.repo,
                        state = state.state,
                        page = nextPage,
                        perPage = PageSize
                    )
                }
            }
            _pullRequestsState.value = result.fold(
                onSuccess = { pullRequests ->
                    val current = _pullRequestsState.value ?: return@fold null
                    current.copy(
                        pullRequests = current.pullRequests + pullRequests,
                        isLoadingMore = false,
                        errorMessage = null,
                        hasMore = pullRequests.size >= PageSize,
                        loadedPages = nextPage
                    )
                },
                onFailure = { error ->
                    _pullRequestsState.value?.copy(
                        isLoadingMore = false,
                        errorMessage = error.message?.takeIf { it.isNotBlank() } ?: UnknownErrorMessage
                    )
                }
            )
        }
    }

    private fun RepositoryPullRequestsUiState.firstPageCacheKey(): String {
        return "${owner.lowercase()}/${repo.lowercase()}:$state"
    }

    private suspend fun ensureAccessToken(): String? {
        if (accessToken.isNotBlank()) return accessToken
        val account = withContext(Dispatchers.IO) {
            currentAccountStore.getCurrentAccount()
        } ?: return null
        val token = withContext(Dispatchers.IO) {
            tokenStore.getAccessToken(account.id)
        }?.takeIf { it.isNotBlank() } ?: return null
        accessToken = token
        return token
    }

    private companion object {
        const val PageSize = 20
        const val NotSignedInMessage = "当前账号未登录或令牌已失效。"
        const val UnknownErrorMessage = "加载 Pull Requests 时发生未知错误。"
    }
}