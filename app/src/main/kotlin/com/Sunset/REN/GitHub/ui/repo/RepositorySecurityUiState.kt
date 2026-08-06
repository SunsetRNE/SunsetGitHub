package com.Sunset.REN.GitHub.ui.repo

import com.Sunset.REN.GitHub.domain.repo.RepositorySecurityAlert
import com.Sunset.REN.GitHub.domain.repo.RepositorySecuritySummary

data class RepositorySecurityAlertFilter(
    val alertType: String = RepositorySecurityUiState.AlertTypeDependabot,
    val alertState: String? = RepositorySecurityUiState.AlertStateOpen
)

data class RepositorySecurityUiState(
    val owner: String = "",
    val repo: String = "",
    val summary: RepositorySecuritySummary? = null,
    val alertFilter: RepositorySecurityAlertFilter = RepositorySecurityAlertFilter(),
    val alerts: List<RepositorySecurityAlert> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingAlerts: Boolean = false,
    val isLoadingMoreAlerts: Boolean = false,
    val errorMessage: String? = null,
    val alertsErrorMessage: String? = null,
    val hasMoreAlerts: Boolean = true,
    val loadedAlertPages: Int = 0,
    val isShowingStaleAlerts: Boolean = false
) {
    val isInitialLoad: Boolean
        get() = isLoading && summary == null

    val isAlertsEmpty: Boolean
        get() = !isLoadingAlerts && alerts.isEmpty() && alertsErrorMessage == null

    companion object {
        const val AlertTypeDependabot = "dependabot_alerts"
        const val AlertTypeCodeScanning = "code_scanning_alerts"
        const val AlertTypeSecretScanning = "secret_scanning_alerts"
        const val AlertStateOpen = "open"
        const val AlertStateFixed = "fixed"
        const val AlertStateDismissed = "dismissed"
        const val AlertStateResolved = "resolved"
        const val AlertStateAutoDismissed = "auto_dismissed"
    }
}