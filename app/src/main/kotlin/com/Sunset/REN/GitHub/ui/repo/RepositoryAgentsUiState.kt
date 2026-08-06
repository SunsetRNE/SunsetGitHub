package com.Sunset.REN.GitHub.ui.repo

import com.Sunset.REN.GitHub.data.github.html.RepositoryAgentSession
import com.Sunset.REN.GitHub.data.github.html.RepositoryHtmlSectionSummary

data class RepositoryAgentsUiState(
    val owner: String = "",
    val repo: String = "",
    val isLoading: Boolean = false,
    val summary: RepositoryHtmlSectionSummary? = null,
    val sessions: List<RepositoryAgentSession> = emptyList(),
    val isExperimentalHtmlParse: Boolean = false,
    val errorMessage: String? = null,
    val sourceUrl: String? = null,
    val isShowingStaleContent: Boolean = false
) {
    val isInitialLoad: Boolean get() = isLoading && summary == null && errorMessage == null
}
