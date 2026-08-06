package com.Sunset.REN.GitHub.ui.repo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.Sunset.REN.GitHub.data.github.html.GitHubHtmlParseResult
import com.Sunset.REN.GitHub.data.github.html.GitHubRepositoryActionsSettingsGateway
import com.Sunset.REN.GitHub.data.github.html.RepositoryActionsCacheItem
import com.Sunset.REN.GitHub.data.github.html.RepositoryActionsPermissionsSnapshot
import com.Sunset.REN.GitHub.data.github.html.RepositoryActionsSettingsSnapshot
import com.Sunset.REN.GitHub.data.github.html.RepositorySelectedActionsSnapshot
import com.Sunset.REN.GitHub.data.github.html.deleteSecret
import com.Sunset.REN.GitHub.data.github.html.deleteVariable
import com.Sunset.REN.GitHub.data.github.html.listSecrets
import com.Sunset.REN.GitHub.data.github.html.listVariables
import com.Sunset.REN.GitHub.data.github.html.updateAllowedActions
import com.Sunset.REN.GitHub.data.github.html.updateSelectedActions
import com.Sunset.REN.GitHub.data.github.html.upsertSecret
import com.Sunset.REN.GitHub.data.github.html.upsertVariable
import com.Sunset.REN.GitHub.data.local.EncryptedSharedPreferencesTokenStore
import com.Sunset.REN.GitHub.data.local.SharedPreferencesCurrentAccountStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RepositoryActionsSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val currentAccountStore = SharedPreferencesCurrentAccountStore(application)
    private val tokenStore = EncryptedSharedPreferencesTokenStore(application)
    private val _actionsSettingsState = MutableLiveData(RepositoryActionsSettingsUiState())
    val actionsSettingsState: LiveData<RepositoryActionsSettingsUiState> = _actionsSettingsState
    private var accessToken: String = ""
    private var hasPrepared = false

    fun prepare(owner: String, repo: String) { if (hasPrepared) return; hasPrepared = true; _actionsSettingsState.value = RepositoryActionsSettingsUiState(owner = owner, repo = repo); refresh() }

    fun refresh() {
        val state = _actionsSettingsState.value ?: return
        if (state.owner.isBlank() || state.repo.isBlank()) return
        viewModelScope.launch {
            _actionsSettingsState.value = state.copy(
                isLoading = true,
                isSaving = false,
                errorMessage = null,
                pendingMessage = null,
                sourceUrl = state.sourceUrl.takeIf { state.snapshot != null },
                isShowingStaleContent = state.snapshot != null
            )
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) { _actionsSettingsState.value = _actionsSettingsState.value?.copy(isLoading = false, isShowingStaleContent = false, errorMessage = NotSignedInMessage); return@launch }
            val result = withContext(Dispatchers.IO) { runCatching { GitHubRepositoryActionsSettingsGateway(token).loadActionsSettings(state.owner, state.repo) } }
            _actionsSettingsState.value = result.fold(onSuccess = { it.toUiState() }, onFailure = { error -> _actionsSettingsState.value?.copy(isLoading = false, isShowingStaleContent = false, errorMessage = error.message?.takeIf { it.isNotBlank() } ?: UnknownErrorMessage) })
        }
    }

    fun setActionsEnabled(enabled: Boolean) {
        val state = _actionsSettingsState.value ?: return
        val snapshot = state.snapshot ?: return
        if (!snapshot.canAdmin) { _actionsSettingsState.value = state.copy(errorMessage = ReadOnlyMessage); return }
        viewModelScope.launch {
            _actionsSettingsState.value = state.copy(isSaving = true, errorMessage = null, pendingMessage = if (enabled) EnablingMessage else DisablingMessage)
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) { _actionsSettingsState.value = state.copy(isSaving = false, errorMessage = NotSignedInMessage, pendingMessage = null); return@launch }
            val result = withContext(Dispatchers.IO) { runCatching { GitHubRepositoryActionsSettingsGateway(token).updateActionsEnabled(state.owner, state.repo, enabled) } }
            _actionsSettingsState.value = result.fold(onSuccess = { it.toActionsPermissionUiState(enabled) }, onFailure = { error -> _actionsSettingsState.value?.copy(isSaving = false, errorMessage = error.message?.takeIf { it.isNotBlank() } ?: UpdateFailedMessage, pendingMessage = null) })
        }
    }

    fun setWorkflowDefaultPermission(defaultPermission: String) {
        val state = _actionsSettingsState.value ?: return
        val snapshot = state.snapshot ?: return
        updateWorkflowPermissions(
            defaultPermission = defaultPermission,
            canApprovePullRequestReviews = snapshot.workflowPermissions?.canApprovePullRequestReviews ?: false
        )
    }

    fun setWorkflowPullRequestApproval(canApprovePullRequestReviews: Boolean) {
        val state = _actionsSettingsState.value ?: return
        val snapshot = state.snapshot ?: return
        updateWorkflowPermissions(
            defaultPermission = snapshot.workflowPermissions?.defaultWorkflowPermissions ?: "read",
            canApprovePullRequestReviews = canApprovePullRequestReviews
        )
    }

    fun setAllowedActions(allowedActions: String) {
        val state = _actionsSettingsState.value ?: return
        val snapshot = state.snapshot ?: return
        if (!snapshot.canAdmin) { _actionsSettingsState.value = state.copy(errorMessage = ReadOnlyMessage); return }
        viewModelScope.launch {
            _actionsSettingsState.value = state.copy(isSaving = true, errorMessage = null, pendingMessage = UpdatingAllowedActionsMessage)
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) { _actionsSettingsState.value = state.copy(isSaving = false, errorMessage = NotSignedInMessage, pendingMessage = null); return@launch }
            val result = withContext(Dispatchers.IO) { runCatching { GitHubRepositoryActionsSettingsGateway(token).updateAllowedActions(state.owner, state.repo, allowedActions) } }
            _actionsSettingsState.value = result.fold(
                onSuccess = { parseResult -> parseResult.toActionsPermissionUiState(snapshot.actionsPermissions?.enabled == true, AllowedActionsUpdatedMessage) },
                onFailure = { error -> _actionsSettingsState.value?.copy(isSaving = false, errorMessage = error.message?.takeIf { it.isNotBlank() } ?: UpdateFailedMessage, pendingMessage = null) }
            )
        }
    }

    fun setSelectedActions(githubOwnedAllowed: Boolean, verifiedAllowed: Boolean, patterns: List<String>) {
        val state = _actionsSettingsState.value ?: return
        val snapshot = state.snapshot ?: return
        if (!snapshot.canAdmin) { _actionsSettingsState.value = state.copy(errorMessage = ReadOnlyMessage); return }
        val selected = RepositorySelectedActionsSnapshot(githubOwnedAllowed, verifiedAllowed, patterns.map { it.trim() }.filter { it.isNotBlank() }.distinct())
        viewModelScope.launch {
            _actionsSettingsState.value = state.copy(isSaving = true, errorMessage = null, pendingMessage = UpdatingSelectedActionsMessage)
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) { _actionsSettingsState.value = state.copy(isSaving = false, errorMessage = NotSignedInMessage, pendingMessage = null); return@launch }
            val result = withContext(Dispatchers.IO) { runCatching { GitHubRepositoryActionsSettingsGateway(token).updateSelectedActions(state.owner, state.repo, selected) } }
            _actionsSettingsState.value = result.fold(
                onSuccess = { parseResult ->
                    val current = _actionsSettingsState.value ?: state
                    when (parseResult) {
                        is GitHubHtmlParseResult.Success -> current.copy(isSaving = false, snapshot = current.snapshot?.copy(selectedActions = parseResult.value), errorMessage = null, pendingMessage = SelectedActionsUpdatedMessage)
                        is GitHubHtmlParseResult.AccessDenied -> current.copy(isSaving = false, errorMessage = parseResult.message, pendingMessage = null, sourceUrl = parseResult.sourceUrl)
                        is GitHubHtmlParseResult.NotFound -> current.copy(isSaving = false, errorMessage = parseResult.message, pendingMessage = null, sourceUrl = parseResult.sourceUrl)
                        is GitHubHtmlParseResult.ParseError -> current.copy(isSaving = false, errorMessage = parseResult.message, pendingMessage = null, sourceUrl = parseResult.sourceUrl)
                    }
                },
                onFailure = { error -> _actionsSettingsState.value?.copy(isSaving = false, errorMessage = error.message?.takeIf { it.isNotBlank() } ?: UpdateFailedMessage, pendingMessage = null) }
            )
        }
    }

    fun loadSecretsAndVariables() {
        val state = _actionsSettingsState.value ?: return
        if (state.owner.isBlank() || state.repo.isBlank() || state.isSaving) return
        viewModelScope.launch {
            _actionsSettingsState.value = state.copy(isSaving = true, errorMessage = null, pendingMessage = LoadingStorageMessage)
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) { _actionsSettingsState.value = state.copy(isSaving = false, errorMessage = NotSignedInMessage, pendingMessage = null); return@launch }
            val gateway = GitHubRepositoryActionsSettingsGateway(token)
            val secrets = withContext(Dispatchers.IO) { runCatching { gateway.listSecrets(state.owner, state.repo) } }
            val variables = withContext(Dispatchers.IO) { runCatching { gateway.listVariables(state.owner, state.repo) } }
            var latest = _actionsSettingsState.value ?: state
            secrets.getOrNull()?.let { result -> latest = latest.copy(secrets = result.successValueOrNull() ?: latest.secrets, errorMessage = result.errorMessageOrNull() ?: latest.errorMessage) }
            variables.getOrNull()?.let { result -> latest = latest.copy(variables = result.successValueOrNull() ?: latest.variables, errorMessage = result.errorMessageOrNull() ?: latest.errorMessage) }
            _actionsSettingsState.value = latest.copy(isSaving = false, pendingMessage = StorageLoadedMessage)
        }
    }

    fun upsertVariable(name: String, value: String) {
        mutateStorage(SavingVariableMessage) { gateway, state -> gateway.upsertVariable(state.owner, state.repo, name.trim(), value) }
    }

    fun upsertSecret(name: String, value: String) {
        val normalized = name.trim()
        if (normalized.isBlank() || value.isBlank()) {
            _actionsSettingsState.value = _actionsSettingsState.value?.copy(errorMessage = SecretNameValueRequiredMessage)
            return
        }
        mutateStorage(SavingSecretMessage) { gateway, state -> gateway.upsertSecret(state.owner, state.repo, normalized, value) }
    }

    fun deleteVariable(name: String) {
        mutateStorage(DeletingVariableMessage) { gateway, state -> gateway.deleteVariable(state.owner, state.repo, name) }
    }
    fun deleteSecret(name: String) {
        mutateStorage(DeletingSecretMessage) { gateway, state -> gateway.deleteSecret(state.owner, state.repo, name) }
    }

    fun setRetentionDays(daysText: String) {
        val days = daysText.trim().toIntOrNull()
        if (days == null || days !in 1..400) {
            _actionsSettingsState.value = _actionsSettingsState.value?.copy(errorMessage = RetentionDaysInvalidMessage)
            return
        }
        val state = _actionsSettingsState.value ?: return
        if (state.snapshot?.canAdmin != true) { _actionsSettingsState.value = state.copy(errorMessage = ReadOnlyMessage); return }
        viewModelScope.launch {
            _actionsSettingsState.value = state.copy(isSaving = true, errorMessage = null, pendingMessage = UpdatingRetentionMessage)
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) { _actionsSettingsState.value = state.copy(isSaving = false, errorMessage = NotSignedInMessage, pendingMessage = null); return@launch }
            val result = withContext(Dispatchers.IO) { runCatching { GitHubRepositoryActionsSettingsGateway(token).updateRetentionDays(state.owner, state.repo, days) } }
            _actionsSettingsState.value = result.fold(
                onSuccess = { parseResult ->
                    val latest = _actionsSettingsState.value ?: state
                    when (parseResult) {
                        is GitHubHtmlParseResult.Success -> latest.copy(isSaving = false, snapshot = latest.snapshot?.copy(retentionDays = parseResult.value), errorMessage = null, pendingMessage = RetentionUpdatedMessage)
                        is GitHubHtmlParseResult.AccessDenied -> latest.copy(isSaving = false, errorMessage = parseResult.message, pendingMessage = null, sourceUrl = parseResult.sourceUrl)
                        is GitHubHtmlParseResult.NotFound -> latest.copy(isSaving = false, errorMessage = parseResult.message, pendingMessage = null, sourceUrl = parseResult.sourceUrl)
                        is GitHubHtmlParseResult.ParseError -> latest.copy(isSaving = false, errorMessage = parseResult.message, pendingMessage = null, sourceUrl = parseResult.sourceUrl)
                    }
                },
                onFailure = { error -> _actionsSettingsState.value?.copy(isSaving = false, errorMessage = error.message?.takeIf { it.isNotBlank() } ?: UpdateFailedMessage, pendingMessage = null) }
            )
        }
    }

    fun loadCaches() {
        val state = _actionsSettingsState.value ?: return
        if (state.owner.isBlank() || state.repo.isBlank() || state.isSaving) return
        viewModelScope.launch {
            _actionsSettingsState.value = state.copy(isSaving = true, errorMessage = null, pendingMessage = LoadingCachesMessage)
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) { _actionsSettingsState.value = state.copy(isSaving = false, errorMessage = NotSignedInMessage, pendingMessage = null); return@launch }
            val result = withContext(Dispatchers.IO) { runCatching { GitHubRepositoryActionsSettingsGateway(token).listCaches(state.owner, state.repo) } }
            _actionsSettingsState.value = result.fold(
                onSuccess = { parseResult ->
                    val latest = _actionsSettingsState.value ?: state
                    when (parseResult) {
                        is GitHubHtmlParseResult.Success -> latest.copy(isSaving = false, caches = parseResult.value, errorMessage = null, pendingMessage = CachesLoadedMessage)
                        is GitHubHtmlParseResult.AccessDenied -> latest.copy(isSaving = false, errorMessage = parseResult.message, pendingMessage = null, sourceUrl = parseResult.sourceUrl)
                        is GitHubHtmlParseResult.NotFound -> latest.copy(isSaving = false, errorMessage = parseResult.message, pendingMessage = null, sourceUrl = parseResult.sourceUrl)
                        is GitHubHtmlParseResult.ParseError -> latest.copy(isSaving = false, errorMessage = parseResult.message, pendingMessage = null, sourceUrl = parseResult.sourceUrl)
                    }
                },
                onFailure = { error -> _actionsSettingsState.value?.copy(isSaving = false, errorMessage = error.message?.takeIf { it.isNotBlank() } ?: UpdateFailedMessage, pendingMessage = null) }
            )
        }
    }

    fun deleteCache(cache: RepositoryActionsCacheItem) {
        mutateCache(DeletingCacheMessage) { gateway, state -> gateway.deleteCache(state.owner, state.repo, cache.id) }
    }

    fun deleteCachesByKey(key: String, ref: String?) {
        val normalizedKey = key.trim()
        if (normalizedKey.isBlank()) { _actionsSettingsState.value = _actionsSettingsState.value?.copy(errorMessage = CacheKeyRequiredMessage); return }
        mutateCache(DeletingCacheMessage) { gateway, state -> gateway.deleteCachesByKey(state.owner, state.repo, normalizedKey, ref?.trim()?.takeIf { it.isNotBlank() }) }
    }

    private fun mutateCache(message: String, action: (GitHubRepositoryActionsSettingsGateway, RepositoryActionsSettingsUiState) -> GitHubHtmlParseResult<*>) {
        val state = _actionsSettingsState.value ?: return
        if (state.snapshot?.canAdmin != true) { _actionsSettingsState.value = state.copy(errorMessage = ReadOnlyMessage); return }
        viewModelScope.launch {
            _actionsSettingsState.value = state.copy(isSaving = true, errorMessage = null, pendingMessage = message)
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) { _actionsSettingsState.value = state.copy(isSaving = false, errorMessage = NotSignedInMessage, pendingMessage = null); return@launch }
            val result = withContext(Dispatchers.IO) { runCatching { action(GitHubRepositoryActionsSettingsGateway(token), state) } }
            _actionsSettingsState.value = result.fold(
                onSuccess = { parseResult ->
                    val latest = _actionsSettingsState.value ?: state
                    when (parseResult) {
                        is GitHubHtmlParseResult.Success -> latest.copy(isSaving = false, errorMessage = null, pendingMessage = CacheDeletedMessage).also { loadCaches() }
                        is GitHubHtmlParseResult.AccessDenied -> latest.copy(isSaving = false, errorMessage = parseResult.message, pendingMessage = null, sourceUrl = parseResult.sourceUrl)
                        is GitHubHtmlParseResult.NotFound -> latest.copy(isSaving = false, errorMessage = parseResult.message, pendingMessage = null, sourceUrl = parseResult.sourceUrl)
                        is GitHubHtmlParseResult.ParseError -> latest.copy(isSaving = false, errorMessage = parseResult.message, pendingMessage = null, sourceUrl = parseResult.sourceUrl)
                    }
                },
                onFailure = { error -> _actionsSettingsState.value?.copy(isSaving = false, errorMessage = error.message?.takeIf { it.isNotBlank() } ?: UpdateFailedMessage, pendingMessage = null) }
            )
        }
    }

    private fun mutateStorage(message: String, action: (GitHubRepositoryActionsSettingsGateway, RepositoryActionsSettingsUiState) -> GitHubHtmlParseResult<Unit>) {
        val state = _actionsSettingsState.value ?: return
        if (state.snapshot?.canAdmin != true) { _actionsSettingsState.value = state.copy(errorMessage = ReadOnlyMessage); return }
        viewModelScope.launch {
            _actionsSettingsState.value = state.copy(isSaving = true, errorMessage = null, pendingMessage = message)
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) { _actionsSettingsState.value = state.copy(isSaving = false, errorMessage = NotSignedInMessage, pendingMessage = null); return@launch }
            val result = withContext(Dispatchers.IO) { runCatching { action(GitHubRepositoryActionsSettingsGateway(token), state) } }
            _actionsSettingsState.value = result.fold(
                onSuccess = { parseResult ->
                    when (parseResult) {
                        is GitHubHtmlParseResult.Success -> (_actionsSettingsState.value ?: state).copy(isSaving = false, errorMessage = null, pendingMessage = StorageUpdatedMessage).also { loadSecretsAndVariables() }
                        is GitHubHtmlParseResult.AccessDenied -> (_actionsSettingsState.value ?: state).copy(isSaving = false, errorMessage = parseResult.message, pendingMessage = null, sourceUrl = parseResult.sourceUrl)
                        is GitHubHtmlParseResult.NotFound -> (_actionsSettingsState.value ?: state).copy(isSaving = false, errorMessage = parseResult.message, pendingMessage = null, sourceUrl = parseResult.sourceUrl)
                        is GitHubHtmlParseResult.ParseError -> (_actionsSettingsState.value ?: state).copy(isSaving = false, errorMessage = parseResult.message, pendingMessage = null, sourceUrl = parseResult.sourceUrl)
                    }
                },
                onFailure = { error -> _actionsSettingsState.value?.copy(isSaving = false, errorMessage = error.message?.takeIf { it.isNotBlank() } ?: UpdateFailedMessage, pendingMessage = null) }
            )
        }
    }

    private fun updateWorkflowPermissions(defaultPermission: String, canApprovePullRequestReviews: Boolean) {
        val state = _actionsSettingsState.value ?: return
        val snapshot = state.snapshot ?: return
        if (!snapshot.canAdmin) { _actionsSettingsState.value = state.copy(errorMessage = ReadOnlyMessage); return }
        viewModelScope.launch {
            _actionsSettingsState.value = state.copy(isSaving = true, errorMessage = null, pendingMessage = UpdatingWorkflowPermissionMessage)
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) { _actionsSettingsState.value = state.copy(isSaving = false, errorMessage = NotSignedInMessage, pendingMessage = null); return@launch }
            val result = withContext(Dispatchers.IO) { runCatching { GitHubRepositoryActionsSettingsGateway(token).updateWorkflowPermissions(state.owner, state.repo, defaultPermission, canApprovePullRequestReviews) } }
            _actionsSettingsState.value = result.fold(
                onSuccess = {
                    val latest = _actionsSettingsState.value ?: state
                    latest.copy(
                        isSaving = false,
                        snapshot = latest.snapshot?.copy(
                            workflowPermissions = latest.snapshot.workflowPermissions?.copy(
                                defaultWorkflowPermissions = defaultPermission,
                                canApprovePullRequestReviews = canApprovePullRequestReviews
                            )
                        ),
                        errorMessage = null,
                        pendingMessage = WorkflowPermissionUpdatedMessage
                    )
                },
                onFailure = { error -> _actionsSettingsState.value?.copy(isSaving = false, errorMessage = error.message?.takeIf { it.isNotBlank() } ?: UpdateFailedMessage, pendingMessage = null) }
            )
        }
    }

    private fun GitHubHtmlParseResult<RepositoryActionsSettingsSnapshot>.toUiState(): RepositoryActionsSettingsUiState? {
        val current = _actionsSettingsState.value ?: return null
        return when (this) {
            is GitHubHtmlParseResult.Success -> current.copy(
                isLoading = false,
                snapshot = value,
                isShowingStaleContent = false,
                errorMessage = null,
                sourceUrl = value.sourceUrl
            )
            is GitHubHtmlParseResult.AccessDenied -> current.copy(
                isLoading = false,
                isShowingStaleContent = false,
                errorMessage = message,
                sourceUrl = sourceUrl
            )
            is GitHubHtmlParseResult.NotFound -> current.copy(
                isLoading = false,
                isShowingStaleContent = false,
                errorMessage = message,
                sourceUrl = sourceUrl
            )
            is GitHubHtmlParseResult.ParseError -> current.copy(
                isLoading = false,
                isShowingStaleContent = false,
                errorMessage = message,
                sourceUrl = sourceUrl
            )
        }
    }
private fun GitHubHtmlParseResult<RepositoryActionsPermissionsSnapshot>.toActionsPermissionUiState(enabled: Boolean): RepositoryActionsSettingsUiState? = toActionsPermissionUiState(enabled, if (enabled) EnabledMessage else DisabledMessage)
    private fun GitHubHtmlParseResult<RepositoryActionsPermissionsSnapshot>.toActionsPermissionUiState(enabled: Boolean, successMessage: String): RepositoryActionsSettingsUiState? { val current = _actionsSettingsState.value ?: return null; return when (this) { is GitHubHtmlParseResult.Success -> current.copy(isSaving = false, snapshot = current.snapshot?.copy(actionsPermissions = value), errorMessage = null, pendingMessage = successMessage); is GitHubHtmlParseResult.AccessDenied -> current.copy(isSaving = false, errorMessage = message, pendingMessage = null, sourceUrl = sourceUrl); is GitHubHtmlParseResult.NotFound -> current.copy(isSaving = false, errorMessage = message, pendingMessage = null, sourceUrl = sourceUrl); is GitHubHtmlParseResult.ParseError -> current.copy(isSaving = false, errorMessage = message, pendingMessage = null, sourceUrl = sourceUrl) } }
    private fun <T> GitHubHtmlParseResult<T>.successValueOrNull(): T? = (this as? GitHubHtmlParseResult.Success<T>)?.value
    private fun <T> GitHubHtmlParseResult<T>.errorMessageOrNull(): String? = when (this) { is GitHubHtmlParseResult.Success -> null; is GitHubHtmlParseResult.AccessDenied -> message; is GitHubHtmlParseResult.NotFound -> message; is GitHubHtmlParseResult.ParseError -> message }
    private suspend fun ensureAccessToken(): String? { if (accessToken.isNotBlank()) return accessToken; val account = withContext(Dispatchers.IO) { currentAccountStore.getCurrentAccount() } ?: return null; val token = withContext(Dispatchers.IO) { tokenStore.getAccessToken(account.id) }?.takeIf { it.isNotBlank() } ?: return null; accessToken = token; return token }

    private companion object {
        const val NotSignedInMessage = "当前账号未登录或令牌已失效。"
        const val UnknownErrorMessage = "加载工作流设置时发生未知错误。"
        const val ReadOnlyMessage = "当前账号没有管理员权限，无法修改工作流设置。"
        const val UpdateFailedMessage = "更新工作流设置时发生未知错误。"
        const val EnablingMessage = "正在启用工作流……"
        const val DisablingMessage = "正在关闭工作流……"
        const val EnabledMessage = "工作流已启用。"
        const val DisabledMessage = "工作流已关闭。"
        const val UpdatingWorkflowPermissionMessage = "正在更新 工作流默认权限……"
        const val WorkflowPermissionUpdatedMessage = "工作流默认权限已更新。"
        const val UpdatingAllowedActionsMessage = "正在更新 工作流允许运行范围……"
        const val AllowedActionsUpdatedMessage = "工作流允许运行范围已更新。"
        const val UpdatingSelectedActionsMessage = "正在更新 工作流白名单……"
        const val SelectedActionsUpdatedMessage = "工作流白名单已更新。"
        const val LoadingStorageMessage = "正在读取 密钥 / 变量……"
        const val StorageLoadedMessage = "密钥 / 变量 已刷新。"
        const val SavingVariableMessage = "正在保存 工作流变量……"
        const val SavingSecretMessage = "正在加密并保存 工作流密钥……"
        const val SecretNameValueRequiredMessage = "请输入密钥名称和值。"
        const val DeletingVariableMessage = "正在删除 工作流变量……"
        const val DeletingSecretMessage = "正在删除 工作流密钥……"
        const val StorageUpdatedMessage = "密钥 / 变量 已更新。"
        const val UpdatingRetentionMessage = "正在更新 工作流产物与日志保留天数……"
        const val RetentionUpdatedMessage = "工作流产物与日志保留天数已更新。"
        const val RetentionDaysInvalidMessage = "请输入 1 到 400 之间的保留天数。"
        const val LoadingCachesMessage = "正在读取 工作流缓存……"
        const val CachesLoadedMessage = "工作流缓存已刷新。"
        const val DeletingCacheMessage = "正在删除 工作流缓存……"
        const val CacheDeletedMessage = "工作流缓存已删除。"
        const val CacheKeyRequiredMessage = "请输入要清理的缓存 key。"
    }
}