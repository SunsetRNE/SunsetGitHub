package com.Sunset.REN.GitHub.ui.repo

import androidx.annotation.StringRes
import com.Sunset.REN.GitHub.domain.repo.GitHubRepository
import com.Sunset.REN.GitHub.domain.repo.RepositoryBranch
import com.Sunset.REN.GitHub.domain.repo.RepositoryContentItem
import com.Sunset.REN.GitHub.domain.repo.RepositoryFilePreview
import com.Sunset.REN.GitHub.domain.repo.RepositoryForkSyncStatus
import com.Sunset.REN.GitHub.domain.repo.RepositoryPullRequest
import com.Sunset.REN.GitHub.domain.repo.RepositorySidebarInfo

sealed class RepositoryDetailUiState {
    data object Loading : RepositoryDetailUiState()
    data object SignedOut : RepositoryDetailUiState()

    data class Content(
        val repository: GitHubRepository,
        val currentPath: String,
        val contents: List<RepositoryContentItem>,
        val contentsError: String? = null,
        val isContentsLoading: Boolean = false,
        val filePreview: RepositoryFilePreview? = null,
        val filePreviewError: String? = null,
        val isFilePreviewLoading: Boolean = false,
        val selectedBranch: String = repository.defaultBranch,
        val branches: List<RepositoryBranch> = emptyList(),
        val isBranchesLoading: Boolean = false,
        val branchesError: String? = null,
        val sidebarInfo: RepositorySidebarInfo? = null,
        @StringRes val lightManagementMessageResId: Int? = null,
        val isLightManagementLoading: Boolean = false,
        val isStarred: Boolean? = null,
        val isWatching: Boolean? = null,
        val forkedRepository: GitHubRepository? = null,
        val currentAccountFork: GitHubRepository? = null,
        val isForkEligibilityLoading: Boolean = false,
        val forkEligibilityError: String? = null,
        val forkSyncStatus: RepositoryForkSyncStatus? = null,
        val forkSyncError: String? = null,
        val isForkSyncLoading: Boolean = false,
        @StringRes val forkSyncMessageResId: Int? = null,
        val createdPullRequest: RepositoryPullRequest? = null,
        val contributeError: String? = null,
        val isContributeLoading: Boolean = false,
        @StringRes val contributeMessageResId: Int? = null,
        val lightManagementStateError: String? = null,
        val isRefreshingFromCache: Boolean = false,
        val canPush: Boolean = false,
        val currentAccountLogin: String = ""
    ) : RepositoryDetailUiState() {
        val regions: RepositoryDetailRegions
            get() = RepositoryDetailRegions(
                header = RepositoryDetailHeaderRegion(repository = repository, isRefreshingFromCache = isRefreshingFromCache),
                contents = RepositoryDetailContentsRegion(
                    currentPath = currentPath,
                    contents = contents,
                    contentsError = contentsError,
                    isLoading = isContentsLoading,
                    repositoryOwnerLogin = repository.ownerLogin,
                    currentAccountLogin = currentAccountLogin
                ),
                preview = RepositoryDetailPreviewRegion(
                    filePreview = filePreview,
                    filePreviewError = filePreviewError,
                    isLoading = isFilePreviewLoading
                ),
                sidebar = RepositoryDetailSidebarRegion(
                    sidebarInfo = sidebarInfo,
                    canPush = canPush
                ),
                management = RepositoryDetailManagementRegion(
                    lightManagementMessageResId = lightManagementMessageResId,
                    isLightManagementLoading = isLightManagementLoading,
                    isStarred = isStarred,
                    isWatching = isWatching,
                    forkedRepository = forkedRepository,
                    currentAccountFork = currentAccountFork,
                    isForkEligibilityLoading = isForkEligibilityLoading,
                    forkEligibilityError = forkEligibilityError,
                    forkSyncStatus = forkSyncStatus,
                    forkSyncError = forkSyncError,
                    isForkSyncLoading = isForkSyncLoading,
                    forkSyncMessageResId = forkSyncMessageResId,
                    createdPullRequest = createdPullRequest,
                    contributeError = contributeError,
                    isContributeLoading = isContributeLoading,
                    contributeMessageResId = contributeMessageResId,
                    lightManagementStateError = lightManagementStateError
                )
            )
    }

    data class Error(
        val message: String
    ) : RepositoryDetailUiState()
}

data class RepositoryDetailRegions(
    val header: RepositoryDetailHeaderRegion,
    val contents: RepositoryDetailContentsRegion,
    val preview: RepositoryDetailPreviewRegion,
    val sidebar: RepositoryDetailSidebarRegion,
    val management: RepositoryDetailManagementRegion
)

data class RepositoryDetailHeaderRegion(
    val repository: GitHubRepository,
    val isRefreshingFromCache: Boolean
)

data class RepositoryDetailContentsRegion(
    val currentPath: String,
    val contents: List<RepositoryContentItem>,
    val contentsError: String?,
    val isLoading: Boolean,
    val repositoryOwnerLogin: String,
    val currentAccountLogin: String
)

data class RepositoryDetailPreviewRegion(
    val filePreview: RepositoryFilePreview?,
    val filePreviewError: String?,
    val isLoading: Boolean
)

data class RepositoryDetailSidebarRegion(
    val sidebarInfo: RepositorySidebarInfo?,
    val canPush: Boolean
)

data class RepositoryDetailManagementRegion(
    @StringRes val lightManagementMessageResId: Int?,
    val isLightManagementLoading: Boolean,
    val isStarred: Boolean?,
    val isWatching: Boolean?,
    val forkedRepository: GitHubRepository?,
    val currentAccountFork: GitHubRepository?,
    val isForkEligibilityLoading: Boolean,
    val forkEligibilityError: String?,
    val forkSyncStatus: RepositoryForkSyncStatus?,
    val forkSyncError: String?,
    val isForkSyncLoading: Boolean,
    @StringRes val forkSyncMessageResId: Int?,
    val createdPullRequest: RepositoryPullRequest?,
    val contributeError: String?,
    val isContributeLoading: Boolean,
    @StringRes val contributeMessageResId: Int?,
    val lightManagementStateError: String?
)