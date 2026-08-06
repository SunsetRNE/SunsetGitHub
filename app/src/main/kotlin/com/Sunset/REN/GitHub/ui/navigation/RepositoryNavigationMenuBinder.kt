package com.Sunset.REN.GitHub.ui.navigation

import android.view.Menu
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.ui.repo.RepositorySection
import com.google.android.material.bottomnavigation.BottomNavigationView

class RepositoryNavigationMenuBinder(
    private val navView: BottomNavigationView,
    private val getTitle: (Int) -> String
) {
    private var isSettingSelectedItem = false
    private var onSectionSelected: ((RepositorySection) -> Unit)? = null

    fun bindSections(
        sections: List<RepositorySection>,
        selectedSection: RepositorySection,
        onSectionSelected: (RepositorySection) -> Unit,
        onMoreSelected: () -> Unit
    ) {
        this.onSectionSelected = onSectionSelected
        updateMenu(sections)
        bindListeners(onMoreSelected)
        selectSection(selectedSection)
    }

    fun clearAndRestoreMainMenu(configureMainNavigation: () -> Unit) {
        onSectionSelected = null
        navView.setOnItemSelectedListener(null)
        navView.setOnItemReselectedListener(null)
        navView.menu.clear()
        navView.inflateMenu(R.menu.bottom_nav_menu)
        configureMainNavigation()
    }

    fun selectSection(selectedSection: RepositorySection) {
        if (navView.selectedItemId == selectedSection.menuItemId) return
        isSettingSelectedItem = true
        navView.selectedItemId = selectedSection.menuItemId
        isSettingSelectedItem = false
    }

    private fun updateMenu(sections: List<RepositorySection>) {
        val currentMenuIds = (0 until navView.menu.size()).map { index -> navView.menu.getItem(index).itemId }
        val targetMenuIds = sections.map { section -> section.menuItemId }
        if (currentMenuIds == targetMenuIds) return

        navView.menu.clear()
        sections.forEach { section ->
            navView.menu.add(Menu.NONE, section.menuItemId, Menu.NONE, getTitle(section.titleResId)).apply {
                setIcon(section.navigationIconResId)
                isCheckable = true
            }
        }
    }

    private fun bindListeners(onMoreSelected: () -> Unit) {
        navView.setOnItemSelectedListener { item ->
            if (isSettingSelectedItem) return@setOnItemSelectedListener true
            val section = RepositorySection.fromMenuItemId(item.itemId) ?: return@setOnItemSelectedListener false
            if (section == RepositorySection.More) {
                onMoreSelected()
                // More only opens the section sheet; returning false keeps the current section highlighted.
                return@setOnItemSelectedListener false
            }
            onSectionSelected?.invoke(section)
            true
        }
        navView.setOnItemReselectedListener { item ->
            val section = RepositorySection.fromMenuItemId(item.itemId) ?: return@setOnItemReselectedListener
            if (section == RepositorySection.More) {
                onMoreSelected()
            }
        }
    }
}
