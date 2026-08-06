package com.Sunset.REN.GitHub.data.github.html

import com.Sunset.REN.GitHub.data.github.network.GitHubApiHeaders
import com.Sunset.REN.GitHub.data.github.network.GitHubHttpClient
import com.Sunset.REN.GitHub.data.github.network.GitHubHttpRequest
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.util.Locale

class GitHubRepositoryAgentsGateway(
    accessToken: String
) {
    private val httpClient = GitHubHttpClient(accessToken, timeoutMillis = TimeoutMillis)
    fun loadAgentsSummary(owner: String, repo: String): GitHubHtmlParseResult<RepositoryHtmlSectionSummary> {
        return when (val result = loadAgentsPage(owner, repo)) {
            is GitHubHtmlParseResult.Success -> GitHubHtmlParseResult.Success(result.value.summary)
            is GitHubHtmlParseResult.AccessDenied -> result
            is GitHubHtmlParseResult.NotFound -> result
            is GitHubHtmlParseResult.ParseError -> result
        }
    }

    fun loadAgentsPage(owner: String, repo: String): GitHubHtmlParseResult<RepositoryAgentsPageSummary> {
        val agentsUrl = "https://github.com/${owner.toAgentsPathSegment()}/${repo.toAgentsPathSegment()}/copilot"
        val apiUrl = "https://api.github.com/repos/${owner.toAgentsPathSegment()}/${repo.toAgentsPathSegment()}"
        val repositoryResponse = try {
            request(apiUrl, accept = "application/vnd.github+json")
        } catch (error: SocketTimeoutException) {
            return GitHubHtmlParseResult.ParseError(
                message = "GitHub REST Agents 仓库状态请求超时。",
                sourceUrl = agentsUrl,
                statusCode = NetworkErrorStatusCode
            )
        } catch (error: IOException) {
            return GitHubHtmlParseResult.ParseError(
                message = error.message?.takeIf { it.isNotBlank() } ?: "GitHub REST Agents 仓库状态请求失败。",
                sourceUrl = agentsUrl,
                statusCode = NetworkErrorStatusCode
            )
        }
        val repositoryPreview = repositoryResponse.preview
        if (repositoryResponse.statusCode == HttpURLConnection.HTTP_FORBIDDEN) {
            return GitHubHtmlParseResult.AccessDenied(
                message = "当前令牌无法读取仓库 Agents 诊断所需的仓库状态。",
                sourceUrl = agentsUrl,
                statusCode = repositoryResponse.statusCode,
                htmlPreview = repositoryPreview
            )
        }
        if (repositoryResponse.statusCode == HttpURLConnection.HTTP_NOT_FOUND) {
            return GitHubHtmlParseResult.NotFound(
                message = "仓库不存在，或当前令牌无法访问该仓库。",
                sourceUrl = agentsUrl,
                statusCode = repositoryResponse.statusCode,
                htmlPreview = repositoryPreview
            )
        }
        if (repositoryResponse.statusCode !in 200..299) {
            return GitHubHtmlParseResult.ParseError(
                message = "GitHub REST Agents 仓库状态返回 HTTP ${repositoryResponse.statusCode}。",
                sourceUrl = agentsUrl,
                statusCode = repositoryResponse.statusCode,
                htmlPreview = repositoryPreview
            )
        }

        val repository = runCatching { JSONObject(repositoryResponse.body) }.getOrElse {
            return GitHubHtmlParseResult.ParseError(
                message = "GitHub REST Agents 仓库状态返回内容不是有效 JSON。",
                sourceUrl = agentsUrl,
                statusCode = repositoryResponse.statusCode,
                htmlPreview = repositoryPreview
            )
        }
        val defaultBranch = repository.optionalAgentsString("default_branch") ?: "未返回"
        val visibility = repository.optionalAgentsString("visibility") ?: if (repository.optBoolean("private", false)) "private" else "public"

        val agentsResponse = runCatching {
            request(agentsUrl, accept = "text/html,application/xhtml+xml")
        }
        val response = agentsResponse.getOrNull()
        val body = response?.body.orEmpty()
        val sessions = when {
            response?.statusCode in 200..299 && !body.isSignInPage() && !body.isCopilotMarketingPage() -> {
                body.extractAgentSessions(owner = owner, repo = repo, sourceUrl = agentsUrl)
            }
            else -> emptyList()
        }
        val diagnosis = when {
            sessions.isNotEmpty() -> AgentsDiagnosis(
                status = RepositoryHtmlSectionStatus.Available,
                state = "发现 Sessions",
                notice = "已从 GitHub Agents 页面解析到 ${sessions.size} 个 session；该能力基于 HTML 页面结构，属于实验性解析。",
                signals = sessions.map { it.title }.take(SignalNoticeLimit)
            )
            agentsResponse.exceptionOrNull() != null -> AgentsDiagnosis(
                status = RepositoryHtmlSectionStatus.ParsePartial,
                state = "页面请求失败",
                notice = agentsResponse.exceptionOrNull()?.message ?: "Copilot / Agents 页面请求失败。"
            )
            response == null -> AgentsDiagnosis(
                status = RepositoryHtmlSectionStatus.ParsePartial,
                state = "无页面响应",
                notice = "Copilot / Agents 页面没有返回响应。"
            )
            response.statusCode == HttpURLConnection.HTTP_FORBIDDEN -> AgentsDiagnosis(
                status = RepositoryHtmlSectionStatus.AccessDenied,
                state = "无权限或 feature gate",
                notice = "Copilot / Agents 页面返回 HTTP 403，可能需要组织权限、订阅或功能开关。"
            )
            response.statusCode == HttpURLConnection.HTTP_NOT_FOUND -> AgentsDiagnosis(
                status = RepositoryHtmlSectionStatus.Disabled,
                state = "未开放或未启用",
                notice = "Copilot / Agents 页面返回 HTTP 404，当前仓库或账号可能没有该功能入口。"
            )
            response.statusCode !in 200..299 -> AgentsDiagnosis(
                status = RepositoryHtmlSectionStatus.ParsePartial,
                state = "页面异常",
                notice = "Copilot / Agents 页面返回 HTTP ${response.statusCode}。"
            )
            body.isSignInPage() -> AgentsDiagnosis(
                status = RepositoryHtmlSectionStatus.AccessDenied,
                state = "需要网页登录",
                notice = "Copilot / Agents 页面返回登录入口，当前 Web 会话可能未登录或 cookie 不可用。"
            )
            body.isCopilotFeatureGate() -> AgentsDiagnosis(
                status = RepositoryHtmlSectionStatus.Disabled,
                state = "功能未开放",
                notice = "页面显示 Copilot / Agents 功能未开放、不可用或未为该仓库启用。"
            )
            body.isCopilotMarketingPage() -> AgentsDiagnosis(
                status = RepositoryHtmlSectionStatus.Empty,
                state = "营销或介绍页",
                notice = "页面更像 Copilot 介绍/营销页，没有返回仓库级 Agents 配置。"
            )
            body.extractAgentSignals().isNotEmpty() -> AgentsDiagnosis(
                status = RepositoryHtmlSectionStatus.ParsePartial,
                state = "发现页面信号",
                notice = "页面包含 Copilot / Agents 相关信号，但未能稳定解析 session 列表。",
                signals = body.extractAgentSignals()
            )
            else -> AgentsDiagnosis(
                status = RepositoryHtmlSectionStatus.ParsePartial,
                state = "未识别配置",
                notice = "Copilot / Agents 页面可访问，但没有匹配到可靠仓库级 session 数据。"
            )
        }

        val summary = RepositoryHtmlSectionSummary(
            owner = owner,
            repo = repo,
            sectionKey = SectionKey,
            title = "Agents",
            status = diagnosis.status,
            description = if (sessions.isNotEmpty()) {
                "GitHub Agents sessions 已通过页面解析加载；结果会随 GitHub 页面结构变化而变化。"
            } else {
                "GitHub Copilot / Agents 暂无稳定仓库级 REST 摘要接口，当前展示页面诊断结果。"
            },
            metrics = buildMetrics(
                defaultBranch = defaultBranch,
                visibility = visibility,
                pageStatusCode = response?.statusCode,
                diagnosis = diagnosis,
                sessions = sessions
            ),
            notices = buildNotices(diagnosis = diagnosis, response = response, sessions = sessions),
            actions = listOf(
                "Agents session 列表使用 GitHub HTML 页面实验性解析，解析失败时会回退到诊断摘要。",
                "若解析结果为空，请到 GitHub 网页端确认 Copilot、组织策略、仓库设置和账号权限。"
            ),
            sourceUrl = agentsUrl
        )
        return GitHubHtmlParseResult.Success(
            RepositoryAgentsPageSummary(
                summary = summary,
                sessions = sessions,
                isExperimentalHtmlParse = true
            )
        )
    }

    private fun request(url: String, accept: String): AgentsNetworkResponse {
        return httpClient.execute(
            GitHubHttpRequest(
                pathOrUrl = url,
                accept = accept,
                apiVersion = GitHubApiHeaders.LegacyApiVersion,
                followRedirects = true
            )
        ).let { response ->
            AgentsNetworkResponse(statusCode = response.statusCode, body = response.body)
        }
    }

    private fun buildMetrics(
        defaultBranch: String,
        visibility: String,
        pageStatusCode: Int?,
        diagnosis: AgentsDiagnosis,
        sessions: List<RepositoryAgentSession>
    ): List<RepositoryHtmlMetric> {
        return listOf(
            RepositoryHtmlMetric("数据来源", "REST + GitHub Agents 页面解析"),
            RepositoryHtmlMetric("诊断状态", diagnosis.state),
            RepositoryHtmlMetric("页面状态码", pageStatusCode?.toString() ?: "未返回"),
            RepositoryHtmlMetric("页面信号数", diagnosis.signals.size.toString()),
            RepositoryHtmlMetric("Sessions", sessions.size.toString()),
            RepositoryHtmlMetric("Active", sessions.count { !it.status.isCompleted }.toString()),
            RepositoryHtmlMetric("Completed", sessions.count { it.status.isCompleted }.toString()),
            RepositoryHtmlMetric("默认分支", defaultBranch),
            RepositoryHtmlMetric("可见性", visibility)
        )
    }

    private fun buildNotices(
        diagnosis: AgentsDiagnosis,
        response: AgentsNetworkResponse?,
        sessions: List<RepositoryAgentSession>
    ): List<String> {
        return buildList {
            add(diagnosis.notice)
            if (sessions.isNotEmpty()) {
                add("实验性解析：已识别 ${sessions.size} 个 Agents session，若 GitHub 页面结构变化，结果可能为空或不完整。")
            }
            diagnosis.signals.take(SignalNoticeLimit).forEachIndexed { index, signal -> add("页面信号 ${index + 1}：$signal") }
            response?.statusCode?.takeIf { diagnosis.status != RepositoryHtmlSectionStatus.Available }?.let { statusCode ->
                add("页面诊断：GitHub 返回 HTTP $statusCode，原始响应已保留在调试输出链路中。")
            }
        }.distinct().take(MaxNotices)
    }

    private data class AgentsDiagnosis(
        val status: RepositoryHtmlSectionStatus,
        val state: String,
        val notice: String,
        val signals: List<String> = emptyList()
    )

    private data class AgentsNetworkResponse(
        val statusCode: Int,
        val body: String
    ) {
        val preview: String get() = body.take(240).replace(Regex("\\s+"), " ").decodeHtmlEntities().trim()
    }

    private companion object {
        private const val SectionKey = "agents"
        private const val TimeoutMillis = 15_000
        private const val NetworkErrorStatusCode = 0
        private const val SignalNoticeLimit = 4
        private const val MaxNotices = 6
    }
}

private fun String.isSignInPage(): Boolean {
    return contains("Sign in to GitHub", ignoreCase = true) ||
        contains("/login", ignoreCase = true) && contains("password", ignoreCase = true)
}

private fun String.isCopilotFeatureGate(): Boolean {
    return contains("Copilot is not available", ignoreCase = true) ||
        contains("This feature is not available", ignoreCase = true) ||
        contains("not enabled for this repository", ignoreCase = true) ||
        contains("not available for this repository", ignoreCase = true) ||
        contains("You do not have access", ignoreCase = true)
}

private fun String.isCopilotMarketingPage(): Boolean {
    return contains("GitHub Copilot", ignoreCase = true) &&
        (contains("AI pair programmer", ignoreCase = true) ||
            contains("Get started with Copilot", ignoreCase = true) ||
            contains("Copilot plans", ignoreCase = true))
}

private fun String.extractAgentSignals(): List<String> {
    val plain = replace(Regex("<[^>]+>"), " ").decodeHtmlEntities().normalizeWhitespace()
    return AgentSignalKeywords.mapNotNull { keyword ->
        if (plain.contains(keyword, ignoreCase = true)) keyword else null
    }
}

private fun String.extractAgentSessions(owner: String, repo: String, sourceUrl: String): List<RepositoryAgentSession> {
    val anchorSessions = AgentAnchorRegex.findAll(this).mapNotNull { match ->
        val href = match.groupValues.getOrNull(1).orEmpty().decodeHtmlEntities().trim()
        val text = match.groupValues.getOrNull(2).orEmpty().toPlainAgentsText().toAgentSessionTitleOrNull() ?: return@mapNotNull null
        text.toAgentSessionCandidate(href.toAbsoluteGitHubUrl(sourceUrl))
    }
    val jsonSessions = AgentJsonObjectRegex.findAll(this).mapNotNull { match ->
        val block = match.value.decodeJsonEscapes().decodeHtmlEntities()
        val title = block.extractJsonField("title", "displayTitle", "name", "subject", "task")
            ?.toAgentSessionTitleOrNull()
            ?: return@mapNotNull null
        val href = block.extractJsonField("url", "htmlUrl", "href", "permalink")?.toAbsoluteGitHubUrl(sourceUrl)
        val author = block.extractJsonField("author", "actor", "userLogin", "login", "creator", "createdBy")
        val branch = block.extractJsonField("branch", "headBranch", "ref", "refName", "sourceBranch", "baseRefName", "headRefName")
        val updatedAt = block.extractJsonField("updatedAt", "updated_at", "lastUpdatedAt", "completedAt", "createdAt", "startedAt")
        val target = block.extractJsonField("target", "targetTitle", "pullRequestTitle", "issueTitle", "prTitle", "number")
        val targetUrl = block.extractJsonField(
            "targetUrl",
            "target_url",
            "targetHref",
            "pullRequestUrl",
            "issueUrl",
            "pullRequestHref",
            "issueHref"
        )?.toAbsoluteGitHubUrl(sourceUrl)
        val context = listOf(
            title,
            block.extractJsonField("status", "state", "conclusion").orEmpty(),
            block.extractJsonField("type", "source", "client").orEmpty(),
            block.extractJsonField("agent", "agentName", "appName").orEmpty(),
            author.orEmpty(),
            branch.orEmpty(),
            updatedAt.orEmpty(),
            target.orEmpty()
        ).joinToString(" ")
        RepositoryAgentSession(
            id = block.extractJsonField("id", "databaseId", "nodeId") ?: title.stableAgentSessionId(),
            title = title,
            status = context.inferAgentSessionStatus(),
            type = context.inferAgentSessionType(),
            agent = context.inferAgentSessionAgent(),
            summary = listOfNotNull(
                context.toAgentSessionSummary(),
                author?.let { "Author $it" },
                branch?.let { "Branch $it" },
                updatedAt,
                target?.let { "Target $it" }
            ).distinct().joinToString(" · "),
            htmlUrl = href,
            author = author,
            branch = branch,
            updatedAt = updatedAt,
            target = target,
            targetUrl = targetUrl
        )
    }
    return (anchorSessions + jsonSessions)
        .filter { session -> session.title.isReliableAgentSessionTitle(owner, repo) }
        .distinctBy { session -> listOf(session.title.lowercase(Locale.US), session.status.name, session.htmlUrl.orEmpty()).joinToString("|") }
        .take(MaxParsedSessions)
        .toList()
}

private fun String.toAgentSessionCandidate(htmlUrl: String?): RepositoryAgentSession {
    return RepositoryAgentSession(
        id = stableAgentSessionId(),
        title = this,
        status = inferAgentSessionStatus(),
        type = inferAgentSessionType(),
        agent = inferAgentSessionAgent(),
        summary = toAgentSessionSummary(),
        htmlUrl = htmlUrl
    )
}

private fun String.toPlainAgentsText(): String {
    return replace(Regex("(?is)<script[^>]*>.*?</script>"), " ")
        .replace(Regex("(?is)<style[^>]*>.*?</style>"), " ")
        .replace(Regex("<[^>]+>"), " ")
        .decodeHtmlEntities()
        .decodeJsonEscapes()
        .normalizeWhitespace()
}

private fun String.toAgentSessionTitleOrNull(): String? {
    val normalized = normalizeWhitespace()
        .removePrefix("Session: ")
        .removePrefix("Agent session: ")
        .trim()
    if (normalized.length !in 4..140) return null
    if (AgentBoilerplateTitles.any { value -> normalized.equals(value, ignoreCase = true) }) return null
    if (!normalized.containsAnyAgentSessionSignal()) return null
    return normalized
}

private fun String.isReliableAgentSessionTitle(owner: String, repo: String): Boolean {
    if (equals(owner, ignoreCase = true) || equals(repo, ignoreCase = true)) return false
    if (startsWith("GitHub Copilot", ignoreCase = true) && length < 28) return false
    return true
}

private fun String.containsAnyAgentSessionSignal(): Boolean {
    return AgentSessionSignalKeywords.any { keyword -> contains(keyword, ignoreCase = true) } ||
        AgentStatusKeywords.any { (_, keywords) -> keywords.any { keyword -> contains(keyword, ignoreCase = true) } }
}

private fun String.inferAgentSessionStatus(): RepositoryAgentSessionStatus {
    return AgentStatusKeywords.firstOrNull { (_, keywords) ->
        keywords.any { keyword -> contains(keyword, ignoreCase = true) }
    }?.first ?: RepositoryAgentSessionStatus.Unknown
}

private fun String.inferAgentSessionType(): RepositoryAgentSessionType? {
    return when {
        contains("cloud agent", ignoreCase = true) || contains("cloud_agents", ignoreCase = true) -> RepositoryAgentSessionType.CloudAgents
        contains("cli", ignoreCase = true) -> RepositoryAgentSessionType.Cli
        contains("vs code", ignoreCase = true) || contains("vscode", ignoreCase = true) -> RepositoryAgentSessionType.VsCode
        else -> null
    }
}

private fun String.inferAgentSessionAgent(): RepositoryAgentSessionAgent? {
    return when {
        contains("cloud agent", ignoreCase = true) || contains("cloud_agents", ignoreCase = true) -> RepositoryAgentSessionAgent.CloudAgents
        contains("copilot cli", ignoreCase = true) || contains(" cli", ignoreCase = true) -> RepositoryAgentSessionAgent.CopilotCli
        contains("jetbrains", ignoreCase = true) -> RepositoryAgentSessionAgent.JetBrains
        contains("vs code", ignoreCase = true) || contains("vscode", ignoreCase = true) -> RepositoryAgentSessionAgent.VsCode
        else -> null
    }
}

private fun String.toAgentSessionSummary(): String {
    val parts = buildList {
        val status = inferAgentSessionStatus().takeIf { it != RepositoryAgentSessionStatus.Unknown }?.displayLabel
        val type = inferAgentSessionType()?.displayLabel
        val agent = inferAgentSessionAgent()?.displayLabel
        status?.let { add(it) }
        type?.let { add(it) }
        agent?.let { add(it) }
    }
    return parts.joinToString(separator = " · ").ifBlank { "Parsed from GitHub Agents page" }
}

private fun String.extractJsonField(vararg names: String): String? {
    for (name in names) {
        val escapedName = Regex.escape(name)
        val stringRegex = Regex(""""$escapedName"\s*:\s*"((?:\\.|[^"\\])*)"""")
        val stringValue = stringRegex.find(this)?.groupValues?.getOrNull(1)
            ?.decodeJsonEscapes()
            ?.decodeHtmlEntities()
            ?.normalizeWhitespace()
        if (!stringValue.isNullOrBlank()) return stringValue

        val objectRegex = Regex(
            """"$escapedName"\s*:\s*\{[^{}]{0,800}"(?:login|name|title)"\s*:\s*"((?:\\.|[^"\\])*)""",
            RegexOption.IGNORE_CASE
        )
        val objectValue = objectRegex.find(this)?.groupValues?.getOrNull(1)
            ?.decodeJsonEscapes()
            ?.decodeHtmlEntities()
            ?.normalizeWhitespace()
        if (!objectValue.isNullOrBlank()) return objectValue

        val numericRegex = Regex(""""$escapedName"\s*:\s*([0-9]+)""")
        val numericValue = numericRegex.find(this)?.groupValues?.getOrNull(1)?.normalizeWhitespace()
        if (!numericValue.isNullOrBlank()) return numericValue
    }
    return null
}

private fun String.decodeJsonEscapes(): String {
    return replace("\\/", "/")
        .replace("\\n", " ")
        .replace("\\t", " ")
        .replace("\\r", " ")
        .replace("\\\"", "\"")
}

private fun String.toAbsoluteGitHubUrl(sourceUrl: String): String? {
    val normalized = trim().takeIf { it.isNotBlank() } ?: return null
    if (normalized.startsWith("http://", ignoreCase = true) || normalized.startsWith("https://", ignoreCase = true)) return normalized
    if (normalized.startsWith("/")) return "https://github.com$normalized"
    return sourceUrl.substringBeforeLast('/') + "/" + normalized
}

private fun String.stableAgentSessionId(): String = lowercase(Locale.US).filter { it.isLetterOrDigit() }.take(48).ifBlank { hashCode().toString() }

private fun String.toAgentsPathSegment(): String {
    return java.net.URLEncoder.encode(this, Charsets.UTF_8.name()).replace("+", "%20")
}

private fun JSONObject.optionalAgentsString(name: String): String? {
    if (isNull(name)) return null
    return optString(name).takeIf { it.isNotBlank() && it != "null" }
}

private val AgentAnchorRegex = Regex("""(?is)<a\s+[^>]*href=["']([^"']+)["'][^>]*>(.*?)</a>""")
private val AgentJsonObjectRegex = Regex(
    """\{[^{}]{0,2000}(?:copilot|agent|session|queued|in_progress|completed|failed|timed_out)[^{}]{0,2000}\}""",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
)
private const val MaxParsedSessions = 30

private val AgentSessionSignalKeywords = listOf(
    "Copilot",
    "agent",
    "session",
    "Cloud agents",
    "Copilot CLI",
    "JetBrains",
    "VS Code",
    "vscode"
)

private val AgentSignalKeywords = listOf(
    "Assign to Copilot",
    "Copilot coding agent",
    "Coding agent",
    "Agent mode",
    "Pull request",
    "agent session",
    "AI-powered"
)

private val AgentBoilerplateTitles = setOf(
    "Copilot",
    "GitHub Copilot",
    "Copilot uses AI. Check for mistakes.",
    "Agents",
    "Sessions",
    "Status",
    "Type",
    "Agent",
    "More"
)

private val AgentStatusKeywords = listOf(
    RepositoryAgentSessionStatus.InProgress to listOf("In progress", "in_progress", "in-progress", "running"),
    RepositoryAgentSessionStatus.Queued to listOf("Queued", "queued", "waiting"),
    RepositoryAgentSessionStatus.Idle to listOf("Idle", "idle"),
    RepositoryAgentSessionStatus.NeedsAttention to listOf("Needs attention", "needs_attention", "needs-attention", "attention"),
    RepositoryAgentSessionStatus.Failed to listOf("Failed", "failure", "failed"),
    RepositoryAgentSessionStatus.Completed to listOf("Completed", "complete", "success", "succeeded"),
    RepositoryAgentSessionStatus.Cancelled to listOf("Cancelled", "canceled", "cancelled"),
    RepositoryAgentSessionStatus.TimedOut to listOf("Timed out", "timed_out", "timed-out", "timeout")
)
