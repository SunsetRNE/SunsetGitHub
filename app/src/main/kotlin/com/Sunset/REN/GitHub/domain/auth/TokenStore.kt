package com.Sunset.REN.GitHub.domain.auth

/**
 * Token 访问边界。
 *
 * V0.1 要求：Android 本地不得明文存储 GitHub access token，
 * 任何 token 持久化实现都必须经由该抽象。
 */
interface TokenStore {
    suspend fun saveAccessToken(accountId: Long, token: String)
    suspend fun getAccessToken(accountId: Long): String?
    suspend fun clearAccessToken(accountId: Long)
    suspend fun clearAll()

    /**
     * token 生命周期封装：解密 → 仅在 block 内使用 → 返回后引用即弃。
     *
     * 新代码应优先使用本方法而不是直接 [getAccessToken]，把明文 token 的
     * 内存驻留时间压缩到单个请求作用域内；网络层不得将 token 缓存为成员。
     *
     * 注：JVM String 无法物理擦除底层字节，这里保证的是"引用不再存活 + 调用方
     * 约定不缓存"；这是无服务端方案下"用完即弃"的现实边界。
     */
    suspend fun <T> withAccessToken(accountId: Long, block: (String) -> T): T? {
        val token = getAccessToken(accountId) ?: return null
        return try {
            block(token)
        } finally {
            // 引用在 block 结束后不再存活；禁止在 block 内将 token 存入成员/日志。
        }
    }
}
