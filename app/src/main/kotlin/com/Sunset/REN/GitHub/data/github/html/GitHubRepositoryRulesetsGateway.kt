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

class GitHubRepositoryRulesetsGateway(accessToken: String) {
    private val httpClient = GitHubHttpClient(accessToken, timeoutMillis = 15_000)
    fun loadRulesets(owner: String, repo: String): GitHubHtmlParseResult<RepositoryRulesetsSnapshot> {
        val sourceUrl = webUrl(owner, repo)
        val response = try {
            request("GET", apiUrl(owner, repo))
        } catch (error: SocketTimeoutException) {
            return GitHubHtmlParseResult.ParseError("GitHub REST Rulesets 请求超时。", sourceUrl, 0)
        } catch (error: IOException) {
            return GitHubHtmlParseResult.ParseError(error.message ?: "GitHub REST Rulesets 请求失败。", sourceUrl, 0)
        }
        response.toFailureOrNull(sourceUrl, "Rulesets")?.let { return it }
        val array = runCatching { JSONArray(response.body) }.getOrElse {
            return GitHubHtmlParseResult.ParseError("GitHub REST Rulesets 返回内容不是有效 JSON。", sourceUrl, response.statusCode, response.preview)
        }
        return GitHubHtmlParseResult.Success(
            RepositoryRulesetsSnapshot(
                owner = owner,
                repo = repo,
                canAdmin = readCanAdmin(owner, repo),
                rulesets = (0 until array.length()).mapNotNull { array.optJSONObject(it)?.toRuleset() },
                sourceUrl = sourceUrl
            )
        )
    }

    private fun readCanAdmin(owner: String, repo: String): Boolean {
        val response = runCatching { request("GET", "https://api.github.com/repos/${owner.toRulesetsPathSegment()}/${repo.toRulesetsPathSegment()}") }.getOrNull() ?: return false
        return runCatching { JSONObject(response.body).optJSONObject("permissions")?.optBoolean("admin", false) == true }.getOrDefault(false)
    }

    private fun request(method: String, url: String, body: String? = null): RulesetsNetworkResponse {
        return httpClient.execute(
            GitHubHttpRequest(
                pathOrUrl = url,
                method = method.toRulesetsGitHubHttpMethod(),
                body = body,
                apiVersion = GitHubApiHeaders.LegacyApiVersion
            )
        ).toRulesetsNetworkResponse()
    }

    private fun JSONObject.toRuleset(): RepositoryRulesetItem {
        val rules = optJSONArray("rules") ?: JSONArray()
        val conditions = optJSONObject("conditions")
        return RepositoryRulesetItem(
            id = optLong("id", 0L),
            name = optString("name", ""),
            target = optString("target", ""),
            enforcement = optString("enforcement", ""),
            sourceType = optString("source_type", ""),
            nodeId = optString("node_id", ""),
            createdAt = optString("created_at", ""),
            updatedAt = optString("updated_at", ""),
            rulesCount = rules.length(),
            ruleTypes = (0 until rules.length()).mapNotNull { rules.optJSONObject(it)?.optString("type")?.takeIf { value -> value.isNotBlank() } }.distinct(),
            conditionsSummary = conditions?.toConditionsSummary().orEmpty()
        )
    }

    private fun JSONObject.toConditionsSummary(): List<String> {
        val result = mutableListOf<String>()
        optJSONObject("ref_name")?.let { ref ->
            ref.optJSONArray("include")?.toRulesetsStringList()?.takeIf { it.isNotEmpty() }?.let { result += "包含引用：${it.joinToString()}" }
            ref.optJSONArray("exclude")?.toRulesetsStringList()?.takeIf { it.isNotEmpty() }?.let { result += "排除引用：${it.joinToString()}" }
        }
        optJSONObject("repository_name")?.let { repo ->
            repo.optJSONArray("include")?.toRulesetsStringList()?.takeIf { it.isNotEmpty() }?.let { result += "仓库名包含：${it.joinToString()}" }
            repo.optJSONArray("exclude")?.toRulesetsStringList()?.takeIf { it.isNotEmpty() }?.let { result += "仓库名排除：${it.joinToString()}" }
        }
        return result
    }

    private fun apiUrl(owner: String, repo: String) = "https://api.github.com/repos/${owner.toRulesetsPathSegment()}/${repo.toRulesetsPathSegment()}/rulesets"
    private fun webUrl(owner: String, repo: String) = "https://github.com/${owner.toRulesetsPathSegment()}/${repo.toRulesetsPathSegment()}/settings/rules"
}

data class RepositoryRulesetsSnapshot(
    val owner: String,
    val repo: String,
    val canAdmin: Boolean,
    val rulesets: List<RepositoryRulesetItem>,
    val sourceUrl: String
)

data class RepositoryRulesetItem(
    val id: Long,
    val name: String,
    val target: String,
    val enforcement: String,
    val sourceType: String,
    val nodeId: String,
    val createdAt: String,
    val updatedAt: String,
    val rulesCount: Int,
    val ruleTypes: List<String>,
    val conditionsSummary: List<String>
)

private fun GitHubHttpResponse.toRulesetsNetworkResponse(): RulesetsNetworkResponse {
    return RulesetsNetworkResponse(statusCode = statusCode, body = body)
}

private fun String.toRulesetsGitHubHttpMethod(): GitHubHttpMethod {
    return when (uppercase()) {
        "GET" -> GitHubHttpMethod.GET
        "POST" -> GitHubHttpMethod.POST
        "PATCH" -> GitHubHttpMethod.PATCH
        "PUT" -> GitHubHttpMethod.PUT
        "DELETE" -> GitHubHttpMethod.DELETE
        else -> throw IllegalArgumentException("Unsupported GitHub HTTP method: $this")
    }
}

private data class RulesetsNetworkResponse(val statusCode: Int, val body: String) {
    val preview: String get() = body.take(240).replace(Regex("\\s+"), " ").trim()
    fun toFailureOrNull(sourceUrl: String, label: String): GitHubHtmlParseResult<Nothing>? = when {
        statusCode == HttpURLConnection.HTTP_FORBIDDEN -> GitHubHtmlParseResult.AccessDenied("当前令牌无法访问$label。", sourceUrl, statusCode, preview)
        statusCode == HttpURLConnection.HTTP_NOT_FOUND -> GitHubHtmlParseResult.NotFound("$label 不存在，或当前令牌无法访问。", sourceUrl, statusCode, preview)
        statusCode !in 200..299 -> GitHubHtmlParseResult.ParseError("GitHub REST $label 返回 HTTP $statusCode：${errorDetail()}", sourceUrl, statusCode, preview)
        else -> null
    }
    private fun errorDetail(): String = runCatching { JSONObject(body).optString("message") }.getOrNull()?.takeIf { it.isNotBlank() } ?: preview
}

private fun JSONArray.toRulesetsStringList(): List<String> = (0 until length()).mapNotNull { optString(it).takeIf { value -> value.isNotBlank() } }
private fun String.toRulesetsPathSegment(): String = java.net.URLEncoder.encode(this, Charsets.UTF_8.name()).replace("+", "%20")
