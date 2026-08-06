package com.Sunset.REN.GitHub.ui.editor

import android.content.Context
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import org.eclipse.tm4e.core.registry.IThemeSource
object TextMateLanguageProvider {

    private var isInitialized = false

    fun create(context: Context, mode: String?): Language? {
        val scopeName = when (mode?.lowercase()) {
            "javascript" -> "source.js"
            "json" -> "source.json"
            "markdown" -> "text.html.markdown"
            else -> return null
        }
        ensureInitialized(context.applicationContext)
        return TextMateLanguage.create(scopeName, true)
    }

    fun supports(mode: String?): Boolean {
        return when (mode?.lowercase()) {
            "javascript", "json", "markdown" -> true
            else -> false
        }
    }


    private fun ensureInitialized(context: Context) {
        if (isInitialized) return
        FileProviderRegistry.getInstance().addFileProvider(AssetsFileResolver(context.assets))
        val themePath = "textmate/quietlight.json"
        val themeRegistry = ThemeRegistry.getInstance()
        themeRegistry.loadTheme(
            ThemeModel(
                IThemeSource.fromInputStream(
                    FileProviderRegistry.getInstance().tryGetInputStream(themePath),
                    themePath,
                    null
                ),
                TextMateThemeName
            )
        )
        themeRegistry.setTheme(TextMateThemeName)
        GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")
        isInitialized = true
    }

    private const val TextMateThemeName = "quietlight"
}