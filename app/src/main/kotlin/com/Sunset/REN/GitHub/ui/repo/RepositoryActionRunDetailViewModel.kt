package com.Sunset.REN.GitHub.ui.repo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.Sunset.REN.GitHub.data.github.GitHubRepositoryApiGateway
import com.Sunset.REN.GitHub.data.github.GitHubRepositoryFeatureUnavailableException
import com.Sunset.REN.GitHub.data.local.EncryptedSharedPreferencesTokenStore
import com.Sunset.REN.GitHub.data.local.RepositoryActionRunDetailCacheSnapshot
import com.Sunset.REN.GitHub.data.local.RepositoryCacheRepository
import com.Sunset.REN.GitHub.data.local.SharedPreferencesCurrentAccountStore
import com.Sunset.REN.GitHub.domain.repo.RepositoryActionRunLogPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

class RepositoryActionRunDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val currentAccountStore = SharedPreferencesCurrentAccountStore(application)
    private val tokenStore = EncryptedSharedPreferencesTokenStore(application)
    private val cacheRepository = RepositoryCacheRepository(application)

    private val _detailState = MutableLiveData(RepositoryActionRunDetailUiState())
    val detailState: LiveData<RepositoryActionRunDetailUiState> = _detailState

    private var accessToken: String = ""
    private var hasPrepared = false

    fun prepare(owner: String, repo: String, runId: Long) {
        if (hasPrepared) return
        hasPrepared = true
        _detailState.value = RepositoryActionRunDetailUiState(owner = owner, repo = repo, runId = runId)
        if (owner.isNotBlank() && repo.isNotBlank() && runId > 0L) {
            load()
        }
    }

    fun load() {
        val state = _detailState.value ?: return
        if (state.owner.isBlank() || state.repo.isBlank() || state.runId <= 0L) return
        viewModelScope.launch {
            val cachedSnapshot = withContext(Dispatchers.IO) {
                cacheRepository.getActionRunDetail(state.owner, state.repo, state.runId)
            }
            _detailState.value = state.copy(
                actionRun = cachedSnapshot?.actionRun ?: state.actionRun,
                artifacts = cachedSnapshot?.artifacts ?: state.artifacts,
                logPreview = cachedSnapshot?.logPreview ?: state.logPreview,
                refreshedAtMillis = cachedSnapshot?.refreshedAtMillis ?: state.refreshedAtMillis,
                isLoading = cachedSnapshot == null,
                isRefreshing = cachedSnapshot != null,
                isLoadingArtifacts = cachedSnapshot == null,
                isLoadingLogs = cachedSnapshot == null,
                errorMessage = null,
                artifactsErrorMessage = null,
                logsErrorMessage = null,
                unavailableMessage = null
            )
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) {
                val hasContent = _detailState.value?.actionRun != null
                _detailState.value = _detailState.value?.copy(
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = if (hasContent) null else NotSignedInMessage,
                    unavailableMessage = if (hasContent) NotSignedInMessage else null
                )
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    GitHubRepositoryApiGateway(token).getRepositoryActionRun(
                        owner = state.owner,
                        repo = state.repo,
                        runId = state.runId
                    )
                }
            }
            _detailState.value = result.fold(
                onSuccess = { run ->
                    _detailState.value?.copy(
                        actionRun = run,
                        refreshedAtMillis = System.currentTimeMillis(),
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingArtifacts = _detailState.value?.artifacts.isNullOrEmpty(),
                        isLoadingLogs = _detailState.value?.logPreview == null,
                        errorMessage = null,
                        artifactsErrorMessage = null,
                        logsErrorMessage = null,
                        unavailableMessage = null
                    )
                },
                onFailure = { error ->
                    val hasContent = _detailState.value?.actionRun != null
                    when (error) {
                        is GitHubRepositoryFeatureUnavailableException -> _detailState.value?.copy(
                            actionRun = if (hasContent) _detailState.value?.actionRun else null,
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = null,
                            unavailableMessage = error.message
                        )
                        else -> _detailState.value?.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = if (hasContent) null else error.message?.takeIf { it.isNotBlank() } ?: UnknownErrorMessage,
                            unavailableMessage = if (hasContent) error.message?.takeIf { it.isNotBlank() } ?: UnknownErrorMessage else null
                        )
                    }
                }
            )
            if (result.isSuccess) {
                cacheCurrentSnapshot(state.owner, state.repo, state.runId)
                loadArtifacts(owner = state.owner, repo = state.repo, runId = state.runId, token = token)
                if (_detailState.value?.logPreview == null) {
                    loadLogs(owner = state.owner, repo = state.repo, runId = state.runId, token = token)
                }
            }
        }
    }

    private suspend fun loadArtifacts(owner: String, repo: String, runId: Long, token: String): List<com.Sunset.REN.GitHub.domain.repo.RepositoryActionArtifact> {
        val result = withContext(Dispatchers.IO) {
            runCatching {
                GitHubRepositoryApiGateway(token).listRepositoryActionRunArtifacts(
                    owner = owner,
                    repo = repo,
                    runId = runId
                )
            }
        }
        _detailState.value = result.fold(
            onSuccess = { artifacts ->
                _detailState.value?.copy(
                    artifacts = artifacts,
                    isLoadingArtifacts = false,
                    artifactsErrorMessage = null
                )
            },
            onFailure = { error ->
                _detailState.value?.copy(
                    isLoadingArtifacts = false,
                    artifactsErrorMessage = error.message?.takeIf { it.isNotBlank() } ?: ArtifactsUnknownErrorMessage
                )
            }
        )
        if (result.isSuccess) {
            cacheCurrentSnapshot(owner, repo, runId)
        }
        return result.getOrDefault(emptyList())
    }

    private suspend fun loadLogs(owner: String, repo: String, runId: Long, token: String) {
        val result = withContext(Dispatchers.IO) {
            runCatching {
                GitHubRepositoryApiGateway(token)
                    .downloadRepositoryActionRunLogs(owner = owner, repo = repo, runId = runId)
                    .toLogPreview()
            }
        }
        _detailState.value = result.fold(
            onSuccess = { preview ->
                _detailState.value?.copy(
                    logPreview = preview,
                    isLoadingLogs = false,
                    logsErrorMessage = null
                )
            },
            onFailure = { error ->
                _detailState.value?.copy(
                    isLoadingLogs = false,
                    logsErrorMessage = error.message?.takeIf { it.isNotBlank() } ?: LogsUnknownErrorMessage
                )
            }
        )
        if (result.isSuccess) {
            cacheCurrentSnapshot(owner, repo, runId)
        }
    }

    private suspend fun cacheCurrentSnapshot(owner: String, repo: String, runId: Long) {
        val state = _detailState.value ?: return
        val actionRun = state.actionRun ?: return
        val snapshot = RepositoryActionRunDetailCacheSnapshot(
            actionRun = actionRun,
            artifacts = state.artifacts,
            logPreview = state.logPreview,
            refreshedAtMillis = System.currentTimeMillis()
        )
        withContext(Dispatchers.IO) {
            cacheRepository.cacheActionRunDetail(owner, repo, runId, snapshot)
        }
    }

    private fun ByteArray.toLogPreview(): RepositoryActionRunLogPreview {
        val entries = mutableListOf<LogEntryPreview>()
        var fileCount = 0
        var truncated = false
        ZipInputStream(ByteArrayInputStream(this)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) continue
                fileCount += 1
                val entryPreview = StringBuilder()
                var entryTruncated = false
                val chunk = ByteArray(8 * 1024)
                while (entryPreview.length < LogsEntryPreviewMaxChars) {
                    val bytesRead = zip.read(chunk)
                    if (bytesRead <= 0) break
                    entryPreview.append(String(chunk, 0, bytesRead, Charsets.UTF_8))
                }
                if (entryPreview.length >= LogsEntryPreviewMaxChars) {
                    entryTruncated = true
                }
                entries += LogEntryPreview(
                    name = entry.name,
                    text = entryPreview.toString().takeLast(LogsEntryPreviewMaxChars).trim(),
                    truncated = entryTruncated
                )
                zip.closeEntry()
            }
        }
        val selectedEntries = entries.take(LogsPreviewHeadFileCount) + entries.drop(LogsPreviewHeadFileCount).takeLast(LogsPreviewTailFileCount)
        val skippedFiles = (entries.size - selectedEntries.size).coerceAtLeast(0)
        val text = buildString {
            selectedEntries.forEachIndexed { index, entry ->
                if (index == LogsPreviewHeadFileCount && skippedFiles > 0) {
                    appendLine("== 已省略中间 $skippedFiles 个日志文件 ==")
                    appendLine()
                }
                appendLine("== ${entry.name}${if (entry.truncated) " · 单文件已截断" else ""} ==")
                appendLine(entry.text.ifBlank { "（该日志文件没有可显示的文本内容）" })
                appendLine()
            }
        }.trim()
        truncated = skippedFiles > 0 || selectedEntries.any { it.truncated }
        return RepositoryActionRunLogPreview(
            text = text.ifBlank { "日志归档中没有可显示的文本内容。" },
            fileCount = fileCount,
            truncated = truncated
        )
    }

    private data class LogEntryPreview(
        val name: String,
        val text: String,
        val truncated: Boolean
    )

    fun currentAuthorizationHeader(): String? {
        return accessToken.takeIf { it.isNotBlank() }?.let { token -> "Bearer $token" }
    }

    fun refreshLogs() {
        val state = _detailState.value ?: return
        if (state.owner.isBlank() || state.repo.isBlank() || state.runId <= 0L || state.isLoadingLogs) return
        viewModelScope.launch {
            _detailState.value = _detailState.value?.copy(isLoadingLogs = true, logsErrorMessage = null)
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) {
                _detailState.value = _detailState.value?.copy(
                    isLoadingLogs = false,
                    logsErrorMessage = if (state.logPreview == null) NotSignedInMessage else null
                )
                return@launch
            }
            loadLogs(owner = state.owner, repo = state.repo, runId = state.runId, token = token)
        }
    }

    private suspend fun ensureAccessToken(): String? {
        if (accessToken.isNotBlank()) return accessToken
        val account = withContext(Dispatchers.IO) { currentAccountStore.getCurrentAccount() } ?: return null
        val token = withContext(Dispatchers.IO) { tokenStore.getAccessToken(account.id) }?.takeIf { it.isNotBlank() } ?: return null
        accessToken = token
        return token
    }

    private companion object {
        const val NotSignedInMessage = "当前账号未登录或令牌已失效。"
        const val UnknownErrorMessage = "加载 Actions 运行详情时发生未知错误。"
        const val ArtifactsUnknownErrorMessage = "加载构建产物时发生未知错误。"
        const val LogsUnknownErrorMessage = "加载编译日志时发生未知错误。"
        const val LogsPreviewHeadFileCount = 2
        const val LogsPreviewTailFileCount = 6
        const val LogsEntryPreviewMaxChars = 8_000
    }
}