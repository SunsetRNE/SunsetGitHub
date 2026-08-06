package com.Sunset.REN.GitHub.ui.repo

interface RepositorySectionNavigationHost {
    fun showRepositorySectionNavigation(
        sections: List<RepositorySection>,
        selectedSection: RepositorySection,
        onSectionSelected: (RepositorySection) -> Unit
    )

    fun updateRepositorySectionSelection(selectedSection: RepositorySection)

    fun clearRepositorySectionNavigation()
}
