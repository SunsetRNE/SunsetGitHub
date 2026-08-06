package com.Sunset.REN.GitHub.data.github

import android.util.Base64
import com.Sunset.REN.GitHub.data.github.network.GitHubHttpClient
import com.Sunset.REN.GitHub.data.github.network.GitHubHttpMethod
import com.Sunset.REN.GitHub.data.github.network.GitHubHttpRequest
import com.Sunset.REN.GitHub.data.github.network.GitHubHttpResponse
import com.Sunset.REN.GitHub.domain.sync.RemoteBlob
import com.Sunset.REN.GitHub.domain.sync.RemoteBranchHead
import com.Sunset.REN.GitHub.domain.sync.RemoteCommit
import com.Sunset.REN.GitHub.domain.sync.RemoteRepositoryCommitBackend
import com.Sunset.REN.GitHub.domain.sync.RemoteTree
import com.Sunset.REN.GitHub.domain.sync.RemoteTreeEntry
import com.Sunset.REN.GitHub.domain.sync.RemoteTreeEntryMode
import com.Sunset.REN.GitHub.domain.sync.RemoteTreeEntryType
import com.Sunset.REN.GitHub.domain.sync.RemoteTreeEntryWrite
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/**
 * GitHub Git Data API 实现。
 *
 * 该后端只负责远端 Git 对象读写，不直接理解应用工作区。
 * 工作区扫描、冲突策略、危险确认和文件读取由 WorkspaceSyncBackend 负责。
 */
class GitHubApiRemoteRepositoryCommitBackend(
    accessToken: String
) : RemoteRepositoryCommitBackend {

    private val httpClient = GitHubHttpClient(accessToken)

    override suspend fun getBranchHead(owner: String, repo: String, branch: String): RemoteBranchHead {
        val response = getJson(path = "/repos/$owner/$repo/git/ref/heads/${branch.toGitHubPathSegments()}")
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("获取分支 HEAD 失败"))
        }
        val json = JSONObject(response.body)
        val commitSha = json.optJSONObject("object")?.optString("sha").orEmpty()
        if (commitSha.isBlank()) {
            throw IllegalStateException("GitHub 未返回分支 $branch 的 commit sha。")
        }
        return RemoteBranchHead(
            branch = branch,
            commitSha = commitSha,
            ref = json.optionalString("ref") ?: "refs/heads/$branch"
        )
    }

    override suspend fun getCommit(owner: String, repo: String, commitSha: String): RemoteCommit {
        val response = getJson(path = "/repos/$owner/$repo/git/commits/${commitSha.toGitHubPathSegments()}")
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("获取 Git commit 失败"))
        }
        return JSONObject(response.body).toRemoteCommit()
    }

    override suspend fun getTree(
        owner: String,
        repo: String,
        treeSha: String,
        recursive: Boolean
    ): RemoteTree {
        val recursiveQuery = if (recursive) "?recursive=1" else ""
        val response = getJson(path = "/repos/$owner/$repo/git/trees/${treeSha.toGitHubPathSegments()}$recursiveQuery")
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("获取 Git tree 失败"))
        }
        return JSONObject(response.body).toRemoteTree()
    }

    override suspend fun createBlob(owner: String, repo: String, contentBytes: ByteArray): RemoteBlob {
        val body = JSONObject()
            .put("content", Base64.encodeToString(contentBytes, Base64.NO_WRAP))
            .put("encoding", "base64")
            .toString()
        val response = request(path = "/repos/$owner/$repo/git/blobs", method = GitHubHttpMethod.POST, body = body)
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("创建 Git blob 失败"))
        }
        val sha = JSONObject(response.body).optString("sha")
        if (sha.isBlank()) {
            throw IllegalStateException("GitHub 未返回新建 blob 的 sha。")
        }
        return RemoteBlob(sha = sha)
    }

    override suspend fun createTree(
        owner: String,
        repo: String,
        baseTreeSha: String?,
        entries: List<RemoteTreeEntryWrite>
    ): RemoteTree {
        val tree = JSONArray().also { array ->
            entries.forEach { entry ->
                array.put(entry.toJson())
            }
        }
        val body = JSONObject()
            .put("tree", tree)
            .apply {
                baseTreeSha?.takeIf { it.isNotBlank() }?.let { put("base_tree", it) }
            }
            .toString()
        val response = request(path = "/repos/$owner/$repo/git/trees", method = GitHubHttpMethod.POST, body = body)
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("创建 Git tree 失败"))
        }
        return JSONObject(response.body).toRemoteTree()
    }

    override suspend fun createCommit(
        owner: String,
        repo: String,
        message: String,
        treeSha: String,
        parentCommitSha: String
    ): RemoteCommit {
        val body = JSONObject()
            .put("message", message)
            .put("tree", treeSha)
            .put("parents", JSONArray().put(parentCommitSha))
            .toString()
        val response = request(path = "/repos/$owner/$repo/git/commits", method = GitHubHttpMethod.POST, body = body)
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("创建 Git commit 失败"))
        }
        return JSONObject(response.body).toRemoteCommit()
    }

    override suspend fun updateBranchHead(
        owner: String,
        repo: String,
        branch: String,
        commitSha: String,
        force: Boolean
    ) {
        val body = JSONObject()
            .put("sha", commitSha)
            .put("force", force)
            .toString()
        val response = request(
            path = "/repos/$owner/$repo/git/refs/heads/${branch.toGitHubPathSegments()}",
            method = GitHubHttpMethod.PATCH,
            body = body
        )
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("更新分支 HEAD 失败"))
        }
    }

    private fun getJson(path: String): GitHubHttpResponse {
        return request(path = path, method = GitHubHttpMethod.GET)
    }

    private fun request(path: String, method: GitHubHttpMethod, body: String? = null): GitHubHttpResponse {
        return httpClient.execute(
            GitHubHttpRequest(
                pathOrUrl = path,
                method = method,
                body = body
            )
        )
    }

    private fun JSONObject.toRemoteCommit(): RemoteCommit {
        val treeSha = optJSONObject("tree")?.optString("sha").orEmpty()
        if (treeSha.isBlank()) {
            throw IllegalStateException("GitHub commit 响应缺少 tree sha。")
        }
        val parents = optJSONArray("parents") ?: JSONArray()
        return RemoteCommit(
            sha = optString("sha"),
            treeSha = treeSha,
            parentShas = buildList {
                for (index in 0 until parents.length()) {
                    parents.optJSONObject(index)?.optString("sha")?.takeIf { it.isNotBlank() }?.let(::add)
                }
            },
            message = optionalString("message")
        )
    }

    private fun JSONObject.toRemoteTree(): RemoteTree {
        val entries = optJSONArray("tree") ?: JSONArray()
        return RemoteTree(
            sha = optString("sha"),
            entries = buildList {
                for (index in 0 until entries.length()) {
                    entries.optJSONObject(index)?.toRemoteTreeEntry()?.let(::add)
                }
            },
            truncated = optBoolean("truncated", false)
        )
    }

    private fun JSONObject.toRemoteTreeEntry(): RemoteTreeEntry? {
        val path = optionalString("path") ?: return null
        val mode = parseRemoteTreeEntryMode(optionalString("mode"))
        val type = parseRemoteTreeEntryType(optionalString("type"))
        return RemoteTreeEntry(
            path = path,
            mode = mode,
            type = type,
            sha = optionalString("sha"),
            sizeBytes = if (isNull("size")) null else optLong("size", 0L)
        )
    }

    private fun RemoteTreeEntryWrite.toJson(): JSONObject {
        return JSONObject()
            .put("path", path)
            .put("mode", mode.wireValue)
            .put("type", type.wireValue)
            .apply {
                if (content != null) {
                    put("content", content)
                } else {
                    put("sha", sha ?: JSONObject.NULL)
                }
            }
    }

    private fun JSONObject.optionalString(name: String): String? {
        return if (isNull(name)) null else optString(name).takeIf { it.isNotBlank() }
    }

    private fun String.toGitHubPathSegments(): String {
        return split('/')
            .filter { it.isNotBlank() }
            .joinToString("/") { segment ->
                URLEncoder.encode(segment, Charsets.UTF_8.name()).replace("+", "%20")
            }
    }

    // Shared network behavior is provided by GitHubHttpClient.
}

private fun parseRemoteTreeEntryMode(value: String?): RemoteTreeEntryMode {
    return RemoteTreeEntryMode.entries.firstOrNull { it.wireValue == value } ?: RemoteTreeEntryMode.File
}

private fun parseRemoteTreeEntryType(value: String?): RemoteTreeEntryType {
    return RemoteTreeEntryType.entries.firstOrNull { it.wireValue == value } ?: RemoteTreeEntryType.Blob
}