package com.Sunset.REN.GitHub.ui.repo

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.data.github.GitHubRepositoryApiGateway
import com.Sunset.REN.GitHub.data.local.EncryptedSharedPreferencesTokenStore
import com.Sunset.REN.GitHub.data.local.RepositoryCacheRepository
import com.Sunset.REN.GitHub.data.local.RepositoryDetailCacheSnapshot
import com.Sunset.REN.GitHub.data.local.RepositoryNavigationPreferencesRepository
import com.Sunset.REN.GitHub.data.local.SharedPreferencesCurrentAccountStore
import com.Sunset.REN.GitHub.domain.repo.GitHubRepository
import com.Sunset.REN.GitHub.domain.repo.RepositoryBranch
import com.Sunset.REN.GitHub.domain.repo.RepositoryContentItem
import com.Sunset.REN.GitHub.domain.repo.RepositoryDetailLoadUseCase
import com.Sunset.REN.GitHub.domain.repo.RepositoryDetailSecondaryResult
import com.Sunset.REN.GitHub.domain.repo.RepositoryFilePreview
import com.Sunset.REN.GitHub.domain.repo.RepositoryPullRequest
import com.Sunset.REN.GitHub.domain.repo.RepositorySidebarInfo
import com.Sunset.REN.GitHub.util.AppLogger
import com.Sunset.REN.GitHub.util.PerformanceTrace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RepositoryDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val currentAccountStore = SharedPreferencesCurrentAccountStore(application)
    private val tokenStore = EncryptedSharedPreferencesTokenStore(application)
    private val repositoryNavigationPreferencesRepository = RepositoryNavigationPreferencesRepository(application)
    private val repositoryCacheRepository = RepositoryCacheRepository(application)
    private val repositoryDetailLoadUseCase = RepositoryDetailLoadUseCase()

    private val _repositoryState = MutableLiveData<RepositoryDetailUiState>(RepositoryDetailUiState.Loading)
    val repositoryState: LiveData<RepositoryDetailUiState> = _repositoryState
    fun shortcutSections(repositoryFullName: String): List<RepositorySection> {
        return repositoryNavigationPreferencesRepository.getRepositoryShortcutSections(repositoryFullName)
    }

    fun sectionOrder(repositoryFullName: String): List<RepositorySection> {
        return repositoryNavigationPreferencesRepository.getRepositorySectionOrder(repositoryFullName)
    }

    /** 把分区钉进当前仓库的中间快捷区，返回更新后的快捷区列表。 */
    fun pinShortcutSection(repositoryFullName: String, section: RepositorySection): List<RepositorySection> {
        return repositoryNavigationPreferencesRepository.pinShortcutSection(repositoryFullName, section)
    }

    /** 把分区从当前仓库的中间快捷区取消钉选，返回更新后的快捷区列表。 */
    fun unpinShortcutSection(repositoryFullName: String, section: RepositorySection): List<RepositorySection> {
        return repositoryNavigationPreferencesRepository.unpinShortcutSection(repositoryFullName, section)
    }

    fun setRepositorySectionOrder(repositoryFullName: String, sections: List<RepositorySection>): List<RepositorySection> {
        repositoryNavigationPreferencesRepository.setRepositorySectionOrder(repositoryFullName, sections)
        return repositoryNavigationPreferencesRepository.getRepositorySectionOrder(repositoryFullName)
    }

    fun moveRepositorySection(repositoryFullName: String, section: RepositorySection, delta: Int): List<RepositorySection> {
        return repositoryNavigationPreferencesRepository.moveRepositorySection(repositoryFullName, section, delta)
    }

    fun canPinShortcutSection(repositoryFullName: String): Boolean {
        return repositoryNavigationPreferencesRepository.canPinShortcutSection(repositoryFullName)
    }

    fun canUnpinShortcutSection(repositoryFullName: String): Boolean {
        return repositoryNavigationPreferencesRepository.canUnpinShortcutSection(repositoryFullName)
    }

    private var loadedOwner: String = ""
    private var loadedRepo: String = ""
    private var accessToken: String = ""
    private var currentAccountLogin: String = ""
    private var currentContentsPath: String = ""
    private var selectedBranch: String = ""
    private var cachedBranches: List<RepositoryBranch> = emptyList()
    private var branchesLoadKey: String = ""
    private var lastPreviewPath: String = ""
    private var lastPreviewName: String = ""
    private var cachedFilePreview: RepositoryFilePreview? = null
    private var cachedFilePreviewKey: String = ""
    private var activeFilePreviewKey: String = ""
    private var repositoryLoadGeneration = 0
    private var secondaryLoadKey: String = ""
    private var forkSyncLoadKey: String = ""

    fun loadRepository(
        owner: String,
        repo: String,
        autoPreviewReadme: Boolean = true,
        forceRefresh: Boolean = false
    ) {
        viewModelScope.launch {
            var loadGeneration = repositoryLoadGeneration
            try {
                val account = withContext(Dispatchers.IO) {
                    currentAccountStore.getCurrentAccount()
                }
                if (account == null) {
                    _repositoryState.value = RepositoryDetailUiState.SignedOut
                    return@launch
                }
                val token = withContext(Dispatchers.IO) {
                    tokenStore.getAccessToken(account.id)
                }
                if (token.isNullOrBlank()) {
                    _repositoryState.value = RepositoryDetailUiState.SignedOut
                    return@launch
                }
                val isSameRepository = owner == loadedOwner && repo == loadedRepo && accessToken.isNotBlank()
                val currentContent = _repositoryState.value as? RepositoryDetailUiState.Content
                if (isSameRepository && !forceRefresh && currentContent != null) {
                    if (currentContent.sidebarInfo != null && currentContent.isStarred != null && currentContent.isWatching != null) {
                        return@launch
                    }
                    loadRepositorySecondaryDetail(
                        owner = owner,
                        repo = repo,
                        token = token,
                        generation = loadGeneration,
                        baseRepository = currentContent.repository,
                        contents = currentContent.contents,
                        contentsError = currentContent.contentsError
                    )
                    return@launch
                }
                if (!isSameRepository || forceRefresh) {
                    loadGeneration = ++repositoryLoadGeneration
                    currentContentsPath = RootPath
                    selectedBranch = ""
                    cachedBranches = emptyList()
                    branchesLoadKey = ""
                    lastPreviewPath = ""
                    lastPreviewName = ""
                    cachedFilePreview = null
                    cachedFilePreviewKey = ""
                    activeFilePreviewKey = ""
                    secondaryLoadKey = ""
                    forkSyncLoadKey = ""
                }
                loadedOwner = owner
                loadedRepo = repo
                accessToken = token
                currentAccountLogin = account.login
                repositoryCacheRepository.getDetail(owner, repo)?.takeIf { !forceRefresh }?.let { cached ->
                    _repositoryState.value = RepositoryDetailUiState.Content(
                        repository = cached.repository,
                        currentPath = cached.currentPath,
                        contents = cached.contents,
                        contentsError = cached.contentsError,
                        isContentsLoading = cached.contents.isEmpty() && cached.contentsError.isNullOrBlank(),
                        isStarred = cached.isStarred,
                        isWatching = cached.isWatching,
                        lightManagementStateError = cached.lightManagementStateError,
                        sidebarInfo = cached.sidebarInfo,
                        isRefreshingFromCache = true,
                        canPush = cached.canPush,
                        currentAccountLogin = currentAccountLogin
                    )
                    if (autoPreviewReadme) {
                        findDefaultReadme(cached.contents, cached.contentsError)?.let { readme ->
                            loadFilePreview(readme.path, readme.name)
                        }
                    }
                }
                if (!forceRefresh && currentContentState() == null) {
                    val persistedSnapshot = withContext(Dispatchers.IO) {
                        repositoryCacheRepository.getPersistedDetail(owner, repo)
                    }
                    if (persistedSnapshot != null) {
                        _repositoryState.value = RepositoryDetailUiState.Content(
                            repository = persistedSnapshot.repository,
                            currentPath = RootPath,
                            contents = persistedSnapshot.contents,
                            contentsError = null,
                            isContentsLoading = persistedSnapshot.contents.isEmpty(),
                            isStarred = persistedSnapshot.isStarred,
                            isWatching = persistedSnapshot.isWatching,
                            sidebarInfo = persistedSnapshot.sidebarInfo,
                            isRefreshingFromCache = true,
                            currentAccountLogin = currentAccountLogin
                        )
                        if (autoPreviewReadme) {
                            findDefaultReadme(persistedSnapshot.contents, contentsError = null)?.let { readme ->
                                loadFilePreview(readme.path, readme.name)
                            }
                        }
                    } else {
                        val preloadRepository = withContext(Dispatchers.IO) {
                            repositoryCacheRepository.findRepository(account.id, owner, repo)
                        }
                        _repositoryState.value = RepositoryDetailUiState.Content(
                            repository = preloadRepository ?: createRepositoryTemplate(owner, repo),
                            currentPath = RootPath,
                            contents = emptyList(),
                            contentsError = null,
                            isContentsLoading = true,
                            sidebarInfo = null,
                            isRefreshingFromCache = preloadRepository != null,
                            currentAccountLogin = currentAccountLogin
                        )
                    }
                }
                loadRepositoryDetailInPhases(
                    owner = owner,
                    repo = repo,
                    token = token,
                    generation = loadGeneration,
                    autoPreviewReadme = autoPreviewReadme,
                    forceRefresh = forceRefresh
                )
            } catch (exception: Exception) {
                if (loadGeneration != repositoryLoadGeneration) return@launch
                val currentContent = currentContentState()
                if (currentContent != null) {
                    _repositoryState.value = currentContent.copy(
                        contentsError = currentContent.contentsError
                            ?: exception.message
                            ?: getApplication<Application>().getString(R.string.repository_detail_load_failed_fallback),
                        isContentsLoading = false,
                        isRefreshingFromCache = false
                    )
                } else {
                    _repositoryState.value = RepositoryDetailUiState.Error(
                        exception.message ?: getApplication<Application>().getString(R.string.repository_detail_load_failed_fallback)
                    )
                }
            }
        }
    }

    fun prepareFilePreview(owner: String, repo: String, path: String, name: String) {
        if (owner.isBlank() || repo.isBlank() || path.isBlank()) return
        viewModelScope.launch {
            try {
                val account = withContext(Dispatchers.IO) {
                    currentAccountStore.getCurrentAccount()
                }
                if (account == null) {
                    _repositoryState.value = RepositoryDetailUiState.SignedOut
                    return@launch
                }
                val token = withContext(Dispatchers.IO) {
                    tokenStore.getAccessToken(account.id)
                }
                if (token.isNullOrBlank()) {
                    _repositoryState.value = RepositoryDetailUiState.SignedOut
                    return@launch
                }
                loadedOwner = owner
                loadedRepo = repo
                accessToken = token
                currentAccountLogin = account.login
                currentContentsPath = RootPath
                if (lastPreviewPath != path) {
                    cachedFilePreview = null
                    cachedFilePreviewKey = ""
                    activeFilePreviewKey = ""
                }
                lastPreviewPath = path
                lastPreviewName = name
                val cached = repositoryCacheRepository.getDetail(owner, repo)
                if (cached != null) {
                    _repositoryState.value = RepositoryDetailUiState.Content(
                        repository = cached.repository,
                        currentPath = cached.currentPath,
                        contents = cached.contents,
                        contentsError = cached.contentsError,
                        isFilePreviewLoading = true,
                        isStarred = cached.isStarred,
                        isWatching = cached.isWatching,
                        lightManagementStateError = cached.lightManagementStateError,
                        sidebarInfo = cached.sidebarInfo,
                        currentAccountLogin = currentAccountLogin
                    )
                } else {
                    val repository = withContext(Dispatchers.IO) {
                        GitHubRepositoryApiGateway(token).getRepository(owner, repo)
                    }
                    _repositoryState.value = RepositoryDetailUiState.Content(
                        repository = repository,
                        currentPath = RootPath,
                        contents = emptyList(),
                        contentsError = null,
                        isFilePreviewLoading = true,
                        sidebarInfo = null,
                        currentAccountLogin = currentAccountLogin
                    )
                }
                loadFilePreview(path, name)
            } catch (exception: Exception) {
                _repositoryState.value = RepositoryDetailUiState.Error(
                    exception.message ?: getApplication<Application>().getString(R.string.repository_detail_load_failed_fallback)
                )
            }
        }
    }

    fun openDirectory(path: String) {
        if (path.isBlank()) return
        loadContents(path)
    }

    fun selectBranch(branch: String) {
        val normalizedBranch = branch.trim()
        if (normalizedBranch.isBlank() || accessToken.isBlank() || loadedOwner.isBlank() || loadedRepo.isBlank()) return
        if (normalizedBranch == currentBranchName()) return
        selectedBranch = normalizedBranch
        currentContentsPath = RootPath
        cachedFilePreview = null
        cachedFilePreviewKey = ""
        activeFilePreviewKey = ""
        lastPreviewPath = ""
        lastPreviewName = ""
        loadContents(RootPath, forceRefresh = true)
    }

    fun refreshBranches() {
        loadBranches(forceRefresh = true)
    }

    fun openParentDirectory() {
        val parentPath = currentContentsPath.substringBeforeLast(delimiter = "/", missingDelimiterValue = RootPath)
        loadContents(parentPath)
    }

    fun retryCurrentContents() {
        loadContents(currentContentsPath, forceRefresh = true)
    }

    fun refreshParentDirectoryForFile(path: String) {
        invalidateFilePreview(path)
        val parentPath = path.toParentDirectoryPath()
        if (parentPath == currentContentsPath.trim('/')) {
            loadContents(currentContentsPath, forceRefresh = true)
        } else {
            repositoryCacheRepository.removeContents(loadedOwner, loadedRepo, parentPath)
        }
    }

    fun retryLastFilePreview() {
        if (lastPreviewPath.isBlank()) return
        loadFilePreview(lastPreviewPath, lastPreviewName, forceRefresh = true)
    }

    fun openFile(path: String, name: String) {
        if (path.isBlank()) return
        loadFilePreview(path, name)
    }

    fun refreshFilePreview(path: String, name: String) {
        if (path.isBlank()) return
        loadFilePreview(path, name, forceRefresh = true)
    }

    fun canOpenFilePreview(): Boolean {
        return accessToken.isNotBlank() && loadedOwner.isNotBlank() && loadedRepo.isNotBlank()
    }

    fun getImageAccessToken(): String = accessToken

    fun toggleStarRepository() {
        when (currentContentState()?.isStarred) {
            true -> unstarRepository()
            false -> starRepository()
            null -> Unit
        }
    }

    fun toggleWatchRepository() {
        when (currentContentState()?.isWatching) {
            true -> unwatchRepository()
            false -> watchRepository()
            null -> Unit
        }
    }

    fun createFork(
        targetOwner: String? = null,
        targetName: String? = null,
        defaultBranchOnly: Boolean = false
    ) {
        if (accessToken.isBlank() || loadedOwner.isBlank() || loadedRepo.isBlank()) return
        val current = currentContentState() ?: return
        if (!current.canCreateFork(currentAccountLogin)) return
        viewModelScope.launch {
            _repositoryState.value = currentContentState()?.copy(
                lightManagementMessageResId = R.string.repository_detail_fork_loading,
                isLightManagementLoading = true,
                lightManagementStateError = null
            ) ?: return@launch
            val owner = loadedOwner
            val repo = loadedRepo
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    GitHubRepositoryApiGateway(accessToken).createFork(
                        owner = owner,
                        repo = repo,
                        targetOwner = targetOwner,
                        targetName = targetName,
                        defaultBranchOnly = defaultBranchOnly
                    )
                }
            }
            val latestState = currentContentState() ?: return@launch
            _repositoryState.value = if (result.isSuccess) {
                latestState.copy(
                    forkedRepository = result.getOrThrow(),
                    currentAccountFork = result.getOrThrow(),
                    lightManagementMessageResId = R.string.repository_detail_fork_success,
                    isLightManagementLoading = false,
                    lightManagementStateError = null
                )
            } else {
                latestState.copy(
                    lightManagementMessageResId = null,
                    isLightManagementLoading = false,
                    lightManagementStateError = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun consumeForkedRepository(): GitHubRepository? {
        val state = currentContentState() ?: return null
        val forkedRepository = state.forkedRepository ?: return null
        _repositoryState.value = state.copy(forkedRepository = null)
        return forkedRepository
    }

    fun syncForkWithUpstream() {
        if (accessToken.isBlank() || loadedOwner.isBlank() || loadedRepo.isBlank()) return
        val state = currentContentState() ?: return
        if (!state.repository.fork) return
        val owner = loadedOwner
        val repo = loadedRepo
        viewModelScope.launch {
            _repositoryState.value = currentContentState()?.copy(
                isForkSyncLoading = true,
                forkSyncMessageResId = R.string.repository_fork_sync_loading,
                forkSyncError = null
            ) ?: return@launch
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    GitHubRepositoryApiGateway(accessToken).syncFork(
                        owner = owner,
                        repo = repo,
                        branch = state.repository.defaultBranch
                    )
                }
            }
            val latestState = currentContentState() ?: return@launch
            _repositoryState.value = if (result.isSuccess) {
                val updatedState = latestState.copy(
                    forkSyncStatus = result.getOrThrow(),
                    forkSyncError = null,
                    isForkSyncLoading = false,
                    forkSyncMessageResId = R.string.repository_fork_sync_success
                )
                loadRepository(owner, repo, autoPreviewReadme = false, forceRefresh = true)
                updatedState
            } else {
                latestState.copy(
                    forkSyncError = result.exceptionOrNull()?.message,
                    isForkSyncLoading = false,
                    forkSyncMessageResId = null
                )
            }
        }
    }

    fun createContributionPullRequest(title: String, body: String) {
        if (accessToken.isBlank()) return
        val state = currentContentState() ?: return
        val repository = state.repository
        if (!repository.fork) return
        val parentFullName = repository.parentFullName ?: return
        val parentOwner = parentFullName.substringBefore('/').takeIf { it.isNotBlank() } ?: return
        val parentRepo = parentFullName.substringAfter('/').takeIf { it.isNotBlank() && it != parentFullName } ?: return
        val baseBranch = state.forkSyncStatus?.upstreamBranch
            ?: repository.parentDefaultBranch
            ?: repository.defaultBranch
        val headBranch = state.forkSyncStatus?.forkBranch ?: repository.defaultBranch
        val head = "${repository.ownerLogin}:$headBranch"
        val pullRequestTitle = title.ifBlank {
            getApplication<Application>().getString(
                R.string.repository_fork_contribute_default_title,
                repository.ownerLogin,
                headBranch
            )
        }
        viewModelScope.launch {
            _repositoryState.value = currentContentState()?.copy(
                isContributeLoading = true,
                contributeMessageResId = R.string.repository_fork_contribute_creating,
                contributeError = null,
                createdPullRequest = null
            ) ?: return@launch
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    GitHubRepositoryApiGateway(accessToken).createPullRequest(
                        owner = parentOwner,
                        repo = parentRepo,
                        title = pullRequestTitle,
                        head = head,
                        base = baseBranch,
                        body = body
                    )
                }
            }
            val latestState = currentContentState() ?: return@launch
            _repositoryState.value = if (result.isSuccess) {
                latestState.copy(
                    createdPullRequest = result.getOrThrow(),
                    contributeError = null,
                    isContributeLoading = false,
                    contributeMessageResId = R.string.repository_fork_contribute_created
                )
            } else {
                latestState.copy(
                    contributeError = result.exceptionOrNull()?.message,
                    isContributeLoading = false,
                    contributeMessageResId = null
                )
            }
        }
    }

    fun consumeCreatedPullRequest(): RepositoryPullRequest? {
        val state = currentContentState() ?: return null
        val pullRequest = state.createdPullRequest ?: return null
        _repositoryState.value = state.copy(createdPullRequest = null)
        return pullRequest
    }

    fun starRepository() {
        runLightManagementAction(
            loadingMessageResId = R.string.repository_detail_star_loading,
            successMessageResId = R.string.repository_detail_star_success,
            starredAfterSuccess = true
        ) { gateway ->
            gateway.star(loadedOwner, loadedRepo)
        }
    }

    fun unstarRepository() {
        runLightManagementAction(
            loadingMessageResId = R.string.repository_detail_unstar_loading,
            successMessageResId = R.string.repository_detail_unstar_success,
            starredAfterSuccess = false
        ) { gateway ->
            gateway.unstar(loadedOwner, loadedRepo)
        }
    }

    fun watchRepository() {
        runLightManagementAction(
            loadingMessageResId = R.string.repository_detail_watch_loading,
            successMessageResId = R.string.repository_detail_watch_success,
            watchingAfterSuccess = true
        ) { gateway ->
            gateway.watch(loadedOwner, loadedRepo)
        }
    }

    fun unwatchRepository() {
        runLightManagementAction(
            loadingMessageResId = R.string.repository_detail_unwatch_loading,
            successMessageResId = R.string.repository_detail_unwatch_success,
            watchingAfterSuccess = false
        ) { gateway ->
            gateway.unwatch(loadedOwner, loadedRepo)
        }
    }

    private fun runLightManagementAction(
        @StringRes loadingMessageResId: Int,
        @StringRes successMessageResId: Int,
        starredAfterSuccess: Boolean? = null,
        watchingAfterSuccess: Boolean? = null,
        action: suspend (GitHubRepositoryApiGateway) -> Unit
    ) {
        if (accessToken.isBlank() || loadedOwner.isBlank() || loadedRepo.isBlank()) return
        viewModelScope.launch {
            _repositoryState.value = currentContentState()?.copy(
                lightManagementMessageResId = loadingMessageResId,
                isLightManagementLoading = true
            ) ?: return@launch
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val gateway = GitHubRepositoryApiGateway(accessToken)
                    action(gateway)
                    gateway.getRepository(loadedOwner, loadedRepo)
                }
            }
            val latestState = currentContentState() ?: return@launch
            _repositoryState.value = if (result.isSuccess) {
                val updatedState = latestState.copy(
                    repository = result.getOrThrow(),
                    lightManagementMessageResId = successMessageResId,
                    isLightManagementLoading = false,
                    isStarred = starredAfterSuccess ?: latestState.isStarred,
                    isWatching = watchingAfterSuccess ?: latestState.isWatching,
                    lightManagementStateError = null
                )
                updateRepositoryDetailCache(updatedState)
                updatedState
            } else {
                latestState.copy(
                    lightManagementMessageResId = null,
                    lightManagementStateError = result.exceptionOrNull()?.message,
                    isLightManagementLoading = false
                )
            }
        }
    }

    private suspend fun loadRepositoryDetailInPhases(
        owner: String,
        repo: String,
        token: String,
        generation: Int,
        autoPreviewReadme: Boolean,
        forceRefresh: Boolean = false
    ) {
        val gateway = GitHubRepositoryApiGateway(token)
        val primaryResult = PerformanceTrace.measureSuspend(
            name = "repository.detail.primary",
            metadata = { "repo=$owner/$repo generation=$generation" }
        ) {
            withContext(Dispatchers.IO) {
                repositoryDetailLoadUseCase.loadPrimary(
                    gateway = gateway,
                    owner = owner,
                    repo = repo,
                    rootPath = RootPath
                )
            }
        }
        if (generation != repositoryLoadGeneration) return
        val repository = primaryResult.repository
        val contentsResult = primaryResult.contents
        val previousContent = currentContentState()
        val isForkRepository = repository.fork
        val coreState = RepositoryDetailUiState.Content(
            repository = repository,
            currentPath = RootPath,
            contents = contentsResult.getOrDefault(emptyList()),
            contentsError = contentsResult.exceptionOrNull()?.message,
            isContentsLoading = false,
            isStarred = previousContent?.isStarred,
            isWatching = previousContent?.isWatching,
            currentAccountFork = previousContent?.currentAccountFork?.takeIf { repository.canBeForkedBy(currentAccountLogin) },
            isForkEligibilityLoading = repository.canBeForkedBy(currentAccountLogin),
            forkEligibilityError = null,
            forkSyncStatus = previousContent?.forkSyncStatus?.takeIf { isForkRepository },
            forkSyncError = previousContent?.forkSyncError?.takeIf { isForkRepository },
            isForkSyncLoading = isForkRepository && previousContent?.isForkSyncLoading == true,
            forkSyncMessageResId = previousContent?.forkSyncMessageResId?.takeIf { isForkRepository },
            lightManagementStateError = previousContent?.lightManagementStateError,
            sidebarInfo = previousContent?.sidebarInfo,
            canPush = previousContent?.canPush ?: false,
            currentAccountLogin = currentAccountLogin
        )
        _repositoryState.value = coreState
        loadForkEligibility(owner = owner, repo = repo, token = token, generation = generation, repository = repository)
        if (isForkRepository) {
            loadForkSyncStatus(owner = owner, repo = repo, token = token, generation = generation)
        }
        if (contentsResult.isSuccess) {
            repositoryCacheRepository.cacheContents(owner, repo, RootPath, contentsResult.getOrDefault(emptyList()))
        }
        if (autoPreviewReadme) {
            findDefaultReadme(coreState.contents, coreState.contentsError)?.let { readme ->
                loadFilePreview(readme.path, readme.name)
            }
        }
        loadRepositorySecondaryDetail(
            owner = owner,
            repo = repo,
            token = token,
            generation = generation,
            baseRepository = repository,
            contents = coreState.contents,
            contentsError = coreState.contentsError
        )
    }

    private fun loadRepositorySecondaryDetail(
        owner: String,
        repo: String,
        token: String,
        generation: Int,
        baseRepository: GitHubRepository,
        contents: List<RepositoryContentItem>,
        contentsError: String?
    ) {
        val requestKey = "${owner.lowercase()}/${repo.lowercase()}:$generation"
        if (secondaryLoadKey == requestKey) return
        secondaryLoadKey = requestKey
        viewModelScope.launch {
            try {
                val result = PerformanceTrace.measureSuspend(
                    name = "repository.detail.secondary",
                    metadata = { "repo=$owner/$repo generation=$generation contents=${contents.size}" }
                ) {
                    withContext(Dispatchers.IO) {
                        runCatching {
                            repositoryDetailLoadUseCase.loadSecondary(
                                gateway = GitHubRepositoryApiGateway(token),
                                owner = owner,
                                repo = repo,
                                baseRepository = baseRepository,
                                rootPath = RootPath,
                                contents = contents,
                                contentsError = contentsError
                            )
                        }
                    }
                }
                if (generation != repositoryLoadGeneration) return@launch
                val latestState = currentContentState() ?: return@launch
                val loadedSecondary = result.getOrNull()
                val updatedState = if (loadedSecondary != null) {
                    latestState.copy(
                        repository = loadedSecondary.repository,
                        isStarred = loadedSecondary.isStarred ?: latestState.isStarred,
                        isWatching = loadedSecondary.isWatching ?: latestState.isWatching,
                        lightManagementStateError = loadedSecondary.lightManagementStateError,
                        sidebarInfo = mergeSidebarInfo(latestState.sidebarInfo, loadedSecondary.sidebarInfo),
                        canPush = loadedSecondary.canPush || latestState.canPush,
                        isRefreshingFromCache = false
                    )
                } else {
                    latestState.copy(
                        lightManagementStateError = result.exceptionOrNull()?.message,
                        sidebarInfo = RepositorySidebarInfo(error = result.exceptionOrNull()?.message),
                        isRefreshingFromCache = false
                    )
                }
                AppLogger.d(
                    RepositoryDetailLogTag,
                    "secondary publish repo=$owner/$repo generation=$generation sidebar=${describeSidebarInfo(updatedState.sidebarInfo)} starred=${updatedState.isStarred} watching=${updatedState.isWatching}"
                )
                _repositoryState.value = updatedState
                if (loadedSecondary != null) {
                    repositoryCacheRepository.cacheDetail(owner, repo, loadedSecondary)
                    if (contentsError.isNullOrBlank()) {
                        withContext(Dispatchers.IO) {
                            repositoryCacheRepository.cachePersistedDetail(
                                owner = owner,
                                repo = repo,
                                snapshot = RepositoryDetailCacheSnapshot(
                                    repository = loadedSecondary.repository,
                                    contents = loadedSecondary.contents,
                                    isStarred = loadedSecondary.isStarred,
                                    isWatching = loadedSecondary.isWatching,
                                    sidebarInfo = loadedSecondary.sidebarInfo,
                                    refreshedAtMillis = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }
            } finally {
                if (secondaryLoadKey == requestKey) {
                    secondaryLoadKey = ""
                }
            }
        }
    }

    private fun loadForkSyncStatus(
        owner: String,
        repo: String,
        token: String,
        generation: Int
    ) {
        val requestKey = "$owner/$repo@$generation"
        if (forkSyncLoadKey == requestKey) return
        forkSyncLoadKey = requestKey
        viewModelScope.launch {
            try {
                _repositoryState.value = currentContentState()?.copy(
                    forkSyncError = null
                ) ?: return@launch
                val result = withContext(Dispatchers.IO) {
                    runCatching { GitHubRepositoryApiGateway(token).getForkSyncStatus(owner, repo) }
                }
                if (generation != repositoryLoadGeneration) return@launch
                val latestState = currentContentState() ?: return@launch
                _repositoryState.value = if (result.isSuccess) {
                    latestState.copy(
                        forkSyncStatus = result.getOrThrow(),
                        forkSyncError = null,
                        isForkSyncLoading = false,
                        forkSyncMessageResId = null
                    )
                } else {
                    latestState.copy(
                        forkSyncError = result.exceptionOrNull()?.message,
                        isForkSyncLoading = false,
                        forkSyncMessageResId = null
                    )
                }
            } finally {
                if (forkSyncLoadKey == requestKey) {
                    forkSyncLoadKey = ""
                }
            }
        }
    }

    private suspend fun loadRepositoryDetail(
        owner: String,
        repo: String,
        token: String,
        forceRefresh: Boolean = false
    ): RepositoryDetailSecondaryResult {
        if (!forceRefresh) {
            repositoryCacheRepository.getDetail(owner, repo)?.let { return it }
        }
        return withContext(Dispatchers.IO) {
            val gateway = GitHubRepositoryApiGateway(token)
            val primaryResult = repositoryDetailLoadUseCase.loadPrimary(
                gateway = gateway,
                owner = owner,
                repo = repo,
                rootPath = RootPath
            )
            val result = repositoryDetailLoadUseCase.loadSecondary(
                gateway = gateway,
                owner = owner,
                repo = repo,
                baseRepository = primaryResult.repository,
                rootPath = RootPath,
                contents = primaryResult.contents.getOrDefault(emptyList()),
                contentsError = primaryResult.contents.exceptionOrNull()?.message
            )
            repositoryCacheRepository.cacheDetail(owner, repo, result)
            if (result.contentsError.isNullOrBlank()) {
                repositoryCacheRepository.cachePersistedDetail(
                    owner = owner,
                    repo = repo,
                    snapshot = RepositoryDetailCacheSnapshot(
                        repository = result.repository,
                        contents = result.contents,
                        isStarred = result.isStarred,
                        isWatching = result.isWatching,
                        sidebarInfo = result.sidebarInfo,
                        refreshedAtMillis = System.currentTimeMillis()
                    )
                )
            }
            result
        }
    }

    private fun loadBranches(forceRefresh: Boolean = false) {
        if (accessToken.isBlank() || loadedOwner.isBlank() || loadedRepo.isBlank()) return
        val current = currentContentState() ?: return
        if (!forceRefresh && cachedBranches.isNotEmpty()) {
            _repositoryState.value = current.copy(branches = cachedBranches, selectedBranch = currentBranchName(), isBranchesLoading = false, branchesError = null)
            return
        }
        val requestKey = "${loadedOwner}/${loadedRepo}:${repositoryLoadGeneration}"
        if (branchesLoadKey == requestKey && !forceRefresh) return
        branchesLoadKey = requestKey
        viewModelScope.launch {
            _repositoryState.value = currentContentState()?.copy(isBranchesLoading = true, branchesError = null, selectedBranch = currentBranchName()) ?: return@launch
            val owner = loadedOwner
            val repo = loadedRepo
            val result = withContext(Dispatchers.IO) { runCatching { GitHubRepositoryApiGateway(accessToken).listRepositoryBranches(owner, repo) } }
            val latest = currentContentState() ?: return@launch
            val branches = result.getOrNull()
            if (branches != null) cachedBranches = branches
            _repositoryState.value = latest.copy(
                branches = branches ?: latest.branches,
                selectedBranch = currentBranchName(),
                isBranchesLoading = false,
                branchesError = result.exceptionOrNull()?.message
            )
            if (branchesLoadKey == requestKey) branchesLoadKey = ""
        }
    }

    private fun currentBranchName(): String {
        return selectedBranch.ifBlank { currentContentState()?.repository?.defaultBranch.orEmpty() }.ifBlank { "main" }
    }

    private fun branchScopedPath(branch: String, path: String): String {
        return "refs/heads/${branch.trim()}/${path.trim('/')}"
    }

    private fun loadContents(path: String, forceRefresh: Boolean = false) {
        if (accessToken.isBlank() || loadedOwner.isBlank() || loadedRepo.isBlank()) return
        currentContentsPath = path
        val branch = currentBranchName()
        val contentsCachePath = branchScopedPath(branch, path)
        val cachedContents = if (!forceRefresh) {
            repositoryCacheRepository.getContents(loadedOwner, loadedRepo, contentsCachePath)
        } else {
            null
        }
        if (cachedContents != null) {
            _repositoryState.value = currentContentState()?.copy(
                currentPath = path,
                contents = cachedContents,
                contentsError = null,
                isContentsLoading = false,
                filePreview = null,
                filePreviewError = null,
                isFilePreviewLoading = false
            )
        }
        viewModelScope.launch {
            _repositoryState.value = currentContentState()?.copy(
                currentPath = path,
                contents = cachedContents ?: currentContentState()?.contents.orEmpty(),
                contentsError = null,
                isContentsLoading = cachedContents == null,
                filePreview = null,
                filePreviewError = null,
                isFilePreviewLoading = false
            ) ?: return@launch
            val contentsResult = withContext(Dispatchers.IO) {
                runCatching {
                    GitHubRepositoryApiGateway(accessToken).listContents(loadedOwner, loadedRepo, path, ref = branch)
                }
            }
            if (path != currentContentsPath) return@launch
            contentsResult.getOrNull()?.let { contents ->
                repositoryCacheRepository.cacheContents(loadedOwner, loadedRepo, contentsCachePath, contents)
            }
            val latestState = currentContentState() ?: return@launch
            val updatedState = latestState.copy(
                currentPath = path,
                contents = contentsResult.getOrDefault(latestState.contents),
                contentsError = contentsResult.exceptionOrNull()?.message,
                isContentsLoading = false
            )
            if (contentsResult.isSuccess) {
                updateRepositoryDetailCache(updatedState)
            }
            _repositoryState.value = updatedState
            if (contentsResult.isSuccess) {
                findDefaultReadme(updatedState.contents, updatedState.contentsError)?.let { readme ->
                    loadFilePreview(readme.path, readme.name)
                }
            }
        }
    }

    private fun invalidateFilePreview(path: String) {
        val normalizedPath = path.trim('/')
        if (normalizedPath.isBlank()) return
        if (cachedFilePreview?.path?.trim('/') == normalizedPath) {
            cachedFilePreview = null
            cachedFilePreviewKey = ""
            activeFilePreviewKey = ""
        }
        if (lastPreviewPath.trim('/') == normalizedPath) {
            lastPreviewPath = ""
            lastPreviewName = ""
        }
    }

    private fun loadFilePreview(path: String, name: String, forceRefresh: Boolean = false) {
        if (accessToken.isBlank() || loadedOwner.isBlank() || loadedRepo.isBlank()) return
        val cacheKey = repositoryCacheRepository.buildContentsKey(loadedOwner, loadedRepo, "${currentBranchName()}:${path.trim('/')}")
        val reusablePreview = if (!forceRefresh) cachedFilePreview?.takeIf { cachedFilePreviewKey == cacheKey && it.path == path } else null
        if (reusablePreview != null) {
            _repositoryState.value = currentContentState()?.copy(
                filePreview = reusablePreview,
                filePreviewError = null,
                isFilePreviewLoading = false
            )
            return
        }
        lastPreviewPath = path
        lastPreviewName = name
        activeFilePreviewKey = cacheKey
        viewModelScope.launch {
            val stateBeforeLoading = currentContentState() ?: return@launch
            _repositoryState.value = stateBeforeLoading.copy(
                filePreviewError = null,
                isFilePreviewLoading = true
            )
            val previewResult = withContext(Dispatchers.IO) {
                runCatching {
                    GitHubRepositoryApiGateway(accessToken).getFilePreview(loadedOwner, loadedRepo, path, ref = currentBranchName())
                }
            }
            if (activeFilePreviewKey != cacheKey) return@launch
            val latestState = currentContentState() ?: return@launch
            val preview = previewResult.getOrNull()
            if (preview != null) {
                cachedFilePreview = preview
                cachedFilePreviewKey = cacheKey
            }
            val currentState = currentContentState() ?: latestState
            AppLogger.d(
                RepositoryDetailLogTag,
                "preview publish repo=$loadedOwner/$loadedRepo path=$path sidebarBefore=${describeSidebarInfo(latestState.sidebarInfo)} sidebarCurrent=${describeSidebarInfo(currentState.sidebarInfo)}"
            )
            _repositoryState.value = currentState.copy(
                filePreview = preview ?: currentState.filePreview,
                filePreviewError = previewResult.exceptionOrNull()?.message ?: if (preview == null && currentState.filePreview == null) "$name 无法展示" else null,
                isFilePreviewLoading = false
            )
        }
    }

    private fun currentContentState(): RepositoryDetailUiState.Content? {
        return _repositoryState.value as? RepositoryDetailUiState.Content
    }

    private fun loadForkEligibility(
        owner: String,
        repo: String,
        token: String,
        generation: Int,
        repository: GitHubRepository
    ) {
        if (!repository.canBeForkedBy(currentAccountLogin)) {
            currentContentState()?.let { state ->
                if (state.isForkEligibilityLoading || state.currentAccountFork != null || state.forkEligibilityError != null) {
                    _repositoryState.value = state.copy(
                        currentAccountFork = null,
                        isForkEligibilityLoading = false,
                        forkEligibilityError = null
                    )
                }
            }
            return
        }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { GitHubRepositoryApiGateway(token).findCurrentAccountFork(owner, repo, currentAccountLogin) }
            }
            if (generation != repositoryLoadGeneration) return@launch
            val latestState = currentContentState() ?: return@launch
            _repositoryState.value = latestState.copy(
                currentAccountFork = result.getOrNull(),
                isForkEligibilityLoading = false,
                forkEligibilityError = result.exceptionOrNull()?.message
            )
        }
    }

    fun forkDraftForCurrentRepository(): RepositoryForkDraft? {
        val state = currentContentState() ?: return null
        if (!state.canCreateFork(currentAccountLogin)) return null
        return RepositoryForkDraft(
            sourceRepository = state.repository,
            targetOwner = currentAccountLogin,
            targetName = state.repository.name,
            defaultBranchOnly = false
        )
    }

    private fun RepositoryDetailUiState.Content.canCreateFork(currentAccountLogin: String): Boolean {
        return repository.canBeForkedBy(currentAccountLogin) && currentAccountFork == null
    }

    private fun GitHubRepository.canBeForkedBy(currentAccountLogin: String): Boolean {
        if (currentAccountLogin.isBlank()) return false
        if (ownerLogin.equals(currentAccountLogin, ignoreCase = true)) return false
        if (fork && sourceFullName?.equals(fullName, ignoreCase = true) == false) return false
        return true
    }

    data class RepositoryForkDraft(
        val sourceRepository: GitHubRepository,
        val targetOwner: String,
        val targetName: String,
        val defaultBranchOnly: Boolean
    )

    private fun createRepositoryTemplate(owner: String, repo: String): GitHubRepository {
        val normalizedOwner = owner.trim()
        val normalizedRepo = repo.trim()
        val fullName = listOf(normalizedOwner, normalizedRepo)
            .filter { it.isNotBlank() }
            .joinToString(separator = "/")
        return GitHubRepository(
            id = 0L,
            name = normalizedRepo,
            fullName = fullName,
            ownerLogin = normalizedOwner,
            description = null,
            isPrivate = false,
            fork = false,
            archived = false,
            defaultBranch = "",
            stargazersCount = 0,
            watchersCount = 0,
            forksCount = 0,
            openIssuesCount = 0,
            language = null,
            languages = emptyList(),
            ownerAvatarUrl = null,
            ownerName = null,
            ownerType = null,
            parentFullName = null,
            parentDefaultBranch = null,
            sourceFullName = null,
            htmlUrl = if (fullName.isBlank()) "" else "https://github.com/$fullName"
        )
    }

    private fun updateRepositoryDetailCache(state: RepositoryDetailUiState.Content) {
        if (state.contentsError.isNullOrBlank()) {
            repositoryCacheRepository.cacheContents(loadedOwner, loadedRepo, state.currentPath, state.contents)
        }
        val rootContents = repositoryCacheRepository.getContents(loadedOwner, loadedRepo, RootPath)
            ?: repositoryCacheRepository.getDetail(loadedOwner, loadedRepo)?.contents
            ?: emptyList()
        repositoryCacheRepository.cacheDetail(
            owner = loadedOwner,
            repo = loadedRepo,
            detail = RepositoryDetailSecondaryResult(
                repository = state.repository,
                currentPath = RootPath,
                contents = rootContents,
                contentsError = null,
                isStarred = state.isStarred,
                isWatching = state.isWatching,
                lightManagementStateError = state.lightManagementStateError,
                sidebarInfo = state.sidebarInfo,
                canPush = state.canPush
            )
        )
    }

    private fun String.toParentDirectoryPath(): String {
        val normalizedPath = trim('/')
        return normalizedPath.substringBeforeLast(delimiter = "/", missingDelimiterValue = RootPath)
    }

    private fun findDefaultReadme(
        contents: List<RepositoryContentItem>,
        contentsError: String?
    ): RepositoryContentItem.File? {
        if (!contentsError.isNullOrBlank()) return null
        val files = contents.filterIsInstance<RepositoryContentItem.File>()
        if (files.isEmpty()) return null
        val filesByLowercaseName = files.associateBy { it.name.lowercase() }
        ReadmePriorityNames.forEach { readmeName ->
            filesByLowercaseName[readmeName]?.let { return it }
        }
        return files.firstOrNull { file -> file.name.isReadmeVariantName() }
    }

    private fun String.isReadmeVariantName(): Boolean {
        val lowercaseName = lowercase()
        if (!lowercaseName.startsWith(ReadmePrefix)) return false
        return lowercaseName.substringAfter(ReadmePrefix).isNotBlank()
    }

    private fun mergeSidebarInfo(
        current: RepositorySidebarInfo?,
        incoming: RepositorySidebarInfo?
    ): RepositorySidebarInfo? {
        if (incoming == null) return current
        if (current == null) return incoming
        val incomingHasData = incoming.releases.isNotEmpty() || incoming.contributors.isNotEmpty() || incoming.languages.isNotEmpty()
        val currentHasData = current.releases.isNotEmpty() || current.contributors.isNotEmpty() || current.languages.isNotEmpty()
        if (incomingHasData || !currentHasData) return incoming
        return current.copy(error = incoming.error ?: current.error)
    }

    private fun describeSidebarInfo(sidebarInfo: RepositorySidebarInfo?): String {
        return if (sidebarInfo == null) {
            "loading"
        } else {
            "releases=${sidebarInfo.releases.size} contributors=${sidebarInfo.contributors.size} languages=${sidebarInfo.languages.size} error=${sidebarInfo.error.orEmpty()}"
        }
    }

    private companion object {
        const val RepositoryDetailLogTag = "RepositoryDetail"
        const val RootPath = ""
        const val ReadmePrefix = "readme."
        val ReadmePriorityNames = listOf(
            "readme.md",
            "readme.markdown",
            "readme.mdown",
            "readme.mkdn",
            "readme.rst",
            "readme.txt",
            "readme"
        )
    }
}