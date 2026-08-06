package com.Sunset.REN.GitHub.ui.repo

import com.Sunset.REN.GitHub.data.github.html.RepositoryHtmlSectionSummary

data class RepositoryProjectsUiState(
    val owner: String = "",
    val repo: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val summary: RepositoryHtmlSectionSummary? = null,
    val errorMessage: String? = null,
    val pendingMessage: String? = null,
    val sourceUrl: String? = null,
    val projectStateFilter: RepositoryProjectsFilter = RepositoryProjectsFilter.Open,
    val searchQuery: String = "",
    val sortNewestFirst: Boolean = true,
    val workspaceMode: RepositoryProjectsWorkspaceMode = RepositoryProjectsWorkspaceMode.Board,
    val selectedProjectTitle: String? = null,
    val selectedItemTitle: String? = null,
    val isShowingStaleContent: Boolean = false
) {
    val isInitialLoad: Boolean get() = isLoading && summary == null && errorMessage == null
}

enum class RepositoryProjectsFilter {
    Open,
    Closed
}

enum class RepositoryProjectsWorkspaceMode {
    Board,
    Table,
    Timeline
}
