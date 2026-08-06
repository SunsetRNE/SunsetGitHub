package com.Sunset.REN.GitHub.ui.navigation

import android.os.Bundle
import com.Sunset.REN.GitHub.ui.repo.RepositorySection

object RepositorySectionDestinationRegistry {

    fun isRepositoryRelatedDestination(destinationId: Int): Boolean {
        return NavigationDestinationRegistry.isRepositoryRelatedDestination(destinationId)
    }

    fun hasRepositoryIdentity(arguments: Bundle?): Boolean {
        return NavigationDestinationRegistry.hasRepositoryIdentity(arguments)
    }

    fun repositoryFullNameFrom(arguments: Bundle?): String {
        return NavigationDestinationRegistry.repositoryFullNameFrom(arguments)
    }

    fun sectionForDestination(destinationId: Int?): RepositorySection {
        return NavigationDestinationRegistry.sectionForDestination(destinationId)
    }

    fun destinationForSection(section: RepositorySection): Int? {
        return NavigationDestinationRegistry.destinationForSection(section)
    }

    fun argumentsForSection(section: RepositorySection, currentArguments: Bundle?): Bundle? {
        return NavigationDestinationRegistry.argumentsForSection(section, currentArguments)
    }
}