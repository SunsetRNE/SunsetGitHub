package com.Sunset.REN.GitHub.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView
import com.Sunset.REN.GitHub.R

/** Text-based segmented option button with selected state baked into the component. */
class SunsetSegmentButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    init {
        isClickable = true
        isFocusable = true
        gravity = Gravity.CENTER
        minimumHeight = dpToPx(DefaultMinHeightDp)
        refreshSegmentAppearance()
    }

    override fun setSelected(selected: Boolean) {
        if (isSelected == selected) return
        super.setSelected(selected)
        refreshSegmentAppearance()
    }

    private fun refreshSegmentAppearance() {
        setBackgroundResource(
            if (isSelected) {
                R.drawable.bg_repository_release_type_selected
            } else {
                R.drawable.bg_repository_release_type_unselected
            }
        )
        setTextColor(context.getColor(if (isSelected) R.color.github_success else R.color.github_text_secondary))
        setTypeface(null, if (isSelected) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
    }

    private fun dpToPx(valueDp: Int): Int = (valueDp * resources.displayMetrics.density).toInt()

    private companion object {
        private const val DefaultMinHeightDp = 48
    }
}