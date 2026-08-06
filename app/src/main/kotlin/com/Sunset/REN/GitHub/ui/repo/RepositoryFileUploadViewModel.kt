package com.Sunset.REN.GitHub.ui.repo

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.data.github.GitHubRepositoryApiGateway
import com.Sunset.REN.GitHub.data.local.EncryptedSharedPreferencesTokenStore
import com.Sunset.REN.GitHub.data.local.SharedPreferencesCurrentAccountStore
import com.Sunset.REN.GitHub.domain.repo.DefaultPreSubmitValidator
import com.Sunset.REN.GitHub.domain.repo.FileCapability
import com.Sunset.REN.GitHub.domain.repo.FileWriteOperation
import com.Sunset.REN.GitHub.domain.repo.FileWriteSession
import com.Sunset.REN.GitHub.domain.repo.GitHubContentApiLimits
import com.Sunset.REN.GitHub.domain.repo.RepositoryUploadTargetPath
import com.Sunset.REN.GitHub.domain.repo.SelectedRepositoryWriteFile
import com.Sunset.REN.GitHub.domain.repo.toDisplayMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RepositoryFileUploadViewModel(application: Application) : AndroidViewModel(application) {

    private val currentAccountStore = SharedPreferencesCurrentAccountStore(application)
    private val tokenStore = EncryptedSharedPreferencesTokenStore(application)
    private val preSubmitValidator = DefaultPreSubmitValidator()

    private val _uploadState = MutableLiveData(RepositoryFileUploadUiState())
    val uploadState: LiveData<RepositoryFileUploadUiState> = _uploadState

    private var accessToken: String = ""

    fun prepare(owner: String, repo: String, sourceUri: String, displayName: String) {
        _uploadState.value = RepositoryFileUploadUiState(
            owner = owner,
            repo = repo,
            sourceUri = sourceUri,
            displayName = displayName,
            sourceSizeBytes = getContentLength(Uri.parse(sourceUri)),
            targetPath = RepositoryUploadTargetPath.defaultDirectoryForDisplayName(displayName)
        )
        viewModelScope.launch {
            val token = loadAccessTokenOrNull()
            if (token.isNullOrBlank()) {
                _uploadState.value = _uploadState.value?.copy(
                    errorMessage = getApplication<Application>().getString(R.string.repository_file_edit_signed_out)
                )
                return@launch
            }
            accessToken = token
        }
    }

    fun updateTargetPath(targetPath: String) {
        _uploadState.value = _uploadState.value?.copy(
            targetPath = targetPath,
            errorMessage = null,
            pendingConflict = null,
            uploadedPath = "",
            submitSuccess = false
        )
    }

    fun submit(
        message: String,
        conflictResolution: RepositoryFileWriteConflictResolution = RepositoryFileWriteConflictResolution.Prompt
    ) {
        val state = _uploadState.value ?: return
        val targetPath = RepositoryUploadTargetPath.resolve(state.targetPath, state.displayName)
        val commitMessage = message.ifBlank {
            getApplication<Application>().getString(R.string.repository_file_edit_default_commit_message, targetPath)
        }
        val sourceUri = Uri.parse(state.sourceUri)
        val sizeBytes = getContentLength(sourceUri)
        val session = FileWriteSession(
            repositoryId = "${state.owner}/${state.repo}",
            owner = state.owner,
            repo = state.repo,
            targetPath = targetPath,
            operation = FileWriteOperation.Upload,
            commitMessage = commitMessage,
            selectedFiles = listOf(
                SelectedRepositoryWriteFile(
                    displayName = state.displayName,
                    uri = state.sourceUri,
                    sizeBytes = sizeBytes
                )
            ),
            capability = FileCapability.UploadOnly
        )
        val validationResult = preSubmitValidator.validate(session)
        if (!validationResult.canSubmit) {
            _uploadState.value = state.copy(
                errorMessage = validationResult.toDisplayMessage(),
                pendingConflict = null,
                uploadedPath = "",
                submitSuccess = false,
                targetPath = targetPath
            )
            return
        }
        if (accessToken.isBlank()) {
            _uploadState.value = state.copy(
                errorMessage = getApplication<Application>().getString(R.string.repository_file_edit_signed_out),
                pendingConflict = null,
                uploadedPath = "",
                submitSuccess = false,
                targetPath = targetPath
            )
            return
        }
        viewModelScope.launch {
            _uploadState.value = state.copy(
                isSubmitting = true,
                errorMessage = null,
                pendingConflict = null,
                uploadedPath = "",
                submitSuccess = false,
                targetPath = targetPath
            )
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val gateway = GitHubRepositoryApiGateway(accessToken)
                    val targetInfo = gateway.getWriteTarget(state.owner, state.repo, targetPath)
                    if (targetInfo?.isDirectory == true) {
                        throw IllegalStateException(
                            getApplication<Application>().getString(
                                R.string.repository_file_upload_target_directory_conflict,
                                targetPath
                            )
                        )
                    }
                    if (targetInfo != null && targetInfo.sha.isNotBlank() && conflictResolution == RepositoryFileWriteConflictResolution.Prompt) {
                        return@runCatching UploadSubmitOutcome.Conflict(
                            RepositoryFileWriteConflictUiState(
                                targetPath = targetPath,
                                existingName = targetInfo.name,
                                existingSizeBytes = targetInfo.sizeBytes
                            )
                        )
                    }
                    val bytes = getApplication<Application>().contentResolver.openInputStream(sourceUri)?.use { input ->
                        input.readBytesLimited(
                            GitHubContentApiLimits.RecommendedDirectUploadMaxBytes,
                            getApplication<Application>().getString(R.string.repository_file_upload_large_file_blocked)
                        )
                    } ?: throw IllegalStateException(
                        getApplication<Application>().getString(R.string.repository_file_upload_permission_failed)
                    )
                    if (targetInfo != null && targetInfo.sha.isNotBlank()) {
                        gateway.updateFileContentBytes(
                            owner = state.owner,
                            repo = state.repo,
                            path = targetPath,
                            message = commitMessage,
                            contentBytes = bytes,
                            sha = targetInfo.sha
                        )
                    } else {
                        gateway.createFileContentBytes(
                            owner = state.owner,
                            repo = state.repo,
                            path = targetPath,
                            message = commitMessage,
                            contentBytes = bytes
                        )
                    }
                    UploadSubmitOutcome.Submitted
                }
            }
            _uploadState.value = result.fold(
                onSuccess = { outcome ->
                    when (outcome) {
                        is UploadSubmitOutcome.Conflict -> state.copy(
                            isSubmitting = false,
                            errorMessage = null,
                            pendingConflict = outcome.conflict,
                            uploadedPath = "",
                            submitSuccess = false,
                            targetPath = targetPath
                        )

                        UploadSubmitOutcome.Submitted -> state.copy(
                            isSubmitting = false,
                            errorMessage = null,
                            pendingConflict = null,
                            uploadedPath = targetPath,
                            submitSuccess = true,
                            targetPath = targetPath
                        )
                    }
                },
                onFailure = { error ->
                    state.copy(
                        isSubmitting = false,
                        errorMessage = error.message?.takeIf { it.isNotBlank() }
                            ?: getApplication<Application>().getString(R.string.repository_file_submit_unknown_error),
                        pendingConflict = null,
                        uploadedPath = "",
                        submitSuccess = false,
                        targetPath = targetPath
                    )
                }
            )
        }
    }

    fun clearSubmitSuccess() {
        _uploadState.value = _uploadState.value?.copy(
            uploadedPath = "",
            submitSuccess = false
        )
    }

    fun clearPendingConflict() {
        _uploadState.value = _uploadState.value?.copy(pendingConflict = null)
    }

    private fun getContentLength(uri: Uri): Long? {
        return runCatching {
            getApplication<Application>().contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                descriptor.statSize.takeIf { it >= 0L }
            }
        }.getOrNull()
    }

    private fun java.io.InputStream.readBytesLimited(limitBytes: Long, limitMessage: String): ByteArray {
        val limit = limitBytes.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val buffer = ByteArray(8 * 1024)
        val output = java.io.ByteArrayOutputStream()
        var total = 0
        while (true) {
            val read = read(buffer)
            if (read <= 0) break
            total += read
            if (total > limit) {
                throw IllegalStateException(limitMessage)
            }
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private suspend fun loadAccessTokenOrNull(): String? {
        val account = withContext(Dispatchers.IO) {
            currentAccountStore.getCurrentAccount()
        } ?: return null
        return withContext(Dispatchers.IO) {
            tokenStore.getAccessToken(account.id)
        }?.takeIf { it.isNotBlank() }
    }

    private sealed interface UploadSubmitOutcome {
        data object Submitted : UploadSubmitOutcome
        data class Conflict(val conflict: RepositoryFileWriteConflictUiState) : UploadSubmitOutcome
    }
}