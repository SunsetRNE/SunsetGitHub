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

class GitHubRepositoryDeployKeysGateway(accessToken: String) {
    private val httpClient = GitHubHttpClient(accessToken, timeoutMillis = 15_000)
    fun loadDeployKeys(owner: String, repo: String): GitHubHtmlParseResult<RepositoryDeployKeysSnapshot> {
        val sourceUrl = webUrl(owner, repo)
        val response = try { request("GET", apiUrl(owner, repo)) } catch (error: SocketTimeoutException) {
            return GitHubHtmlParseResult.ParseError("GitHub REST Deploy Keys 请求超时。", sourceUrl, 0)
        } catch (error: IOException) {
            return GitHubHtmlParseResult.ParseError(error.message ?: "GitHub REST Deploy Keys 请求失败。", sourceUrl, 0)
        }
        response.toFailureOrNull(sourceUrl, "Deploy Keys")?.let { return it }
        val array = runCatching { JSONArray(response.body) }.getOrElse {
            return GitHubHtmlParseResult.ParseError("GitHub REST Deploy Keys 返回内容不是有效 JSON。", sourceUrl, response.statusCode, response.preview)
        }
        return GitHubHtmlParseResult.Success(
            RepositoryDeployKeysSnapshot(owner, repo, readCanAdmin(owner, repo), (0 until array.length()).mapNotNull { array.optJSONObject(it)?.toDeployKey() }, sourceUrl)
        )
    }

    fun addDeployKey(owner: String, repo: String, title: String, key: String, readOnly: Boolean): GitHubHtmlParseResult<RepositoryDeployKeyItem> {
        val sourceUrl = webUrl(owner, repo)
        val body = JSONObject().put("title", title).put("key", key).put("read_only", readOnly).toString()
        val response = try { request("POST", apiUrl(owner, repo), body) } catch (error: IOException) {
            return GitHubHtmlParseResult.ParseError(error.message ?: "GitHub REST 添加 Deploy Key 请求失败。", sourceUrl, 0)
        }
        response.toFailureOrNull(sourceUrl, "添加 Deploy Key")?.let { return it }
        val json = runCatching { JSONObject(response.body) }.getOrElse {
            return GitHubHtmlParseResult.ParseError("GitHub REST 添加 Deploy Key 返回内容不是有效 JSON。", sourceUrl, response.statusCode, response.preview)
        }
        return GitHubHtmlParseResult.Success(json.toDeployKey())
    }

    fun deleteDeployKey(owner: String, repo: String, keyId: Long): GitHubHtmlParseResult<Unit> {
        val sourceUrl = webUrl(owner, repo)
        val response = try { request("DELETE", "${apiUrl(owner, repo)}/$keyId") } catch (error: IOException) {
            return GitHubHtmlParseResult.ParseError(error.message ?: "GitHub REST 删除 Deploy Key 请求失败。", sourceUrl, 0)
        }
        response.toFailureOrNull(sourceUrl, "删除 Deploy Key")?.let { return it }
        return GitHubHtmlParseResult.Success(Unit)
    }

    private fun readCanAdmin(owner: String, repo: String): Boolean {
        val response = runCatching { request("GET", "https://api.github.com/repos/${owner.toDeployKeyPathSegment()}/${repo.toDeployKeyPathSegment()}") }.getOrNull() ?: return false
        return runCatching { JSONObject(response.body).optJSONObject("permissions")?.optBoolean("admin", false) == true }.getOrDefault(false)
    }

    private fun request(method: String, url: String, body: String? = null): DeployKeyNetworkResponse {
        return httpClient.execute(
            GitHubHttpRequest(
                pathOrUrl = url,
                method = method.toDeployKeyGitHubHttpMethod(),
                body = body,
                apiVersion = GitHubApiHeaders.LegacyApiVersion
            )
        ).toDeployKeyNetworkResponse()
    }

    private fun JSONObject.toDeployKey(): RepositoryDeployKeyItem = RepositoryDeployKeyItem(
        id = optLong("id", 0L),
        title = optString("title", ""),
        key = optString("key", ""),
        readOnly = optBoolean("read_only", true),
        verified = optBoolean("verified", false),
        createdAt = optString("created_at", ""),
        url = optString("url", "")
    )

    private fun apiUrl(owner: String, repo: String) = "https://api.github.com/repos/${owner.toDeployKeyPathSegment()}/${repo.toDeployKeyPathSegment()}/keys"
    private fun webUrl(owner: String, repo: String) = "https://github.com/${owner.toDeployKeyPathSegment()}/${repo.toDeployKeyPathSegment()}/settings/keys"
}

data class RepositoryDeployKeysSnapshot(val owner: String, val repo: String, val canAdmin: Boolean, val keys: List<RepositoryDeployKeyItem>, val sourceUrl: String)
data class RepositoryDeployKeyItem(val id: Long, val title: String, val key: String, val readOnly: Boolean, val verified: Boolean, val createdAt: String, val url: String)

private fun GitHubHttpResponse.toDeployKeyNetworkResponse(): DeployKeyNetworkResponse {
    return DeployKeyNetworkResponse(statusCode = statusCode, body = body)
}

private fun String.toDeployKeyGitHubHttpMethod(): GitHubHttpMethod {
    return when (uppercase()) {
        "GET" -> GitHubHttpMethod.GET
        "POST" -> GitHubHttpMethod.POST
        "PATCH" -> GitHubHttpMethod.PATCH
        "PUT" -> GitHubHttpMethod.PUT
        "DELETE" -> GitHubHttpMethod.DELETE
        else -> throw IllegalArgumentException("Unsupported GitHub HTTP method: $this")
    }
}

private data class DeployKeyNetworkResponse(val statusCode: Int, val body: String) {
    val preview: String get() = body.take(240).replace(Regex("\\s+"), " ").trim()
    fun toFailureOrNull(sourceUrl: String, label: String): GitHubHtmlParseResult<Nothing>? = when {
        statusCode == HttpURLConnection.HTTP_FORBIDDEN -> GitHubHtmlParseResult.AccessDenied("当前令牌无法访问$label。", sourceUrl, statusCode, preview)
        statusCode == HttpURLConnection.HTTP_NOT_FOUND -> GitHubHtmlParseResult.NotFound("$label 不存在，或当前令牌无法访问。", sourceUrl, statusCode, preview)
        statusCode !in 200..299 -> GitHubHtmlParseResult.ParseError("GitHub REST $label 返回 HTTP $statusCode：${errorDetail()}", sourceUrl, statusCode, preview)
        else -> null
    }
    private fun errorDetail(): String = runCatching { JSONObject(body).optString("message") }.getOrNull()?.takeIf { it.isNotBlank() } ?: preview
}
private fun String.toDeployKeyPathSegment(): String = java.net.URLEncoder.encode(this, Charsets.UTF_8.name()).replace("+", "%20")
