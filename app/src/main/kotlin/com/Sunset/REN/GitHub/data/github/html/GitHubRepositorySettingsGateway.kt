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

class GitHubRepositorySettingsGateway(
    accessToken: String
) {
    private val httpClient = GitHubHttpClient(accessToken, timeoutMillis = TimeoutMillis)

    fun loadSettingsSummary(owner: String, repo: String): GitHubHtmlParseResult<RepositoryHtmlSectionSummary> {
        return when (val result = loadSettingsSnapshot(owner, repo)) {
            is GitHubHtmlParseResult.Success -> GitHubHtmlParseResult.Success(result.value.toLegacySummary())
            is GitHubHtmlParseResult.AccessDenied -> result
            is GitHubHtmlParseResult.NotFound -> result
            is GitHubHtmlParseResult.ParseError -> result
        }
    }

    fun loadSettingsSnapshot(owner: String, repo: String): GitHubHtmlParseResult<RepositorySettingsSnapshot> {
        val webSettingsUrl = "https://github.com/${owner.toGitHubPathSegment()}/${repo.toGitHubPathSegment()}/settings"
        val apiUrl = "https://api.github.com/repos/${owner.toGitHubPathSegment()}/${repo.toGitHubPathSegment()}"
        val response = try {
            request("GET", apiUrl)
        } catch (error: SocketTimeoutException) {
            return GitHubHtmlParseResult.ParseError(
                message = "GitHub REST 仓库设置请求超时。",
                sourceUrl = webSettingsUrl,
                statusCode = NetworkErrorStatusCode
            )
        } catch (error: IOException) {
            return GitHubHtmlParseResult.ParseError(
                message = error.message?.takeIf { it.isNotBlank() } ?: "GitHub REST 仓库设置请求失败。",
                sourceUrl = webSettingsUrl,
                statusCode = NetworkErrorStatusCode
            )
        }

        val preview = response.body.take(240).replace(Regex("\\s+"), " ").trim().takeIf { it.isNotBlank() }
        if (response.statusCode == HttpURLConnection.HTTP_FORBIDDEN) {
            return GitHubHtmlParseResult.AccessDenied(
                message = "当前令牌无法读取仓库设置摘要。",
                sourceUrl = webSettingsUrl,
                statusCode = response.statusCode,
                htmlPreview = preview
            )
        }
        if (response.statusCode == HttpURLConnection.HTTP_NOT_FOUND) {
            return GitHubHtmlParseResult.NotFound(
                message = "仓库不存在，或当前令牌无法访问该仓库。",
                sourceUrl = webSettingsUrl,
                statusCode = response.statusCode,
                htmlPreview = preview
            )
        }
        if (response.statusCode !in 200..299) {
            return GitHubHtmlParseResult.ParseError(
                message = "GitHub REST 仓库设置返回 HTTP ${response.statusCode}。",
                sourceUrl = webSettingsUrl,
                statusCode = response.statusCode,
                htmlPreview = preview
            )
        }

        val json = runCatching { JSONObject(response.body) }.getOrElse {
            return GitHubHtmlParseResult.ParseError(
                message = "GitHub REST 仓库设置返回内容不是有效 JSON。",
                sourceUrl = webSettingsUrl,
                statusCode = response.statusCode,
                htmlPreview = preview
            )
        }
        return GitHubHtmlParseResult.Success(json.toSettingsSnapshot(owner, repo, webSettingsUrl))
    }

    fun updateSettings(owner: String, repo: String, request: RepositorySettingsUpdateRequest): RepositorySettingsSnapshot {
        val apiUrl = "https://api.github.com/repos/${owner.toGitHubPathSegment()}/${repo.toGitHubPathSegment()}"
        val response = try {
            request("PATCH", apiUrl, request.toJson().toString())
        } catch (error: SocketTimeoutException) {
            throw IllegalStateException("GitHub REST 更新仓库设置请求超时。")
        } catch (error: IOException) {
            throw IllegalStateException(error.message?.takeIf { it.isNotBlank() } ?: "GitHub REST 更新仓库设置请求失败。")
        }
        if (response.statusCode !in 200..299) {
            throw IllegalStateException(response.toUpdateErrorMessage())
        }
        val json = runCatching { JSONObject(response.body) }.getOrElse {
            throw IllegalStateException("GitHub REST 更新仓库设置返回内容不是有效 JSON。")
        }
        val webSettingsUrl = "https://github.com/${owner.toGitHubPathSegment()}/${repo.toGitHubPathSegment()}/settings"
        return json.toSettingsSnapshot(owner, repo, webSettingsUrl)
    }

    fun transferRepository(owner: String, repo: String, newOwner: String) {
        val apiUrl = "https://api.github.com/repos/${owner.toGitHubPathSegment()}/${repo.toGitHubPathSegment()}/transfer"
        val body = JSONObject().put("new_owner", newOwner).toString()
        val response = try {
            request("POST", apiUrl, body)
        } catch (error: SocketTimeoutException) {
            throw IllegalStateException("GitHub REST 转移仓库请求超时。")
        } catch (error: IOException) {
            throw IllegalStateException(error.message?.takeIf { it.isNotBlank() } ?: "GitHub REST 转移仓库请求失败。")
        }
        if (response.statusCode !in 200..299) {
            throw IllegalStateException(response.toTransferErrorMessage())
        }
    }

    fun deleteRepository(owner: String, repo: String) {
        val apiUrl = "https://api.github.com/repos/${owner.toGitHubPathSegment()}/${repo.toGitHubPathSegment()}"
        val response = try {
            request("DELETE", apiUrl)
        } catch (error: SocketTimeoutException) {
            throw IllegalStateException("GitHub REST 删除仓库请求超时。")
        } catch (error: IOException) {
            throw IllegalStateException(error.message?.takeIf { it.isNotBlank() } ?: "GitHub REST 删除仓库请求失败。")
        }
        if (response.statusCode !in 200..299) {
            throw IllegalStateException(response.toDeleteErrorMessage())
        }
    }

    private fun request(method: String, url: String, body: String? = null): SettingsNetworkResponse {
        return httpClient.execute(
            GitHubHttpRequest(
                pathOrUrl = url,
                method = method.toGitHubHttpMethod(),
                body = body,
                apiVersion = GitHubApiHeaders.LegacyApiVersion
            )
        ).toSettingsNetworkResponse()
    }

    private fun JSONObject.toSettingsSnapshot(owner: String, repo: String, webSettingsUrl: String): RepositorySettingsSnapshot {
        val permissions = optJSONObject("permissions")
        val canAdmin = permissions?.optBoolean("admin", false) == true
        val canPush = permissions?.optBoolean("push", false) == true
        val apiName = optionalString("name") ?: repo
        val apiFullName = optionalString("full_name") ?: "$owner/$apiName"
        val actualOwner = apiFullName.substringBefore('/', owner).ifBlank { owner }
        val actualRepo = apiFullName.substringAfter('/', apiName).ifBlank { apiName }
        val actualSettingsUrl = "https://github.com/${actualOwner.toGitHubPathSegment()}/${actualRepo.toGitHubPathSegment()}/settings"
        return RepositorySettingsSnapshot(
            owner = actualOwner,
            repo = actualRepo,
            name = apiName,
            fullName = apiFullName,
            description = optionalString("description").orEmpty(),
            homepage = optionalString("homepage").orEmpty(),
            defaultBranch = optionalString("default_branch") ?: "main",
            visibilityLabel = optionalString("visibility") ?: if (optBoolean("private", false)) "private" else "public",
            permissionLabel = permissionText(canAdmin, canPush),
            licenseLabel = optJSONObject("license")?.optionalString("name") ?: "未设置",
            languageLabel = optionalString("language") ?: "未设置",
            createdAt = optionalString("created_at") ?: "未知",
            updatedAt = optionalString("updated_at") ?: "未知",
            pushedAt = optionalString("pushed_at") ?: "未知",
            stargazersCount = optInt("stargazers_count", 0),
            forksCount = optInt("forks_count", 0),
            openIssuesCount = optInt("open_issues_count", 0),
            hasIssues = optBoolean("has_issues", false),
            hasProjects = optBoolean("has_projects", false),
            hasWiki = optBoolean("has_wiki", false),
            hasDiscussions = optBoolean("has_discussions", false),
            allowForking = optBoolean("allow_forking", false),
            archived = optBoolean("archived", false),
            allowSquashMerge = optBoolean("allow_squash_merge", false),
            allowMergeCommit = optBoolean("allow_merge_commit", false),
            allowRebaseMerge = optBoolean("allow_rebase_merge", false),
            deleteBranchOnMerge = optBoolean("delete_branch_on_merge", false),
            allowAutoMerge = optBoolean("allow_auto_merge", false),
            canAdmin = canAdmin,
            canPush = canPush,
            sourceUrl = actualSettingsUrl
        )
    }

    private fun RepositorySettingsSnapshot.toLegacySummary(): RepositoryHtmlSectionSummary {
        val metrics = listOf(
            RepositoryHtmlMetric("数据来源", "GitHub REST API"),
            RepositoryHtmlMetric("默认分支", defaultBranch),
            RepositoryHtmlMetric("可见性", visibilityLabel),
            RepositoryHtmlMetric("Issues", enabledText(hasIssues)),
            RepositoryHtmlMetric("Projects", enabledText(hasProjects)),
            RepositoryHtmlMetric("Wiki", enabledText(hasWiki)),
            RepositoryHtmlMetric("Discussions", enabledText(hasDiscussions)),
            RepositoryHtmlMetric("Fork", enabledText(allowForking)),
            RepositoryHtmlMetric("归档", enabledText(archived)),
            RepositoryHtmlMetric("Squash merge", enabledText(allowSquashMerge)),
            RepositoryHtmlMetric("Merge commit", enabledText(allowMergeCommit)),
            RepositoryHtmlMetric("Rebase merge", enabledText(allowRebaseMerge)),
            RepositoryHtmlMetric("合并后删除分支", enabledText(deleteBranchOnMerge))
        )
        return RepositoryHtmlSectionSummary(
            owner = owner,
            repo = repo,
            sectionKey = "settings",
            title = "Settings",
            status = RepositoryHtmlSectionStatus.Available,
            description = "当前显示的是可通过 GitHub REST API 稳定读取的仓库设置。",
            metrics = metrics,
            notices = listOfNotNull(
                description.takeIf { it.isNotBlank() }?.let { "仓库简介：$it" },
                homepage.takeIf { it.isNotBlank() }?.let { "主页：$it" },
                "权限：$permissionLabel",
                licenseLabel.takeIf { it.isNotBlank() && it != "未设置" }
            ),
            actions = listOf(
                "已接入可编辑的仓库基础设置与合并策略主干。",
                "危险区、协作者、分支规则等高级设置仍需后续接入对应 API。"
            ),
            sourceUrl = sourceUrl
        )
    }

    private fun GitHubHttpResponse.toSettingsNetworkResponse(): SettingsNetworkResponse {
        return SettingsNetworkResponse(statusCode = statusCode, body = body)
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

    private data class SettingsNetworkResponse(
        val statusCode: Int,
        val body: String
    ) {
        fun toUpdateErrorMessage(): String {
            val json = runCatching { JSONObject(body) }.getOrNull()
            val message = json?.optionalString("message")
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
            return if (detail.isNotBlank()) "更新仓库设置失败：$detail" else "更新仓库设置失败：HTTP $statusCode"
        }
        fun toTransferErrorMessage(): String = toActionErrorMessage("转移仓库失败")
        fun toDeleteErrorMessage(): String = toActionErrorMessage("删除仓库失败")
        private fun toActionErrorMessage(prefix: String): String {
            val json = runCatching { JSONObject(body) }.getOrNull()
            val message = json?.optionalString("message")
            val errors = json?.optJSONArray("errors")
            val detail = buildList {
                if (!message.isNullOrBlank()) add(message)
                if (errors != null) for (index in 0 until errors.length()) errors.opt(index)?.toString()?.takeIf { it.isNotBlank() }?.let { add(it) }
            }.joinToString("；")
            return if (detail.isNotBlank()) "$prefix：$detail" else "$prefix：HTTP $statusCode"
        }
    }

    private companion object {
        private const val TimeoutMillis = 15_000
        private const val NetworkErrorStatusCode = 0
    }
}

private fun JSONObject.optionalString(name: String): String? {
    if (isNull(name)) return null
    return optString(name).takeIf { it.isNotBlank() && it != "null" }
}

private fun enabledText(enabled: Boolean): String = if (enabled) "开启" else "关闭"

private fun permissionText(canAdmin: Boolean, canPush: Boolean): String {
    return when {
        canAdmin -> "管理员，可修改仓库设置"
        canPush -> "可写入，但不一定能修改仓库设置"
        else -> "只读或无写入权限"
    }
}

private fun String.toGitHubPathSegment(): String {
    return java.net.URLEncoder.encode(this, Charsets.UTF_8.name()).replace("+", "%20")
}
