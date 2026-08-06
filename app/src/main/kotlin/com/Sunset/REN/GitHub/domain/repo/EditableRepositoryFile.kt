package com.Sunset.REN.GitHub.domain.repo

data class EditableRepositoryFile(
    val name: String,
    val path: String,
    val sha: String,
    val sizeBytes: Long,
    val text: String,
    val htmlUrl: String? = null
)
