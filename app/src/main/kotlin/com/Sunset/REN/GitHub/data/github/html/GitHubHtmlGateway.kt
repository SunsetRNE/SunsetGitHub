package com.Sunset.REN.GitHub.data.github.html

import com.Sunset.REN.GitHub.data.github.network.GitHubHttpClient
import com.Sunset.REN.GitHub.data.github.network.GitHubHttpRequest
import java.io.IOException
import java.net.SocketTimeoutException
class GitHubHtmlGateway(
    accessToken: String?
) {
    private val httpClient = GitHubHttpClient(accessToken.orEmpty(), timeoutMillis = TimeoutMillis)


    fun getRepositorySectionPage(owner: String, repo: String, sectionPath: String): GitHubHtmlPage {
        val encodedOwner = owner.toGitHubPathSegment()
        val encodedRepo = repo.toGitHubPathSegment()
        val normalizedPath = sectionPath.trim('/').takeIf { it.isNotBlank() }
        val url = buildString {
            append(GitHubWebBaseUrl)
            append('/')
            append(encodedOwner)
            append('/')
            append(encodedRepo)
            if (normalizedPath != null) {
                append('/')
                append(normalizedPath)
            }
        }
        return getPage(url)
    }

    fun getPage(url: String): GitHubHtmlPage {
        return try {
            val response = httpClient.execute(
                GitHubHttpRequest(
                    pathOrUrl = url,
                    accept = "text/html,application/xhtml+xml",
                    apiVersion = null,
                    followRedirects = true
                )
            )
            GitHubHtmlPage(url = url, statusCode = response.statusCode, html = response.body)
        } catch (error: SocketTimeoutException) {
            GitHubHtmlPage(
                url = url,
                statusCode = NetworkErrorStatusCode,
                html = "",
                errorMessage = "GitHub HTML 页面请求超时，请稍后重试。"
            )
        } catch (error: IOException) {
            GitHubHtmlPage(
                url = url,
                statusCode = NetworkErrorStatusCode,
                html = "",
                errorMessage = error.message?.takeIf { it.isNotBlank() } ?: "GitHub HTML 页面请求失败。"
            )
        }
    }

    companion object {
        private const val GitHubWebBaseUrl = "https://github.com"
        private const val TimeoutMillis = 15_000
        private const val NetworkErrorStatusCode = 0
    }
}

private fun String.toGitHubPathSegment(): String {
    return java.net.URLEncoder.encode(this, Charsets.UTF_8.name()).replace("+", "%20")
}