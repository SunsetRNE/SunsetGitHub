package com.Sunset.REN.GitHub.ui.workspace

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
import com.Sunset.REN.GitHub.domain.workspace.WorkspaceImportOptions
import com.Sunset.REN.GitHub.domain.workspace.WorkspaceImportRequest
import com.Sunset.REN.GitHub.domain.workspace.WorkspaceImportSource
import com.Sunset.REN.GitHub.domain.workspace.WorkspaceProject
import com.Sunset.REN.GitHub.domain.workspace.WorkspaceRemoteBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WorkspaceSyncViewModel(application: Application) : AndroidViewModel(application) {
    private val workspaceGateway = AppInternalWorkspaceGateway(application)
    private val currentAccountStore = SharedPreferencesCurrentAccountStore(application)
    private val tokenStore = EncryptedSharedPreferencesTokenStore(application)

    private val _state = MutableLiveData(WorkspaceSyncUiState())
    val state: LiveData<WorkspaceSyncUiState> = _state

    fun loadInitialWorkspace() {
        viewModelScope.launch {
            val workspace = withContext(Dispatchers.IO) {
                workspaceGateway.listWorkspaces().firstOrNull()
            }
            _state.value = _state.value.orEmpty().copy(selectedWorkspace = workspace)
            appendLog(workspace?.let { "已选择最近工作区：${it.name}\n${it.rootPath}" } ?: "暂无工作区，请先创建。")
        }
    }

    fun createWorkspace(name: String) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { workspaceGateway.createWorkspace(name) }
            }.onSuccess { workspace ->
                _state.value = _state.value.orEmpty().copy(selectedWorkspace = workspace)
                appendLog("创建工作区：${workspace.name}\n${workspace.rootPath}")
            }.onFailure { error ->
                appendLog("创建工作区失败：${error.message}")
            }
        }
    }

    fun importPath(sourcePath: String, targetDirectory: String) {
        val workspace = _state.value?.selectedWorkspace
        if (workspace == null) {
            appendLog("请先创建或选择工作区。")
            return
        }
        if (sourcePath.isBlank()) {
            appendLog("导入来源路径不能为空。")
            return
        }
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    workspaceGateway.importIntoWorkspace(
                        WorkspaceImportRequest(
                            workspaceId = workspace.id,
                            sources = listOf(WorkspaceImportSource.AppInternalPath(sourcePath.trim())),
                            targetDirectory = targetDirectory,
                            options = WorkspaceImportOptions(
                                overwriteExisting = true,
                                preserveDirectoryStructure = true,
                                skipHiddenFiles = true,
                                applyIgnoreRules = true,
                                detectSensitiveFiles = true
                            )
                        )
                    )
                }
            }.onSuccess { result ->
                appendLog(
                    buildString {
                        appendLine("导入完成：${result.importedFiles.size} 个文件")
                        appendLine("跳过：${result.skippedFiles.size} 个")
                        if (result.sensitiveFiles.isNotEmpty()) {
                            appendLine("敏感文件：${result.sensitiveFiles.size} 个")
                            result.sensitiveFiles.take(8).forEach { appendLine("- ${it.relativePath}: ${it.reason}") }
                        }
                    }.trimEnd()
                )
            }.onFailure { error ->
                appendLog("导入失败：${error.message}")
            }
        }
    }

    fun dryRun(input: WorkspaceSyncInput) {
        sync(input, dryRun = true)
    }

    fun execute(input: WorkspaceSyncInput) {
        sync(input, dryRun = false)
    }

    private fun sync(input: WorkspaceSyncInput, dryRun: Boolean) {
        val workspace = _state.value?.selectedWorkspace
        if (workspace == null) {
            appendLog("请先创建或选择工作区。")
            return
        }
        viewModelScope.launch {
            runCatching {
                val token = loadAccessTokenOrThrow()
                val backend = GitHubApiWorkspaceSyncBackend(
                    workspaceRootResolver = { workspaceId -> workspaceGateway.resolveWorkspaceRoot(workspaceId) },
                    remoteCommitBackend = GitHubApiRemoteRepositoryCommitBackend(token)
                )
                val request = WorkspaceSyncRequest(
                    workspaceId = workspace.id,
                    remote = WorkspaceRemoteBinding(
                        owner = input.owner,
                        repo = input.repo,
                        branch = input.branch.ifBlank { "main" },
                        remotePath = input.remotePath
                    ),
                    commitMessage = input.commitMessage.ifBlank { "Sync workspace from SunsetGitHub" },
                    mode = if (input.mirrorMode) WorkspaceSyncMode.MirrorRemotePath else WorkspaceSyncMode.UploadOnly,
                    options = WorkspaceSyncOptions(
                        dryRun = dryRun,
                        allowDeletes = input.mirrorMode && input.destructiveConfirmed,
                        allowOverwriteRemoteChanges = input.allowOverwriteRemoteChanges,
                        blockOnSensitiveFiles = true,
                        destructiveOperationConfirmed = input.destructiveConfirmed
                    )
                )
                appendLog(if (dryRun) "开始 Dry Run..." else "开始执行同步...")
                val plan = withContext(Dispatchers.IO) { backend.buildPlan(request) }
                appendLog(
                    buildString {
                        appendLine("计划 ${plan.planId}")
                        appendLine("操作：${plan.operations.size}，冲突：${plan.conflicts.size}，敏感：${plan.sensitiveFiles.size}")
                        plan.warnings.forEach { appendLine("警告：$it") }
                        plan.conflicts.take(8).forEach { appendLine("冲突：${it.remotePath} - ${it.message}") }
                        plan.operations.take(20).forEach { appendLine("${it::class.simpleName}: ${it.remotePath}") }
                    }.trimEnd()
                )
                val result = withContext(Dispatchers.IO) {
                    backend.executePlan(plan) { progress ->
                        appendLog("${progress.phase}: ${progress.completedOperations}/${progress.totalOperations} ${progress.message}")
                    }
                }
                appendLog(
                    buildString {
                        appendLine("同步结束")
                        appendLine("commit: ${result.commitHash ?: "<none>"}")
                        appendLine("applied=${result.appliedOperations.size}, skipped=${result.skippedOperations.size}, failed=${result.failedOperations.size}")
                    }.trimEnd()
                )
            }.onFailure { error ->
                appendLog("同步失败：${error.message}")
            }
        }
    }

    private suspend fun loadAccessTokenOrThrow(): String {
        val account = withContext(Dispatchers.IO) { currentAccountStore.getCurrentAccount() }
            ?: throw IllegalStateException("尚未登录 GitHub。")
        return withContext(Dispatchers.IO) { tokenStore.getAccessToken(account.id) }
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("当前账号缺少 access token。")
    }

    private fun appendLog(message: String) {
        val current = _state.value.orEmpty()
        _state.postValue(current.copy(log = (current.log + "\n" + message).trim()))
    }

    private fun WorkspaceSyncUiState?.orEmpty(): WorkspaceSyncUiState = this ?: WorkspaceSyncUiState()
}

data class WorkspaceSyncUiState(
    val selectedWorkspace: WorkspaceProject? = null,
    val log: String = ""
)

data class WorkspaceSyncInput(
    val owner: String,
    val repo: String,
    val branch: String,
    val remotePath: String,
    val commitMessage: String,
    val mirrorMode: Boolean,
    val destructiveConfirmed: Boolean,
    val allowOverwriteRemoteChanges: Boolean
)