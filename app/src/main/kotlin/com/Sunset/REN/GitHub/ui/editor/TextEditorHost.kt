package com.Sunset.REN.GitHub.ui.editor

import android.view.View

/**
 * 编辑器内核宿主。
 *
 * 业务页面通过这个宿主持有编辑器根 View 与统一 adapter，不直接依赖
 * EditText 或 Sora CodeEditor。Compose 页面应通过 TextEditorHostView 嵌入它。
 */
data class TextEditorHost(
    val rootView: View,
    val adapter: TextEditorAdapter,
    val engine: TextEditorEngine
)

enum class TextEditorEngine {
    EditText,
    SoraFallback,
    Sora
}