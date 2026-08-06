package com.Sunset.REN.GitHub.domain.repo

data class RepositoryFileWriteTarget(
    val path: String,
    val name: String,
    val sha: String,
    val sizeBytes: Long,
    val isDirectory: Boolean = false
)
