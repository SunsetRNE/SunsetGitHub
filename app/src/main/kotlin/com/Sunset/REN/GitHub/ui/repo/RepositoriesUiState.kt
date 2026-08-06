package com.Sunset.REN.GitHub.ui.repo

import com.Sunset.REN.GitHub.domain.repo.GitHubRepository
import com.Sunset.REN.GitHub.domain.repo.RepositoryLocalState

sealed class RepositoriesUiState {
    data object Loading : RepositoriesUiState()
    data object SignedOut : RepositoriesUiState()
    data object Empty : RepositoriesUiState()

    data class Content(
        val repositories: List<GitHubRepository>,
        val repositoryLocalStates: Map<String, RepositoryLocalState> = emptyMap(),
        val currentAccountLogin: String,
        val currentPage: Int,
        val canLoadMore: Boolean,
        val isRefreshingFromCache: Boolean = false,
        val isRefreshing: Boolean = false,
        val isLoadingMore: Boolean = false,
        val loadMoreError: String? = null,
        val refreshedAtMillis: Long? = null
    ) : RepositoriesUiState()

    data class Error(
        val message: String
    ) : RepositoriesUiState()
}