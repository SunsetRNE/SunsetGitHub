package com.Sunset.REN.GitHub.ui.repo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.Sunset.REN.GitHub.data.github.GitHubRepositoryApiGateway
import com.Sunset.REN.GitHub.data.local.EncryptedSharedPreferencesTokenStore
import com.Sunset.REN.GitHub.data.local.SharedPreferencesCurrentAccountStore
import com.Sunset.REN.GitHub.domain.repo.RepositorySecurityAlert
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RepositorySecurityAlertDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val currentAccountStore = SharedPreferencesCurrentAccountStore(application)
    private val tokenStore = EncryptedSharedPreferencesTokenStore(application)

    private val _detailState = MutableLiveData(RepositorySecurityAlertDetailUiState())
    val detailState: LiveData<RepositorySecurityAlertDetailUiState> = _detailState

    private var accessToken: String = ""
    private var hasPrepared = false

    fun prepare(
        owner: String,
        repo: String,
        alertType: String,
        number: Int,
        initialAlert: RepositorySecurityAlert?
    ) {
        if (hasPrepared) return
        hasPrepared = true
        _detailState.value = RepositorySecurityAlertDetailUiState(
            owner = owner,
            repo = repo,
            alertType = alertType,
            number = number,
            alert = initialAlert
        )
        if (owner.isNotBlank() && repo.isNotBlank() && alertType.isNotBlank() && number > 0) {
            load()
        }
    }

    fun load() {
        val state = _detailState.value ?: return
        if (state.owner.isBlank() || state.repo.isBlank() || state.alertType.isBlank() || state.number <= 0) return
        viewModelScope.launch {
            _detailState.value = state.copy(isLoading = true, errorMessage = null)
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) {
                _detailState.value = _detailState.value?.copy(
                    isLoading = false,
                    errorMessage = NotSignedInMessage
                )
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    GitHubRepositoryApiGateway(token).getRepositorySecurityAlert(
                        owner = state.owner,
                        repo = state.repo,
                        alertType = state.alertType,
                        number = state.number
                    )
                }
            }
            _detailState.value = result.fold(
                onSuccess = { alert ->
                    _detailState.value?.copy(
                        alert = alert,
                        isLoading = false,
                        errorMessage = null
                    )
                },
                onFailure = { error ->
                    _detailState.value?.copy(
                        isLoading = false,
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
        const val UnknownErrorMessage = "加载安全告警详情时发生未知错误。"
    }
}