package com.Sunset.REN.GitHub.ui.repo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.Sunset.REN.GitHub.data.github.html.GitHubHtmlParseResult
import com.Sunset.REN.GitHub.data.github.html.GitHubRepositoryProjectsGateway
import com.Sunset.REN.GitHub.data.local.EncryptedSharedPreferencesTokenStore
import com.Sunset.REN.GitHub.data.local.SharedPreferencesCurrentAccountStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RepositoryProjectsViewModel(application: Application) : AndroidViewModel(application) {

    private val currentAccountStore = SharedPreferencesCurrentAccountStore(application)
    private val tokenStore = EncryptedSharedPreferencesTokenStore(application)

    private val _projectsState = MutableLiveData(RepositoryProjectsUiState())
    val projectsState: LiveData<RepositoryProjectsUiState> = _projectsState

    private var accessToken: String = ""
    private var hasPrepared = false

    fun prepare(owner: String, repo: String) {
        if (hasPrepared) return
        hasPrepared = true
        _projectsState.value = RepositoryProjectsUiState(owner = owner, repo = repo)
        refresh()
    }

    fun refresh() {
        val state = _projectsState.value ?: return
        if (state.owner.isBlank() || state.repo.isBlank()) return
        viewModelScope.launch {
            _projectsState.value = state.copy(
                isLoading = true,
                errorMessage = null,
                pendingMessage = null,
                sourceUrl = state.sourceUrl.takeIf { state.summary != null },
                isShowingStaleContent = state.summary != null
            )
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) {
                _projectsState.value = _projectsState.value?.copy(
                    isLoading = false,
                    isShowingStaleContent = false,
                    errorMessage = NotSignedInMessage
                )
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                runCatching { GitHubRepositoryProjectsGateway(token).loadProjectsSummary(state.owner, state.repo) }
            }
            _projectsState.value = result.fold(
                onSuccess = { parseResult ->
                    when (parseResult) {
                        is GitHubHtmlParseResult.Success -> _projectsState.value?.copy(
                            isLoading = false,
                            summary = parseResult.value,
                            isShowingStaleContent = false,
                            errorMessage = null,
                            sourceUrl = parseResult.value.sourceUrl
                        )
                        is GitHubHtmlParseResult.AccessDenied -> _projectsState.value?.copy(
                            isLoading = false,
                            isShowingStaleContent = false,
                            errorMessage = parseResult.message,
                            sourceUrl = parseResult.sourceUrl
                        )
                        is GitHubHtmlParseResult.NotFound -> _projectsState.value?.copy(
                            isLoading = false,
                            isShowingStaleContent = false,
                            errorMessage = parseResult.message,
                            sourceUrl = parseResult.sourceUrl
                        )
                        is GitHubHtmlParseResult.ParseError -> _projectsState.value?.copy(
                            isLoading = false,
                            isShowingStaleContent = false,
                            errorMessage = parseResult.message,
                            sourceUrl = parseResult.sourceUrl
                        )
                    }
                },
                onFailure = { error ->
                    _projectsState.value?.copy(
                        isLoading = false,
                        isShowingStaleContent = false,
                        errorMessage = error.message?.takeIf { it.isNotBlank() } ?: UnknownErrorMessage
                    )
                }
            )
        }
    }

    fun setProjectsEnabled(enabled: Boolean) {
        val state = _projectsState.value ?: return
        if (state.owner.isBlank() || state.repo.isBlank() || state.isSaving) return
        viewModelScope.launch {
            _projectsState.value = state.copy(isSaving = true, errorMessage = null, pendingMessage = if (enabled) EnablingMessage else DisablingMessage)
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) {
                _projectsState.value = state.copy(isSaving = false, errorMessage = NotSignedInMessage, pendingMessage = null)
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                runCatching { GitHubRepositoryProjectsGateway(token).updateProjectsEnabled(state.owner, state.repo, enabled) }
            }
            _projectsState.value = result.fold(
                onSuccess = { parseResult ->
                    when (parseResult) {
                        is GitHubHtmlParseResult.Success -> _projectsState.value?.copy(
                            isSaving = false,
                            summary = parseResult.value,
                            errorMessage = null,
                            pendingMessage = if (enabled) EnabledMessage else DisabledMessage,
                            sourceUrl = parseResult.value.sourceUrl
                        )
                        is GitHubHtmlParseResult.AccessDenied -> _projectsState.value?.copy(isSaving = false, errorMessage = parseResult.message, pendingMessage = null, sourceUrl = parseResult.sourceUrl)
                        is GitHubHtmlParseResult.NotFound -> _projectsState.value?.copy(isSaving = false, errorMessage = parseResult.message, pendingMessage = null, sourceUrl = parseResult.sourceUrl)
                        is GitHubHtmlParseResult.ParseError -> _projectsState.value?.copy(isSaving = false, errorMessage = parseResult.message, pendingMessage = null, sourceUrl = parseResult.sourceUrl)
                    }
                },
                onFailure = { error ->
                    _projectsState.value?.copy(
                        isSaving = false,
                        errorMessage = error.message?.takeIf { it.isNotBlank() } ?: UnknownErrorMessage,
                        pendingMessage = null
                    )
                }
            )
        }
    }

    fun setProjectStateFilter(filter: RepositoryProjectsFilter) {
        val state = _projectsState.value ?: return
        if (state.projectStateFilter == filter) return
        _projectsState.value = state.copy(projectStateFilter = filter)
    }

    fun setSearchQuery(query: String) {
        val state = _projectsState.value ?: return
        val normalizedQuery = query.trim()
        if (state.searchQuery == normalizedQuery) return
        _projectsState.value = state.copy(searchQuery = normalizedQuery)
    }

    fun toggleSortOrder() {
        val state = _projectsState.value ?: return
        _projectsState.value = state.copy(sortNewestFirst = !state.sortNewestFirst)
    }

    fun setWorkspaceMode(mode: RepositoryProjectsWorkspaceMode) {
        val state = _projectsState.value ?: return
        if (state.workspaceMode == mode) return
        _projectsState.value = state.copy(workspaceMode = mode)
    }

    fun selectProject(title: String) {
        val state = _projectsState.value ?: return
        if (state.selectedProjectTitle == title) return
        _projectsState.value = state.copy(selectedProjectTitle = title, selectedItemTitle = null)
    }

    fun selectItem(title: String) {
        val state = _projectsState.value ?: return
        if (state.selectedItemTitle == title) return
        _projectsState.value = state.copy(selectedItemTitle = title)
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
        const val UnknownErrorMessage = "加载仓库 Projects 页面时发生未知错误。"
        const val EnablingMessage = "正在启用 Projects..."
        const val DisablingMessage = "正在关闭 Projects..."
        const val EnabledMessage = "Projects 已启用。"
        const val DisabledMessage = "Projects 已关闭。"
    }
}