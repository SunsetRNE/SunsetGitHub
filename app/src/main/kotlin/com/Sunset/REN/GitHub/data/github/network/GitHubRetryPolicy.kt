package com.Sunset.REN.GitHub.data.github.network

/** Retry policy for idempotent GitHub requests. */
data class GitHubRetryPolicy(
    val maxAttempts: Int = DefaultMaxAttempts,
    val baseDelayMillis: Long = DefaultBaseDelayMillis,
    val retryableStatusCodes: Set<Int> = DefaultRetryableStatusCodes,
    val sleeper: (Long) -> Unit = { delayMillis -> Thread.sleep(delayMillis) }
) {
    fun backoffMillis(attempt: Int): Long = baseDelayMillis * (1L shl (attempt - 1))

    companion object {
        const val DefaultMaxAttempts = 3
        const val DefaultBaseDelayMillis = 500L
        val DefaultRetryableStatusCodes = setOf(429, 500, 502, 503, 504)
        val Default = GitHubRetryPolicy()
    }
}