package com.Sunset.REN.GitHub.domain.auth

/**
 * GitHub Device Flow 返回的设备授权信息。
 */
data class DeviceCodeGrant(
    val deviceCode: String,
    val userCode: String,
    val verificationUri: String,
    val verificationUriComplete: String?,
    val expiresInSeconds: Long,
    val intervalSeconds: Long
)