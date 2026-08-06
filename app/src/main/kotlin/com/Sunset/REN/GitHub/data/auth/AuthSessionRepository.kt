package com.Sunset.REN.GitHub.data.auth

import android.content.Context
import com.Sunset.REN.GitHub.data.local.EncryptedSharedPreferencesTokenStore
import com.Sunset.REN.GitHub.data.local.SharedPreferencesAccountStore
import com.Sunset.REN.GitHub.data.local.SharedPreferencesCurrentAccountStore
import com.Sunset.REN.GitHub.domain.auth.GitHubAccount
import com.Sunset.REN.GitHub.domain.auth.RememberedAccount
import com.Sunset.REN.GitHub.domain.auth.RememberedAccountLoginType

/**
 * 统一管理一次 GitHub 登录会话的本地持久化边界。
 *
 * Access token 写入加密 token store；账号元数据写入 remembered/current account store。
 * UI 层不应再分别操作三份 store，以避免 Device Flow、Token 登录、账号管理之间行为漂移。
 */
class AuthSessionRepository(context: Context) {
    private val tokenStore = EncryptedSharedPreferencesTokenStore(context)
    private val accountStore = SharedPreferencesAccountStore(context)
    private val currentAccountStore = SharedPreferencesCurrentAccountStore(context)

    suspend fun saveSignedInAccount(
        account: GitHubAccount,
        accessToken: String,
        loginType: RememberedAccountLoginType
    ) {
        tokenStore.saveAccessToken(account.id, accessToken)
        accountStore.saveAccount(account, loginType)
        currentAccountStore.saveCurrentAccount(account)
    }

    suspend fun getCurrentAccount(): GitHubAccount? = currentAccountStore.getCurrentAccount()

    suspend fun hasAccessToken(accountId: Long): Boolean = !tokenStore.getAccessToken(accountId).isNullOrBlank()

    suspend fun hasCurrentSession(): Boolean {
        val current = getCurrentAccount() ?: return false
        return hasAccessToken(current.id)
    }

    suspend fun listRememberedAccounts(): List<RememberedAccount> = accountStore.listRememberedAccounts()

    suspend fun switchAccount(account: GitHubAccount): Boolean {
        if (!hasAccessToken(account.id)) return false
        currentAccountStore.saveCurrentAccount(account)
        return true
    }

    suspend fun removeAccount(account: GitHubAccount) {
        tokenStore.clearAccessToken(account.id)
        accountStore.removeAccount(account.id)
        val current = currentAccountStore.getCurrentAccount()
        if (current?.id == account.id) {
            currentAccountStore.clearCurrentAccount()
        }
    }

    suspend fun signOutCurrent() {
        currentAccountStore.clearCurrentAccount()
    }
}
