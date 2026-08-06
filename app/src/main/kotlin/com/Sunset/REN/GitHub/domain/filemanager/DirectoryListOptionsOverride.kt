package com.Sunset.REN.GitHub.domain.filemanager

/** Directory-specific list presentation override for one file-manager pane. */
data class DirectoryListOptionsOverride(
    val pane: FileManagerPaneId,
    val path: String,
    val options: FileManagerListOptions
)
