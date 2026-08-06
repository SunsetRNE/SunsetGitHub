package com.Sunset.REN.GitHub.ui.repo

import com.Sunset.REN.GitHub.domain.repo.RepositoryIssue
import com.Sunset.REN.GitHub.domain.repo.RepositoryLabel

/** 问题列表页状态。state ∈ "open" / "closed"。 */
data class RepositoryIssuesUiState(
    val owner: String = "",
    val repo: String = "",
    val state: String = OpenState,
    val issues: List<RepositoryIssue> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val hasMore: Boolean = false,
    val loadedPages: Int = 0,
    val currentUserLogin: String? = null,
    val selectedCreator: String? = null,
    val availableLabels: List<RepositoryLabel> = emptyList(),
    val selectedLabels: List<String> = emptyList(),
    val isLoadingLabels: Boolean = false,
    val labelErrorMessage: String? = null,
    val isShowingStaleContent: Boolean = false,
    // 写操作能力相关
    val canPush: Boolean = false
) {
    val isInitialLoad: Boolean
        get() = isLoading && issues.isEmpty()

    val isEmpty: Boolean
        get() = !isLoading && issues.isEmpty() && errorMessage == null

    /** 是否显示「新建问题」入口：需要写权限。 */
    val canCreateIssue: Boolean
        get() = canPush

    companion object {
        const val OpenState = "open"
        const val ClosedState = "closed"
    }
}