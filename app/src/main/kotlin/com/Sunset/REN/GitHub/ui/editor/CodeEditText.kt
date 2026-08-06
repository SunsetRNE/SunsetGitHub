package com.Sunset.REN.GitHub.ui.editor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText

/**
 * Lightweight fallback code editor view.
 *
 * Sora remains the preferred code editor. This view only upgrades the EditText fallback
 * so it does not look like a generic multi-line input when Sora is unavailable.
 */
class CodeEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {

    private val currentLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = resolveCurrentLineColor()
        style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        drawCurrentLineHighlight(canvas)
        super.onDraw(canvas)
    }

    private fun drawCurrentLineHighlight(canvas: Canvas) {
        val layout = layout ?: return
        if (!isFocused || selectionStart < 0) return
        val line = layout.getLineForOffset(selectionStart.coerceAtMost(text?.length ?: 0))
        val top = totalPaddingTop + layout.getLineTop(line)
        val bottom = totalPaddingTop + layout.getLineBottom(line)
        canvas.drawRect(
            scrollX.toFloat(),
            top.toFloat(),
            (scrollX + width).toFloat(),
            bottom.toFloat(),
            currentLinePaint
        )
    }

    private fun resolveCurrentLineColor(): Int {
        val base = Color.parseColor("#0969DA")
        return Color.argb(18, Color.red(base), Color.green(base), Color.blue(base))
    }

}
