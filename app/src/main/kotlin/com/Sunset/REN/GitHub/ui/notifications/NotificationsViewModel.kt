package com.Sunset.REN.GitHub.ui.notifications

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

class NotificationsViewModel(application: Application) : AndroidViewModel(application) {

    private val currentAccountStore = SharedPreferencesCurrentAccountStore(application)
    private val tokenStore = EncryptedSharedPreferencesTokenStore(application)

    private val _notificationsState = MutableLiveData(NotificationsUiState())
    val notificationsState: LiveData<NotificationsUiState> = _notificationsState

    private var accessToken: String = ""

    init {
        loadFirstPage()
    }

    fun switchAll(all: Boolean) {
        val current = _notificationsState.value ?: return
        if (current.all == all) return
        _notificationsState.value = current.copy(
            all = all,
            notifications = emptyList(),
            isLoading = false,
            isLoadingMore = false,
            errorMessage = null,
            hasMore = false,
            loadedPages = 0
        )
        loadFirstPage()
    }

    fun loadFirstPage() {
        val state = _notificationsState.value ?: NotificationsUiState()
        viewModelScope.launch {
            _notificationsState.value = state.copy(isLoading = true, isLoadingMore = false, errorMessage = null)
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) {
                _notificationsState.value = _notificationsState.value?.copy(
                    isLoading = false,
                    errorMessage = NotSignedInMessage
                )
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    GitHubRepositoryApiGateway(token).listNotifications(
                        all = state.all,
                        page = 1,
                        perPage = PageSize
                    )
                }
            }
            _notificationsState.value = result.fold(
                onSuccess = { notifications ->
                    _notificationsState.value?.copy(
                        notifications = notifications,
                        isLoading = false,
                        isLoadingMore = false,
                        errorMessage = null,
                        hasMore = notifications.size >= PageSize,
                        loadedPages = 1
                    )
                },
                onFailure = { error ->
                    _notificationsState.value?.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        errorMessage = error.message?.takeIf { it.isNotBlank() } ?: UnknownErrorMessage
                    )
                }
            )
        }
    }

    fun loadNextPage() {
        val state = _notificationsState.value ?: return
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return
        viewModelScope.launch {
            _notificationsState.value = state.copy(isLoadingMore = true, errorMessage = null)
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) {
                _notificationsState.value = _notificationsState.value?.copy(
                    isLoadingMore = false,
                    errorMessage = NotSignedInMessage
                )
                return@launch
            }
            val nextPage = state.loadedPages + 1
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    GitHubRepositoryApiGateway(token).listNotifications(
                        all = state.all,
                        page = nextPage,
                        perPage = PageSize
                    )
                }
            }
            _notificationsState.value = result.fold(
                onSuccess = { notifications ->
                    val current = _notificationsState.value ?: return@fold null
                    current.copy(
                        notifications = current.notifications + notifications,
                        isLoadingMore = false,
                        errorMessage = null,
                        hasMore = notifications.size >= PageSize,
                        loadedPages = nextPage
                    )
                },
                onFailure = { error ->
                    _notificationsState.value?.copy(
                        isLoadingMore = false,
                        errorMessage = error.message?.takeIf { it.isNotBlank() } ?: UnknownErrorMessage
                    )
                }
            )
        }
    }
    fun markAsRead(notificationId: String) {
        runThreadAction(notificationId) { gateway, id -> gateway.markNotificationThreadAsRead(id) }
    }

    fun markAsDone(notificationId: String) {
        runThreadAction(notificationId) { gateway, id -> gateway.markNotificationThreadAsDone(id) }
    }

    fun unsubscribe(notificationId: String) {
        runThreadAction(notificationId) { gateway, id -> gateway.unsubscribeNotificationThread(id) }
    }

    fun subscribe(notificationId: String) {
        runThreadAction(notificationId) { gateway, id -> gateway.subscribeNotificationThread(id) }
    }

    private fun runThreadAction(
        notificationId: String,
        action: suspend (GitHubRepositoryApiGateway, String) -> Unit
    ) {
        if (notificationId.isBlank()) return
        viewModelScope.launch {
            _notificationsState.value = _notificationsState.value?.copy(errorMessage = null)
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) {
                _notificationsState.value = _notificationsState.value?.copy(errorMessage = NotSignedInMessage)
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                runCatching { action(GitHubRepositoryApiGateway(token), notificationId) }
            }
            result.fold(
                onSuccess = { loadFirstPage() },
                onFailure = { error ->
                    _notificationsState.value = _notificationsState.value?.copy(
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
        const val PageSize = 20
        const val NotSignedInMessage = "当前账号未登录或令牌已失效。"
        const val UnknownErrorMessage = "加载通知时发生未知错误。"
    }
}