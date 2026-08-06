package com.Sunset.REN.GitHub.ui.navigation

import com.Sunset.REN.GitHub.ui.repo.RepositorySection

interface NavigationMenuSurface {
    fun bindMainNavigation(
        currentDestinationId: Int?,
        isCurrentDestination: (Int) -> Boolean,
        onDestinationSelected: (Int) -> Unit
    )

    fun selectMainDestination(destinationId: Int?)

    fun bindRepositorySections(
        sections: List<RepositorySection>,
        selectedSection: RepositorySection,
        onSectionSelected: (RepositorySection) -> Unit,
        onMoreSelected: () -> Unit
    )

    fun selectRepositorySection(selectedSection: RepositorySection)

    fun restoreMainNavigation(configureMainNavigation: () -> Unit)
}