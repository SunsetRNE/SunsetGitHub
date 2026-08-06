package com.Sunset.REN.GitHub.data.github.html

import com.Sunset.REN.GitHub.data.github.GitHubRepositoryApiGateway
import com.Sunset.REN.GitHub.domain.repo.GitHubRepository
import com.Sunset.REN.GitHub.domain.repo.RepositorySecurityAlert
import com.Sunset.REN.GitHub.domain.repo.RepositorySecurityProbe
import com.Sunset.REN.GitHub.domain.repo.RepositorySecurityProbeStatus
import com.Sunset.REN.GitHub.domain.repo.RepositorySecuritySummary

class GitHubRepositorySecurityQualityGateway(
    private val accessToken: String
) {
    suspend fun loadSecurityQualitySummary(owner: String, repo: String): GitHubHtmlParseResult<RepositoryHtmlSectionSummary> {
        val sourceUrl = "https://github.com/${owner.toSecurityQualityPathSegment()}/${repo.toSecurityQualityPathSegment()}/security"
        val gateway = GitHubRepositoryApiGateway(accessToken)
        val repositoryResult = runCatching { gateway.getRepository(owner, repo, includeLanguages = false) }
        val securityResult = runCatching { gateway.getRepositorySecuritySummary(owner, repo) }
        val security = securityResult.getOrElse { error ->
            return GitHubHtmlParseResult.ParseError(
                message = error.message?.takeIf { it.isNotBlank() } ?: "GitHub REST 安全摘要请求失败。",
                sourceUrl = sourceUrl
            )
        }

        return GitHubHtmlParseResult.Success(
            RepositoryHtmlSectionSummary(
                owner = owner,
                repo = repo,
                sectionKey = SectionKey,
                title = "Security / Quality",
                status = security.toSectionStatus(),
                description = "HTML Security 页面未能稳定解析，已改用 GitHub REST 安全接口生成只读摘要。",
                metrics = buildMetrics(
                    repository = repositoryResult.getOrNull(),
                    security = security
                ),
                notices = buildNotices(
                    security = security,
                    repositoryResult = repositoryResult,
                    securityResult = securityResult
                ),
                actions = listOf(
                    "REST 摘要可稳定展示安全策略、Dependabot、Code scanning 和 Secret scanning 探测状态。",
                    "部分安全接口是否可读取取决于仓库设置、仓库权限和 token scope。"
                ),
                sourceUrl = sourceUrl
            )
        )
    }

    private fun RepositorySecuritySummary.toSectionStatus(): RepositoryHtmlSectionStatus {
        return when {
            probes.any { probe -> probe.status == RepositorySecurityProbeStatus.Available } -> RepositoryHtmlSectionStatus.Available
            probes.any { probe -> probe.status == RepositorySecurityProbeStatus.Empty } -> RepositoryHtmlSectionStatus.Empty
            probes.any { probe -> probe.status == RepositorySecurityProbeStatus.Error || probe.status == RepositorySecurityProbeStatus.Inaccessible } -> RepositoryHtmlSectionStatus.ParsePartial
            else -> RepositoryHtmlSectionStatus.Disabled
        }
    }

    private fun buildMetrics(
        repository: GitHubRepository?,
        security: RepositorySecuritySummary
    ): List<RepositoryHtmlMetric> {
        return buildList {
            add(RepositoryHtmlMetric("数据来源", "GitHub REST 安全接口"))
            add(RepositoryHtmlMetric("可用/空安全项", security.availableCount.toString()))
            add(RepositoryHtmlMetric("不可用安全项", security.unavailableCount.toString()))
            add(RepositoryHtmlMetric("告警样本数", security.alerts.size.toString()))
            repository?.let {
                add(RepositoryHtmlMetric("默认分支", it.defaultBranch))
                add(RepositoryHtmlMetric("可见性", if (it.isPrivate) "private" else "public"))
            }
            security.probes.take(ProbeMetricLimit).forEach { probe ->
                add(RepositoryHtmlMetric(probe.title, probe.value ?: probe.status.name))
            }
        }
    }

    private fun buildNotices(
        security: RepositorySecuritySummary,
        repositoryResult: Result<GitHubRepository>,
        securityResult: Result<RepositorySecuritySummary>
    ): List<String> {
        return buildList {
            security.probes.take(ProbeNoticeLimit).forEach { probe -> add(probe.toNotice()) }
            security.alerts.take(AlertNoticeLimit).forEach { alert -> add(alert.toNotice()) }
            security.notices.forEach { notice -> add(notice) }
            repositoryResult.exceptionOrNull()?.message?.takeIf { it.isNotBlank() }?.let { message ->
                add("仓库详情探测失败：$message")
            }
            securityResult.exceptionOrNull()?.message?.takeIf { it.isNotBlank() }?.let { message ->
                add("安全摘要探测失败：$message")
            }
        }.distinct().take(MaxNotices)
    }

    private fun RepositorySecurityProbe.toNotice(): String {
        val valueText = value?.takeIf { it.isNotBlank() } ?: status.name
        val detailText = detail?.takeIf { it.isNotBlank() }?.let { "，$it" }.orEmpty()
        return "$title：$valueText$detailText"
    }

    private fun RepositorySecurityAlert.toNotice(): String {
        val severityText = severity?.takeIf { it.isNotBlank() }?.let { " [$it]" }.orEmpty()
        val numberText = number?.let { "#$it " }.orEmpty()
        return "$source 告警：$numberText$title$severityText ($state)"
    }

    private companion object {
        private const val SectionKey = "security_quality"
        private const val ProbeMetricLimit = 6
        private const val ProbeNoticeLimit = 6
        private const val AlertNoticeLimit = 3
        private const val MaxNotices = 10
    }
}

private fun String.toSecurityQualityPathSegment(): String {
    return java.net.URLEncoder.encode(this, Charsets.UTF_8.name()).replace("+", "%20")
}
