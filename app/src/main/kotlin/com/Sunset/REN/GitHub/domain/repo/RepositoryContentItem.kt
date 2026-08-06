package com.Sunset.REN.GitHub.domain.repo

/**
 * V0.1 文件浏览只支持默认分支、目录下钻、文本预览和二进制/大文件占位。
 */
sealed class RepositoryContentItem {
    abstract val name: String
    abstract val path: String
    abstract val htmlUrl: String?

    data class Directory(
        override val name: String,
        override val path: String,
        override val htmlUrl: String? = null
    ) : RepositoryContentItem()

    data class File(
        override val name: String,
        override val path: String,
        val sizeBytes: Long,
        val downloadUrl: String? = null,
        override val htmlUrl: String? = null
    ) : RepositoryContentItem()

    data class Unsupported(
        override val name: String,
        override val path: String,
        val reason: String,
        override val htmlUrl: String? = null
    ) : RepositoryContentItem()
}