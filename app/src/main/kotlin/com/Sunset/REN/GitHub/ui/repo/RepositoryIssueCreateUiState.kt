package com.Sunset.REN.GitHub.ui.repo

import com.Sunset.REN.GitHub.domain.repo.RepositoryLabel

/** 新建问题页状态。 */
data class RepositoryIssueCreateUiState(
    val owner: String = "",
    val repo: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val createdIssueNumber: Int? = null,
    val availableLabels: List<RepositoryLabel> = emptyList(),
    val selectedLabels: List<String> = emptyList(),
    val isLoadingLabels: Boolean = false,
    val labelErrorMessage: String? = null
)