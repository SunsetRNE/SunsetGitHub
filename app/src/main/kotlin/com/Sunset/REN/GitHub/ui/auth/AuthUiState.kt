package com.Sunset.REN.GitHub.ui.auth

import androidx.annotation.StringRes

sealed class AuthUiState {
    data object Loading : AuthUiState()
    data object SignedOut : AuthUiState()
    data object OpeningBrowser : AuthUiState()

    data class DeviceCodePending(
        val userCode: String,
        val verificationUri: String,
        val verificationUriComplete: String?,
        val expiresInSeconds: Long,
        val expiresAtMillis: Long
    ) : AuthUiState()

    data class Authorized(
        val login: String
    ) : AuthUiState()

    data object Cancelled : AuthUiState()
    data object Expired : AuthUiState()
    data object Denied : AuthUiState()

    data class NetworkError(
        val message: String? = null,
        @StringRes val fallbackMessageResId: Int? = null
    ) : AuthUiState()
}
