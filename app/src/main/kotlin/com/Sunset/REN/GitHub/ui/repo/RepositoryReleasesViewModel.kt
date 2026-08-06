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

class RepositoryReleasesViewModel(application: Application) : AndroidViewModel(application) {

    private val currentAccountStore = SharedPreferencesCurrentAccountStore(application)
    private val tokenStore = EncryptedSharedPreferencesTokenStore(application)

    private val _releasesState = MutableLiveData(RepositoryReleasesUiState())
    val releasesState: LiveData<RepositoryReleasesUiState> = _releasesState

    private var accessToken: String = ""
    private var hasPrepared = false

    fun prepare(owner: String, repo: String) {
        if (hasPrepared) return
        hasPrepared = true
        _releasesState.value = RepositoryReleasesUiState(owner = owner, repo = repo)
        loadFirstPage()
    }

    fun loadFirstPage() {
        val state = _releasesState.value ?: return
        if (state.owner.isBlank() || state.repo.isBlank()) return
        viewModelScope.launch {
            _releasesState.value = state.copy(
                isLoading = true,
                isLoadingMore = false,
                errorMessage = null,
                isShowingStaleContent = state.releases.isNotEmpty()
            )
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) {
                _releasesState.value = _releasesState.value?.copy(
                    isLoading = false,
                    isShowingStaleContent = false,
                    errorMessage = NotSignedInMessage
                )
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    GitHubRepositoryApiGateway(token).listRepositoryReleases(
                        owner = state.owner,
                        repo = state.repo,
                        page = 1,
                        perPage = PageSize
                    )
                }
            }
            _releasesState.value = result.fold(
                onSuccess = { releases ->
                    _releasesState.value?.copy(
                        releases = releases,
                        isLoading = false,
                        isLoadingMore = false,
                        errorMessage = null,
                        hasMore = releases.size >= PageSize,
                        loadedPages = 1,
                        isShowingStaleContent = false
                    )
                },
                onFailure = { error ->
                    _releasesState.value?.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        isShowingStaleContent = false,
                        errorMessage = error.message?.takeIf { it.isNotBlank() } ?: UnknownErrorMessage
                    )
                }
            )
        }
    }

    fun loadNextPage() {
        val state = _releasesState.value ?: return
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return
        if (state.owner.isBlank() || state.repo.isBlank()) return
        viewModelScope.launch {
            _releasesState.value = state.copy(isLoadingMore = true, errorMessage = null)
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) {
                _releasesState.value = _releasesState.value?.copy(
                    isLoadingMore = false,
                    errorMessage = NotSignedInMessage
                )
                return@launch
            }
            val nextPage = state.loadedPages + 1
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    GitHubRepositoryApiGateway(token).listRepositoryReleases(
                        owner = state.owner,
                        repo = state.repo,
                        page = nextPage,
                        perPage = PageSize
                    )
                }
            }
            _releasesState.value = result.fold(
                onSuccess = { releases ->
                    val current = _releasesState.value ?: return@fold null
                    current.copy(
                        releases = current.releases + releases,
                        isLoadingMore = false,
                        errorMessage = null,
                        hasMore = releases.size >= PageSize,
                        loadedPages = nextPage
                    )
                },
                onFailure = { error ->
                    _releasesState.value?.copy(
                        isLoadingMore = false,
                        errorMessage = error.message?.takeIf { it.isNotBlank() } ?: UnknownErrorMessage
                    )
                }
            )
        }
    }

    fun currentAuthorizationHeader(): String? {
        return accessToken.takeIf { it.isNotBlank() }?.let { token -> "Bearer $token" }
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
        const val UnknownErrorMessage = "加载发布版本时发生未知错误。"
    }
}