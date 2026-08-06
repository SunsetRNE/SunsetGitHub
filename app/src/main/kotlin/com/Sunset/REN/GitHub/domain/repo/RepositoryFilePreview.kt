package com.Sunset.REN.GitHub.domain.repo

data class RepositoryFilePreview(
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val text: String,
    val htmlUrl: String? = null
)
