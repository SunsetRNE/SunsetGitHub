package com.Sunset.REN.GitHub.domain.auth

/**
 * Device Flow 认证仓库接口。
 *
 * 实现层负责调用 GitHub Device Flow API；UI/ViewModel 只依赖该接口。
 */
interface DeviceFlowRepository {
    suspend fun requestDeviceCode(): DeviceCodeGrant

    suspend fun pollAccessToken(deviceCode: String): DeviceTokenPollResult

    suspend fun fetchCurrentAccount(accessToken: String): GitHubAccount
}
