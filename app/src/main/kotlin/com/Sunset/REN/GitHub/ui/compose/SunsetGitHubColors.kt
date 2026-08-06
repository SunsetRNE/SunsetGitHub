package com.Sunset.REN.GitHub.ui.compose

import androidx.compose.ui.graphics.Color

/**
 * Compose-side color tokens mapped from the existing GitHub/Primer XML palette.
 *
 * Keep these values aligned with res/values/colors.xml and res/values-night/colors.xml until
 * migrated screens can read all color decisions from a single design-token source.
 */
object SunsetGitHubLightColors {
    val Canvas = Color(0xFFFFFFFF)
    val Surface = Color(0xFFFFFFFF)
    val SubtleBackground = Color(0xFFFFFFFF)
    val Border = Color(0xFFD0D7DE)
    val BorderStrong = Color(0xFF8C959F)
    val Divider = Color(0xFFD8DEE4)
    val TextPrimary = Color(0xFF24292F)
    val TextSecondary = Color(0xFF57606A)
    val TextMuted = Color(0xFF6E7781)
    val Accent = Color(0xFF0969DA)
    val AccentSoft = Color(0xFFDDF0FF)
    val AccentSoftBorder = Color(0xFFADD5FF)
    val Success = Color(0xFF1A7F37)
    val SuccessSoft = Color(0xFFDAFBE1)
    val Attention = Color(0xFFBC4C00)
    val AttentionSoft = Color(0xFFFFF1E5)
    val Danger = Color(0xFFCF222E)
    val DangerSoft = Color(0xFFFFEBE9)
    val Done = Color(0xFF8250DF)
    val DoneSoft = Color(0xFFFBEFFF)
    val ChipBackground = Color(0xFFEFF2F5)
}

object SunsetGitHubDarkColors {
    val Canvas = Color(0xFF0D1117)
    val Surface = Color(0xFF161B22)
    val SubtleBackground = Color(0xFF0D1117)
    val Border = Color(0xFF30363D)
    val BorderStrong = Color(0xFF8B949E)
    val Divider = Color(0xFF21262D)
    val TextPrimary = Color(0xFFE6EDF3)
    val TextSecondary = Color(0xFF8B949E)
    val TextMuted = Color(0xFF7D8590)
    val Accent = Color(0xFF58A6FF)
    val AccentSoft = Color(0x332F81F7)
    val AccentSoftBorder = Color(0xFF1F6FEB)
    val Success = Color(0xFF3FB950)
    val SuccessSoft = Color(0x3326A641)
    val Attention = Color(0xFFD29922)
    val AttentionSoft = Color(0x33D29922)
    val Danger = Color(0xFFFF7B72)
    val DangerSoft = Color(0x33F85149)
    val Done = Color(0xFFBC8CFF)
    val DoneSoft = Color(0x33A371F7)
    val ChipBackground = Color(0xFF21262D)
}

data class SunsetGitHubExtraColors(
    val canvas: Color,
    val surface: Color,
    val subtleBackground: Color,
    val border: Color,
    val borderStrong: Color,
    val divider: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val accentSoft: Color,
    val accentSoftBorder: Color,
    val success: Color,
    val successSoft: Color,
    val attention: Color,
    val attentionSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
    val done: Color,
    val doneSoft: Color,
    val chipBackground: Color
)

fun sunsetGitHubExtraColors(darkTheme: Boolean): SunsetGitHubExtraColors {
    return if (darkTheme) {
        SunsetGitHubExtraColors(
            canvas = SunsetGitHubDarkColors.Canvas,
            surface = SunsetGitHubDarkColors.Surface,
            subtleBackground = SunsetGitHubDarkColors.SubtleBackground,
            border = SunsetGitHubDarkColors.Border,
            borderStrong = SunsetGitHubDarkColors.BorderStrong,
            divider = SunsetGitHubDarkColors.Divider,
            textPrimary = SunsetGitHubDarkColors.TextPrimary,
            textSecondary = SunsetGitHubDarkColors.TextSecondary,
            textMuted = SunsetGitHubDarkColors.TextMuted,
            accent = SunsetGitHubDarkColors.Accent,
            accentSoft = SunsetGitHubDarkColors.AccentSoft,
            accentSoftBorder = SunsetGitHubDarkColors.AccentSoftBorder,
            success = SunsetGitHubDarkColors.Success,
            successSoft = SunsetGitHubDarkColors.SuccessSoft,
            attention = SunsetGitHubDarkColors.Attention,
            attentionSoft = SunsetGitHubDarkColors.AttentionSoft,
            danger = SunsetGitHubDarkColors.Danger,
            dangerSoft = SunsetGitHubDarkColors.DangerSoft,
            done = SunsetGitHubDarkColors.Done,
            doneSoft = SunsetGitHubDarkColors.DoneSoft,
            chipBackground = SunsetGitHubDarkColors.ChipBackground
        )
    } else {
        SunsetGitHubExtraColors(
            canvas = SunsetGitHubLightColors.Canvas,
            surface = SunsetGitHubLightColors.Surface,
            subtleBackground = SunsetGitHubLightColors.SubtleBackground,
            border = SunsetGitHubLightColors.Border,
            borderStrong = SunsetGitHubLightColors.BorderStrong,
            divider = SunsetGitHubLightColors.Divider,
            textPrimary = SunsetGitHubLightColors.TextPrimary,
            textSecondary = SunsetGitHubLightColors.TextSecondary,
            textMuted = SunsetGitHubLightColors.TextMuted,
            accent = SunsetGitHubLightColors.Accent,
            accentSoft = SunsetGitHubLightColors.AccentSoft,
            accentSoftBorder = SunsetGitHubLightColors.AccentSoftBorder,
            success = SunsetGitHubLightColors.Success,
            successSoft = SunsetGitHubLightColors.SuccessSoft,
            attention = SunsetGitHubLightColors.Attention,
            attentionSoft = SunsetGitHubLightColors.AttentionSoft,
            danger = SunsetGitHubLightColors.Danger,
            dangerSoft = SunsetGitHubLightColors.DangerSoft,
            done = SunsetGitHubLightColors.Done,
            doneSoft = SunsetGitHubLightColors.DoneSoft,
            chipBackground = SunsetGitHubLightColors.ChipBackground
        )
    }
}
