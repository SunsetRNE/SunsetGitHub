package com.Sunset.REN.GitHub.domain.filemanager

data class FileManagerPaneNavigationState(
    val paneId: FileManagerPaneId,
    val currentPath: String,
    val backStack: List<String> = emptyList(),
    val forwardStack: List<String> = emptyList()
) {
    init {
        require(currentPath.isNotBlank()) { "Current path must not be blank." }
        require(backStack.none { it.isBlank() }) { "Back stack paths must not be blank." }
        require(forwardStack.none { it.isBlank() }) { "Forward stack paths must not be blank." }
    }
    val canGoBack: Boolean get() = backStack.isNotEmpty()
    val canGoForward: Boolean get() = forwardStack.isNotEmpty()
    val canGoUp: Boolean get() = !currentPath.startsWith("content://", ignoreCase = true) && parentPathOrNull(currentPath) != null


    fun withPaneId(paneId: FileManagerPaneId): FileManagerPaneNavigationState {
        return copy(paneId = paneId)
    }

    fun enter(path: String): FileManagerPaneNavigationState {
        val normalizedPath = normalizePath(path)
        if (normalizedPath == currentPath) return this
        return copy(
            currentPath = normalizedPath,
            backStack = backStack + currentPath,
            forwardStack = emptyList()
        )
    }

    fun replace(path: String): FileManagerPaneNavigationState {
        return copy(
            currentPath = normalizePath(path),
            backStack = emptyList(),
            forwardStack = emptyList()
        )
    }

    fun goBack(): FileManagerPaneNavigationState {
        if (backStack.isEmpty()) return this
        return copy(
            currentPath = backStack.last(),
            backStack = backStack.dropLast(1),
            forwardStack = forwardStack + currentPath
        )
    }

    fun goForward(): FileManagerPaneNavigationState {
        if (forwardStack.isEmpty()) return this
        return copy(
            currentPath = forwardStack.last(),
            backStack = backStack + currentPath,
            forwardStack = forwardStack.dropLast(1)
        )
    }

    fun goUp(): FileManagerPaneNavigationState {
        val parentPath = parentPathOrNull(currentPath) ?: return this
        return enter(parentPath)
    }

    companion object {
        fun root(paneId: FileManagerPaneId, path: String): FileManagerPaneNavigationState {
            return FileManagerPaneNavigationState(
                paneId = paneId,
                currentPath = normalizePath(path)
            )
        }

        fun normalizePath(path: String): String {
            val trimmed = path.trim().replace('\\', '/')
            require(trimmed.isNotBlank()) { "Path must not be blank." }
            if (trimmed.startsWith("root://", ignoreCase = true)) {
                val rootPath = trimmed.removePrefix("root://").trimEnd('/').ifBlank { "/" }
                val absoluteRootPath = if (rootPath.startsWith('/')) rootPath else "/$rootPath"
                return if (absoluteRootPath == "/") "root:///" else "root://$absoluteRootPath"
            }
            if (trimmed == "/" || trimmed.startsWith("content://", ignoreCase = true)) return trimmed
            return trimmed.trimEnd('/')
        }

        fun parentPathOrNull(path: String): String? {
            val normalizedPath = normalizePath(path)
            if (normalizedPath == "/" || normalizedPath == "root:///") return null
            if (normalizedPath.startsWith("root://", ignoreCase = true)) {
                val rootPath = normalizedPath.removePrefix("root://")
                val index = rootPath.lastIndexOf('/')
                return when {
                    index <= 0 -> "root:///"
                    else -> "root://${rootPath.substring(0, index)}"
                }
            }
            val index = normalizedPath.lastIndexOf('/')
            return when {
                index < 0 -> null
                index == 0 -> "/"
                else -> normalizedPath.substring(0, index)
            }
        }
    }
}

data class FileManagerDualPaneNavigationState(
    val left: FileManagerPaneNavigationState,
    val right: FileManagerPaneNavigationState
) {
    init {
        require(left.paneId == FileManagerPaneId.Left) { "Left state must use the left pane id." }
        require(right.paneId == FileManagerPaneId.Right) { "Right state must use the right pane id." }
    }

    fun pane(paneId: FileManagerPaneId): FileManagerPaneNavigationState {
        return when (paneId) {
            FileManagerPaneId.Left -> left
            FileManagerPaneId.Right -> right
        }
    }

    fun updatePane(
        paneId: FileManagerPaneId,
        update: (FileManagerPaneNavigationState) -> FileManagerPaneNavigationState
    ): FileManagerDualPaneNavigationState {
        return when (paneId) {
            FileManagerPaneId.Left -> copy(left = update(left))
            FileManagerPaneId.Right -> copy(right = update(right))
        }
    }

    fun enter(paneId: FileManagerPaneId, path: String): FileManagerDualPaneNavigationState {
        return updatePane(paneId) { it.enter(path) }
    }

    fun replace(paneId: FileManagerPaneId, path: String): FileManagerDualPaneNavigationState {
        return updatePane(paneId) { it.replace(path) }
    }

    fun goBack(paneId: FileManagerPaneId): FileManagerDualPaneNavigationState {
        return updatePane(paneId) { it.goBack() }
    }

    fun goForward(paneId: FileManagerPaneId): FileManagerDualPaneNavigationState {
        return updatePane(paneId) { it.goForward() }
    }

    fun goUp(paneId: FileManagerPaneId): FileManagerDualPaneNavigationState {
        return updatePane(paneId) { it.goUp() }
    }

    fun swapPanes(): FileManagerDualPaneNavigationState {
        return FileManagerDualPaneNavigationState(
            left = right.withPaneId(FileManagerPaneId.Left),
            right = left.withPaneId(FileManagerPaneId.Right)
        )
    }

    companion object {
        fun roots(
            leftPath: String,
            rightPath: String
        ): FileManagerDualPaneNavigationState {
            return FileManagerDualPaneNavigationState(
                left = FileManagerPaneNavigationState.root(FileManagerPaneId.Left, leftPath),
                right = FileManagerPaneNavigationState.root(FileManagerPaneId.Right, rightPath)
            )
        }
    }
}