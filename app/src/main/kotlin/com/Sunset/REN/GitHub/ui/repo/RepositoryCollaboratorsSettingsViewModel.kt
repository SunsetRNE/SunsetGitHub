package com.Sunset.REN.GitHub.ui.repo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.Sunset.REN.GitHub.data.github.html.GitHubHtmlParseResult
import com.Sunset.REN.GitHub.data.github.html.GitHubRepositoryCollaboratorsGateway
import com.Sunset.REN.GitHub.data.github.html.RepositoryCollaboratorMutationResult
import com.Sunset.REN.GitHub.data.github.html.RepositoryCollaboratorPermission
import com.Sunset.REN.GitHub.data.github.html.RepositoryCollaboratorsSnapshot
import com.Sunset.REN.GitHub.data.local.EncryptedSharedPreferencesTokenStore
import com.Sunset.REN.GitHub.data.local.SharedPreferencesCurrentAccountStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RepositoryCollaboratorsSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val currentAccountStore = SharedPreferencesCurrentAccountStore(application)
    private val tokenStore = EncryptedSharedPreferencesTokenStore(application)
    private val _collaboratorsState = MutableLiveData(RepositoryCollaboratorsSettingsUiState())
    val collaboratorsState: LiveData<RepositoryCollaboratorsSettingsUiState> = _collaboratorsState
    private var accessToken: String = ""
    private var hasPrepared = false

    fun prepare(owner: String, repo: String) {
        if (hasPrepared) return
        hasPrepared = true
        _collaboratorsState.value = RepositoryCollaboratorsSettingsUiState(owner = owner, repo = repo)
        refresh()
    }

    fun refresh() {
        val state = _collaboratorsState.value ?: return
        if (state.owner.isBlank() || state.repo.isBlank()) return
        viewModelScope.launch {
            _collaboratorsState.value = state.copy(
                isLoading = true,
                isSaving = false,
                errorMessage = null,
                pendingMessage = null,
                sourceUrl = state.sourceUrl.takeIf { state.snapshot != null },
                isShowingStaleContent = state.snapshot != null
            )
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) { _collaboratorsState.value = _collaboratorsState.value?.copy(isLoading = false, isShowingStaleContent = false, errorMessage = NotSignedInMessage); return@launch }
            val result = withContext(Dispatchers.IO) { runCatching { GitHubRepositoryCollaboratorsGateway(token).loadCollaborators(state.owner, state.repo) } }
            _collaboratorsState.value = result.fold(
                onSuccess = { it.toUiState() },
                onFailure = { error -> _collaboratorsState.value?.copy(isLoading = false, isShowingStaleContent = false, errorMessage = error.message?.takeIf { message -> message.isNotBlank() } ?: UnknownErrorMessage) }
            )
        }
    }

    fun selectCollaborator(login: String) { _collaboratorsState.value = _collaboratorsState.value?.copy(selectedLogin = login, errorMessage = null, pendingMessage = null) }

    fun updateSelectedPermission(permission: RepositoryCollaboratorPermission) {
        val state = _collaboratorsState.value ?: return
        val login = state.selectedLogin.orEmpty()
        if (login.isBlank()) return
        inviteCollaborator(login, permission)
    }

    fun inviteCollaborator(username: String, permission: RepositoryCollaboratorPermission) {
        val state = _collaboratorsState.value ?: return
        val normalized = username.trim().trimStart('@')
        if (normalized.isBlank() || state.owner.isBlank() || state.repo.isBlank() || state.isSaving) return
        if (state.snapshot?.canAdmin != true) { _collaboratorsState.value = state.copy(errorMessage = ReadOnlyMessage); return }
        viewModelScope.launch {
            _collaboratorsState.value = state.copy(isSaving = true, errorMessage = null, pendingMessage = InvitingMessage)
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) { _collaboratorsState.value = state.copy(isSaving = false, errorMessage = NotSignedInMessage, pendingMessage = null); return@launch }
            val result = withContext(Dispatchers.IO) { runCatching { GitHubRepositoryCollaboratorsGateway(token).inviteCollaborator(state.owner, state.repo, normalized, permission) } }
            _collaboratorsState.value = result.fold(
                onSuccess = { parseResult -> parseResult.toInviteUiState() },
                onFailure = { error -> _collaboratorsState.value?.copy(isSaving = false, errorMessage = error.message?.takeIf { it.isNotBlank() } ?: InviteFailedMessage, pendingMessage = null) }
            )
        }
    }

    fun removeSelectedCollaborator() { _collaboratorsState.value?.selectedLogin?.let { removeCollaborator(it) } }

    fun removeCollaborator(login: String) {
        val state = _collaboratorsState.value ?: return
        if (login.isBlank() || state.owner.isBlank() || state.repo.isBlank() || state.isSaving) return
        if (state.snapshot?.canAdmin != true) { _collaboratorsState.value = state.copy(errorMessage = ReadOnlyMessage); return }
        viewModelScope.launch {
            _collaboratorsState.value = state.copy(isSaving = true, selectedLogin = login, errorMessage = null, pendingMessage = RemovingMessage)
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) { _collaboratorsState.value = state.copy(isSaving = false, errorMessage = NotSignedInMessage, pendingMessage = null); return@launch }
            val result = withContext(Dispatchers.IO) { runCatching { GitHubRepositoryCollaboratorsGateway(token).removeCollaborator(state.owner, state.repo, login) } }
            _collaboratorsState.value = result.fold(
                onSuccess = { it.toRemoveUiState(login) },
                onFailure = { error -> _collaboratorsState.value?.copy(isSaving = false, errorMessage = error.message?.takeIf { it.isNotBlank() } ?: RemoveFailedMessage, pendingMessage = null) }
            )
        }
    }

    fun cancelInvitation(invitationId: Long) {
        val state = _collaboratorsState.value ?: return
        if (invitationId <= 0L || state.owner.isBlank() || state.repo.isBlank() || state.isSaving) return
        if (state.snapshot?.canAdmin != true) { _collaboratorsState.value = state.copy(errorMessage = ReadOnlyMessage); return }
        viewModelScope.launch {
            _collaboratorsState.value = state.copy(isSaving = true, errorMessage = null, pendingMessage = CancellingInvitationMessage)
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) { _collaboratorsState.value = state.copy(isSaving = false, errorMessage = NotSignedInMessage, pendingMessage = null); return@launch }
            val result = withContext(Dispatchers.IO) { runCatching { GitHubRepositoryCollaboratorsGateway(token).cancelInvitation(state.owner, state.repo, invitationId) } }
            _collaboratorsState.value = result.fold(
                onSuccess = { parseResult -> parseResult.toCancelInvitationUiState(invitationId) },
                onFailure = { error -> _collaboratorsState.value?.copy(isSaving = false, errorMessage = error.message?.takeIf { it.isNotBlank() } ?: CancelInvitationFailedMessage, pendingMessage = null) }
            )
        }
    }

    private fun GitHubHtmlParseResult<RepositoryCollaboratorsSnapshot>.toUiState(): RepositoryCollaboratorsSettingsUiState? {
        val current = _collaboratorsState.value ?: return null
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

    private fun GitHubHtmlParseResult<RepositoryCollaboratorMutationResult>.toInviteUiState(): RepositoryCollaboratorsSettingsUiState? {
        val current = _collaboratorsState.value ?: return null
        return when (this) {
            is GitHubHtmlParseResult.Success -> current.copy(isSaving = false, errorMessage = null, snapshot = current.snapshot?.copy(collaborators = current.snapshot.collaborators.map { if (it.login == value.username) it.copy(permission = value.permission) else it }), pendingMessage = if (value.invitationCreated) InvitationCreatedMessage else CollaboratorUpdatedMessage)
            is GitHubHtmlParseResult.AccessDenied -> current.copy(isSaving = false, errorMessage = message, pendingMessage = null, sourceUrl = sourceUrl)
            is GitHubHtmlParseResult.NotFound -> current.copy(isSaving = false, errorMessage = message, pendingMessage = null, sourceUrl = sourceUrl)
            is GitHubHtmlParseResult.ParseError -> current.copy(isSaving = false, errorMessage = message, pendingMessage = null, sourceUrl = sourceUrl)
        }
    }

    private fun GitHubHtmlParseResult<Unit>.toRemoveUiState(login: String): RepositoryCollaboratorsSettingsUiState? {
        val current = _collaboratorsState.value ?: return null
        return when (this) {
            is GitHubHtmlParseResult.Success -> current.copy(isSaving = false, selectedLogin = null, snapshot = current.snapshot?.copy(collaborators = current.snapshot.collaborators.filterNot { it.login == login }), errorMessage = null, pendingMessage = RemovedMessage)
            is GitHubHtmlParseResult.AccessDenied -> current.copy(isSaving = false, errorMessage = message, pendingMessage = null, sourceUrl = sourceUrl)
            is GitHubHtmlParseResult.NotFound -> current.copy(isSaving = false, errorMessage = message, pendingMessage = null, sourceUrl = sourceUrl)
            is GitHubHtmlParseResult.ParseError -> current.copy(isSaving = false, errorMessage = message, pendingMessage = null, sourceUrl = sourceUrl)
        }
    }

    private fun GitHubHtmlParseResult<Unit>.toCancelInvitationUiState(invitationId: Long): RepositoryCollaboratorsSettingsUiState? {
        val current = _collaboratorsState.value ?: return null
        return when (this) {
            is GitHubHtmlParseResult.Success -> current.copy(isSaving = false, snapshot = current.snapshot?.copy(invitations = current.snapshot.invitations.filterNot { it.id == invitationId }), errorMessage = null, pendingMessage = InvitationCancelledMessage)
            is GitHubHtmlParseResult.AccessDenied -> current.copy(isSaving = false, errorMessage = message, pendingMessage = null, sourceUrl = sourceUrl)
            is GitHubHtmlParseResult.NotFound -> current.copy(isSaving = false, errorMessage = message, pendingMessage = null, sourceUrl = sourceUrl)
            is GitHubHtmlParseResult.ParseError -> current.copy(isSaving = false, errorMessage = message, pendingMessage = null, sourceUrl = sourceUrl)
        }
    }

    private fun refreshAfterMutationIfNeeded() { if (_collaboratorsState.value?.errorMessage == null) refresh() }
    private suspend fun ensureAccessToken(): String? { if (accessToken.isNotBlank()) return accessToken; val account = withContext(Dispatchers.IO) { currentAccountStore.getCurrentAccount() } ?: return null; val token = withContext(Dispatchers.IO) { tokenStore.getAccessToken(account.id) }?.takeIf { it.isNotBlank() } ?: return null; accessToken = token; return token }

    private companion object {
        const val NotSignedInMessage = "当前账号未登录或令牌已失效。"
        const val UnknownErrorMessage = "加载协作者与访问权限时发生未知错误。"
        const val ReadOnlyMessage = "当前账号没有管理员权限，无法修改协作者。"
        const val InvitingMessage = "正在添加协作者……"
        const val RemovingMessage = "正在移除协作者……"
        const val CancellingInvitationMessage = "正在取消协作者邀请……"
        const val InvitationCreatedMessage = "协作者邀请已创建。"
        const val CollaboratorUpdatedMessage = "协作者权限已更新。"
        const val RemovedMessage = "协作者已移除。"
        const val InvitationCancelledMessage = "协作者邀请已取消。"
        const val InviteFailedMessage = "添加协作者时发生未知错误。"
        const val RemoveFailedMessage = "移除协作者时发生未知错误。"
        const val CancelInvitationFailedMessage = "取消协作者邀请时发生未知错误。"
    }
}