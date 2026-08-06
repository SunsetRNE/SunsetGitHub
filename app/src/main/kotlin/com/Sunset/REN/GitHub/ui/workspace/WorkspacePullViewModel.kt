package com.Sunset.REN.GitHub.ui.workspace

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.Sunset.REN.GitHub.data.github.GitHubRepositoryApiGateway
import com.Sunset.REN.GitHub.data.local.EncryptedSharedPreferencesTokenStore
import com.Sunset.REN.GitHub.data.local.SharedPreferencesCurrentAccountStore
import com.Sunset.REN.GitHub.data.workspace.AppInternalWorkspaceGateway
import com.Sunset.REN.GitHub.domain.repo.RepositoryContentItem
import com.Sunset.REN.GitHub.domain.workspace.WorkspaceProject
import com.Sunset.REN.GitHub.domain.workspace.WorkspaceRemoteBinding
import com.Sunset.REN.GitHub.domain.workspace.toNormalizedRepositoryPath
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WorkspacePullViewModel(application: Application) : AndroidViewModel(application) {
    private val workspaceGateway = AppInternalWorkspaceGateway(application)
    private val currentAccountStore = SharedPreferencesCurrentAccountStore(application)
    private val tokenStore = EncryptedSharedPreferencesTokenStore(application)

    private val _state = MutableLiveData(WorkspacePullUiState())
    val state: LiveData<WorkspacePullUiState> = _state

    fun loadInitialWorkspace() {
        viewModelScope.launch {
            val workspace = withContext(Dispatchers.IO) { workspaceGateway.listWorkspaces().firstOrNull() }
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

    fun preview(input: WorkspacePullInput) {
        val workspace = _state.value?.selectedWorkspace
        if (workspace == null) {
            appendLog("请先创建或选择工作区。")
            return
        }
        if (!input.hasRequiredRemote()) {
            appendLog("请填写仓库所有者和仓库名称。")
            return
        }
        viewModelScope.launch {
            runCatching {
                val token = loadAccessTokenOrThrow()
                val gateway = GitHubRepositoryApiGateway(token)
                val files = withContext(Dispatchers.IO) {
                    collectRemoteFiles(gateway, input, input.remotePath.toNormalizedRepositoryPath())
                }
                files
            }.onSuccess { files ->
                appendLog(
                    buildString {
                        appendLine("远端文件预览：${files.size} 个文本候选")
                        files.take(30).forEach { appendLine("- $it") }
                        if (files.size > 30) appendLine("… 还有 ${files.size - 30} 个")
                    }.trimEnd()
                )
            }.onFailure { error ->
                appendLog("预览失败：${error.message}")
            }
        }
    }

    fun pull(input: WorkspacePullInput) {
        val workspace = _state.value?.selectedWorkspace
        if (workspace == null) {
            appendLog("请先创建或选择工作区。")
            return
        }
        if (!input.hasRequiredRemote()) {
            appendLog("请填写仓库所有者和仓库名称。")
            return
        }
        viewModelScope.launch {
            runCatching {
                val token = loadAccessTokenOrThrow()
                val gateway = GitHubRepositoryApiGateway(token)
                withContext(Dispatchers.IO) { pullRemoteToWorkspace(gateway, workspace, input) }
            }.onSuccess { result ->
                appendLog(
                    buildString {
                        appendLine("远端同步本地完成")
                        appendLine("写入：${result.written} 个文件")
                        appendLine("跳过：${result.skipped} 个文件")
                        result.messages.take(30).forEach { appendLine(it) }
                        if (result.messages.size > 30) appendLine("… 还有 ${result.messages.size - 30} 条")
                    }.trimEnd()
                )
            }.onFailure { error ->
                appendLog("拉取失败：${error.message}")
            }
        }
    }

    private suspend fun pullRemoteToWorkspace(
        gateway: GitHubRepositoryApiGateway,
        workspace: WorkspaceProject,
        input: WorkspacePullInput
    ): WorkspacePullResult {
        val root = workspaceGateway.resolveWorkspaceRoot(workspace.id).canonicalFile.also { it.mkdirs() }
        val remoteRoot = input.remotePath.toNormalizedRepositoryPath()
        val localTarget = input.localTarget.toNormalizedRepositoryPath()
        val files = collectRemoteFiles(gateway, input, remoteRoot)
        var written = 0
        var skipped = 0
        val messages = mutableListOf<String>()
        files.forEach { remoteFilePath ->
            val relativeFromRemoteRoot = remoteFilePath.removePrefix(remoteRoot).trimStart('/')
            val localRelativePath = listOf(localTarget, relativeFromRemoteRoot)
                .filter { it.isNotBlank() }
                .joinToString("/")
                .toNormalizedRepositoryPath()
            val target = root.resolveInsideWorkspace(localRelativePath)
            if (target.exists() && !input.overwriteLocal) {
                skipped++
                messages += "跳过已存在：$localRelativePath"
                return@forEach
            }
            runCatching {
                val preview = gateway.getFilePreview(input.owner, input.repo, remoteFilePath, input.branch.ifBlank { "main" })
                target.parentFile?.mkdirs()
                target.writeText(preview.text)
                written++
                messages += "写入：$localRelativePath"
            }.onFailure { error ->
                skipped++
                messages += "跳过：$remoteFilePath - ${error.message}"
            }
        }
        workspaceGateway.bindRemote(
            workspace.id,
            WorkspaceRemoteBinding(
                owner = input.owner,
                repo = input.repo,
                branch = input.branch.ifBlank { "main" },
                remotePath = input.remotePath
            )
        )
        return WorkspacePullResult(written = written, skipped = skipped, messages = messages)
    }

    private suspend fun collectRemoteFiles(
        gateway: GitHubRepositoryApiGateway,
        input: WorkspacePullInput,
        path: String,
        depth: Int = 0
    ): List<String> {
        if (depth > MaxPullDepth) return emptyList()
        val ref = input.branch.ifBlank { "main" }
        val items = gateway.listContents(input.owner, input.repo, path, ref)
        return buildList {
            items.forEach { item ->
                when (item) {
                    is RepositoryContentItem.File -> add(item.path)
                    is RepositoryContentItem.Directory -> addAll(collectRemoteFiles(gateway, input, item.path, depth + 1))
                    is RepositoryContentItem.Unsupported -> Unit
                }
                if (size >= MaxPullFiles) return@buildList
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

    private fun File.resolveInsideWorkspace(relativePath: String): File {
        val root = canonicalFile
        val target = relativePath
            .toNormalizedRepositoryPath()
            .split('/')
            .filter { it.isNotBlank() }
            .fold(root) { current, segment -> current.resolve(segment) }
            .canonicalFile
        require(target.path == root.path || target.path.startsWith(root.path + File.separator)) {
            "文件路径越过工作区边界：$relativePath"
        }
        return target
    }

    private fun appendLog(message: String) {
        val current = _state.value.orEmpty()
        _state.postValue(current.copy(log = (current.log + "\n" + message).trim()))
    }

    private fun WorkspacePullUiState?.orEmpty(): WorkspacePullUiState = this ?: WorkspacePullUiState()

    private companion object {
        const val MaxPullDepth = 8
        const val MaxPullFiles = 300
    }
}

data class WorkspacePullUiState(
    val selectedWorkspace: WorkspaceProject? = null,
    val log: String = ""
)

data class WorkspacePullInput(
    val owner: String,
    val repo: String,
    val branch: String,
    val remotePath: String,
    val localTarget: String,
    val overwriteLocal: Boolean
) {
    fun hasRequiredRemote(): Boolean = owner.isNotBlank() && repo.isNotBlank()
}

private data class WorkspacePullResult(
    val written: Int,
    val skipped: Int,
    val messages: List<String>
)