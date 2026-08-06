package com.Sunset.REN.GitHub.ui.repo

import com.Sunset.REN.GitHub.domain.repo.RepositoryActionRun
import com.Sunset.REN.GitHub.domain.repo.RepositoryActionWorkflow
import com.Sunset.REN.GitHub.domain.repo.RepositoryBranch

data class RepositoryActionsUiState(
    val owner: String = "",
    val repo: String = "",
    val status: String? = null,
    val selectedWorkflowId: Long? = null,
    val workflows: List<RepositoryActionWorkflow> = emptyList(),
    val workflowRuns: List<RepositoryActionRun> = emptyList(),
    val branches: List<RepositoryBranch> = emptyList(),
    val defaultBranch: String = "",
    val headBranch: String = "",
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val dispatchingWorkflowId: Long? = null,
    val errorMessage: String? = null,
    val unavailableMessage: String? = null,
    val hasMoreRuns: Boolean = true,
    val loadedRunPages: Int = 0,
    val isShowingStaleContent: Boolean = false
) {
    val actionRuns: List<RepositoryActionRun>
        get() = workflowRuns

    val hasMore: Boolean
        get() = hasMoreRuns

    val loadedPages: Int
        get() = loadedRunPages

    val isInitialLoad: Boolean
        get() = isLoading && workflowRuns.isEmpty() && workflows.isEmpty()

    val isEmpty: Boolean
        get() = !isLoading && workflowRuns.isEmpty() && workflows.isEmpty() && errorMessage == null && unavailableMessage == null

    val actionsHtmlUrl: String?
        get() = if (owner.isNotBlank() && repo.isNotBlank()) "https://github.com/$owner/$repo/actions" else null

    companion object {
        const val StatusAll = ""
        const val StatusQueued = "queued"
        const val StatusInProgress = "in_progress"
        const val StatusCompleted = "completed"
    }
}