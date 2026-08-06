package com.Sunset.REN.GitHub.data.auth

import com.Sunset.REN.GitHub.core.network.GitHubEndpoints
import com.Sunset.REN.GitHub.domain.auth.DeviceCodeGrant
import com.Sunset.REN.GitHub.domain.auth.DeviceFlowRepository
import com.Sunset.REN.GitHub.domain.auth.DeviceTokenPollResult
import com.Sunset.REN.GitHub.domain.auth.GitHubAccount
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * GitHub Device Flow 的最小真实网络实现。
 *
 * V0.1 早期为了降低构建风险，先使用 Android/Java 标准库的 HttpURLConnection，
 * 不引入 Retrofit/OkHttp 等额外依赖。后续若网络层稳定后，可迁移到统一 API Client。
 *
 * 注意：Device Flow 只需要 OAuth App 的 client_id，不得在 APK 内置 client_secret。
 */
class GitHubDeviceFlowRepository(
    private val clientId: String
) : DeviceFlowRepository {

    override suspend fun requestDeviceCode(): DeviceCodeGrant {
        val response = postForm(
            url = GitHubEndpoints.LoginBaseUrl + GitHubEndpoints.DeviceCodePath,
            form = mapOf(
                "client_id" to clientId,
                "scope" to RequestedScopes
            )
        )
        val json = JSONObject(response.body)
        if (!response.isSuccessful) {
            throw IllegalStateException(
                json.optString("error_description", "Request device code failed: HTTP ${response.statusCode}")
            )
        }
        return DeviceCodeGrant(
            deviceCode = json.getString("device_code"),
            userCode = json.getString("user_code"),
            verificationUri = json.getString("verification_uri"),
            verificationUriComplete = json.optString("verification_uri_complete").takeIf { it.isNotBlank() },
            expiresInSeconds = json.getLong("expires_in"),
            intervalSeconds = json.optLong("interval", 5L)
        )
    }

    override suspend fun pollAccessToken(deviceCode: String): DeviceTokenPollResult {
        return try {
            val response = postForm(
                url = GitHubEndpoints.LoginBaseUrl + GitHubEndpoints.AccessTokenPath,
                form = mapOf(
                    "client_id" to clientId,
                    "device_code" to deviceCode,
                    "grant_type" to "urn:ietf:params:oauth:grant-type:device_code"
                )
            )
            val json = JSONObject(response.body)
            val accessToken = json.optString("access_token", "")
            if (response.isSuccessful && accessToken.isNotBlank()) {
                DeviceTokenPollResult.Success(
                    accessToken = accessToken,
                    tokenType = json.optString("token_type", "bearer"),
                    scope = json.optString("scope", "")
                )
            } else {
                when (val error = json.optString("error", "")) {
                    "authorization_pending" -> DeviceTokenPollResult.AuthorizationPending
                    "slow_down" -> DeviceTokenPollResult.SlowDown
                    "expired_token" -> DeviceTokenPollResult.ExpiredToken
                    "access_denied" -> DeviceTokenPollResult.AccessDenied
                    else -> DeviceTokenPollResult.UnknownError(
                        code = error.ifBlank { null },
                        description = json.optString("error_description", "").ifBlank { null }
                    )
                }
            }
        } catch (exception: Exception) {
            DeviceTokenPollResult.NetworkError(exception.message)
        }
    }

    override suspend fun fetchCurrentAccount(accessToken: String): GitHubAccount {
        val response = getJson(
            url = GitHubEndpoints.ApiBaseUrl + GitHubEndpoints.CurrentUserPath,
            accessToken = accessToken
        )
        val json = JSONObject(response.body)
        if (!response.isSuccessful) {
            throw IllegalStateException(json.optString("message", "Fetch current account failed"))
        }
        return GitHubAccount(
            id = json.getLong("id"),
            login = json.getString("login"),
            avatarUrl = json.optString("avatar_url").takeIf { it.isNotBlank() },
            name = json.optString("name").takeIf { it.isNotBlank() }
        )
    }

    private fun postForm(url: String, form: Map<String, String>): NetworkResponse {
        val encodedBody = form.entries.joinToString("&") { (key, value) ->
            "${key.urlEncode()}=${value.urlEncode()}"
        }
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = TimeoutMillis
            readTimeout = TimeoutMillis
            doOutput = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            setRequestProperty("User-Agent", "SunsetGitHub-Android")
        }
        OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(encodedBody)
        }
        return connection.toNetworkResponse()
    }

    private fun getJson(url: String, accessToken: String): NetworkResponse {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TimeoutMillis
            readTimeout = TimeoutMillis
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            setRequestProperty("User-Agent", "SunsetGitHub-Android")
        }
        return connection.toNetworkResponse()
    }

    private fun HttpURLConnection.toNetworkResponse(): NetworkResponse {
        return try {
            val code = responseCode
            val stream = if (code in 200..299) inputStream else errorStream ?: inputStream
            val body = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                reader.readText()
            }
            NetworkResponse(
                statusCode = code,
                body = body
            )
        } finally {
            disconnect()
        }
    }

    private fun String.urlEncode(): String = URLEncoder.encode(this, "UTF-8")

    private data class NetworkResponse(
        val statusCode: Int,
        val body: String
    ) {
        val isSuccessful: Boolean = statusCode in 200..299
    }

    private companion object {
        const val TimeoutMillis = 15_000

        /**
         * Classic OAuth App scopes requested by Device Flow.
         *
         * Keep this list centralized so GitHub login permissions stay aligned with
         * repository browsing, file editing, workflow, issue, notification, profile,
         * organization, project, package, gist, hook, security, and admin features.
         */
        val RequestedScopes: String = listOf(
            "repo",
            "workflow",
            "delete_repo",
            "notifications",
            "user",
            "read:user",
            "user:email",
            "user:follow",
            "read:org",
            "write:org",
            "admin:org",
            "admin:public_key",
            "admin:repo_hook",
            "admin:org_hook",
            "admin:gpg_key",
            "admin:ssh_signing_key",
            "gist",
            "project",
            "read:packages",
            "write:packages",
            "delete:packages",
            "security_events",
            "read:discussion",
            "write:discussion",
            "codespace",
            "read:enterprise",
            "manage_billing:enterprise",
            "admin:enterprise",
            "manage_billing:copilot"
        ).joinToString(" ")
    }
}