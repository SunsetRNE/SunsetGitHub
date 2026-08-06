package com.Sunset.REN.GitHub.ui.repo

import android.net.Uri

/** GitHub 网页链接在应用内可识别的目标。 */
sealed class GitHubInternalLinkTarget {
    data class Repository(val owner: String, val repo: String) : GitHubInternalLinkTarget()
    data class File(val owner: String, val repo: String, val path: String, val name: String) : GitHubInternalLinkTarget()
    data class Issue(val owner: String, val repo: String, val number: Int) : GitHubInternalLinkTarget()
    data class PullRequest(val owner: String, val repo: String, val number: Int) : GitHubInternalLinkTarget()
    data class User(val login: String) : GitHubInternalLinkTarget()
}

/**
 * README / 搜索 / 个人页等位置共用的 GitHub 链接语法拦截器。
 *
 * 只接管明确能在当前 App 内展示的 github.com 仓库、文件、Issue、PR、用户链接；
 * 其余外部站点、锚点、mailto/tel/data 等仍交给系统默认处理。
 */
object GitHubInternalLinkParser {
    fun parse(url: String): GitHubInternalLinkTarget? {
        val trimmed = url.trim()
        if (trimmed.isBlank() || trimmed.startsWith("#")) return null
        val uri = runCatching { Uri.parse(trimmed) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase().orEmpty()
        if (scheme != "http" && scheme != "https") return null
        val host = uri.host?.lowercase()?.removePrefix("www.") ?: return null
        if (host != "github.com") return null
        val segments = uri.pathSegments.filter { it.isNotBlank() }
        if (segments.isEmpty()) return null

        if (segments.size == 1) {
            return GitHubInternalLinkTarget.User(segments[0])
        }

        val owner = segments[0]
        val repo = segments[1].removeSuffix(".git")
        if (owner.isBlank() || repo.isBlank()) return null
        if (segments.size == 2) {
            return GitHubInternalLinkTarget.Repository(owner, repo)
        }

        return when (segments[2].lowercase()) {
            "issues" -> segments.getOrNull(3)?.toIntOrNull()?.let { number ->
                GitHubInternalLinkTarget.Issue(owner, repo, number)
            }
            "pull" -> segments.getOrNull(3)?.toIntOrNull()?.let { number ->
                GitHubInternalLinkTarget.PullRequest(owner, repo, number)
            }
            "blob" -> {
                val path = segments.drop(4).joinToString("/")
                if (path.isBlank()) {
                    GitHubInternalLinkTarget.Repository(owner, repo)
                } else {
                    GitHubInternalLinkTarget.File(
                        owner = owner,
                        repo = repo,
                        path = path,
                        name = path.substringAfterLast('/')
                    )
                }
            }
            "tree" -> GitHubInternalLinkTarget.Repository(owner, repo)
            else -> GitHubInternalLinkTarget.Repository(owner, repo)
        }
    }
}
