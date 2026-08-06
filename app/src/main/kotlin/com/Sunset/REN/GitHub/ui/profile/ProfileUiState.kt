package com.Sunset.REN.GitHub.ui.profile

import com.Sunset.REN.GitHub.domain.profile.GitHubContributionCalendar
import com.Sunset.REN.GitHub.domain.profile.GitHubUserProfile
import com.Sunset.REN.GitHub.domain.repo.GitHubRepository

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    object SignedOut : ProfileUiState()
    data class Content(
        val profile: GitHubUserProfile,
        val profileRepositories: List<GitHubRepository>,
        val primaryLanguage: String?,
        val languageSummaries: List<ProfileLanguageSummary>,
        val contributionCalendar: GitHubContributionCalendar?,
        val contributionError: String?,
        val sourceRepositoryCount: Int,
        val forkRepositoryCount: Int,
        val archivedRepositoryCount: Int,
        val totalStars: Int,
        val totalForks: Int,
        val totalWatchers: Int,
        val totalOpenIssues: Int,
        val refreshedAtMillis: Long?,
        val isRefreshingFromCache: Boolean,
        val refreshError: String? = null
    ) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

data class ProfileLanguageSummary(
    val name: String,
    val repositoryCount: Int,
    val bytes: Long,
    val percentage: Int
)