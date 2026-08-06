package com.Sunset.REN.GitHub.ui.repo

import android.text.Selection
import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.view.MotionEvent
import android.widget.TextView

/**
 * Markwon 渲染 README 后仍会生成 URLSpan。该 MovementMethod 在点击链接时先给业务层一次拦截机会，
 * 拦截成功则留在 App 内自回环导航；未识别链接继续走 URLSpan 默认行为（浏览器/外部 App）。
 */
class RepositoryMarkdownLinkMovementMethod(
    private val onUrlClick: (String) -> Boolean
) : LinkMovementMethod() {
    override fun onTouchEvent(widget: TextView, buffer: Spannable, event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_UP) {
            return super.onTouchEvent(widget, buffer, event)
        }
        var x = event.x.toInt() - widget.totalPaddingLeft + widget.scrollX
        var y = event.y.toInt() - widget.totalPaddingTop + widget.scrollY
        val layout = widget.layout ?: return super.onTouchEvent(widget, buffer, event)
        val line = layout.getLineForVertical(y)
        x = x.coerceAtLeast(0)
        y = y.coerceAtLeast(0)
        val offset = layout.getOffsetForHorizontal(line, x.toFloat())
        val spans = buffer.getSpans(offset, offset, URLSpan::class.java)
        val span = spans.firstOrNull() ?: return super.onTouchEvent(widget, buffer, event)
        if (onUrlClick(span.url)) {
            Selection.removeSelection(buffer)
            return true
        }
        return super.onTouchEvent(widget, buffer, event)
    }
}