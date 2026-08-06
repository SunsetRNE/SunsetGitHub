package com.Sunset.REN.GitHub.ui.navigation

import android.content.res.Resources
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.WeakHashMap

class NavigationBarController(
    private val container: ViewGroup,
    private val navView: BottomNavigationView,
    private val divider: View,
    private val navHostViewProvider: () -> ViewGroup?,
    private val resources: Resources
) {
    private val basePaddings = WeakHashMap<View, PaddingSnapshot>()
    private var currentMode: NavigationBarMode = NavigationBarMode.Hidden
    private var currentAppearance: NavigationBarAppearanceSpec = NavigationBarAppearanceRegistry.resolveAppearance(NavigationBarMode.Hidden)
    private var systemNavigationBottomInsetPx: Int = 0
    private var previousInsetTarget: View? = null
    fun render(state: NavigationBarRenderState) {
        val mode = state.mode
        val appearance = state.appearance
        val targetVisibility = if (mode == NavigationBarMode.Hidden) View.GONE else View.VISIBLE

        container.visibility = targetVisibility
        container.isEnabled = mode != NavigationBarMode.Hidden
        container.isClickable = mode != NavigationBarMode.Hidden
        navView.visibility = targetVisibility
        navView.isEnabled = mode != NavigationBarMode.Hidden
        navView.isClickable = mode != NavigationBarMode.Hidden
        divider.visibility = if (appearance.isDividerVisible) View.VISIBLE else View.GONE
        divider.isEnabled = mode != NavigationBarMode.Hidden
        currentMode = mode
        currentAppearance = appearance
        applyAppearance(appearance)

        // Apply once for the current destination root and once after the navigation surface has
        // settled; floating mode changes margin/padding and its measured height can lag one frame.
        applyContentBottomInset()
        container.post { applyContentBottomInset() }
    }

    fun render(mode: NavigationBarMode) {
        render(NavigationBarRenderState.forMode(mode))
    }

    fun updateSystemNavigationBottomInset(insetPx: Int) {
        val normalizedInsetPx = insetPx.coerceAtLeast(0)
        if (systemNavigationBottomInsetPx == normalizedInsetPx) return
        systemNavigationBottomInsetPx = normalizedInsetPx
        applyAppearance(currentAppearance)
        applyContentBottomInset()
        container.post { applyContentBottomInset() }
    }

    /**
     * Docked mode reserves layout space for the bottom bar.
     * Floating mode overlays the pill above the destination content and only adds scroll padding,
     * so the floating pill is the only visible navigation surface instead of a full-width white band.
     */
    private fun applyContentBottomInset() {
        val navHostView = navHostViewProvider() ?: return
        constrainNavHostAboveNavigationSurface(navHostView)

        navHostView.post {
            val contentRoot = navHostView.getChildAt(0) ?: return@post
            if (contentRoot is ViewGroup) {
                contentRoot.clipToPadding = false
            }
            val insetTarget = findPrimaryVerticalInsetTarget(contentRoot)
            if (insetTarget == null) {
                previousInsetTarget?.let { previousTarget -> applyBottomPaddingInset(previousTarget, 0) }
                previousInsetTarget = null
                return@post
            }
            previousInsetTarget?.takeIf { it !== insetTarget }?.let { previousTarget ->
                applyBottomPaddingInset(previousTarget, 0)
            }
            previousInsetTarget = insetTarget
            applyBottomPaddingInset(insetTarget, contentBottomInsetPx())
        }
    }

    private fun contentBottomInsetPx(): Int {
        if (currentMode != NavigationBarMode.FloatingReserved || container.visibility != View.VISIBLE) return 0
        val measuredHeight = container.measuredHeight.takeIf { it > 0 } ?: container.minimumHeight
        val bottomMargin = (container.layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin ?: 0
        return measuredHeight + bottomMargin + dpToPx(FloatingNavContentGapDp)
    }

    private fun constrainNavHostAboveNavigationSurface(navHostView: ViewGroup) {
        val layoutParams = navHostView.layoutParams
        if (layoutParams is ConstraintLayout.LayoutParams) {
            val reservesNavigationSpace = currentMode == NavigationBarMode.Docked
            val targetBottomToTop = if (reservesNavigationSpace) {
                container.id
            } else {
                ConstraintLayout.LayoutParams.UNSET
            }
            val targetBottomToBottom = if (reservesNavigationSpace) {
                ConstraintLayout.LayoutParams.UNSET
            } else {
                ConstraintLayout.LayoutParams.PARENT_ID
            }
            val targetBottomMargin = 0
            if (
                layoutParams.bottomToTop != targetBottomToTop ||
                layoutParams.bottomToBottom != targetBottomToBottom ||
                layoutParams.bottomMargin != targetBottomMargin
            ) {
                layoutParams.bottomToTop = targetBottomToTop
                layoutParams.bottomToBottom = targetBottomToBottom
                layoutParams.bottomMargin = targetBottomMargin
                navHostView.layoutParams = layoutParams
            }
            return
        }

        val targetBottomMargin = navHostBottomMarginPx()
        if (layoutParams is ViewGroup.MarginLayoutParams && layoutParams.bottomMargin != targetBottomMargin) {
            layoutParams.bottomMargin = targetBottomMargin
            navHostView.layoutParams = layoutParams
        }
    }

    private fun navHostBottomMarginPx(): Int {
        if (currentMode != NavigationBarMode.Docked || container.visibility != View.VISIBLE) return 0
        val measuredHeight = container.measuredHeight.takeIf { it > 0 } ?: container.minimumHeight
        val bottomMargin = (container.layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin ?: 0
        return measuredHeight + bottomMargin
    }

    private fun findPrimaryVerticalInsetTarget(root: View): View? {
        if (root is RecyclerView || root is ScrollView || root is NestedScrollView) return root
        if (root !is ViewGroup) return null

        var fallbackScrollable: View? = null
        for (index in 0 until root.childCount) {
            val child = root.getChildAt(index)
            val nestedTarget = findPrimaryVerticalInsetTarget(child) ?: continue
            if (nestedTarget.layoutParams?.height == 0) return nestedTarget
            fallbackScrollable = fallbackScrollable ?: nestedTarget
        }
        return fallbackScrollable
    }

    private fun applyBottomPaddingInset(view: View, bottomInset: Int) {
        val basePadding = basePaddings.getOrPut(view) {
            PaddingSnapshot(
                left = view.paddingLeft,
                top = view.paddingTop,
                right = view.paddingRight,
                bottom = view.paddingBottom
            )
        }
        val targetBottomPadding = basePadding.bottom + bottomInset
        if (
            view.paddingLeft != basePadding.left ||
            view.paddingTop != basePadding.top ||
            view.paddingRight != basePadding.right ||
            view.paddingBottom != targetBottomPadding
        ) {
            view.setPadding(
                basePadding.left,
                basePadding.top,
                basePadding.right,
                targetBottomPadding
            )
        }
        if (view is ViewGroup) {
            view.clipToPadding = false
        }
    }


    private fun applyAppearance(appearance: NavigationBarAppearanceSpec) {
        // Keep the floating pill on the outer container. In floating mode this outer pill is
        // also the hard visual bounds for the Material navigation child.
        container.setBackgroundResource(appearance.containerBackgroundResId)
        container.elevation = dpToPxFloat(appearance.elevationDp)
        val isFloating = appearance.clipToOutline
        container.clipToOutline = isFloating
        // In floating mode the pill is the visual boundary. Clip the Material navigation child to
        // that boundary instead of letting BottomNavigationView's wrap_content measurement expand
        // the white floating surface beyond the intended compact pill.
        container.clipChildren = isFloating
        container.clipToPadding = !isFloating
        navView.backgroundTintList = null
        navView.setBackgroundResource(appearance.navigationBackgroundResId)
        navView.elevation = 0f
        navView.clipToOutline = false
        navView.clipToPadding = isFloating

        val horizontalPadding = if (isFloating) dpToPx(FloatingNavInnerHorizontalPaddingDp) else 0
        val verticalPadding = if (isFloating) dpToPx(FloatingNavInnerVerticalPaddingDp) else 0
        if (
            container.paddingLeft != horizontalPadding ||
            container.paddingRight != horizontalPadding ||
            container.paddingTop != verticalPadding ||
            container.paddingBottom != verticalPadding
        ) {
            container.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
        }
        val navigationBarHeight = dpToPx(NavigationBarMinHeightDp)
        val containerHeight = navigationBarHeight + verticalPadding * 2
        navView.minimumHeight = navigationBarHeight
        container.minimumHeight = containerHeight

        container.layoutParams = container.layoutParams.apply {
            height = if (isFloating) containerHeight else ViewGroup.LayoutParams.WRAP_CONTENT
            if (this is ViewGroup.MarginLayoutParams) {
                bottomMargin = dpToPx(appearance.bottomMarginDp) + if (isFloating) systemNavigationBottomInsetPx else 0
                marginStart = dpToPx(appearance.sideMarginDp)
                marginEnd = dpToPx(appearance.sideMarginDp)
            }
        }
        navView.layoutParams = navView.layoutParams.apply {
            height = if (isFloating) navigationBarHeight else ViewGroup.LayoutParams.WRAP_CONTENT
            if (this is ViewGroup.MarginLayoutParams) {
                bottomMargin = 0
                marginStart = 0
                marginEnd = 0
            }
        }
        divider.layoutParams = divider.layoutParams.apply {
            if (this is ViewGroup.MarginLayoutParams) {
                bottomMargin = 0
                marginStart = 0
                marginEnd = 0
            }
        }
        container.requestLayout()
        navView.requestLayout()
    }

    private data class PaddingSnapshot(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int
    )
    private companion object {
        const val NavigationBarMinHeightDp = 56
        const val FloatingNavInnerHorizontalPaddingDp = 6
        const val FloatingNavInnerVerticalPaddingDp = 0
        const val FloatingNavContentGapDp = 10
    }


    private fun dpToPx(valueDp: Int): Int {
        return (valueDp * resources.displayMetrics.density).toInt()
    }

    private fun dpToPxFloat(valueDp: Float): Float {
        return valueDp * resources.displayMetrics.density
    }
}
