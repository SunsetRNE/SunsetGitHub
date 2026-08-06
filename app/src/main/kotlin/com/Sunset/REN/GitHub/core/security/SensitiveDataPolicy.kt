package com.Sunset.REN.GitHub.core.security

/**
 * V0.1 敏感数据边界。
 */
object SensitiveDataPolicy {
    const val DoNotLogAccessToken = true
    const val DoNotLogPrivateRepositoryContent = true
    const val ClearAccountDataOnLogout = true
}