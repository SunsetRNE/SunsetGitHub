package com.Sunset.REN.GitHub.ui.repo

import com.Sunset.REN.GitHub.data.github.html.RepositoryHtmlSectionSummary

data class RepositoryWikiUiState(
    val owner: String = "",
    val repo: String = "",
    val isLoading: Boolean = false,
    val summary: RepositoryHtmlSectionSummary? = null,
    val errorMessage: String? = null,
    val sourceUrl: String? = null,
    val isShowingStaleContent: Boolean = false
) {
    val isInitialLoad: Boolean get() = isLoading && summary == null && errorMessage == null
}
