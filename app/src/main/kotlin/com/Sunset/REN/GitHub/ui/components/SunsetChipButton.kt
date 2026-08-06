package com.Sunset.REN.GitHub.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView
import com.Sunset.REN.GitHub.R

/** Compact pill-shaped text button used by picker entries and inline tool actions. */
class SunsetChipButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    init {
        if (background == null) {
            setBackgroundResource(R.drawable.bg_repository_release_markdown_chip)
        }
        isClickable = true
        isFocusable = true
        gravity = Gravity.CENTER
        minimumHeight = dpToPx(DefaultMinHeightDp)
        if (paddingStart == 0 && paddingEnd == 0) {
            setPadding(dpToPx(DefaultHorizontalPaddingDp), paddingTop, dpToPx(DefaultHorizontalPaddingDp), paddingBottom)
        }
    }

    private fun dpToPx(valueDp: Int): Int = (valueDp * resources.displayMetrics.density).toInt()

    private companion object {
        private const val DefaultMinHeightDp = 28
        private const val DefaultHorizontalPaddingDp = 10
    }
}
