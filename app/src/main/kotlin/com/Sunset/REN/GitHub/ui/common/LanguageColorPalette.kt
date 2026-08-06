package com.Sunset.REN.GitHub.ui.common

import android.graphics.Color

/**
 * 常见编程语言 → 代表色映射（对齐 GitHub Linguist 配色），用于语言占比条与仓库项语言色点。
 * 未知语言回退到中性灰。
 */
object LanguageColorPalette {

    private val colors: Map<String, String> = mapOf(
        "kotlin" to "#A97BFF",
        "java" to "#B07219",
        "javascript" to "#F1E05A",
        "typescript" to "#3178C6",
        "python" to "#3572A5",
        "html" to "#E34C26",
        "css" to "#563D7C",
        "c" to "#555555",
        "c++" to "#F34B7D",
        "c#" to "#178600",
        "go" to "#00ADD8",
        "rust" to "#DEA584",
        "ruby" to "#701516",
        "php" to "#4F5D95",
        "swift" to "#F05138",
        "dart" to "#00B4AB",
        "shell" to "#89E051",
        "objective-c" to "#438EFF",
        "scala" to "#C22D40",
        "vue" to "#41B883",
        "xml" to "#0060AC",
        "json" to "#292929",
        "markdown" to "#083FA1",
        "lua" to "#000080",
        "perl" to "#0298C3",
        "haskell" to "#5E5086",
        "elixir" to "#6E4A7E",
        "clojure" to "#DB5855",
        "groovy" to "#4298B8",
        "r" to "#198CE7"
    )

    private const val FALLBACK_COLOR = "#8B949E"

    /** 取语言代表色（int 形式）。语言名大小写不敏感，未知回退中性灰。 */
    fun colorFor(language: String?): Int {
        val key = language?.trim()?.lowercase()
        val hex = key?.let { colors[it] } ?: FALLBACK_COLOR
        return Color.parseColor(hex)
    }
}
