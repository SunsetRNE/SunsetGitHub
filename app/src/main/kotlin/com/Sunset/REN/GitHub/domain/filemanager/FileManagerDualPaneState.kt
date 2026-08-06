package com.Sunset.REN.GitHub.domain.filemanager

enum class FileManagerPaneId {
    Left,
    Right
}

data class FileManagerDualPaneState(
    val isDualPane: Boolean,
    val focusedPane: FileManagerPaneId = FileManagerPaneId.Left,
    val sourcePane: FileManagerPaneId = FileManagerPaneId.Left,
    val targetPane: FileManagerPaneId = FileManagerPaneId.Right
) {
    init {
        require(sourcePane != targetPane) { "Source and target panes must be different." }
    }

    fun focus(pane: FileManagerPaneId): FileManagerDualPaneState {
        return if (isDualPane) copy(focusedPane = pane) else copy(focusedPane = FileManagerPaneId.Left)
    }

    fun toggleFocus(): FileManagerDualPaneState {
        return focus(focusedPane.opposite())
    }

    fun withSourcePane(pane: FileManagerPaneId): FileManagerDualPaneState {
        return if (isDualPane) {
            copy(sourcePane = pane, targetPane = pane.opposite(), focusedPane = pane)
        } else {
            copy(sourcePane = FileManagerPaneId.Left, targetPane = FileManagerPaneId.Right, focusedPane = FileManagerPaneId.Left)
        }
    }

    fun swapPanes(): FileManagerDualPaneState {
        return if (isDualPane) {
            copy(sourcePane = targetPane, targetPane = sourcePane, focusedPane = targetPane)
        } else {
            this
        }
    }

    companion object {
        private const val WideScreenMinDp = 600

        fun fromConfiguration(
            screenWidthDp: Int,
            isLandscape: Boolean,
            userEnabledDualPane: Boolean = true,
            forceDualPane: Boolean = false
        ): FileManagerDualPaneState {
            val isWideScreen = screenWidthDp >= WideScreenMinDp
            val isDualPane = userEnabledDualPane && (forceDualPane || (isLandscape && isWideScreen))
            return FileManagerDualPaneState(isDualPane = isDualPane)
        }
    }
}

fun FileManagerPaneId.opposite(): FileManagerPaneId {
    return when (this) {
        FileManagerPaneId.Left -> FileManagerPaneId.Right
        FileManagerPaneId.Right -> FileManagerPaneId.Left
    }
}
