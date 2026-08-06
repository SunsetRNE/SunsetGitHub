package com.Sunset.REN.GitHub.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import com.Sunset.REN.GitHub.R

enum class NavigationBarMode {
    Hidden,
    Docked,
    FloatingReserved
}

data class NavigationBarDestinationConfig(
    @IdRes val destinationId: Int,
    val mode: NavigationBarMode,
    val showWhenSignedOut: Boolean = false
)

data class NavigationBarAppearanceSpec(
    @DrawableRes val containerBackgroundResId: Int,
    @DrawableRes val navigationBackgroundResId: Int,
    val elevationDp: Float,
    val bottomMarginDp: Int,
    val sideMarginDp: Int,
    val clipToOutline: Boolean,
    val isDividerVisible: Boolean
)

data class NavigationBarRenderState(
    val mode: NavigationBarMode,
    val appearance: NavigationBarAppearanceSpec
) {
    companion object {
        fun forMode(mode: NavigationBarMode): NavigationBarRenderState {
            return NavigationBarRenderState(
                mode = mode,
                appearance = NavigationBarAppearanceRegistry.resolveAppearance(mode)
            )
        }

        fun hidden(): NavigationBarRenderState = forMode(NavigationBarMode.Hidden)
    }
}

object NavigationBarDestinationRegistry {

    fun resolveMode(
        destinationId: Int?,
        isAuthorized: Boolean,
        isFloatingNavigationEnabled: Boolean,
        forceHidden: Boolean = false
    ): NavigationBarMode {
        return NavigationDestinationRegistry.resolveMode(
            destinationId = destinationId,
            isAuthorized = isAuthorized,
            isFloatingNavigationEnabled = isFloatingNavigationEnabled,
            forceHidden = forceHidden
        )
    }

    fun resolveRenderState(
        destinationId: Int?,
        isAuthorized: Boolean,
        isFloatingNavigationEnabled: Boolean,
        forceHidden: Boolean = false
    ): NavigationBarRenderState {
        return NavigationDestinationRegistry.resolveRenderState(
            destinationId = destinationId,
            isAuthorized = isAuthorized,
            isFloatingNavigationEnabled = isFloatingNavigationEnabled,
            forceHidden = forceHidden
        )
    }
}

object NavigationBarAppearanceRegistry {

    fun resolveAppearance(mode: NavigationBarMode): NavigationBarAppearanceSpec {
        return when (mode) {
            NavigationBarMode.Hidden -> NavigationBarAppearanceSpec(
                containerBackgroundResId = R.drawable.bg_bottom_nav_transparent,
                navigationBackgroundResId = R.drawable.bg_bottom_nav_transparent,
                elevationDp = 0f,
                bottomMarginDp = 0,
                sideMarginDp = 0,
                clipToOutline = false,
                isDividerVisible = false
            )

            NavigationBarMode.Docked -> NavigationBarAppearanceSpec(
                containerBackgroundResId = R.drawable.bg_bottom_nav_docked,
                navigationBackgroundResId = R.drawable.bg_bottom_nav_transparent,
                elevationDp = 0f,
                bottomMarginDp = 0,
                sideMarginDp = 0,
                clipToOutline = false,
                isDividerVisible = true
            )

            NavigationBarMode.FloatingReserved -> NavigationBarAppearanceSpec(
                containerBackgroundResId = R.drawable.bg_bottom_nav_floating,
                navigationBackgroundResId = R.drawable.bg_bottom_nav_transparent,
                elevationDp = 10f,
                bottomMarginDp = FloatingNavBottomMarginDp,
                sideMarginDp = FloatingNavSideMarginDp,
                clipToOutline = true,
                isDividerVisible = false
            )
        }
    }

    private const val FloatingNavBottomMarginDp = 8
    private const val FloatingNavSideMarginDp = 24
}
