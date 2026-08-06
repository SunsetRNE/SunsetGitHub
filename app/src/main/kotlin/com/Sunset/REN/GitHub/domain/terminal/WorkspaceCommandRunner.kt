package com.Sunset.REN.GitHub.domain.terminal

import com.Sunset.REN.GitHub.domain.workspace.WorkspaceProject
import com.Sunset.REN.GitHub.domain.workspace.WorkspaceRemoteBinding
import com.Sunset.REN.GitHub.domain.workspace.WorkspaceScanResult
import java.io.File

class WorkspaceCommandRunner(
    private val workspaceRootResolver: (String) -> File,
    private val workspaceScanner: suspend (String) -> WorkspaceScanResult,
    private val workspaceLister: suspend () -> List<WorkspaceProject>,
    private val remoteBinder: suspend (String, WorkspaceRemoteBinding) -> WorkspaceProject,
    private val remoteClearer: suspend (String) -> WorkspaceProject,
    private val syncExecutor: suspend (TerminalSyncCommandRequest) -> String,
    private val progressReporter: suspend (TerminalCommandProgress) -> Unit,
    commands: List<WorkspaceTerminalCommand> = TerminalCommandCatalog.defaultCommands(workspaceScanner)
) {
    private val commands: List<WorkspaceTerminalCommand> = commands.sortedBy { it.name }
    private val commandByName: Map<String, WorkspaceTerminalCommand> = this.commands.associateBy { it.name }

    suspend fun run(request: TerminalCommandRequest): TerminalCommandResult {
        val tokens = tokenize(request.input)
        if (tokens.isEmpty()) return TerminalCommandResult()
        val commandName = tokens.first().lowercase()
        val command = commandByName[commandName]
            ?: return TerminalCommandResult(error = "未知命令：$commandName。输入 help 查看可用命令。")
        val root = workspaceRootResolver(request.workspace.id).canonicalFile
        val context = WorkspaceTerminalContext(
            workspace = request.workspace,
            rootDirectory = root,
            currentDirectory = normalizeDirectory(request.currentDirectory),
            workspaceLister = workspaceLister,
            remoteBinder = remoteBinder,
            remoteClearer = remoteClearer,
            syncExecutor = syncExecutor,
            progressReporter = progressReporter,
            history = request.history
        )
        return runCatching {
            command.execute(context, tokens.drop(1), commands)
        }.getOrElse { error ->
            TerminalCommandResult(error = error.message ?: error::class.java.simpleName)
        }
    }
}

private fun tokenize(input: String): List<String> {
    val tokens = mutableListOf<String>()
    val current = StringBuilder()
    var quote: Char? = null
    input.trim().forEach { char ->
        when {
            quote != null && char == quote -> quote = null
            quote == null && (char == '\'' || char == '"') -> quote = char
            quote == null && char.isWhitespace() -> {
                if (current.isNotEmpty()) {
                    tokens += current.toString()
                    current.clear()
                }
            }
            else -> current.append(char)
        }
    }
    if (current.isNotEmpty()) tokens += current.toString()
    return tokens
}

private fun normalizeDirectory(path: String): String = path.trim().trim('/').replace('\\', '/')
