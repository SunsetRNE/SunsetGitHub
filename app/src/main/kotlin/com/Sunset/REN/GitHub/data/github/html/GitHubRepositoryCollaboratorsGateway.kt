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

class GitHubRepositoryCollaboratorsGateway(accessToken: String) {
    private val httpClient = GitHubHttpClient(accessToken, timeoutMillis = TimeoutMillis)

    fun loadCollaborators(owner: String, repo: String): GitHubHtmlParseResult<RepositoryCollaboratorsSnapshot> {
        val sourceUrl = webAccessUrl(owner, repo)
        val repoResponse = try { request("GET", repoApiUrl(owner, repo)) } catch (error: SocketTimeoutException) {
            return GitHubHtmlParseResult.ParseError("GitHub REST 访问权限请求超时。", sourceUrl, NetworkErrorStatusCode)
        } catch (error: IOException) {
            return GitHubHtmlParseResult.ParseError(error.message?.takeIf { it.isNotBlank() } ?: "GitHub REST 访问权限请求失败。", sourceUrl, NetworkErrorStatusCode)
        }
        repoResponse.toParseFailureOrNull(sourceUrl, "仓库访问权限")?.let { return it }
        val repoJson = runCatching { JSONObject(repoResponse.body) }.getOrElse {
            return GitHubHtmlParseResult.ParseError("GitHub REST 仓库访问权限返回内容不是有效 JSON。", sourceUrl, repoResponse.statusCode, repoResponse.preview)
        }
        val permissions = repoJson.optJSONObject("permissions")
        val canAdmin = permissions?.optBoolean("admin", false) == true
        val canPush = permissions?.optBoolean("push", false) == true

        val collaboratorResponses = try {
            requestPaged("GET", "${repoApiUrl(owner, repo)}/collaborators?affiliation=direct&per_page=$PageSize")
        } catch (error: SocketTimeoutException) {
            return GitHubHtmlParseResult.ParseError("GitHub REST 协作者列表请求超时。", sourceUrl, NetworkErrorStatusCode)
        } catch (error: IOException) {
            return GitHubHtmlParseResult.ParseError(error.message?.takeIf { it.isNotBlank() } ?: "GitHub REST 协作者列表请求失败。", sourceUrl, NetworkErrorStatusCode)
        }
        val firstResponse = collaboratorResponses.firstOrNull() ?: return GitHubHtmlParseResult.ParseError("GitHub REST 协作者列表没有返回响应。", sourceUrl, NetworkErrorStatusCode)
        firstResponse.toParseFailureOrNull(sourceUrl, "协作者列表")?.let { return it }
        val collaborators = collaboratorResponses.flatMap { response ->
            val array = runCatching { JSONArray(response.body) }.getOrElse {
                return GitHubHtmlParseResult.ParseError("GitHub REST 协作者列表返回内容不是有效 JSON。", sourceUrl, response.statusCode, response.preview)
            }
            (0 until array.length()).mapNotNull { array.optJSONObject(it)?.toCollaboratorItem() }
        }
        val invitations = if (canAdmin) {
            when (val result = loadPendingInvitations(owner, repo, sourceUrl)) {
                is GitHubHtmlParseResult.Success -> result.value
                is GitHubHtmlParseResult.AccessDenied -> return result
                is GitHubHtmlParseResult.NotFound -> return result
                is GitHubHtmlParseResult.ParseError -> return result
            }
        } else {
            emptyList()
        }
        return GitHubHtmlParseResult.Success(
            RepositoryCollaboratorsSnapshot(owner, repo, canAdmin, canPush, collaborators, invitations, sourceUrl)
        )
    }

    fun inviteCollaborator(owner: String, repo: String, username: String, permission: RepositoryCollaboratorPermission): GitHubHtmlParseResult<RepositoryCollaboratorMutationResult> {
        val sourceUrl = webAccessUrl(owner, repo)
        val body = JSONObject().put("permission", permission.apiValue).toString()
        val response = try { request("PUT", "${repoApiUrl(owner, repo)}/collaborators/${username.toCollaboratorsPathSegment()}", body) } catch (error: SocketTimeoutException) {
            return GitHubHtmlParseResult.ParseError("GitHub REST 添加协作者请求超时。", sourceUrl, NetworkErrorStatusCode)
        } catch (error: IOException) {
            return GitHubHtmlParseResult.ParseError(error.message?.takeIf { it.isNotBlank() } ?: "GitHub REST 添加协作者请求失败。", sourceUrl, NetworkErrorStatusCode)
        }
        if (response.statusCode !in 200..299) return GitHubHtmlParseResult.ParseError(response.toUpdateErrorMessage("添加协作者失败"), sourceUrl, response.statusCode, response.preview)
        return GitHubHtmlParseResult.Success(RepositoryCollaboratorMutationResult(username, permission, response.statusCode == HttpURLConnection.HTTP_CREATED, response.statusCode))
    }

    fun removeCollaborator(owner: String, repo: String, username: String): GitHubHtmlParseResult<Unit> {
        val sourceUrl = webAccessUrl(owner, repo)
        val response = try { request("DELETE", "${repoApiUrl(owner, repo)}/collaborators/${username.toCollaboratorsPathSegment()}") } catch (error: SocketTimeoutException) {
            return GitHubHtmlParseResult.ParseError("GitHub REST 移除协作者请求超时。", sourceUrl, NetworkErrorStatusCode)
        } catch (error: IOException) {
            return GitHubHtmlParseResult.ParseError(error.message?.takeIf { it.isNotBlank() } ?: "GitHub REST 移除协作者请求失败。", sourceUrl, NetworkErrorStatusCode)
        }
        if (response.statusCode !in 200..299) return GitHubHtmlParseResult.ParseError(response.toUpdateErrorMessage("移除协作者失败"), sourceUrl, response.statusCode, response.preview)
        return GitHubHtmlParseResult.Success(Unit)
    }

fun cancelInvitation(owner: String, repo: String, invitationId: Long): GitHubHtmlParseResult<Unit> {
        val sourceUrl = webAccessUrl(owner, repo)
        val response = try { request("DELETE", "${repoApiUrl(owner, repo)}/invitations/$invitationId") } catch (error: SocketTimeoutException) {
            return GitHubHtmlParseResult.ParseError("GitHub REST 取消协作者邀请请求超时。", sourceUrl, NetworkErrorStatusCode)
        } catch (error: IOException) {
            return GitHubHtmlParseResult.ParseError(error.message?.takeIf { it.isNotBlank() } ?: "GitHub REST 取消协作者邀请请求失败。", sourceUrl, NetworkErrorStatusCode)
        }
        if (response.statusCode !in 200..299) return GitHubHtmlParseResult.ParseError(response.toUpdateErrorMessage("取消协作者邀请失败"), sourceUrl, response.statusCode, response.preview)
        return GitHubHtmlParseResult.Success(Unit)
    }

    private fun loadPendingInvitations(owner: String, repo: String, sourceUrl: String): GitHubHtmlParseResult<List<RepositoryCollaboratorInvitationItem>> {
        val responses = try { requestPaged("GET", "${repoApiUrl(owner, repo)}/invitations?per_page=$PageSize") } catch (error: SocketTimeoutException) {
            return GitHubHtmlParseResult.ParseError("GitHub REST 待处理邀请列表请求超时。", sourceUrl, NetworkErrorStatusCode)
        } catch (error: IOException) {
            return GitHubHtmlParseResult.ParseError(error.message?.takeIf { it.isNotBlank() } ?: "GitHub REST 待处理邀请列表请求失败。", sourceUrl, NetworkErrorStatusCode)
        }
        val first = responses.firstOrNull() ?: return GitHubHtmlParseResult.Success(emptyList())
        first.toParseFailureOrNull(sourceUrl, "待处理邀请列表")?.let { return it }
        val invitations = responses.flatMap { response ->
            val array = runCatching { JSONArray(response.body) }.getOrElse {
                return GitHubHtmlParseResult.ParseError("GitHub REST 待处理邀请列表返回内容不是有效 JSON。", sourceUrl, response.statusCode, response.preview)
            }
            (0 until array.length()).mapNotNull { array.optJSONObject(it)?.toInvitationItem() }
        }
        return GitHubHtmlParseResult.Success(invitations)
    }

    private fun requestPaged(method: String, firstUrl: String): List<CollaboratorsNetworkResponse> {
        val responses = mutableListOf<CollaboratorsNetworkResponse>()
        var nextUrl: String? = firstUrl
        var pageCount = 0
        while (!nextUrl.isNullOrBlank() && pageCount < MaxPages) {
            val response = request(method, nextUrl)
            responses += response
            if (response.statusCode !in 200..299) break
            nextUrl = response.nextUrl
            pageCount += 1
        }
        return responses
    }
    private fun request(method: String, url: String, body: String? = null): CollaboratorsNetworkResponse {
        return httpClient.execute(
            GitHubHttpRequest(
                pathOrUrl = url,
                method = method.toGitHubHttpMethod(),
                body = body,
                apiVersion = GitHubApiHeaders.LegacyApiVersion,
                followRedirects = true
            )
        ).toCollaboratorsNetworkResponse()
    }


    private fun CollaboratorsNetworkResponse.toParseFailureOrNull(sourceUrl: String, label: String): GitHubHtmlParseResult<Nothing>? = when {
        statusCode == HttpURLConnection.HTTP_FORBIDDEN -> GitHubHtmlParseResult.AccessDenied("当前令牌无法读取$label。", sourceUrl, statusCode, preview)
        statusCode == HttpURLConnection.HTTP_NOT_FOUND -> GitHubHtmlParseResult.NotFound("${label}不存在，或当前令牌无法访问。", sourceUrl, statusCode, preview)
        statusCode !in 200..299 -> GitHubHtmlParseResult.ParseError("GitHub REST $label 返回 HTTP $statusCode。", sourceUrl, statusCode, preview)
        else -> null
    }

    private fun repoApiUrl(owner: String, repo: String) = "https://api.github.com/repos/${owner.toCollaboratorsPathSegment()}/${repo.toCollaboratorsPathSegment()}"
    private fun webAccessUrl(owner: String, repo: String) = "https://github.com/${owner.toCollaboratorsPathSegment()}/${repo.toCollaboratorsPathSegment()}/settings/access"

    private fun GitHubHttpResponse.toCollaboratorsNetworkResponse(): CollaboratorsNetworkResponse {
        return CollaboratorsNetworkResponse(
            statusCode = statusCode,
            body = body,
            linkHeader = headers["Link"]?.firstOrNull()
        )
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

    private data class CollaboratorsNetworkResponse(val statusCode: Int, val body: String, val linkHeader: String? = null) {
        val preview: String get() = body.take(240).replace(Regex("\\s+"), " ").trim()
        val nextUrl: String? get() = linkHeader?.extractCollaboratorsNextUrl()
        fun toUpdateErrorMessage(prefix: String): String {
            val json = runCatching { JSONObject(body) }.getOrNull()
            val message = json?.optionalCollaboratorsString("message")
            val errors = json?.optJSONArray("errors")
            val detail = buildList {
                if (!message.isNullOrBlank()) add(message)
                if (errors != null) for (index in 0 until errors.length()) errors.opt(index)?.toString()?.takeIf { it.isNotBlank() }?.let { add(it) }
            }.joinToString("；")
            return if (detail.isNotBlank()) "$prefix：$detail" else "$prefix：HTTP $statusCode"
        }
    }

    private companion object { const val TimeoutMillis = 15_000; const val NetworkErrorStatusCode = 0; const val PageSize = 100; const val MaxPages = 5 }
}

data class RepositoryCollaboratorsSnapshot(val owner: String, val repo: String, val canAdmin: Boolean, val canPush: Boolean, val collaborators: List<RepositoryCollaboratorItem>, val invitations: List<RepositoryCollaboratorInvitationItem>, val sourceUrl: String) {
    val adminCount: Int get() = collaborators.count { it.permission == RepositoryCollaboratorPermission.Admin }
    val writeLikeCount: Int get() = collaborators.count { it.permission in setOf(RepositoryCollaboratorPermission.Admin, RepositoryCollaboratorPermission.Maintain, RepositoryCollaboratorPermission.Push) }
}

data class RepositoryCollaboratorItem(val login: String, val avatarUrl: String, val htmlUrl: String, val permission: RepositoryCollaboratorPermission, val permissions: Map<String, Boolean>)
data class RepositoryCollaboratorInvitationItem(val id: Long, val login: String, val email: String, val permission: RepositoryCollaboratorPermission, val createdAt: String, val url: String)
data class RepositoryCollaboratorMutationResult(val username: String, val permission: RepositoryCollaboratorPermission, val invitationCreated: Boolean, val statusCode: Int)

enum class RepositoryCollaboratorPermission(val apiValue: String, val displayName: String) { Pull("pull", "读取"), Triage("triage", "分类"), Push("push", "写入"), Maintain("maintain", "维护"), Admin("admin", "管理员") }

private fun JSONObject.toInvitationItem(): RepositoryCollaboratorInvitationItem {
    val invitee = optJSONObject("invitee")
    return RepositoryCollaboratorInvitationItem(
        id = optLong("id", 0L),
        login = invitee?.optionalCollaboratorsString("login").orEmpty(),
        email = optionalCollaboratorsString("email").orEmpty(),
        permission = (optionalCollaboratorsString("permissions") ?: optionalCollaboratorsString("permission") ?: "pull").toCollaboratorPermission(),
        createdAt = optionalCollaboratorsString("created_at").orEmpty(),
        url = optionalCollaboratorsString("html_url") ?: optionalCollaboratorsString("url").orEmpty()
    )
}

private fun JSONObject.toCollaboratorItem(): RepositoryCollaboratorItem {
    val permissionsObject = optJSONObject("permissions")
    val permission = optionalCollaboratorsString("role_name")?.toCollaboratorPermission()
        ?: permissionsObject.toCollaboratorPermission()
    return RepositoryCollaboratorItem(
        login = optionalCollaboratorsString("login").orEmpty(),
        avatarUrl = optionalCollaboratorsString("avatar_url").orEmpty(),
        htmlUrl = optionalCollaboratorsString("html_url").orEmpty(),
        permission = permission,
        permissions = mapOf(
            "pull" to (permissionsObject?.optBoolean("pull", false) == true),
            "triage" to (permissionsObject?.optBoolean("triage", false) == true),
            "push" to (permissionsObject?.optBoolean("push", false) == true),
            "maintain" to (permissionsObject?.optBoolean("maintain", false) == true),
            "admin" to (permissionsObject?.optBoolean("admin", false) == true)
        )
    )
}

private fun JSONObject?.toCollaboratorPermission(): RepositoryCollaboratorPermission = when {
    this?.optBoolean("admin", false) == true -> RepositoryCollaboratorPermission.Admin
    this?.optBoolean("maintain", false) == true -> RepositoryCollaboratorPermission.Maintain
    this?.optBoolean("push", false) == true -> RepositoryCollaboratorPermission.Push
    this?.optBoolean("triage", false) == true -> RepositoryCollaboratorPermission.Triage
    else -> RepositoryCollaboratorPermission.Pull
}

private fun String.toCollaboratorPermission(): RepositoryCollaboratorPermission = when (lowercase()) {
    "admin" -> RepositoryCollaboratorPermission.Admin
    "maintain" -> RepositoryCollaboratorPermission.Maintain
    "write", "push" -> RepositoryCollaboratorPermission.Push
    "triage" -> RepositoryCollaboratorPermission.Triage
    else -> RepositoryCollaboratorPermission.Pull
}

private fun JSONObject.optionalCollaboratorsString(name: String): String? { if (isNull(name)) return null; return optString(name).takeIf { it.isNotBlank() && it != "null" } }
private fun String.extractCollaboratorsNextUrl(): String? = split(',').firstOrNull { it.contains("rel=\"next\"") }?.substringAfter('<', "")?.substringBefore('>', "")?.takeIf { it.isNotBlank() }
private fun String.toCollaboratorsPathSegment(): String = java.net.URLEncoder.encode(this, Charsets.UTF_8.name()).replace("+", "%20")