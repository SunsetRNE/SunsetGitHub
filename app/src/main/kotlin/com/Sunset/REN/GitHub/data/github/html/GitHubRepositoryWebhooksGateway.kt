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

class GitHubRepositoryWebhooksGateway(accessToken: String) {
    private val httpClient = GitHubHttpClient(accessToken, timeoutMillis = 15_000)
    fun loadWebhooks(owner: String, repo: String): GitHubHtmlParseResult<RepositoryWebhooksSnapshot> {
        val sourceUrl = webUrl(owner, repo)
        val response = try { request("GET", apiUrl(owner, repo)) } catch (error: SocketTimeoutException) {
            return GitHubHtmlParseResult.ParseError("GitHub REST Webhooks 请求超时。", sourceUrl, 0)
        } catch (error: IOException) {
            return GitHubHtmlParseResult.ParseError(error.message ?: "GitHub REST Webhooks 请求失败。", sourceUrl, 0)
        }
        response.toFailureOrNull(sourceUrl, "Webhooks")?.let { return it }
        val array = runCatching { JSONArray(response.body) }.getOrElse {
            return GitHubHtmlParseResult.ParseError("GitHub REST Webhooks 返回内容不是有效 JSON。", sourceUrl, response.statusCode, response.preview)
        }
        return GitHubHtmlParseResult.Success(
            RepositoryWebhooksSnapshot(owner, repo, readCanAdmin(owner, repo), (0 until array.length()).mapNotNull { array.optJSONObject(it)?.toWebhook() }, sourceUrl)
        )
    }

    fun createWebhook(owner: String, repo: String, request: RepositoryWebhookUpsertRequest): GitHubHtmlParseResult<RepositoryWebhookItem> {
        return mutateWebhook("POST", apiUrl(owner, repo), request.toJson().toString(), owner, repo, "创建 Webhook")
    }

    fun updateWebhook(owner: String, repo: String, hookId: Long, request: RepositoryWebhookUpsertRequest): GitHubHtmlParseResult<RepositoryWebhookItem> {
        return mutateWebhook("PATCH", "${apiUrl(owner, repo)}/$hookId", request.toJson().toString(), owner, repo, "更新 Webhook")
    }

    fun pingWebhook(owner: String, repo: String, hookId: Long): GitHubHtmlParseResult<Unit> {
        val sourceUrl = webUrl(owner, repo)
        val response = try { request("POST", "${apiUrl(owner, repo)}/$hookId/pings") } catch (error: IOException) {
            return GitHubHtmlParseResult.ParseError(error.message ?: "GitHub REST Ping Webhook 请求失败。", sourceUrl, 0)
        }
        response.toFailureOrNull(sourceUrl, "Ping Webhook")?.let { return it }
        return GitHubHtmlParseResult.Success(Unit)
    }

    fun deleteWebhook(owner: String, repo: String, hookId: Long): GitHubHtmlParseResult<Unit> {
        val sourceUrl = webUrl(owner, repo)
        val response = try { request("DELETE", "${apiUrl(owner, repo)}/$hookId") } catch (error: IOException) {
            return GitHubHtmlParseResult.ParseError(error.message ?: "GitHub REST 删除 Webhook 请求失败。", sourceUrl, 0)
        }
        response.toFailureOrNull(sourceUrl, "删除 Webhook")?.let { return it }
        return GitHubHtmlParseResult.Success(Unit)
    }

    private fun mutateWebhook(method: String, url: String, body: String, owner: String, repo: String, label: String): GitHubHtmlParseResult<RepositoryWebhookItem> {
        val sourceUrl = webUrl(owner, repo)
        val response = try { request(method, url, body) } catch (error: IOException) {
            return GitHubHtmlParseResult.ParseError(error.message ?: "GitHub REST $label 请求失败。", sourceUrl, 0)
        }
        response.toFailureOrNull(sourceUrl, label)?.let { return it }
        val json = runCatching { JSONObject(response.body) }.getOrElse {
            return GitHubHtmlParseResult.ParseError("GitHub REST $label 返回内容不是有效 JSON。", sourceUrl, response.statusCode, response.preview)
        }
        return GitHubHtmlParseResult.Success(json.toWebhook())
    }

    private fun readCanAdmin(owner: String, repo: String): Boolean {
        val response = runCatching { request("GET", "https://api.github.com/repos/${owner.toWebhookPathSegment()}/${repo.toWebhookPathSegment()}") }.getOrNull() ?: return false
        return runCatching { JSONObject(response.body).optJSONObject("permissions")?.optBoolean("admin", false) == true }.getOrDefault(false)
    }

    private fun request(method: String, url: String, body: String? = null): WebhookNetworkResponse {
        return httpClient.execute(
            GitHubHttpRequest(
                pathOrUrl = url,
                method = method.toWebhookGitHubHttpMethod(),
                body = body,
                apiVersion = GitHubApiHeaders.LegacyApiVersion
            )
        ).toWebhookNetworkResponse()
    }

    private fun JSONObject.toWebhook(): RepositoryWebhookItem {
        val config = optJSONObject("config") ?: JSONObject()
        return RepositoryWebhookItem(
            id = optLong("id", 0L),
            name = optString("name", "web"),
            active = optBoolean("active", false),
            events = optJSONArray("events")?.toStringList().orEmpty(),
            url = config.optString("url", ""),
            contentType = config.optString("content_type", "json"),
            insecureSsl = config.optString("insecure_ssl", "0") == "1",
            createdAt = optString("created_at", ""),
            updatedAt = optString("updated_at", ""),
            lastResponseCode = optJSONObject("last_response")?.optString("code", "").orEmpty(),
            lastResponseStatus = optJSONObject("last_response")?.optString("status", "").orEmpty(),
            lastResponseMessage = optJSONObject("last_response")?.optString("message", "").orEmpty()
        )
    }

    private fun apiUrl(owner: String, repo: String) = "https://api.github.com/repos/${owner.toWebhookPathSegment()}/${repo.toWebhookPathSegment()}/hooks"
    private fun webUrl(owner: String, repo: String) = "https://github.com/${owner.toWebhookPathSegment()}/${repo.toWebhookPathSegment()}/settings/hooks"
}

data class RepositoryWebhooksSnapshot(val owner: String, val repo: String, val canAdmin: Boolean, val hooks: List<RepositoryWebhookItem>, val sourceUrl: String)
data class RepositoryWebhookItem(val id: Long, val name: String, val active: Boolean, val events: List<String>, val url: String, val contentType: String, val insecureSsl: Boolean, val createdAt: String, val updatedAt: String, val lastResponseCode: String, val lastResponseStatus: String, val lastResponseMessage: String)
data class RepositoryWebhookUpsertRequest(val url: String, val contentType: String = "json", val secret: String? = null, val insecureSsl: Boolean = false, val active: Boolean = true, val events: List<String> = listOf("push")) {
    fun toJson(): JSONObject = JSONObject()
        .put("name", "web")
        .put("active", active)
        .put("events", JSONArray(events.ifEmpty { listOf("push") }))
        .put("config", JSONObject().put("url", url).put("content_type", contentType).put("insecure_ssl", if (insecureSsl) "1" else "0").apply { secret?.takeIf { it.isNotBlank() }?.let { put("secret", it) } })
}

private fun GitHubHttpResponse.toWebhookNetworkResponse(): WebhookNetworkResponse {
    return WebhookNetworkResponse(statusCode = statusCode, body = body)
}

private fun String.toWebhookGitHubHttpMethod(): GitHubHttpMethod {
    return when (uppercase()) {
        "GET" -> GitHubHttpMethod.GET
        "POST" -> GitHubHttpMethod.POST
        "PATCH" -> GitHubHttpMethod.PATCH
        "PUT" -> GitHubHttpMethod.PUT
        "DELETE" -> GitHubHttpMethod.DELETE
        else -> throw IllegalArgumentException("Unsupported GitHub HTTP method: $this")
    }
}

private data class WebhookNetworkResponse(val statusCode: Int, val body: String) {
    val preview: String get() = body.take(240).replace(Regex("\\s+"), " ").trim()
    fun toFailureOrNull(sourceUrl: String, label: String): GitHubHtmlParseResult<Nothing>? = when {
        statusCode == HttpURLConnection.HTTP_FORBIDDEN -> GitHubHtmlParseResult.AccessDenied("当前令牌无法访问$label。", sourceUrl, statusCode, preview)
        statusCode == HttpURLConnection.HTTP_NOT_FOUND -> GitHubHtmlParseResult.NotFound("$label 不存在，或当前令牌无法访问。", sourceUrl, statusCode, preview)
        statusCode !in 200..299 -> GitHubHtmlParseResult.ParseError("GitHub REST $label 返回 HTTP $statusCode：${errorDetail()}", sourceUrl, statusCode, preview)
        else -> null
    }
    private fun errorDetail(): String = runCatching { JSONObject(body).optString("message") }.getOrNull()?.takeIf { it.isNotBlank() } ?: preview
}
private fun JSONArray.toStringList(): List<String> = (0 until length()).mapNotNull { optString(it).takeIf { value -> value.isNotBlank() } }
private fun String.toWebhookPathSegment(): String = java.net.URLEncoder.encode(this, Charsets.UTF_8.name()).replace("+", "%20")
