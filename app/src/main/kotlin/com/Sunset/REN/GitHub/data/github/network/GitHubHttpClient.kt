package com.Sunset.REN.GitHub.data.github.network

import com.Sunset.REN.GitHub.core.network.GitHubEndpoints
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Small shared transport client for GitHub HTTP requests.
 *
 * This keeps headers, timeout, body writing, response reading and GET retry
 * behavior in one place while existing gateways are migrated incrementally.
 */
class GitHubHttpClient(
    private val accessToken: String,
    private val timeoutMillis: Int = DefaultTimeoutMillis,
    private val retryPolicy: GitHubRetryPolicy = GitHubRetryPolicy.Default
) {

    fun execute(request: GitHubHttpRequest): GitHubHttpResponse {
        val maxAttempts = if (request.isRetryable) retryPolicy.maxAttempts else 1
        var lastError: Exception? = null
        for (attempt in 1..maxAttempts) {
            try {
                val response = executeOnce(request)
                if (
                    request.isRetryable &&
                    response.statusCode in retryPolicy.retryableStatusCodes &&
                    attempt < maxAttempts
                ) {
                    retryPolicy.sleeper(retryPolicy.backoffMillis(attempt))
                    continue
                }
                return response
            } catch (exception: Exception) {
                lastError = exception
                if (!request.isRetryable || attempt >= maxAttempts) {
                    throw exception
                }
                retryPolicy.sleeper(retryPolicy.backoffMillis(attempt))
            }
        }
        throw lastError ?: IllegalStateException("GitHub request failed: ${request.pathOrUrl}")
    }

    private fun executeOnce(request: GitHubHttpRequest): GitHubHttpResponse {
        val url = request.pathOrUrl.toGitHubUrl()
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = request.method.name
            connectTimeout = timeoutMillis
            readTimeout = timeoutMillis
            instanceFollowRedirects = request.followRedirects
            setRequestProperty("Accept", request.accept)
            setRequestProperty("User-Agent", GitHubApiHeaders.UserAgent)
            request.apiVersion?.takeIf { it.isNotBlank() }?.let { apiVersion ->
                setRequestProperty("X-GitHub-Api-Version", apiVersion)
            }
            if (request.requiresAuth && accessToken.isNotBlank()) {
                setRequestProperty("Authorization", "Bearer $accessToken")
            }
            request.bodyBytes?.let { bytes ->
                doOutput = true
                request.contentType?.takeIf { it.isNotBlank() }?.let { contentType ->
                    setRequestProperty("Content-Type", contentType)
                }
                setRequestProperty("Content-Length", bytes.size.toString())
                outputStream.use { stream -> stream.write(bytes) }
            } ?: request.body?.let { body ->
                doOutput = true
                request.contentType?.takeIf { it.isNotBlank() }?.let { contentType ->
                    setRequestProperty("Content-Type", contentType)
                }
                outputStream.use { stream -> stream.write(body.toByteArray(Charsets.UTF_8)) }
            }
        }
        return connection.toGitHubHttpResponse(url, request.responseBodyAsBytes, request.responseBodyMaxBytes)
    }

    private fun String.toGitHubUrl(): String {
        return if (startsWith("http://") || startsWith("https://")) this else GitHubEndpoints.ApiBaseUrl + this
    }

    private fun HttpURLConnection.toGitHubHttpResponse(
        sourceUrl: String,
        responseBodyAsBytes: Boolean,
        responseBodyMaxBytes: Int?
    ): GitHubHttpResponse {
        return try {
            val code = responseCode
            val stream = if (code in 200..299) inputStream else errorStream ?: inputStream
            if (responseBodyAsBytes) {
                val bytes = stream.use { input ->
                    val maxBytes = responseBodyMaxBytes ?: Int.MAX_VALUE
                    val buffer = ByteArray(8 * 1024)
                    val output = ByteArrayOutputStream()
                    while (output.size() < maxBytes) {
                        val bytesRead = input.read(buffer, 0, minOf(buffer.size, maxBytes - output.size()))
                        if (bytesRead <= 0) break
                        output.write(buffer, 0, bytesRead)
                    }
                    output.toByteArray()
                }
                GitHubHttpResponse(
                    statusCode = code,
                    body = String(bytes, Charsets.UTF_8),
                    bodyBytes = bytes,
                    headers = headerFields.orEmpty().filterKeys { key -> key != null }.mapKeys { it.key.orEmpty() },
                    sourceUrl = sourceUrl
                )
            } else {
                val body = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader -> reader.readText() }
                GitHubHttpResponse(
                    statusCode = code,
                    body = body,
                    headers = headerFields.orEmpty().filterKeys { key -> key != null }.mapKeys { it.key.orEmpty() },
                    sourceUrl = sourceUrl
                )
            }
        } finally {
            disconnect()
        }
    }

    companion object {
        const val DefaultTimeoutMillis = 20_000
    }
}