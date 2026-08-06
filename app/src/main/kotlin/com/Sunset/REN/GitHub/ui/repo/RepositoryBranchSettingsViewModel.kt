package com.Sunset.REN.GitHub.ui.repo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.Sunset.REN.GitHub.data.github.html.GitHubHtmlParseResult
import com.Sunset.REN.GitHub.data.github.html.GitHubRepositoryBranchSettingsGateway
import com.Sunset.REN.GitHub.data.github.html.RepositoryBranchProtectionSnapshot
import com.Sunset.REN.GitHub.data.github.html.RepositoryBranchProtectionUpdateRequest
import com.Sunset.REN.GitHub.data.github.html.RepositoryBranchSettingsItem
import com.Sunset.REN.GitHub.data.github.html.RepositoryBranchSettingsSnapshot
import com.Sunset.REN.GitHub.data.github.html.RepositoryRequiredPullRequestReviewsUpdate
import com.Sunset.REN.GitHub.data.github.html.RepositoryRequiredStatusChecksUpdate
import com.Sunset.REN.GitHub.data.local.EncryptedSharedPreferencesTokenStore
import com.Sunset.REN.GitHub.data.local.SharedPreferencesCurrentAccountStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RepositoryBranchSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val currentAccountStore = SharedPreferencesCurrentAccountStore(application)
    private val tokenStore = EncryptedSharedPreferencesTokenStore(application)

    private val _branchSettingsState = MutableLiveData(RepositoryBranchSettingsUiState())
    val branchSettingsState: LiveData<RepositoryBranchSettingsUiState> = _branchSettingsState

    private var accessToken: String = ""
    private var hasPrepared = false

    fun prepare(owner: String, repo: String) {
        if (hasPrepared) return
        hasPrepared = true
        _branchSettingsState.value = RepositoryBranchSettingsUiState(owner = owner, repo = repo)
        refresh()
    }

    fun refresh() {
        val state = _branchSettingsState.value ?: return
        if (state.owner.isBlank() || state.repo.isBlank()) return
        viewModelScope.launch {
            _branchSettingsState.value = state.copy(
                isLoading = true,
                isLoadingProtection = false,
                isSaving = false,
                errorMessage = null,
                pendingMessage = null,
                sourceUrl = state.sourceUrl.takeIf { state.snapshot != null },
                isShowingStaleContent = state.snapshot != null
            )
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) {
                _branchSettingsState.value = _branchSettingsState.value?.copy(
                    isLoading = false,
                    isShowingStaleContent = false,
                    errorMessage = NotSignedInMessage
                )
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                runCatching { GitHubRepositoryBranchSettingsGateway(token).loadBranchSettings(state.owner, state.repo) }
            }
            _branchSettingsState.value = result.fold(
                onSuccess = { parseResult -> parseResult.toUiState(isLoading = false) },
                onFailure = { error ->
                    _branchSettingsState.value?.copy(
                        isLoading = false,
                        isShowingStaleContent = false,
                        errorMessage = error.message?.takeIf { it.isNotBlank() } ?: UnknownErrorMessage
                    )
                }
            )
        }
    }

    fun loadProtection(branch: String) {
        val state = _branchSettingsState.value ?: return
        if (state.owner.isBlank() || state.repo.isBlank() || branch.isBlank()) return
        viewModelScope.launch {
            _branchSettingsState.value = state.copy(
                selectedBranch = branch,
                selectedProtection = state.snapshot?.branches?.firstOrNull { it.name == branch }?.protection,
                isLoadingProtection = true,
                errorMessage = null,
                pendingMessage = null
            )
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) {
                _branchSettingsState.value = _branchSettingsState.value?.copy(
                    isLoadingProtection = false,
                    errorMessage = NotSignedInMessage
                )
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                runCatching { GitHubRepositoryBranchSettingsGateway(token).loadBranchProtection(state.owner, state.repo, branch) }
            }
            _branchSettingsState.value = result.fold(
                onSuccess = { parseResult -> parseResult.toProtectionUiState(branch) },
                onFailure = { error ->
                    _branchSettingsState.value?.copy(
                        isLoadingProtection = false,
                        errorMessage = error.message?.takeIf { it.isNotBlank() } ?: UnknownProtectionErrorMessage
                    )
                }
            )
        }
    }

    fun enablePullRequestProtection(branch: String, requiredApprovingReviewCount: Int = 1) {
        val request = RepositoryBranchProtectionUpdateRequest(
            requiredStatusChecks = null,
            enforceAdmins = false,
            requiredPullRequestReviews = RepositoryRequiredPullRequestReviewsUpdate(
                dismissStaleReviews = true,
                requireCodeOwnerReviews = false,
                requiredApprovingReviewCount = requiredApprovingReviewCount.coerceIn(1, 6)
            ),
            restrictions = null
        )
        updateProtection(branch, request)
    }

    fun enableStatusCheckProtection(branch: String, strict: Boolean, contexts: List<String>) {
        val request = RepositoryBranchProtectionUpdateRequest(
            requiredStatusChecks = RepositoryRequiredStatusChecksUpdate(
                strict = strict,
                contexts = contexts.map { it.trim() }.filter { it.isNotBlank() }.distinct()
            ),
            enforceAdmins = false,
            requiredPullRequestReviews = null,
            restrictions = null
        )
        updateProtection(branch, request)
    }

    fun updateProtection(branch: String, request: RepositoryBranchProtectionUpdateRequest) {
        val state = _branchSettingsState.value ?: return
        if (state.owner.isBlank() || state.repo.isBlank() || branch.isBlank() || state.isSaving) return
        val snapshot = state.snapshot ?: return
        if (!snapshot.canAdmin) {
            _branchSettingsState.value = state.copy(errorMessage = ReadOnlyMessage)
            return
        }
        viewModelScope.launch {
            _branchSettingsState.value = state.copy(
                selectedBranch = branch,
                isSaving = true,
                errorMessage = null,
                pendingMessage = UpdatingProtectionMessage
            )
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) {
                _branchSettingsState.value = state.copy(isSaving = false, errorMessage = NotSignedInMessage, pendingMessage = null)
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                runCatching { GitHubRepositoryBranchSettingsGateway(token).updateBranchProtection(state.owner, state.repo, branch, request) }
            }
            _branchSettingsState.value = result.fold(
                onSuccess = { parseResult -> parseResult.toProtectionSavedUiState(branch) },
                onFailure = { error ->
                    _branchSettingsState.value?.copy(
                        isSaving = false,
                        errorMessage = error.message?.takeIf { it.isNotBlank() } ?: UpdateProtectionErrorMessage,
                        pendingMessage = null
                    )
                }
            )
        }
    }

    fun deleteProtection(branch: String) {
        val state = _branchSettingsState.value ?: return
        if (state.owner.isBlank() || state.repo.isBlank() || branch.isBlank() || state.isSaving) return
        val snapshot = state.snapshot ?: return
        if (!snapshot.canAdmin) {
            _branchSettingsState.value = state.copy(errorMessage = ReadOnlyMessage)
            return
        }
        viewModelScope.launch {
            _branchSettingsState.value = state.copy(
                selectedBranch = branch,
                isSaving = true,
                errorMessage = null,
                pendingMessage = DeletingProtectionMessage
            )
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) {
                _branchSettingsState.value = state.copy(isSaving = false, errorMessage = NotSignedInMessage, pendingMessage = null)
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                runCatching { GitHubRepositoryBranchSettingsGateway(token).deleteBranchProtection(state.owner, state.repo, branch) }
            }
            _branchSettingsState.value = result.fold(
                onSuccess = { parseResult -> parseResult.toProtectionDeletedUiState(branch) },
                onFailure = { error ->
                    _branchSettingsState.value?.copy(
                        isSaving = false,
                        errorMessage = error.message?.takeIf { it.isNotBlank() } ?: DeleteProtectionErrorMessage,
                        pendingMessage = null
                    )
                }
            )
        }
    }

    private fun GitHubHtmlParseResult<RepositoryBranchSettingsSnapshot>.toUiState(isLoading: Boolean): RepositoryBranchSettingsUiState? {
        val current = _branchSettingsState.value ?: return null
        return when (this) {
            is GitHubHtmlParseResult.Success -> current.copy(
                isLoading = isLoading,
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

    private fun GitHubHtmlParseResult<RepositoryBranchProtectionSnapshot>.toProtectionUiState(branch: String): RepositoryBranchSettingsUiState? {
        val current = _branchSettingsState.value ?: return null
        return when (this) {
            is GitHubHtmlParseResult.Success -> current.copy(
                selectedBranch = branch,
                selectedProtection = value,
                snapshot = current.snapshot?.withBranchProtection(branch, value),
                isLoadingProtection = false,
                errorMessage = null,
                sourceUrl = current.sourceUrl
            )
            is GitHubHtmlParseResult.AccessDenied -> current.copy(isLoadingProtection = false, errorMessage = message, sourceUrl = sourceUrl)
            is GitHubHtmlParseResult.NotFound -> current.copy(isLoadingProtection = false, errorMessage = message, sourceUrl = sourceUrl)
            is GitHubHtmlParseResult.ParseError -> current.copy(isLoadingProtection = false, errorMessage = message, sourceUrl = sourceUrl)
        }
    }

    private fun GitHubHtmlParseResult<RepositoryBranchProtectionSnapshot>.toProtectionSavedUiState(branch: String): RepositoryBranchSettingsUiState? {
        val current = _branchSettingsState.value ?: return null
        return when (this) {
            is GitHubHtmlParseResult.Success -> current.copy(
                selectedBranch = branch,
                selectedProtection = value,
                snapshot = current.snapshot?.withBranchProtection(branch, value),
                isSaving = false,
                errorMessage = null,
                pendingMessage = ProtectionUpdatedMessage
            )
            is GitHubHtmlParseResult.AccessDenied -> current.copy(isSaving = false, errorMessage = message, pendingMessage = null, sourceUrl = sourceUrl)
            is GitHubHtmlParseResult.NotFound -> current.copy(isSaving = false, errorMessage = message, pendingMessage = null, sourceUrl = sourceUrl)
            is GitHubHtmlParseResult.ParseError -> current.copy(isSaving = false, errorMessage = message, pendingMessage = null, sourceUrl = sourceUrl)
        }
    }

    private fun GitHubHtmlParseResult<Unit>.toProtectionDeletedUiState(branch: String): RepositoryBranchSettingsUiState? {
        val current = _branchSettingsState.value ?: return null
        return when (this) {
            is GitHubHtmlParseResult.Success -> current.copy(
                selectedBranch = branch,
                selectedProtection = null,
                snapshot = current.snapshot?.withoutBranchProtection(branch),
                isSaving = false,
                errorMessage = null,
                pendingMessage = ProtectionDeletedMessage
            )
            is GitHubHtmlParseResult.AccessDenied -> current.copy(isSaving = false, errorMessage = message, pendingMessage = null, sourceUrl = sourceUrl)
            is GitHubHtmlParseResult.NotFound -> current.copy(isSaving = false, errorMessage = message, pendingMessage = null, sourceUrl = sourceUrl)
            is GitHubHtmlParseResult.ParseError -> current.copy(isSaving = false, errorMessage = message, pendingMessage = null, sourceUrl = sourceUrl)
        }
    }

    private fun RepositoryBranchSettingsSnapshot.withBranchProtection(
        branch: String,
        protection: RepositoryBranchProtectionSnapshot
    ): RepositoryBranchSettingsSnapshot {
        return copy(branches = branches.map { item ->
            if (item.name == branch) item.copy(protected = true, protection = protection) else item
        })
    }

    private fun RepositoryBranchSettingsSnapshot.withoutBranchProtection(branch: String): RepositoryBranchSettingsSnapshot {
        return copy(branches = branches.map { item ->
            if (item.name == branch) item.copy(protected = false, protection = null) else item
        })
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
        const val UnknownErrorMessage = "加载仓库分支设置时发生未知错误。"
        const val UnknownProtectionErrorMessage = "加载分支保护规则时发生未知错误。"
        const val UpdateProtectionErrorMessage = "更新分支保护规则时发生未知错误。"
        const val DeleteProtectionErrorMessage = "删除分支保护规则时发生未知错误。"
        const val ReadOnlyMessage = "当前账号没有管理员权限，无法修改分支保护规则。"
        const val UpdatingProtectionMessage = "正在更新分支保护规则……"
        const val DeletingProtectionMessage = "正在删除分支保护规则……"
        const val ProtectionUpdatedMessage = "分支保护规则已更新。"
        const val ProtectionDeletedMessage = "分支保护规则已删除。"
    }
}
