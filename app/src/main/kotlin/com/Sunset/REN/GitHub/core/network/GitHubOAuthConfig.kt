package com.Sunset.REN.GitHub.core.network

import com.Sunset.REN.GitHub.BuildConfig

/**
 * GitHub OAuth App 配置入口。
 *
 * Device Flow 只需要 client_id；不得在 APK 内置 client_secret。
 */
object GitHubOAuthConfig {
    val ClientId: String = BuildConfig.GITHUB_OAUTH_CLIENT_ID.trim()
}