package com.Sunset.REN.GitHub.ui.editor

import android.graphics.Typeface
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.ContentListener
import io.github.rosemoe.sora.text.Cursor
import io.github.rosemoe.sora.text.Indexer
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.SchemeDarcula
import io.github.rosemoe.sora.widget.schemes.SchemeGitHub

class SoraTextEditorAdapter(
    private val editor: CodeEditor
) : TextEditorAdapter,
    SearchReplaceCapableEditor,
    UndoRedoCapableEditor,
    DisplayTunableEditor,
    ReleasableTextEditor {
    private var onTextChanged: ((String) -> Unit)? = null
    private var onSelectionChanged: ((TextSelection) -> Unit)? = null
    private var lastSelection: TextSelection = TextSelection(0, 0)
    private var languageMode: String? = null
    private var theme: String? = null
    private val contentListener = object : ContentListener {
        override fun beforeReplace(content: Content) = Unit
        override fun afterInsert(
            content: Content,
            startLine: Int,
            startColumn: Int,
            endLine: Int,
            endColumn: Int,
            insertedContent: CharSequence
        ) {
            onTextChanged?.invoke(content.toString())
            notifySelectionChangedIfNeeded()
        }

        override fun afterDelete(
            content: Content,
            startLine: Int,
            startColumn: Int,
            endLine: Int,
            endColumn: Int,
            deletedContent: CharSequence
        ) {
            onTextChanged?.invoke(content.toString())
            notifySelectionChangedIfNeeded()
        }
    }

    init {
        editor.setTypefaceText(Typeface.MONOSPACE)
        lastSelection = getSelection()
        editor.getText().addContentListener(contentListener)
    }

    override fun setText(content: String) {
        if (getText() != content) {
            editor.setText(content)
        }
    }

    override fun getText(): String {
        return editor.getText().toString()
    }

    override fun setReadOnly(enabled: Boolean) {
        editor.setEditable(!enabled)
        editor.isFocusable = !enabled
        editor.isFocusableInTouchMode = !enabled
    }

    override fun setLanguageMode(mode: String?) {
        languageMode = mode?.lowercase()
        editor.setEditorLanguage(TextEditorLanguageRegistry.resolve(editor.context, languageMode))
        applyTextMateThemeIfNeeded()
    }

    private fun applyTextMateThemeIfNeeded() {
        if (!TextMateLanguageProvider.supports(languageMode)) {
            applyBuiltInTheme()
            return
        }
        if (editor.colorScheme !is TextMateColorScheme) {
            editor.colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
        }
    }

    private fun applyBuiltInTheme() {
        editor.colorScheme = when (theme) {
            "dark", "darcula", "night" -> SchemeDarcula()
            else -> SchemeGitHub()
        }
        editor.isWordwrap = editor.isWordwrap
        editor.isVerticalScrollBarEnabled = true
        editor.isHorizontalScrollBarEnabled = editor.isHorizontalScrollBarEnabled
        editor.setTextSize(14f)
    }
    override fun setTheme(theme: String?) {
        this.theme = theme?.lowercase()
        applyTextMateThemeIfNeeded()
    }

    override fun find(query: String, ignoreCase: Boolean): Boolean {
        if (query.isBlank()) {
            editor.searcher.stopSearch()
            return false
        }
        return runCatching {
            editor.searcher.search(query, io.github.rosemoe.sora.widget.EditorSearcher.SearchOptions(ignoreCase, false))
            val matched = editor.searcher.gotoNext()
            notifySelectionChangedIfNeeded()
            matched
        }.getOrDefault(false)
    }

    override fun replaceCurrent(replacement: String): Boolean {
        if (!editor.searcher.hasQuery()) return false
        return runCatching {
            editor.searcher.replaceCurrentMatch(replacement)
            notifySelectionChangedIfNeeded()
            true
        }.getOrDefault(false)
    }

    override fun undo(): Boolean {
        if (!editor.canUndo()) return false
        editor.undo()
        notifySelectionChangedIfNeeded()
        return true
    }

    override fun redo(): Boolean {
        if (!editor.canRedo()) return false
        editor.redo()
        notifySelectionChangedIfNeeded()
        return true
    }


    override fun setSelection(start: Int, end: Int) {
        val content = editor.getText()
        val safeStart = start.coerceIn(0, content.length)
        val safeEnd = end.coerceIn(0, content.length)

        val indexer: Indexer = content.getIndexer()
        val startPosition = indexer.getCharPosition(safeStart)
        val endPosition = indexer.getCharPosition(safeEnd)
        editor.setSelectionRegion(
            startPosition.line,
            startPosition.column,
            endPosition.line,
            endPosition.column
        )
        notifySelectionChangedIfNeeded()
    }

    override fun getSelection(): TextSelection {
        val cursor: Cursor = editor.getCursor()
        val indexer: Indexer = editor.getText().getIndexer()
        return TextSelection(
            start = indexer.getCharIndex(cursor.getLeftLine(), cursor.getLeftColumn()),
            end = indexer.getCharIndex(cursor.getRightLine(), cursor.getRightColumn())
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
            onSelectionChanged?.invoke(selection)
        }
    }

    override fun scrollToTop() {
        editor.post {
            editor.setSelection(0, 0)
            editor.ensurePositionVisible(0, 0)
        }
    }

    override fun scrollToSelectionEnd() {
        editor.post {
            val length = editor.getText().length
            setSelection(length, length)
            editor.ensureSelectionVisible()
        }
    }

    override fun focus() {
        editor.requestFocus()
    }

    override fun setTextSizeSp(sizeSp: Float) {
        editor.setTextSize(sizeSp)
    }

    override fun setSoftWrap(enabled: Boolean) {
        editor.isHorizontalScrollBarEnabled = !enabled
        editor.setWordwrap(enabled, false)
    }

    override fun release() {
        editor.getText().removeContentListener(contentListener)
        editor.release()
    }
}