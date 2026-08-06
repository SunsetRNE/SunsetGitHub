package com.Sunset.REN.GitHub.ui.navigation

import android.content.res.Resources
import android.view.View
import android.view.ViewGroup
import com.Sunset.REN.GitHub.ui.repo.RepositorySection
import com.google.android.material.bottomnavigation.BottomNavigationView

class MaterialBottomNavigationBarSurface(
    container: ViewGroup,
    navView: BottomNavigationView,
    divider: View,
    navHostViewProvider: () -> ViewGroup?,
    resources: Resources,
    getTitle: (Int) -> String
) : NavigationBarSurface {
    private val renderer = MaterialBottomNavigationBarRenderer(
        container = container,
        navView = navView,
        divider = divider,
        navHostViewProvider = navHostViewProvider,
        resources = resources
    )
    private val menuSurface = MaterialBottomNavigationMenuSurface(
        navView = navView,
        getTitle = getTitle
    )

    override fun render(state: NavigationBarRenderState) {
        renderer.render(state)
    }

    override fun updateSystemNavigationBottomInset(insetPx: Int) {
        renderer.updateSystemNavigationBottomInset(insetPx)
    }

    override fun bindMainNavigation(
        currentDestinationId: Int?,
        isCurrentDestination: (Int) -> Boolean,
        onDestinationSelected: (Int) -> Unit
    ) {
        menuSurface.bindMainNavigation(currentDestinationId, isCurrentDestination, onDestinationSelected)
    }

    override fun selectMainDestination(destinationId: Int?) {
        menuSurface.selectMainDestination(destinationId)
    }

    override fun bindRepositorySections(
        sections: List<RepositorySection>,
        selectedSection: RepositorySection,
        onSectionSelected: (RepositorySection) -> Unit,
        onMoreSelected: () -> Unit
    ) {
        menuSurface.bindRepositorySections(sections, selectedSection, onSectionSelected, onMoreSelected)
    }

    override fun selectRepositorySection(selectedSection: RepositorySection) {
        menuSurface.selectRepositorySection(selectedSection)
    }

    override fun restoreMainNavigation(configureMainNavigation: () -> Unit) {
        menuSurface.restoreMainNavigation(configureMainNavigation)
    }
}