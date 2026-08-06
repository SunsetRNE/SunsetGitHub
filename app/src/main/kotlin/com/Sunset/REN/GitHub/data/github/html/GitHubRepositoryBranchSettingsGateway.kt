package com.Sunset.REN.GitHub.data.github.html

import com.Sunset.REN.GitHub.data.github.network.GitHubApiHeaders
import com.Sunset.REN.GitHub.data.github.network.GitHubHttpClient
import com.Sunset.REN.GitHub.data.github.network.GitHubHttpMethod
import com.Sunset.REN.GitHub.data.github.network.GitHubHttpRequest
import com.Sunset.REN.GitHub.data.github.network.GitHubHttpResponse
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException

/**
 * GitHub 仓库「分支与保护规则」能力网关。
 *
 * 先把 Settings > Branches 的核心能力落到数据层：读取分支列表、读取保护规则、
 * 以及提供开启/更新/删除保护规则的安全 API 封装。UI 可以后续基于这些模型迭代。
 */
class GitHubRepositoryBranchSettingsGateway(
    accessToken: String
) {
    private val httpClient = GitHubHttpClient(accessToken, timeoutMillis = TimeoutMillis)

    fun loadBranchSettings(owner: String, repo: String): GitHubHtmlParseResult<RepositoryBranchSettingsSnapshot> {
        val branchesUrl = buildWebBranchesUrl(owner, repo)
        val repositoryResponse = try {
            request("GET", buildRepoApiUrl(owner, repo))
        } catch (error: SocketTimeoutException) {
            return GitHubHtmlParseResult.ParseError(
                message = "GitHub REST 分支设置请求超时。",
                sourceUrl = branchesUrl,
                statusCode = NetworkErrorStatusCode
            )
        } catch (error: IOException) {
            return GitHubHtmlParseResult.ParseError(
                message = error.message?.takeIf { it.isNotBlank() } ?: "GitHub REST 分支设置请求失败。",
                sourceUrl = branchesUrl,
                statusCode = NetworkErrorStatusCode
            )
        }
        repositoryResponse.toParseFailureOrNull(branchesUrl, "仓库分支设置")?.let { return it }

        val repository = runCatching { JSONObject(repositoryResponse.body) }.getOrElse {
            return GitHubHtmlParseResult.ParseError(
                message = "GitHub REST 分支设置返回内容不是有效 JSON。",
                sourceUrl = branchesUrl,
                statusCode = repositoryResponse.statusCode,
                htmlPreview = repositoryResponse.preview
            )
        }
        val defaultBranch = repository.optionalBranchString("default_branch") ?: "main"
        val permissions = repository.optJSONObject("permissions")
        val canAdmin = permissions?.optBoolean("admin", false) == true
        val canPush = permissions?.optBoolean("push", false) == true

        val branchResponses = try {
            requestPaged("GET", "${buildRepoApiUrl(owner, repo)}/branches?per_page=$PageSize")
        } catch (error: SocketTimeoutException) {
            return GitHubHtmlParseResult.ParseError(
                message = "GitHub REST 分支列表请求超时。",
                sourceUrl = branchesUrl,
                statusCode = NetworkErrorStatusCode
            )
        } catch (error: IOException) {
            return GitHubHtmlParseResult.ParseError(
                message = error.message?.takeIf { it.isNotBlank() } ?: "GitHub REST 分支列表请求失败。",
                sourceUrl = branchesUrl,
                statusCode = NetworkErrorStatusCode
            )
        }
        val firstBranchResponse = branchResponses.firstOrNull()
            ?: return GitHubHtmlParseResult.ParseError(
                message = "GitHub REST 分支列表没有返回响应。",
                sourceUrl = branchesUrl,
                statusCode = NetworkErrorStatusCode
            )
        firstBranchResponse.toParseFailureOrNull(branchesUrl, "仓库分支列表")?.let { return it }

        val branchItems = branchResponses.flatMap { response ->
            val array = runCatching { JSONArray(response.body) }.getOrElse {
                return GitHubHtmlParseResult.ParseError(
                    message = "GitHub REST 分支列表返回内容不是有效 JSON。",
                    sourceUrl = branchesUrl,
                    statusCode = response.statusCode,
                    htmlPreview = response.preview
                )
            }
            (0 until array.length()).mapNotNull { index -> array.optJSONObject(index)?.toBranchItem(defaultBranch) }
        }

        val branches = branchItems.mapIndexed { index, branch ->
            if (!branch.protected || index >= ProtectionDetailsLimit) {
                branch
            } else {
                branch.copy(protection = loadBranchProtectionOrNull(owner, repo, branch.name))
            }
        }

        return GitHubHtmlParseResult.Success(
            RepositoryBranchSettingsSnapshot(
                owner = owner,
                repo = repo,
                defaultBranch = defaultBranch,
                canAdmin = canAdmin,
                canPush = canPush,
                branches = branches,
                sourceUrl = branchesUrl,
                protectionDetailsLimit = ProtectionDetailsLimit
            )
        )
    }

    fun loadBranchProtection(owner: String, repo: String, branch: String): GitHubHtmlParseResult<RepositoryBranchProtectionSnapshot> {
        val sourceUrl = "${buildWebBranchesUrl(owner, repo)}/${branch.toBranchPathSegment()}"
        val response = try {
            request("GET", buildBranchProtectionApiUrl(owner, repo, branch))
        } catch (error: SocketTimeoutException) {
            return GitHubHtmlParseResult.ParseError(
                message = "GitHub REST 分支保护规则请求超时。",
                sourceUrl = sourceUrl,
                statusCode = NetworkErrorStatusCode
            )
        } catch (error: IOException) {
            return GitHubHtmlParseResult.ParseError(
                message = error.message?.takeIf { it.isNotBlank() } ?: "GitHub REST 分支保护规则请求失败。",
                sourceUrl = sourceUrl,
                statusCode = NetworkErrorStatusCode
            )
        }
        if (response.statusCode == HttpURLConnection.HTTP_NOT_FOUND) {
            return GitHubHtmlParseResult.NotFound(
                message = "该分支未启用保护规则，或当前令牌无法访问该规则。",
                sourceUrl = sourceUrl,
                statusCode = response.statusCode,
                htmlPreview = response.preview
            )
        }
        response.toParseFailureOrNull(sourceUrl, "分支保护规则")?.let { return it }
        val json = runCatching { JSONObject(response.body) }.getOrElse {
            return GitHubHtmlParseResult.ParseError(
                message = "GitHub REST 分支保护规则返回内容不是有效 JSON。",
                sourceUrl = sourceUrl,
                statusCode = response.statusCode,
                htmlPreview = response.preview
            )
        }
        return GitHubHtmlParseResult.Success(json.toProtectionSnapshot(branch))
    }

    fun updateBranchProtection(
        owner: String,
        repo: String,
        branch: String,
        request: RepositoryBranchProtectionUpdateRequest
    ): GitHubHtmlParseResult<RepositoryBranchProtectionSnapshot> {
        val sourceUrl = "${buildWebBranchesUrl(owner, repo)}/${branch.toBranchPathSegment()}"
        val response = try {
            request("PUT", buildBranchProtectionApiUrl(owner, repo, branch), request.toJson().toString())
        } catch (error: SocketTimeoutException) {
            return GitHubHtmlParseResult.ParseError(
                message = "GitHub REST 更新分支保护规则请求超时。",
                sourceUrl = sourceUrl,
                statusCode = NetworkErrorStatusCode
            )
        } catch (error: IOException) {
            return GitHubHtmlParseResult.ParseError(
                message = error.message?.takeIf { it.isNotBlank() } ?: "GitHub REST 更新分支保护规则请求失败。",
                sourceUrl = sourceUrl,
                statusCode = NetworkErrorStatusCode
            )
        }
        if (response.statusCode !in 200..299) {
            return GitHubHtmlParseResult.ParseError(
                message = response.toUpdateErrorMessage("更新分支保护规则失败"),
                sourceUrl = sourceUrl,
                statusCode = response.statusCode,
                htmlPreview = response.preview
            )
        }
        request.requiredSignatures?.let { enabled ->
            when (val signatureResult = updateRequiredSignatures(owner, repo, branch, enabled)) {
                is GitHubHtmlParseResult.Success -> Unit
                is GitHubHtmlParseResult.AccessDenied -> return signatureResult
                is GitHubHtmlParseResult.NotFound -> return signatureResult
                is GitHubHtmlParseResult.ParseError -> return signatureResult
            }
        }
        val json = runCatching { JSONObject(response.body) }.getOrElse {
            return GitHubHtmlParseResult.ParseError(
                message = "GitHub REST 更新分支保护规则返回内容不是有效 JSON。",
                sourceUrl = sourceUrl,
                statusCode = response.statusCode,
                htmlPreview = response.preview
            )
        }
        val protection = json.toProtectionSnapshot(branch)
        return GitHubHtmlParseResult.Success(protection.copy(requiredSignatures = request.requiredSignatures ?: protection.requiredSignatures))
    }

    fun deleteBranchProtection(owner: String, repo: String, branch: String): GitHubHtmlParseResult<Unit> {
        val sourceUrl = "${buildWebBranchesUrl(owner, repo)}/${branch.toBranchPathSegment()}"
        val response = try {
            request("DELETE", buildBranchProtectionApiUrl(owner, repo, branch))
        } catch (error: SocketTimeoutException) {
            return GitHubHtmlParseResult.ParseError(
                message = "GitHub REST 删除分支保护规则请求超时。",
                sourceUrl = sourceUrl,
                statusCode = NetworkErrorStatusCode
            )
        } catch (error: IOException) {
            return GitHubHtmlParseResult.ParseError(
                message = error.message?.takeIf { it.isNotBlank() } ?: "GitHub REST 删除分支保护规则请求失败。",
                sourceUrl = sourceUrl,
                statusCode = NetworkErrorStatusCode
            )
        }
        if (response.statusCode !in 200..299) {
            return GitHubHtmlParseResult.ParseError(
                message = response.toUpdateErrorMessage("删除分支保护规则失败"),
                sourceUrl = sourceUrl,
                statusCode = response.statusCode,
                htmlPreview = response.preview
            )
        }
        return GitHubHtmlParseResult.Success(Unit)
    }

    private fun loadBranchProtectionOrNull(owner: String, repo: String, branch: String): RepositoryBranchProtectionSnapshot? {
        return when (val result = loadBranchProtection(owner, repo, branch)) {
            is GitHubHtmlParseResult.Success -> result.value
            else -> null
        }
    }

    private fun updateRequiredSignatures(owner: String, repo: String, branch: String, enabled: Boolean): GitHubHtmlParseResult<Unit> {
        val sourceUrl = "${buildWebBranchesUrl(owner, repo)}/${branch.toBranchPathSegment()}"
        val url = "${buildBranchProtectionApiUrl(owner, repo, branch)}/required_signatures"
        val response = try {
            request(if (enabled) "POST" else "DELETE", url)
        } catch (error: SocketTimeoutException) {
            return GitHubHtmlParseResult.ParseError("GitHub REST 更新签名提交保护请求超时。", sourceUrl, NetworkErrorStatusCode)
        } catch (error: IOException) {
            return GitHubHtmlParseResult.ParseError(error.message?.takeIf { it.isNotBlank() } ?: "GitHub REST 更新签名提交保护请求失败。", sourceUrl, NetworkErrorStatusCode)
        }
        if (response.statusCode !in 200..299) {
            return GitHubHtmlParseResult.ParseError(response.toUpdateErrorMessage("更新签名提交保护失败"), sourceUrl, response.statusCode, response.preview)
        }
        return GitHubHtmlParseResult.Success(Unit)
    }

    private fun requestPaged(method: String, firstUrl: String): List<BranchSettingsNetworkResponse> {
        val responses = mutableListOf<BranchSettingsNetworkResponse>()
        var nextUrl: String? = firstUrl
        var pageCount = 0
        while (!nextUrl.isNullOrBlank() && pageCount < MaxPages) {
            val response = request(method, nextUrl)
            responses += response
            if (response.statusCode !in 200..299) break
            nextUrl = response.nextUrl
            pageCount += 1
        }
        return responses
    }

    private fun request(method: String, url: String, body: String? = null): BranchSettingsNetworkResponse {
        return httpClient.execute(
            GitHubHttpRequest(
                pathOrUrl = url,
                method = method.toGitHubHttpMethod(),
                body = body,
                apiVersion = GitHubApiHeaders.LegacyApiVersion,
                followRedirects = true
            )
        ).toBranchSettingsNetworkResponse()
    }

    private fun buildRepoApiUrl(owner: String, repo: String): String {
        return "https://api.github.com/repos/${owner.toBranchPathSegment()}/${repo.toBranchPathSegment()}"
    }

    private fun buildBranchProtectionApiUrl(owner: String, repo: String, branch: String): String {
        return "${buildRepoApiUrl(owner, repo)}/branches/${branch.toBranchPathSegment()}/protection"
    }

    private fun buildWebBranchesUrl(owner: String, repo: String): String {
        return "https://github.com/${owner.toBranchPathSegment()}/${repo.toBranchPathSegment()}/settings/branches"
    }

    private fun BranchSettingsNetworkResponse.toParseFailureOrNull(
        sourceUrl: String,
        label: String
    ): GitHubHtmlParseResult<Nothing>? {
        return when {
            statusCode == HttpURLConnection.HTTP_FORBIDDEN -> GitHubHtmlParseResult.AccessDenied(
                message = "当前令牌无法读取$label。",
                sourceUrl = sourceUrl,
                statusCode = statusCode,
                htmlPreview = preview
            )
            statusCode == HttpURLConnection.HTTP_NOT_FOUND -> GitHubHtmlParseResult.NotFound(
                message = "${label}不存在，或当前令牌无法访问。",
                sourceUrl = sourceUrl,
                statusCode = statusCode,
                htmlPreview = preview
            )
            statusCode !in 200..299 -> GitHubHtmlParseResult.ParseError(
                message = "GitHub REST $label 返回 HTTP $statusCode。",
                sourceUrl = sourceUrl,
                statusCode = statusCode,
                htmlPreview = preview
            )
            else -> null
        }
    }

    private fun GitHubHttpResponse.toBranchSettingsNetworkResponse(): BranchSettingsNetworkResponse {
        return BranchSettingsNetworkResponse(
            statusCode = statusCode,
            body = body,
            linkHeader = headers["Link"]?.firstOrNull()
        )
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

    private data class BranchSettingsNetworkResponse(
        val statusCode: Int,
        val body: String,
        val linkHeader: String? = null
    ) {
        val preview: String get() = body.take(240).replace(Regex("\\s+"), " ").trim()
        val nextUrl: String? get() = linkHeader?.extractGitHubNextUrl()

        fun toUpdateErrorMessage(prefix: String): String {
            val json = runCatching { JSONObject(body) }.getOrNull()
            val message = json?.optionalBranchString("message")
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
        private const val TimeoutMillis = 15_000
        private const val NetworkErrorStatusCode = 0
        private const val PageSize = 100
        private const val MaxPages = 5
        private const val ProtectionDetailsLimit = 30
    }
}

data class RepositoryBranchSettingsSnapshot(
    val owner: String,
    val repo: String,
    val defaultBranch: String,
    val canAdmin: Boolean,
    val canPush: Boolean,
    val branches: List<RepositoryBranchSettingsItem>,
    val sourceUrl: String,
    val protectionDetailsLimit: Int
) {
    val protectedBranchCount: Int get() = branches.count { it.protected }
    val hasMoreProtectionDetailsThanLoaded: Boolean get() = branches.count { it.protected } > protectionDetailsLimit
}

data class RepositoryBranchSettingsItem(
    val name: String,
    val sha: String,
    val protected: Boolean,
    val isDefault: Boolean,
    val protection: RepositoryBranchProtectionSnapshot? = null
)

data class RepositoryBranchProtectionSnapshot(
    val branch: String,
    val requiredStatusChecks: RepositoryRequiredStatusChecksSnapshot?,
    val requiredPullRequestReviews: RepositoryRequiredPullRequestReviewsSnapshot?,
    val enforceAdmins: Boolean,
    val restrictions: RepositoryBranchRestrictionSnapshot?,
    val requiredLinearHistory: Boolean,
    val allowForcePushes: Boolean,
    val allowDeletions: Boolean,
    val requiredConversationResolution: Boolean,
    val requiredSignatures: Boolean
)

data class RepositoryRequiredStatusChecksSnapshot(
    val strict: Boolean,
    val contexts: List<String>,
    val checks: List<String>
)

data class RepositoryRequiredPullRequestReviewsSnapshot(
    val requiredApprovingReviewCount: Int,
    val dismissStaleReviews: Boolean,
    val requireCodeOwnerReviews: Boolean,
    val requireLastPushApproval: Boolean
)

data class RepositoryBranchRestrictionSnapshot(
    val users: List<String>,
    val teams: List<String>,
    val apps: List<String>
)

data class RepositoryBranchProtectionUpdateRequest(
    val requiredStatusChecks: RepositoryRequiredStatusChecksUpdate? = null,
    val enforceAdmins: Boolean = false,
    val requiredPullRequestReviews: RepositoryRequiredPullRequestReviewsUpdate? = null,
    val restrictions: RepositoryBranchRestrictionUpdate? = null,
    val requiredLinearHistory: Boolean? = null,
    val allowForcePushes: Boolean? = null,
    val allowDeletions: Boolean? = null,
    val requiredConversationResolution: Boolean? = null,
    val requiredSignatures: Boolean? = null
) {
    fun toJson(): JSONObject {
        return JSONObject()
            .put("required_status_checks", requiredStatusChecks?.toJson() ?: JSONObject.NULL)
            .put("enforce_admins", enforceAdmins)
            .put("required_pull_request_reviews", requiredPullRequestReviews?.toJson() ?: JSONObject.NULL)
            .put("restrictions", restrictions?.toJson() ?: JSONObject.NULL)
            .apply {
                requiredLinearHistory?.let { put("required_linear_history", it) }
                allowForcePushes?.let { put("allow_force_pushes", it) }
                allowDeletions?.let { put("allow_deletions", it) }
                requiredConversationResolution?.let { put("required_conversation_resolution", it) }
            }
    }
}

data class RepositoryRequiredStatusChecksUpdate(
    val strict: Boolean,
    val contexts: List<String> = emptyList()
) {
    fun toJson(): JSONObject {
        return JSONObject()
            .put("strict", strict)
            .put("contexts", JSONArray(contexts))
    }
}

data class RepositoryRequiredPullRequestReviewsUpdate(
    val dismissStaleReviews: Boolean = false,
    val requireCodeOwnerReviews: Boolean = false,
    val requiredApprovingReviewCount: Int = 1,
    val requireLastPushApproval: Boolean? = null
) {
    fun toJson(): JSONObject {
        return JSONObject()
            .put("dismiss_stale_reviews", dismissStaleReviews)
            .put("require_code_owner_reviews", requireCodeOwnerReviews)
            .put("required_approving_review_count", requiredApprovingReviewCount)
            .apply { requireLastPushApproval?.let { put("require_last_push_approval", it) } }
    }
}

data class RepositoryBranchRestrictionUpdate(
    val users: List<String> = emptyList(),
    val teams: List<String> = emptyList(),
    val apps: List<String> = emptyList()
) {
    fun toJson(): JSONObject {
        return JSONObject()
            .put("users", JSONArray(users))
            .put("teams", JSONArray(teams))
            .put("apps", JSONArray(apps))
    }
}

private fun JSONObject.toBranchItem(defaultBranch: String): RepositoryBranchSettingsItem {
    val name = optionalBranchString("name").orEmpty()
    return RepositoryBranchSettingsItem(
        name = name,
        sha = optJSONObject("commit")?.optionalBranchString("sha").orEmpty(),
        protected = optBoolean("protected", false),
        isDefault = name == defaultBranch
    )
}

private fun JSONObject.toProtectionSnapshot(branch: String): RepositoryBranchProtectionSnapshot {
    return RepositoryBranchProtectionSnapshot(
        branch = branch,
        requiredStatusChecks = optJSONObject("required_status_checks")?.toRequiredStatusChecksSnapshot(),
        requiredPullRequestReviews = optJSONObject("required_pull_request_reviews")?.toRequiredPullRequestReviewsSnapshot(),
        enforceAdmins = optJSONObject("enforce_admins")?.optBoolean("enabled", false) == true,
        restrictions = optJSONObject("restrictions")?.toBranchRestrictionSnapshot(),
        requiredLinearHistory = optJSONObject("required_linear_history")?.optBoolean("enabled", false) == true,
        allowForcePushes = optJSONObject("allow_force_pushes")?.optBoolean("enabled", false) == true,
        allowDeletions = optJSONObject("allow_deletions")?.optBoolean("enabled", false) == true,
        requiredConversationResolution = optJSONObject("required_conversation_resolution")?.optBoolean("enabled", false) == true,
        requiredSignatures = optJSONObject("required_signatures")?.optBoolean("enabled", false) == true
    )
}

private fun JSONObject.toRequiredStatusChecksSnapshot(): RepositoryRequiredStatusChecksSnapshot {
    return RepositoryRequiredStatusChecksSnapshot(
        strict = optBoolean("strict", false),
        contexts = optJSONArray("contexts").toStringList(),
        checks = optJSONArray("checks").toNamedStringList("context")
    )
}

private fun JSONObject.toRequiredPullRequestReviewsSnapshot(): RepositoryRequiredPullRequestReviewsSnapshot {
    return RepositoryRequiredPullRequestReviewsSnapshot(
        requiredApprovingReviewCount = optInt("required_approving_review_count", 0),
        dismissStaleReviews = optBoolean("dismiss_stale_reviews", false),
        requireCodeOwnerReviews = optBoolean("require_code_owner_reviews", false),
        requireLastPushApproval = optBoolean("require_last_push_approval", false)
    )
}

private fun JSONObject.toBranchRestrictionSnapshot(): RepositoryBranchRestrictionSnapshot {
    return RepositoryBranchRestrictionSnapshot(
        users = optJSONArray("users").toNamedStringList("login"),
        teams = optJSONArray("teams").toNamedStringList("slug"),
        apps = optJSONArray("apps").toNamedStringList("slug")
    )
}

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { index -> optString(index).takeIf { it.isNotBlank() } }
}

private fun JSONArray?.toNamedStringList(name: String): List<String> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { index -> optJSONObject(index)?.optionalBranchString(name) }
}

private fun JSONObject.optionalBranchString(name: String): String? {
    if (isNull(name)) return null
    return optString(name).takeIf { it.isNotBlank() && it != "null" }
}

private fun String.extractGitHubNextUrl(): String? {
    return split(',')
        .firstOrNull { part -> part.contains("rel=\"next\"") }
        ?.substringAfter('<', missingDelimiterValue = "")
        ?.substringBefore('>', missingDelimiterValue = "")
        ?.takeIf { it.isNotBlank() }
}

private fun String.toBranchPathSegment(): String {
    return java.net.URLEncoder.encode(this, Charsets.UTF_8.name()).replace("+", "%20")
}
