package com.Sunset.REN.GitHub.ui.repo

import android.app.Application
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
import com.Sunset.REN.GitHub.domain.repo.toDisplayMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RepositoryFileEditViewModel(application: Application) : AndroidViewModel(application) {

    private val currentAccountStore = SharedPreferencesCurrentAccountStore(application)
    private val tokenStore = EncryptedSharedPreferencesTokenStore(application)
    private val preSubmitValidator = DefaultPreSubmitValidator()

    private val _editState = MutableLiveData(RepositoryFileEditUiState())
    val editState: LiveData<RepositoryFileEditUiState> = _editState

    private var accessToken: String = ""
    private var loadedOwner: String = ""
    private var loadedRepo: String = ""
    private var loadedPath: String = ""
    private var loadedBranch: String? = null
    private var createMode: Boolean = false

    fun prepareNewFile(
        owner: String,
        repo: String,
        nameHint: String,
        branch: String? = null,
        initialContent: String = ""
    ) {
        if (owner.isBlank() || repo.isBlank()) {
            _editState.value = RepositoryFileEditUiState(
                fileName = nameHint,
                errorMessage = getApplication<Application>().getString(R.string.repository_file_edit_missing_args),
                isCreateMode = true
            )
            return
        }
        loadedOwner = owner
        loadedRepo = repo
        loadedPath = nameHint
        loadedBranch = branch
        createMode = true
        viewModelScope.launch {
            val token = loadAccessTokenOrNull()
            if (token.isNullOrBlank()) {
                _editState.value = RepositoryFileEditUiState(
                    fileName = nameHint,
                    filePath = nameHint,
                    errorMessage = getApplication<Application>().getString(R.string.repository_file_edit_signed_out),
                    isCreateMode = true
                )
                return@launch
            }
            accessToken = token
            _editState.value = RepositoryFileEditUiState(
                isLoading = false,
                fileName = nameHint.ifBlank { getApplication<Application>().getString(R.string.repository_file_create_title) },
                filePath = nameHint,
                content = initialContent,
                originalContent = initialContent,
                originalSha = "",
                isCreateMode = true
            )
        }
    }

    fun loadFile(owner: String, repo: String, path: String, nameHint: String) {
        if (owner.isBlank() || repo.isBlank() || path.isBlank()) {
            _editState.value = RepositoryFileEditUiState(
                fileName = nameHint,
                filePath = path,
                errorMessage = getApplication<Application>().getString(R.string.repository_file_edit_missing_args)
            )
            return
        }
        loadedOwner = owner
        loadedRepo = repo
        loadedPath = path
        loadedBranch = null
        createMode = false
        viewModelScope.launch {
            _editState.value = RepositoryFileEditUiState(
                isLoading = true,
                fileName = nameHint.ifBlank { path.substringAfterLast('/') },
                filePath = path
            )
            val token = loadAccessTokenOrNull()
            if (token.isNullOrBlank()) {
                _editState.value = _editState.value?.copy(
                    isLoading = false,
                    errorMessage = getApplication<Application>().getString(R.string.repository_file_edit_signed_out)
                )
                return@launch
            }
            accessToken = token
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    GitHubRepositoryApiGateway(token).getEditableFile(owner, repo, path)
                }
            }
            _editState.value = result.fold(
                onSuccess = { file ->
                    RepositoryFileEditUiState(
                        fileName = file.name,
                        filePath = file.path,
                        content = file.text,
                        originalContent = file.text,
                        originalSha = file.sha
                    )
                },
                onFailure = { error ->
                    RepositoryFileEditUiState(
                        fileName = nameHint.ifBlank { path.substringAfterLast('/') },
                        filePath = path,
                        errorMessage = error.message
                    )
                }
            )
        }
    }

    fun updateTargetPath(targetPath: String) {
        if (!createMode) return
        loadedPath = targetPath
        _editState.value = _editState.value?.copy(
            fileName = targetPath.trim().ifBlank { getApplication<Application>().getString(R.string.repository_file_create_title) },
            filePath = targetPath,
            errorMessage = null,
            pendingConflict = null,
            submitSuccess = false
        )
    }

    fun updateContent(content: String) {
        _editState.value = _editState.value?.copy(
            content = content,
            errorMessage = null,
            pendingConflict = null,
            submitSuccess = false
        )
    }

    fun submit(
        message: String,
        conflictResolution: RepositoryFileWriteConflictResolution = RepositoryFileWriteConflictResolution.Prompt
    ) {
        val state = _editState.value ?: return
        val targetPath = loadedPath.trim()
        if (!state.canSubmit || accessToken.isBlank() || loadedOwner.isBlank() || loadedRepo.isBlank() || (targetPath.isBlank() && !createMode)) return
        val commitMessage = message.ifBlank {
            getApplication<Application>().getString(R.string.repository_file_edit_default_commit_message, state.fileName.ifBlank { targetPath })
        }
        val session = FileWriteSession(
            repositoryId = "$loadedOwner/$loadedRepo",
            owner = loadedOwner,
            repo = loadedRepo,
            targetPath = targetPath,
            originalPath = if (createMode) null else loadedPath,
            operation = if (createMode) FileWriteOperation.Create else FileWriteOperation.Edit,
            commitMessage = commitMessage,
            baseSha = state.originalSha.takeIf { it.isNotBlank() },
            selectedFiles = emptyList(),
            content = state.content,
            capability = FileCapability.EditableText
        )
        val validationResult = preSubmitValidator.validate(session)
        if (!validationResult.canSubmit) {
            _editState.value = state.copy(
                isSubmitting = false,
                errorMessage = validationResult.toDisplayMessage(),
                submitSuccess = false,
                pendingConflict = null
            )
            return
        }
        viewModelScope.launch {
            _editState.value = state.copy(isSubmitting = true, errorMessage = null, submitSuccess = false, pendingConflict = null)
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val gateway = GitHubRepositoryApiGateway(accessToken)
                    if (createMode) {
                        val targetInfo = gateway.getWriteTarget(loadedOwner, loadedRepo, targetPath)
                        if (targetInfo?.isDirectory == true) {
                            throw IllegalStateException(
                                getApplication<Application>().getString(R.string.repository_file_upload_target_directory_conflict, targetPath)
                            )
                        }
                        if (targetInfo != null && targetInfo.sha.isNotBlank() && conflictResolution == RepositoryFileWriteConflictResolution.Prompt) {
                            return@runCatching EditSubmitOutcome.Conflict(
                                RepositoryFileWriteConflictUiState(
                                    targetPath = targetPath,
                                    existingName = targetInfo.name,
                                    existingSizeBytes = targetInfo.sizeBytes
                                )
                            )
                        }
                        if (targetInfo != null && targetInfo.sha.isNotBlank()) {
                            gateway.updateFileContent(
                                owner = loadedOwner,
                                repo = loadedRepo,
                                path = targetPath,
                                message = commitMessage,
                                content = state.content,
                                sha = targetInfo.sha,
                                branch = loadedBranch
                            )
                        } else {
                            gateway.createFileContent(
                                owner = loadedOwner,
                                repo = loadedRepo,
                                path = targetPath,
                                message = commitMessage,
                                content = state.content,
                                branch = loadedBranch
                            )
                        }
                    } else {
                        gateway.updateFileContent(
                            owner = loadedOwner,
                            repo = loadedRepo,
                            path = targetPath,
                            message = commitMessage,
                            content = state.content,
                            sha = state.originalSha,
                            branch = loadedBranch
                        )
                    }
                    EditSubmitOutcome.Submitted
                }
            }
            _editState.value = result.fold(
                onSuccess = { outcome ->
                    when (outcome) {
                        is EditSubmitOutcome.Conflict -> state.copy(
                            isSubmitting = false,
                            errorMessage = null,
                            pendingConflict = outcome.conflict,
                            submitSuccess = false,
                            filePath = targetPath,
                            fileName = targetPath.substringAfterLast('/')
                        )

                        EditSubmitOutcome.Submitted -> state.copy(
                            isSubmitting = false,
                            submitSuccess = true,
                            errorMessage = null,
                            pendingConflict = null,
                            fileName = targetPath.substringAfterLast('/'),
                            filePath = targetPath,
                            originalContent = state.content
                        )
                    }
                },
                onFailure = { error ->
                    state.copy(
                        isSubmitting = false,
                        errorMessage = error.message?.takeIf { it.isNotBlank() }
                            ?: getApplication<Application>().getString(R.string.repository_file_submit_unknown_error),
                        submitSuccess = false,
                        pendingConflict = null
                    )
                }
            )
        }
    }

    fun deleteFile(message: String = "") {
        val state = _editState.value ?: return
        val targetPath = loadedPath.trim()
        if (accessToken.isBlank() || loadedOwner.isBlank() || loadedRepo.isBlank() || createMode || targetPath.isBlank() || state.originalSha.isBlank()) return
        val commitMessage = message.ifBlank {
            getApplication<Application>().getString(R.string.repository_file_delete_default_commit_message, state.fileName.ifBlank { targetPath })
        }
        val validationResult = preSubmitValidator.validate(
            FileWriteSession(
                repositoryId = "$loadedOwner/$loadedRepo",
                owner = loadedOwner,
                repo = loadedRepo,
                targetPath = targetPath,
                originalPath = loadedPath,
                operation = FileWriteOperation.Edit,
                commitMessage = commitMessage,
                baseSha = state.originalSha,
                selectedFiles = emptyList(),
                content = null,
                capability = FileCapability.EditableText
            )
        )
        if (!validationResult.canSubmit) {
            _editState.value = state.copy(errorMessage = validationResult.toDisplayMessage())
            return
        }
        viewModelScope.launch {
            _editState.value = state.copy(isDeleting = true, errorMessage = null, submitSuccess = false, pendingConflict = null)
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    GitHubRepositoryApiGateway(accessToken).deleteFileContent(
                        owner = loadedOwner,
                        repo = loadedRepo,
                        path = targetPath,
                        message = commitMessage,
                        sha = state.originalSha,
                        branch = loadedBranch
                    )
                }
            }
            _editState.value = result.fold(
                onSuccess = {
                    state.copy(isDeleting = false, deleteSuccess = true, errorMessage = null, pendingConflict = null)
                },
                onFailure = { error ->
                    state.copy(
                        isDeleting = false,
                        errorMessage = error.message?.takeIf { it.isNotBlank() }
                            ?: getApplication<Application>().getString(R.string.repository_file_submit_unknown_error),
                        pendingConflict = null,
                        submitSuccess = false
                    )
                }
            )
        }
    }

    fun clearSubmitSuccess() {
        _editState.value = _editState.value?.copy(submitSuccess = false)
    }

    fun clearDeleteSuccess() {
        _editState.value = _editState.value?.copy(deleteSuccess = false)
    }

    fun clearPendingConflict() {
        _editState.value = _editState.value?.copy(pendingConflict = null)
    }

    private suspend fun loadAccessTokenOrNull(): String? {
        val account = withContext(Dispatchers.IO) {
            currentAccountStore.getCurrentAccount()
        } ?: return null
        return withContext(Dispatchers.IO) {
            tokenStore.getAccessToken(account.id)
        }?.takeIf { it.isNotBlank() }
    }

    private sealed interface EditSubmitOutcome {
        data object Submitted : EditSubmitOutcome
        data class Conflict(val conflict: RepositoryFileWriteConflictUiState) : EditSubmitOutcome
    }
}