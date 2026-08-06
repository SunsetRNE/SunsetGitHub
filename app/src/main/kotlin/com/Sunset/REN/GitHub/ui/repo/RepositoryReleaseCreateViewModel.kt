package com.Sunset.REN.GitHub.ui.repo

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.Sunset.REN.GitHub.data.github.GitHubRepositoryApiGateway
import com.Sunset.REN.GitHub.data.local.EncryptedSharedPreferencesTokenStore
import com.Sunset.REN.GitHub.data.local.SharedPreferencesCurrentAccountStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RepositoryReleaseCreateViewModel(application: Application) : AndroidViewModel(application) {

    private val currentAccountStore = SharedPreferencesCurrentAccountStore(application)
    private val tokenStore = EncryptedSharedPreferencesTokenStore(application)

    private val _createState = MutableLiveData(RepositoryReleaseCreateUiState())
    val createState: LiveData<RepositoryReleaseCreateUiState> = _createState

    private var accessToken: String = ""
    private var hasPrepared = false

    fun prepare(owner: String, repo: String) {
        if (hasPrepared) return
        hasPrepared = true
        _createState.value = RepositoryReleaseCreateUiState(
            owner = owner,
            repo = repo,
            isLoadingBranches = owner.isNotBlank() && repo.isNotBlank(),
            isLoadingPreviousTag = owner.isNotBlank() && repo.isNotBlank()
        )
        loadBranches(owner, repo)
        loadPreviousTag(owner, repo)
    }

    fun selectBranch(branchName: String) {
        val state = _createState.value ?: return
        if (state.isSubmitting) return
        _createState.value = state.copy(selectedBranchName = branchName, errorMessage = null)
    }

    private fun loadBranches(owner: String, repo: String) {
        if (owner.isBlank() || repo.isBlank()) return
        viewModelScope.launch {
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) {
                _createState.value = _createState.value?.copy(
                    isLoadingBranches = false,
                    branchErrorMessage = NotSignedInMessage
                )
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                runCatching { GitHubRepositoryApiGateway(token).listRepositoryBranches(owner, repo) }
            }
            _createState.value = result.fold(
                onSuccess = { branches ->
                    val selectedBranch = branches.firstOrNull { it.isDefault }?.name
                        ?: branches.firstOrNull()?.name
                        ?: "main"
                    _createState.value?.copy(
                        branches = branches,
                        selectedBranchName = selectedBranch,
                        isLoadingBranches = false,
                        branchErrorMessage = null
                    )
                },
                onFailure = { error ->
                    _createState.value?.copy(
                        branches = emptyList(),
                        selectedBranchName = "main",
                        isLoadingBranches = false,
                        branchErrorMessage = error.message?.takeIf { it.isNotBlank() } ?: "无法读取仓库分支。"
                    )
                }
            )
        }
    }

    private fun loadPreviousTag(owner: String, repo: String) {
        if (owner.isBlank() || repo.isBlank()) return
        viewModelScope.launch {
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) {
                _createState.value = _createState.value?.copy(isLoadingPreviousTag = false)
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    GitHubRepositoryApiGateway(token)
                        .listRepositoryReleases(owner = owner, repo = repo, page = 1, perPage = 1)
                        .firstOrNull()
                        ?.tagName
                        ?.takeIf { it.isNotBlank() }
                }
            }
            _createState.value = _createState.value?.copy(
                previousTagName = result.getOrNull(),
                isLoadingPreviousTag = false
            )
        }
    }

    fun addAsset(uri: Uri, fileName: String, mimeType: String, sizeBytes: Long) {
        val state = _createState.value ?: return
        if (state.isSubmitting) return
        val asset = RepositoryReleaseAssetDraft(
            uri = uri,
            fileName = fileName,
            mimeType = mimeType,
            sizeBytes = sizeBytes
        )
        _createState.value = state.copy(assets = state.assets + asset, errorMessage = null)
    }

    fun removeAsset(index: Int) {
        val state = _createState.value ?: return
        if (state.isSubmitting) return
        if (index !in state.assets.indices) return
        _createState.value = state.copy(
            assets = state.assets.filterIndexed { assetIndex, _ -> assetIndex != index },
            errorMessage = null
        )
    }

    fun submit(
        tagName: String,
        targetCommitish: String,
        name: String,
        body: String,
        prerelease: Boolean,
        draft: Boolean,
        makeLatest: Boolean
    ) {
        val state = _createState.value ?: return
        if (tagName.isBlank()) return
        if (state.isSubmitting) return
        if (state.owner.isBlank() || state.repo.isBlank()) return
        viewModelScope.launch {
            _createState.value = state.copy(
                isSubmitting = true,
                statusMessage = CreatingReleaseMessage,
                errorMessage = null
            )
            val token = ensureAccessToken()
            if (token.isNullOrBlank()) {
                _createState.value = _createState.value?.copy(
                    isSubmitting = false,
                    statusMessage = null,
                    errorMessage = NotSignedInMessage
                )
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val gateway = GitHubRepositoryApiGateway(token)
                    val created = gateway.createRelease(
                        owner = state.owner,
                        repo = state.repo,
                        tagName = tagName,
                        targetCommitish = targetCommitish.takeIf { it.isNotBlank() },
                        name = name,
                        body = body,
                        draft = draft,
                        prerelease = prerelease,
                        makeLatest = makeLatest
                    )
                    if (state.assets.isNotEmpty()) {
                        val uploadUrl = created.uploadUrl?.takeIf { it.isNotBlank() }
                            ?: throw IllegalStateException("GitHub 未返回发布附件上传地址。")
                        state.assets.forEachIndexed { index, asset ->
                            _createState.postValue(
                                state.copy(
                                    isSubmitting = true,
                                    statusMessage = "正在上传附件 ${index + 1}/${state.assets.size}…",
                                    errorMessage = null
                                )
                            )
                            val bytes = getApplication<Application>().contentResolver
                                .openInputStream(asset.uri)
                                ?.use { input -> input.readBytesLimited(ReleaseAssetMaxBytes) }
                                ?: throw IllegalStateException("无法读取附件：${asset.fileName}")
                            gateway.uploadReleaseAsset(
                                uploadUrl = uploadUrl,
                                fileName = asset.fileName,
                                mimeType = asset.mimeType,
                                fileBytes = bytes
                            )
                        }
                    }
                    created
                }
            }
            _createState.value = result.fold(
                onSuccess = { created ->
                    _createState.value?.copy(
                        isSubmitting = false,
                        statusMessage = null,
                        errorMessage = null,
                        createdTagName = created.tagName.ifBlank { tagName }
                    )
                },
                onFailure = { error ->
                    _createState.value?.copy(
                        isSubmitting = false,
                        statusMessage = null,
                        errorMessage = error.message?.takeIf { it.isNotBlank() } ?: UnknownErrorMessage
                    )
                }
            )
        }
    }

    private fun java.io.InputStream.readBytesLimited(limitBytes: Long): ByteArray {
        val limit = limitBytes.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val buffer = ByteArray(8 * 1024)
        val output = java.io.ByteArrayOutputStream()
        var total = 0
        while (true) {
            val read = read(buffer)
            if (read == -1) break
            total += read
            if (total > limit) {
                throw IllegalStateException("附件超过 100 MiB，暂不支持在应用内直接上传。")
            }
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private suspend fun ensureAccessToken(): String? {
        if (accessToken.isNotBlank()) return accessToken
        val account = withContext(Dispatchers.IO) {
            currentAccountStore.getCurrentAccount()
        } ?: return null
        val token = withContext(Dispatchers.IO) {
            tokenStore.getAccessToken(account.id)
        }?.takeIf { it.isNotBlank() } ?: return null
        accessToken = token
        return token
    }

    private companion object {
        const val NotSignedInMessage = "当前账号未登录或令牌已失效。"
        const val UnknownErrorMessage = "创建发布版本时发生未知错误。"
        const val CreatingReleaseMessage = "正在创建发布版本…"
        const val ReleaseAssetMaxBytes = 100L * 1024L * 1024L
    }
}