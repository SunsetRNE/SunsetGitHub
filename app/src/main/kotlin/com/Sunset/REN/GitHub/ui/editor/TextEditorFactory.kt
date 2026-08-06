package com.Sunset.REN.GitHub.ui.editor

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.Sunset.REN.GitHub.R
import io.github.rosemoe.sora.widget.CodeEditor

/**
 * 编辑器内核工厂。
 *
 * 根据设置创建 Sora CodeEditor 或 EditText fallback，并统一挂接语言、主题和显示配置。
 * 仓库编辑页、本地文件预览等业务层只依赖 TextEditorHost/TextEditorAdapter。
 */
object TextEditorFactory {

    fun create(
        inflater: LayoutInflater,
        parent: ViewGroup,
        config: TextEditorConfig = TextEditorConfig()
    ): TextEditorHost {
        val host = when (config.preferredEngine) {
            TextEditorEngine.EditText -> createEditTextHost(inflater, parent, TextEditorEngine.EditText, config.softWrap)
            TextEditorEngine.SoraFallback -> createEditTextHost(inflater, parent, TextEditorEngine.SoraFallback, config.softWrap)
            TextEditorEngine.Sora -> createSoraHost(parent, config.softWrap)
        }
        host.adapter.setLanguageMode(config.languageMode)
        host.adapter.setTheme(config.theme)
        return host
    }

    private fun createEditTextHost(
        inflater: LayoutInflater,
        parent: ViewGroup,
        engine: TextEditorEngine,
        softWrap: Boolean
    ): TextEditorHost {
        val root = inflater.inflate(R.layout.view_text_editor_edit_text, parent, false)
        val editText = root.findViewById<EditText>(R.id.editor_edit_text)
        val lineNumbers = root.findViewById<TextView>(R.id.editor_line_numbers)
        editText.setHorizontallyScrolling(!softWrap)
        editText.isHorizontalScrollBarEnabled = !softWrap
        return TextEditorHost(
            rootView = root,
            adapter = EditTextEditorAdapter(editText, lineNumbers),
            engine = engine
        )
    }

    private fun createSoraHost(parent: ViewGroup, softWrap: Boolean): TextEditorHost {
        val editor = CodeEditor(parent.context)
        editor.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        editor.isHorizontalScrollBarEnabled = !softWrap
        editor.setTextSize(14f)
        editor.setWordwrap(softWrap, false)
        return TextEditorHost(
            rootView = editor,
            adapter = SoraTextEditorAdapter(editor),
            engine = TextEditorEngine.Sora
        )
    }
}