package com.Sunset.REN.GitHub.ui.repo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.Sunset.REN.GitHub.data.github.GitHubRepositoryApiGateway
import com.Sunset.REN.GitHub.data.local.EncryptedSharedPreferencesTokenStore
import com.Sunset.REN.GitHub.data.local.SharedPreferencesCurrentAccountStore
import com.Sunset.REN.GitHub.domain.repo.RepositoryIssueComment
import com.Sunset.REN.GitHub.domain.repo.RepositoryIssueDetail
import com.Sunset.REN.GitHub.domain.repo.RepositoryLabel
import com.Sunset.REN.GitHub.domain.repo.RepositoryPermissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RepositoryIssueDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val currentAccountStore = SharedPreferencesCurrentAccountStore(application)
    private val tokenStore = EncryptedSharedPreferencesTokenStore(application)

    private val _detailState = MutableLiveData(RepositoryIssueDetailUiState())
    val detailState: LiveData<RepositoryIssueDetailUiState> = _detailState

    private var accessToken: String = ""
    private var currentUserLogin: String = ""
    private var hasPrepared = false

    fun prepare(owner: String, repo: String, number: Int) {
        if (hasPrepared) return
        hasPrepared = true
        _detailState.value = RepositoryIssueDetailUiState(owner = owner, repo = repo, number = number)
        load()
    }

    fun load() {
        val state = _detailState.value ?: return
        if (state.owner.isBlank() || state.repo.isBlank() || state.number <= 0) return
        viewModelScope.launch {
            _detailState.value = state.copy(
                isLoading = true,
                isLoadingMoreComments = false,
                errorMessage = null
            )
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) {
                _detailState.value = _detailState.value?.copy(
                    isLoading = false,
                    errorMessage = NotSignedInMessage
                )
                return@launch
            }
            val login = ensureCurrentUserLogin()
            // 内容、权限、标签一起到位后再首次渲染，避免写操作入口先显示后隐藏的闪烁。
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val gateway = GitHubRepositoryApiGateway(token)
                    val issueDeferred = async {
                        gateway.getRepositoryIssue(state.owner, state.repo, state.number)
                    }
                    val commentsDeferred = async {
                        gateway.listRepositoryIssueComments(
                            owner = state.owner,
                            repo = state.repo,
                            number = state.number,
                            page = 1,
                            perPage = PageSize
                        )
                    }
                    val permissionsDeferred = async {
                        runCatching {
                            gateway.getRepositoryPermissions(state.owner, state.repo)
                        }.getOrDefault(RepositoryPermissions())
                    }
                    val labelsDeferred = async {
                        runCatching {
                            gateway.listRepositoryLabels(state.owner, state.repo)
                        }.getOrDefault(emptyList())
                    }
                    IssueDetailBundle(
                        issue = issueDeferred.await(),
                        comments = commentsDeferred.await(),
                        permissions = permissionsDeferred.await(),
                        labels = labelsDeferred.await()
                    )
                }
            }
            _detailState.value = result.fold(
                onSuccess = { bundle ->
                    _detailState.value?.copy(
                        issue = bundle.issue,
                        comments = bundle.comments,
                        isLoading = false,
                        isLoadingMoreComments = false,
                        errorMessage = null,
                        hasMoreComments = bundle.comments.size >= PageSize,
                        loadedCommentPages = 1,
                        canPush = bundle.permissions.canPush,
                        currentUserLogin = login.orEmpty(),
                        availableLabels = bundle.labels
                    )
                },
                onFailure = { error ->
                    _detailState.value?.copy(
                        isLoading = false,
                        isLoadingMoreComments = false,
                        errorMessage = error.message?.takeIf { it.isNotBlank() } ?: UnknownErrorMessage
                    )
                }
            )
        }
    }

    fun loadMoreComments() {
        val state = _detailState.value ?: return
        if (state.isLoading || state.isLoadingMoreComments || !state.hasMoreComments) return
        if (state.owner.isBlank() || state.repo.isBlank() || state.number <= 0) return
        viewModelScope.launch {
            _detailState.value = state.copy(isLoadingMoreComments = true, errorMessage = null)
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) {
                _detailState.value = _detailState.value?.copy(
                    isLoadingMoreComments = false,
                    errorMessage = NotSignedInMessage
                )
                return@launch
            }
            val nextPage = state.loadedCommentPages + 1
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    GitHubRepositoryApiGateway(token).listRepositoryIssueComments(
                        owner = state.owner,
                        repo = state.repo,
                        number = state.number,
                        page = nextPage,
                        perPage = PageSize
                    )
                }
            }
            _detailState.value = result.fold(
                onSuccess = { comments ->
                    val current = _detailState.value ?: return@fold null
                    current.copy(
                        comments = current.comments + comments,
                        isLoadingMoreComments = false,
                        errorMessage = null,
                        hasMoreComments = comments.size >= PageSize,
                        loadedCommentPages = nextPage
                    )
                },
                onFailure = { error ->
                    _detailState.value?.copy(
                        isLoadingMoreComments = false,
                        errorMessage = error.message?.takeIf { it.isNotBlank() } ?: UnknownErrorMessage
                    )
                }
            )
        }
    }

    /** 切换开启/关闭状态。 */
    fun toggleIssueState() {
        val state = _detailState.value ?: return
        val issue = state.issue ?: return
        val targetState = if (issue.state == RepositoryIssuesUiState.ClosedState) {
            RepositoryIssuesUiState.OpenState
        } else {
            RepositoryIssuesUiState.ClosedState
        }
        runMutation(
            mutate = { gateway ->
                gateway.updateIssueState(state.owner, state.repo, state.number, targetState)
            },
            onSuccess = { updated, current ->
                current.copy(issue = updated, statusMessage = StateChangedMessage)
            }
        )
    }

    fun createComment(body: String) {
        val state = _detailState.value ?: return
        if (body.isBlank()) return
        runMutation(
            mutate = { gateway ->
                gateway.createIssueComment(state.owner, state.repo, state.number, body)
            },
            onSuccess = { newComment, current ->
                current.copy(
                    comments = current.comments + newComment,
                    statusMessage = CommentCreatedMessage
                )
            }
        )
    }

    fun updateComment(commentId: Long, body: String) {
        val state = _detailState.value ?: return
        if (body.isBlank()) return
        runMutation(
            mutate = { gateway ->
                gateway.updateIssueComment(state.owner, state.repo, commentId, body)
            },
            onSuccess = { updated, current ->
                current.copy(
                    comments = current.comments.map { if (it.id == commentId) updated else it },
                    statusMessage = CommentUpdatedMessage
                )
            }
        )
    }

    fun deleteComment(commentId: Long) {
        val state = _detailState.value ?: return
        runMutation(
            mutate = { gateway ->
                gateway.deleteIssueComment(state.owner, state.repo, commentId)
            },
            onSuccess = { _, current ->
                current.copy(
                    comments = current.comments.filterNot { it.id == commentId },
                    statusMessage = CommentDeletedMessage
                )
            }
        )
    }

    fun setLabels(labelNames: List<String>) {
        val state = _detailState.value ?: return
        runMutation(
            mutate = { gateway ->
                gateway.setIssueLabels(state.owner, state.repo, state.number, labelNames)
            },
            onSuccess = { updated, current ->
                current.copy(issue = updated, statusMessage = LabelsUpdatedMessage)
            }
        )
    }

    /** 消费一次性提示，避免旋转屏幕等重建时重复弹出。 */
    fun consumeStatusMessage() {
        _detailState.value = _detailState.value?.copy(statusMessage = null)
    }

    private fun <T> runMutation(
        mutate: suspend (GitHubRepositoryApiGateway) -> T,
        onSuccess: (T, RepositoryIssueDetailUiState) -> RepositoryIssueDetailUiState
    ) {
        val state = _detailState.value ?: return
        if (state.isMutating) return
        viewModelScope.launch {
            _detailState.value = state.copy(isMutating = true, statusMessage = null)
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) {
                _detailState.value = _detailState.value?.copy(
                    isMutating = false,
                    statusMessage = NotSignedInMessage
                )
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                runCatching { mutate(GitHubRepositoryApiGateway(token)) }
            }
            _detailState.value = result.fold(
                onSuccess = { value ->
                    val current = _detailState.value ?: return@fold null
                    onSuccess(value, current).copy(isMutating = false)
                },
                onFailure = { error ->
                    _detailState.value?.copy(
                        isMutating = false,
                        statusMessage = error.message?.takeIf { it.isNotBlank() } ?: MutationFailedMessage
                    )
                }
            )
        }
    }

    private suspend fun ensureAccessToken(): String? {
        if (accessToken.isNotBlank()) return accessToken
        val account = withContext(Dispatchers.IO) {
            currentAccountStore.getCurrentAccount()
        } ?: return null
        if (currentUserLogin.isBlank()) {
            currentUserLogin = account.login
        }
        val token = withContext(Dispatchers.IO) {
            tokenStore.getAccessToken(account.id)
        }?.takeIf { it.isNotBlank() } ?: return null
        accessToken = token
        return token
    }

    private suspend fun ensureCurrentUserLogin(): String? {
        if (currentUserLogin.isNotBlank()) return currentUserLogin
        val account = withContext(Dispatchers.IO) {
            currentAccountStore.getCurrentAccount()
        } ?: return null
        currentUserLogin = account.login
        return currentUserLogin
    }

    private data class IssueDetailBundle(
        val issue: RepositoryIssueDetail,
        val comments: List<RepositoryIssueComment>,
        val permissions: RepositoryPermissions,
        val labels: List<RepositoryLabel>
    )

    private companion object {
        const val PageSize = 20
        const val NotSignedInMessage = "当前账号未登录或令牌已失效。"
        const val UnknownErrorMessage = "加载问题详情时发生未知错误。"
        const val MutationFailedMessage = "操作失败，请稍后重试。"
        const val StateChangedMessage = "问题状态已更新。"
        const val CommentCreatedMessage = "评论已发表。"
        const val CommentUpdatedMessage = "评论已更新。"
        const val CommentDeletedMessage = "评论已删除。"
        const val LabelsUpdatedMessage = "标签已更新。"
    }
}