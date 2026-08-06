package com.Sunset.REN.GitHub.ui.debug

import android.content.res.Configuration
import android.graphics.Rect
import java.util.Locale

data class UiRenderSnapshot(
    val destinationIdName: String = "unknown",
    val destinationLabel: String = "",
    val fragmentClassName: String = "unknown",
    val screenWidthPx: Int = 0,
    val screenHeightPx: Int = 0,
    val density: Float = 0f,
    val fontScale: Float = 1f,
    val isNightMode: Boolean = false,
    val statusBarInsets: Rect = Rect(),
    val navigationBarInsets: Rect = Rect(),
    val rootHeightPx: Int = 0,
    val navHostTopPx: Int = 0,
    val navHostBottomPx: Int = 0,
    val navHostHeightPx: Int = 0,
    val navContainerTopPx: Int = 0,
    val navContainerBottomPx: Int = 0,
    val navContainerHeightPx: Int = 0,
    val navContainerVisibility: String = "unknown"
) {
    fun compactText(): String {
        return buildString {
            append("UI ")
            append(destinationIdName)
            if (destinationLabel.isNotBlank()) {
                append(" / ")
                append(destinationLabel)
            }
            append('\n')
            append(fragmentClassName.substringAfterLast('.'))
            append('\n')
            append(screenWidthPx)
            append('x')
            append(screenHeightPx)
            append(" dp=")
            append(String.format(Locale.US, "%.2f", density))
            append(" fs=")
            append(String.format(Locale.US, "%.2f", fontScale))
            append(if (isNightMode) " night" else " day")
            append('\n')
            append("status=")
            append(statusBarInsets.toShortString())
            append(" nav=")
            append(navigationBarInsets.toShortString())
            append('\n')
            append("rootH=")
            append(rootHeightPx)
            append(" host=")
            append(navHostTopPx)
            append("..")
            append(navHostBottomPx)
            append(" h=")
            append(navHostHeightPx)
            append('\n')
            append("bar=")
            append(navContainerTopPx)
            append("..")
            append(navContainerBottomPx)
            append(" h=")
            append(navContainerHeightPx)
            append(" gap=")
            append(navContainerTopPx - navHostBottomPx)
            append(' ')
            append(navContainerVisibility)
        }
    }

    fun logText(): String {
        return "destination=$destinationIdName label=$destinationLabel fragment=$fragmentClassName " +
            "screen=${screenWidthPx}x$screenHeightPx density=$density fontScale=$fontScale " +
            "nightMode=$isNightMode statusInsets=${statusBarInsets.toShortString()} " +
            "navigationInsets=${navigationBarInsets.toShortString()} " +
            "rootHeight=$rootHeightPx navHost=$navHostTopPx..$navHostBottomPx/$navHostHeightPx " +
            "navContainer=$navContainerTopPx..$navContainerBottomPx/$navContainerHeightPx " +
            "navContainerVisibility=$navContainerVisibility gap=${navContainerTopPx - navHostBottomPx}"
    }

    companion object {
        fun isNightMode(configuration: Configuration): Boolean {
            return configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        }
    }
}