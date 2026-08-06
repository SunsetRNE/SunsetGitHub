package com.Sunset.REN.GitHub.ui.repo

import com.Sunset.REN.GitHub.domain.repo.RepositorySecurityAlert

data class RepositorySecurityAlertDetailUiState(
    val owner: String = "",
    val repo: String = "",
    val alertType: String = "",
    val number: Int = 0,
    val alert: RepositorySecurityAlert? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
