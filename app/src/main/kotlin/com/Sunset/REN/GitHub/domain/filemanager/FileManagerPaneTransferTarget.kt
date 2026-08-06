package com.Sunset.REN.GitHub.domain.filemanager

data class FileManagerPaneTransferTarget(
    val sourcePane: FileManagerPaneId,
    val targetPane: FileManagerPaneId,
    val sourcePath: String,
    val targetPath: String,
    val isExplicitDualPaneTarget: Boolean
) {
    val hasDistinctTargetPath: Boolean get() = sourcePath != targetPath
}

object FileManagerPaneTransferTargetResolver {
    fun resolve(
        paneState: FileManagerDualPaneState,
        navigationState: FileManagerDualPaneNavigationState
    ): FileManagerPaneTransferTarget? {
        if (!paneState.isDualPane) return null
        val sourcePane = paneState.focusedPane
        val targetPane = sourcePane.opposite()
        val sourcePath = navigationState.pane(sourcePane).currentPath
        val targetPath = navigationState.pane(targetPane).currentPath
        return FileManagerPaneTransferTarget(
            sourcePane = sourcePane,
            targetPane = targetPane,
            sourcePath = sourcePath,
            targetPath = targetPath,
            isExplicitDualPaneTarget = true
        )
    }
}