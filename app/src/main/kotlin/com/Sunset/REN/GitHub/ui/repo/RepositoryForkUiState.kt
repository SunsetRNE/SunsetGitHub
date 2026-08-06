package com.Sunset.REN.GitHub.ui.repo

import com.Sunset.REN.GitHub.domain.repo.GitHubRepository

sealed class RepositoryForkUiState {
    data object Loading : RepositoryForkUiState()
    data object SignedOut : RepositoryForkUiState()

    data class Content(
        val sourceRepository: GitHubRepository,
        val currentAccountLogin: String,
        val existingFork: GitHubRepository? = null,
        val isCheckingExistingFork: Boolean = false,
        val targetOwner: String = currentAccountLogin,
        val targetName: String = sourceRepository.name,
        val description: String = sourceRepository.description.orEmpty(),
        val isNameAvailable: Boolean? = null,
        val isCheckingName: Boolean = false,
        val nameCheckError: String? = null,
        val eligibilityError: String? = null,
        val isCreating: Boolean = false,
        val createdFork: GitHubRepository? = null,
        val errorMessage: String? = null
    ) : RepositoryForkUiState() {
        val canCreateFork: Boolean
            get() = eligibilityError == null && existingFork == null && !isCheckingExistingFork && !isCheckingName && isNameAvailable != false && !isCreating
    }

    data class Error(val message: String) : RepositoryForkUiState()
}
