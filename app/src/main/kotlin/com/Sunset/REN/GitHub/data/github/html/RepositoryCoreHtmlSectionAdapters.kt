package com.Sunset.REN.GitHub.data.github.html

class RepositorySecurityHtmlAdapter : BaseRepositoryHtmlSectionAdapter() {
    override val sectionKey: String = "security_quality"
    override val sectionPath: String = "security"
    override val displayTitle: String = "Security / Quality"

    override fun parseDocument(
        owner: String,
        repo: String,
        page: GitHubHtmlPage,
        document: GitHubHtmlDocument
    ): GitHubHtmlParseResult<RepositoryHtmlSectionSummary> {
        val signals = buildList {
            addAll(document.extractKeywordNotices("Security", "Dependabot", "Code scanning", "Secret scanning", "Vulnerability", "Security policy", "Security advisories"))
            document.firstMetaContent("og:title")?.let { add(it) }
            document.firstMetaContent("description")?.let { add(it) }
            document.title?.let { add(it) }
        }.distinct()

        val metrics = listOf(
            document.featureMetric("Security policy", "Security policy"),
            document.featureMetric("Dependabot", "Dependabot"),
            document.featureMetric("Code scanning", "Code scanning"),
            document.featureMetric("Secret scanning", "Secret scanning"),
            document.featureMetric("Security advisories", "Security advisories")
        ).distinctBy { metric -> metric.label }

        val status = when {
            document.containsAny("Security policy", "Dependabot", "Code scanning", "Secret scanning", "Security advisories", "Vulnerability alerts") -> RepositoryHtmlSectionStatus.Available
            signals.isNotEmpty() -> RepositoryHtmlSectionStatus.ParsePartial
            else -> RepositoryHtmlSectionStatus.ParseFailed
        }

        val notices = signals.take(3)

        return summary(
            owner = owner,
            repo = repo,
            page = page,
            status = status,
            description = when (status) {
                RepositoryHtmlSectionStatus.Available -> "已从 GitHub Security 页面解析到安全相关信息。"
                RepositoryHtmlSectionStatus.ParsePartial -> "已进入 Security 页面，但仅解析到标题与片段信息。"
                else -> "Security 页面结构暂未匹配到可识别内容。"
            },
            metrics = metrics,
            notices = notices,
            actions = listOf(
                "下一步可接入 REST/GraphQL 安全告警接口，替代当前 HTML 摘要。",
                "优先细化 Dependabot、Code scanning、Secret scanning 的开关与计数解析。"
            )
        )
    }
}

class RepositoryInsightsHtmlAdapter : BaseRepositoryHtmlSectionAdapter() {
    override val sectionKey: String = "insights"
    override val sectionPath: String = "pulse"
    override val displayTitle: String = "Insights"

    override fun parseDocument(
        owner: String,
        repo: String,
        page: GitHubHtmlPage,
        document: GitHubHtmlDocument
    ): GitHubHtmlParseResult<RepositoryHtmlSectionSummary> {
        val summarySignals = listOfNotNull(
            document.title,
            document.firstMetaContent("og:title"),
            document.firstMetaContent("description")
        ).filter { it.isNotBlank() }.distinct()

        val notices = document.extractKeywordNotices(
            "Pulse",
            "Contributors",
            "Commits",
            "Pull requests",
            "Issues",
            "Merged pull requests",
            "Opened issues",
            "Closed issues"
        )

        val metrics = listOfNotNull(
            document.keywordMetric("Commits"),
            document.keywordMetric("Pull requests"),
            document.keywordMetric("Merged pull requests"),
            document.keywordMetric("Issues"),
            document.keywordMetric("Opened issues"),
            document.keywordMetric("Closed issues"),
            document.keywordMetric("Contributors")
        ).distinctBy { metric -> metric.label }

        val hasOverview = document.containsAny("Pulse", "Contributors", "Traffic", "Commits", "Issues", "Pull requests")
        val status = when {
            hasOverview -> RepositoryHtmlSectionStatus.Available
            summarySignals.isNotEmpty() || notices.isNotEmpty() || metrics.isNotEmpty() -> RepositoryHtmlSectionStatus.ParsePartial
            else -> RepositoryHtmlSectionStatus.ParseFailed
        }

        return summary(
            owner = owner,
            repo = repo,
            page = page,
            status = status,
            description = when (status) {
                RepositoryHtmlSectionStatus.Available -> "已从 GitHub Insights/Pulse 页面解析到仓库活动摘要。"
                RepositoryHtmlSectionStatus.ParsePartial -> "已识别到 Insights 页面标题或片段，但未提取到完整统计。"
                else -> "Insights 页面结构暂未匹配到可识别内容。"
            },
            metrics = metrics,
            notices = (summarySignals + notices).distinct().take(4),
            actions = listOf(
                "下一步可拆分 Pulse、Contributors、Traffic 三个原生子页。",
                "优先用 API 聚合 commits、PR、issues 趋势，HTML 仅作为过渡摘要。"
            )
        )
    }
}

class RepositoryWikiHtmlAdapter : BaseRepositoryHtmlSectionAdapter() {
    override val sectionKey: String = "wiki"
    override val sectionPath: String = "wiki"
    override val displayTitle: String = "Wiki"

    override fun parseDocument(
        owner: String,
        repo: String,
        page: GitHubHtmlPage,
        document: GitHubHtmlDocument
    ): GitHubHtmlParseResult<RepositoryHtmlSectionSummary> {
        val disabled = document.containsAny("Wiki is disabled", "The wiki has been disabled", "There are no pages")
        val titleSignals = listOfNotNull(document.title, document.firstMetaContent("og:title"), document.firstMetaContent("description"))
            .filter { it.isNotBlank() }
            .distinct()
        val notices = document.extractKeywordNotices("Wiki", "Pages", "Clone this wiki locally")
        val hasWikiSignals = disabled || titleSignals.isNotEmpty() || notices.isNotEmpty() || document.containsAny("Create the first page", "Edit this page", "Wiki home")
        return summary(
            owner = owner,
            repo = repo,
            page = page,
            status = when {
                disabled -> RepositoryHtmlSectionStatus.Empty
                hasWikiSignals && notices.isNotEmpty() -> RepositoryHtmlSectionStatus.Available
                hasWikiSignals -> RepositoryHtmlSectionStatus.ParsePartial
                else -> RepositoryHtmlSectionStatus.ParseFailed
            },
            description = when {
                disabled -> "该仓库 Wiki 未启用或暂无页面。"
                hasWikiSignals && notices.isNotEmpty() -> "已从 GitHub Wiki 页面解析到 Wiki 摘要。"
                hasWikiSignals -> "已识别到 Wiki 页面结构，但仅解析到标题片段。"
                else -> "Wiki 页面结构暂未匹配到可识别内容。"
            },
            notices = (titleSignals + notices).distinct().take(4)
        )
    }
}

private fun GitHubHtmlDocument.keywordMetric(keyword: String): RepositoryHtmlMetric? {
    val text = textsNear(keyword, radius = 72).firstOrNull() ?: return null
    val value = Regex("""\b\d+[,.]?\d*\b""").find(text)?.value ?: return null
    return RepositoryHtmlMetric(label = keyword, value = value)
}

fun GitHubHtmlDocument.featureMetric(label: String, keyword: String): RepositoryHtmlMetric {
    val value = if (containsAny(keyword)) "已识别" else "未识别"
    return RepositoryHtmlMetric(label = label, value = value)
}