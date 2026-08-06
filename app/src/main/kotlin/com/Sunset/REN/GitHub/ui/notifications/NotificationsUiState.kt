package com.Sunset.REN.GitHub.ui.notifications

import com.Sunset.REN.GitHub.domain.notification.GitHubNotification

data class NotificationsUiState(
    val all: Boolean = false,
    val notifications: List<GitHubNotification> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val hasMore: Boolean = true,
    val loadedPages: Int = 0
) {
    val isInitialLoad: Boolean
        get() = isLoading && notifications.isEmpty()

    val isEmpty: Boolean
        get() = !isLoading && notifications.isEmpty() && errorMessage == null
}