package com.Sunset.REN.GitHub.ui.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalSunsetGitHubExtraColors = staticCompositionLocalOf {
    sunsetGitHubExtraColors(darkTheme = false)
}

private val LocalSunsetGitHubSpacing = staticCompositionLocalOf { SunsetGitHubSpacing() }

private fun sunsetGitHubMaterialColorScheme(darkTheme: Boolean) = if (darkTheme) {
    darkColorScheme(
        primary = SunsetGitHubDarkColors.Accent,
        onPrimary = SunsetGitHubDarkColors.Canvas,
        primaryContainer = SunsetGitHubDarkColors.AccentSoft,
        onPrimaryContainer = SunsetGitHubDarkColors.TextPrimary,
        secondary = SunsetGitHubDarkColors.TextSecondary,
        onSecondary = SunsetGitHubDarkColors.Canvas,
        background = SunsetGitHubDarkColors.Canvas,
        onBackground = SunsetGitHubDarkColors.TextPrimary,
        surface = SunsetGitHubDarkColors.Surface,
        onSurface = SunsetGitHubDarkColors.TextPrimary,
        surfaceVariant = SunsetGitHubDarkColors.ChipBackground,
        onSurfaceVariant = SunsetGitHubDarkColors.TextSecondary,
        outline = SunsetGitHubDarkColors.Border,
        error = SunsetGitHubDarkColors.Danger,
        onError = SunsetGitHubDarkColors.Canvas
    )
} else {
    lightColorScheme(
        primary = SunsetGitHubLightColors.Accent,
        onPrimary = SunsetGitHubLightColors.Surface,
        primaryContainer = SunsetGitHubLightColors.AccentSoft,
        onPrimaryContainer = SunsetGitHubLightColors.TextPrimary,
        secondary = SunsetGitHubLightColors.TextSecondary,
        onSecondary = SunsetGitHubLightColors.Surface,
        background = SunsetGitHubLightColors.Canvas,
        onBackground = SunsetGitHubLightColors.TextPrimary,
        surface = SunsetGitHubLightColors.Surface,
        onSurface = SunsetGitHubLightColors.TextPrimary,
        surfaceVariant = SunsetGitHubLightColors.ChipBackground,
        onSurfaceVariant = SunsetGitHubLightColors.TextSecondary,
        outline = SunsetGitHubLightColors.Border,
        error = SunsetGitHubLightColors.Danger,
        onError = SunsetGitHubLightColors.Surface
    )
}

@Composable
fun SunsetGitHubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val extraColors = sunsetGitHubExtraColors(darkTheme)
    CompositionLocalProvider(
        LocalSunsetGitHubExtraColors provides extraColors,
        LocalSunsetGitHubSpacing provides SunsetGitHubSpacing()
    ) {
        MaterialTheme(
            colorScheme = sunsetGitHubMaterialColorScheme(darkTheme),
            typography = SunsetGitHubTypography,
            content = content
        )
    }
}

object SunsetGitHubThemeTokens {
    val colors: SunsetGitHubExtraColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSunsetGitHubExtraColors.current

    val spacing: SunsetGitHubSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalSunsetGitHubSpacing.current
}