package com.Sunset.REN.GitHub.ui.repo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.Sunset.REN.GitHub.data.github.GitHubRepositoryApiGateway
import com.Sunset.REN.GitHub.data.local.EncryptedSharedPreferencesTokenStore
import com.Sunset.REN.GitHub.data.local.SharedPreferencesCurrentAccountStore
import com.Sunset.REN.GitHub.domain.repo.RepositoryIssue
import com.Sunset.REN.GitHub.domain.repo.RepositoryPermissions
import com.Sunset.REN.GitHub.domain.repo.RepositoryLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RepositoryIssuesViewModel(application: Application) : AndroidViewModel(application) {

    private val currentAccountStore = SharedPreferencesCurrentAccountStore(application)
    private val tokenStore = EncryptedSharedPreferencesTokenStore(application)

    private val _issuesState = MutableLiveData(RepositoryIssuesUiState())
    val issuesState: LiveData<RepositoryIssuesUiState> = _issuesState

    private var accessToken: String = ""
    private var hasPrepared = false
    private var permissionsResolved = false
    private var cachedCanPush = false

    fun prepare(owner: String, repo: String) {
        if (hasPrepared) return
        hasPrepared = true
        _issuesState.value = RepositoryIssuesUiState(owner = owner, repo = repo, isLoading = true)
        loadFirstPage()
        loadAvailableLabels()
    }

    /** REST 的状态/作者/标签筛选都会重置分页，避免把不同查询条件的页数据拼在一起。 */
    fun switchCreator(creator: String?) {
        val current = _issuesState.value ?: return
        val normalizedCreator = creator?.takeIf { it.isNotBlank() }
        if (current.selectedCreator == normalizedCreator) return
        _issuesState.value = current.copy(
            selectedCreator = normalizedCreator,
            isLoading = true,
            isLoadingMore = false,
            errorMessage = null,
            isShowingStaleContent = current.issues.isNotEmpty()
        )
        loadFirstPage()
    }

    fun switchLabels(labels: List<String>) {
        val current = _issuesState.value ?: return
        val normalizedLabels = labels.mapNotNull { it.takeIf(String::isNotBlank) }.distinct()
        if (current.selectedLabels == normalizedLabels) return
        _issuesState.value = current.copy(
            selectedLabels = normalizedLabels,
            isLoading = true,
            isLoadingMore = false,
            errorMessage = null,
            isShowingStaleContent = current.issues.isNotEmpty()
        )
        loadFirstPage()
    }

    fun loadAvailableLabels() {
        val state = _issuesState.value ?: return
        if (state.owner.isBlank() || state.repo.isBlank() || state.isLoadingLabels) return
        viewModelScope.launch {
            _issuesState.value = state.copy(isLoadingLabels = true, labelErrorMessage = null)
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) {
                _issuesState.value = _issuesState.value?.copy(
                    isLoadingLabels = false,
                    labelErrorMessage = NotSignedInMessage
                )
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                runCatching { GitHubRepositoryApiGateway(token).listRepositoryLabels(state.owner, state.repo) }
            }
            _issuesState.value = result.fold(
                onSuccess = { labels ->
                    _issuesState.value?.copy(
                        availableLabels = labels,
                        isLoadingLabels = false,
                        labelErrorMessage = null
                    )
                },
                onFailure = { error ->
                    _issuesState.value?.copy(
                        isLoadingLabels = false,
                        labelErrorMessage = error.message?.takeIf { it.isNotBlank() } ?: UnknownErrorMessage
                    )
                }
            )
        }
    }

    /** 切换 open/closed 状态：重置列表与分页后重新拉首页。 */
    fun switchState(state: String) {
        val current = _issuesState.value ?: return
        if (current.state == state) return
        _issuesState.value = current.copy(
            state = state,
            isLoading = true,
            isLoadingMore = false,
            errorMessage = null,
            isShowingStaleContent = current.issues.isNotEmpty()
        )
        loadFirstPage()
    }

    fun loadFirstPage() {
        val state = _issuesState.value ?: return
        if (state.owner.isBlank() || state.repo.isBlank()) return
        viewModelScope.launch {
            _issuesState.value = state.copy(
                isLoading = true,
                isLoadingMore = false,
                errorMessage = null
            )
            val token = ensureAccessToken()
            val currentUserLogin = withContext(Dispatchers.IO) {
                currentAccountStore.getCurrentAccount()?.login
            }
            if (token.isNullOrBlank()) {
                _issuesState.value = _issuesState.value?.copy(
                    isLoading = false,
                    errorMessage = NotSignedInMessage
                )
                return@launch
            }
            // 首页内容与写权限一起到位后再渲染，避免「新建问题」入口先显示后隐藏的闪烁。
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val gateway = GitHubRepositoryApiGateway(token)
                    val issuesDeferred = async {
                        gateway.listRepositoryIssues(
                            owner = state.owner,
                            repo = state.repo,
                            state = state.state,
                            page = 1,
                            perPage = PageSize,
                            creator = state.selectedCreator,
                            labels = state.selectedLabels
                        )
                    }
                    val permissionsDeferred = async {
                        if (permissionsResolved) {
                            RepositoryPermissions(canPush = cachedCanPush)
                        } else {
                            runCatching {
                                gateway.getRepositoryPermissions(state.owner, state.repo)
                            }.getOrDefault(RepositoryPermissions())
                        }
                    }
                    IssuesFirstPageBundle(
                        issues = issuesDeferred.await(),
                        permissions = permissionsDeferred.await(),
                        currentUserLogin = currentUserLogin
                    )
                }
            }
            _issuesState.value = result.fold(
                onSuccess = { bundle ->
                    permissionsResolved = true
                    cachedCanPush = bundle.permissions.canPush
                    _issuesState.value?.copy(
                        issues = bundle.issues.withoutPullRequests(),
                        isLoading = false,
                        isLoadingMore = false,
                        errorMessage = null,
                        hasMore = bundle.issues.size >= PageSize,
                        loadedPages = 1,
                        isShowingStaleContent = false,
                        canPush = bundle.permissions.canPush,
                        currentUserLogin = bundle.currentUserLogin
                    )
                },
                onFailure = { error ->
                    _issuesState.value?.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        errorMessage = error.message?.takeIf { it.isNotBlank() } ?: UnknownErrorMessage
                    )
                }
            )
        }
    }

    fun loadNextPage() {
        val state = _issuesState.value ?: return
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return
        if (state.owner.isBlank() || state.repo.isBlank()) return
        viewModelScope.launch {
            _issuesState.value = state.copy(isLoadingMore = true, errorMessage = null)
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) {
                _issuesState.value = _issuesState.value?.copy(
                    isLoadingMore = false,
                    errorMessage = NotSignedInMessage
                )
                return@launch
            }
            val nextPage = state.loadedPages + 1
            // 翻页必须沿用当前筛选条件，保持与首页同一个 REST 查询窗口。
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    GitHubRepositoryApiGateway(token).listRepositoryIssues(
                        owner = state.owner,
                        repo = state.repo,
                        state = state.state,
                        page = nextPage,
                        perPage = PageSize,
                        creator = state.selectedCreator,
                        labels = state.selectedLabels
                    )
                }
            }
            _issuesState.value = result.fold(
                onSuccess = { rawIssues ->
                    val current = _issuesState.value ?: return@fold null
                    current.copy(
                        issues = current.issues + rawIssues.withoutPullRequests(),
                        isLoadingMore = false,
                        errorMessage = null,
                        hasMore = rawIssues.size >= PageSize,
                        loadedPages = nextPage
                    )
                },
                onFailure = { error ->
                    _issuesState.value?.copy(
                        isLoadingMore = false,
                        errorMessage = error.message?.takeIf { it.isNotBlank() } ?: UnknownErrorMessage
                    )
                }
            )
        }
    }

    private fun List<RepositoryIssue>.withoutPullRequests(): List<RepositoryIssue> {
        return filterNot { it.isPullRequest }
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

    private data class IssuesFirstPageBundle(
        val issues: List<RepositoryIssue>,
        val permissions: RepositoryPermissions,
        val currentUserLogin: String?
    )

    private companion object {
        const val PageSize = 20
        const val NotSignedInMessage = "当前账号未登录或令牌已失效。"
        const val UnknownErrorMessage = "加载问题列表时发生未知错误。"
    }
}