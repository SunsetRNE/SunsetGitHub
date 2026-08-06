package com.Sunset.REN.GitHub.domain.repo

/**
 * GitHub Contents API 相关限制配置。
 *
 * 数值跟随 GitHub 官方接口约束维护，业务层不应把它理解为应用自定义永久限制。
 */
object GitHubContentApiLimits {
    const val RecommendedDirectUploadMaxBytes: Long = 50L * 1024L * 1024L
    const val KnownSingleContentMaxBytes: Long = 100L * 1024L * 1024L
}