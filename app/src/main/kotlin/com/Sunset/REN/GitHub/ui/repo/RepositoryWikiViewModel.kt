package com.Sunset.REN.GitHub.ui.repo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.Sunset.REN.GitHub.data.github.html.GitHubHtmlParseResult
import com.Sunset.REN.GitHub.data.github.html.GitHubRepositoryWikiGateway
import com.Sunset.REN.GitHub.data.local.EncryptedSharedPreferencesTokenStore
import com.Sunset.REN.GitHub.data.local.SharedPreferencesCurrentAccountStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RepositoryWikiViewModel(application: Application) : AndroidViewModel(application) {

    private val currentAccountStore = SharedPreferencesCurrentAccountStore(application)
    private val tokenStore = EncryptedSharedPreferencesTokenStore(application)

    private val _wikiState = MutableLiveData(RepositoryWikiUiState())
    val wikiState: LiveData<RepositoryWikiUiState> = _wikiState

    private var accessToken: String = ""
    private var hasPrepared = false

    fun prepare(owner: String, repo: String) {
        if (hasPrepared) return
        hasPrepared = true
        _wikiState.value = RepositoryWikiUiState(owner = owner, repo = repo)
        refresh()
    }

    fun refresh() {
        val state = _wikiState.value ?: return
        if (state.owner.isBlank() || state.repo.isBlank()) return
        viewModelScope.launch {
            _wikiState.value = state.copy(
                isLoading = true,
                errorMessage = null,
                sourceUrl = state.sourceUrl.takeIf { state.summary != null },
                isShowingStaleContent = state.summary != null
            )
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) {
                _wikiState.value = _wikiState.value?.copy(
                    isLoading = false,
                    isShowingStaleContent = false,
                    errorMessage = NotSignedInMessage
                )
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                runCatching { GitHubRepositoryWikiGateway(token).loadWikiSummary(state.owner, state.repo) }
            }
            _wikiState.value = result.fold(
                onSuccess = { parseResult ->
                    when (parseResult) {
                        is GitHubHtmlParseResult.Success -> _wikiState.value?.copy(
                            isLoading = false,
                            summary = parseResult.value,
                            isShowingStaleContent = false,
                            errorMessage = null,
                            sourceUrl = parseResult.value.sourceUrl
                        )
                        is GitHubHtmlParseResult.AccessDenied -> _wikiState.value?.copy(
                            isLoading = false,
                            isShowingStaleContent = false,
                            errorMessage = parseResult.message,
                            sourceUrl = parseResult.sourceUrl
                        )
                        is GitHubHtmlParseResult.NotFound -> _wikiState.value?.copy(
                            isLoading = false,
                            isShowingStaleContent = false,
                            errorMessage = parseResult.message,
                            sourceUrl = parseResult.sourceUrl
                        )
                        is GitHubHtmlParseResult.ParseError -> _wikiState.value?.copy(
                            isLoading = false,
                            isShowingStaleContent = false,
                            errorMessage = parseResult.message,
                            sourceUrl = parseResult.sourceUrl
                        )
                    }
                },
                onFailure = { error ->
                    _wikiState.value?.copy(
                        isLoading = false,
                        isShowingStaleContent = false,
                        errorMessage = error.message?.takeIf { it.isNotBlank() } ?: UnknownErrorMessage
                    )
                }
            )
        }
    }

    private suspend fun ensureAccessToken(): String? {
        if (accessToken.isNotBlank()) return accessToken
        val account = withContext(Dispatchers.IO) { currentAccountStore.getCurrentAccount() } ?: return null
        val token = withContext(Dispatchers.IO) { tokenStore.getAccessToken(account.id) }
            ?.takeIf { it.isNotBlank() } ?: return null
        accessToken = token
        return token
    }

    private companion object {
        const val NotSignedInMessage = "当前账号未登录或令牌已失效。"
        const val UnknownErrorMessage = "加载仓库 Wiki 页面时发生未知错误。"
    }
}