package com.Sunset.REN.GitHub.ui.repo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.Sunset.REN.GitHub.data.github.html.GitHubHtmlParseResult
import com.Sunset.REN.GitHub.data.github.html.GitHubRepositorySettingsGateway
import com.Sunset.REN.GitHub.data.github.html.RepositorySettingsUpdateRequest
import com.Sunset.REN.GitHub.data.local.EncryptedSharedPreferencesTokenStore
import com.Sunset.REN.GitHub.data.local.RepositorySettingsCacheStore
import com.Sunset.REN.GitHub.data.local.SharedPreferencesCurrentAccountStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RepositoryDangerZoneViewModel(application: Application) : AndroidViewModel(application) {
    private val currentAccountStore = SharedPreferencesCurrentAccountStore(application)
    private val tokenStore = EncryptedSharedPreferencesTokenStore(application)
    private val settingsCacheStore = RepositorySettingsCacheStore(application)
    private val _state = MutableLiveData(RepositoryDangerZoneUiState())
    val state: LiveData<RepositoryDangerZoneUiState> = _state
    private var accessToken: String = ""
    private var hasPrepared = false

    fun prepare(owner: String, repo: String) {
        if (hasPrepared) return
        hasPrepared = true
        _state.value = RepositoryDangerZoneUiState(owner = owner, repo = repo)
        refresh()
    }

    fun refresh() {
        val state = _state.value ?: return
        if (state.owner.isBlank() || state.repo.isBlank()) return
        viewModelScope.launch {
            _state.value = state.copy(isLoading = true, isSaving = false, errorMessage = null, pendingMessage = null)
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) { _state.value = state.copy(isLoading = false, errorMessage = NotSignedInMessage); return@launch }
            val result = withContext(Dispatchers.IO) { runCatching { GitHubRepositorySettingsGateway(token).loadSettingsSnapshot(state.owner, state.repo) } }
            _state.value = result.fold(
                onSuccess = { parseResult ->
                    val latest = _state.value ?: state
                    when (parseResult) {
                        is GitHubHtmlParseResult.Success -> latest.copy(isLoading = false, snapshot = parseResult.value, errorMessage = null, sourceUrl = parseResult.value.sourceUrl)
                        is GitHubHtmlParseResult.AccessDenied -> latest.copy(isLoading = false, errorMessage = parseResult.message, sourceUrl = parseResult.sourceUrl)
                        is GitHubHtmlParseResult.NotFound -> latest.copy(isLoading = false, errorMessage = parseResult.message, sourceUrl = parseResult.sourceUrl)
                        is GitHubHtmlParseResult.ParseError -> latest.copy(isLoading = false, errorMessage = parseResult.message, sourceUrl = parseResult.sourceUrl)
                    }
                },
                onFailure = { error -> (_state.value ?: state).copy(isLoading = false, errorMessage = error.message ?: UnknownErrorMessage) }
            )
        }
    }

    fun setArchived(archived: Boolean) {
        val state = _state.value ?: return
        val snapshot = state.snapshot ?: return
        if (!snapshot.canAdmin) { _state.value = state.copy(errorMessage = ReadOnlyMessage); return }
        viewModelScope.launch {
            _state.value = state.copy(isSaving = true, errorMessage = null, pendingMessage = if (archived) ArchivingMessage else UnarchivingMessage)
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) { _state.value = state.copy(isSaving = false, errorMessage = NotSignedInMessage, pendingMessage = null); return@launch }
            val result = withContext(Dispatchers.IO) { runCatching { GitHubRepositorySettingsGateway(token).updateSettings(state.owner, state.repo, RepositorySettingsUpdateRequest(archived = archived)) } }
            _state.value = result.fold(
                onSuccess = { updated ->
                    if (!state.owner.equals(updated.owner, ignoreCase = true) || !state.repo.equals(updated.repo, ignoreCase = true)) {
                        settingsCacheStore.clearCachedSettings(state.owner, state.repo)
                    }
                    settingsCacheStore.cacheSettings(updated.owner, updated.repo, updated, System.currentTimeMillis())
                    (_state.value ?: state).copy(owner = updated.owner, repo = updated.repo, isSaving = false, snapshot = updated, errorMessage = null, pendingMessage = if (archived) ArchivedMessage else UnarchivedMessage)
                },
                onFailure = { error -> (_state.value ?: state).copy(isSaving = false, errorMessage = error.message ?: UpdateFailedMessage, pendingMessage = null) }
            )
        }
    }

    fun transferRepository(newOwner: String, confirmFullName: String) {
        val state = _state.value ?: return
        val snapshot = state.snapshot ?: return
        val normalizedOwner = newOwner.trim().trimStart('@')
        if (!snapshot.canAdmin) { _state.value = state.copy(errorMessage = ReadOnlyMessage); return }
        if (normalizedOwner.isBlank()) { _state.value = state.copy(errorMessage = TransferOwnerRequiredMessage); return }
        if (confirmFullName.trim() != snapshot.fullName) { _state.value = state.copy(errorMessage = DeleteConfirmationMismatchMessage); return }
        viewModelScope.launch {
            _state.value = state.copy(isSaving = true, errorMessage = null, pendingMessage = TransferringMessage)
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) { _state.value = state.copy(isSaving = false, errorMessage = NotSignedInMessage, pendingMessage = null); return@launch }
            val result = withContext(Dispatchers.IO) { runCatching { GitHubRepositorySettingsGateway(token).transferRepository(state.owner, state.repo, normalizedOwner) } }
            _state.value = result.fold(
                onSuccess = {
                    settingsCacheStore.clearCachedSettings(state.owner, state.repo)
                    state.copy(
                        owner = normalizedOwner,
                        isSaving = false,
                        snapshot = snapshot.copy(owner = normalizedOwner, fullName = "$normalizedOwner/${snapshot.name}"),
                        errorMessage = null,
                        pendingMessage = "仓库转移请求已提交。新地址通常为 $normalizedOwner/${snapshot.name}，请稍后刷新仓库列表或打开新地址确认。"
                    )
                },
                onFailure = { error -> state.copy(isSaving = false, errorMessage = error.message ?: TransferFailedMessage, pendingMessage = null) }
            )
        }
    }

    fun deleteRepository(confirmFullName: String) {
        val state = _state.value ?: return
        val snapshot = state.snapshot ?: return
        if (!snapshot.canAdmin) { _state.value = state.copy(errorMessage = ReadOnlyMessage); return }
        if (confirmFullName.trim() != snapshot.fullName) { _state.value = state.copy(errorMessage = DeleteConfirmationMismatchMessage); return }
        viewModelScope.launch {
            _state.value = state.copy(isSaving = true, errorMessage = null, pendingMessage = DeletingMessage)
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) { _state.value = state.copy(isSaving = false, errorMessage = NotSignedInMessage, pendingMessage = null); return@launch }
            val result = withContext(Dispatchers.IO) { runCatching { GitHubRepositorySettingsGateway(token).deleteRepository(state.owner, state.repo) } }
            _state.value = result.fold(
                onSuccess = {
                    settingsCacheStore.clearCachedSettings(state.owner, state.repo)
                    state.copy(isSaving = false, isDeleted = true, snapshot = null, errorMessage = DeletedMessage, pendingMessage = null)
                },
                onFailure = { error -> state.copy(isSaving = false, errorMessage = error.message ?: DeleteFailedMessage, pendingMessage = null) }
            )
        }
    }

    private suspend fun ensureAccessToken(): String? {
        if (accessToken.isNotBlank()) return accessToken
        val account = withContext(Dispatchers.IO) { currentAccountStore.getCurrentAccount() } ?: return null
        val token = withContext(Dispatchers.IO) { tokenStore.getAccessToken(account.id) }?.takeIf { it.isNotBlank() } ?: return null
        accessToken = token
        return token
    }

    private companion object {
        const val NotSignedInMessage = "当前账号未登录或令牌已失效。"
        const val UnknownErrorMessage = "加载危险区设置时发生未知错误。"
        const val ReadOnlyMessage = "当前账号没有管理员权限，无法执行危险区操作。"
        const val UpdateFailedMessage = "更新归档状态失败。"
        const val ArchivingMessage = "正在归档仓库……"
        const val UnarchivingMessage = "正在取消归档……"
        const val ArchivedMessage = "仓库已归档。"
        const val UnarchivedMessage = "仓库已取消归档。"
        const val TransferringMessage = "正在发起仓库转移……"
        const val TransferredMessage = "仓库转移请求已提交。"
        const val TransferFailedMessage = "转移仓库失败。"
        const val TransferOwnerRequiredMessage = "请输入新的 Owner。"
        const val DeletingMessage = "正在删除仓库……"
        const val DeletedMessage = "仓库已删除。"
        const val DeleteFailedMessage = "删除仓库失败。"
        const val DeleteConfirmationMismatchMessage = "确认文本不匹配，请输入完整仓库名。"
    }
}
