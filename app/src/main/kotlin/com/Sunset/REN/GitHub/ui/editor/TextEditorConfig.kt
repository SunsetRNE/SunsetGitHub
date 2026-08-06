package com.Sunset.REN.GitHub.ui.editor

data class TextEditorConfig(
    val preferredEngine: TextEditorEngine = TextEditorEngine.EditText,
    val languageMode: String? = null,
    val theme: String? = null,
    val softWrap: Boolean = false
)