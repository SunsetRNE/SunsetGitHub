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

class RepositorySecurityViewModel(application: Application) : AndroidViewModel(application) {

    private val currentAccountStore = SharedPreferencesCurrentAccountStore(application)
    private val tokenStore = EncryptedSharedPreferencesTokenStore(application)

    private val _securityState = MutableLiveData(RepositorySecurityUiState())
    val securityState: LiveData<RepositorySecurityUiState> = _securityState

    private var accessToken: String = ""
    private var hasPrepared = false

    fun prepare(owner: String, repo: String) {
        if (hasPrepared) return
        hasPrepared = true
        _securityState.value = RepositorySecurityUiState(owner = owner, repo = repo)
        refresh()
    }

    fun refresh() {
        val state = _securityState.value ?: return
        if (state.owner.isBlank() || state.repo.isBlank()) return
        viewModelScope.launch {
            _securityState.value = state.copy(isLoading = true, errorMessage = null)
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) {
                _securityState.value = _securityState.value?.copy(
                    isLoading = false,
                    errorMessage = NotSignedInMessage
                )
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    GitHubRepositoryApiGateway(token).getRepositorySecuritySummary(
                        owner = state.owner,
                        repo = state.repo
                    )
                }
            }
            _securityState.value = result.fold(
                onSuccess = { summary ->
                    _securityState.value?.copy(
                        summary = summary,
                        isLoading = false,
                        errorMessage = null
                    )
                },
                onFailure = { error ->
                    _securityState.value?.copy(
                        isLoading = false,
                        errorMessage = error.message?.takeIf { it.isNotBlank() } ?: UnknownErrorMessage
                    )
                }
            )
            if (result.isSuccess) {
                loadFirstAlertPage()
            }
        }
    }

    fun switchAlertType(alertType: String) {
        val current = _securityState.value ?: return
        if (current.alertFilter.alertType == alertType) return
        _securityState.value = current.resetAlerts(
            alertFilter = current.alertFilter.copy(
                alertType = alertType,
                alertState = compatibleAlertState(alertType, current.alertFilter.alertState)
            )
        )
        loadFirstAlertPage()
    }

    fun switchAlertState(alertState: String?) {
        val current = _securityState.value ?: return
        if (current.alertFilter.alertState == alertState) return
        _securityState.value = current.resetAlerts(
            alertFilter = current.alertFilter.copy(alertState = alertState)
        )
        loadFirstAlertPage()
    }

    fun loadFirstAlertPage() {
        val state = _securityState.value ?: return
        if (state.owner.isBlank() || state.repo.isBlank()) return
        viewModelScope.launch {
            _securityState.value = state.copy(
                isLoadingAlerts = true,
                isLoadingMoreAlerts = false,
                alertsErrorMessage = null,
                isShowingStaleAlerts = state.alerts.isNotEmpty()
            )
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) {
                _securityState.value = _securityState.value?.copy(
                    isLoadingAlerts = false,
                    isShowingStaleAlerts = false,
                    alertsErrorMessage = NotSignedInMessage
                )
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    GitHubRepositoryApiGateway(token).listRepositorySecurityAlerts(
                        owner = state.owner,
                        repo = state.repo,
                        alertType = state.alertFilter.alertType,
                        alertState = state.alertFilter.alertState,
                        page = 1,
                        perPage = PageSize
                    )
                }
            }
            _securityState.value = result.fold(
                onSuccess = { alerts ->
                    _securityState.value?.copy(
                        alerts = alerts,
                        isLoadingAlerts = false,
                        isLoadingMoreAlerts = false,
                        alertsErrorMessage = null,
                        hasMoreAlerts = alerts.size >= PageSize,
                        loadedAlertPages = 1,
                        isShowingStaleAlerts = false
                    )
                },
                onFailure = { error ->
                    _securityState.value?.copy(
                        isLoadingAlerts = false,
                        isLoadingMoreAlerts = false,
                        alertsErrorMessage = error.message?.takeIf { it.isNotBlank() } ?: UnknownAlertsErrorMessage,
                        hasMoreAlerts = false,
                        isShowingStaleAlerts = false
                    )
                }
            )
        }
    }

    fun loadNextAlertPage() {
        val state = _securityState.value ?: return
        if (state.isLoadingAlerts || state.isLoadingMoreAlerts || !state.hasMoreAlerts) return
        if (state.owner.isBlank() || state.repo.isBlank()) return
        viewModelScope.launch {
            _securityState.value = state.copy(isLoadingMoreAlerts = true, alertsErrorMessage = null)
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) {
                _securityState.value = _securityState.value?.copy(
                    isLoadingMoreAlerts = false,
                    alertsErrorMessage = NotSignedInMessage
                )
                return@launch
            }
            val nextPage = state.loadedAlertPages + 1
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    GitHubRepositoryApiGateway(token).listRepositorySecurityAlerts(
                        owner = state.owner,
                        repo = state.repo,
                        alertType = state.alertFilter.alertType,
                        alertState = state.alertFilter.alertState,
                        page = nextPage,
                        perPage = PageSize
                    )
                }
            }
            _securityState.value = result.fold(
                onSuccess = { alerts ->
                    val current = _securityState.value ?: return@fold null
                    current.copy(
                        alerts = current.alerts + alerts,
                        isLoadingMoreAlerts = false,
                        alertsErrorMessage = null,
                        hasMoreAlerts = alerts.size >= PageSize,
                        loadedAlertPages = nextPage
                    )
                },
                onFailure = { error ->
                    _securityState.value?.copy(
                        isLoadingMoreAlerts = false,
                        alertsErrorMessage = error.message?.takeIf { it.isNotBlank() } ?: UnknownAlertsErrorMessage
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

    private fun RepositorySecurityUiState.resetAlerts(
        alertFilter: RepositorySecurityAlertFilter = this.alertFilter
    ): RepositorySecurityUiState = copy(
        alertFilter = alertFilter,
        isLoadingAlerts = false,
        isLoadingMoreAlerts = false,
        alertsErrorMessage = null,
        hasMoreAlerts = true,
        loadedAlertPages = 0,
        isShowingStaleAlerts = alerts.isNotEmpty()
    )

    private fun compatibleAlertState(alertType: String, alertState: String?): String? {
        if (alertState == null) return null
        return alertState.takeIf { it in supportedAlertStates(alertType) } ?: RepositorySecurityUiState.AlertStateOpen
    }

    private fun supportedAlertStates(alertType: String): Set<String> = when (alertType) {
        RepositorySecurityUiState.AlertTypeSecretScanning -> setOf(
            RepositorySecurityUiState.AlertStateOpen,
            RepositorySecurityUiState.AlertStateResolved
        )
        RepositorySecurityUiState.AlertTypeDependabot -> setOf(
            RepositorySecurityUiState.AlertStateOpen,
            RepositorySecurityUiState.AlertStateFixed,
            RepositorySecurityUiState.AlertStateDismissed,
            RepositorySecurityUiState.AlertStateAutoDismissed
        )
        else -> setOf(
            RepositorySecurityUiState.AlertStateOpen,
            RepositorySecurityUiState.AlertStateFixed,
            RepositorySecurityUiState.AlertStateDismissed
        )
    }

    private companion object {
        const val PageSize = 20
        const val NotSignedInMessage = "当前账号未登录或令牌已失效。"
        const val UnknownErrorMessage = "加载安全与质量摘要时发生未知错误。"
        const val UnknownAlertsErrorMessage = "加载安全告警列表时发生未知错误。"
    }
}