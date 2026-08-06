package com.Sunset.REN.GitHub.ui.repo

data class RepositoryFileWriteConflictUiState(
    val targetPath: String,
    val existingName: String,
    val existingSizeBytes: Long
) {
    val dialogKey: String
        get() = "$targetPath#$existingSizeBytes"
}

enum class RepositoryFileWriteConflictResolution {
    Prompt,
    Overwrite,
    Rename
}