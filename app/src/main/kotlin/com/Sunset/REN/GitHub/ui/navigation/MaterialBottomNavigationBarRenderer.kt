package com.Sunset.REN.GitHub.ui.navigation

import android.content.res.Resources
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomnavigation.BottomNavigationView

class MaterialBottomNavigationBarRenderer(
    container: ViewGroup,
    navView: BottomNavigationView,
    divider: View,
    navHostViewProvider: () -> ViewGroup?,
    resources: Resources
) : NavigationBarRenderer {
    private val navigationBarController = NavigationBarController(
        container = container,
        navView = navView,
        divider = divider,
        navHostViewProvider = navHostViewProvider,
        resources = resources
    )

    override fun render(state: NavigationBarRenderState) {
        navigationBarController.render(state)
    }

    override fun updateSystemNavigationBottomInset(insetPx: Int) {
        navigationBarController.updateSystemNavigationBottomInset(insetPx)
    }
}