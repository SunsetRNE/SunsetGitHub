package com.Sunset.REN.GitHub.ui.repo

data class RepositoryFileEditUiState(
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val isDeleting: Boolean = false,
    val fileName: String = "",
    val filePath: String = "",
    val content: String = "",
    val originalContent: String = "",
    val originalSha: String = "",
    val errorMessage: String? = null,
    val pendingConflict: RepositoryFileWriteConflictUiState? = null,
    val submitSuccess: Boolean = false,
    val deleteSuccess: Boolean = false,
    val isCreateMode: Boolean = false
) {
    val canSubmit: Boolean
        get() = !isLoading && !isSubmitting && !isDeleting && (originalSha.isNotBlank() || isCreateMode) && (!isCreateMode || filePath.trim().isNotBlank())

    val hasUnsavedChanges: Boolean
        get() = !isLoading && !isSubmitting && !isDeleting && content != originalContent
}