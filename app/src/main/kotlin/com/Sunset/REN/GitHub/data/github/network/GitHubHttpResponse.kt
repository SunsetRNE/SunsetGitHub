package com.Sunset.REN.GitHub.data.github.network

import org.json.JSONObject

/** Raw GitHub HTTP response shared by REST and HTML-backed gateways. */
data class GitHubHttpResponse(
    val statusCode: Int,
    val body: String,
    val bodyBytes: ByteArray? = null,
    val headers: Map<String, List<String>> = emptyMap(),
    val sourceUrl: String = ""
) {
    val isSuccessful: Boolean = statusCode in 200..299

    fun toGitHubErrorMessage(fallback: String): String {
        val parsedMessage = runCatching {
            val json = JSONObject(body)
            buildString {
                append(json.optString("message", fallback))
                json.optString("documentation_url").takeIf { it.isNotBlank() }?.let { documentationUrl ->
                    append("\n")
                    append(documentationUrl)
                }
            }
        }.getOrElse {
            body.takeIf { it.isNotBlank() } ?: fallback
        }
        return "HTTP $statusCode: $parsedMessage"
    }
}