package com.Sunset.REN.GitHub.ui.navigation

interface NavigationBarRenderer {
    fun render(state: NavigationBarRenderState)

    fun updateSystemNavigationBottomInset(insetPx: Int)
}