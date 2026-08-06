package com.Sunset.REN.GitHub.ui.repo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.Sunset.REN.GitHub.data.github.html.GitHubHtmlParseResult
import com.Sunset.REN.GitHub.data.github.html.GitHubRepositorySettingsGateway
import com.Sunset.REN.GitHub.data.github.html.RepositorySettingsSnapshot
import com.Sunset.REN.GitHub.data.github.html.RepositorySettingsUpdateRequest
import com.Sunset.REN.GitHub.data.local.EncryptedSharedPreferencesTokenStore
import com.Sunset.REN.GitHub.data.local.RepositorySettingsCacheStore
import com.Sunset.REN.GitHub.data.local.SharedPreferencesCurrentAccountStore
import com.Sunset.REN.GitHub.ui.repo.RepositorySettingsVisibility.Internal
import com.Sunset.REN.GitHub.ui.repo.RepositorySettingsVisibility.Private
import com.Sunset.REN.GitHub.ui.repo.RepositorySettingsVisibility.Public
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RepositorySettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val currentAccountStore = SharedPreferencesCurrentAccountStore(application)
    private val tokenStore = EncryptedSharedPreferencesTokenStore(application)
    private val settingsCacheStore = RepositorySettingsCacheStore(application)

    private val _settingsState = MutableLiveData(RepositorySettingsUiState())
    val settingsState: LiveData<RepositorySettingsUiState> = _settingsState

    private var accessToken: String = ""
    private var hasPrepared = false

    fun prepare(owner: String, repo: String) {
        if (hasPrepared) return
        hasPrepared = true
        _settingsState.value = RepositorySettingsUiState(owner = owner, repo = repo)
        refresh(forceRefresh = false)
    }

    fun refresh() {
        refresh(forceRefresh = true)
    }

    private fun refresh(forceRefresh: Boolean) {
        val state = _settingsState.value ?: return
        if (state.owner.isBlank() || state.repo.isBlank()) return
        viewModelScope.launch {
            val cachedSettings = withContext(Dispatchers.IO) {
                settingsCacheStore.getCachedSettings(state.owner, state.repo)
            }
            if (!forceRefresh && cachedSettings != null) {
                val shouldRefresh = shouldRefresh(cachedSettings.refreshedAtMillis)
                _settingsState.value = cachedSettings.snapshot.toUiState(
                    refreshedAtMillis = cachedSettings.refreshedAtMillis,
                    isShowingCachedContent = true,
                    isLoading = shouldRefresh
                )
                if (!shouldRefresh) return@launch
            } else {
                _settingsState.value = state.copy(isLoading = true, errorMessage = null, pendingMessage = null)
            }

            val latestState = _settingsState.value ?: state
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) {
                _settingsState.value = latestState.copy(
                    isLoading = false,
                    errorMessage = NotSignedInMessage
                )
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    GitHubRepositorySettingsGateway(token).loadSettingsSnapshot(latestState.owner, latestState.repo)
                }
            }
            _settingsState.value = result.fold(
                onSuccess = { parseResult ->
                    when (parseResult) {
                        is GitHubHtmlParseResult.Success -> {
                            val refreshedAtMillis = System.currentTimeMillis()
                            settingsCacheStore.cacheSettings(
                                parseResult.value.owner,
                                parseResult.value.repo,
                                parseResult.value,
                                refreshedAtMillis
                            )
                            parseResult.value.toUiState(refreshedAtMillis = refreshedAtMillis)
                        }
                        is GitHubHtmlParseResult.AccessDenied -> latestState.copy(
                            isLoading = false,
                            errorMessage = parseResult.message,
                            sourceUrl = parseResult.sourceUrl
                        )
                        is GitHubHtmlParseResult.NotFound -> latestState.copy(
                            isLoading = false,
                            errorMessage = parseResult.message,
                            sourceUrl = parseResult.sourceUrl
                        )
                        is GitHubHtmlParseResult.ParseError -> latestState.copy(
                            isLoading = false,
                            errorMessage = parseResult.message,
                            sourceUrl = parseResult.sourceUrl
                        )
                    }
                },
                onFailure = { error ->
                    latestState.copy(
                        isLoading = false,
                        errorMessage = error.message?.takeIf { it.isNotBlank() } ?: UnknownErrorMessage
                    )
                }
            )
        }
    }

    fun updateField(key: RepositorySettingsEditableFieldKey, value: String) {
        val state = _settingsState.value ?: return
        val snapshot = state.snapshot ?: return
        if (!snapshot.canAdmin) {
            _settingsState.value = state.copy(errorMessage = ReadOnlyMessage)
            return
        }
        val trimmed = value.trim()
        val request = when (key) {
            RepositorySettingsEditableFieldKey.Name -> RepositorySettingsUpdateRequest(name = trimmed)
            RepositorySettingsEditableFieldKey.Description -> RepositorySettingsUpdateRequest(description = trimmed)
            RepositorySettingsEditableFieldKey.Homepage -> RepositorySettingsUpdateRequest(homepage = trimmed)
            RepositorySettingsEditableFieldKey.DefaultBranch -> RepositorySettingsUpdateRequest(defaultBranch = trimmed)
        }
        saveSettings(request, SavingMessage)
    }

    fun updateVisibility(visibility: RepositorySettingsVisibility) {
        val state = _settingsState.value ?: return
        val snapshot = state.snapshot ?: return
        if (!snapshot.canAdmin) {
            _settingsState.value = state.copy(errorMessage = ReadOnlyMessage)
            return
        }
        val apiValue = when (visibility) {
            Public -> "public"
            Internal -> "internal"
            Private -> "private"
        }
        saveSettings(RepositorySettingsUpdateRequest(visibility = apiValue), SavingMessage)
    }

    fun updateToggle(key: RepositorySettingsToggleKey, checked: Boolean) {
        val state = _settingsState.value ?: return
        val snapshot = state.snapshot ?: return
        if (!snapshot.canAdmin) {
            _settingsState.value = state.copy(errorMessage = ReadOnlyMessage)
            return
        }
        if (key.isMergeStrategyKey() && !checked && snapshot.enabledMergeStrategyCount() <= 1) {
            _settingsState.value = state.copy(errorMessage = MergeStrategyRequiredMessage)
            return
        }
        val request = when (key) {
            RepositorySettingsToggleKey.HasIssues -> RepositorySettingsUpdateRequest(hasIssues = checked)
            RepositorySettingsToggleKey.HasProjects -> RepositorySettingsUpdateRequest(hasProjects = checked)
            RepositorySettingsToggleKey.HasWiki -> RepositorySettingsUpdateRequest(hasWiki = checked)
            RepositorySettingsToggleKey.HasDiscussions -> RepositorySettingsUpdateRequest(hasDiscussions = checked)
            RepositorySettingsToggleKey.AllowForking -> RepositorySettingsUpdateRequest(allowForking = checked)
            RepositorySettingsToggleKey.Archived -> RepositorySettingsUpdateRequest(archived = checked)
            RepositorySettingsToggleKey.AllowSquashMerge -> RepositorySettingsUpdateRequest(allowSquashMerge = checked)
            RepositorySettingsToggleKey.AllowMergeCommit -> RepositorySettingsUpdateRequest(allowMergeCommit = checked)
            RepositorySettingsToggleKey.AllowRebaseMerge -> RepositorySettingsUpdateRequest(allowRebaseMerge = checked)
            RepositorySettingsToggleKey.DeleteBranchOnMerge -> RepositorySettingsUpdateRequest(deleteBranchOnMerge = checked)
            RepositorySettingsToggleKey.AllowAutoMerge -> RepositorySettingsUpdateRequest(allowAutoMerge = checked)
        }
        saveSettings(request, SavingMessage)
    }

    fun deleteRepository(confirmFullName: String) {
        val state = _settingsState.value ?: return
        val snapshot = state.snapshot ?: return
        if (!snapshot.canAdmin) {
            _settingsState.value = state.copy(errorMessage = ReadOnlyMessage)
            return
        }
        if (confirmFullName.trim() != snapshot.fullName) {
            _settingsState.value = state.copy(errorMessage = DeleteConfirmationMismatchMessage)
            return
        }
        viewModelScope.launch {
            _settingsState.value = state.copy(isSaving = true, errorMessage = null, pendingMessage = DeletingRepositoryMessage)
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) {
                _settingsState.value = state.copy(isSaving = false, errorMessage = NotSignedInMessage, pendingMessage = null)
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                runCatching { GitHubRepositorySettingsGateway(token).deleteRepository(state.owner, state.repo) }
            }
            _settingsState.value = result.fold(
                onSuccess = { state.copy(isSaving = false, screen = null, snapshot = null, errorMessage = RepositoryDeletedMessage, pendingMessage = null) },
                onFailure = { error -> state.copy(isSaving = false, errorMessage = error.message?.takeIf { it.isNotBlank() } ?: DeleteRepositoryFailedMessage, pendingMessage = null) }
            )
        }
    }

    private fun saveSettings(request: RepositorySettingsUpdateRequest, pendingMessage: String) {
        val state = _settingsState.value ?: return
        if (state.owner.isBlank() || state.repo.isBlank()) return
        viewModelScope.launch {
            _settingsState.value = state.copy(isSaving = true, errorMessage = null, pendingMessage = pendingMessage)
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) {
                _settingsState.value = state.copy(isSaving = false, errorMessage = NotSignedInMessage, pendingMessage = null)
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    GitHubRepositorySettingsGateway(token).updateSettings(state.owner, state.repo, request)
                }
            }
            _settingsState.value = result.fold(
                onSuccess = { snapshot ->
                    val refreshedAtMillis = System.currentTimeMillis()
                    if (!state.owner.equals(snapshot.owner, ignoreCase = true) || !state.repo.equals(snapshot.repo, ignoreCase = true)) {
                        settingsCacheStore.clearCachedSettings(state.owner, state.repo)
                    }
                    settingsCacheStore.cacheSettings(snapshot.owner, snapshot.repo, snapshot, refreshedAtMillis)
                    snapshot.toUiState(
                        isSaving = false,
                        pendingMessage = SaveSuccessMessage,
                        refreshedAtMillis = refreshedAtMillis
                    )
                },
                onFailure = { error ->
                    state.copy(
                        isSaving = false,
                        errorMessage = error.message?.takeIf { it.isNotBlank() } ?: UpdateFailedMessage,
                        pendingMessage = null
                    )
                }
            )
        }
    }

    private fun RepositorySettingsSnapshot.toUiState(
        isLoading: Boolean = false,
        isSaving: Boolean = false,
        pendingMessage: String? = null,
        refreshedAtMillis: Long? = null,
        isShowingCachedContent: Boolean = false
    ): RepositorySettingsUiState {
        return RepositorySettingsUiState(
            owner = owner,
            repo = repo,
            screen = toScreenState(),
            snapshot = this,
            isLoading = isLoading,
            isSaving = isSaving,
            errorMessage = null,
            sourceUrl = sourceUrl,
            pendingMessage = pendingMessage,
            refreshedAtMillis = refreshedAtMillis,
            isShowingCachedContent = isShowingCachedContent
        )
    }

    private fun shouldRefresh(refreshedAtMillis: Long): Boolean {
        if (refreshedAtMillis <= 0L) return true
        return System.currentTimeMillis() - refreshedAtMillis > SettingsCacheTtlMillis
    }

    private suspend fun ensureAccessToken(): String? {
        if (accessToken.isNotBlank()) return accessToken
        val account = withContext(Dispatchers.IO) { currentAccountStore.getCurrentAccount() } ?: return null
        val token = withContext(Dispatchers.IO) { tokenStore.getAccessToken(account.id) }
            ?.takeIf { it.isNotBlank() } ?: return null
        accessToken = token
        return token
    }

    private fun RepositorySettingsSnapshot.enabledMergeStrategyCount(): Int {
    return listOf(allowSquashMerge, allowMergeCommit, allowRebaseMerge).count { it }
}

private fun RepositorySettingsToggleKey.isMergeStrategyKey(): Boolean {
    return this == RepositorySettingsToggleKey.AllowSquashMerge ||
        this == RepositorySettingsToggleKey.AllowMergeCommit ||
        this == RepositorySettingsToggleKey.AllowRebaseMerge
}

private companion object {
        const val NotSignedInMessage = "当前账号未登录或令牌已失效。"
        const val UnknownErrorMessage = "加载仓库设置页面时发生未知错误。"
        const val ReadOnlyMessage = "当前账号没有管理员权限，无法修改仓库设置。"
        const val SavingMessage = "正在提交仓库设置……"
        const val SaveSuccessMessage = "仓库设置已更新。"
        const val UpdateFailedMessage = "更新仓库设置时发生未知错误。"
        const val MergeStrategyRequiredMessage = "至少需要保留一种 Pull Request 合并方式。"
        const val DeletingRepositoryMessage = "正在删除仓库……"
        const val RepositoryDeletedMessage = "仓库已删除。"
        const val DeleteRepositoryFailedMessage = "删除仓库时发生未知错误。"
        const val DeleteConfirmationMismatchMessage = "确认文本不匹配，请输入完整仓库名。"
        const val SettingsCacheTtlMillis = 30L * 60L * 1_000L
    }
}