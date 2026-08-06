package com.Sunset.REN.GitHub.ui.components

import android.content.Context
import android.util.AttributeSet
import com.Sunset.REN.GitHub.R
import com.google.android.material.chip.Chip

/** Project-owned filter chip so dynamic screens do not hand-style Material Chip directly. */
class SunsetFilterChip @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.chipStyle
) : Chip(context, attrs, defStyleAttr) {

    init {
        isCheckable = true
        setEnsureMinTouchTargetSize(false)
        chipBackgroundColor = context.getColorStateList(R.color.sunset_filter_chip_background_tint)
        chipStrokeColor = context.getColorStateList(R.color.sunset_filter_chip_stroke_tint)
        chipStrokeWidth = resources.displayMetrics.density
        setTextColor(context.getColorStateList(R.color.sunset_filter_chip_text_tint))
        checkedIcon = null
        isCheckedIconVisible = false
        closeIcon = null
        isCloseIconVisible = false
        minimumHeight = dpToPx(DefaultMinHeightDp)
    }

    private fun dpToPx(valueDp: Int): Int = (valueDp * resources.displayMetrics.density).toInt()

    private companion object {
        private const val DefaultMinHeightDp = 32
    }
}