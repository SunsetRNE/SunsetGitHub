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

class RepositoryIssueCreateViewModel(application: Application) : AndroidViewModel(application) {

    private val currentAccountStore = SharedPreferencesCurrentAccountStore(application)
    private val tokenStore = EncryptedSharedPreferencesTokenStore(application)

    private val _createState = MutableLiveData(RepositoryIssueCreateUiState())
    val createState: LiveData<RepositoryIssueCreateUiState> = _createState

    private var accessToken: String = ""
    private var hasPrepared = false

    fun prepare(owner: String, repo: String) {
        if (hasPrepared) return
        hasPrepared = true
        _createState.value = RepositoryIssueCreateUiState(owner = owner, repo = repo)
        loadAvailableLabels()
    }

    fun switchLabels(labels: List<String>) {
        val state = _createState.value ?: return
        val normalizedLabels = labels.mapNotNull { it.takeIf(String::isNotBlank) }.distinct()
        if (state.selectedLabels == normalizedLabels) return
        _createState.value = state.copy(selectedLabels = normalizedLabels)
    }

    fun loadAvailableLabels() {
        val state = _createState.value ?: return
        if (state.owner.isBlank() || state.repo.isBlank() || state.isLoadingLabels) return
        viewModelScope.launch {
            _createState.value = state.copy(isLoadingLabels = true, labelErrorMessage = null)
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) {
                _createState.value = _createState.value?.copy(
                    isLoadingLabels = false,
                    labelErrorMessage = NotSignedInMessage
                )
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                runCatching { GitHubRepositoryApiGateway(token).listRepositoryLabels(state.owner, state.repo) }
            }
            _createState.value = result.fold(
                onSuccess = { labels ->
                    _createState.value?.copy(
                        availableLabels = labels,
                        isLoadingLabels = false,
                        labelErrorMessage = null
                    )
                },
                onFailure = { error ->
                    _createState.value?.copy(
                        isLoadingLabels = false,
                        labelErrorMessage = error.message?.takeIf { it.isNotBlank() } ?: UnknownErrorMessage
                    )
                }
            )
        }
    }

    fun submit(title: String, body: String) {
        val state = _createState.value ?: return
        if (title.isBlank()) return
        if (state.isSubmitting) return
        if (state.owner.isBlank() || state.repo.isBlank()) return
        viewModelScope.launch {
            _createState.value = state.copy(isSubmitting = true, errorMessage = null)
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) {
                _createState.value = _createState.value?.copy(
                    isSubmitting = false,
                    errorMessage = NotSignedInMessage
                )
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    GitHubRepositoryApiGateway(token).createIssue(
                        state.owner,
                        state.repo,
                        title,
                        body,
                        state.selectedLabels
                    )
                }
            }
            _createState.value = result.fold(
                onSuccess = { created ->
                    _createState.value?.copy(
                        isSubmitting = false,
                        errorMessage = null,
                        createdIssueNumber = created.number
                    )
                },
                onFailure = { error ->
                    _createState.value?.copy(
                        isSubmitting = false,
                        errorMessage = error.message?.takeIf { it.isNotBlank() } ?: UnknownErrorMessage
                    )
                }
            )
        }
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
        const val NotSignedInMessage = "当前账号未登录或令牌已失效。"
        const val UnknownErrorMessage = "创建问题时发生未知错误。"
    }
}