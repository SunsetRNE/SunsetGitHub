package com.Sunset.REN.GitHub.data.github.html

interface RepositoryHtmlSectionAdapter {
    val sectionKey: String
    val sectionPath: String
    val displayTitle: String

    fun parse(owner: String, repo: String, page: GitHubHtmlPage): GitHubHtmlParseResult<RepositoryHtmlSectionSummary>
}

abstract class BaseRepositoryHtmlSectionAdapter : RepositoryHtmlSectionAdapter {

    override fun parse(owner: String, repo: String, page: GitHubHtmlPage): GitHubHtmlParseResult<RepositoryHtmlSectionSummary> {
        val document = GitHubHtmlDocument(page.html)
        val htmlPreview = page.htmlPreview.takeIf { it.isNotBlank() }
        return when {
            page.hasNetworkError -> GitHubHtmlParseResult.ParseError(
                message = page.errorMessage ?: "GitHub HTML 页面请求失败。",
                sourceUrl = page.url,
                statusCode = page.statusCode,
                htmlPreview = htmlPreview
            )
            page.statusCode == HttpURLConnectionForbidden -> GitHubHtmlParseResult.AccessDenied(
                message = "GitHub 拒绝访问该分区。",
                sourceUrl = page.url,
                statusCode = page.statusCode,
                htmlPreview = htmlPreview
            )
            page.statusCode == HttpURLConnectionNotFound -> GitHubHtmlParseResult.NotFound(
                message = "该分区不存在或仓库未启用。",
                sourceUrl = page.url,
                statusCode = page.statusCode,
                htmlPreview = htmlPreview
            )
            document.isGitHubTransientErrorPage() -> GitHubHtmlParseResult.ParseError(
                message = "GitHub 返回了临时错误页面，未进入目标分区内容。",
                sourceUrl = page.url,
                statusCode = page.statusCode,
                htmlPreview = htmlPreview
            )
            document.containsAny("Sign in to GitHub", "You must be signed in", "This repository is private") ->
                GitHubHtmlParseResult.AccessDenied(
                    message = "当前账号无法访问该 GitHub 页面。",
                    sourceUrl = page.url,
                    statusCode = page.statusCode,
                    htmlPreview = htmlPreview
                )
            !page.isSuccessful -> GitHubHtmlParseResult.ParseError(
                message = "GitHub 页面返回 HTTP ${page.statusCode}。",
                sourceUrl = page.url,
                statusCode = page.statusCode,
                htmlPreview = htmlPreview
            )
            else -> parseDocument(owner, repo, page, document)
        }
    }

    protected abstract fun parseDocument(
        owner: String,
        repo: String,
        page: GitHubHtmlPage,
        document: GitHubHtmlDocument
    ): GitHubHtmlParseResult<RepositoryHtmlSectionSummary>

    protected fun summary(
        owner: String,
        repo: String,
        page: GitHubHtmlPage,
        status: RepositoryHtmlSectionStatus,
        description: String,
        metrics: List<RepositoryHtmlMetric> = emptyList(),
        notices: List<String> = emptyList(),
        actions: List<String> = emptyList()
    ): GitHubHtmlParseResult.Success<RepositoryHtmlSectionSummary> {
        return GitHubHtmlParseResult.Success(
            RepositoryHtmlSectionSummary(
                owner = owner,
                repo = repo,
                sectionKey = sectionKey,
                title = displayTitle,
                status = status,
                description = description,
                metrics = metrics,
                notices = notices,
                actions = actions,
                sourceUrl = page.url
            )
        )
    }

    protected fun GitHubHtmlDocument.extractKeywordNotices(vararg keywords: String, limit: Int = 3): List<String> {
        return keywords.flatMap { keyword -> textsNear(keyword) }
            .map { text -> text.take(MaxNoticeLength).trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(limit)
    }

    companion object {
        private const val HttpURLConnectionForbidden = 403
        private const val HttpURLConnectionNotFound = 404
        private const val MaxNoticeLength = 220
    }
}