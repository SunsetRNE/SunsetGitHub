package com.Sunset.REN.GitHub.domain.terminal

import com.Sunset.REN.GitHub.domain.workspace.WorkspaceProject
import java.io.File

/**
 * 面向 App 内部工作区的轻量命令终端模型。
 * 这不是系统 shell，也不承诺执行任意 Android/Linux 命令。
 */
data class TerminalCommandRequest(
    val input: String,
    val workspace: WorkspaceProject,
    val currentDirectory: String = "",
    val history: List<String> = emptyList()
)

data class TerminalCommandResult(
    val output: String = "",
    val error: String? = null,
    val nextDirectory: String? = null,
    val updatedWorkspace: WorkspaceProject? = null,
    val clearScreen: Boolean = false
) {
    val isSuccess: Boolean get() = error == null
}

data class TerminalSyncCommandRequest(
    val workspace: WorkspaceProject,
    val dryRun: Boolean,
    val mirrorMode: Boolean,
    val destructiveConfirmed: Boolean,
    val allowOverwriteRemoteChanges: Boolean,
    val commitMessage: String,
    val progress: suspend (TerminalCommandProgress) -> Unit = {}
)

data class TerminalCommandProgress(
    val message: String,
    val completedOperations: Int? = null,
    val totalOperations: Int? = null
) {
    val percentage: Int?
        get() = totalOperations
            ?.takeIf { it > 0 }
            ?.let { total -> (((completedOperations ?: 0).coerceIn(0, total) * 100f) / total).toInt().coerceIn(0, 100) }
}

data class WorkspaceTerminalContext(
    val workspace: WorkspaceProject,
    val rootDirectory: File,
    val currentDirectory: String = "",
    val workspaceLister: suspend () -> List<WorkspaceProject>,
    val remoteBinder: suspend (String, com.Sunset.REN.GitHub.domain.workspace.WorkspaceRemoteBinding) -> WorkspaceProject,
    val remoteClearer: suspend (String) -> WorkspaceProject,
    val syncExecutor: suspend (TerminalSyncCommandRequest) -> String,
    val progressReporter: suspend (TerminalCommandProgress) -> Unit = {},
    val history: List<String> = emptyList()
)

interface WorkspaceTerminalCommand {
    val name: String
    val description: String
    val usage: String

    suspend fun execute(
        context: WorkspaceTerminalContext,
        args: List<String>,
        allCommands: List<WorkspaceTerminalCommand>
    ): TerminalCommandResult
}
