package com.Sunset.REN.GitHub.ui.repo

import com.Sunset.REN.GitHub.domain.repo.RepositoryActionArtifact
import com.Sunset.REN.GitHub.domain.repo.RepositoryActionRunDetail
import com.Sunset.REN.GitHub.domain.repo.RepositoryActionRunLogPreview

data class RepositoryActionRunDetailUiState(
    val owner: String = "",
    val repo: String = "",
    val runId: Long = 0L,
    val actionRun: RepositoryActionRunDetail? = null,
    val artifacts: List<RepositoryActionArtifact> = emptyList(),
    val logPreview: RepositoryActionRunLogPreview? = null,
    val refreshedAtMillis: Long = 0L,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingArtifacts: Boolean = false,
    val isLoadingLogs: Boolean = false,
    val artifactsErrorMessage: String? = null,
    val logsErrorMessage: String? = null,
    val errorMessage: String? = null,
    val unavailableMessage: String? = null
) {
    val actionsHtmlUrl: String?
        get() = if (owner.isNotBlank() && repo.isNotBlank()) "https://github.com/$owner/$repo/actions" else null
}