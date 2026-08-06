package com.Sunset.REN.GitHub.ui.filemanager

import android.content.Context
import com.Sunset.REN.GitHub.data.local.ThemePreferenceStore
import com.Sunset.REN.GitHub.ui.editor.TextEditorEngine

object LocalFileEditorConfigPolicy {
    fun resolvePreferredEditorEngine(context: Context): TextEditorEngine {
        return if (ThemePreferenceStore(context).isSoraEditorEnabled()) {
            TextEditorEngine.Sora
        } else {
            TextEditorEngine.SoraFallback
        }
    }

    fun resolveEditorLanguageMode(path: String): String? {
        val normalized = path.lowercase()
        if (normalized.substringAfterLast('/').startsWith("readme")) return "markdown"
        return when (normalized.substringAfterLast('.', missingDelimiterValue = "")) {
            "kt", "kts" -> "kotlin"
            "java" -> "java"
            "js", "mjs", "cjs" -> "javascript"
            "ts", "tsx" -> "typescript"
            "py" -> "python"
            "go" -> "go"
            "rs" -> "rust"
            "c", "h" -> "c"
            "cc", "cpp", "cxx", "hpp" -> "cpp"
            "json" -> "json"
            "xml" -> "xml"
            "html", "htm" -> "html"
            "css" -> "css"
            "md", "markdown", "mdown", "mkdn" -> "markdown"
            "yml", "yaml" -> "yaml"
            "sh", "bash", "zsh" -> "shell"
            else -> null
        }
    }

    fun shouldUseSoftWrap(path: String): Boolean {
        return resolveEditorLanguageMode(path) == "markdown" ||
            path.substringAfterLast('.', missingDelimiterValue = "") in setOf("txt", "log")
    }
}