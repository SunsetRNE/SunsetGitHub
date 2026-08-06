package com.Sunset.REN.GitHub.data.local

import com.Sunset.REN.GitHub.domain.auth.GitHubAccount

/**
 * 当前登录账号的本地分区入口。
 */
interface CurrentAccountStore {
    suspend fun saveCurrentAccount(account: GitHubAccount)

    suspend fun getCurrentAccount(): GitHubAccount?

    suspend fun clearCurrentAccount()
}