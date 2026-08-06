package com.Sunset.REN.GitHub.domain.auth

/**
 * 登录成功后用于本地数据分区的 GitHub 账号信息。
 */
data class GitHubAccount(
    val id: Long,
    val login: String,
    val avatarUrl: String? = null,
    val name: String? = null
)
