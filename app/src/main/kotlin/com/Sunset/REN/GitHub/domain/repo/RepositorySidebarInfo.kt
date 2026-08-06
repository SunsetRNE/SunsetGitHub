package com.Sunset.REN.GitHub.domain.repo

data class RepositorySidebarInfo(
    val releases: List<RepositoryRelease> = emptyList(),
    val contributors: List<RepositoryContributor> = emptyList(),
    val languages: List<RepositoryLanguage> = emptyList(),
    val error: String? = null
)

data class RepositoryRelease(
    val name: String,
    val tagName: String,
    val htmlUrl: String?,
    val publishedAt: String? = null,
    val createdAt: String? = null,
    val authorLogin: String? = null,
    val isLatest: Boolean = false,
    val isPrerelease: Boolean = false,
    val isDraft: Boolean = false,
    val body: String? = null,
    val bodySummary: String? = null,
    val targetCommitish: String? = null,
    val uploadUrl: String? = null,
    val zipballUrl: String? = null,
    val tarballUrl: String? = null,
    val assets: List<RepositoryReleaseAsset> = emptyList()
)

data class RepositoryReleaseAsset(
    val id: Long,
    val name: String,
    val label: String?,
    val contentType: String?,
    val sizeBytes: Long,
    val downloadCount: Int,
    val browserDownloadUrl: String?,
    val createdAt: String?,
    val updatedAt: String?
)

data class RepositoryContributor(
    val login: String,
    val contributions: Int,
    val htmlUrl: String?
)

data class RepositoryLanguage(
    val name: String,
    val bytes: Long,
    val percentage: Int
)