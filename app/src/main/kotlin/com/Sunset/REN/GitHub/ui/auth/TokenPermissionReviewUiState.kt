package com.Sunset.REN.GitHub.ui.auth

import com.Sunset.REN.GitHub.domain.auth.GitHubAccount
import com.Sunset.REN.GitHub.domain.auth.TokenPermissionStatus

data class TokenPermissionReviewUiState(
    val token: String = "",
    val account: GitHubAccount? = null,
    val scopes: List<String> = emptyList(),
    val checks: List<TokenPermissionCheckUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val signedInLogin: String? = null
)

data class TokenPermissionCheckUiModel(
    val title: String,
    val description: String,
    val status: TokenPermissionStatus,
    val detail: String,
    val isCritical: Boolean = false
)
