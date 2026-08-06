package com.Sunset.REN.GitHub.data.auth

import com.Sunset.REN.GitHub.core.network.GitHubEndpoints
import com.Sunset.REN.GitHub.domain.auth.TokenInspectionResult
import com.Sunset.REN.GitHub.domain.auth.TokenPermissionCapability
import com.Sunset.REN.GitHub.domain.auth.TokenPermissionCheck
import com.Sunset.REN.GitHub.domain.auth.TokenPermissionDetail
import com.Sunset.REN.GitHub.domain.auth.TokenPermissionStatus
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class TokenAuthRepository(
    private val accountGateway: GitHubDeviceFlowRepository = GitHubDeviceFlowRepository("")
) {

    suspend fun inspectToken(accessToken: String): TokenInspectionResult {
        val account = accountGateway.fetchCurrentAccount(accessToken)
        val scopes = fetchTokenScopes(accessToken)
        val probes = runApiProbes(accessToken)
        return TokenInspectionResult(
            account = account,
            scopes = scopes,
            checks = buildPermissionChecks(scopes, probes)
        )
    }

    private fun fetchTokenScopes(accessToken: String): List<String> {
        val connection = openGitHubConnection(accessToken, GitHubEndpoints.CurrentUserPath)
        return try {
            val code = connection.responseCode
            if (code !in 200..299) {
                val body = (connection.errorStream ?: connection.inputStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
                val message = runCatching { JSONObject(body).optString("message") }.getOrNull().orEmpty()
                throw IllegalStateException(message.ifBlank { "GitHub token 检查返回 HTTP $code" })
            }
            connection.getHeaderField("X-OAuth-Scopes")
                .orEmpty()
                .split(',')
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
        } finally {
            connection.disconnect()
        }
    }

    private fun runApiProbes(accessToken: String): Map<String, Boolean> {
        return mapOf(
            ProbeRepositories to probeEndpoint(accessToken, "/user/repos?per_page=1"),
            ProbeNotifications to probeEndpoint(accessToken, "/notifications?per_page=1"),
            ProbeSearchIssues to probeEndpoint(accessToken, "/search/issues?q=repo:octocat/Hello-World+is:issue&per_page=1")
        )
    }

    private fun probeEndpoint(accessToken: String, path: String): Boolean {
        val connection = openGitHubConnection(accessToken, path)
        return try {
            connection.responseCode in 200..299
        } catch (_: Exception) {
            false
        } finally {
            connection.disconnect()
        }
    }

    private fun openGitHubConnection(accessToken: String, path: String): HttpURLConnection {
        return (URL(GitHubEndpoints.ApiBaseUrl + path).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TimeoutMillis
            readTimeout = TimeoutMillis
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            setRequestProperty("User-Agent", "SunsetGitHub-Android")
        }
    }

    private fun buildPermissionChecks(
        scopes: List<String>,
        probes: Map<String, Boolean>
    ): List<TokenPermissionCheck> {
        val normalized = scopes.map { it.lowercase() }.toSet()
        return listOf(
            buildCheck(
                capability = TokenPermissionCapability.Repository,
                granted = "repo" in normalized || probes[ProbeRepositories] == true,
                alternative = normalized.any { it.startsWith("public_repo") || it.startsWith("contents") },
                missingDetail = TokenPermissionDetail.RepositoryMissing,
                isCritical = true
            ),
            buildCheck(
                capability = TokenPermissionCapability.Workflow,
                granted = "workflow" in normalized,
                alternative = normalized.any { it.contains("actions") || it == "repo" },
                missingDetail = TokenPermissionDetail.WorkflowMissing
            ),
            buildCheck(
                capability = TokenPermissionCapability.Issues,
                granted = "repo" in normalized || probes[ProbeSearchIssues] == true,
                alternative = normalized.any { it.contains("issues") || it.contains("pull_requests") },
                missingDetail = TokenPermissionDetail.IssuesMissing,
                isCritical = true
            ),
            buildCheck(
                capability = TokenPermissionCapability.Notifications,
                granted = probes[ProbeNotifications] == true,
                alternative = normalized.any { it.contains("notifications") || it == "repo" },
                missingDetail = TokenPermissionDetail.NotificationsMissing
            ),
            buildCheck(
                capability = TokenPermissionCapability.UserProfile,
                granted = normalized.any { it == "user" || it == "read:user" || it == "user:email" },
                alternative = true,
                missingDetail = TokenPermissionDetail.UserProfileMissing
            )
        )
    }

    private fun buildCheck(
        capability: TokenPermissionCapability,
        granted: Boolean,
        alternative: Boolean,
        missingDetail: TokenPermissionDetail,
        isCritical: Boolean = false
    ): TokenPermissionCheck {
        val status = when {
            granted -> TokenPermissionStatus.Granted
            alternative -> TokenPermissionStatus.Unknown
            else -> TokenPermissionStatus.Missing
        }
        val detail = when (status) {
            TokenPermissionStatus.Granted -> TokenPermissionDetail.Granted
            TokenPermissionStatus.Unknown -> TokenPermissionDetail.Unknown
            TokenPermissionStatus.Missing -> missingDetail
        }
        return TokenPermissionCheck(capability, status, detail, isCritical)
    }

    private companion object {
        const val TimeoutMillis = 15_000
        const val ProbeRepositories = "repositories"
        const val ProbeNotifications = "notifications"
        const val ProbeSearchIssues = "search_issues"
    }
}
