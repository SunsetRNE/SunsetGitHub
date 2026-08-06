package com.Sunset.REN.GitHub.domain.repo

/**
 * Parameters for creating a repository under the current GitHub account.
 * The first app surface intentionally mirrors the GitHub web "New repository" essentials.
 */
data class RepositoryCreateRequest(
    val name: String,
    val description: String?,
    val homepage: String?,
    val isPrivate: Boolean,
    val autoInit: Boolean,
    val gitignoreTemplate: String?,
    val licenseTemplate: String?,
    val hasIssues: Boolean,
    val hasProjects: Boolean,
    val hasWiki: Boolean
)
