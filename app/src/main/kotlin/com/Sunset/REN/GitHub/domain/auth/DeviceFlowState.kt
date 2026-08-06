package com.Sunset.REN.GitHub.domain.auth

/**
 * V0.1 默认认证主线：GitHub Device Flow。
 *
 * 该状态机只描述客户端可见状态，不负责持久化 token。
 */
sealed class DeviceFlowState {
    data object Idle : DeviceFlowState()
    data object RequestingCode : DeviceFlowState()

    data class Pending(
        val deviceCode: String,
        val userCode: String,
        val verificationUri: String,
        val expiresInSeconds: Long,
        val intervalSeconds: Long
    ) : DeviceFlowState()

    data object Authorized : DeviceFlowState()
    data object Denied : DeviceFlowState()
    data object Expired : DeviceFlowState()
    data object Cancelled : DeviceFlowState()

    data class NetworkError(
        val message: String? = null
    ) : DeviceFlowState()
}
