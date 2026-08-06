package com.Sunset.REN.GitHub.ui.repo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.Sunset.REN.GitHub.data.github.GitHubRepositoryApiGateway
import com.Sunset.REN.GitHub.data.github.GitHubRepositoryFeatureUnavailableException
import com.Sunset.REN.GitHub.data.local.EncryptedSharedPreferencesTokenStore
import com.Sunset.REN.GitHub.data.local.SharedPreferencesCurrentAccountStore
import com.Sunset.REN.GitHub.domain.repo.RepositoryActionRun
import com.Sunset.REN.GitHub.domain.repo.RepositoryActionWorkflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RepositoryActionsViewModel(application: Application) : AndroidViewModel(application) {

    private val currentAccountStore = SharedPreferencesCurrentAccountStore(application)
    private val tokenStore = EncryptedSharedPreferencesTokenStore(application)

    private val _actionsState = MutableLiveData(RepositoryActionsUiState())
    val actionsState: LiveData<RepositoryActionsUiState> = _actionsState

    private val _openRunDetailEvent = MutableLiveData<RepositoryActionRun?>()
    val openRunDetailEvent: LiveData<RepositoryActionRun?> = _openRunDetailEvent

    private val _workflowDispatchMetadataEvent = MutableLiveData<RepositoryActionWorkflow?>()
    val workflowDispatchMetadataEvent: LiveData<RepositoryActionWorkflow?> = _workflowDispatchMetadataEvent

    private val firstPageCache = mutableMapOf<String, RepositoryActionsUiState>()

    private val loadingWorkflowMetadataIds = mutableSetOf<Long>()

    private var accessToken: String = ""
    private var hasPrepared = false

    fun prepare(owner: String, repo: String) {
        if (hasPrepared) return
        hasPrepared = true
        _actionsState.value = RepositoryActionsUiState(owner = owner, repo = repo)
        reload()
    }

    fun reload() {
        loadFirstPage(includeWorkflows = true)
    }

    fun switchStatus(status: String?) {
        val current = _actionsState.value ?: return
        if (current.status == status) return
        _actionsState.value = current.copy(
            status = status,
            isLoading = true,
            isLoadingMore = false,
            errorMessage = null,
            unavailableMessage = null,
            isShowingStaleContent = current.workflowRuns.isNotEmpty()
        )
        loadFirstPage(includeWorkflows = false)
    }

    fun switchWorkflow(workflowId: Long?) {
        val current = _actionsState.value ?: return
        if (current.selectedWorkflowId == workflowId) return
        _actionsState.value = current.copy(
            selectedWorkflowId = workflowId,
            isLoading = true,
            isLoadingMore = false,
            errorMessage = null,
            unavailableMessage = null,
            isShowingStaleContent = current.workflowRuns.isNotEmpty()
        )
        loadFirstPage(includeWorkflows = false)
    }

    fun consumeOpenRunDetailEvent() {
        _openRunDetailEvent.value = null
    }

    fun consumeWorkflowDispatchMetadataEvent() {
        _workflowDispatchMetadataEvent.value = null
    }

    fun loadWorkflowDispatchMetadata(workflow: RepositoryActionWorkflow) {
        val state = _actionsState.value ?: return
        if (workflow.hasLoadedDispatchMetadata || workflow.id in loadingWorkflowMetadataIds) {
            if (workflow.hasLoadedDispatchMetadata) _workflowDispatchMetadataEvent.value = workflow
            return
        }
        loadingWorkflowMetadataIds.add(workflow.id)
        viewModelScope.launch {
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) {
                loadingWorkflowMetadataIds.remove(workflow.id)
                _workflowDispatchMetadataEvent.value = workflow.copy(
                    isDispatchable = false,
                    hasLoadedDispatchMetadata = true
                )
                _actionsState.value = _actionsState.value?.copy(errorMessage = NotSignedInMessage)
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    GitHubRepositoryApiGateway(token).getRepositoryActionWorkflowDispatchMetadata(
                        owner = state.owner,
                        repo = state.repo,
                        workflow = workflow
                    )
                }
            }
            loadingWorkflowMetadataIds.remove(workflow.id)
            val metadataWorkflow = result.getOrElse {
                workflow.copy(isDispatchable = false, hasLoadedDispatchMetadata = true)
            }
            _actionsState.value = (_actionsState.value ?: state).copy(
                workflows = (_actionsState.value ?: state).workflows.map {
                    if (it.id == metadataWorkflow.id) metadataWorkflow else it
                },
                errorMessage = result.exceptionOrNull()?.message?.takeIf { it.isNotBlank() }
            )
            _workflowDispatchMetadataEvent.value = metadataWorkflow
        }
    }

    fun dispatchWorkflow(workflow: RepositoryActionWorkflow, ref: String, inputs: Map<String, String> = emptyMap()) {
        val state = _actionsState.value ?: return
        if (!workflow.isDispatchable || state.owner.isBlank() || state.repo.isBlank()) return
        viewModelScope.launch {
            _actionsState.value = state.copy(
                selectedWorkflowId = workflow.id,
                dispatchingWorkflowId = workflow.id,
                errorMessage = null,
                unavailableMessage = null
            )
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) {
                _actionsState.value = _actionsState.value?.copy(
                    dispatchingWorkflowId = null,
                    errorMessage = NotSignedInMessage
                )
                return@launch
            }
            val gateway = GitHubRepositoryApiGateway(token)
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val previousLatestRunId = gateway.listRepositoryActionRuns(
                        owner = state.owner,
                        repo = state.repo,
                        page = 1,
                        perPage = 1,
                        status = null,
                        workflowId = workflow.id
                    ).firstOrNull()?.id
                    gateway.dispatchRepositoryWorkflow(
                        owner = state.owner,
                        repo = state.repo,
                        workflowIdOrFileName = workflow.id.toString(),
                        ref = ref,
                        inputs = inputs
                    )
                    previousLatestRunId to waitForDispatchedWorkflowRun(
                        gateway = gateway,
                        owner = state.owner,
                        repo = state.repo,
                        workflowId = workflow.id,
                        previousLatestRunId = previousLatestRunId
                    )
                }
            }
            _actionsState.value = result.fold(
                onSuccess = { refreshResult ->
                    val previousLatestRunId = refreshResult.first
                    val refreshedRuns = refreshResult.second
                    val current = _actionsState.value ?: state
                    val nextRun = refreshedRuns.firstOrNull { it.id != previousLatestRunId } ?: refreshedRuns.firstOrNull()
                    nextRun?.let { _openRunDetailEvent.value = it }
                    current.copy(
                        selectedWorkflowId = workflow.id,
                        workflowRuns = refreshedRuns,
                        dispatchingWorkflowId = null,
                        isLoading = false,
                        isLoadingMore = false,
                        errorMessage = DispatchSuccessMessage,
                        unavailableMessage = null,
                        hasMoreRuns = refreshedRuns.size >= PageSize,
                        loadedRunPages = 1
                    )
                },
                onFailure = { error ->
                    (_actionsState.value ?: state).copy(
                        selectedWorkflowId = workflow.id,
                        dispatchingWorkflowId = null,
                        errorMessage = error.message?.takeIf { it.isNotBlank() } ?: DispatchFailedMessage
                    )
                }
            )
        }
    }

    fun loadNextPage() {
        val state = _actionsState.value ?: return
        if (state.isLoading || state.isLoadingMore || !state.hasMoreRuns) return
        if (state.owner.isBlank() || state.repo.isBlank()) return
        viewModelScope.launch {
            _actionsState.value = state.copy(isLoadingMore = true, errorMessage = null, unavailableMessage = null)
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) {
                _actionsState.value = _actionsState.value?.copy(
                    isLoadingMore = false,
                    errorMessage = NotSignedInMessage,
                    unavailableMessage = null
                )
                return@launch
            }
            val nextPage = state.loadedRunPages + 1
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    GitHubRepositoryApiGateway(token).listRepositoryActionRuns(
                        owner = state.owner,
                        repo = state.repo,
                        page = nextPage,
                        perPage = PageSize,
                        status = state.status,
                        workflowId = state.selectedWorkflowId
                    )
                }
            }
            _actionsState.value = result.fold(
                onSuccess = { actionRuns ->
                    val current = _actionsState.value ?: return@fold null
                    current.copy(
                        workflowRuns = current.workflowRuns + actionRuns,
                        isLoadingMore = false,
                        errorMessage = null,
                        unavailableMessage = null,
                        hasMoreRuns = actionRuns.size >= PageSize,
                        loadedRunPages = nextPage
                    )
                },
                onFailure = { error ->
                    when (error) {
                        is GitHubRepositoryFeatureUnavailableException -> _actionsState.value?.copy(
                            workflowRuns = emptyList(),
                            isLoadingMore = false,
                            errorMessage = null,
                            unavailableMessage = error.message,
                            hasMoreRuns = false
                        )
                        else -> _actionsState.value?.copy(
                            isLoadingMore = false,
                            errorMessage = error.message?.takeIf { it.isNotBlank() } ?: UnknownErrorMessage,
                            unavailableMessage = null
                        )
                    }
                }
            )
        }
    }

    private fun loadFirstPage(includeWorkflows: Boolean) {
        val state = _actionsState.value ?: return
        if (state.owner.isBlank() || state.repo.isBlank()) return
        val cacheKey = state.firstPageCacheKey()
        val cachedState = firstPageCache[cacheKey]
        if (cachedState != null && cachedState.workflowRuns.isNotEmpty()) {
            _actionsState.value = cachedState.copy(
                workflows = if (includeWorkflows) cachedState.workflows else state.workflows,
                branches = if (includeWorkflows) cachedState.branches else state.branches,
                defaultBranch = if (includeWorkflows) cachedState.defaultBranch else state.defaultBranch,
                isLoading = true,
                isLoadingMore = false,
                errorMessage = null,
                unavailableMessage = null,
                dispatchingWorkflowId = null
            )
        }
        val requestState = _actionsState.value ?: state
        viewModelScope.launch {
            _actionsState.value = requestState.copy(
                isLoading = true,
                isLoadingMore = false,
                errorMessage = null,
                unavailableMessage = null,
                dispatchingWorkflowId = null
            )
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) {
                _actionsState.value = _actionsState.value?.copy(
                    isLoading = false,
                    errorMessage = NotSignedInMessage
                )
                return@launch
            }
            val gateway = GitHubRepositoryApiGateway(token)
            val repositoryDeferred = if (includeWorkflows) {
                async(Dispatchers.IO) { runCatching { gateway.getRepository(requestState.owner, requestState.repo, includeLanguages = false) } }
            } else {
                null
            }
            val workflowsDeferred = async(Dispatchers.IO) {
                if (includeWorkflows) runCatching { gateway.listRepositoryActionWorkflows(requestState.owner, requestState.repo) } else Result.success(requestState.workflows)
            }
            val branchesDeferred = async(Dispatchers.IO) {
                if (includeWorkflows) runCatching { gateway.listRepositoryBranches(requestState.owner, requestState.repo) } else Result.success(requestState.branches)
            }
            val runsDeferred = async(Dispatchers.IO) {
                runCatching {
                    gateway.listRepositoryActionRuns(
                        owner = requestState.owner,
                        repo = requestState.repo,
                        page = 1,
                        perPage = PageSize,
                        status = requestState.status,
                        workflowId = requestState.selectedWorkflowId
                    )
                }
            }
            val runsResult = runsDeferred.await()
            val runsUnavailable = runsResult.exceptionOrNull() as? GitHubRepositoryFeatureUnavailableException
            if (runsUnavailable != null) {
                _actionsState.value = (_actionsState.value ?: requestState).copy(
                    workflows = emptyList(),
                    workflowRuns = emptyList(),
                    isLoading = false,
                    isLoadingMore = false,
                    errorMessage = null,
                    unavailableMessage = runsUnavailable.message,
                    hasMoreRuns = false,
                    loadedRunPages = 0
                )
                return@launch
            }
            val firstPageRuns = runsResult.getOrDefault(emptyList())
            _actionsState.value = (_actionsState.value ?: requestState).copy(
                workflowRuns = firstPageRuns,
                headBranch = firstPageRuns.firstOrNull()?.headBranch.orEmpty(),
                isLoading = false,
                isLoadingMore = false,
                errorMessage = runsResult.exceptionOrNull()?.message?.takeIf { it.isNotBlank() },
                unavailableMessage = null,
                hasMoreRuns = firstPageRuns.size >= PageSize,
                loadedRunPages = 1,
                isShowingStaleContent = false
            )

            val repositoryResult = repositoryDeferred?.await() ?: Result.success(null)
            val workflowsResult = workflowsDeferred.await()
            val branchesResult = branchesDeferred.await()
            val metadataUnavailable = listOf(repositoryResult.exceptionOrNull(), workflowsResult.exceptionOrNull())
                .filterIsInstance<GitHubRepositoryFeatureUnavailableException>()
                .firstOrNull()
            if (metadataUnavailable != null && firstPageRuns.isEmpty()) {
                _actionsState.value = (_actionsState.value ?: requestState).copy(
                    workflows = emptyList(),
                    workflowRuns = emptyList(),
                    isLoading = false,
                    isLoadingMore = false,
                    errorMessage = null,
                    unavailableMessage = metadataUnavailable.message,
                    hasMoreRuns = false,
                    loadedRunPages = 0
                )
                return@launch
            }
            _actionsState.value = (_actionsState.value ?: requestState).copy(
                workflows = workflowsResult.getOrDefault(requestState.workflows),
                workflowRuns = firstPageRuns,
                branches = branchesResult.getOrDefault(requestState.branches),
                defaultBranch = repositoryResult.getOrNull()?.defaultBranch?.takeIf { it.isNotBlank() } ?: requestState.defaultBranch,
                headBranch = firstPageRuns.firstOrNull()?.headBranch.orEmpty(),
                isLoading = false,
                isLoadingMore = false,
                errorMessage = workflowsResult.exceptionOrNull()?.message?.takeIf { it.isNotBlank() }
                    ?: repositoryResult.exceptionOrNull()?.message?.takeIf { it.isNotBlank() }
                    ?: runsResult.exceptionOrNull()?.message?.takeIf { it.isNotBlank() },
                unavailableMessage = null,
                hasMoreRuns = firstPageRuns.size >= PageSize,
                loadedRunPages = 1,
                isShowingStaleContent = false
            ).also { refreshedState ->
                firstPageCache[cacheKey] = refreshedState
            }
        }
    }

    private fun RepositoryActionsUiState.firstPageCacheKey(): String {
        val workflowPart = selectedWorkflowId?.toString() ?: "all"
        return "${owner.lowercase()}/${repo.lowercase()}:${status.orEmpty()}:$workflowPart"
    }

    private suspend fun waitForDispatchedWorkflowRun(
        gateway: GitHubRepositoryApiGateway,
        owner: String,
        repo: String,
        workflowId: Long,
        previousLatestRunId: Long?
    ): List<RepositoryActionRun> {
        var latestRuns = emptyList<RepositoryActionRun>()
        repeat(DispatchRunRefreshAttempts) { attempt ->
            if (attempt > 0) delay(DispatchRunRefreshDelayMillis)
            latestRuns = gateway.listRepositoryActionRuns(
                owner = owner,
                repo = repo,
                page = 1,
                perPage = PageSize,
                status = null,
                workflowId = workflowId
            )
            if (latestRuns.firstOrNull()?.id != previousLatestRunId) return latestRuns
        }
        return latestRuns
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
        const val UnknownErrorMessage = "加载 Actions 页面时发生未知错误。"
        const val DispatchFailedMessage = "触发 workflow 失败。"
        const val DispatchSuccessMessage = "Workflow 已触发。"
        const val DispatchRunRefreshAttempts = 6
        const val DispatchRunRefreshDelayMillis = 1_000L
    }
}