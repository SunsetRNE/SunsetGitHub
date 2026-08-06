package com.Sunset.REN.GitHub.domain.filemanager

data class PaneStartupSettings(
    val mode: StartupPathMode = StartupPathMode.Home,
    val homePath: String? = null
)

enum class StartupPathMode {
    Home,
    StorageRoot,
    AppFiles
}