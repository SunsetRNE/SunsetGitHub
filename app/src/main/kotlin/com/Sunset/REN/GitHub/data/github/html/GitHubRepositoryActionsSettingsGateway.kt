package com.Sunset.REN.GitHub.data.github.html

import com.Sunset.REN.GitHub.data.github.network.GitHubApiHeaders
import com.Sunset.REN.GitHub.data.github.network.GitHubHttpClient
import com.Sunset.REN.GitHub.data.github.network.GitHubHttpMethod
import com.Sunset.REN.GitHub.data.github.network.GitHubHttpRequest
import com.Sunset.REN.GitHub.data.github.network.GitHubHttpResponse
import com.Sunset.REN.GitHub.util.SodiumSealedBox
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.util.Base64

class GitHubRepositoryActionsSettingsGateway(accessToken: String) {
    private val httpClient = GitHubHttpClient(accessToken, timeoutMillis = TimeoutMillis)
    fun loadActionsSettings(owner: String, repo: String): GitHubHtmlParseResult<RepositoryActionsSettingsSnapshot> {
        val sourceUrl = webActionsSettingsUrl(owner, repo)
        val repoResponse = try { request("GET", repoApiUrl(owner, repo)) } catch (error: SocketTimeoutException) {
            return GitHubHtmlParseResult.ParseError("GitHub REST Actions 设置请求超时。", sourceUrl, NetworkErrorStatusCode)
        } catch (error: IOException) {
            return GitHubHtmlParseResult.ParseError(error.message?.takeIf { it.isNotBlank() } ?: "GitHub REST Actions 设置请求失败。", sourceUrl, NetworkErrorStatusCode)
        }
        repoResponse.toParseFailureOrNull(sourceUrl, "Actions 仓库权限")?.let { return it }
        val repoJson = runCatching { JSONObject(repoResponse.body) }.getOrElse {
            return GitHubHtmlParseResult.ParseError("GitHub REST Actions 仓库权限返回内容不是有效 JSON。", sourceUrl, repoResponse.statusCode, repoResponse.preview)
        }
        val permissions = repoJson.optJSONObject("permissions")
        val canAdmin = permissions?.optBoolean("admin", false) == true
        val canPush = permissions?.optBoolean("push", false) == true

        val actionsPermissions = loadOptionalJson(
            url = "${repoApiUrl(owner, repo)}/actions/permissions",
            sourceUrl = sourceUrl,
            label = "Actions 权限"
        )?.toActionsPermissions()
        val selectedActions = if (actionsPermissions?.allowedActions == "selected") {
            loadOptionalJson(
                url = "${repoApiUrl(owner, repo)}/actions/permissions/selected-actions",
                sourceUrl = sourceUrl,
                label = "Actions 允许运行范围"
            )?.toSelectedActions()
        } else {
            null
        }
        val workflowPermissions = loadOptionalJson(
            url = "${repoApiUrl(owner, repo)}/actions/permissions/workflow",
            sourceUrl = sourceUrl,
            label = "Workflow 默认权限"
        )?.toWorkflowPermissions()
        val secretsCount = loadOptionalJson(
            url = "${repoApiUrl(owner, repo)}/actions/secrets?per_page=1",
            sourceUrl = sourceUrl,
            label = "Actions Secrets"
        )?.optInt("total_count", 0)
        val variablesCount = loadOptionalJson(
            url = "${repoApiUrl(owner, repo)}/actions/variables?per_page=1",
            sourceUrl = sourceUrl,
            label = "Actions Variables"
        )?.optInt("total_count", 0)
        val retentionDays = loadOptionalJson(
            url = "${repoApiUrl(owner, repo)}/actions/permissions/artifact-and-log-retention",
            sourceUrl = sourceUrl,
            label = "Actions artifact/log retention"
        )?.optInt("days", -1)?.takeIf { it > 0 }
        val cacheUsage = loadOptionalJson(
            url = "${repoApiUrl(owner, repo)}/actions/cache/usage",
            sourceUrl = sourceUrl,
            label = "Actions Cache"
        )?.toActionsCacheUsage()

        return GitHubHtmlParseResult.Success(
            RepositoryActionsSettingsSnapshot(
                owner = owner,
                repo = repo,
                canAdmin = canAdmin,
                canPush = canPush,
                actionsPermissions = actionsPermissions,
                selectedActions = selectedActions,
                workflowPermissions = workflowPermissions,
                secretsCount = secretsCount,
                variablesCount = variablesCount,
                retentionDays = retentionDays,
                cacheUsage = cacheUsage,
                sourceUrl = sourceUrl
            )
        )
    }

    fun updateActionsEnabled(owner: String, repo: String, enabled: Boolean): GitHubHtmlParseResult<RepositoryActionsPermissionsSnapshot> {
        val sourceUrl = webActionsSettingsUrl(owner, repo)
        val body = JSONObject().put("enabled", enabled).toString()
        val response = try { request("PUT", "${repoApiUrl(owner, repo)}/actions/permissions", body) } catch (error: SocketTimeoutException) {
            return GitHubHtmlParseResult.ParseError("GitHub REST 更新 Actions 权限请求超时。", sourceUrl, NetworkErrorStatusCode)
        } catch (error: IOException) {
            return GitHubHtmlParseResult.ParseError(error.message?.takeIf { it.isNotBlank() } ?: "GitHub REST 更新 Actions 权限请求失败。", sourceUrl, NetworkErrorStatusCode)
        }
        if (response.statusCode !in 200..299) return GitHubHtmlParseResult.ParseError(response.toUpdateErrorMessage("更新 Actions 权限失败"), sourceUrl, response.statusCode, response.preview)
        return GitHubHtmlParseResult.Success(JSONObject(response.body.takeIf { it.isNotBlank() } ?: "{}").toActionsPermissions().copy(enabled = enabled))
    }

    fun updateWorkflowPermissions(owner: String, repo: String, defaultPermission: String, canApprovePullRequestReviews: Boolean): GitHubHtmlParseResult<Unit> {
        val sourceUrl = webActionsSettingsUrl(owner, repo)
        val body = JSONObject()
            .put("default_workflow_permissions", defaultPermission)
            .put("can_approve_pull_request_reviews", canApprovePullRequestReviews)
            .toString()
        val response = try { request("PUT", "${repoApiUrl(owner, repo)}/actions/permissions/workflow", body) } catch (error: SocketTimeoutException) {
            return GitHubHtmlParseResult.ParseError("GitHub REST 更新 Workflow 默认权限请求超时。", sourceUrl, NetworkErrorStatusCode)
        } catch (error: IOException) {
            return GitHubHtmlParseResult.ParseError(error.message?.takeIf { it.isNotBlank() } ?: "GitHub REST 更新 Workflow 默认权限请求失败。", sourceUrl, NetworkErrorStatusCode)
        }
        if (response.statusCode !in 200..299) return GitHubHtmlParseResult.ParseError(response.toUpdateErrorMessage("更新 Workflow 默认权限失败"), sourceUrl, response.statusCode, response.preview)
        return GitHubHtmlParseResult.Success(Unit)
    }

    fun updateRetentionDays(owner: String, repo: String, days: Int): GitHubHtmlParseResult<Int> {
        val sourceUrl = webActionsSettingsUrl(owner, repo)
        val normalizedDays = days.coerceIn(MinRetentionDays, MaxRetentionDays)
        val body = JSONObject().put("days", normalizedDays).toString()
        val response = try { request("PUT", "${repoApiUrl(owner, repo)}/actions/permissions/artifact-and-log-retention", body) } catch (error: SocketTimeoutException) {
            return GitHubHtmlParseResult.ParseError("GitHub REST 更新 Actions 保留天数请求超时。", sourceUrl, NetworkErrorStatusCode)
        } catch (error: IOException) {
            return GitHubHtmlParseResult.ParseError(error.message?.takeIf { it.isNotBlank() } ?: "GitHub REST 更新 Actions 保留天数请求失败。", sourceUrl, NetworkErrorStatusCode)
        }
        if (response.statusCode !in 200..299) return GitHubHtmlParseResult.ParseError(response.toUpdateErrorMessage("更新 Actions 保留天数失败"), sourceUrl, response.statusCode, response.preview)
        return GitHubHtmlParseResult.Success(normalizedDays)
    }

    fun listCaches(owner: String, repo: String): GitHubHtmlParseResult<List<RepositoryActionsCacheItem>> {
        val sourceUrl = webActionsSettingsUrl(owner, repo)
        val response = try { request("GET", "${repoApiUrl(owner, repo)}/actions/caches?per_page=100&sort=last_accessed_at&direction=desc") } catch (error: IOException) {
            return GitHubHtmlParseResult.ParseError(error.message?.takeIf { it.isNotBlank() } ?: "GitHub REST Actions Cache 请求失败。", sourceUrl, NetworkErrorStatusCode)
        }
        if (response.statusCode == HttpURLConnection.HTTP_FORBIDDEN) return GitHubHtmlParseResult.AccessDenied("当前令牌无法读取 Actions Cache。需要仓库管理员权限或 repo 权限。", sourceUrl, response.statusCode, response.preview)
        if (response.statusCode !in 200..299) return GitHubHtmlParseResult.ParseError(response.toUpdateErrorMessage("读取 Actions Cache 失败"), sourceUrl, response.statusCode, response.preview)
        val array = JSONObject(response.body).optJSONArray("actions_caches") ?: JSONArray()
        return GitHubHtmlParseResult.Success((0 until array.length()).mapNotNull { array.optJSONObject(it)?.toActionsCacheItem() })
    }

    fun deleteCache(owner: String, repo: String, cacheId: Long): GitHubHtmlParseResult<Unit> {
        val sourceUrl = webActionsSettingsUrl(owner, repo)
        val response = try { request("DELETE", "${repoApiUrl(owner, repo)}/actions/caches/$cacheId") } catch (error: IOException) {
            return GitHubHtmlParseResult.ParseError(error.message?.takeIf { it.isNotBlank() } ?: "GitHub REST 删除 Actions Cache 请求失败。", sourceUrl, NetworkErrorStatusCode)
        }
        if (response.statusCode !in 200..299) return GitHubHtmlParseResult.ParseError(response.toUpdateErrorMessage("删除 Actions Cache 失败"), sourceUrl, response.statusCode, response.preview)
        return GitHubHtmlParseResult.Success(Unit)
    }

    fun deleteCachesByKey(owner: String, repo: String, key: String, ref: String? = null): GitHubHtmlParseResult<Int> {
        val sourceUrl = webActionsSettingsUrl(owner, repo)
        val query = buildString {
            append("key=").append(key.toActionsSettingsPathSegment())
            ref?.takeIf { it.isNotBlank() }?.let { append("&ref=").append(it.toActionsSettingsPathSegment()) }
        }
        val response = try { request("DELETE", "${repoApiUrl(owner, repo)}/actions/caches?$query") } catch (error: IOException) {
            return GitHubHtmlParseResult.ParseError(error.message?.takeIf { it.isNotBlank() } ?: "GitHub REST 删除 Actions Cache 请求失败。", sourceUrl, NetworkErrorStatusCode)
        }
        if (response.statusCode !in 200..299) return GitHubHtmlParseResult.ParseError(response.toUpdateErrorMessage("按 key 删除 Actions Cache 失败"), sourceUrl, response.statusCode, response.preview)
        val count = runCatching { JSONObject(response.body).optInt("total_count", 0) }.getOrDefault(0)
        return GitHubHtmlParseResult.Success(count)
    }

    private fun loadOptionalJson(url: String, sourceUrl: String, label: String): JSONObject? {
        val response = try { request("GET", url) } catch (_: SocketTimeoutException) { return null } catch (_: IOException) { return null }
        if (response.statusCode == HttpURLConnection.HTTP_FORBIDDEN || response.statusCode == HttpURLConnection.HTTP_NOT_FOUND) return null
        if (response.statusCode !in 200..299) return null
        return runCatching { JSONObject(response.body) }.getOrNull()
    }

    private fun request(method: String, url: String, body: String? = null): ActionsSettingsNetworkResponse {
        return httpClient.execute(
            GitHubHttpRequest(
                pathOrUrl = url,
                method = method.toGitHubHttpMethod(),
                body = body,
                apiVersion = GitHubApiHeaders.LegacyApiVersion,
                followRedirects = true
            )
        ).toActionsSettingsNetworkResponse()
    }

    private fun ActionsSettingsNetworkResponse.toParseFailureOrNull(sourceUrl: String, label: String): GitHubHtmlParseResult<Nothing>? = when {
        statusCode == HttpURLConnection.HTTP_FORBIDDEN -> GitHubHtmlParseResult.AccessDenied("当前令牌无法读取$label。", sourceUrl, statusCode, preview)
        statusCode == HttpURLConnection.HTTP_NOT_FOUND -> GitHubHtmlParseResult.NotFound("${label}不存在，或当前令牌无法访问。", sourceUrl, statusCode, preview)
        statusCode !in 200..299 -> GitHubHtmlParseResult.ParseError("GitHub REST $label 返回 HTTP $statusCode。", sourceUrl, statusCode, preview)
        else -> null
    }

    private fun repoApiUrl(owner: String, repo: String) = "https://api.github.com/repos/${owner.toActionsSettingsPathSegment()}/${repo.toActionsSettingsPathSegment()}"
    private fun webActionsSettingsUrl(owner: String, repo: String) = "https://github.com/${owner.toActionsSettingsPathSegment()}/${repo.toActionsSettingsPathSegment()}/settings/actions"

    private fun GitHubHttpResponse.toActionsSettingsNetworkResponse(): ActionsSettingsNetworkResponse {
        return ActionsSettingsNetworkResponse(statusCode = statusCode, body = body)
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

    private data class ActionsSettingsNetworkResponse(val statusCode: Int, val body: String) {
        val preview: String get() = body.take(240).replace(Regex("\\s+"), " ").trim()
        fun toUpdateErrorMessage(prefix: String): String {
            val json = runCatching { JSONObject(body) }.getOrNull()
            val message = json?.optionalActionsSettingsString("message")
            val errors = json?.optJSONArray("errors")
            val detail = buildList {
                if (!message.isNullOrBlank()) add(message)
                if (errors != null) for (index in 0 until errors.length()) errors.opt(index)?.toString()?.takeIf { it.isNotBlank() }?.let { add(it) }
            }.joinToString("；")
            return if (detail.isNotBlank()) "$prefix：$detail" else "$prefix：HTTP $statusCode"
        }
    }

    private companion object { const val TimeoutMillis = 15_000; const val NetworkErrorStatusCode = 0; const val MinRetentionDays = 1; const val MaxRetentionDays = 400 }
}
data class RepositoryActionsSettingsSnapshot(
    val owner: String,
    val repo: String,
    val canAdmin: Boolean,
    val canPush: Boolean,
    val actionsPermissions: RepositoryActionsPermissionsSnapshot?,
    val selectedActions: RepositorySelectedActionsSnapshot?,
    val workflowPermissions: RepositoryWorkflowPermissionsSnapshot?,
    val secretsCount: Int?,
    val variablesCount: Int?,
    val retentionDays: Int?,
    val cacheUsage: RepositoryActionsCacheUsage?,
    val sourceUrl: String
)

data class RepositoryActionsPermissionsSnapshot(val enabled: Boolean, val allowedActions: String, val selectedActionsUrl: String?)
data class RepositorySelectedActionsSnapshot(
    val githubOwnedAllowed: Boolean,
    val verifiedAllowed: Boolean,
    val patternsAllowed: List<String>
)
data class RepositoryWorkflowPermissionsSnapshot(val defaultWorkflowPermissions: String, val canApprovePullRequestReviews: Boolean)
data class RepositoryActionsSecretItem(val name: String, val createdAt: String, val updatedAt: String)
data class RepositoryActionsVariableItem(val name: String, val value: String, val createdAt: String, val updatedAt: String)
data class RepositoryActionsCacheUsage(val activeCachesSizeInBytes: Long, val activeCachesCount: Int)
data class RepositoryActionsCacheItem(
    val id: Long,
    val ref: String,
    val key: String,
    val version: String,
    val sizeInBytes: Long,
    val createdAt: String,
    val lastAccessedAt: String
)


fun GitHubRepositoryActionsSettingsGateway.updateAllowedActions(owner: String, repo: String, allowedActions: String): GitHubHtmlParseResult<RepositoryActionsPermissionsSnapshot> {
    val sourceUrl = "https://github.com/${owner.toActionsSettingsPathSegment()}/${repo.toActionsSettingsPathSegment()}/settings/actions"
    val body = JSONObject().put("allowed_actions", allowedActions).toString()
    val response = try { requestForExtensions("PUT", "https://api.github.com/repos/${owner.toActionsSettingsPathSegment()}/${repo.toActionsSettingsPathSegment()}/actions/permissions", body) } catch (error: SocketTimeoutException) {
        return GitHubHtmlParseResult.ParseError("GitHub REST 更新 Actions 允许运行范围请求超时。", sourceUrl, 0)
    } catch (error: IOException) {
        return GitHubHtmlParseResult.ParseError(error.message?.takeIf { it.isNotBlank() } ?: "GitHub REST 更新 Actions 允许运行范围请求失败。", sourceUrl, 0)
    }
    if (response.statusCode !in 200..299) return GitHubHtmlParseResult.ParseError(response.toUpdateErrorMessage("更新 Actions 允许运行范围失败"), sourceUrl, response.statusCode, response.preview)
    return GitHubHtmlParseResult.Success(JSONObject(response.body.takeIf { it.isNotBlank() } ?: "{}").toActionsPermissions().copy(allowedActions = allowedActions))
}

fun GitHubRepositoryActionsSettingsGateway.updateSelectedActions(owner: String, repo: String, selected: RepositorySelectedActionsSnapshot): GitHubHtmlParseResult<RepositorySelectedActionsSnapshot> {
    val sourceUrl = "https://github.com/${owner.toActionsSettingsPathSegment()}/${repo.toActionsSettingsPathSegment()}/settings/actions"
    val body = JSONObject()
        .put("github_owned_allowed", selected.githubOwnedAllowed)
        .put("verified_allowed", selected.verifiedAllowed)
        .put("patterns_allowed", JSONArray(selected.patternsAllowed))
        .toString()
    val response = try { requestForExtensions("PUT", "https://api.github.com/repos/${owner.toActionsSettingsPathSegment()}/${repo.toActionsSettingsPathSegment()}/actions/permissions/selected-actions", body) } catch (error: SocketTimeoutException) {
        return GitHubHtmlParseResult.ParseError("GitHub REST 更新 Actions 白名单请求超时。", sourceUrl, 0)
    } catch (error: IOException) {
        return GitHubHtmlParseResult.ParseError(error.message?.takeIf { it.isNotBlank() } ?: "GitHub REST 更新 Actions 白名单请求失败。", sourceUrl, 0)
    }
    if (response.statusCode !in 200..299) return GitHubHtmlParseResult.ParseError(response.toUpdateErrorMessage("更新 Actions 白名单失败"), sourceUrl, response.statusCode, response.preview)
    return GitHubHtmlParseResult.Success(selected)
}

fun GitHubRepositoryActionsSettingsGateway.listSecrets(owner: String, repo: String): GitHubHtmlParseResult<List<RepositoryActionsSecretItem>> {
    val sourceUrl = "https://github.com/${owner.toActionsSettingsPathSegment()}/${repo.toActionsSettingsPathSegment()}/settings/secrets/actions"
    val response = try { requestForExtensions("GET", "https://api.github.com/repos/${owner.toActionsSettingsPathSegment()}/${repo.toActionsSettingsPathSegment()}/actions/secrets?per_page=100") } catch (error: IOException) {
        return GitHubHtmlParseResult.ParseError(error.message ?: "GitHub REST Actions Secrets 请求失败。", sourceUrl, 0)
    }
    if (response.statusCode == HttpURLConnection.HTTP_FORBIDDEN) return GitHubHtmlParseResult.AccessDenied("当前令牌无法读取 Actions Secrets。需要仓库管理员权限或 repo 权限。", sourceUrl, response.statusCode, response.preview)
    if (response.statusCode !in 200..299) return GitHubHtmlParseResult.ParseError(response.toUpdateErrorMessage("读取 Actions Secrets 失败"), sourceUrl, response.statusCode, response.preview)
    val array = JSONObject(response.body).optJSONArray("secrets") ?: JSONArray()
    return GitHubHtmlParseResult.Success((0 until array.length()).mapNotNull { array.optJSONObject(it)?.let { json -> RepositoryActionsSecretItem(json.optionalActionsSettingsString("name").orEmpty(), json.optionalActionsSettingsString("created_at").orEmpty(), json.optionalActionsSettingsString("updated_at").orEmpty()) } })
}

fun GitHubRepositoryActionsSettingsGateway.listVariables(owner: String, repo: String): GitHubHtmlParseResult<List<RepositoryActionsVariableItem>> {
    val sourceUrl = "https://github.com/${owner.toActionsSettingsPathSegment()}/${repo.toActionsSettingsPathSegment()}/settings/variables/actions"
    val response = try { requestForExtensions("GET", "https://api.github.com/repos/${owner.toActionsSettingsPathSegment()}/${repo.toActionsSettingsPathSegment()}/actions/variables?per_page=100") } catch (error: IOException) {
        return GitHubHtmlParseResult.ParseError(error.message ?: "GitHub REST Actions Variables 请求失败。", sourceUrl, 0)
    }
    if (response.statusCode == HttpURLConnection.HTTP_FORBIDDEN) return GitHubHtmlParseResult.AccessDenied("当前令牌无法读取 Actions Variables。需要仓库管理员权限或 repo 权限。", sourceUrl, response.statusCode, response.preview)
    if (response.statusCode !in 200..299) return GitHubHtmlParseResult.ParseError(response.toUpdateErrorMessage("读取 Actions Variables 失败"), sourceUrl, response.statusCode, response.preview)
    val array = JSONObject(response.body).optJSONArray("variables") ?: JSONArray()
    return GitHubHtmlParseResult.Success((0 until array.length()).mapNotNull { array.optJSONObject(it)?.let { json -> RepositoryActionsVariableItem(json.optionalActionsSettingsString("name").orEmpty(), json.optionalActionsSettingsString("value").orEmpty(), json.optionalActionsSettingsString("created_at").orEmpty(), json.optionalActionsSettingsString("updated_at").orEmpty()) } })
}

fun GitHubRepositoryActionsSettingsGateway.upsertVariable(owner: String, repo: String, name: String, value: String): GitHubHtmlParseResult<Unit> {
    val sourceUrl = "https://github.com/${owner.toActionsSettingsPathSegment()}/${repo.toActionsSettingsPathSegment()}/settings/variables/actions"
    val base = "https://api.github.com/repos/${owner.toActionsSettingsPathSegment()}/${repo.toActionsSettingsPathSegment()}/actions/variables"
    val body = JSONObject().put("name", name).put("value", value).toString()
    val create = try { requestForExtensions("POST", base, body) } catch (error: IOException) { return GitHubHtmlParseResult.ParseError(error.message ?: "GitHub REST 保存 Actions Variable 请求失败。", sourceUrl, 0) }
    val response = if (create.statusCode == HttpURLConnection.HTTP_CONFLICT || create.statusCode == 422) {
        requestForExtensions("PATCH", "$base/${name.toActionsSettingsPathSegment()}", JSONObject().put("name", name).put("value", value).toString())
    } else create
    if (response.statusCode !in 200..299) return GitHubHtmlParseResult.ParseError(response.toUpdateErrorMessage("保存 Actions Variable 失败"), sourceUrl, response.statusCode, response.preview)
    return GitHubHtmlParseResult.Success(Unit)
}

fun GitHubRepositoryActionsSettingsGateway.deleteVariable(owner: String, repo: String, name: String): GitHubHtmlParseResult<Unit> {
    val sourceUrl = "https://github.com/${owner.toActionsSettingsPathSegment()}/${repo.toActionsSettingsPathSegment()}/settings/variables/actions"
    val response = try { requestForExtensions("DELETE", "https://api.github.com/repos/${owner.toActionsSettingsPathSegment()}/${repo.toActionsSettingsPathSegment()}/actions/variables/${name.toActionsSettingsPathSegment()}") } catch (error: IOException) { return GitHubHtmlParseResult.ParseError(error.message ?: "GitHub REST 删除 Actions Variable 请求失败。", sourceUrl, 0) }
    if (response.statusCode !in 200..299) return GitHubHtmlParseResult.ParseError(response.toUpdateErrorMessage("删除 Actions Variable 失败"), sourceUrl, response.statusCode, response.preview)
    return GitHubHtmlParseResult.Success(Unit)
}
fun GitHubRepositoryActionsSettingsGateway.deleteSecret(owner: String, repo: String, name: String): GitHubHtmlParseResult<Unit> {
val sourceUrl = "https://github.com/${owner.toActionsSettingsPathSegment()}/${repo.toActionsSettingsPathSegment()}/settings/secrets/actions"
val response = try { requestForExtensions("DELETE", "https://api.github.com/repos/${owner.toActionsSettingsPathSegment()}/${repo.toActionsSettingsPathSegment()}/actions/secrets/${name.toActionsSettingsPathSegment()}") } catch (error: IOException) { return GitHubHtmlParseResult.ParseError(error.message ?: "GitHub REST 删除 Actions Secret 请求失败。", sourceUrl, 0) }
if (response.statusCode !in 200..299) return GitHubHtmlParseResult.ParseError(response.toUpdateErrorMessage("删除 Actions Secret 失败"), sourceUrl, response.statusCode, response.preview)
return GitHubHtmlParseResult.Success(Unit)
}

fun GitHubRepositoryActionsSettingsGateway.upsertSecret(owner: String, repo: String, name: String, value: String): GitHubHtmlParseResult<Unit> {
val sourceUrl = "https://github.com/${owner.toActionsSettingsPathSegment()}/${repo.toActionsSettingsPathSegment()}/settings/secrets/actions"
val base = "https://api.github.com/repos/${owner.toActionsSettingsPathSegment()}/${repo.toActionsSettingsPathSegment()}/actions/secrets"
val keyResponse = try { requestForExtensions("GET", "$base/public-key") } catch (error: IOException) { return GitHubHtmlParseResult.ParseError(error.message ?: "GitHub REST 读取 Actions Secret 公钥请求失败。", sourceUrl, 0) }
if (keyResponse.statusCode !in 200..299) return GitHubHtmlParseResult.ParseError(keyResponse.toUpdateErrorMessage("读取 Actions Secret 公钥失败"), sourceUrl, keyResponse.statusCode, keyResponse.preview)
val keyJson = runCatching { JSONObject(keyResponse.body) }.getOrElse { return GitHubHtmlParseResult.ParseError("Actions Secret 公钥返回内容不是有效 JSON。", sourceUrl, keyResponse.statusCode, keyResponse.preview) }
val keyId = keyJson.optionalActionsSettingsString("key_id").orEmpty()
val publicKey = keyJson.optionalActionsSettingsString("key").orEmpty()
if (keyId.isBlank() || publicKey.isBlank()) return GitHubHtmlParseResult.ParseError("Actions Secret 公钥缺少 key_id 或 key。", sourceUrl, keyResponse.statusCode, keyResponse.preview)
val encryptedValue = runCatching {
Base64.getEncoder().encodeToString(SodiumSealedBox.seal(value.toByteArray(Charsets.UTF_8), Base64.getDecoder().decode(publicKey)))
}.getOrElse { error -> return GitHubHtmlParseResult.ParseError(error.message ?: "加密 Actions Secret 失败。", sourceUrl, 0) }
val body = JSONObject().put("encrypted_value", encryptedValue).put("key_id", keyId).toString()
val response = try { requestForExtensions("PUT", "$base/${name.toActionsSettingsPathSegment()}", body) } catch (error: IOException) { return GitHubHtmlParseResult.ParseError(error.message ?: "GitHub REST 保存 Actions Secret 请求失败。", sourceUrl, 0) }
if (response.statusCode !in 200..299) return GitHubHtmlParseResult.ParseError(response.toUpdateErrorMessage("保存 Actions Secret 失败"), sourceUrl, response.statusCode, response.preview)
return GitHubHtmlParseResult.Success(Unit)
}

private fun GitHubRepositoryActionsSettingsGateway.requestForExtensions(method: String, url: String, body: String? = null): ActionsSettingsExtensionNetworkResponse {
    val field = GitHubRepositoryActionsSettingsGateway::class.java.getDeclaredField("httpClient").apply { isAccessible = true }
    val client = field.get(this) as GitHubHttpClient
    return client.execute(
        GitHubHttpRequest(
            pathOrUrl = url,
            method = method.toActionsSettingsGitHubHttpMethod(),
            body = body,
            apiVersion = GitHubApiHeaders.LegacyApiVersion,
            followRedirects = true
        )
    ).toActionsSettingsExtensionNetworkResponse()
}

private fun GitHubHttpResponse.toActionsSettingsExtensionNetworkResponse(): ActionsSettingsExtensionNetworkResponse {
    return ActionsSettingsExtensionNetworkResponse(statusCode = statusCode, body = body)
}

private fun String.toActionsSettingsGitHubHttpMethod(): GitHubHttpMethod {
    return when (uppercase()) {
        "GET" -> GitHubHttpMethod.GET
        "POST" -> GitHubHttpMethod.POST
        "PATCH" -> GitHubHttpMethod.PATCH
        "PUT" -> GitHubHttpMethod.PUT
        "DELETE" -> GitHubHttpMethod.DELETE
        else -> throw IllegalArgumentException("Unsupported GitHub HTTP method: $this")
    }
}

private data class ActionsSettingsExtensionNetworkResponse(val statusCode: Int, val body: String) {
    val preview: String get() = body.take(240).replace(Regex("\\s+"), " ").trim()
    fun toUpdateErrorMessage(prefix: String): String {
        val json = runCatching { JSONObject(body) }.getOrNull()
        val message = json?.optionalActionsSettingsString("message")
        val errors = json?.optJSONArray("errors")
        val detail = buildList {
            if (!message.isNullOrBlank()) add(message)
            if (errors != null) for (index in 0 until errors.length()) errors.opt(index)?.toString()?.takeIf { it.isNotBlank() }?.let { add(it) }
        }.joinToString("；")
        return if (detail.isNotBlank()) "$prefix：$detail" else "$prefix：HTTP $statusCode"
    }
}

private fun JSONObject.toActionsPermissions(): RepositoryActionsPermissionsSnapshot = RepositoryActionsPermissionsSnapshot(
    enabled = optBoolean("enabled", false),
    allowedActions = optionalActionsSettingsString("allowed_actions") ?: "unknown",
    selectedActionsUrl = optionalActionsSettingsString("selected_actions_url")
)

private fun JSONObject.toSelectedActions(): RepositorySelectedActionsSnapshot = RepositorySelectedActionsSnapshot(
    githubOwnedAllowed = optBoolean("github_owned_allowed", false),
    verifiedAllowed = optBoolean("verified_allowed", false),
    patternsAllowed = optJSONArray("patterns_allowed").toActionsSettingsStringList()
)

private fun JSONObject.toWorkflowPermissions(): RepositoryWorkflowPermissionsSnapshot = RepositoryWorkflowPermissionsSnapshot(
    defaultWorkflowPermissions = optionalActionsSettingsString("default_workflow_permissions") ?: "read",
    canApprovePullRequestReviews = optBoolean("can_approve_pull_request_reviews", false)
)

private fun JSONObject.toActionsCacheUsage(): RepositoryActionsCacheUsage = RepositoryActionsCacheUsage(
    activeCachesSizeInBytes = optLong("active_caches_size_in_bytes", 0L),
    activeCachesCount = optInt("active_caches_count", 0)
)

private fun JSONObject.toActionsCacheItem(): RepositoryActionsCacheItem = RepositoryActionsCacheItem(
    id = optLong("id", 0L),
    ref = optionalActionsSettingsString("ref").orEmpty(),
    key = optionalActionsSettingsString("key").orEmpty(),
    version = optionalActionsSettingsString("version").orEmpty(),
    sizeInBytes = optLong("size_in_bytes", 0L),
    createdAt = optionalActionsSettingsString("created_at").orEmpty(),
    lastAccessedAt = optionalActionsSettingsString("last_accessed_at").orEmpty()
)

private fun JSONArray?.toActionsSettingsStringList(): List<String> { if (this == null) return emptyList(); return (0 until length()).mapNotNull { optString(it).takeIf { value -> value.isNotBlank() } } }
private fun JSONObject.optionalActionsSettingsString(name: String): String? { if (isNull(name)) return null; return optString(name).takeIf { it.isNotBlank() && it != "null" } }
private fun String.toActionsSettingsPathSegment(): String = java.net.URLEncoder.encode(this, Charsets.UTF_8.name()).replace("+", "%20")