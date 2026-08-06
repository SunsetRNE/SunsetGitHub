package com.Sunset.REN.GitHub.domain.profile

data class GitHubUserProfile(
    val id: Long,
    val login: String,
    val name: String?,
    val avatarUrl: String?,
    val bio: String?,
    val company: String?,
    val location: String?,
    val blog: String?,
    val email: String?,
    val twitterUsername: String?,
    val publicRepos: Int,
    val publicGists: Int,
    val followers: Int,
    val following: Int,
    val htmlUrl: String,
    val createdAt: String?,
    val updatedAt: String?
)
