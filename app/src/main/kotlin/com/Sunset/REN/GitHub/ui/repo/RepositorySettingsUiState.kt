package com.Sunset.REN.GitHub.ui.repo

import com.Sunset.REN.GitHub.data.github.html.RepositorySettingsSnapshot

data class RepositorySettingsUiState(
    val owner: String = "",
    val repo: String = "",
    val screen: RepositorySettingsScreenState? = null,
    val snapshot: RepositorySettingsSnapshot? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val sourceUrl: String? = null,
    val pendingMessage: String? = null,
    val refreshedAtMillis: Long? = null,
    val isShowingCachedContent: Boolean = false
) {
    val isInitialLoad: Boolean get() = isLoading && screen == null && errorMessage == null
}
