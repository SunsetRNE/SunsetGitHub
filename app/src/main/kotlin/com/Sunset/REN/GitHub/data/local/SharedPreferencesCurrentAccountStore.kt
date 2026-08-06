package com.Sunset.REN.GitHub.data.local

import android.content.Context
import androidx.core.content.edit
import com.Sunset.REN.GitHub.domain.auth.GitHubAccount

/**
 * 当前登录账号的本地分区入口。
 */
class SharedPreferencesCurrentAccountStore(
    context: Context
) : CurrentAccountStore {

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override suspend fun saveCurrentAccount(account: GitHubAccount) {
        preferences.edit {
            putLong(KEY_ACCOUNT_ID, account.id)
            putString(KEY_LOGIN, account.login)
            putString(KEY_AVATAR_URL, account.avatarUrl)
            putString(KEY_NAME, account.name)
        }
    }

    override suspend fun getCurrentAccount(): GitHubAccount? {
        val accountId = preferences.getLong(KEY_ACCOUNT_ID, DEFAULT_ACCOUNT_ID)
        val login = preferences.getString(KEY_LOGIN, null) ?: return null
        return GitHubAccount(
            id = accountId,
            login = login,
            avatarUrl = preferences.getString(KEY_AVATAR_URL, null),
            name = preferences.getString(KEY_NAME, null)
        )
    }

    override suspend fun clearCurrentAccount() {
        preferences.edit {
            clear()
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "sunset_github_current_account"
        const val KEY_ACCOUNT_ID = "account_id"
        const val KEY_LOGIN = "login"
        const val KEY_AVATAR_URL = "avatar_url"
        const val KEY_NAME = "name"
        const val DEFAULT_ACCOUNT_ID = -1L
    }
}