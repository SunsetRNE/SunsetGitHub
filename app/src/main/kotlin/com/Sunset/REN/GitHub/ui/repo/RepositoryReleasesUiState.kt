package com.Sunset.REN.GitHub.ui.repo

import com.Sunset.REN.GitHub.domain.repo.RepositoryRelease

data class RepositoryReleasesUiState(
    val owner: String = "",
    val repo: String = "",
    val releases: List<RepositoryRelease> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val hasMore: Boolean = false,
    val loadedPages: Int = 0,
    val isShowingStaleContent: Boolean = false
) {
    val isInitialLoad: Boolean
        get() = isLoading && releases.isEmpty()

    val isEmpty: Boolean
        get() = !isLoading && releases.isEmpty() && errorMessage == null
}
