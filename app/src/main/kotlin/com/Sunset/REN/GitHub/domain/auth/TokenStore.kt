package com.Sunset.REN.GitHub.domain.auth

/**
 * Token 访问边界。
 *
 * V0.1 要求：Android 本地不得明文存储 GitHub access token，
 * 任何 token 持久化实现都必须经由该抽象。
 */
interface TokenStore {
    suspend fun saveAccessToken(accountId: Long, token: String)

    suspend fun getAccessToken(accountId: Long): String?

    suspend fun clearAccessToken(accountId: Long)

    suspend fun clearAll()
}
