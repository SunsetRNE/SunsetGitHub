package com.Sunset.REN.GitHub.ui.navigation

import com.Sunset.REN.GitHub.ui.repo.RepositorySection
import com.google.android.material.bottomnavigation.BottomNavigationView

class MaterialBottomNavigationMenuSurface(
    navView: BottomNavigationView,
    getTitle: (Int) -> String
) : NavigationMenuSurface {
    private val mainNavigationMenuBinder = MainNavigationMenuBinder(navView)
    private val repositoryNavigationMenuBinder = RepositoryNavigationMenuBinder(
        navView = navView,
        getTitle = getTitle
    )

    override fun bindMainNavigation(
        currentDestinationId: Int?,
        isCurrentDestination: (Int) -> Boolean,
        onDestinationSelected: (Int) -> Unit
    ) {
        mainNavigationMenuBinder.bind(
            currentDestinationId = currentDestinationId,
            isCurrentDestination = isCurrentDestination,
            onDestinationSelected = onDestinationSelected
        )
    }

    override fun selectMainDestination(destinationId: Int?) {
        mainNavigationMenuBinder.selectDestination(destinationId)
    }

    override fun bindRepositorySections(
        sections: List<RepositorySection>,
        selectedSection: RepositorySection,
        onSectionSelected: (RepositorySection) -> Unit,
        onMoreSelected: () -> Unit
    ) {
        repositoryNavigationMenuBinder.bindSections(
            sections = sections,
            selectedSection = selectedSection,
            onSectionSelected = onSectionSelected,
            onMoreSelected = onMoreSelected
        )
    }

    override fun selectRepositorySection(selectedSection: RepositorySection) {
        repositoryNavigationMenuBinder.selectSection(selectedSection)
    }

    override fun restoreMainNavigation(configureMainNavigation: () -> Unit) {
        repositoryNavigationMenuBinder.clearAndRestoreMainMenu(configureMainNavigation)
    }
}