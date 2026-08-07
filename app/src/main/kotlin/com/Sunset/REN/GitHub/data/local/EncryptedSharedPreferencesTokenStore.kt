package com.Sunset.REN.GitHub.data.local

import android.content.Context
import androidx.core.content.edit
import com.Sunset.REN.GitHub.domain.auth.TokenStore
import java.util.Base64

/**
 * 使用 Android Keystore 进行加密后，再落到私有 SharedPreferences。
 */
class EncryptedSharedPreferencesTokenStore(
    context: Context,
    private val helper: TokenEncryptionHelper = TokenEncryptionHelper()
) : TokenStore {

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override suspend fun saveAccessToken(accountId: Long, token: String) {
        val payload = helper.encrypt(token)
        preferences.edit {
            putString(keyForAccount(accountId), encodePayload(payload))
        }
    }

    override suspend fun getAccessToken(accountId: Long): String? {
        val encoded = preferences.getString(keyForAccount(accountId), null) ?: return null
        return try {
            helper.decrypt(decodePayload(encoded))
        } catch (exception: java.security.GeneralSecurityException) {
            // Keystore 密钥失效（换机/备份恢复/OEM 实现差异）或密文损坏。
            // 降级：清除该条目并让用户重新登录，而不是把异常抛给 UI。
            preferences.edit { remove(keyForAccount(accountId)) }
            null
        } catch (exception: IllegalArgumentException) {
            // Base64 / 载荷格式损坏。
            preferences.edit { remove(keyForAccount(accountId)) }
            null
        }
    }

    override suspend fun clearAccessToken(accountId: Long) {
        preferences.edit {
            remove(keyForAccount(accountId))
        }
    }

    override suspend fun clearAll() {
        preferences.edit {
            clear()
        }
    }

    private fun keyForAccount(accountId: Long): String = "access_token_$accountId"

    private fun encodePayload(payload: TokenEncryptionHelper.EncryptedPayload): String {
        val cipherText = Base64.getEncoder().encodeToString(payload.cipherText)
        val iv = Base64.getEncoder().encodeToString(payload.iv)
        return "$iv:$cipherText"
    }

    private fun decodePayload(encoded: String): TokenEncryptionHelper.EncryptedPayload {
        val parts = encoded.split(":", limit = 2)
        require(parts.size == 2) { "Invalid encrypted payload" }
        return TokenEncryptionHelper.EncryptedPayload(
            iv = Base64.getDecoder().decode(parts[0]),
            cipherText = Base64.getDecoder().decode(parts[1])
        )
    }

    private companion object {
        const val PREFERENCES_NAME = "sunset_github_token_store_encrypted"
    }
}