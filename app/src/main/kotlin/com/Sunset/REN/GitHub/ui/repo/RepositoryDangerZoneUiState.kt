package com.Sunset.REN.GitHub.ui.repo

import com.Sunset.REN.GitHub.data.github.html.RepositorySettingsSnapshot

data class RepositoryDangerZoneUiState(
    val owner: String = "",
    val repo: String = "",
    val snapshot: RepositorySettingsSnapshot? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleted: Boolean = false,
    val errorMessage: String? = null,
    val pendingMessage: String? = null,
    val sourceUrl: String? = null
) {
    val isInitialLoad: Boolean get() = snapshot == null && isLoading
}
