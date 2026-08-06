package com.Sunset.REN.GitHub.domain.auth

data class RememberedAccount(
    val account: GitHubAccount,
    val loginType: RememberedAccountLoginType
)

enum class RememberedAccountLoginType {
    DeviceFlow,
    AccessToken
}
