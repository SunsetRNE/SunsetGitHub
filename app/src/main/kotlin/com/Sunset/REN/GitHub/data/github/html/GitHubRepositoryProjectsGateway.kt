package com.Sunset.REN.GitHub.data.github.html

import com.Sunset.REN.GitHub.data.github.network.GitHubApiHeaders
import com.Sunset.REN.GitHub.data.github.network.GitHubHttpClient
import com.Sunset.REN.GitHub.data.github.network.GitHubHttpMethod
import com.Sunset.REN.GitHub.data.github.network.GitHubHttpRequest
import com.Sunset.REN.GitHub.data.github.network.GitHubHttpResponse
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException

class GitHubRepositoryProjectsGateway(
    accessToken: String
) {
    private val httpClient = GitHubHttpClient(accessToken, timeoutMillis = TimeoutMillis)

    fun loadProjectsSummary(owner: String, repo: String): GitHubHtmlParseResult<RepositoryHtmlSectionSummary> {
        val projectsUrl = "https://github.com/${owner.toProjectsPathSegment()}/${repo.toProjectsPathSegment()}/projects"
        val apiUrl = "https://api.github.com/repos/${owner.toProjectsPathSegment()}/${repo.toProjectsPathSegment()}"
        val repositoryResponse = try {
            request(apiUrl, accept = "application/vnd.github+json")
        } catch (error: SocketTimeoutException) {
            return GitHubHtmlParseResult.ParseError(
                message = "读取仓库项目状态超时。",
                sourceUrl = projectsUrl,
                statusCode = NetworkErrorStatusCode
            )
        } catch (error: IOException) {
            return GitHubHtmlParseResult.ParseError(
                message = error.message?.takeIf { it.isNotBlank() } ?: "读取仓库项目状态失败。",
                sourceUrl = projectsUrl,
                statusCode = NetworkErrorStatusCode
            )
        }
        val repositoryPreview = repositoryResponse.preview
        if (repositoryResponse.statusCode == HttpURLConnection.HTTP_FORBIDDEN) {
            return GitHubHtmlParseResult.AccessDenied(
                message = "当前账号无权读取仓库项目状态。",
                sourceUrl = projectsUrl,
                statusCode = repositoryResponse.statusCode,
                htmlPreview = repositoryPreview
            )
        }
        if (repositoryResponse.statusCode == HttpURLConnection.HTTP_NOT_FOUND) {
            return GitHubHtmlParseResult.NotFound(
                message = "仓库不存在，或当前令牌无法访问该仓库。",
                sourceUrl = projectsUrl,
                statusCode = repositoryResponse.statusCode,
                htmlPreview = repositoryPreview
            )
        }
        if (repositoryResponse.statusCode !in 200..299) {
            return GitHubHtmlParseResult.ParseError(
                message = "读取仓库项目状态失败：HTTP ${repositoryResponse.statusCode}。",
                sourceUrl = projectsUrl,
                statusCode = repositoryResponse.statusCode,
                htmlPreview = repositoryPreview
            )
        }

        val repository = runCatching { JSONObject(repositoryResponse.body) }.getOrElse {
            return GitHubHtmlParseResult.ParseError(
                message = "仓库项目状态返回内容异常。",
                sourceUrl = projectsUrl,
                statusCode = repositoryResponse.statusCode,
                htmlPreview = repositoryPreview
            )
        }
        val hasProjects = repository.optBoolean("has_projects", false)
        val defaultBranch = repository.optionalProjectsString("default_branch") ?: "未返回"
        val visibility = repository.optionalProjectsString("visibility") ?: if (repository.optBoolean("private", false)) "private" else "public"
        val permissions = repository.optJSONObject("permissions")
        val canAdmin = permissions?.optBoolean("admin", false) == true
        if (!hasProjects) {
            return GitHubHtmlParseResult.Success(
                buildSummary(
                    owner = owner,
                    repo = repo,
                    projectsUrl = projectsUrl,
                    status = RepositoryHtmlSectionStatus.Disabled,
                    description = "该仓库项目功能未开启，管理员可在本页开启。",
                    metrics = baseMetrics(hasProjects = false, defaultBranch = defaultBranch, visibility = visibility, canAdmin = canAdmin, projects = emptyList()),
                    notices = listOf("仓库项目功能当前处于关闭状态。", if (canAdmin) "当前账号具备管理员权限，可直接开启项目功能。" else "当前账号没有管理员权限，仅能查看状态。")
                )
            )
        }

        val projectsResponse = runCatching {
            request(projectsUrl, accept = "text/html,application/xhtml+xml")
        }
        val projects = projectsResponse.getOrNull()
            ?.takeIf { response -> response.statusCode in 200..299 }
            ?.body
            ?.extractRepositoryProjects(owner, repo)
            .orEmpty()
        val hasProjectPageSignals = projectsResponse.getOrNull()
            ?.takeIf { response -> response.statusCode in 200..299 }
            ?.body
            ?.containsProjectPageSignals(owner, repo)
            ?: false
        val pageFailure = projectsResponse.exceptionOrNull()?.message
            ?: projectsResponse.getOrNull()?.takeIf { response -> response.statusCode !in 200..299 }?.let { response ->
                "Projects 页面返回 HTTP ${response.statusCode}。"
            }
        val status = when {
            projects.isNotEmpty() -> RepositoryHtmlSectionStatus.Available
            pageFailure == null && hasProjectPageSignals -> RepositoryHtmlSectionStatus.Empty
            pageFailure == null -> RepositoryHtmlSectionStatus.ParsePartial
            else -> RepositoryHtmlSectionStatus.ParsePartial
        }
        val notices = buildList {
            add("仓库项目功能已开启。")
            when {
                projects.isNotEmpty() -> projects.take(ProjectNoticeLimit).forEachIndexed { index, project -> add("项目 ${index + 1}：${project.title}") }
                pageFailure == null && hasProjectPageSignals -> add("暂未发现可展示的项目，可能还没有创建项目。")
                pageFailure == null -> add("项目页面可访问，但暂未读取到可展示的项目卡片。")
            }
            pageFailure?.takeIf { it.isNotBlank() }?.let { add("页面检查：$it") }
        }
        return GitHubHtmlParseResult.Success(
            buildSummary(
                owner = owner,
                repo = repo,
                projectsUrl = projectsUrl,
                status = status,
                description = when (status) {
                    RepositoryHtmlSectionStatus.Available -> "已读取到项目摘要。"
                    RepositoryHtmlSectionStatus.Empty -> "仓库项目已开启，但暂未发现可展示的项目。"
                    else -> "仓库项目已开启，但暂时无法读取完整列表。"
                },
                metrics = baseMetrics(hasProjects = true, defaultBranch = defaultBranch, visibility = visibility, canAdmin = canAdmin, projects = projects),
                notices = notices
            )
        )
    }

    fun updateProjectsEnabled(owner: String, repo: String, enabled: Boolean): GitHubHtmlParseResult<RepositoryHtmlSectionSummary> {
        val apiUrl = "https://api.github.com/repos/${owner.toProjectsPathSegment()}/${repo.toProjectsPathSegment()}"
        val body = JSONObject().put("has_projects", enabled).toString()
        val response = try {
            request(apiUrl, accept = "application/vnd.github+json", method = "PATCH", body = body)
        } catch (error: SocketTimeoutException) {
            return GitHubHtmlParseResult.ParseError(
                message = "更新仓库项目开关超时。",
                sourceUrl = "https://github.com/${owner.toProjectsPathSegment()}/${repo.toProjectsPathSegment()}/projects",
                statusCode = NetworkErrorStatusCode
            )
        } catch (error: IOException) {
            return GitHubHtmlParseResult.ParseError(
                message = error.message?.takeIf { it.isNotBlank() } ?: "更新仓库项目开关失败。",
                sourceUrl = "https://github.com/${owner.toProjectsPathSegment()}/${repo.toProjectsPathSegment()}/projects",
                statusCode = NetworkErrorStatusCode
            )
        }
        if (response.statusCode !in 200..299) {
            return GitHubHtmlParseResult.ParseError(
                message = response.toUpdateErrorMessage("更新项目开关失败"),
                sourceUrl = "https://github.com/${owner.toProjectsPathSegment()}/${repo.toProjectsPathSegment()}/projects",
                statusCode = response.statusCode,
                htmlPreview = response.preview
            )
        }
        return loadProjectsSummary(owner, repo)
    }

    private fun request(url: String, accept: String, method: String = "GET", body: String? = null): ProjectsNetworkResponse {
        return httpClient.execute(
            GitHubHttpRequest(
                pathOrUrl = url,
                method = method.toGitHubHttpMethod(),
                body = body,
                accept = accept,
                apiVersion = GitHubApiHeaders.LegacyApiVersion,
                followRedirects = true
            )
        ).toProjectsNetworkResponse()
    }

    private fun buildSummary(
        owner: String,
        repo: String,
        projectsUrl: String,
        status: RepositoryHtmlSectionStatus,
        description: String,
        metrics: List<RepositoryHtmlMetric>,
        notices: List<String>
    ): RepositoryHtmlSectionSummary {
        return RepositoryHtmlSectionSummary(
            owner = owner,
            repo = repo,
            sectionKey = SectionKey,
            title = "项目",
            status = status,
            description = description,
            metrics = metrics,
            notices = notices.distinct().take(MaxNotices),
            actions = listOf(
                "管理员可在本页开启或关闭仓库项目功能。",
                "看板、表格、路线图和自动化设置可在网页端查看。"
            ),
            sourceUrl = projectsUrl
        )
    }

    private fun baseMetrics(
        hasProjects: Boolean,
        defaultBranch: String,
        visibility: String,
        canAdmin: Boolean,
        projects: List<RepositoryProjectSummary>
    ): List<RepositoryHtmlMetric> {
        return buildList {
            add(RepositoryHtmlMetric("数据来源", "仓库设置与项目页面"))
            add(RepositoryHtmlMetric("功能开关", if (hasProjects) "开启" else "关闭"))
            add(RepositoryHtmlMetric("管理权限", if (canAdmin) "可修改" else "只读"))
            add(RepositoryHtmlMetric("默认分支", defaultBranch))
            add(RepositoryHtmlMetric("可见性", visibility))
            add(RepositoryHtmlMetric("项目数量", projects.size.toString()))
            projects.take(ProjectNoticeLimit).forEachIndexed { index, project ->
                val ordinal = index + 1
                add(RepositoryHtmlMetric("项目 $ordinal", project.title))
                add(RepositoryHtmlMetric("项目 $ordinal 编号", project.identifier))
                add(RepositoryHtmlMetric("项目 $ordinal 类型", project.typeLabel))
                add(RepositoryHtmlMetric("项目 $ordinal 链接", project.url))
            }
        }
    }

    private fun GitHubHttpResponse.toProjectsNetworkResponse(): ProjectsNetworkResponse {
        return ProjectsNetworkResponse(statusCode = statusCode, body = body)
    }

    private fun String.toGitHubHttpMethod(): GitHubHttpMethod {
        return when (uppercase()) {
            "GET" -> GitHubHttpMethod.GET
            "POST" -> GitHubHttpMethod.POST
            "PATCH" -> GitHubHttpMethod.PATCH
            "PUT" -> GitHubHttpMethod.PUT
            "DELETE" -> GitHubHttpMethod.DELETE
            else -> throw IllegalArgumentException("Unsupported GitHub HTTP method: $this")
        }
    }

    private data class ProjectsNetworkResponse(
        val statusCode: Int,
        val body: String
    ) {
        val preview: String get() = body.take(240).replace(Regex("\\s+"), " ").trim()

        fun toUpdateErrorMessage(prefix: String): String {
            val json = runCatching { JSONObject(body) }.getOrNull()
            val message = json?.optionalProjectsString("message")
            val errors = json?.optJSONArray("errors")
            val detail = buildList {
                if (!message.isNullOrBlank()) add(message)
                if (errors != null) {
                    for (index in 0 until errors.length()) {
                        val item = errors.opt(index)?.toString()?.takeIf { it.isNotBlank() } ?: continue
                        add(item)
                    }
                }
            }.joinToString("；")
            return if (detail.isNotBlank()) "$prefix：$detail" else "$prefix：HTTP $statusCode"
        }
    }

    private companion object {
        private const val SectionKey = "projects"
        private const val TimeoutMillis = 15_000
        private const val NetworkErrorStatusCode = 0
        private const val ProjectNoticeLimit = 6
        private const val MaxNotices = 8
    }
}

private data class RepositoryProjectSummary(
    val title: String,
    val url: String,
    val identifier: String,
    val typeLabel: String
)

private fun String.extractRepositoryProjects(owner: String, repo: String): List<RepositoryProjectSummary> {
    val ownerPattern = Regex.escape(owner)
    val repoPattern = Regex.escape(repo)
    val projectLinkRegex = Regex(
        """<a\b(?=[^>]+href=["']([^"']*(?:/$ownerPattern/$repoPattern/projects|/(?:orgs|users)/$ownerPattern/projects)/(?:classic/)?(?:\d+|[A-Za-z0-9_-]{12,})(?:/views/\d+)?[^"']*)["'])[^>]*>(.*?)</a>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    return projectLinkRegex.findAll(this)
        .mapNotNull { match ->
            val href = match.groupValues.getOrNull(1).orEmpty().decodeHtmlEntities().trim()
            val title = match.groupValues.getOrNull(2).orEmpty().toPlainProjectText().toReliableProjectNameOrNull() ?: return@mapNotNull null
            val url = href.toGitHubProjectUrlOrNull() ?: return@mapNotNull null
            RepositoryProjectSummary(
                title = title,
                url = url,
                identifier = url.toProjectIdentifier(),
                typeLabel = if (url.contains("/classic/", ignoreCase = true)) "Classic" else "Projects v2"
            )
        }
        .distinctBy { project -> project.url.ifBlank { project.title } }
        .take(20)
        .toList()
}

private fun String.toGitHubProjectUrlOrNull(): String? {
    val normalized = trim().replace("&amp;", "&")
    if (normalized.isBlank() || normalized.startsWith("#") || normalized.startsWith("mailto:", ignoreCase = true)) return null
    val withoutFragment = normalized.substringBefore("#")
    return when {
        withoutFragment.startsWith("https://github.com/", ignoreCase = true) -> withoutFragment
        withoutFragment.startsWith("/") -> "https://github.com$withoutFragment"
        else -> null
    }
}

private fun String.toProjectIdentifier(): String {
    return Regex("""/projects/(?:classic/)?([^/?#]+)""", RegexOption.IGNORE_CASE)
        .find(this)
        ?.groupValues
        ?.getOrNull(1)
        ?.takeIf { it.isNotBlank() }
        ?: "未返回"
}

private fun String.containsProjectPageSignals(owner: String, repo: String): Boolean {
    val ownerPattern = Regex.escape(owner)
    val repoPattern = Regex.escape(repo)
    val repositoryProjectsSignal = Regex(
        """/$ownerPattern/$repoPattern/projects(?:["'/?#]|$)""",
        RegexOption.IGNORE_CASE
    )
    return repositoryProjectsSignal.containsMatchIn(this) || contains("Repository projects", ignoreCase = true)
}

private fun String.toReliableProjectNameOrNull(): String? {
    val normalized = removePrefix("Project: ").trim()
    if (normalized.isBlank() || normalized.length > 120) return null
    val lower = normalized.lowercase()
    if (ProjectTextBlocklist.any { blocked -> lower == blocked || lower.contains(blocked) }) return null
    if (normalized.count { it.isLetterOrDigit() } < 2) return null
    return normalized
}

private val ProjectTextBlocklist = setOf(
    "navigation menu",
    "search code",
    "repositories",
    "users",
    "issues",
    "pull requests",
    "provide feedback",
    "saved searches",
    "use saved searches",
    "repository projects",
    "new project",
    "projects",
    "github"
)

private fun String.toPlainProjectText(): String {
    return replace(Regex("<[^>]+>"), " ")
        .decodeHtmlEntities()
        .normalizeWhitespace()
}

private fun JSONObject.optionalProjectsString(name: String): String? {
    if (isNull(name)) return null
    return optString(name).takeIf { it.isNotBlank() && it != "null" }
}

private fun String.toProjectsPathSegment(): String {
    return java.net.URLEncoder.encode(this, Charsets.UTF_8.name()).replace("+", "%20")
}
