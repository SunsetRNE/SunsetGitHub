package com.Sunset.REN.GitHub.core.network

import com.Sunset.REN.GitHub.BuildConfig

/**
 * GitHub OAuth App 配置入口。
 *
 * Device Flow 只需要 client_id；不得在 APK 内置 client_secret。
 *
 * P2（构建期混淆）：BuildConfig 中只有密文 + 每次构建随机的盐；
 * 明文由 Rust(.so) 内的 [uniffi.sunset_ffi.resolveOauthClientId] 解码，
 * 解码结果通过格式校验才返回——Kotlin 层不接触密钥，APK 无明文 client_id。
 */
object GitHubOAuthConfig {

    val ClientId: String by lazy {
        val obfuscated = BuildConfig.OAUTH_CLIENT_ID_OBFUSCATED
        val salt = BuildConfig.OAUTH_CLIENT_ID_SALT
        if (obfuscated.isBlank() || salt.isBlank()) {
            ""
        } else {
            // 注意：uniffi camelCase 规则为下划线后首字符大写 → resolve_oauth_client_id
            // 生成 resolveOauthClientId（Oauth 而非 OAuth），保持大小写一致。
            runCatching { uniffi.sunset_ffi.resolveOauthClientId(obfuscated, salt) }
                .getOrDefault("")
                .trim()
        }
    }
}