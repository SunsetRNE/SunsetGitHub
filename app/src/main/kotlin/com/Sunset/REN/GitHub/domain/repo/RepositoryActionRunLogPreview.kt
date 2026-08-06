package com.Sunset.REN.GitHub.domain.repo

/** Preview text extracted from a GitHub Actions run log archive. */
data class RepositoryActionRunLogPreview(
    val text: String,
    val fileCount: Int,
    val truncated: Boolean
)
