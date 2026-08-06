package com.Sunset.REN.GitHub.data.github.html

class RepositoryProjectsHtmlAdapter : BaseRepositoryHtmlSectionAdapter() {
    override val sectionKey: String = "projects"
    override val sectionPath: String = "projects"
    override val displayTitle: String = "Projects"

    override fun parseDocument(
        owner: String,
        repo: String,
        page: GitHubHtmlPage,
        document: GitHubHtmlDocument
    ): GitHubHtmlParseResult<RepositoryHtmlSectionSummary> {
        val disabled = document.containsAny(
            "Projects are disabled",
            "There aren't any projects yet",
            "No projects",
            "This repository doesn't have any projects yet"
        )
        val titleSignals = document.sectionTitleSignals()
        val notices = document.extractKeywordNotices(
            "Projects",
            "Board",
            "Roadmap",
            "Table",
            "Status",
            "Backlog",
            "Iteration"
        )
        val metrics = listOf(
            document.featureMetric("Board", "Board"),
            document.featureMetric("Roadmap", "Roadmap"),
            document.featureMetric("Table", "Table"),
            document.featureMetric("Status", "Status"),
            document.featureMetric("Iteration", "Iteration")
        ).distinctBy { metric -> metric.label }
        val hasProjectSignals = titleSignals.isNotEmpty() || notices.isNotEmpty() || document.containsAny(
            "New project",
            "Open project",
            "Project board",
            "Roadmap",
            "Project template"
        )
        val status = when {
            disabled -> RepositoryHtmlSectionStatus.Empty
            hasProjectSignals && notices.isNotEmpty() -> RepositoryHtmlSectionStatus.Available
            hasProjectSignals -> RepositoryHtmlSectionStatus.ParsePartial
            else -> RepositoryHtmlSectionStatus.ParseFailed
        }

        return summary(
            owner = owner,
            repo = repo,
            page = page,
            status = status,
            description = when (status) {
                RepositoryHtmlSectionStatus.Empty -> "该仓库暂未展示可解析的 Projects。"
                RepositoryHtmlSectionStatus.Available -> "已从 GitHub Projects 页面解析到项目摘要。"
                RepositoryHtmlSectionStatus.ParsePartial -> "已识别到 Projects 页面结构，但仅解析到标题或入口片段。"
                else -> "Projects 页面结构暂未匹配到可识别内容。"
            },
            metrics = metrics,
            notices = (titleSignals + notices).distinct().take(4),
            actions = listOf(
                "可在 GitHub 网页端查看看板、路线图和表格视图。",
                "完整字段和自动化配置以 GitHub Projects 页面为准。"
            )
        )
    }
}

class RepositoryAgentsHtmlAdapter : BaseRepositoryHtmlSectionAdapter() {
    override val sectionKey: String = "agents"
    override val sectionPath: String = "copilot"
    override val displayTitle: String = "Agents"

    override fun parseDocument(
        owner: String,
        repo: String,
        page: GitHubHtmlPage,
        document: GitHubHtmlDocument
    ): GitHubHtmlParseResult<RepositoryHtmlSectionSummary> {
        val unavailable = document.containsAny(
            "Copilot is not available",
            "This feature is not available",
            "not enabled for this repository"
        )
        val titleSignals = document.sectionTitleSignals()
        val notices = document.extractKeywordNotices(
            "Copilot",
            "Agents",
            "Coding agent",
            "Pull request",
            "Agent mode",
            "Assign to Copilot"
        )
        val metrics = listOf(
            document.featureMetric("Copilot", "Copilot"),
            document.featureMetric("Coding agent", "Coding agent"),
            document.featureMetric("Pull request", "Pull request"),
            document.featureMetric("Agent mode", "Agent mode")
        ).distinctBy { metric -> metric.label }
        val hasAgentSignals = titleSignals.isNotEmpty() || notices.isNotEmpty() || document.containsAny(
            "Assign to Copilot",
            "Copilot coding agent",
            "agent session",
            "AI-powered"
        )
        val status = when {
            unavailable -> RepositoryHtmlSectionStatus.Empty
            hasAgentSignals && notices.isNotEmpty() -> RepositoryHtmlSectionStatus.Available
            hasAgentSignals -> RepositoryHtmlSectionStatus.ParsePartial
            else -> RepositoryHtmlSectionStatus.ParseFailed
        }

        return summary(
            owner = owner,
            repo = repo,
            page = page,
            status = status,
            description = when (status) {
                RepositoryHtmlSectionStatus.Empty -> "该仓库暂未展示可解析的 Copilot / Agents 能力。"
                RepositoryHtmlSectionStatus.Available -> "已从 GitHub Copilot / Agents 页面解析到代理能力摘要。"
                RepositoryHtmlSectionStatus.ParsePartial -> "已识别到 Copilot / Agents 页面结构，但仅解析到标题或入口片段。"
                else -> "Agents 页面结构暂未匹配到可识别内容。"
            },
            metrics = metrics,
            notices = (titleSignals + notices).distinct().take(4),
            actions = listOf(
                "下一步可把 Copilot coding agent 入口与 PR 分派能力拆成独立状态。",
                "该页面变化较快，HTML 摘要应继续作为过渡层。"
            )
        )
    }
}

class RepositorySettingsHtmlAdapter : BaseRepositoryHtmlSectionAdapter() {
    override val sectionKey: String = "settings"
    override val sectionPath: String = "settings"
    override val displayTitle: String = "Settings"

    override fun parseDocument(
        owner: String,
        repo: String,
        page: GitHubHtmlPage,
        document: GitHubHtmlDocument
    ): GitHubHtmlParseResult<RepositoryHtmlSectionSummary> {
        val titleSignals = document.sectionTitleSignals()
        val notices = document.extractKeywordNotices(
            "Settings",
            "General",
            "Features",
            "Danger Zone",
            "Collaborators",
            "Branches",
            "Rules",
            "Webhooks"
        )
        val metrics = listOf(
            document.featureMetric("Features", "Features"),
            document.featureMetric("Danger Zone", "Danger Zone"),
            document.featureMetric("Collaborators", "Collaborators"),
            document.featureMetric("Branches", "Branches"),
            document.featureMetric("Rules", "Rules"),
            document.featureMetric("Webhooks", "Webhooks")
        ).distinctBy { metric -> metric.label }
        val hasSettingsSignals = document.containsAny(
            "Danger Zone",
            "Repository name",
            "Features",
            "Rules",
            "Collaborators",
            "Branches",
            "Webhooks"
        )
        val status = when {
            hasSettingsSignals -> RepositoryHtmlSectionStatus.Available
            titleSignals.isNotEmpty() || notices.isNotEmpty() -> RepositoryHtmlSectionStatus.ParsePartial
            else -> RepositoryHtmlSectionStatus.ParseFailed
        }
        return summary(
            owner = owner,
            repo = repo,
            page = page,
            status = status,
            description = when (status) {
                RepositoryHtmlSectionStatus.Available -> "已从 GitHub Settings 页面解析到仓库设置摘要。"
                RepositoryHtmlSectionStatus.ParsePartial -> "已进入 Settings 页面，但仅解析到标题与部分文本片段。"
                else -> "Settings 页面结构暂未匹配到可识别内容，或当前账号没有设置权限。"
            },
            metrics = metrics,
            notices = (titleSignals + notices).distinct().take(4),
            actions = listOf(
                "下一步可把 Settings 拆成仓库概览、分支规则和危险区子页。",
                "优先映射可修改字段和权限边界，再补交互入口。"
            )
        )
    }
}

private fun GitHubHtmlDocument.sectionTitleSignals(): List<String> {
    return listOfNotNull(
        title,
        firstMetaContent("og:title"),
        firstMetaContent("description")
    ).filter { signal -> signal.isNotBlank() && signal.isUsefulHtmlSectionSignal() }
        .distinct()
}

private fun String.isUsefulHtmlSectionSignal(): Boolean {
    val normalized = lowercase().trim()
    return normalized !in setOf("timeout", "server error", "github · timeout", "github · server error") &&
        !normalized.contains("page is taking too long") &&
        !normalized.contains("temporarily unavailable")
}
