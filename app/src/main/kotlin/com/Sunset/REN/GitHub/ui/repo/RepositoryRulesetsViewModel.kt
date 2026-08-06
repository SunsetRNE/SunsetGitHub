package com.Sunset.REN.GitHub.ui.repo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.Sunset.REN.GitHub.data.github.html.GitHubHtmlParseResult
import com.Sunset.REN.GitHub.data.github.html.GitHubRepositoryRulesetsGateway
import com.Sunset.REN.GitHub.data.github.html.RepositoryRulesetsSnapshot
import com.Sunset.REN.GitHub.data.local.EncryptedSharedPreferencesTokenStore
import com.Sunset.REN.GitHub.data.local.SharedPreferencesCurrentAccountStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RepositoryRulesetsUiState(
    val owner: String = "",
    val repo: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val snapshot: RepositoryRulesetsSnapshot? = null
) {
    val isInitialLoad: Boolean get() = isLoading && snapshot == null && errorMessage == null
}

class RepositoryRulesetsViewModel(application: Application) : AndroidViewModel(application) {
    private val accountStore = SharedPreferencesCurrentAccountStore(application)
    private val tokenStore = EncryptedSharedPreferencesTokenStore(application)
    private val _state = MutableLiveData(RepositoryRulesetsUiState())
    val state: LiveData<RepositoryRulesetsUiState> = _state
    private var token: String = ""
    private var prepared = false

    fun prepare(owner: String, repo: String) {
        if (prepared) return
        prepared = true
        _state.value = RepositoryRulesetsUiState(owner = owner, repo = repo)
        refresh()
    }

    fun refresh() {
        val state = _state.value ?: return
        if (state.owner.isBlank() || state.repo.isBlank()) return
        viewModelScope.launch {
            _state.value = state.copy(isLoading = true, errorMessage = null)
            val accessToken = ensureToken()
            if (accessToken.isNullOrBlank()) {
                _state.value = state.copy(isLoading = false, errorMessage = "当前账号未登录或令牌已失效。")
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                GitHubRepositoryRulesetsGateway(accessToken).loadRulesets(state.owner, state.repo)
            }
            _state.value = when (result) {
                is GitHubHtmlParseResult.Success -> (_state.value ?: state).copy(isLoading = false, snapshot = result.value, errorMessage = null)
                is GitHubHtmlParseResult.AccessDenied -> (_state.value ?: state).copy(isLoading = false, errorMessage = result.message)
                is GitHubHtmlParseResult.NotFound -> (_state.value ?: state).copy(isLoading = false, errorMessage = result.message)
                is GitHubHtmlParseResult.ParseError -> (_state.value ?: state).copy(isLoading = false, errorMessage = result.message)
            }
        }
    }

    private suspend fun ensureToken(): String? {
        if (token.isNotBlank()) return token
        val account = withContext(Dispatchers.IO) { accountStore.getCurrentAccount() } ?: return null
        token = withContext(Dispatchers.IO) { tokenStore.getAccessToken(account.id) }?.takeIf { it.isNotBlank() } ?: return null
        return token
    }
}
