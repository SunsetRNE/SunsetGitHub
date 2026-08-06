package com.Sunset.REN.GitHub.ui.repo

import com.Sunset.REN.GitHub.domain.repo.RepositoryPullRequest

/** Pull Requests 列表页状态。state ∈ "open" / "closed" / "all"。 */
data class RepositoryPullRequestsUiState(
    val owner: String = "",
    val repo: String = "",
    val state: String = OpenState,
    val pullRequests: List<RepositoryPullRequest> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val hasMore: Boolean = false,
    val loadedPages: Int = 0,
    val isShowingStaleContent: Boolean = false
) {
    val isInitialLoad: Boolean
        get() = isLoading && pullRequests.isEmpty()

    val isEmpty: Boolean
        get() = !isLoading && pullRequests.isEmpty() && errorMessage == null

    companion object {
        const val OpenState = "open"
        const val ClosedState = "closed"
        const val AllState = "all"
    }
}