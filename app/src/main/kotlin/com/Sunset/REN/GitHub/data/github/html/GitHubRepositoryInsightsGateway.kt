package com.Sunset.REN.GitHub.data.github.html

import com.Sunset.REN.GitHub.data.github.GitHubRepositoryApiGateway
import com.Sunset.REN.GitHub.domain.repo.GitHubRepository
import com.Sunset.REN.GitHub.domain.repo.RepositoryContributor
import com.Sunset.REN.GitHub.domain.repo.RepositoryActionRun
import com.Sunset.REN.GitHub.domain.repo.RepositoryIssue
import com.Sunset.REN.GitHub.domain.repo.RepositoryPullRequest
import com.Sunset.REN.GitHub.domain.repo.RepositoryRelease
import com.Sunset.REN.GitHub.domain.repo.RepositorySecurityProbeStatus
import com.Sunset.REN.GitHub.domain.repo.RepositorySecuritySummary

class GitHubRepositoryInsightsGateway(
    private val accessToken: String
) {
    suspend fun loadInsightsSummary(owner: String, repo: String): GitHubHtmlParseResult<RepositoryHtmlSectionSummary> {
        val sourceUrl = "https://github.com/${owner.toInsightsPathSegment()}/${repo.toInsightsPathSegment()}/pulse"
        val gateway = GitHubRepositoryApiGateway(accessToken)
        val repositoryResult = runCatching { gateway.getRepository(owner, repo, includeLanguages = true) }
        val repository = repositoryResult.getOrElse { error ->
            return GitHubHtmlParseResult.ParseError(
                message = error.message?.takeIf { it.isNotBlank() } ?: "GitHub REST 仓库洞察请求失败。",
                sourceUrl = sourceUrl
            )
        }

        val releasesResult = runCatching { gateway.listRepositoryReleases(owner, repo, page = 1, perPage = PreviewLimit) }
        val pullRequestsResult = runCatching { gateway.listRepositoryPullRequests(owner, repo, state = "all", page = 1, perPage = PreviewLimit) }
        val issuesResult = runCatching { gateway.listRepositoryIssues(owner, repo, state = "all", page = 1, perPage = PreviewLimit) }
        val actionsResult = runCatching { gateway.listRepositoryActionRuns(owner, repo, page = 1, perPage = PreviewLimit) }
        val securityResult = runCatching { gateway.getRepositorySecuritySummary(owner, repo) }
        val readmeResult = runCatching { gateway.repositoryContentPathExists(owner, repo, "README.md") }
        val licenseResult = runCatching { gateway.repositoryLicensePresent(owner, repo) }
        val issueTemplateResult = runCatching {
            IssueTemplateProbePaths.any { path -> gateway.repositoryContentPathExists(owner, repo, path) }
        }
        val trafficViewsResult = runCatching { gateway.getRepositoryTrafficViews(owner, repo) }
        val trafficClonesResult = runCatching { gateway.getRepositoryTrafficClones(owner, repo) }
        val commitActivityResult = runCatching { gateway.getRepositoryCommitActivity(owner, repo) }
        val contributorsResult = runCatching { gateway.listRepositoryContributorsPreview(owner, repo, perPage = ContributorsPreviewLimit) }

        return GitHubHtmlParseResult.Success(
            RepositoryHtmlSectionSummary(
                owner = owner,
                repo = repo,
                sectionKey = SectionKey,
                title = "洞察",
                status = RepositoryHtmlSectionStatus.Available,
                description = "GitHub 洞察页面未能稳定解析，已改用 GitHub REST 数据生成仓库洞察摘要。",
                metrics = buildMetrics(
                    repository = repository,
                    releases = releasesResult.getOrNull().orEmpty(),
                    pullRequests = pullRequestsResult.getOrNull().orEmpty(),
                    issues = issuesResult.getOrNull().orEmpty(),
                    actions = actionsResult.getOrNull().orEmpty(),
                    security = securityResult.getOrNull(),
                    readmePresent = readmeResult.getOrNull(),
                    licensePresent = licenseResult.getOrNull(),
                    issueTemplatesPresent = issueTemplateResult.getOrNull(),
                    trafficViews = trafficViewsResult.getOrNull(),
                    trafficClones = trafficClonesResult.getOrNull(),
                    trafficRestricted = trafficViewsResult.isFailure || trafficClonesResult.isFailure,
                    commitActivity = commitActivityResult.getOrNull().orEmpty(),
                    contributors = contributorsResult.getOrNull().orEmpty()
                ),
                notices = buildNotices(
                    repository = repository,
                    releasesResult = releasesResult,
                    pullRequestsResult = pullRequestsResult,
                    issuesResult = issuesResult,
                    actionsResult = actionsResult,
                    securityResult = securityResult,
                    readmeResult = readmeResult,
                    licenseResult = licenseResult,
                    issueTemplateResult = issueTemplateResult,
                    trafficViewsResult = trafficViewsResult,
                    trafficClonesResult = trafficClonesResult,
                    commitActivityResult = commitActivityResult,
                    contributorsResult = contributorsResult
                ),
                actions = buildActions(
                    readmePresent = readmeResult.getOrNull(),
                    licensePresent = licenseResult.getOrNull(),
                    issueTemplatesPresent = issueTemplateResult.getOrNull(),
                    trafficRestricted = trafficViewsResult.isFailure || trafficClonesResult.isFailure
                ),
                sourceUrl = sourceUrl
            )
        )
    }

    private fun buildMetrics(
        repository: GitHubRepository,
        releases: List<RepositoryRelease>,
        pullRequests: List<RepositoryPullRequest>,
        issues: List<RepositoryIssue>,
        actions: List<RepositoryActionRun>,
        security: RepositorySecuritySummary?,
        readmePresent: Boolean?,
        licensePresent: Boolean?,
        issueTemplatesPresent: Boolean?,
        trafficViews: Int?,
        trafficClones: Int?,
        trafficRestricted: Boolean,
        commitActivity: List<Int>,
        contributors: List<RepositoryContributor>
    ): List<RepositoryHtmlMetric> {
        val pureIssues = issues.filterNot { issue -> issue.isPullRequest }
        val failedRuns = actions.count { run -> run.conclusion == "failure" || run.conclusion == "timed_out" || run.conclusion == "cancelled" }
        val availableSecurityProbes = security?.probes.orEmpty().count { probe -> probe.status == RepositorySecurityProbeStatus.Available }
        val commitActivityValue = commitActivity.joinToString(separator = ",").ifBlank { "--" }
        val topContributor = contributors.maxByOrNull { contributor -> contributor.contributions }
        return listOf(
            RepositoryHtmlMetric("数据来源", "GitHub REST 接口"),
            RepositoryHtmlMetric("星标", repository.stargazersCount.toString()),
            RepositoryHtmlMetric("复刻", repository.forksCount.toString()),
            RepositoryHtmlMetric("关注者", repository.watchersCount.toString()),
            RepositoryHtmlMetric("未关闭议题", repository.openIssuesCount.toString()),
            RepositoryHtmlMetric("默认分支", repository.defaultBranch),
            RepositoryHtmlMetric("语言", repository.language ?: repository.languages.firstOrNull()?.name ?: "未返回"),
            RepositoryHtmlMetric("最近 Releases", releases.size.toString()),
            RepositoryHtmlMetric("最近 PR", pullRequests.size.toString()),
            RepositoryHtmlMetric("最近 Issues", pureIssues.size.toString()),
            RepositoryHtmlMetric("最近 Actions", actions.size.toString()),
            RepositoryHtmlMetric("失败 Actions", failedRuns.toString()),
            RepositoryHtmlMetric("安全探测可用项", availableSecurityProbes.toString()),
            RepositoryHtmlMetric("README", readmePresent.toPresenceText()),
            RepositoryHtmlMetric("License", licensePresent.toPresenceText()),
            RepositoryHtmlMetric("Issue templates", issueTemplatesPresent.toPresenceText()),
            RepositoryHtmlMetric("Traffic views", trafficViews?.toString() ?: if (trafficRestricted) "权限受限" else "--"),
            RepositoryHtmlMetric("Traffic clones", trafficClones?.toString() ?: if (trafficRestricted) "权限受限" else "--"),
            RepositoryHtmlMetric("Commit activity", commitActivityValue),
            RepositoryHtmlMetric("贡献者", contributors.size.toString()),
            RepositoryHtmlMetric("主要贡献者", topContributor?.let { "${it.login} (${it.contributions})" } ?: "--")
        )
    }

    private fun buildNotices(
        repository: GitHubRepository,
        releasesResult: Result<List<RepositoryRelease>>,
        pullRequestsResult: Result<List<RepositoryPullRequest>>,
        issuesResult: Result<List<RepositoryIssue>>,
        actionsResult: Result<List<RepositoryActionRun>>,
        securityResult: Result<RepositorySecuritySummary>,
        readmeResult: Result<Boolean>,
        licenseResult: Result<Boolean>,
        issueTemplateResult: Result<Boolean>,
        trafficViewsResult: Result<Int>,
        trafficClonesResult: Result<Int>,
        commitActivityResult: Result<List<Int>>,
        contributorsResult: Result<List<RepositoryContributor>>
    ): List<String> {
        val latestRelease = releasesResult.getOrNull().orEmpty().firstOrNull()
        val latestPullRequest = pullRequestsResult.getOrNull().orEmpty().firstOrNull()
        val latestIssue = issuesResult.getOrNull().orEmpty().firstOrNull { issue -> !issue.isPullRequest }
        val latestAction = actionsResult.getOrNull().orEmpty().firstOrNull()
        val security = securityResult.getOrNull()
        val probeText = security?.probes.orEmpty()
            .take(3)
            .joinToString { probe -> "${probe.title}：${probe.status.toLocalizedInsightStatus()}" }
        return buildList {
            repository.description?.takeIf { it.isNotBlank() }?.let { add("仓库简介：$it") }
            latestRelease?.let { add("最近发布：${it.name} (${it.tagName})") }
            latestPullRequest?.let { add("最近拉取请求：#${it.number} ${it.title} [${it.state.toLocalizedInsightStatus()}]") }
            latestIssue?.let { add("最近议题：#${it.number} ${it.title} [${it.state.toLocalizedInsightStatus()}]") }
            latestAction?.let {
                val stateText = listOf(it.status, it.conclusion)
                    .mapNotNull { state -> state?.takeIf(String::isNotBlank)?.toLocalizedInsightStatus() }
                    .distinct()
                    .joinToString(" / ")
                add("最近自动化：${it.name}${stateText.takeIf(String::isNotBlank)?.let { state -> " [$state]" }.orEmpty()}")
            }
            if (probeText.isNotBlank()) add("安全探测：$probeText")
            readmeResult.getOrNull()?.let { add("说明文档：${it.toPresenceText()}") }
            licenseResult.getOrNull()?.let { add("许可证：${it.toPresenceText()}") }
            issueTemplateResult.getOrNull()?.let { add("议题模板：${it.toPresenceText()}") }
            trafficViewsResult.getOrNull()?.let { add("浏览量：$it") }
            trafficClonesResult.getOrNull()?.let { add("克隆量：$it") }
            commitActivityResult.getOrNull()?.takeIf { it.isNotEmpty() }?.let { activity -> add("提交活跃度：最近 ${activity.size} 周共 ${activity.sum()} 次提交") }
            contributorsResult.getOrNull()?.takeIf { it.isNotEmpty() }?.let { contributors ->
                add("主要贡献者：${contributors.maxByOrNull { it.contributions }?.login.orEmpty()} / ${contributors.size} 位贡献者预览")
            }
            addResultFailure("发布", releasesResult)
            addResultFailure("拉取请求", pullRequestsResult)
            addResultFailure("议题", issuesResult)
            addResultFailure("自动化", actionsResult)
            addResultFailure("安全", securityResult)
            addResultFailure("说明文档", readmeResult)
            addResultFailure("许可证", licenseResult)
            addResultFailure("议题模板", issueTemplateResult)
            addResultFailure("浏览量", trafficViewsResult)
            addResultFailure("克隆量", trafficClonesResult)
            addResultFailure("提交活跃度", commitActivityResult)
            addResultFailure("贡献者", contributorsResult)
        }.distinct().take(MaxNotices)
    }

    private fun buildActions(
        readmePresent: Boolean?,
        licensePresent: Boolean?,
        issueTemplatesPresent: Boolean?,
        trafficRestricted: Boolean
    ): List<String> {
        return buildList {
            add("REST 摘要可稳定展示仓库活跃度、最近拉取请求、议题、发布、自动化和安全探测概况。")
            if (readmePresent == false) add("建议补充说明文档，提升仓库说明和新访客理解效率。")
            if (licensePresent == false) add("建议补齐许可证，明确开源使用边界。")
            if (issueTemplatesPresent == false) add("建议添加议题模板，提升协作质量。")
            if (trafficRestricted) add("浏览量和克隆量需要推送权限；当前以受限卡片降级展示。")
        }.distinct()
    }

    private fun MutableList<String>.addResultFailure(label: String, result: Result<*>) {
        result.exceptionOrNull()?.message?.takeIf { it.isNotBlank() }?.let { message ->
            add("$label 探测失败：$message")
        }
    }

    private fun Boolean?.toPresenceText(): String {
        return when (this) {
            true -> "已配置"
            false -> "未配置"
            null -> "未知"
        }
    }

    private fun String.toLocalizedInsightStatus(): String = when (lowercase()) {
        "open" -> "进行中"
        "closed" -> "已关闭"
        "queued" -> "排队中"
        "in_progress" -> "运行中"
        "completed" -> "已完成"
        "waiting" -> "等待中"
        "pending" -> "待处理"
        "requested" -> "已请求"
        "success" -> "成功"
        "failure" -> "失败"
        "cancelled" -> "已取消"
        "skipped" -> "已跳过"
        "neutral" -> "无结果"
        "timed_out" -> "已超时"
        "action_required" -> "需要处理"
        "stale" -> "已过期"
        else -> this
    }

    private fun RepositorySecurityProbeStatus.toLocalizedInsightStatus(): String = when (this) {
        RepositorySecurityProbeStatus.Available -> "可用"
        RepositorySecurityProbeStatus.Empty -> "暂无数据"
        RepositorySecurityProbeStatus.Disabled -> "未启用"
        RepositorySecurityProbeStatus.Inaccessible -> "无权访问"
        RepositorySecurityProbeStatus.Error -> "探测失败"
    }

    private companion object {
        private const val SectionKey = "insights"
        private const val PreviewLimit = 5
        private const val ContributorsPreviewLimit = 6
        private const val MaxNotices = 12
        private val IssueTemplateProbePaths = listOf(
            ".github/ISSUE_TEMPLATE.md",
            ".github/ISSUE_TEMPLATE",
            "ISSUE_TEMPLATE.md",
            "docs/ISSUE_TEMPLATE.md"
        )
    }
}

private fun String.toInsightsPathSegment(): String {
    return java.net.URLEncoder.encode(this, Charsets.UTF_8.name()).replace("+", "%20")
}
