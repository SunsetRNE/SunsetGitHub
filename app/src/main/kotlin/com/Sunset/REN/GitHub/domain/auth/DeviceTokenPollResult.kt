package com.Sunset.REN.GitHub.domain.auth

/**
 * 轮询 GitHub Device Flow token 端点后的领域结果。
 */
sealed class DeviceTokenPollResult {
    data object AuthorizationPending : DeviceTokenPollResult()
    data object SlowDown : DeviceTokenPollResult()
    data object ExpiredToken : DeviceTokenPollResult()
    data object AccessDenied : DeviceTokenPollResult()

    data class Success(
        val accessToken: String,
        val tokenType: String,
        val scope: String
    ) : DeviceTokenPollResult()

    data class NetworkError(
        val message: String? = null
    ) : DeviceTokenPollResult()

    data class UnknownError(
        val code: String? = null,
        val description: String? = null
    ) : DeviceTokenPollResult()
}