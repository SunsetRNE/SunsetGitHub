package com.Sunset.REN.GitHub.data.github.html

import com.Sunset.REN.GitHub.data.github.network.GitHubApiHeaders
import com.Sunset.REN.GitHub.data.github.network.GitHubHttpClient
import com.Sunset.REN.GitHub.data.github.network.GitHubHttpRequest
import com.Sunset.REN.GitHub.data.github.network.GitHubHttpResponse
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException

class GitHubRepositoryWikiGateway(
    accessToken: String
) {
    private val httpClient = GitHubHttpClient(accessToken, timeoutMillis = TimeoutMillis)

    fun loadWikiSummary(owner: String, repo: String): GitHubHtmlParseResult<RepositoryHtmlSectionSummary> {
        val wikiUrl = "https://github.com/${owner.toWikiPathSegment()}/${repo.toWikiPathSegment()}/wiki"
        val pagesUrl = "$wikiUrl/_pages"
        val apiUrl = "https://api.github.com/repos/${owner.toWikiPathSegment()}/${repo.toWikiPathSegment()}"
        val repositoryResponse = try {
            request(apiUrl, accept = "application/vnd.github+json")
        } catch (error: SocketTimeoutException) {
            return GitHubHtmlParseResult.ParseError(
                message = "GitHub REST Wiki 状态请求超时。",
                sourceUrl = wikiUrl,
                statusCode = NetworkErrorStatusCode
            )
        } catch (error: IOException) {
            return GitHubHtmlParseResult.ParseError(
                message = error.message?.takeIf { it.isNotBlank() } ?: "GitHub REST Wiki 状态请求失败。",
                sourceUrl = wikiUrl,
                statusCode = NetworkErrorStatusCode
            )
        }
        val repositoryPreview = repositoryResponse.preview
        if (repositoryResponse.statusCode == HttpURLConnection.HTTP_FORBIDDEN) {
            return GitHubHtmlParseResult.AccessDenied(
                message = "当前令牌无法读取仓库 Wiki 状态。",
                sourceUrl = wikiUrl,
                statusCode = repositoryResponse.statusCode,
                htmlPreview = repositoryPreview
            )
        }
        if (repositoryResponse.statusCode == HttpURLConnection.HTTP_NOT_FOUND) {
            return GitHubHtmlParseResult.NotFound(
                message = "仓库不存在，或当前令牌无法访问该仓库。",
                sourceUrl = wikiUrl,
                statusCode = repositoryResponse.statusCode,
                htmlPreview = repositoryPreview
            )
        }
        if (repositoryResponse.statusCode !in 200..299) {
            return GitHubHtmlParseResult.ParseError(
                message = "GitHub REST Wiki 状态返回 HTTP ${repositoryResponse.statusCode}。",
                sourceUrl = wikiUrl,
                statusCode = repositoryResponse.statusCode,
                htmlPreview = repositoryPreview
            )
        }

        val repository = runCatching { JSONObject(repositoryResponse.body) }.getOrElse {
            return GitHubHtmlParseResult.ParseError(
                message = "GitHub REST Wiki 状态返回内容不是有效 JSON。",
                sourceUrl = wikiUrl,
                statusCode = repositoryResponse.statusCode,
                htmlPreview = repositoryPreview
            )
        }
        val hasWiki = repository.optBoolean("has_wiki", false)
        val defaultBranch = repository.optionalWikiString("default_branch") ?: "未返回"
        val visibility = repository.optionalWikiString("visibility") ?: if (repository.optBoolean("private", false)) "private" else "public"
        if (!hasWiki) {
            return GitHubHtmlParseResult.Success(
                buildSummary(
                    owner = owner,
                    repo = repo,
                    wikiUrl = wikiUrl,
                    status = RepositoryHtmlSectionStatus.Disabled,
                    description = "该仓库 Wiki 功能未启用。",
                    metrics = baseMetrics(hasWiki = false, defaultBranch = defaultBranch, visibility = visibility, pages = emptyList()),
                    notices = listOf("REST 仓库设置显示 has_wiki=false。")
                )
            )
        }

        val pagesResponse = runCatching {
            request(pagesUrl, accept = "text/html,application/xhtml+xml")
        }
        val pages = pagesResponse.getOrNull()
            ?.takeIf { response -> response.statusCode in 200..299 }
            ?.body
            ?.extractWikiPages(owner, repo)
            .orEmpty()
        val pageFailure = pagesResponse.exceptionOrNull()?.message
            ?: pagesResponse.getOrNull()?.takeIf { response -> response.statusCode !in 200..299 }?.let { response ->
                "Wiki 页面索引返回 HTTP ${response.statusCode}。"
            }
        val status = when {
            pages.isNotEmpty() -> RepositoryHtmlSectionStatus.Available
            pageFailure == null -> RepositoryHtmlSectionStatus.Empty
            else -> RepositoryHtmlSectionStatus.ParsePartial
        }
        val notices = buildList {
            add("Wiki 已启用。")
            if (pages.isEmpty() && pageFailure == null) add("页面索引暂未返回可解析页面，可能尚未创建 Wiki 页面。")
            pages.take(PageNoticeLimit).forEachIndexed { index, page -> add("页面 ${index + 1}：$page") }
            pageFailure?.takeIf { it.isNotBlank() }?.let { add("页面索引探测：$it") }
        }
        return GitHubHtmlParseResult.Success(
            buildSummary(
                owner = owner,
                repo = repo,
                wikiUrl = wikiUrl,
                status = status,
                description = when (status) {
                    RepositoryHtmlSectionStatus.Available -> "已从 GitHub Wiki 页面索引解析到 Wiki 摘要。"
                    RepositoryHtmlSectionStatus.Empty -> "Wiki 已启用，但页面索引暂未展示可解析页面。"
                    else -> "Wiki 已启用，但页面索引未能完整解析。"
                },
                metrics = baseMetrics(hasWiki = true, defaultBranch = defaultBranch, visibility = visibility, pages = pages),
                notices = notices
            )
        )
    }

    private fun request(url: String, accept: String): WikiNetworkResponse {
        return httpClient.execute(
            GitHubHttpRequest(
                pathOrUrl = url,
                accept = accept,
                apiVersion = GitHubApiHeaders.LegacyApiVersion,
                followRedirects = true
            )
        ).toWikiNetworkResponse()
    }

    private fun buildSummary(
        owner: String,
        repo: String,
        wikiUrl: String,
        status: RepositoryHtmlSectionStatus,
        description: String,
        metrics: List<RepositoryHtmlMetric>,
        notices: List<String>
    ): RepositoryHtmlSectionSummary {
        return RepositoryHtmlSectionSummary(
            owner = owner,
            repo = repo,
            sectionKey = SectionKey,
            title = "Wiki",
            status = status,
            description = description,
            metrics = metrics,
            notices = notices.distinct().take(MaxNotices),
            actions = listOf(
                "REST 状态可稳定区分 Wiki 是否启用。",
                "页面列表来自 GitHub Wiki 页面索引，后续可继续接入 wiki.git 源以读取完整页面内容。"
            ),
            sourceUrl = wikiUrl
        )
    }

    private fun baseMetrics(
        hasWiki: Boolean,
        defaultBranch: String,
        visibility: String,
        pages: List<String>
    ): List<RepositoryHtmlMetric> {
        return listOf(
            RepositoryHtmlMetric("数据来源", "REST + GitHub Wiki 页面索引"),
            RepositoryHtmlMetric("Wiki", if (hasWiki) "开启" else "关闭"),
            RepositoryHtmlMetric("页面样本数", pages.size.toString()),
            RepositoryHtmlMetric("默认分支", defaultBranch),
            RepositoryHtmlMetric("可见性", visibility)
        )
    }

    private fun GitHubHttpResponse.toWikiNetworkResponse(): WikiNetworkResponse {
        return WikiNetworkResponse(statusCode = statusCode, body = body)
    }

    private data class WikiNetworkResponse(
        val statusCode: Int,
        val body: String
    ) {
        val preview: String get() = body.take(240).replace(Regex("\\s+"), " ").trim()
    }

    private companion object {
        private const val SectionKey = "wiki"
        private const val TimeoutMillis = 15_000
        private const val NetworkErrorStatusCode = 0
        private const val PageNoticeLimit = 6
        private const val MaxNotices = 8
    }
}

private fun String.extractWikiPages(owner: String, repo: String): List<String> {
    val ownerPattern = Regex.escape(owner)
    val repoPattern = Regex.escape(repo)
    val linkRegex = Regex(
        """<a\b[^>]+href=["']/+$ownerPattern/$repoPattern/wiki/([^"'#?]+)["'][^>]*>(.*?)</a>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    return linkRegex.findAll(this)
        .mapNotNull { match ->
            val slug = match.groupValues.getOrNull(1).orEmpty()
            val text = match.groupValues.getOrNull(2).orEmpty()
                .replace(Regex("<[^>]+>"), " ")
                .decodeHtmlEntities()
                .normalizeWhitespace()
            val page = text.takeIf { it.isNotBlank() } ?: slug.replace('-', ' ').decodeHtmlEntities().normalizeWhitespace()
            page.takeUnless { value -> value.startsWith("_") || value.equals("Home", ignoreCase = true) && slug.equals("_pages", ignoreCase = true) }
        }
        .filter { page -> page.isNotBlank() }
        .distinct()
        .take(20)
        .toList()
}

private fun JSONObject.optionalWikiString(name: String): String? {
    if (isNull(name)) return null
    return optString(name).takeIf { it.isNotBlank() && it != "null" }
}

private fun String.toWikiPathSegment(): String {
    return java.net.URLEncoder.encode(this, Charsets.UTF_8.name()).replace("+", "%20")
}
