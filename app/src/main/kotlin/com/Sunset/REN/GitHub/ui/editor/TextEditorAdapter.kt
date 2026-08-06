package com.Sunset.REN.GitHub.ui.editor

/**
 * 项目自有文本编辑器抽象。
 *
 * 业务层只能依赖该接口，不能直接绑定 EditText、sora-editor 或其他具体编辑器内核。
 */
interface TextEditorAdapter {
    fun setText(content: String)
    fun getText(): String
    fun setReadOnly(enabled: Boolean)
    fun setLanguageMode(mode: String?)
    fun setTheme(theme: String?)
    fun setSelection(start: Int, end: Int)
    fun getSelection(): TextSelection
    fun setOnTextChangedListener(listener: ((String) -> Unit)?)
    fun setOnSelectionChangedListener(listener: ((TextSelection) -> Unit)?)
    fun scrollToTop()
    fun scrollToSelectionEnd()
    fun focus()
}

data class TextSelection(
    val start: Int,
    val end: Int
)

/**
 * 可选能力接口：搜索替换。
 * 第一阶段只预留能力位，具体实现可由编辑器内核决定。
 */
interface SearchReplaceCapableEditor {
    fun find(query: String, ignoreCase: Boolean = false): Boolean
    fun replaceCurrent(replacement: String): Boolean
}

/**
 * 可选能力接口：撤销重做。
 */
interface UndoRedoCapableEditor {
    fun undo(): Boolean
    fun redo(): Boolean
}

/**
 * 可选能力接口：编辑器显示调节。
 */
interface DisplayTunableEditor {
    fun setTextSizeSp(sizeSp: Float)
    fun setSoftWrap(enabled: Boolean)
}
