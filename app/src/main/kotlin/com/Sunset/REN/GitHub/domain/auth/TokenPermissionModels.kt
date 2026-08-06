package com.Sunset.REN.GitHub.domain.auth

data class TokenInspectionResult(
    val account: GitHubAccount,
    val scopes: List<String>,
    val checks: List<TokenPermissionCheck>
)

data class TokenPermissionCheck(
    val capability: TokenPermissionCapability,
    val status: TokenPermissionStatus,
    val detail: TokenPermissionDetail,
    val isCritical: Boolean = false
)

enum class TokenPermissionCapability {
    Repository,
    Workflow,
    Issues,
    Notifications,
    UserProfile
}

enum class TokenPermissionStatus {
    Granted,
    Missing,
    Unknown
}

enum class TokenPermissionDetail {
    Granted,
    Unknown,
    RepositoryMissing,
    WorkflowMissing,
    IssuesMissing,
    NotificationsMissing,
    UserProfileMissing
}
