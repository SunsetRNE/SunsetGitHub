package com.Sunset.REN.GitHub.ui.navigation

import com.google.android.material.bottomnavigation.BottomNavigationView

class MainNavigationMenuBinder(
    private val navView: BottomNavigationView
) {
    private var isSettingSelectedItem = false

    fun bind(
        currentDestinationId: Int?,
        isCurrentDestination: (Int) -> Boolean,
        onDestinationSelected: (Int) -> Unit
    ) {
        navView.setOnItemSelectedListener { item ->
            if (isSettingSelectedItem) return@setOnItemSelectedListener true
            if (isCurrentDestination(item.itemId)) return@setOnItemSelectedListener true
            onDestinationSelected(item.itemId)
            true
        }
        selectDestination(currentDestinationId)
    }

    fun selectDestination(destinationId: Int?) {
        if (destinationId == null || navView.menu.findItem(destinationId) == null) return
        if (navView.selectedItemId == destinationId) return
        isSettingSelectedItem = true
        navView.selectedItemId = destinationId
        isSettingSelectedItem = false
    }
}