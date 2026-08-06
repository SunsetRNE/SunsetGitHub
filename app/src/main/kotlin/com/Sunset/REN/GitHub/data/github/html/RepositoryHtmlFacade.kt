package com.Sunset.REN.GitHub.data.github.html

class RepositoryHtmlSectionRegistry {
    private val adapters: Map<String, RepositoryHtmlSectionAdapter> = listOf(
        RepositoryProjectsHtmlAdapter(),
        RepositorySecurityHtmlAdapter(),
        RepositoryInsightsHtmlAdapter(),
        RepositoryWikiHtmlAdapter(),
        RepositoryAgentsHtmlAdapter(),
        RepositorySettingsHtmlAdapter()
    ).associateBy { it.sectionKey }

    fun get(sectionKey: String): RepositoryHtmlSectionAdapter? = adapters[sectionKey]

    fun parse(owner: String, repo: String, sectionKey: String, page: GitHubHtmlPage): GitHubHtmlParseResult<RepositoryHtmlSectionSummary> {
        val adapter = get(sectionKey) ?: return GitHubHtmlParseResult.ParseError(
            message = "当前分区尚未注册 HTML 解析适配器。",
            sourceUrl = page.url,
            statusCode = page.statusCode,
            htmlPreview = page.htmlPreview.takeIf { it.isNotBlank() }
        )
        return adapter.parse(owner, repo, page)
    }
}

class RepositoryHtmlFacade(
    private val gateway: GitHubHtmlGateway,
    private val registry: RepositoryHtmlSectionRegistry = RepositoryHtmlSectionRegistry()
) {
    fun loadRepositorySection(owner: String, repo: String, sectionKey: String): GitHubHtmlParseResult<RepositoryHtmlSectionSummary> {
        val adapter = registry.get(sectionKey) ?: return GitHubHtmlParseResult.ParseError(
            message = "当前分区尚未注册 HTML 解析适配器。",
            statusCode = null,
            htmlPreview = null
        )
        val page = gateway.getRepositorySectionPage(owner, repo, adapter.sectionPath)
        return registry.parse(owner, repo, sectionKey, page)
    }
}
