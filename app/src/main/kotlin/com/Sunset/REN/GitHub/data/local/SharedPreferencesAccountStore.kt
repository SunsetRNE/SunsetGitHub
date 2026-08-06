package com.Sunset.REN.GitHub.data.local

import android.content.Context
import androidx.core.content.edit
import com.Sunset.REN.GitHub.domain.auth.GitHubAccount
import com.Sunset.REN.GitHub.domain.auth.RememberedAccount
import com.Sunset.REN.GitHub.domain.auth.RememberedAccountLoginType
import org.json.JSONArray
import org.json.JSONObject

/**
 * Stores the local list of remembered GitHub accounts.
 *
 * Access tokens stay in [EncryptedSharedPreferencesTokenStore]; this store only keeps
 * non-secret account metadata for account switching and startup login gating.
 */
class SharedPreferencesAccountStore(context: Context) {

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    suspend fun saveAccount(
        account: GitHubAccount,
        loginType: RememberedAccountLoginType = RememberedAccountLoginType.AccessToken
    ) {
        val accounts = listRememberedAccounts().toMutableList()
        val existingIndex = accounts.indexOfFirst { it.account.id == account.id }
        val rememberedAccount = RememberedAccount(account, loginType)
        if (existingIndex >= 0) {
            accounts[existingIndex] = rememberedAccount
        } else {
            accounts.add(rememberedAccount)
        }
        saveRememberedAccounts(accounts)
    }

    suspend fun listAccounts(): List<GitHubAccount> = listRememberedAccounts().map { it.account }

    suspend fun listRememberedAccounts(): List<RememberedAccount> {
        val rawValue = preferences.getString(KEY_ACCOUNTS, null).orEmpty()
        if (rawValue.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(rawValue)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optLong(KEY_ACCOUNT_ID, DEFAULT_ACCOUNT_ID)
                    val login = item.optString(KEY_LOGIN).takeIf { it.isNotBlank() } ?: continue
                    if (id == DEFAULT_ACCOUNT_ID) continue
                    add(
                        RememberedAccount(
                            account = GitHubAccount(
                                id = id,
                                login = login,
                                avatarUrl = item.optionalString(KEY_AVATAR_URL),
                                name = item.optionalString(KEY_NAME)
                            ),
                            loginType = item.optionalLoginType()
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    suspend fun removeAccount(accountId: Long) {
        saveRememberedAccounts(listRememberedAccounts().filterNot { it.account.id == accountId })
    }

    suspend fun clearAccounts() {
        preferences.edit { clear() }
    }

    private fun saveRememberedAccounts(accounts: List<RememberedAccount>) {
        val distinctAccounts = accounts.distinctBy { it.account.id }
        val array = JSONArray().also { jsonArray ->
            distinctAccounts.forEach { rememberedAccount ->
                val account = rememberedAccount.account
                jsonArray.put(
                    JSONObject()
                        .put(KEY_ACCOUNT_ID, account.id)
                        .put(KEY_LOGIN, account.login)
                        .put(KEY_AVATAR_URL, account.avatarUrl)
                        .put(KEY_NAME, account.name)
                        .put(KEY_LOGIN_TYPE, rememberedAccount.loginType.name)
                )
            }
        }
        preferences.edit { putString(KEY_ACCOUNTS, array.toString()) }
    }

    private fun JSONObject.optionalString(key: String): String? {
        return if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
    }

    private fun JSONObject.optionalLoginType(): RememberedAccountLoginType {
        val rawValue = optString(KEY_LOGIN_TYPE).takeIf { it.isNotBlank() }
        return runCatching { RememberedAccountLoginType.valueOf(rawValue.orEmpty()) }
            .getOrDefault(RememberedAccountLoginType.AccessToken)
    }

    private companion object {
        const val PREFERENCES_NAME = "sunset_github_accounts"
        const val KEY_ACCOUNTS = "accounts"
        const val KEY_ACCOUNT_ID = "account_id"
        const val KEY_LOGIN = "login"
        const val KEY_AVATAR_URL = "avatar_url"
        const val KEY_NAME = "name"
        const val KEY_LOGIN_TYPE = "login_type"
        const val DEFAULT_ACCOUNT_ID = -1L
    }
}
