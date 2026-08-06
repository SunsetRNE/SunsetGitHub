package com.Sunset.REN.GitHub.ui.repo

data class RepositoryFileUploadUiState(
    val isSubmitting: Boolean = false,
    val owner: String = "",
    val repo: String = "",
    val sourceUri: String = "",
    val displayName: String = "",
    val sourceSizeBytes: Long? = null,
    val targetPath: String = "",
    val errorMessage: String? = null,
    val pendingConflict: RepositoryFileWriteConflictUiState? = null,
    val uploadedPath: String = "",
    val submitSuccess: Boolean = false
) {
    val canSubmit: Boolean
        get() = !isSubmitting && owner.isNotBlank() && repo.isNotBlank() && sourceUri.isNotBlank() && targetPath.trim().isNotBlank()
}