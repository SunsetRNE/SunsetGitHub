package com.Sunset.REN.GitHub.ui.repo

data class RepositorySettingsScreenState(
    val owner: String,
    val repo: String,
    val fullName: String,
    val summary: String,
    val visibilityLabel: String,
    val defaultBranch: String,
    val permissionLabel: String,
    val stats: List<RepositorySettingsStatItem>,
    val basicItems: List<RepositorySettingsInfoItem>,
    val visibilityOption: RepositorySettingsVisibilityOption,
    val editableItems: List<RepositorySettingsEditableItem>,
    val featureItems: List<RepositorySettingsToggleItem>,
    val mergeItems: List<RepositorySettingsToggleItem>,
    val notices: List<String>,
    val sourceUrl: String,
    val canEdit: Boolean
)

data class RepositorySettingsStatItem(
    val label: String,
    val value: String
)
data class RepositorySettingsInfoItem(
    val key: RepositorySettingsInfoKey,
    val label: String,
    val value: String
)

enum class RepositorySettingsInfoKey {
    Source,
    Visibility,
    License,
    Language,
    CreatedAt,
    UpdatedAt,
    PushedAt
}


data class RepositorySettingsVisibilityOption(
    val selected: RepositorySettingsVisibility,
    val editable: Boolean
)

data class RepositorySettingsEditableItem(
    val key: RepositorySettingsEditableFieldKey,
    val label: String,
    val value: String,
    val helper: String? = null,
    val editable: Boolean
)

data class RepositorySettingsToggleItem(
    val key: RepositorySettingsToggleKey,
    val label: String,
    val checked: Boolean,
    val editable: Boolean,
    val description: String
)

enum class RepositorySettingsEditableFieldKey {
    Name,
    Description,
    Homepage,
    DefaultBranch
}

enum class RepositorySettingsVisibility {
    Public,
    Internal,
    Private
}

enum class RepositorySettingsToggleKey {
    HasIssues,
    HasProjects,
    HasWiki,
    HasDiscussions,
    AllowForking,
    Archived,
    AllowSquashMerge,
    AllowMergeCommit,
    AllowRebaseMerge,
    DeleteBranchOnMerge,
    AllowAutoMerge
}
