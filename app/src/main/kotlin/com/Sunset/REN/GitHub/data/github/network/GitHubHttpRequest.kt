package com.Sunset.REN.GitHub.data.github.network

/**
 * Transport-level GitHub HTTP request.
 *
 * [pathOrUrl] accepts either a REST API path such as `/repos/owner/repo`,
 * or an absolute URL for endpoints outside the default API host.
 */
data class GitHubHttpRequest(
    val pathOrUrl: String,
    val method: GitHubHttpMethod = GitHubHttpMethod.GET,
    val body: String? = null,
    val accept: String = GitHubApiHeaders.AcceptJson,
    val apiVersion: String? = GitHubApiHeaders.DefaultApiVersion,
    val requiresAuth: Boolean = true,
    val followRedirects: Boolean = false,
    val contentType: String? = GitHubApiHeaders.ContentTypeJson,
    val bodyBytes: ByteArray? = null,
    val responseBodyAsBytes: Boolean = false,
    val responseBodyMaxBytes: Int? = null,
    val isRetryable: Boolean = method == GitHubHttpMethod.GET && body == null && bodyBytes == null
)