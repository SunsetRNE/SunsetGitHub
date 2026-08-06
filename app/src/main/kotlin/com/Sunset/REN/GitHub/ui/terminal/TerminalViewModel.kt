package com.Sunset.REN.GitHub.ui.terminal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.Sunset.REN.GitHub.data.github.GitHubApiRemoteRepositoryCommitBackend
import com.Sunset.REN.GitHub.data.github.GitHubApiWorkspaceSyncBackend
import com.Sunset.REN.GitHub.data.local.EncryptedSharedPreferencesTokenStore
import com.Sunset.REN.GitHub.data.local.SharedPreferencesCurrentAccountStore
import com.Sunset.REN.GitHub.data.workspace.AppInternalWorkspaceGateway
import com.Sunset.REN.GitHub.domain.sync.WorkspaceSyncMode
import com.Sunset.REN.GitHub.domain.sync.WorkspaceSyncOptions
import com.Sunset.REN.GitHub.domain.sync.WorkspaceSyncRequest
import com.Sunset.REN.GitHub.domain.terminal.TerminalCommandProgress
import com.Sunset.REN.GitHub.domain.terminal.TerminalCommandRequest
import com.Sunset.REN.GitHub.domain.terminal.TerminalSyncCommandRequest
import com.Sunset.REN.GitHub.domain.terminal.WorkspaceCommandRunner
import com.Sunset.REN.GitHub.domain.workspace.WorkspaceProject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TerminalViewModel(application: Application) : AndroidViewModel(application) {
    private val workspaceGateway = AppInternalWorkspaceGateway(application)
    private val currentAccountStore = SharedPreferencesCurrentAccountStore(application)
    private val tokenStore = EncryptedSharedPreferencesTokenStore(application)
    private val commandRunner = WorkspaceCommandRunner(
        workspaceRootResolver = { workspaceId -> workspaceGateway.resolveWorkspaceRoot(workspaceId) },
        workspaceScanner = { workspaceId -> workspaceGateway.scanWorkspace(workspaceId) },
        workspaceLister = { workspaceGateway.listWorkspaces() },
        remoteBinder = { workspaceId, remote -> workspaceGateway.bindRemote(workspaceId, remote) },
        remoteClearer = { workspaceId -> workspaceGateway.clearRemoteBinding(workspaceId) },
        syncExecutor = ::executeSyncCommand,
        progressReporter = { progress -> updateProgress(progress) }
    )

    private val _state = MutableLiveData(TerminalUiState())
    val state: LiveData<TerminalUiState> = _state

    fun loadInitialWorkspace(
        preferredWorkspaceId: String? = null,
        preferredOwner: String? = null,
        preferredRepo: String? = null,
        seedCommand: String? = null,
        autoRunSeedCommand: Boolean = false
    ) {
        viewModelScope.launch {
            val workspaces = withContext(Dispatchers.IO) { workspaceGateway.listWorkspaces() }
            val workspace = selectInitialWorkspace(workspaces, preferredWorkspaceId, preferredOwner, preferredRepo)
            _state.value = _state.value.orEmpty().copy(
                workspaces = workspaces,
                selectedWorkspace = workspace,
                currentDirectory = ""
            )
            seedCommand?.takeIf { it.isNotBlank() }?.let {
                _state.value = _state.value.orEmpty().copy(
                    seedCommand = it,
                    autoRunSeedCommand = autoRunSeedCommand
                )
            }
            if (workspace == null) {
                appendOutput("暂无工作区。请先到“实验性工作区同步”创建或导入工作区。")
            } else {
                appendOutput("已连接工作区：${workspace.name}")
                appendOutput(workspace.rootPath)
                val remote = workspace.remoteBinding?.repositoryFullName
                if (!remote.isNullOrBlank()) appendOutput("远端：$remote")
                appendOutput("输入 help 查看可用命令，或点击“命令面板”选择模板。")
            }
        }
    }

    fun consumeSeedCommand(): TerminalSeedCommand? {
        val current = _state.value.orEmpty()
        val command = current.seedCommand ?: return null
        _state.value = current.copy(seedCommand = null, autoRunSeedCommand = false)
        return TerminalSeedCommand(command, current.autoRunSeedCommand)
    }

    fun selectWorkspace(workspaceId: String) {
        val current = _state.value.orEmpty()
        val workspace = current.workspaces.firstOrNull { it.id == workspaceId } ?: return
        _state.value = current.copy(selectedWorkspace = workspace, currentDirectory = "")
        appendOutput("已切换工作区：${workspace.name}\n${workspace.rootPath}")
    }

    fun runCommand(input: String) {
        val command = input.trim()
        if (command.isBlank()) return
        val currentState = _state.value.orEmpty()
        if (currentState.isCommandRunning) {
            appendOutput("已有命令正在执行，请稍候。")
            return
        }
        val workspace = currentState.selectedWorkspace
        if (workspace == null) {
            appendOutput("> $command\n未选择工作区。请先创建工作区。")
            return
        }
        val nextHistory = (currentState.history + command).takeLast(MaxCommandHistory)
        _state.value = currentState.copy(
            history = nextHistory,
            historyCursor = nextHistory.size,
            isCommandRunning = true,
            commandProgressPercent = null,
            commandProgressText = "执行中…"
        )
        appendOutput(prompt(currentState.currentDirectory) + command)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                commandRunner.run(
                    TerminalCommandRequest(
                        input = command,
                        workspace = workspace,
                        currentDirectory = _state.value.orEmpty().currentDirectory,
                        history = _state.value.orEmpty().history
                    )
                )
            }
            if (result.clearScreen) {
                _state.value = _state.value.orEmpty().copy(
                    output = "",
                    isCommandRunning = false,
                    commandProgressPercent = null,
                    commandProgressText = null
                )
                return@launch
            }
            result.nextDirectory?.let { directory ->
                _state.value = _state.value.orEmpty().copy(currentDirectory = directory)
            }
            result.updatedWorkspace?.let { workspace ->
                _state.value = _state.value.orEmpty().copy(selectedWorkspace = workspace)
            }
            val message = result.error?.let { "错误：$it" } ?: result.output
            if (message.isNotBlank()) appendOutput(message)
            _state.value = _state.value.orEmpty().copy(
                isCommandRunning = false,
                commandProgressPercent = null,
                commandProgressText = null
            )
        }
    }

    fun previousHistory(): String? {
        val current = _state.value.orEmpty()
        if (current.history.isEmpty()) return null
        val nextCursor = (current.historyCursor - 1).coerceAtLeast(0)
        _state.value = current.copy(historyCursor = nextCursor)
        return current.history.getOrNull(nextCursor)
    }

    fun nextHistory(): String? {
        val current = _state.value.orEmpty()
        if (current.history.isEmpty()) return null
        val nextCursor = (current.historyCursor + 1).coerceAtMost(current.history.size)
        _state.value = current.copy(historyCursor = nextCursor)
        return current.history.getOrNull(nextCursor).orEmpty()
    }

    fun favoriteCommands(): List<TerminalCommandTemplate> = FavoriteCommands

    fun commandTemplates(): List<TerminalCommandTemplate> = CommandTemplates

    fun exportOutput(): File? {
        val output = _state.value.orEmpty().output
        if (output.isBlank()) return null
        val exportDirectory = terminalExportDirectory().also { it.mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        return File(exportDirectory, "terminal-$timestamp.txt").also { file -> file.writeText(output) }
    }

    fun listExportedOutputs(): List<File> {
        val exportDirectory = terminalExportDirectory()
        return exportDirectory.listFiles()
            .orEmpty()
            .filter { it.isFile }
            .sortedByDescending { it.lastModified() }
    }

    fun clearExportedOutputs(): Int {
        val files = listExportedOutputs()
        val deletedCount = files.count { it.delete() }
        appendOutput("已清理导出文件：$deletedCount/${files.size}")
        return deletedCount
    }

    private suspend fun executeSyncCommand(request: TerminalSyncCommandRequest): String {
        val remote = request.workspace.remoteBinding ?: throw IllegalStateException("当前工作区尚未绑定远端。")
        val token = loadAccessTokenOrThrow()
        request.progress(TerminalCommandProgress(if (request.dryRun) "开始生成 Dry Run 计划..." else "开始生成同步计划...", 0, 100))
        val backend = GitHubApiWorkspaceSyncBackend(
            workspaceRootResolver = { workspaceId -> workspaceGateway.resolveWorkspaceRoot(workspaceId) },
            remoteCommitBackend = GitHubApiRemoteRepositoryCommitBackend(token)
        )
        val syncRequest = WorkspaceSyncRequest(
            workspaceId = request.workspace.id,
            remote = remote,
            commitMessage = request.commitMessage,
            mode = if (request.mirrorMode) WorkspaceSyncMode.MirrorRemotePath else WorkspaceSyncMode.UploadOnly,
            options = WorkspaceSyncOptions(
                dryRun = request.dryRun,
                allowDeletes = request.mirrorMode && request.destructiveConfirmed,
                allowOverwriteRemoteChanges = request.allowOverwriteRemoteChanges,
                blockOnSensitiveFiles = true,
                destructiveOperationConfirmed = request.destructiveConfirmed
            )
        )
        val plan = backend.buildPlan(syncRequest)
        request.progress(TerminalCommandProgress("计划已生成：operations=${plan.operations.size}, conflicts=${plan.conflicts.size}, sensitive=${plan.sensitiveFiles.size}", 5, 100))
        val planSummary = buildString {
            appendLine(if (request.dryRun) "Dry Run 计划" else "同步计划")
            appendLine("plan: ${plan.planId}")
            appendLine("remote: ${remote.repositoryFullName}:${remote.branch}/${remote.normalizedRemotePath.ifBlank { "" }}")
            appendLine("mode: ${plan.mode}")
            appendLine("operations=${plan.operations.size}, conflicts=${plan.conflicts.size}, sensitive=${plan.sensitiveFiles.size}")
            plan.warnings.take(8).forEach { appendLine("警告：$it") }
            plan.conflicts.take(8).forEach { appendLine("冲突：${it.remotePath} - ${it.message}") }
            plan.sensitiveFiles.take(8).forEach { appendLine("敏感：${it.relativePath} - ${it.reason}") }
            plan.operations.take(20).forEach { operation -> appendLine("${operation::class.simpleName}: ${operation.remotePath}") }
        }.trimEnd()
        request.progress(TerminalCommandProgress(if (request.dryRun) "开始执行 Dry Run..." else "开始执行同步...", 10, 100))
        val result = backend.executePlan(plan) { progress ->
            val percent = when {
                progress.totalOperations <= 0 -> 100
                progress.completedOperations >= progress.totalOperations -> 95
                else -> 10 + ((progress.completedOperations.coerceAtLeast(0) * 80f) / progress.totalOperations).toInt().coerceIn(0, 80)
            }
            request.progress(
                TerminalCommandProgress(
                    message = "${progress.phase}: ${progress.completedOperations}/${progress.totalOperations} ${progress.message}",
                    completedOperations = percent,
                    totalOperations = 100
                )
            )
        }
        request.progress(TerminalCommandProgress(if (request.dryRun) "Dry Run 已完成。" else "同步已完成。", 100, 100))
        return buildString {
            appendLine(planSummary)
            appendLine()
            appendLine(if (request.dryRun) "Dry Run 完成" else "同步完成")
            appendLine("commit: ${result.commitHash ?: "<none>"}")
            appendLine("applied=${result.appliedOperations.size}, skipped=${result.skippedOperations.size}, failed=${result.failedOperations.size}")
            result.failedOperations.take(8).forEach { failure -> appendLine("失败：${failure.operation.remotePath} - ${failure.reason}") }
        }.trimEnd()
    }

    private fun updateProgress(progress: TerminalCommandProgress) {
        val prefix = progress.percentage?.let { "[$it%]" } ?: "[progress]"
        appendOutput("$prefix ${progress.message}")
        _state.postValue(
            _state.value.orEmpty().copy(
                commandProgressPercent = progress.percentage,
                commandProgressText = progress.message
            )
        )
    }

    private suspend fun loadAccessTokenOrThrow(): String {
        val account = currentAccountStore.getCurrentAccount()
            ?: throw IllegalStateException("尚未登录 GitHub。")
        return tokenStore.getAccessToken(account.id)
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("当前账号缺少 access token。")
    }

    private fun selectInitialWorkspace(
        workspaces: List<WorkspaceProject>,
        preferredWorkspaceId: String?,
        preferredOwner: String?,
        preferredRepo: String?
    ): WorkspaceProject? {
        val explicit = preferredWorkspaceId?.takeIf { it.isNotBlank() }?.let { workspaceId ->
            workspaces.firstOrNull { it.id == workspaceId }
        }
        if (explicit != null) return explicit

        val owner = preferredOwner?.takeIf { it.isNotBlank() }
        val repo = preferredRepo?.takeIf { it.isNotBlank() }
        if (owner != null && repo != null) {
            workspaces.firstOrNull { workspace ->
                val remote = workspace.remoteBinding
                remote?.owner.equals(owner, ignoreCase = true) && remote?.repo.equals(repo, ignoreCase = true)
            }?.let { return it }
        }

        return workspaces.firstOrNull()
    }

    private fun appendOutput(message: String) {
        val current = _state.value.orEmpty()
        _state.postValue(current.copy(output = (current.output + "\n" + message).trim()))
    }

    private fun terminalExportDirectory(): File = File(getApplication<Application>().filesDir, TerminalExportDirectory)

    private fun prompt(currentDirectory: String): String {
        val display = currentDirectory.ifBlank { "/" }
        return "sunset:$display$ "
    }

    private fun TerminalUiState?.orEmpty(): TerminalUiState = this ?: TerminalUiState()

    private companion object {
        const val MaxCommandHistory = 80
        const val TerminalExportDirectory = "terminal-exports"
        val FavoriteCommands = listOf(
            TerminalCommandTemplate("帮助", "help"),
            TerminalCommandTemplate("命令历史", "history"),
            TerminalCommandTemplate("工作区状态", "status"),
            TerminalCommandTemplate("最近修改", "recent"),
            TerminalCommandTemplate("同步预演", "dry-run"),
            TerminalCommandTemplate("执行同步", "sync"),
            TerminalCommandTemplate("查看工作区", "workspace"),
            TerminalCommandTemplate("列出文件", "ls")
        )
        val CommandTemplates = listOf(
            TerminalCommandTemplate("搜索文件", "find README"),
            TerminalCommandTemplate("搜索内容", "grep TODO ."),
            TerminalCommandTemplate("最近文件", "recent 30"),
            TerminalCommandTemplate("命令历史", "history 20"),
            TerminalCommandTemplate("查看文件", "cat README.md"),
            TerminalCommandTemplate("行号预览", "preview README.md 1 40"),
            TerminalCommandTemplate("文件信息", "stat README.md"),
            TerminalCommandTemplate("统计文本", "wc README.md"),
            TerminalCommandTemplate("替换预览", "replace README.md old new --all"),
            TerminalCommandTemplate("确认替换", "replace README.md old new --all --confirm"),
            TerminalCommandTemplate("比较文件", "diff README.md README.md"),
            TerminalCommandTemplate("文件摘要", "checksum README.md"),
            TerminalCommandTemplate("目录大小", "du . --human"),
            TerminalCommandTemplate("创建目录", "mkdir docs"),
            TerminalCommandTemplate("写入文件", "write docs/note.md \"# Note\" --overwrite"),
            TerminalCommandTemplate("追加文本", "append docs/note.md \"\nmore text\""),
            TerminalCommandTemplate("绑定远端", "remote set 用户名 仓库名 main"),
            TerminalCommandTemplate("镜像同步预演", "dry-run --mirror"),
            TerminalCommandTemplate("执行镜像同步", "sync --mirror --confirm-delete")
        )
    }
}

data class TerminalUiState(
    val selectedWorkspace: WorkspaceProject? = null,
    val workspaces: List<WorkspaceProject> = emptyList(),
    val currentDirectory: String = "",
    val output: String = "",
    val history: List<String> = emptyList(),
    val historyCursor: Int = 0,
    val isCommandRunning: Boolean = false,
    val commandProgressPercent: Int? = null,
    val commandProgressText: String? = null,
    val seedCommand: String? = null,
    val autoRunSeedCommand: Boolean = false
)

data class TerminalSeedCommand(
    val command: String,
    val autoRun: Boolean
)

data class TerminalCommandTemplate(
    val title: String,
    val command: String
)
