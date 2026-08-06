package com.Sunset.REN.GitHub.domain.filemanager.root

/**
 * User-visible Root configuration for the local file manager.
 *
 * Only [RootAccessBackend.Su] is currently executable. Shell/Shizuku are kept in
 * settings as explicit, disabled roadmap options so the UI can communicate the
 * current boundary without pretending those backends are available.
 */
data class RootAccessSettings(
    val backend: RootAccessBackend = RootAccessBackend.Su,
    val startupPolicy: RootStartupPolicy = RootStartupPolicy.DetectOnly,
    val suCommand: String = DefaultSuCommand
) {
    fun normalized(): RootAccessSettings {
        val normalizedSuCommand = suCommand.trim().ifBlank { DefaultSuCommand }
        val safeSuCommand = if (
            normalizedSuCommand.none { it.isWhitespace() } &&
            normalizedSuCommand.matches(Regex("[A-Za-z0-9_./-]+"))
        ) {
            normalizedSuCommand
        } else {
            DefaultSuCommand
        }
        return copy(
            backend = if (backend.isAvailable) backend else RootAccessBackend.Su,
            suCommand = safeSuCommand
        )
    }

    companion object {
        const val DefaultSuCommand = "su"
    }
}

enum class RootAccessBackend(val isAvailable: Boolean) {
    Su(isAvailable = true),
    Shell(isAvailable = false),
    Shizuku(isAvailable = false)
}

enum class RootStartupPolicy {
    Disabled,
    DetectOnly,
    RequestOnStartup
}
