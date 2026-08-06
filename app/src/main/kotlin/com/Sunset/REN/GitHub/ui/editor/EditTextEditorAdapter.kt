package com.Sunset.REN.GitHub.ui.editor

import android.graphics.Color
import android.graphics.Typeface
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.text.method.KeyListener
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.TypedValue
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.Sunset.REN.GitHub.R

class EditTextEditorAdapter(
    private val editText: EditText,
    private val lineNumbers: TextView? = null
) : TextEditorAdapter, DisplayTunableEditor {
    private val originalKeyListener: KeyListener? = editText.keyListener
    private var onTextChanged: ((String) -> Unit)? = null
    private var onSelectionChanged: ((TextSelection) -> Unit)? = null
    private var lastSelection: TextSelection = TextSelection(0, 0)
    private var isDarkTheme: Boolean = true

    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit

        override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
            val content = text?.toString().orEmpty()
            updateLineNumbers(content, currentLineNumber())
            onTextChanged?.invoke(content)
            notifySelectionChangedIfNeeded()
        }

        override fun afterTextChanged(text: Editable?) = Unit
    }

    init {
        lastSelection = getSelection()
        applyThemeColors()
        updateLineNumbers(getText(), currentLineNumber())
        syncLineNumberTextSize()
        editText.addTextChangedListener(textWatcher)
        editText.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            lineNumbers?.scrollTo(0, scrollY)
        }
        editText.setOnTouchListener { view, event ->
            val handled = view.performClick().let { false }
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                editText.post { notifySelectionChangedIfNeeded() }
            }
            handled
        }
        editText.setOnKeyListener { _, keyCode, event ->
            val shouldCheck = event.action == KeyEvent.ACTION_UP && keyCode in selectionChangingKeys
            if (shouldCheck) {
                editText.post { notifySelectionChangedIfNeeded() }
            }
            false
        }
    }

    override fun setText(content: String) {
        if (editText.text?.toString() != content) {
            editText.setText(content)
        }
        updateLineNumbers(content, currentLineNumber())
    }

    override fun getText(): String {
        return editText.text?.toString().orEmpty()
    }

    override fun setReadOnly(enabled: Boolean) {
        editText.isEnabled = true
        editText.keyListener = if (enabled) null else originalKeyListener
        editText.isFocusable = !enabled
        editText.isFocusableInTouchMode = !enabled
        editText.isCursorVisible = !enabled
        editText.isLongClickable = !enabled
        editText.setTextIsSelectable(enabled)
    }

    override fun setLanguageMode(mode: String?) = Unit

    override fun setTheme(theme: String?) {
        isDarkTheme = theme?.lowercase() in setOf("dark", "darcula", "night")
        applyThemeColors()
        updateLineNumbers(getText(), currentLineNumber())
    }

    override fun setSelection(start: Int, end: Int) {
        val textLength = editText.text?.length ?: 0
        val safeStart = start.coerceIn(0, textLength)
        val safeEnd = end.coerceIn(0, textLength)
        editText.setSelection(safeStart, safeEnd)
        notifySelectionChangedIfNeeded()
    }

    override fun getSelection(): TextSelection {
        return TextSelection(
            start = editText.selectionStart.coerceAtLeast(0),
            end = editText.selectionEnd.coerceAtLeast(0)
        )
    }

    override fun setOnTextChangedListener(listener: ((String) -> Unit)?) {
        onTextChanged = listener
    }

    override fun setOnSelectionChangedListener(listener: ((TextSelection) -> Unit)?) {
        onSelectionChanged = listener
        notifySelectionChangedIfNeeded(force = true)
    }

    private fun notifySelectionChangedIfNeeded(force: Boolean = false) {
        val selection = getSelection()
        if (force || selection != lastSelection) {
            lastSelection = selection
            updateLineNumbers(getText(), currentLineNumber())
            editText.invalidate()
            onSelectionChanged?.invoke(selection)
        }
    }

    override fun scrollToTop() {
        editText.post {
            editText.scrollTo(0, 0)
            lineNumbers?.scrollTo(0, 0)
        }
    }

    override fun scrollToSelectionEnd() {
        editText.post {
            val lastLine = (editText.lineCount - 1).coerceAtLeast(0)
            val y = editText.layout?.getLineTop(lastLine) ?: 0
            editText.scrollTo(0, y)
            lineNumbers?.scrollTo(0, y)
        }
    }

    override fun focus() {
        editText.requestFocus()
    }

    override fun setTextSizeSp(sizeSp: Float) {
        editText.textSize = sizeSp
        lineNumbers?.textSize = sizeSp
    }

    override fun setSoftWrap(enabled: Boolean) {
        editText.setHorizontallyScrolling(!enabled)
        editText.isHorizontalScrollBarEnabled = !enabled
    }

    private fun applyThemeColors() {
        if (isDarkTheme) {
            editText.setTextColor(Color.parseColor("#E6EDF3"))
            editText.setBackgroundColor(ContextCompat.getColor(editText.context, R.color.github_black))
            lineNumbers?.setTextColor(Color.parseColor("#99FFFFFF"))
            lineNumbers?.setBackgroundColor(ContextCompat.getColor(editText.context, R.color.github_black_soft))
        } else {
            editText.setTextColor(ContextCompat.getColor(editText.context, R.color.github_text_primary))
            editText.setBackgroundColor(ContextCompat.getColor(editText.context, R.color.github_canvas))
            lineNumbers?.setTextColor(ContextCompat.getColor(editText.context, R.color.github_text_muted))
            lineNumbers?.setBackgroundColor(Color.parseColor("#FFF6F8FA"))
        }
    }

    private fun updateLineNumbers(content: String, activeLine: Int = currentLineNumber()) {
        val lineCount = content.count { it == '\n' } + 1
        val width = lineCount.toString().length.coerceAtLeast(2)
        val text = (1..lineCount).joinToString(separator = "\n") { line ->
            line.toString().padStart(width)
        }
        val lineNumberView = lineNumbers ?: return
        val spannable = SpannableString(text)
        if (activeLine in 1..lineCount) {
            val start = ((activeLine - 1) * (width + 1)).coerceIn(0, text.length)
            val end = (start + width).coerceIn(start, text.length)
            val activeColor = if (isDarkTheme) {
                Color.parseColor("#E6EDF3")
            } else {
                ContextCompat.getColor(editText.context, R.color.github_text_primary)
            }
            spannable.setSpan(ForegroundColorSpan(activeColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        lineNumberView.text = spannable
    }

    private fun currentLineNumber(): Int {
        val content = getText()
        val cursor = editText.selectionStart.coerceAtLeast(0).coerceAtMost(content.length)
        return content.take(cursor).count { it == '\n' } + 1
    }

    private fun syncLineNumberTextSize() {
        lineNumbers?.setTextSize(TypedValue.COMPLEX_UNIT_PX, editText.textSize)
    }

    companion object {
        private val selectionChangingKeys = setOf(
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_MOVE_HOME,
            KeyEvent.KEYCODE_MOVE_END,
            KeyEvent.KEYCODE_PAGE_UP,
            KeyEvent.KEYCODE_PAGE_DOWN
        )
    }
}