package com.Sunset.REN.GitHub.ui.debug

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.doOnLayout
import com.Sunset.REN.GitHub.util.AppLogger

class UiDebugOverlay(
    private val root: ViewGroup,
    context: Context
) {

    private val textView = TextView(context).apply {
        setBackgroundColor(OverlayBackgroundColor)
        setTextColor(Color.WHITE)
        textSize = 10f
        typeface = Typeface.MONOSPACE
        includeFontPadding = false
        setPadding(dp(8), dp(6), dp(8), dp(6))
        isClickable = false
        isFocusable = false
        alpha = 0.88f
    }
    private var currentSnapshot = UiRenderSnapshot()

    fun attach() {
        if (textView.parent != null) return
        root.addView(
            textView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.END
            ).apply {
                topMargin = dp(8)
                marginEnd = dp(8)
            }
        )
        textView.doOnLayout { view -> view.bringToFront() }
        render()
    }

    fun detach() {
        (textView.parent as? ViewGroup)?.removeView(textView)
    }

    fun updateSnapshot(snapshot: UiRenderSnapshot) {
        currentSnapshot = snapshot
        render()
    }

    fun writeSnapshotToLog() {
        AppLogger.d(Tag, currentSnapshot.logText())
    }

    private fun render() {
        textView.visibility = View.VISIBLE
        textView.text = currentSnapshot.compactText()
        textView.bringToFront()
    }

    private fun dp(value: Int): Int {
        return (value * root.resources.displayMetrics.density).toInt()
    }

    private companion object {
        const val Tag = "UiDebugOverlay"
        const val OverlayBackgroundColor = 0xCC1F2328.toInt()
    }
}