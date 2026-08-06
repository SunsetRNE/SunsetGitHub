package com.Sunset.REN.GitHub.ui.navigation

import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.data.local.RepositoryNavigationPreferencesRepository
import com.Sunset.REN.GitHub.ui.repo.RepositorySection
import com.Sunset.REN.GitHub.util.AppLogger

class NavigationBarHostController(
    private val surface: NavigationBarSurface,
    private val repositoryNavigationPreferencesRepository: RepositoryNavigationPreferencesRepository,
    private val showRepositoryMoreSections: () -> Unit,
    private val logTag: String
) {
    private lateinit var navController: NavController
    private var isRepositorySectionNavigationActive = false
    private var isAuthorized = false
    private var isFloatingNavigationEnabled = false

    fun attach(navController: NavController) {
        this.navController = navController
        surface.render(NavigationBarRenderState.hidden())
        configureMainNavigationBar()
    }

    fun updatePreferences(isAuthorized: Boolean, isFloatingNavigationEnabled: Boolean) {
        this.isAuthorized = isAuthorized
        this.isFloatingNavigationEnabled = isFloatingNavigationEnabled
    }

    fun updateSystemNavigationBottomInset(insetPx: Int) {
        surface.updateSystemNavigationBottomInset(insetPx)
    }

    fun onDestinationChanged(destinationId: Int, arguments: Bundle?) {
        updateRepositorySectionNavigationForDestination(destinationId, arguments)
        if (!isRepositorySectionNavigationActive) {
            updateMainNavigationSelection(destinationId)
        }
        applyNavigationBarForDestination(destinationId, arguments)
    }

    fun renderCurrentDestination(arguments: Bundle? = navController.currentBackStackEntry?.arguments) {
        applyNavigationBarForDestination(navController.currentDestination?.id, arguments)
    }

    fun showRepositorySectionNavigation(
        sections: List<RepositorySection>,
        selectedSection: RepositorySection,
        onSectionSelected: (RepositorySection) -> Unit
    ) {
        isRepositorySectionNavigationActive = true
        renderCurrentDestination()
        surface.bindRepositorySections(
            sections = sections,
            selectedSection = selectedSection,
            onSectionSelected = onSectionSelected,
            onMoreSelected = showRepositoryMoreSections
        )
    }

    fun updateRepositorySectionSelection(selectedSection: RepositorySection) {
        if (!isRepositorySectionNavigationActive) return
        surface.selectRepositorySection(selectedSection)
    }

    fun clearRepositorySectionNavigation() {
        if (!isRepositorySectionNavigationActive) return
        isRepositorySectionNavigationActive = false
        surface.restoreMainNavigation {
            configureMainNavigationBar()
        }
    }

    fun currentRepositoryFullName(): String {
        return NavigationDestinationRegistry.repositoryFullNameFrom(navController.currentBackStackEntry?.arguments)
    }

    fun currentShortcutSections(): List<RepositorySection> {
        return repositoryNavigationPreferencesRepository.getRepositoryShortcutSections(currentRepositoryFullName())
    }

    fun currentSectionOrder(): List<RepositorySection> {
        return repositoryNavigationPreferencesRepository.getRepositorySectionOrder(currentRepositoryFullName())
    }

    fun chooseRepositorySection(section: RepositorySection) {
        if (section != RepositorySection.More) {
            navigateToRepositorySection(section)
        }
    }

    fun pinRepositorySection(section: RepositorySection): Boolean {
        val repositoryFullName = currentRepositoryFullName()
        val before = repositoryNavigationPreferencesRepository.getRepositoryShortcutSections(repositoryFullName)
        val after = repositoryNavigationPreferencesRepository.pinShortcutSection(repositoryFullName, section)
        if (after == before) return false
        refreshRepositorySectionNavigationFromPreferences()
        return true
    }

    fun unpinRepositorySection(section: RepositorySection): Boolean {
        val repositoryFullName = currentRepositoryFullName()
        val before = repositoryNavigationPreferencesRepository.getRepositoryShortcutSections(repositoryFullName)
        val after = repositoryNavigationPreferencesRepository.unpinShortcutSection(repositoryFullName, section)
        if (after == before) return false
        refreshRepositorySectionNavigationFromPreferences()
        return true
    }

    fun reorderRepositorySections(sections: List<RepositorySection>): Boolean {
        val repositoryFullName = currentRepositoryFullName()
        val before = repositoryNavigationPreferencesRepository.getRepositorySectionOrder(repositoryFullName)
        repositoryNavigationPreferencesRepository.setRepositorySectionOrder(repositoryFullName, sections)
        val after = repositoryNavigationPreferencesRepository.getRepositorySectionOrder(repositoryFullName)
        if (after == before) return false
        refreshRepositorySectionNavigationFromPreferences()
        return true
    }

    private fun configureMainNavigationBar() {
        surface.bindMainNavigation(
            currentDestinationId = navController.currentDestination?.id,
            isCurrentDestination = { destinationId -> navController.currentDestination?.id == destinationId },
            onDestinationSelected = ::navigateToMainNavigationDestination
        )
    }

    private fun navigateToMainNavigationDestination(destinationId: Int) {
        val navOptions = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setRestoreState(true)
            .setPopUpTo(R.id.navigation_home, false, true)
            .build()
        runCatching {
            navController.navigate(destinationId, null, navOptions)
        }.onFailure { error ->
            AppLogger.e(logTag, "main navigation failed: $destinationId", error)
        }
    }

    private fun updateMainNavigationSelection(destinationId: Int?) {
        surface.selectMainDestination(destinationId)
    }

    private fun applyNavigationBarForDestination(destinationId: Int?, arguments: Bundle? = null) {
        val state = NavigationDestinationRegistry.resolveRenderState(
            destinationId = destinationId,
            isAuthorized = isAuthorized,
            isFloatingNavigationEnabled = isFloatingNavigationEnabled,
            forceHidden = destinationId == R.id.navigation_profile && isOtherUserProfile(arguments)
        )
        surface.render(state)
    }

    private fun updateRepositorySectionNavigationForDestination(destinationId: Int, arguments: Bundle?) {
        if (
            NavigationDestinationRegistry.isRepositoryRelatedDestination(destinationId) &&
            NavigationDestinationRegistry.hasRepositoryIdentity(arguments)
        ) {
            refreshRepositorySectionNavigationFromPreferences(destinationId, arguments)
        } else if (isRepositorySectionNavigationActive) {
            clearRepositorySectionNavigation()
        }
    }

    private fun refreshRepositorySectionNavigationFromPreferences(
        destinationId: Int = navController.currentDestination?.id ?: R.id.repository_detail_fragment,
        arguments: Bundle? = navController.currentBackStackEntry?.arguments
    ) {
        val repositoryFullName = NavigationDestinationRegistry.repositoryFullNameFrom(arguments)
        if (repositoryFullName.isBlank()) return
        val shortcutSections = repositoryNavigationPreferencesRepository.getRepositoryShortcutSections(repositoryFullName)
        val orderedShortcutSections = repositoryNavigationPreferencesRepository
            .getRepositorySectionOrder(repositoryFullName)
            .filter { section -> section in shortcutSections }
        val navigationSections = listOf(RepositorySection.Code) + orderedShortcutSections + RepositorySection.More
        val selectedSection = NavigationDestinationRegistry.sectionForDestination(destinationId)
        showRepositorySectionNavigation(navigationSections, selectedSection, onSectionSelected = { section ->
            if (section == RepositorySection.More) showRepositoryMoreSections() else navigateToRepositorySection(section)
        })
    }

    private fun navigateToRepositorySection(section: RepositorySection): Boolean {
        val destinationId = NavigationDestinationRegistry.destinationForSection(section) ?: return false
        if (navController.currentDestination?.id == destinationId) {
            surface.selectRepositorySection(section)
            return true
        }
        val arguments = NavigationDestinationRegistry.argumentsForSection(
            section = section,
            currentArguments = navController.currentBackStackEntry?.arguments
        ) ?: return false
        return runCatching {
            navController.navigate(destinationId, arguments)
            true
        }.getOrElse { error ->
            AppLogger.e(logTag, "repository section navigation failed: $section", error)
            false
        }
    }

    private fun isOtherUserProfile(arguments: Bundle?): Boolean {
        return !arguments?.getString(com.Sunset.REN.GitHub.ui.profile.ProfileFragment.ARG_LOGIN).isNullOrBlank()
    }
}
