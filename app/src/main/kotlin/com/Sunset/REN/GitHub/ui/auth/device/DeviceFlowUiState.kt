package com.Sunset.REN.GitHub.ui.auth.device

import com.Sunset.REN.GitHub.domain.auth.GitHubAccount

sealed interface DeviceFlowUiState {
    data object RequestingCode : DeviceFlowUiState
    data class CodeReady(
        val userCode: String,
        val verificationUri: String,
        val verificationUriComplete: String?,
        val expiresAtMillis: Long,
        val message: String
    ) : DeviceFlowUiState
    data class SignedIn(val account: GitHubAccount) : DeviceFlowUiState
    data class Error(val message: String) : DeviceFlowUiState
    data object Cancelled : DeviceFlowUiState
}