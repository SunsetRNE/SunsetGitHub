package com.Sunset.REN.GitHub.ui.repo

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.Sunset.REN.GitHub.data.local.RepositoryNavigationPreferencesRepository

/**
 * Fragment base for repository section pages that share the bottom repository navigation bar.
 *
 * Each visible section page must own the navigation callback. Otherwise the Activity may keep a
 * callback registered by a previous RepositoryDetailFragment instance; tapping "More" from a child
 * section page would then try to show the sheet from that stale fragment and can crash.
 */
abstract class RepositorySectionFragment : Fragment(), RepositoryMoreSectionsBottomSheet.Host {

    protected abstract val repositoryOwner: String
    protected abstract val repositoryName: String
    protected abstract val selectedRepositorySection: RepositorySection

    private val repositorySectionNavigationHost: RepositorySectionNavigationHost?
        get() = activity as? RepositorySectionNavigationHost

    private val navigationPreferencesRepository: RepositoryNavigationPreferencesRepository by lazy {
        RepositoryNavigationPreferencesRepository(requireContext().applicationContext)
    }

    protected fun renderRepositorySectionNavigation() {
        val repositoryFullName = currentRepositoryFullName()
        val shortcutSections = navigationPreferencesRepository.getRepositoryShortcutSections(repositoryFullName)
        val orderedShortcutSections = navigationPreferencesRepository
            .getRepositorySectionOrder(repositoryFullName)
            .filter { section -> section in shortcutSections }
        val navigationSections = listOf(RepositorySection.Code) + orderedShortcutSections + RepositorySection.More

        repositorySectionNavigationHost?.showRepositorySectionNavigation(
            navigationSections,
            selectedRepositorySection,
            ::onRepositorySectionTabSelected
        )
    }

    private fun currentRepositoryFullName(): String {
        return if (repositoryOwner.isNotBlank() && repositoryName.isNotBlank()) {
            "$repositoryOwner/$repositoryName"
        } else {
            repositoryName.ifBlank { repositoryOwner }
        }
    }

    private fun onRepositorySectionTabSelected(section: RepositorySection) {
        if (section == RepositorySection.More) {
            showMoreSectionsSheet()
            return
        }
        selectRepositorySection(section)
    }

    private fun showMoreSectionsSheet() {
        if (!isAdded || childFragmentManager.isStateSaved) return
        if (childFragmentManager.findFragmentByTag(RepositoryMoreSectionsBottomSheet.FRAGMENT_TAG) != null) return
        RepositoryMoreSectionsBottomSheet.newInstance()
            .show(childFragmentManager, RepositoryMoreSectionsBottomSheet.FRAGMENT_TAG)
    }

    private fun selectRepositorySection(section: RepositorySection) {
        if (section == selectedRepositorySection) {
            repositorySectionNavigationHost?.updateRepositorySectionSelection(selectedRepositorySection)
            return
        }
        val destinationId = section.destinationIdResId ?: return
        if (repositoryOwner.isBlank() || repositoryName.isBlank()) return
        findNavController().navigate(
            destinationId,
            Bundle().apply {
                putString(RepositoryDetailFragment.ARG_OWNER, repositoryOwner)
                putString(RepositoryDetailFragment.ARG_REPO, repositoryName)
                if (section == RepositorySection.Code) {
                    putString(RepositoryDetailFragment.ARG_FULL_NAME, currentRepositoryFullName())
                }
            }
        )
    }

    override fun currentShortcutSections(): List<RepositorySection> {
        return navigationPreferencesRepository.getRepositoryShortcutSections(currentRepositoryFullName())
    }

    override fun currentSectionOrder(): List<RepositorySection> {
        return navigationPreferencesRepository.getRepositorySectionOrder(currentRepositoryFullName())
    }

    override fun isSectionSupportedInApp(section: RepositorySection): Boolean {
        return section.destinationIdResId != null && section != RepositorySection.More
    }

    override fun onSectionChosen(section: RepositorySection) {
        onRepositorySectionTabSelected(section)
    }

    override fun onSectionPinned(section: RepositorySection): Boolean {
        val before = currentShortcutSections()
        val after = navigationPreferencesRepository.pinShortcutSection(currentRepositoryFullName(), section)
        if (after == before) return false
        renderRepositorySectionNavigation()
        return true
    }

    override fun onSectionUnpinned(section: RepositorySection): Boolean {
        val before = currentShortcutSections()
        val after = navigationPreferencesRepository.unpinShortcutSection(currentRepositoryFullName(), section)
        if (after == before) return false
        renderRepositorySectionNavigation()
        return true
    }

    override fun onSectionOrderChanged(sections: List<RepositorySection>): Boolean {
        val before = currentSectionOrder()
        navigationPreferencesRepository.setRepositorySectionOrder(currentRepositoryFullName(), sections)
        val after = currentSectionOrder()
        if (after == before) return false
        renderRepositorySectionNavigation()
        return true
    }
}
