package com.Sunset.REN.GitHub.ui.repo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.Sunset.REN.GitHub.data.github.html.GitHubHtmlParseResult
import com.Sunset.REN.GitHub.data.github.html.GitHubRepositoryAgentsGateway
import com.Sunset.REN.GitHub.data.local.EncryptedSharedPreferencesTokenStore
import com.Sunset.REN.GitHub.data.local.SharedPreferencesCurrentAccountStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RepositoryAgentsViewModel(application: Application) : AndroidViewModel(application) {

    private val currentAccountStore = SharedPreferencesCurrentAccountStore(application)
    private val tokenStore = EncryptedSharedPreferencesTokenStore(application)

    private val _agentsState = MutableLiveData(RepositoryAgentsUiState())
    val agentsState: LiveData<RepositoryAgentsUiState> = _agentsState

    private var accessToken: String = ""
    private var hasPrepared = false

    fun prepare(owner: String, repo: String) {
        if (hasPrepared) return
        hasPrepared = true
        _agentsState.value = RepositoryAgentsUiState(owner = owner, repo = repo)
        refresh()
    }

    fun refresh() {
        val state = _agentsState.value ?: return
        if (state.owner.isBlank() || state.repo.isBlank()) return
        viewModelScope.launch {
            _agentsState.value = state.copy(
                isLoading = true,
                errorMessage = null,
                sourceUrl = state.sourceUrl.takeIf { state.summary != null },
                isShowingStaleContent = state.summary != null
            )
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) {
                _agentsState.value = _agentsState.value?.copy(
                    isLoading = false,
                    isShowingStaleContent = false,
                    errorMessage = NotSignedInMessage
                )
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                runCatching { GitHubRepositoryAgentsGateway(token).loadAgentsPage(state.owner, state.repo) }
            }
            _agentsState.value = result.fold(
                onSuccess = { parseResult ->
                    when (parseResult) {
                        is GitHubHtmlParseResult.Success -> _agentsState.value?.copy(
                            isLoading = false,
                            summary = parseResult.value.summary,
                            sessions = parseResult.value.sessions,
                            isExperimentalHtmlParse = parseResult.value.isExperimentalHtmlParse,
                            isShowingStaleContent = false,
                            errorMessage = null,
                            sourceUrl = parseResult.value.summary.sourceUrl
                        )
                        is GitHubHtmlParseResult.AccessDenied -> _agentsState.value?.copy(
                            isLoading = false,
                            isShowingStaleContent = false,
                            errorMessage = parseResult.message,
                            sourceUrl = parseResult.sourceUrl
                        )
                        is GitHubHtmlParseResult.NotFound -> _agentsState.value?.copy(
                            isLoading = false,
                            isShowingStaleContent = false,
                            errorMessage = parseResult.message,
                            sourceUrl = parseResult.sourceUrl
                        )
                        is GitHubHtmlParseResult.ParseError -> _agentsState.value?.copy(
                            isLoading = false,
                            isShowingStaleContent = false,
                            errorMessage = parseResult.message,
                            sourceUrl = parseResult.sourceUrl
                        )
                    }
                },
                onFailure = { error ->
                    _agentsState.value?.copy(
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
        const val UnknownErrorMessage = "加载仓库 Agents 页面时发生未知错误。"
    }
}
