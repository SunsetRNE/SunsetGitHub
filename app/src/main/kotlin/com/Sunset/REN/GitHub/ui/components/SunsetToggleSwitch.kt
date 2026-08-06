package com.Sunset.REN.GitHub.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.Sunset.REN.GitHub.R

/**
 * A project-owned on/off switch with the SunsetGitHub visual language baked in.
 *
 * The component intentionally uses the existing [R.drawable.bg_sunset_toggle]
 * selector and maps the logical checked state to View.selected, so XML and
 * programmatic usages get the same pill track and circular thumb shown in the
 * visual reference.
 */
class SunsetToggleSwitch @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var isChecked: Boolean
        get() = isSelected
        set(value) {
            setChecked(value, notify = true)
        }

    private var checkedChangeListener: ((Boolean) -> Unit)? = null

    init {
        setBackgroundResource(R.drawable.bg_sunset_toggle)
        isClickable = true
        isFocusable = true
        minimumWidth = dpToPx(DefaultWidthDp)
        minimumHeight = dpToPx(DefaultHeightDp)
    }

    override fun performClick(): Boolean {
        super.performClick()
        toggle()
        return true
    }

    fun toggle() {
        isChecked = !isChecked
    }

    fun setOnCheckedChangeListener(listener: ((Boolean) -> Unit)?) {
        checkedChangeListener = listener
    }

    fun setCheckedSilently(checked: Boolean) {
        setChecked(checked, notify = false)
    }

    private fun setChecked(checked: Boolean, notify: Boolean) {
        if (isSelected == checked) return
        isSelected = checked
        refreshDrawableState()
        if (notify) {
            checkedChangeListener?.invoke(checked)
        }
    }

    private fun dpToPx(valueDp: Int): Int {
        return (valueDp * resources.displayMetrics.density).toInt()
    }

    private companion object {
        private const val DefaultWidthDp = 44
        private const val DefaultHeightDp = 32
    }
}
