package com.Sunset.REN.GitHub.ui.editor

import android.content.Context
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.langs.java.JavaLanguage

object TextEditorLanguageRegistry {

    fun resolve(context: Context, mode: String?): Language {
        return TextMateLanguageProvider.create(context, mode)
            ?: when (mode?.lowercase()) {
                "java" -> JavaLanguage()
                else -> EmptyLanguage()
            }
    }

    fun hasNativeLanguage(mode: String?): Boolean {
        return TextMateLanguageProvider.supports(mode) || when (mode?.lowercase()) {
            "java" -> true
            else -> false
        }
    }
}