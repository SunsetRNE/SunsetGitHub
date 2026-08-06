package com.Sunset.REN.GitHub.data.github

import android.util.Base64
import com.Sunset.REN.GitHub.core.network.GitHubEndpoints
import com.Sunset.REN.GitHub.data.github.network.GitHubApiHeaders
import com.Sunset.REN.GitHub.data.github.network.GitHubHttpClient
import com.Sunset.REN.GitHub.data.github.network.GitHubHttpMethod
import com.Sunset.REN.GitHub.data.github.network.GitHubHttpRequest
import com.Sunset.REN.GitHub.data.github.network.GitHubHttpResponse
import com.Sunset.REN.GitHub.domain.notification.GitHubNotification
import com.Sunset.REN.GitHub.domain.profile.GitHubContributionCalendar
import com.Sunset.REN.GitHub.domain.profile.GitHubContributionDay
import com.Sunset.REN.GitHub.domain.profile.GitHubContributionMonth
import com.Sunset.REN.GitHub.domain.profile.GitHubContributionOverview
import com.Sunset.REN.GitHub.domain.profile.GitHubContributionWeek
import com.Sunset.REN.GitHub.domain.profile.GitHubUserProfile

import com.Sunset.REN.GitHub.domain.repo.EditableRepositoryFile
import com.Sunset.REN.GitHub.domain.repo.FileContentWriteResult
import com.Sunset.REN.GitHub.domain.repo.GitHubRepository
import com.Sunset.REN.GitHub.domain.repo.GitHubRepositoryGateway
import com.Sunset.REN.GitHub.domain.repo.CodeSearchPage
import com.Sunset.REN.GitHub.domain.repo.GitHubCodeSearchItem
import com.Sunset.REN.GitHub.domain.repo.GitHubIssueSearchItem
import com.Sunset.REN.GitHub.domain.repo.GitHubUserSearchItem
import com.Sunset.REN.GitHub.domain.repo.IssueSearchPage
import com.Sunset.REN.GitHub.domain.repo.RepositoryActionArtifact
import com.Sunset.REN.GitHub.domain.repo.RepositoryActionRun
import com.Sunset.REN.GitHub.domain.repo.RepositoryActionRunDetail
import com.Sunset.REN.GitHub.domain.repo.RepositoryActionWorkflow
import com.Sunset.REN.GitHub.domain.repo.RepositoryActionWorkflowInput
import com.Sunset.REN.GitHub.domain.repo.RepositoryBranch
import com.Sunset.REN.GitHub.domain.repo.RepositoryContentItem
import com.Sunset.REN.GitHub.domain.repo.RepositoryCreateRequest
import com.Sunset.REN.GitHub.domain.repo.RepositoryContributor
import com.Sunset.REN.GitHub.domain.repo.RepositoryFilePreview
import com.Sunset.REN.GitHub.domain.repo.RepositoryFileWriteTarget
import com.Sunset.REN.GitHub.domain.repo.RepositoryForkSyncStatus
import com.Sunset.REN.GitHub.domain.repo.RepositoryIssue
import com.Sunset.REN.GitHub.domain.repo.RepositoryIssueComment
import com.Sunset.REN.GitHub.domain.repo.RepositoryIssueDetail
import com.Sunset.REN.GitHub.domain.repo.RepositoryIssueLabel
import com.Sunset.REN.GitHub.domain.repo.RepositoryLabel
import com.Sunset.REN.GitHub.domain.repo.RepositoryLanguage
import com.Sunset.REN.GitHub.domain.repo.RepositoryPermissions
import com.Sunset.REN.GitHub.domain.repo.RepositoryPullRequest
import com.Sunset.REN.GitHub.domain.repo.RepositoryRelease
import com.Sunset.REN.GitHub.domain.repo.RepositoryReleaseAsset
import com.Sunset.REN.GitHub.domain.repo.RepositorySearchPage
import com.Sunset.REN.GitHub.domain.repo.RepositorySecurityAlert
import com.Sunset.REN.GitHub.domain.repo.RepositorySecurityAlertDetailGroup
import com.Sunset.REN.GitHub.domain.repo.RepositorySecurityProbe
import com.Sunset.REN.GitHub.domain.repo.RepositorySecurityProbeStatus
import com.Sunset.REN.GitHub.domain.repo.RepositorySecuritySummary
import com.Sunset.REN.GitHub.domain.repo.RepositorySidebarInfo
import com.Sunset.REN.GitHub.domain.repo.UserSearchPage
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.util.concurrent.ConcurrentHashMap

class GitHubRepositoryFeatureUnavailableException(
    val feature: String,
    val statusCode: Int,
    override val message: String
) : IllegalStateException(message)

class GitHubRepositoryApiGateway(
    private val accessToken: String
) : GitHubRepositoryGateway {

    private val httpClient = GitHubHttpClient(accessToken)

    fun getCurrentUserProfile(): GitHubUserProfile {
        val response = getJson(path = "/user")
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("获取个人页面失败"))
        }
        return JSONObject(response.body).toUserProfile()
    }

    fun getUserProfile(login: String): GitHubUserProfile {
        val encodedLogin = URLEncoder.encode(login, "UTF-8")
        val response = getJson(path = "/users/$encodedLogin")
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("获取用户页面失败"))
        }
        return JSONObject(response.body).toUserProfile()
    }

    fun getContributionCalendar(login: String, year: Int? = null): GitHubContributionCalendar {
        val variables = JSONObject().put("login", login)
        if (year != null) {
            variables
                .put("from", "%04d-01-01T00:00:00Z".format(year))
                .put("to", "%04d-12-31T23:59:59Z".format(year))
        } else {
            variables.put("from", JSONObject.NULL).put("to", JSONObject.NULL)
        }
        val response = requestGraphQl(
            query = """
                query(${'$'}login: String!, ${'$'}from: DateTime, ${'$'}to: DateTime) {
                  user(login: ${'$'}login) {
                    contributionsCollection(from: ${'$'}from, to: ${'$'}to) {
                      totalCommitContributions
                      totalIssueContributions
                      totalPullRequestContributions
                      totalPullRequestReviewContributions
                      restrictedContributionsCount
                      commitContributionsByRepository(maxRepositories: 8) {
                        repository { nameWithOwner }
                      }
                      issueContributionsByRepository(maxRepositories: 8) {
                        repository { nameWithOwner }
                      }
                      pullRequestContributionsByRepository(maxRepositories: 8) {
                        repository { nameWithOwner }
                      }
                      pullRequestReviewContributionsByRepository(maxRepositories: 8) {
                        repository { nameWithOwner }
                      }
                      contributionCalendar {
                        totalContributions
                        weeks {
                          firstDay
                          contributionDays {
                            date
                            weekday
                            contributionCount
                            color
                          }
                        }
                        months {
                          name
                          year
                          firstDay
                          totalWeeks
                        }
                      }
                    }
                  }
                }
            """.trimIndent(),
            variables = variables
        )
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("获取贡献墙失败"))
        }
        val root = JSONObject(response.body)
        root.optJSONArray("errors")?.takeIf { it.length() > 0 }?.let { errors ->
            val message = errors.optJSONObject(0)?.optString("message")?.takeIf { it.isNotBlank() }
                ?: "GraphQL 返回错误"
            throw IllegalStateException("获取贡献墙失败：$message")
        }
        val collection = root
            .optJSONObject("data")
            ?.optJSONObject("user")
            ?.optJSONObject("contributionsCollection")
            ?: return GitHubContributionCalendar(totalContributions = 0, weeks = emptyList(), months = emptyList())
        val calendar = collection.optJSONObject("contributionCalendar")
            ?: return GitHubContributionCalendar(totalContributions = 0, weeks = emptyList(), months = emptyList())
        return calendar.toContributionCalendar(collection.toContributionOverview())
    }

    suspend fun listUserRepositories(login: String, page: Int, perPage: Int): List<GitHubRepository> {
        val encodedLogin = URLEncoder.encode(login, "UTF-8")
        val response = getJson(
            path = "/users/$encodedLogin/repos?type=owner&sort=updated&direction=desc&page=$page&per_page=$perPage"
        )
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("获取用户仓库列表失败"))
        }
        val array = JSONArray(response.body)
        return buildList {
            for (index in 0 until array.length()) {
                add(array.getJSONObject(index).toRepository())
            }
        }
    }
    override suspend fun listCurrentUserRepositories(page: Int, perPage: Int): List<GitHubRepository> {
        val response = getJson(
            path = "/user/repos?visibility=all&affiliation=owner,collaborator,organization_member&sort=updated&direction=desc&page=$page&per_page=$perPage"
        )
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("获取仓库列表失败"))
        }
        val array = JSONArray(response.body)
        return buildList {
            for (index in 0 until array.length()) {
                add(array.getJSONObject(index).toRepository())
            }
        }
    }

    override suspend fun createCurrentUserRepository(request: RepositoryCreateRequest): GitHubRepository {
        val body = JSONObject()
            .put("name", request.name)
            .put("private", request.isPrivate)
            .put("auto_init", request.autoInit)
            .put("has_issues", request.hasIssues)
            .put("has_projects", request.hasProjects)
            .put("has_wiki", request.hasWiki)
            .apply {
                request.description?.takeIf { it.isNotBlank() }?.let { description ->
                    put("description", description)
                }
                request.homepage?.takeIf { it.isNotBlank() }?.let { homepage ->
                    put("homepage", homepage)
                }
                request.gitignoreTemplate?.takeIf { it.isNotBlank() }?.let { template ->
                    put("gitignore_template", template)
                }
                request.licenseTemplate?.takeIf { it.isNotBlank() }?.let { template ->
                    put("license_template", template)
                }
            }
            .toString()
        val response = request(path = "/user/repos", method = "POST", body = body)
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("创建仓库失败"))
        }
        return JSONObject(response.body).toRepository()
    }

    override suspend fun searchRepositories(query: String, page: Int, perPage: Int): RepositorySearchPage {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val response = getJson(
            path = "/search/repositories?q=$encodedQuery&page=$page&per_page=$perPage"
        )
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("搜索仓库失败"))
        }
        val root = JSONObject(response.body)
        val items = root.optJSONArray("items") ?: JSONArray()
        val repositories = buildList {
            for (index in 0 until items.length()) {
                add(items.getJSONObject(index).toRepository())
            }
        }
        return RepositorySearchPage(
            repositories = repositories,
            totalCount = root.optInt("total_count", repositories.size),
            incompleteResults = root.optBoolean("incomplete_results", false),
            page = page,
            perPage = perPage
        )
    }

    override suspend fun searchUsers(query: String, page: Int, perPage: Int): UserSearchPage {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val response = getJson(
            path = "/search/users?q=$encodedQuery&page=$page&per_page=$perPage"
        )
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("搜索用户失败"))
        }
        val root = JSONObject(response.body)
        val items = root.optJSONArray("items") ?: JSONArray()
        val users = buildList {
            for (index in 0 until items.length()) {
                add(items.getJSONObject(index).toUserSearchItem())
            }
        }
        return UserSearchPage(
            users = users,
            totalCount = root.optInt("total_count", users.size),
            incompleteResults = root.optBoolean("incomplete_results", false),
            page = page,
            perPage = perPage
        )
    }

    override suspend fun searchIssues(query: String, page: Int, perPage: Int): IssueSearchPage {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val response = getJson(
            path = "/search/issues?q=$encodedQuery&page=$page&per_page=$perPage"
        )
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("搜索 Issue 失败"))
        }
        val root = JSONObject(response.body)
        val items = root.optJSONArray("items") ?: JSONArray()
        val issues = buildList {
            for (index in 0 until items.length()) {
                add(items.getJSONObject(index).toIssueSearchItem())
            }
        }
        return IssueSearchPage(
            issues = issues,
            totalCount = root.optInt("total_count", issues.size),
            incompleteResults = root.optBoolean("incomplete_results", false),
            page = page,
            perPage = perPage
        )
    }

    override suspend fun searchCode(query: String, page: Int, perPage: Int): CodeSearchPage {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val response = getJson(
            path = "/search/code?q=$encodedQuery&page=$page&per_page=$perPage"
        )
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("搜索代码失败"))
        }
        val root = JSONObject(response.body)
        val items = root.optJSONArray("items") ?: JSONArray()
        val codeItems = buildList {
            for (index in 0 until items.length()) {
                add(items.getJSONObject(index).toCodeSearchItem())
            }
        }
        return CodeSearchPage(
            items = codeItems,
            totalCount = root.optInt("total_count", codeItems.size),
            incompleteResults = root.optBoolean("incomplete_results", false),
            page = page,
            perPage = perPage
        )
    }


    override suspend fun getRepository(owner: String, repo: String, includeLanguages: Boolean): GitHubRepository {
        val response = getJson(path = "/repos/$owner/$repo")
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("获取仓库详情失败"))
        }
        val repository = JSONObject(response.body).toRepository()
        if (!includeLanguages) {
            return repository
        }
        val languages = runCatching { listLanguages(owner, repo) }.getOrDefault(emptyList())
        return repository.copy(languages = languages)
    }

    override suspend fun listContents(owner: String, repo: String, path: String): List<RepositoryContentItem> {
        return listContents(owner = owner, repo = repo, path = path, ref = null)
    }

    suspend fun repositoryContentPathExists(owner: String, repo: String, path: String): Boolean {
        val encodedPath = path.toGitHubPath()
        val response = getJson(path = "/repos/$owner/$repo/contents$encodedPath")
        return when (response.statusCode) {
            HttpURLConnection.HTTP_OK -> true
            HttpURLConnection.HTTP_NOT_FOUND -> false
            else -> {
                if (response.isSuccessful) true else throw IllegalStateException(response.toGitHubErrorMessage("检查仓库文件失败"))
            }
        }
    }

    suspend fun repositoryLicensePresent(owner: String, repo: String): Boolean {
        val response = getJson(path = "/repos/$owner/$repo/license")
        return when (response.statusCode) {
            HttpURLConnection.HTTP_OK -> true
            HttpURLConnection.HTTP_NOT_FOUND -> false
            else -> {
                if (response.isSuccessful) true else throw IllegalStateException(response.toGitHubErrorMessage("检查仓库许可证失败"))
            }
        }
    }

    suspend fun getRepositoryTrafficViews(owner: String, repo: String): Int {
        val response = getJson(path = "/repos/$owner/$repo/traffic/views")
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("读取仓库 Views 失败"))
        }
        return JSONObject(response.body).optInt("count", 0)
    }

    suspend fun getRepositoryTrafficClones(owner: String, repo: String): Int {
        val response = getJson(path = "/repos/$owner/$repo/traffic/clones")
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("读取仓库 Clones 失败"))
        }
        return JSONObject(response.body).optInt("count", 0)
    }

    suspend fun getRepositoryCommitActivity(owner: String, repo: String): List<Int> {
        val response = getJson(path = "/repos/$owner/$repo/stats/commit_activity")
        if (response.statusCode == HttpURLConnection.HTTP_ACCEPTED) {
            throw IllegalStateException("GitHub 正在计算 commit activity，请稍后重试。")
        }
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("读取 Commit activity 失败"))
        }
        val weeks = JSONArray(response.body)
        return buildList {
            val startIndex = (weeks.length() - InsightsCommitActivityWeeks).coerceAtLeast(0)
            for (index in startIndex until weeks.length()) {
                add(weeks.getJSONObject(index).optInt("total", 0))
            }
        }
    }

    suspend fun listRepositoryContributorsPreview(owner: String, repo: String, perPage: Int): List<RepositoryContributor> {
        val response = getJson(path = "/repos/$owner/$repo/contributors?per_page=$perPage")
        if (response.statusCode == HttpURLConnection.HTTP_ACCEPTED) {
            throw IllegalStateException("GitHub 正在计算 contributors，请稍后重试。")
        }
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("读取 Contributors 失败"))
        }
        val array = JSONArray(response.body)
        return buildList {
            for (index in 0 until array.length()) {
                val contributor = array.getJSONObject(index)
                add(
                    RepositoryContributor(
                        login = contributor.optString("login", "unknown"),
                        contributions = contributor.optInt("contributions", 0),
                        htmlUrl = contributor.optionalString("html_url")
                    )
                )
            }
        }
    }

    suspend fun listContents(owner: String, repo: String, path: String, ref: String?): List<RepositoryContentItem> {
        val encodedPath = path.toGitHubPath()
        val response = getJson(path = "/repos/$owner/$repo/contents$encodedPath${ref.toRefQuery()}" )
        if (response.isEmptyRepositoryContentsResponse(path)) {
            return emptyList()
        }
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("获取仓库目录失败"))
        }
        val array = JSONArray(response.body)

        return buildList {
            for (index in 0 until array.length()) {
                add(array.getJSONObject(index).toContentItem())
            }
        }.sortedWith(compareBy<RepositoryContentItem> { item ->
            when (item) {
                is RepositoryContentItem.Directory -> 0
                is RepositoryContentItem.File -> 1
                is RepositoryContentItem.Unsupported -> 2
            }
        }.thenBy { it.name.lowercase() })
    }

    override suspend fun getFilePreview(owner: String, repo: String, path: String): RepositoryFilePreview {
        return getFilePreview(owner = owner, repo = repo, path = path, ref = null)
    }

    suspend fun getFilePreview(owner: String, repo: String, path: String, ref: String?): RepositoryFilePreview {
        val encodedPath = path.toGitHubPath()
        val response = getJson(path = "/repos/$owner/$repo/contents$encodedPath${ref.toRefQuery()}" )
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("获取自述文件失败"))
        }
        if (response.body.trimStart().startsWith("[")) {
            throw IllegalStateException("当前路径是目录，不能作为自述文件展示。")
        }
        return JSONObject(response.body).toFilePreview()
    }

    override suspend fun getRepositorySidebarInfo(owner: String, repo: String): RepositorySidebarInfo = coroutineScope {
        val releasesDeferred = async { runCatching { listReleases(owner, repo) } }
        val contributorsDeferred = async { runCatching { listContributors(owner, repo) } }
        val languagesDeferred = async { runCatching { listLanguages(owner, repo) } }
        val releasesResult = releasesDeferred.await()
        val contributorsResult = contributorsDeferred.await()
        val languagesResult = languagesDeferred.await()
        RepositorySidebarInfo(
            releases = releasesResult.getOrDefault(emptyList()),
            contributors = contributorsResult.getOrDefault(emptyList()),
            languages = languagesResult.getOrDefault(emptyList()),
            error = listOfNotNull(
                releasesResult.exceptionOrNull()?.message,
                contributorsResult.exceptionOrNull()?.message,
                languagesResult.exceptionOrNull()?.message
            ).joinToString(separator = "\n").takeIf { it.isNotBlank() }
        )
    }

    private fun listReleases(owner: String, repo: String): List<RepositoryRelease> {
        val response = getJson(path = "/repos/$owner/$repo/releases?per_page=$SidebarReleasesLimit")
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("获取 Releases 失败"))
        }
        return JSONArray(response.body).toReleaseList(latestTagName = fetchLatestReleaseTag(owner, repo))
    }

    override suspend fun listRepositoryReleases(
        owner: String,
        repo: String,
        page: Int,
        perPage: Int
    ): List<RepositoryRelease> {
        val response = getJson(path = "/repos/$owner/$repo/releases?page=$page&per_page=$perPage")
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("获取 Releases 失败"))
        }
        val latestTag = if (page <= 1) fetchLatestReleaseTag(owner, repo) else null
        return JSONArray(response.body).toReleaseList(latestTagName = latestTag)
    }

    suspend fun getRepositoryReleaseByTag(owner: String, repo: String, tagName: String): RepositoryRelease {
        val encodedTag = URLEncoder.encode(tagName, Charsets.UTF_8.name()).replace("+", "%20")
        val response = getJson(path = "/repos/$owner/$repo/releases/tags/$encodedTag")
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("获取 Release 详情失败"))
        }
        val latestTag = fetchLatestReleaseTag(owner, repo)
        return JSONObject(response.body).toRelease(isLatest = latestTag != null && tagName == latestTag)
    }

    override suspend fun listRepositoryPullRequests(
        owner: String,
        repo: String,
        state: String,
        page: Int,
        perPage: Int
    ): List<RepositoryPullRequest> {
        val queryParameters = buildList {
            add("state=${encodeQueryValue(state)}")
            add("page=$page")
            add("per_page=$perPage")
            add("sort=updated")
            add("direction=desc")
        }.joinToString("&")
        val response = getJson(path = "/repos/$owner/$repo/pulls?$queryParameters")
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("获取 Pull Requests 失败"))
        }
        return JSONArray(response.body).toPullRequestList()
    }

    override suspend fun createPullRequest(
        owner: String,
        repo: String,
        title: String,
        head: String,
        base: String,
        body: String
    ): RepositoryPullRequest {
        val requestBody = JSONObject()
            .put("title", title)
            .put("head", head)
            .put("base", base)
            .apply {
                if (body.isNotBlank()) put("body", body)
                put("maintainer_can_modify", true)
            }
            .toString()
        val response = request(
            path = "/repos/$owner/$repo/pulls",
            method = "POST",
            body = requestBody
        )
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("创建 Pull Request 失败"))
        }
        return JSONObject(response.body).toPullRequest()
    }

    override suspend fun listRepositoryActionWorkflows(owner: String, repo: String): List<RepositoryActionWorkflow> {
        val response = getJson(path = "/repos/$owner/$repo/actions/workflows")
        if (!response.isSuccessful) {
            throw response.toActionsUnavailableException() ?: IllegalStateException(
                response.toGitHubErrorMessage("获取 Actions workflow 列表失败")
            )
        }
        val root = JSONObject(response.body)
        val workflows = root.optJSONArray("workflows") ?: JSONArray()
        return (0 until workflows.length()).map { index ->
            workflows.getJSONObject(index).toRepositoryActionWorkflow()
        }
    }

    override suspend fun getRepositoryActionWorkflowDispatchMetadata(
        owner: String,
        repo: String,
        workflow: RepositoryActionWorkflow
    ): RepositoryActionWorkflow {
        if (workflow.hasLoadedDispatchMetadata) return workflow
        val cacheKey = "$owner/$repo:${workflow.path}:${workflow.updatedAt.orEmpty()}"
        val metadata = workflowDispatchMetadataCache.getOrPut(cacheKey) {
            loadWorkflowDispatchMetadata(owner, repo, workflow.path)
        }
        return workflow.copy(
            isDispatchable = metadata.triggers.contains("workflow_dispatch"),
            rawTriggers = metadata.triggers,
            dispatchInputs = metadata.inputs,
            hasLoadedDispatchMetadata = true
        )
    }

    override suspend fun dispatchRepositoryWorkflow(
        owner: String,
        repo: String,
        workflowIdOrFileName: String,
        ref: String,
        inputs: Map<String, String>
    ) {
        val payload = buildString {
            append('{')
            append("\"ref\":\"").append(escapeJsonValue(ref)).append('"')
            if (inputs.isNotEmpty()) {
                append(",\"inputs\":{")
                inputs.entries.forEachIndexed { index, entry ->
                    if (index > 0) append(',')
                    append('"').append(escapeJsonKey(entry.key)).append("\":\"")
                        .append(escapeJsonValue(entry.value)).append('"')
                }
                append('}')
            }
            append('}')
        }
        val response = request(
            path = "/repos/$owner/$repo/actions/workflows/$workflowIdOrFileName/dispatches",
            method = "POST",
            body = payload
        )
        if (!response.isSuccessful) {
            throw response.toActionsUnavailableException() ?: IllegalStateException(
                response.toGitHubErrorMessage("触发 Actions workflow 失败")
            )
        }
    }

    override suspend fun listRepositoryActionRuns(
        owner: String,
        repo: String,
        page: Int,
        perPage: Int,
        status: String?,
        workflowId: Long?
    ): List<RepositoryActionRun> {
        val queryParameters = buildList {
            add("page=$page")
            add("per_page=$perPage")
            status?.takeIf { it.isNotBlank() }?.let { add("status=${encodeQueryValue(it)}") }
        }.joinToString("&")
        val runsPath = workflowId?.takeIf { it > 0L }?.let { workflow ->
            "/repos/$owner/$repo/actions/workflows/$workflow/runs?$queryParameters"
        } ?: "/repos/$owner/$repo/actions/runs?$queryParameters"
        val response = getJson(path = runsPath)
        if (!response.isSuccessful) {
            throw response.toActionsUnavailableException() ?: IllegalStateException(
                response.toGitHubErrorMessage("获取 Actions 运行失败")
            )
        }
        val root = JSONObject(response.body)
        val runs = root.optJSONArray("workflow_runs") ?: JSONArray()
        return buildList {
            for (index in 0 until runs.length()) {
                add(runs.getJSONObject(index).toActionRun())
            }
        }
    }

    fun getRepositoryActionRun(owner: String, repo: String, runId: Long): RepositoryActionRunDetail {
        val response = getJson(path = "/repos/$owner/$repo/actions/runs/$runId")
        if (!response.isSuccessful) {
            throw response.toActionsUnavailableException() ?: IllegalStateException(
                response.toGitHubErrorMessage("获取 Actions 运行详情失败")
            )
        }
        return JSONObject(response.body).toActionRunDetail()
    }

    fun listRepositoryActionRunArtifacts(owner: String, repo: String, runId: Long): List<RepositoryActionArtifact> {
        val response = getJson(path = "/repos/$owner/$repo/actions/runs/$runId/artifacts?per_page=100")
        if (!response.isSuccessful) {
            throw response.toActionsUnavailableException() ?: IllegalStateException(
                response.toGitHubErrorMessage("获取 Actions 构建产物失败")
            )
        }
        val artifacts = JSONObject(response.body).optJSONArray("artifacts") ?: JSONArray()
        return artifacts.toActionArtifactList()
    }

    fun downloadRepositoryActionRunLogs(owner: String, repo: String, runId: Long): ByteArray {
        val response = getBytes(path = "/repos/$owner/$repo/actions/runs/$runId/logs", maxBytes = ActionRunLogsMaxBytes)
        if (!response.isSuccessful) {
            throw response.toActionsUnavailableException() ?: IllegalStateException(
                response.toGitHubErrorMessage("获取 Actions 编译日志失败")
            )
        }
        return response.body
    }

    override suspend fun listRepositorySecurityAlerts(
        owner: String,
        repo: String,
        alertType: String,
        alertState: String?,
        page: Int,
        perPage: Int
    ): List<RepositorySecurityAlert> {
        val probe = SecurityProbe.alertListProbe(alertType)
            ?: throw IllegalArgumentException("不支持的安全告警类型：$alertType")
        val response = getJson(
            path = probe.path(
                owner = owner,
                repo = repo,
                page = page,
                perPage = perPage,
                state = alertState
            )
        )
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("获取安全告警失败"))
        }
        return response.toSecurityAlerts(probe, limit = perPage)
    }

    override suspend fun getRepositorySecurityAlert(
        owner: String,
        repo: String,
        alertType: String,
        number: Int
    ): RepositorySecurityAlert {
        val probe = SecurityProbe.alertListProbe(alertType)
            ?: throw IllegalArgumentException("不支持的安全告警类型：$alertType")
        val response = getJson(path = probe.detailPath(owner, repo, number))
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("获取安全告警详情失败"))
        }
        return JSONObject(response.body).toSecurityAlert(probe)
    }
    suspend fun listNotifications(
        all: Boolean,
        page: Int,
        perPage: Int
    ): List<GitHubNotification> {
        val queryParameters = buildList {
            add("all=$all")
            add("participating=false")
            add("page=$page")
            add("per_page=$perPage")
        }.joinToString("&")
        val response = getJson(path = "/notifications?$queryParameters")
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("获取通知失败"))
        }
        return JSONArray(response.body).toNotificationList()
    }

    suspend fun markNotificationThreadAsRead(threadId: String) {
        val response = request(path = "/notifications/threads/$threadId", method = "PATCH")
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("标记通知已读失败"))
        }
    }

    suspend fun markNotificationThreadAsDone(threadId: String) {
        val response = request(path = "/notifications/threads/$threadId/subscription", method = "DELETE")
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("完成通知失败"))
        }
    }

    suspend fun unsubscribeNotificationThread(threadId: String) {
        val response = request(path = "/notifications/threads/$threadId/subscription", method = "DELETE")
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("取消订阅通知失败"))
        }
    }

    suspend fun subscribeNotificationThread(threadId: String) {
        val response = request(
            path = "/notifications/threads/$threadId/subscription",
            method = "PUT",
            body = "{\"subscribed\":true,\"ignored\":false}"
        )
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("订阅通知失败"))
        }
    }


    override suspend fun listRepositoryBranches(owner: String, repo: String): List<RepositoryBranch> {
        val repository = getRepository(owner, repo, includeLanguages = false)
        val response = getJson(path = "/repos/$owner/$repo/branches?per_page=$BranchesLimit")
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("获取仓库分支失败"))
        }
        return JSONArray(response.body).toBranchList(defaultBranch = repository.defaultBranch)
    }

    suspend fun getRepositorySecuritySummary(owner: String, repo: String): RepositorySecuritySummary = coroutineScope {
        val securityPolicyDeferred = async { probeSecurityPolicy(owner, repo) }
        val dependabotDeferred = async { probeSecurityEndpoint(owner, repo, SecurityProbe.Dependabot) }
        val codeScanningDeferred = async { probeSecurityEndpoint(owner, repo, SecurityProbe.CodeScanning) }
        val secretScanningDeferred = async { probeSecurityEndpoint(owner, repo, SecurityProbe.SecretScanning) }
        val securityPolicy = securityPolicyDeferred.await()
        val dependabot = dependabotDeferred.await()
        val codeScanning = codeScanningDeferred.await()
        val secretScanning = secretScanningDeferred.await()
        val probes = listOf(securityPolicy, dependabot.probe, codeScanning.probe, secretScanning.probe)
        val alerts = listOf(dependabot, codeScanning, secretScanning).flatMap { it.alerts }
        RepositorySecuritySummary(
            probes = probes,
            alerts = alerts,
            notices = buildList {
                add("当前页面使用 GitHub REST API 聚合，只读展示仓库安全能力状态。")
                if (probes.any { it.status == RepositorySecurityProbeStatus.Inaccessible }) {
                    add("部分安全接口需要仓库管理员权限、私有仓库权限或额外 token scope。")
                }
                if (probes.any { it.status == RepositorySecurityProbeStatus.Error }) {
                    add("部分接口返回异常，稍后重试或检查仓库权限后可刷新。")
                }
            }
        )
    }

    private fun probeSecurityPolicy(owner: String, repo: String): RepositorySecurityProbe {
        val response = getJson(path = "/repos/$owner/$repo/community/profile")
        if (!response.isSuccessful) {
            return response.toSecurityProbe(
                key = SecurityProbe.SecurityPolicy.key,
                title = SecurityProbe.SecurityPolicy.title,
                description = SecurityProbe.SecurityPolicy.description
            )
        }
        val files = JSONObject(response.body).optJSONObject("files")
        val policy = files?.optJSONObject("security")
        val status = if (policy != null) RepositorySecurityProbeStatus.Available else RepositorySecurityProbeStatus.Empty
        return RepositorySecurityProbe(
            key = SecurityProbe.SecurityPolicy.key,
            title = SecurityProbe.SecurityPolicy.title,
            description = SecurityProbe.SecurityPolicy.description,
            status = status,
            value = if (status == RepositorySecurityProbeStatus.Available) "已配置" else "未发现",
            detail = policy?.optionalString("html_url") ?: "检查 SECURITY.md 或社区健康文件。"
        )
    }

    private fun probeSecurityEndpoint(owner: String, repo: String, probe: SecurityProbe): SecurityEndpointResult {
        val response = getJson(path = probe.path(owner, repo))
        if (!response.isSuccessful) {
            return SecurityEndpointResult(
                probe = response.toSecurityProbe(
                    key = probe.key,
                    title = probe.title,
                    description = probe.description
                )
            )
        }
        val alerts = response.toSecurityAlerts(probe)
        return SecurityEndpointResult(
            probe = RepositorySecurityProbe(
                key = probe.key,
                title = probe.title,
                description = probe.description,
                status = if (alerts.isNotEmpty()) RepositorySecurityProbeStatus.Available else RepositorySecurityProbeStatus.Empty,
                value = if (alerts.isNotEmpty()) "${alerts.size} 项" else "暂无告警",
                detail = probe.successDetail(alerts.size)
            ),
            alerts = alerts
        )
    }

    private fun NetworkResponse.toSecurityProbe(
        key: String,
        title: String,
        description: String
    ): RepositorySecurityProbe {
        val errorMessage = toGitHubErrorMessage("安全接口探测失败")
        val status = when {
            statusCode == 403 && errorMessage.contains("disabled", ignoreCase = true) -> RepositorySecurityProbeStatus.Disabled
            statusCode == 403 -> RepositorySecurityProbeStatus.Inaccessible
            statusCode == 404 -> RepositorySecurityProbeStatus.Disabled
            statusCode == 451 -> RepositorySecurityProbeStatus.Inaccessible
            else -> RepositorySecurityProbeStatus.Error
        }
        return RepositorySecurityProbe(
            key = key,
            title = title,
            description = description,
            status = status,
            value = when (status) {
                RepositorySecurityProbeStatus.Disabled -> "未启用"
                RepositorySecurityProbeStatus.Inaccessible -> "不可访问"
                RepositorySecurityProbeStatus.Error -> "请求失败"
                else -> null
            },
            detail = errorMessage
        )
    }

    private fun NetworkResponse.toSecurityAlerts(
        probe: SecurityProbe,
        limit: Int = SecurityAlertPreviewLimit
    ): List<RepositorySecurityAlert> {
        val trimmed = body.trim()
        if (!trimmed.startsWith("[")) return emptyList()
        return runCatching {
            JSONArray(trimmed).toSecurityAlertList(probe, limit)
        }.getOrDefault(emptyList())
    }

    private fun JSONArray.toSecurityAlertList(probe: SecurityProbe, limit: Int): List<RepositorySecurityAlert> {
        return buildList {
            for (index in 0 until minOf(length(), limit)) {
                val alert = optJSONObject(index) ?: continue
                add(alert.toSecurityAlert(probe))
            }
        }
    }

    private fun JSONObject.toSecurityAlert(probe: SecurityProbe): RepositorySecurityAlert {
        return when (probe) {
            SecurityProbe.Dependabot -> toDependabotSecurityAlert(probe.title)
            SecurityProbe.CodeScanning -> toCodeScanningSecurityAlert(probe.title)
            SecurityProbe.SecretScanning -> toSecretScanningSecurityAlert(probe.title)
            SecurityProbe.SecurityPolicy -> RepositorySecurityAlert(
                source = probe.title,
                title = probe.title,
                state = "available"
            )
        }
    }

    private fun JSONObject.toDependabotSecurityAlert(source: String): RepositorySecurityAlert {
        val advisory = optJSONObject("security_advisory")
        val dependency = optJSONObject("dependency")
        val packageName = dependency?.optJSONObject("package")?.optionalString("name")
        val ecosystem = dependency?.optJSONObject("package")?.optionalString("ecosystem")
        val vulnerableRequirements = dependency?.optionalString("manifest_path")
        val firstPatchedVersion = optJSONObject("security_vulnerability")
            ?.optJSONObject("first_patched_version")
            ?.optionalString("identifier")
        val dismissedBy = optJSONObject("dismissed_by")?.optionalString("login")
        val fixedAt = optionalString("fixed_at")
        val dismissedAt = optionalString("dismissed_at")
        val dismissedReason = optionalString("dismissed_reason")
        val dismissedComment = optionalString("dismissed_comment")
        val details = listOfNotNull(
            packageName?.let { name -> ecosystem?.let { "$it · $name" } ?: name },
            vulnerableRequirements?.let { "Manifest: $it" },
            firstPatchedVersion?.let { "Patched in: $it" },
            advisory?.optionalString("ghsa_id")?.let { "Advisory: $it" },
            fixedAt?.let { "Fixed at: $it" },
            dismissedReason?.let { "Dismissed: $it" }
        )
        val detailGroups = listOfNotNull(
            groupedDetails(
                title = "依赖",
                items = listOfNotNull(
                    packageName?.let { "Package: $it" },
                    ecosystem?.let { "Ecosystem: $it" },
                    vulnerableRequirements?.let { "Manifest: $it" }
                )
            ),
            groupedDetails(
                title = "漏洞公告",
                items = listOfNotNull(
                    advisory?.optionalString("ghsa_id")?.let { "GHSA: $it" },
                    advisory?.optionalString("cve_id")?.let { "CVE: $it" },
                    advisory?.optionalString("severity")?.let { "Severity: $it" }
                )
            ),
            groupedDetails(
                title = "修复",
                items = listOfNotNull(
                    firstPatchedVersion?.let { "Patched in: $it" },
                    fixedAt?.let { "Fixed at: $it" }
                )
            ),
            groupedDetails(
                title = "忽略",
                items = listOfNotNull(
                    dismissedReason?.let { "Reason: $it" },
                    dismissedComment?.let { "Comment: $it" },
                    dismissedBy?.let { "By: $it" },
                    dismissedAt?.let { "At: $it" }
                )
            )
        )
        return RepositorySecurityAlert(
            number = optionalInt("number"),
            source = source,
            title = advisory?.optionalString("summary") ?: packageName ?: "Dependabot alert",
            state = optionalString("state") ?: "open",
            severity = advisory?.optionalString("severity"),
            createdAt = optionalString("created_at"),
            htmlUrl = optionalString("html_url"),
            details = details,
            detailGroups = detailGroups
        )
    }

    private fun JSONObject.toCodeScanningSecurityAlert(source: String): RepositorySecurityAlert {
        val rule = optJSONObject("rule")
        val tool = optJSONObject("tool")
        val mostRecentInstance = optJSONObject("most_recent_instance")
        val location = mostRecentInstance?.optJSONObject("location")
        val physicalLocation = location?.optJSONObject("physical_location")
        val artifactLocation = physicalLocation?.optJSONObject("artifact_location")
        val path = artifactLocation?.optionalString("uri")
        val startLine = physicalLocation?.optJSONObject("region")?.optInt("start_line", 0)?.takeIf { it > 0 }
        val fixedAt = optionalString("fixed_at")
        val dismissedAt = optionalString("dismissed_at")
        val dismissedReason = optionalString("dismissed_reason")
        val dismissedComment = optionalString("dismissed_comment")
        val dismissedBy = optJSONObject("dismissed_by")?.optionalString("login")
        val details = listOfNotNull(
            tool?.optionalString("name")?.let { "Tool: $it" },
            rule?.optionalString("id")?.let { "Rule: $it" },
            path?.let { filePath -> if (startLine != null) "$filePath:$startLine" else filePath },
            optInt("instances", 0).takeIf { it > 0 }?.let { "Instances: $it" },
            fixedAt?.let { "Fixed at: $it" },
            dismissedReason?.let { "Dismissed: $it" }
        )
        val detailGroups = listOfNotNull(
            groupedDetails(
                title = "工具",
                items = listOfNotNull(tool?.optionalString("name")?.let { "Tool: $it" })
            ),
            groupedDetails(
                title = "规则",
                items = listOfNotNull(
                    rule?.optionalString("id")?.let { "Rule: $it" },
                    rule?.optionalString("name")?.let { "Name: $it" },
                    rule?.optionalString("security_severity_level")?.let { "Security severity: $it" }
                        ?: rule?.optionalString("severity")?.let { "Severity: $it" }
                )
            ),
            groupedDetails(
                title = "位置",
                items = listOfNotNull(
                    path?.let { filePath -> if (startLine != null) "File: $filePath:$startLine" else "File: $filePath" },
                    optInt("instances", 0).takeIf { it > 0 }?.let { "Instances: $it" }
                )
            ),
            groupedDetails(
                title = "处理",
                items = listOfNotNull(
                    fixedAt?.let { "Fixed at: $it" },
                    dismissedReason?.let { "Dismissed reason: $it" },
                    dismissedComment?.let { "Dismissed comment: $it" },
                    dismissedBy?.let { "Dismissed by: $it" },
                    dismissedAt?.let { "Dismissed at: $it" }
                )
            )
        )
        return RepositorySecurityAlert(
            number = optionalInt("number"),
            source = source,
            title = rule?.optionalString("description") ?: rule?.optionalString("name") ?: "Code scanning alert",
            state = optionalString("state") ?: "open",
            severity = rule?.optionalString("security_severity_level") ?: rule?.optionalString("severity"),
            createdAt = optionalString("created_at"),
            htmlUrl = optionalString("html_url"),
            details = details,
            detailGroups = detailGroups
        )
    }

    private fun JSONObject.toSecretScanningSecurityAlert(source: String): RepositorySecurityAlert {
        val secretType = optionalString("secret_type_display_name") ?: optionalString("secret_type")
        val resolution = optionalString("resolution")
        val resolutionComment = optionalString("resolution_comment")
        val resolvedAt = optionalString("resolved_at")
        val resolvedBy = optJSONObject("resolved_by")?.optionalString("login")
        val validity = optionalString("validity")
        val locationsUrl = optionalString("locations_url")
        val details = listOfNotNull(
            secretType?.let { "Secret type: $it" },
            validity?.let { "Validity: $it" },
            resolution?.let { "Resolution: $it" },
            resolvedAt?.let { "Resolved at: $it" },
            locationsUrl?.let { "Locations available" }
        )
        val detailGroups = listOfNotNull(
            groupedDetails(
                title = "密钥",
                items = listOfNotNull(
                    secretType?.let { "Secret type: $it" },
                    validity?.let { "Validity: $it" }
                )
            ),
            groupedDetails(
                title = "处理",
                items = listOfNotNull(
                    resolution?.let { "Resolution: $it" },
                    resolutionComment?.let { "Comment: $it" },
                    resolvedBy?.let { "Resolved by: $it" },
                    resolvedAt?.let { "Resolved at: $it" }
                )
            ),
            groupedDetails(
                title = "位置",
                items = listOfNotNull(locationsUrl?.let { "Locations available" })
            )
        )
        return RepositorySecurityAlert(
            number = optionalInt("number"),
            source = source,
            title = secretType ?: "Secret scanning alert",
            state = optionalString("state") ?: resolution ?: "open",
            severity = null,
            createdAt = optionalString("created_at"),
            htmlUrl = optionalString("html_url"),
            details = details,
            detailGroups = detailGroups
        )
    }

    private fun fetchLatestReleaseTag(owner: String, repo: String): String? {
        val response = runCatching { getJson(path = "/repos/$owner/$repo/releases/latest") }.getOrNull() ?: return null
        if (!response.isSuccessful) return null
        return runCatching { JSONObject(response.body).optionalString("tag_name") }.getOrNull()
    }

    override suspend fun createRelease(
        owner: String,
        repo: String,
        tagName: String,
        targetCommitish: String?,
        name: String,
        body: String,
        draft: Boolean,
        prerelease: Boolean,
        makeLatest: Boolean
    ): RepositoryRelease {
        val requestBody = JSONObject().apply {
            put("tag_name", tagName)
            if (!targetCommitish.isNullOrBlank()) put("target_commitish", targetCommitish)
            if (name.isNotBlank()) put("name", name)
            if (body.isNotBlank()) put("body", body)
            put("draft", draft)
            put("prerelease", prerelease)
            // GitHub 的 make_latest 接受字符串 "true"/"false"；草稿或预发布不应设为 latest。
            put("make_latest", if (makeLatest && !draft && !prerelease) "true" else "false")
        }.toString()
        val response = request(
            path = "/repos/$owner/$repo/releases",
            method = "POST",
            body = requestBody
        )
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("创建发布版本失败"))
        }
        return JSONObject(response.body).toRelease(isLatest = makeLatest && !draft && !prerelease)
    }

    override suspend fun uploadReleaseAsset(
        uploadUrl: String,
        fileName: String,
        mimeType: String,
        fileBytes: ByteArray
    ) {
        val response = uploadReleaseAssetBytes(uploadUrl, fileName, mimeType, fileBytes)
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("上传发布附件失败"))
        }
    }

    private fun uploadReleaseAssetBytes(
        uploadUrl: String,
        fileName: String,
        mimeType: String,
        fileBytes: ByteArray
    ): NetworkResponse {
        val baseUrl = uploadUrl.substringBefore('{')
        val separator = if ('?' in baseUrl) '&' else '?'
        val encodedName = URLEncoder.encode(fileName, Charsets.UTF_8.name()).replace("+", "%20")
        val url = "$baseUrl${separator}name=$encodedName"
        val response = httpClient.execute(
            GitHubHttpRequest(
                pathOrUrl = url,
                method = GitHubHttpMethod.POST,
                bodyBytes = fileBytes,
                contentType = mimeType.ifBlank { "application/octet-stream" },
                apiVersion = "2026-03-10",
                followRedirects = true,
                responseBodyAsBytes = false,
                responseBodyMaxBytes = null,
                isRetryable = false
            )
        )
        return response.toNetworkResponse()
    }

    private fun JSONObject.toRelease(isLatest: Boolean): RepositoryRelease {
        val tagName = optString("tag_name", "")
        val body = optionalString("body")
        return RepositoryRelease(
            name = optionalString("name") ?: tagName.takeIf { it.isNotBlank() } ?: "Release",
            tagName = tagName,
            htmlUrl = optionalString("html_url"),
            publishedAt = optionalString("published_at") ?: optionalString("created_at"),
            createdAt = optionalString("created_at"),
            authorLogin = optJSONObject("author")?.optionalString("login"),
            isLatest = isLatest,
            isPrerelease = optBoolean("prerelease", false),
            isDraft = optBoolean("draft", false),
            body = body,
            bodySummary = body?.toReleaseBodySummary(),
            targetCommitish = optionalString("target_commitish"),
            uploadUrl = optionalString("upload_url"),
            zipballUrl = optionalString("zipball_url"),
            tarballUrl = optionalString("tarball_url"),
            assets = optJSONArray("assets")?.toReleaseAssetList().orEmpty()
        )
    }

    override suspend fun listRepositoryIssues(
        owner: String,
        repo: String,
        state: String,
        page: Int,
        perPage: Int,
        creator: String?,
        labels: List<String>
    ): List<RepositoryIssue> {
        val queryParameters = buildList {
            // GitHub REST Issues 支持 creator 与 labels；类别/已回答状态属于 Discussions/GraphQL，不在这里映射。
            add("state=${encodeQueryValue(state)}")
            add("page=$page")
            add("per_page=$perPage")
            creator?.takeIf { it.isNotBlank() }?.let { add("creator=${encodeQueryValue(it)}") }
            labels.filter { it.isNotBlank() }.takeIf { it.isNotEmpty() }?.let { pickedLabels ->
                add("labels=${encodeQueryValue(pickedLabels.joinToString(","))}")
            }
        }.joinToString("&")
        val response = getJson(
            path = "/repos/$owner/$repo/issues?$queryParameters"
        )
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("获取 Issues 失败"))
        }
        return JSONArray(response.body).toIssueList()
    }

    override suspend fun getRepositoryIssue(
        owner: String,
        repo: String,
        number: Int
    ): RepositoryIssueDetail {
        val response = getJson(path = "/repos/$owner/$repo/issues/$number")
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("获取 Issue 详情失败"))
        }
        return JSONObject(response.body).toIssueDetail()
    }

    override suspend fun listRepositoryIssueComments(
        owner: String,
        repo: String,
        number: Int,
        page: Int,
        perPage: Int
    ): List<RepositoryIssueComment> {
        val response = getJson(
            path = "/repos/$owner/$repo/issues/$number/comments?page=$page&per_page=$perPage"
        )
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("获取 Issue 评论失败"))
        }
        return JSONArray(response.body).toIssueCommentList()
    }

    private fun JSONArray.toIssueList(): List<RepositoryIssue> {
        return buildList {
            for (index in 0 until length()) {
                val issue = getJSONObject(index)
                add(
                    RepositoryIssue(
                        number = issue.optInt("number", 0),
                        title = issue.optionalString("title") ?: "(无标题)",
                        state = issue.optString("state", "open"),
                        authorLogin = issue.optJSONObject("user")?.optionalString("login") ?: "unknown",
                        commentCount = issue.optInt("comments", 0),
                        labels = issue.optJSONArray("labels").toIssueLabelList(),
                        createdAt = issue.optionalString("created_at"),
                        htmlUrl = issue.optionalString("html_url"),
                        isPullRequest = issue.has("pull_request") && !issue.isNull("pull_request")
                    )
                )
            }
        }
    }

    private fun JSONArray.toPullRequestList(): List<RepositoryPullRequest> {
        return buildList {
            for (index in 0 until length()) {
                val pullRequest = getJSONObject(index)
                add(pullRequest.toPullRequest())
            }
        }
    }

    private fun JSONObject.toPullRequest(): RepositoryPullRequest {
    return RepositoryPullRequest(
        number = optInt("number", 0),
        title = optionalString("title") ?: "(无标题)",
        state = optString("state", "open"),
        authorLogin = optJSONObject("user")?.optionalString("login") ?: "unknown",
        commentCount = optInt("comments", 0),
        createdAt = optionalString("created_at"),
        updatedAt = optionalString("updated_at"),
        closedAt = optionalString("closed_at"),
        mergedAt = optionalString("merged_at"),
        draft = optBoolean("draft", false),
        baseRef = optJSONObject("base")?.optionalString("ref").orEmpty(),
        headRef = optJSONObject("head")?.optionalString("ref").orEmpty(),
        htmlUrl = optionalString("html_url")
    )
}

private fun JSONObject.toRepositoryActionWorkflow(): RepositoryActionWorkflow {
    val path = optionalString("path").orEmpty()
    return RepositoryActionWorkflow(
        id = optLong("id", 0L),
        name = optionalString("name") ?: path.substringAfterLast('/').ifBlank { "Workflow" },
        path = path,
        state = optionalString("state"),
        htmlUrl = optionalString("html_url"),
        badgeUrl = optionalString("badge_url"),
        createdAt = optionalString("created_at"),
        updatedAt = optionalString("updated_at"),
        isDispatchable = true
    )
}

private fun loadWorkflowDispatchMetadata(owner: String, repo: String, path: String): WorkflowDispatchMetadata {
    if (path.isBlank()) return WorkflowDispatchMetadata()
    val encodedPath = path.toGitHubPath()
    val response = getJson(path = "/repos/$owner/$repo/contents$encodedPath")
    if (!response.isSuccessful) return WorkflowDispatchMetadata()
    return parseWorkflowDispatchMetadata(JSONObject(response.body).toFilePreview().text)
}

private fun parseWorkflowDispatchMetadata(yaml: String): WorkflowDispatchMetadata {
    val rawLines = yaml.lineSequence().filterNot { it.trimStart().startsWith("#") }.toList()
    val triggers = parseWorkflowTriggers(rawLines)
    val dispatchIndex = rawLines.indexOfFirst { line ->
        line.trim().substringBefore(':').trim().unquoteYamlKey() == "workflow_dispatch"
    }
    if (dispatchIndex < 0) return WorkflowDispatchMetadata(triggers = triggers)
    val dispatchIndent = rawLines[dispatchIndex].countLeadingSpaces()
    val inputsIndex = ((dispatchIndex + 1) until rawLines.size).firstOrNull { index ->
        val line = rawLines[index]
        val trimmed = line.trim()
        line.countLeadingSpaces() > dispatchIndent && trimmed.substringBefore(':').trim().unquoteYamlKey() == "inputs"
    } ?: return WorkflowDispatchMetadata(triggers = triggers)
    val inputsIndent = rawLines[inputsIndex].countLeadingSpaces()
    val inputs = mutableListOf<RepositoryActionWorkflowInput>()
    var index = inputsIndex + 1
    while (index < rawLines.size) {
        val line = rawLines[index]
        val indent = line.countLeadingSpaces()
        val trimmed = line.trim()
        if (trimmed.isBlank()) {
            index++
            continue
        }
        if (indent <= inputsIndent) break
        val name = trimmed.substringBefore(':').trim().unquoteYamlKey()
        if (name.isBlank() || trimmed.startsWith("-")) {
            index++
            continue
        }
        val fieldIndent = indent
        val properties = mutableMapOf<String, String>()
        val options = mutableListOf<String>()
        var optionIndent: Int? = null
        index++
        while (index < rawLines.size) {
            val childLine = rawLines[index]
            val childIndent = childLine.countLeadingSpaces()
            val childTrimmed = childLine.trim()
            if (childTrimmed.isBlank()) {
                index++
                continue
            }
            if (childIndent <= fieldIndent) break
            if (childTrimmed.startsWith("-")) {
                if (optionIndent == null || childIndent > fieldIndent) {
                    options.add(childTrimmed.removePrefix("-").trim().unquoteYamlValue())
                }
            } else {
                val key = childTrimmed.substringBefore(':').trim().unquoteYamlKey()
                val value = childTrimmed.substringAfter(':', "").trim().unquoteYamlValue()
                if (key == "options") optionIndent = childIndent
                if (key.isNotBlank() && value.isNotBlank()) properties[key] = value
            }
            index++
        }
        inputs.add(
            RepositoryActionWorkflowInput(
                name = name,
                description = properties["description"],
                required = properties["required"].equals("true", ignoreCase = true),
                defaultValue = properties["default"],
                type = properties["type"] ?: if (options.isNotEmpty()) "choice" else "string",
                options = options
            )
        )
    }
    return WorkflowDispatchMetadata(triggers = triggers, inputs = inputs)
}

private fun parseWorkflowTriggers(rawLines: List<String>): List<String> {
    val triggers = linkedSetOf<String>()
    rawLines.forEachIndexed { index, line ->
        val trimmed = line.trim()
        if (trimmed.isBlank()) return@forEachIndexed
        val key = trimmed.substringBefore(':').trim().unquoteYamlKey()
        val value = trimmed.substringAfter(':', "").trim()
        if (key in WorkflowTriggerNames) triggers.add(key)
        if (key == "on") {
            parseInlineWorkflowTriggers(value).forEach { triggers.add(it) }
            val onIndent = line.countLeadingSpaces()
            var childIndex = index + 1
            while (childIndex < rawLines.size) {
                val childLine = rawLines[childIndex]
                val childIndent = childLine.countLeadingSpaces()
                val childTrimmed = childLine.trim()
                if (childTrimmed.isBlank()) {
                    childIndex++
                    continue
                }
                if (childIndent <= onIndent) break
                val childKey = when {
                    childTrimmed.startsWith("-") -> childTrimmed.removePrefix("-").trim().substringBefore(':').trim().unquoteYamlKey()
                    else -> childTrimmed.substringBefore(':').trim().unquoteYamlKey()
                }
                if (childKey in WorkflowTriggerNames) triggers.add(childKey)
                childIndex++
            }
        }
    }
    return triggers.toList()
}

private fun parseInlineWorkflowTriggers(value: String): List<String> {
    val normalized = value.unquoteYamlValue()
    if (normalized.isBlank()) return emptyList()
    val values = if (normalized.startsWith("[") && normalized.endsWith("]")) {
        normalized.removePrefix("[").removeSuffix("]").split(',')
    } else {
        listOf(normalized)
    }
    return values.map { it.trim().unquoteYamlKey() }.filter { it in WorkflowTriggerNames }
}

private data class WorkflowDispatchMetadata(
    val triggers: List<String> = emptyList(),
    val inputs: List<RepositoryActionWorkflowInput> = emptyList()
)

private fun String.countLeadingSpaces(): Int = takeWhile { it == ' ' }.length

private fun String.unquoteYamlKey(): String = trim().removeSurrounding("\"").removeSurrounding("'")

private fun String.unquoteYamlValue(): String {
    val withoutComment = substringBefore(" #").trim()
    return withoutComment.removeSurrounding("\"").removeSurrounding("'")
}

private fun JSONObject.toActionRun(): RepositoryActionRun {
    return RepositoryActionRun(
        id = optLong("id", 0L),
        name = optionalString("name") ?: optionalString("display_title") ?: "Workflow run",
        status = optionalString("status"),
        conclusion = optionalString("conclusion"),
        event = optionalString("event") ?: "unknown",
        headBranch = optionalString("head_branch"),
        headSha = optionalString("head_sha"),
        htmlUrl = optionalString("html_url"),
        createdAt = optionalString("created_at"),
        updatedAt = optionalString("updated_at")
    )
}

private fun JSONObject.toActionRunDetail(): RepositoryActionRunDetail {
    val status = optionalString("status")
    val conclusion = optionalString("conclusion")
    val workflowName = optionalString("workflow_name") ?: optionalString("name")
    val runNumber = optionalInt("run_number")
    val runAttempt = optionalInt("run_attempt")
    val actorLogin = optJSONObject("actor")?.optionalString("login")
    val triggeringActorLogin = optJSONObject("triggering_actor")?.optionalString("login")
    val repository = optJSONObject("repository")
    val ownerLogin = repository?.optJSONObject("owner")?.optionalString("login")
    val repositoryName = repository?.optionalString("name")
    val headRepository = optJSONObject("head_repository")
    val headCommit = optJSONObject("head_commit")
    val headCommitAuthor = headCommit?.optJSONObject("author")
    val headSha = optionalString("head_sha")
    val event = optionalString("event")
    val headBranch = optionalString("head_branch")
    val path = optionalString("path")
    val pullRequestRefs = optJSONArray("pull_requests")?.let { pullRequests ->
        buildList {
            for (index in 0 until pullRequests.length()) {
                val pullRequest = pullRequests.optJSONObject(index) ?: continue
                val number = pullRequest.optionalInt("number")
                val headRef = pullRequest.optJSONObject("head")?.optionalString("ref")
                val baseRef = pullRequest.optJSONObject("base")?.optionalString("ref")
                add(
                    listOfNotNull(
                        number?.let { "#$it" },
                        listOfNotNull(headRef, baseRef).takeIf { it.isNotEmpty() }?.joinToString(" -> ")
                    ).joinToString(" ").ifBlank { null } ?: continue
                )
            }
        }
    }.orEmpty()
    val displayState = conclusion?.takeIf { it.isNotBlank() }
        ?: status?.takeIf { it.isNotBlank() }
        ?: "unknown"
    return RepositoryActionRunDetail(
        id = optLong("id", 0L),
        nodeId = optionalString("node_id"),
        name = optionalString("display_title") ?: workflowName ?: "Workflow run",
        status = status,
        conclusion = conclusion,
        event = event,
        headBranch = headBranch,
        headSha = headSha,
        htmlUrl = optionalString("html_url"),
        apiUrl = optionalString("url"),
        workflowUrl = optionalString("workflow_url"),
        jobsUrl = optionalString("jobs_url"),
        logsUrl = optionalString("logs_url"),
        artifactsUrl = optionalString("artifacts_url"),
        cancelUrl = optionalString("cancel_url"),
        rerunUrl = optionalString("rerun_url"),
        previousAttemptUrl = optionalString("previous_attempt_url"),
        createdAt = optionalString("created_at"),
        updatedAt = optionalString("updated_at"),
        runStartedAt = optionalString("run_started_at"),
        runNumber = runNumber,
        runAttempt = runAttempt,
        workflowId = optLong("workflow_id", 0L).takeIf { it > 0L },
        checkSuiteId = optLong("check_suite_id", 0L).takeIf { it > 0L },
        workflowName = workflowName,
        actorLogin = actorLogin,
        triggeringActorLogin = triggeringActorLogin,
        repositoryOwner = ownerLogin,
        repositoryName = repositoryName,
        headRepositoryFullName = headRepository?.optionalString("full_name"),
        headRepositoryHtmlUrl = headRepository?.optionalString("html_url"),
        path = path,
        headCommitMessage = headCommit?.optionalString("message"),
        headCommitAuthorName = headCommitAuthor?.optionalString("name"),
        headCommitAuthorEmail = headCommitAuthor?.optionalString("email"),
        headCommitTimestamp = headCommit?.optionalString("timestamp"),
        sourceZipUrl = optionalString("head_zipball_url"),
        sourceTarUrl = optionalString("head_tarball_url"),
        pullRequestRefs = pullRequestRefs,
        displayState = displayState,
        details = listOfNotNull(
            runNumber?.let { "Run #$it" },
            runAttempt?.let { "Attempt: $it" },
            workflowName?.let { "Workflow: $it" },
            event?.let { "Event: $it" },
            headBranch?.let { "Branch: $it" },
            headSha?.takeIf { it.isNotBlank() }?.let { "Commit: ${it.take(7)}" },
            actorLogin?.let { "Actor: $it" },
            triggeringActorLogin?.let { "Triggered by: $it" },
            path?.let { "Path: $it" }
        )
    )
}

private fun JSONArray.toNotificationList(): List<GitHubNotification> {
    return buildList {
        for (index in 0 until length()) {
            add(getJSONObject(index).toNotification())
        }
    }
}

private fun JSONObject.toNotification(): GitHubNotification {
    val subject = optJSONObject("subject")
    val repository = optJSONObject("repository")
    val subjectUrl = subject?.optionalString("url")
    val latestCommentUrl = subject?.optionalString("latest_comment_url")
    return GitHubNotification(
        id = optionalString("id") ?: optLong("id", 0L).toString(),
        repositoryFullName = repository?.optionalString("full_name") ?: "unknown/repository",
        subjectTitle = subject?.optionalString("title") ?: "(无标题)",
        subjectType = subject?.optionalString("type") ?: "Notification",
        reason = optionalString("reason") ?: "unknown",
        unread = optBoolean("unread", false),
        updatedAt = optionalString("updated_at"),
        lastReadAt = optionalString("last_read_at"),
        url = optionalString("url"),
        htmlUrl = subjectUrl?.toGitHubWebUrl()
            ?: latestCommentUrl?.toGitHubWebUrl()
            ?: repository?.optionalString("html_url"),
        repositoryHtmlUrl = repository?.optionalString("html_url"),
        subjectUrl = subjectUrl,
        latestCommentUrl = latestCommentUrl
    )
}

private fun JSONObject.toIssueDetail(): RepositoryIssueDetail {
        return RepositoryIssueDetail(
            number = optInt("number", 0),
            title = optionalString("title") ?: "(无标题)",
            state = optString("state", "open"),
            authorLogin = optJSONObject("user")?.optionalString("login") ?: "unknown",
            body = optionalString("body").orEmpty(),
            commentCount = optInt("comments", 0),
            labels = optJSONArray("labels").toIssueLabelList(),
            createdAt = optionalString("created_at"),
            htmlUrl = optionalString("html_url")
        )
    }

    private fun JSONArray.toIssueCommentList(): List<RepositoryIssueComment> {
        return buildList {
            for (index in 0 until length()) {
                val comment = getJSONObject(index)
                add(comment.toIssueComment())
            }
        }
    }

    private fun JSONObject.toIssueComment(): RepositoryIssueComment {
        return RepositoryIssueComment(
            id = optLong("id", 0L),
            authorLogin = optJSONObject("user")?.optionalString("login") ?: "unknown",
            body = optionalString("body").orEmpty(),
            createdAt = optionalString("created_at"),
            htmlUrl = optionalString("html_url")
        )
    }
    private fun JSONArray?.toIssueLabelList(): List<RepositoryIssueLabel> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                val label = optJSONObject(index) ?: continue
                val name = label.optionalString("name") ?: continue
                add(
                    RepositoryIssueLabel(
                        name = name,
                        color = label.optionalString("color").orEmpty()
                    )
                )
            }
        }
    }

    override suspend fun getRepositoryPermissions(owner: String, repo: String): RepositoryPermissions {
        val response = getJson(path = "/repos/$owner/$repo")
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("获取仓库权限失败"))
        }
        val permissions = JSONObject(response.body).optJSONObject("permissions")
            ?: return RepositoryPermissions()
        return RepositoryPermissions(
            canPush = permissions.optBoolean("push", false),
            isAdmin = permissions.optBoolean("admin", false)
        )
    }

    override suspend fun listRepositoryLabels(owner: String, repo: String): List<RepositoryLabel> {
        val response = getJson(path = "/repos/$owner/$repo/labels?per_page=$LabelsPerPage")
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("获取仓库标签失败"))
        }
        val array = JSONArray(response.body)
        return buildList {
            for (index in 0 until array.length()) {
                val label = array.optJSONObject(index) ?: continue
                val name = label.optionalString("name") ?: continue
                add(
                    RepositoryLabel(
                        name = name,
                        color = label.optionalString("color").orEmpty(),
                        description = label.optionalString("description")
                    )
                )
            }
        }
    }

    override suspend fun createIssue(
        owner: String,
        repo: String,
        title: String,
        body: String,
        labels: List<String>
    ): RepositoryIssueDetail {
        val pickedLabels = labels.filter { it.isNotBlank() }.distinct()
        val requestBody = JSONObject()
            .put("title", title)
            .apply {
                if (body.isNotBlank()) put("body", body)
                if (pickedLabels.isNotEmpty()) {
                    put("labels", JSONArray().apply { pickedLabels.forEach { put(it) } })
                }
            }
            .toString()
        val response = request(
            path = "/repos/$owner/$repo/issues",
            method = "POST",
            body = requestBody
        )
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("创建问题失败"))
        }
        return JSONObject(response.body).toIssueDetail()
    }

    override suspend fun updateIssueState(
        owner: String,
        repo: String,
        number: Int,
        state: String
    ): RepositoryIssueDetail {
        val requestBody = JSONObject().put("state", state).toString()
        val response = request(
            path = "/repos/$owner/$repo/issues/$number",
            method = "PATCH",
            body = requestBody
        )
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("更新问题状态失败"))
        }
        return JSONObject(response.body).toIssueDetail()
    }

    override suspend fun setIssueLabels(
        owner: String,
        repo: String,
        number: Int,
        labelNames: List<String>
    ): RepositoryIssueDetail {
        val labelsArray = JSONArray().apply { labelNames.forEach { put(it) } }
        val requestBody = JSONObject().put("labels", labelsArray).toString()
        val response = request(
            path = "/repos/$owner/$repo/issues/$number",
            method = "PATCH",
            body = requestBody
        )
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("更新问题标签失败"))
        }
        return JSONObject(response.body).toIssueDetail()
    }

    override suspend fun createIssueComment(
        owner: String,
        repo: String,
        number: Int,
        body: String
    ): RepositoryIssueComment {
        val requestBody = JSONObject().put("body", body).toString()
        val response = request(
            path = "/repos/$owner/$repo/issues/$number/comments",
            method = "POST",
            body = requestBody
        )
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("发表评论失败"))
        }
        return JSONObject(response.body).toIssueComment()
    }

    override suspend fun updateIssueComment(
        owner: String,
        repo: String,
        commentId: Long,
        body: String
    ): RepositoryIssueComment {
        val requestBody = JSONObject().put("body", body).toString()
        val response = request(
            path = "/repos/$owner/$repo/issues/comments/$commentId",
            method = "PATCH",
            body = requestBody
        )
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("编辑评论失败"))
        }
        return JSONObject(response.body).toIssueComment()
    }

    override suspend fun deleteIssueComment(owner: String, repo: String, commentId: Long) {
        val response = request(
            path = "/repos/$owner/$repo/issues/comments/$commentId",
            method = "DELETE"
        )
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("删除评论失败"))
        }
    }


    private fun JSONArray.toBranchList(defaultBranch: String): List<RepositoryBranch> {
        return buildList {
            for (index in 0 until length()) {
                val branch = getJSONObject(index)
                val name = branch.optString("name", "")
                if (name.isBlank()) continue
                add(
                    RepositoryBranch(
                        name = name,
                        isDefault = name == defaultBranch,
                        isProtected = branch.optBoolean("protected", false)
                    )
                )
            }
        }.let { branches ->
            branches.sortedWith(compareByDescending<RepositoryBranch> { it.isDefault }.thenBy { it.name })
        }
    }

    private fun JSONArray.toReleaseList(latestTagName: String?): List<RepositoryRelease> {
        return buildList {
            for (index in 0 until length()) {
                val release = getJSONObject(index)
                val tagName = release.optString("tag_name", "")
                add(release.toRelease(isLatest = latestTagName != null && tagName.isNotBlank() && tagName == latestTagName))
            }
        }
    }

    private fun JSONArray.toReleaseAssetList(): List<RepositoryReleaseAsset> {
        return buildList {
            for (index in 0 until length()) {
                val asset = getJSONObject(index)
                val name = asset.optionalString("name") ?: continue
                add(
                    RepositoryReleaseAsset(
                        id = asset.optLong("id", 0L),
                        name = name,
                        label = asset.optionalString("label"),
                        contentType = asset.optionalString("content_type"),
                        sizeBytes = asset.optLong("size", 0L),
                        downloadCount = asset.optInt("download_count", 0),
                        browserDownloadUrl = asset.optionalString("browser_download_url"),
                        createdAt = asset.optionalString("created_at"),
                        updatedAt = asset.optionalString("updated_at")
                    )
                )
            }
        }
    }

    private fun String.toReleaseBodySummary(): String? {
        return lineSequence()
            .map { line -> line.trim().trimStart('#', '*', '-', '>').trim() }
            .firstOrNull { it.isNotBlank() }
            ?.take(ReleaseBodySummaryMaxChars)
    }

    private fun JSONArray.toActionArtifactList(): List<RepositoryActionArtifact> {
        return buildList {
            for (index in 0 until length()) {
                val artifact = getJSONObject(index)
                val name = artifact.optionalString("name") ?: continue
                add(
                    RepositoryActionArtifact(
                        id = artifact.optLong("id", 0L),
                        name = name,
                        sizeInBytes = artifact.optLong("size_in_bytes", 0L),
                        archiveDownloadUrl = artifact.optionalString("archive_download_url"),
                        expired = artifact.optBoolean("expired", false),
                        createdAt = artifact.optionalString("created_at"),
                        expiresAt = artifact.optionalString("expires_at")
                    )
                )
            }
        }
    }

    private fun listContributors(owner: String, repo: String): List<RepositoryContributor> {
        // GitHub 对大仓库首次请求 /contributors 会返回 202 Accepted（后台正在计算贡献者缓存，body 为空）。
        // 此时需短暂等待后重试同一请求，等计算完成拿到 200 + 数据，否则会误判为「无贡献者」。
        val path = "/repos/$owner/$repo/contributors?per_page=$SidebarContributorsLimit"
        var response = getJson(path = path)
        var acceptedAttempts = 0
        while (response.statusCode == HttpURLConnection.HTTP_ACCEPTED && acceptedAttempts < ContributorsAcceptedMaxAttempts) {
            acceptedAttempts++
            Thread.sleep(ContributorsAcceptedDelayMillis)
            response = getJson(path = path)
        }
        if (response.statusCode == HttpURLConnection.HTTP_ACCEPTED) {
            // 仍在计算中：视为暂无可展示数据，返回空而非抛错，避免卡住整页详情。
            return emptyList()
        }
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("获取 Contributors 失败"))
        }
        val body = response.body.trim()
        if (body.isEmpty() || !body.startsWith("[")) {
            return emptyList()
        }
        val array = JSONArray(body)
        return buildList {
            for (index in 0 until array.length()) {
                val contributor = array.getJSONObject(index)
                add(
                    RepositoryContributor(
                        login = contributor.optString("login", "unknown"),
                        contributions = contributor.optInt("contributions", 0),
                        htmlUrl = contributor.optionalString("html_url")
                    )
                )
            }
        }
    }

    fun listLanguages(owner: String, repo: String): List<RepositoryLanguage> {
        val response = getJson(path = "/repos/$owner/$repo/languages")
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("获取 Languages 失败"))
        }
        val languages = JSONObject(response.body)
        val totalBytes = languages.keys().asSequence().sumOf { key -> languages.optLong(key, 0L) }.coerceAtLeast(1L)
        return languages.keys().asSequence()
            .map { key ->
                val bytes = languages.optLong(key, 0L)
                RepositoryLanguage(
                    name = key,
                    bytes = bytes,
                    percentage = ((bytes * 100) / totalBytes).toInt()
                )
            }
            .sortedByDescending { it.bytes }
            .toList()
    }

    override suspend fun getEditableFile(owner: String, repo: String, path: String): EditableRepositoryFile {
        val encodedPath = path.toGitHubPath()
        val response = getJson(path = "/repos/$owner/$repo/contents$encodedPath")
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("获取文件编辑内容失败"))
        }
        if (response.body.trimStart().startsWith("[")) {
            throw IllegalStateException("当前路径是目录，无法编辑为文件。")
        }
        return JSONObject(response.body).toEditableFile()
    }

    override suspend fun getWriteTarget(owner: String, repo: String, path: String): RepositoryFileWriteTarget? {
        val encodedPath = path.toGitHubPath()
        val response = getJson(path = "/repos/$owner/$repo/contents$encodedPath")
        if (response.statusCode == HttpURLConnection.HTTP_NOT_FOUND) return null
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("检查目标路径失败"))
        }
        val body = response.body.trimStart()
        if (body.startsWith("[")) {
            return RepositoryFileWriteTarget(
                path = path,
                name = path.substringAfterLast('/'),
                sha = "",
                sizeBytes = 0L,
                isDirectory = true
            )
        }
        val target = JSONObject(response.body)
        return RepositoryFileWriteTarget(
            path = target.optString("path", path),
            name = target.optString("name", path.substringAfterLast('/')),
            sha = target.optString("sha", ""),
            sizeBytes = target.optLong("size", 0L),
            isDirectory = target.optString("type") == "dir"
        )
    }

    override suspend fun updateFileContent(
        owner: String,
        repo: String,
        path: String,
        message: String,
        content: String,
        sha: String,
        branch: String?
    ): FileContentWriteResult {
        return updateFileContentBase64(
            owner = owner,
            repo = repo,
            path = path,
            message = message,
            encodedContent = Base64.encodeToString(content.toByteArray(Charsets.UTF_8), Base64.NO_WRAP),
            sha = sha,
            branch = branch,
            errorPrefix = "提交文件修改失败"
        )
    }

    override suspend fun updateFileContentBytes(
        owner: String,
        repo: String,
        path: String,
        message: String,
        contentBytes: ByteArray,
        sha: String,
        branch: String?
    ): FileContentWriteResult {
        return updateFileContentBase64(
            owner = owner,
            repo = repo,
            path = path,
            message = message,
            encodedContent = Base64.encodeToString(contentBytes, Base64.NO_WRAP),
            sha = sha,
            branch = branch,
            errorPrefix = "上传覆盖文件失败"
        )
    }

    private fun updateFileContentBase64(
        owner: String,
        repo: String,
        path: String,
        message: String,
        encodedContent: String,
        sha: String,
        branch: String?,
        errorPrefix: String
    ): FileContentWriteResult {
        val encodedPath = path.toGitHubPath()
        val body = JSONObject()
            .put("message", message)
            .put("content", encodedContent)
            .put("sha", sha)
            .apply {
                branch?.takeIf { it.isNotBlank() }?.let { put("branch", it) }
            }
            .toString()
        val response = request(
            path = "/repos/$owner/$repo/contents$encodedPath",
            method = "PUT",
            body = body
        )
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage(errorPrefix))
        }
        return response.toFileContentWriteResult()
    }

    override suspend fun createFileContentBytes(
        owner: String,
        repo: String,
        path: String,
        message: String,
        contentBytes: ByteArray,
        branch: String?
    ): FileContentWriteResult {
        return createFileContentBase64(
            owner = owner,
            repo = repo,
            path = path,
            message = message,
            encodedContent = Base64.encodeToString(contentBytes, Base64.NO_WRAP),
            branch = branch,
            errorPrefix = "上传文件失败"
        )
    }

    override suspend fun createFileContent(
        owner: String,
        repo: String,
        path: String,
        message: String,
        content: String,
        branch: String?
    ): FileContentWriteResult {
        return createFileContentBase64(
            owner = owner,
            repo = repo,
            path = path,
            message = message,
            encodedContent = Base64.encodeToString(content.toByteArray(Charsets.UTF_8), Base64.NO_WRAP),
            branch = branch,
            errorPrefix = "创建文件失败"
        )
    }

    private fun createFileContentBase64(
        owner: String,
        repo: String,
        path: String,
        message: String,
        encodedContent: String,
        branch: String?,
        errorPrefix: String
    ): FileContentWriteResult {
        val encodedPath = path.toGitHubPath()
        val body = JSONObject()
            .put("message", message)
            .put("content", encodedContent)
            .apply {
                branch?.takeIf { it.isNotBlank() }?.let { put("branch", it) }
            }
            .toString()
        val response = request(
            path = "/repos/$owner/$repo/contents$encodedPath",
            method = "PUT",
            body = body
        )
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage(errorPrefix))
        }
        return response.toFileContentWriteResult()
    }

    override suspend fun deleteFileContent(
        owner: String,
        repo: String,
        path: String,
        message: String,
        sha: String,
        branch: String?
    ): FileContentWriteResult {
        val encodedPath = path.toGitHubPath()
        val body = JSONObject()
            .put("message", message)
            .put("sha", sha)
            .apply {
                branch?.takeIf { it.isNotBlank() }?.let { put("branch", it) }
            }
            .toString()
        val response = request(
            path = "/repos/$owner/$repo/contents$encodedPath",
            method = "DELETE",
            body = body
        )
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("删除文件失败"))
        }
        return response.toFileContentWriteResult()
    }

    override suspend fun createFork(owner: String, repo: String): GitHubRepository {
return createFork(
owner = owner,
repo = repo,
targetOwner = null,
targetName = null,
defaultBranchOnly = false
)
}

override suspend fun createFork(
owner: String,
repo: String,
targetOwner: String?,
targetName: String?,
defaultBranchOnly: Boolean
): GitHubRepository {
val payload = JSONObject().apply {
targetOwner?.trim()?.takeIf { it.isNotBlank() }?.let { put("organization", it) }
targetName?.trim()?.takeIf { it.isNotBlank() && it != repo }?.let { put("name", it) }
if (defaultBranchOnly) {
put("default_branch_only", true)
}
}.toString()
val response = request(path = "/repos/$owner/$repo/forks", method = "POST", body = payload)
if (!response.isSuccessful) {
throw IllegalStateException(response.toGitHubErrorMessage("派生仓库失败"))
}
return JSONObject(response.body).toRepository()
}

override suspend fun findCurrentAccountFork(owner: String, repo: String, currentAccountLogin: String): GitHubRepository? {
val login = currentAccountLogin.trim().takeIf { it.isNotBlank() } ?: return null
val response = getJson(path = "/repos/$owner/$repo/forks?per_page=$ForkLookupLimit&sort=newest")
if (!response.isSuccessful) {
throw IllegalStateException(response.toGitHubErrorMessage("检查当前账号 Fork 失败"))
}
val forks = JSONArray(response.body)
for (index in 0 until forks.length()) {
val fork = forks.getJSONObject(index).toRepository()
if (fork.ownerLogin.equals(login, ignoreCase = true)) {
return fork
}
}
return null
}

override suspend fun isRepositoryNameAvailable(owner: String, repo: String): Boolean {
val encodedOwner = URLEncoder.encode(owner, Charsets.UTF_8.name()).replace("+", "%20")
val encodedRepo = URLEncoder.encode(repo, Charsets.UTF_8.name()).replace("+", "%20")
val response = request(path = "/repos/$encodedOwner/$encodedRepo", method = "GET")
return when (response.statusCode) {
HttpURLConnection.HTTP_NOT_FOUND -> true
HttpURLConnection.HTTP_OK -> false
else -> {
if (response.isSuccessful) false else throw IllegalStateException(response.toGitHubErrorMessage("检查仓库名可用性失败"))
}
}
}

override suspend fun updateRepositoryDescription(owner: String, repo: String, description: String?): GitHubRepository {
val requestBody = JSONObject().put("description", description).toString()
val response = request(path = "/repos/$owner/$repo", method = "PATCH", body = requestBody)
if (!response.isSuccessful) {
throw IllegalStateException(response.toGitHubErrorMessage("更新 Fork 描述失败"))
}
return JSONObject(response.body).toRepository()
}

override suspend fun getForkSyncStatus(owner: String, repo: String): RepositoryForkSyncStatus {
        val forkRepository = getRepository(owner, repo, includeLanguages = false)
        val parentFullName = forkRepository.parentFullName
            ?: throw IllegalStateException("当前仓库不是派生仓库，无法同步上游。")
        val parentBranch = forkRepository.parentDefaultBranch?.takeIf { it.isNotBlank() } ?: forkRepository.defaultBranch
        val parentOwner = parentFullName.substringBefore('/').takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("无法识别上游仓库所有者：$parentFullName")
        val forkBranch = forkRepository.defaultBranch
        val response = getJson(
            path = "/repos/$owner/$repo/compare/${toCompareRef(parentOwner, parentBranch)}...${toCompareRef(owner, forkBranch)}"
        )
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("检查 Fork 同步状态失败"))
        }
        return JSONObject(response.body).toForkSyncStatus(
            upstreamFullName = parentFullName,
            upstreamBranch = parentBranch,
            forkBranch = forkBranch
        )
    }

    override suspend fun syncFork(owner: String, repo: String, branch: String): RepositoryForkSyncStatus {
        val targetBranch = branch.ifBlank { getRepository(owner, repo, includeLanguages = false).defaultBranch }
        val response = request(
            path = "/repos/$owner/$repo/merge-upstream",
            method = "POST",
            body = JSONObject().put("branch", targetBranch).toString()
        )
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("同步 Fork 失败"))
        }
        return getForkSyncStatus(owner, repo)
    }

    override suspend fun isStarred(owner: String, repo: String): Boolean {
        val response = request(path = "/user/starred/$owner/$repo", method = "GET")
        return when (response.statusCode) {
            HttpURLConnection.HTTP_NO_CONTENT -> true
            HttpURLConnection.HTTP_NOT_FOUND -> false
            else -> {
                if (response.isSuccessful) true else throw IllegalStateException(
                    response.toGitHubErrorMessage("获取收藏状态失败")
                )
            }
        }
    }

    override suspend fun isWatching(owner: String, repo: String): Boolean {
        val response = request(path = "/repos/$owner/$repo/subscription", method = "GET")
        return when (response.statusCode) {
            HttpURLConnection.HTTP_OK -> true
            HttpURLConnection.HTTP_NOT_FOUND -> false
            else -> {
                if (response.isSuccessful) true else throw IllegalStateException(
                    response.toGitHubErrorMessage("获取关注状态失败")
                )
            }
        }
    }

    override suspend fun star(owner: String, repo: String) {
        val response = request(path = "/user/starred/$owner/$repo", method = "PUT")
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("收藏仓库失败"))
        }
    }

    override suspend fun unstar(owner: String, repo: String) {
        val response = request(path = "/user/starred/$owner/$repo", method = "DELETE")
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("取消收藏仓库失败"))
        }
    }

    override suspend fun watch(owner: String, repo: String) {
        val response = request(
            path = "/repos/$owner/$repo/subscription",
            method = "PUT",
            body = "{\"subscribed\":true,\"ignored\":false}"
        )
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("关注仓库失败"))
        }
    }

    override suspend fun unwatch(owner: String, repo: String) {
        val response = request(path = "/repos/$owner/$repo/subscription", method = "DELETE")
        if (!response.isSuccessful) {
            throw IllegalStateException(response.toGitHubErrorMessage("取消关注仓库失败"))
        }
    }

    private fun getJson(path: String): NetworkResponse {
        return request(path = path, method = "GET")
    }

    private fun getBytes(path: String, maxBytes: Int): BinaryNetworkResponse {
        return executeBinaryRequest(path = path, maxBytes = maxBytes)
    }

    private fun requestGraphQl(query: String, variables: JSONObject): NetworkResponse {
        val body = JSONObject()
            .put("query", query)
            .put("variables", variables)
            .toString()
        return request(path = "/graphql", method = "POST", body = body)
    }
    /**
     * 发起一次 GitHub API 请求。
     *
     * Transport details live in GitHubHttpClient. This gateway keeps the legacy
     * NetworkResponse facade so domain parsing code can migrate incrementally.
     */
    private fun request(path: String, method: String, body: String? = null): NetworkResponse {
        val isGraphQlQuery = path == "/graphql" && method.equals("POST", ignoreCase = true)
        val response = httpClient.execute(
            GitHubHttpRequest(
                pathOrUrl = path,
                method = method.toGitHubHttpMethod(),
                body = body,
                isRetryable = isGraphQlQuery || (method.equals("GET", ignoreCase = true) && body == null)
            )
        )
        return response.toNetworkResponse()
    }


    private fun executeBinaryRequest(path: String, maxBytes: Int): BinaryNetworkResponse {
        return httpClient.execute(
            GitHubHttpRequest(
                pathOrUrl = GitHubEndpoints.ApiBaseUrl + path,
                method = GitHubHttpMethod.GET,
                apiVersion = "2026-03-10",
                followRedirects = true,
                responseBodyAsBytes = true,
                responseBodyMaxBytes = maxBytes,
                isRetryable = false
            )
        ).toBinaryNetworkResponse()
    }

    private fun GitHubHttpResponse.toBinaryNetworkResponse(): BinaryNetworkResponse {
        return BinaryNetworkResponse(statusCode = statusCode, body = bodyBytes ?: body.toByteArray(Charsets.UTF_8))
    }

    private fun HttpURLConnection.toNetworkResponse(): NetworkResponse {
        return try {
            val code = responseCode
            val stream = if (code in 200..299) inputStream else errorStream ?: inputStream
            val body = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader -> reader.readText() }
            NetworkResponse(statusCode = code, body = body)
        } finally {
            disconnect()
        }
    }

    private fun GitHubHttpResponse.toNetworkResponse(): NetworkResponse {
        return NetworkResponse(statusCode = statusCode, body = body)
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

    private fun JSONObject.toUserProfile(): GitHubUserProfile {
        return GitHubUserProfile(
            id = getLong("id"),
            login = getString("login"),
            name = optionalString("name"),
            avatarUrl = optionalString("avatar_url"),
            bio = optionalString("bio"),
            company = optionalString("company"),
            location = optionalString("location"),
            blog = optionalString("blog"),
            email = optionalString("email"),
            twitterUsername = optionalString("twitter_username"),
            publicRepos = optInt("public_repos", 0),
            publicGists = optInt("public_gists", 0),
            followers = optInt("followers", 0),
            following = optInt("following", 0),
            htmlUrl = getString("html_url"),
            createdAt = optionalString("created_at"),
            updatedAt = optionalString("updated_at")
        )
    }

    private fun JSONObject.toContributionCalendar(overview: GitHubContributionOverview): GitHubContributionCalendar {
        val weeksArray = optJSONArray("weeks") ?: JSONArray()
        val weeks = buildList {
            for (weekIndex in 0 until weeksArray.length()) {
                val weekObject = weeksArray.getJSONObject(weekIndex)
                val daysArray = weekObject.optJSONArray("contributionDays") ?: JSONArray()
                val days = buildList {
                    for (dayIndex in 0 until daysArray.length()) {
                        val dayObject = daysArray.getJSONObject(dayIndex)
                        add(
                            GitHubContributionDay(
                                date = dayObject.optString("date", ""),
                                weekday = dayObject.optInt("weekday", dayIndex),
                                contributionCount = dayObject.optInt("contributionCount", 0),
                                color = dayObject.optionalString("color")
                            )
                        )
                    }
                }
                add(
                    GitHubContributionWeek(
                        firstDay = weekObject.optString("firstDay", ""),
                        days = days
                    )
                )
            }
        }
        val monthsArray = optJSONArray("months") ?: JSONArray()
        val months = buildList {
            for (index in 0 until monthsArray.length()) {
                val monthObject = monthsArray.getJSONObject(index)
                add(
                    GitHubContributionMonth(
                        name = monthObject.optString("name", ""),
                        year = monthObject.optInt("year", 0),
                        firstDay = monthObject.optString("firstDay", ""),
                        totalWeeks = monthObject.optInt("totalWeeks", 0)
                    )
                )
            }
        }
        return GitHubContributionCalendar(
            totalContributions = optInt("totalContributions", 0),
            weeks = weeks,
            months = months,
            overview = overview,
            year = months.lastOrNull()?.year
        )
    }

    private fun JSONObject.toContributionOverview(): GitHubContributionOverview {
        val repositoryNames = listOf(
            repositoryNamesFromContributionArray("commitContributionsByRepository"),
            repositoryNamesFromContributionArray("issueContributionsByRepository"),
            repositoryNamesFromContributionArray("pullRequestContributionsByRepository"),
            repositoryNamesFromContributionArray("pullRequestReviewContributionsByRepository")
        ).flatten().distinct().take(8)
        return GitHubContributionOverview(
            commitCount = optInt("totalCommitContributions", 0),
            issueCount = optInt("totalIssueContributions", 0),
            pullRequestCount = optInt("totalPullRequestContributions", 0),
            pullRequestReviewCount = optInt("totalPullRequestReviewContributions", 0),
            restrictedContributionCount = optInt("restrictedContributionsCount", 0),
            repositoryNames = repositoryNames
        )
    }

    private fun JSONObject.repositoryNamesFromContributionArray(name: String): List<String> {
        val array = optJSONArray(name) ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val repositoryName = array
                    .optJSONObject(index)
                    ?.optJSONObject("repository")
                    ?.optString("nameWithOwner")
                    ?.takeIf { it.isNotBlank() }
                if (repositoryName != null) add(repositoryName)
            }
        }
    }

    private fun JSONObject.toRepository(): GitHubRepository {
        val owner = getJSONObject("owner")
val parent = optJSONObject("parent")
val source = optJSONObject("source")
return GitHubRepository(
            id = getLong("id"),
            name = getString("name"),
            fullName = getString("full_name"),
            ownerLogin = owner.getString("login"),
            description = optionalString("description"),
            isPrivate = getBoolean("private"),
            fork = getBoolean("fork"),
            archived = getBoolean("archived"),
            defaultBranch = optString("default_branch", "main"),
            stargazersCount = optInt("stargazers_count", 0),
            watchersCount = optInt("watchers_count", 0),
            forksCount = optInt("forks_count", 0),
            openIssuesCount = optInt("open_issues_count", 0),
            language = optionalString("language"),
            ownerAvatarUrl = owner.optionalString("avatar_url"),
            ownerName = owner.optionalString("name"),
            ownerType = owner.optionalString("type"),
            parentFullName = parent?.optionalString("full_name"),
            parentDefaultBranch = parent?.optionalString("default_branch"),
            sourceFullName = source?.optionalString("full_name"),
            updatedAt = optionalString("updated_at"),
            pushedAt = optionalString("pushed_at"),
            htmlUrl = getString("html_url")
        )
    }

    private fun JSONObject.toForkSyncStatus(
    upstreamFullName: String,
    upstreamBranch: String,
    forkBranch: String
): RepositoryForkSyncStatus {
    return RepositoryForkSyncStatus(
        upstreamFullName = upstreamFullName,
        upstreamBranch = upstreamBranch,
        forkBranch = forkBranch,
        aheadBy = optInt("ahead_by", 0),
        behindBy = optInt("behind_by", 0)
    )
}

private fun JSONObject.toUserSearchItem(): GitHubUserSearchItem {
        return GitHubUserSearchItem(
            login = optString("login", "unknown"),
            avatarUrl = optionalString("avatar_url"),
            type = optionalString("type"),
            htmlUrl = optionalString("html_url")
        )
    }

    private fun JSONObject.toIssueSearchItem(): GitHubIssueSearchItem {
        val (owner, repo) = parseOwnerRepoFromRepositoryUrl(optionalString("repository_url"))
        return GitHubIssueSearchItem(
            number = optInt("number", 0),
            title = optString("title", ""),
            state = optString("state", ""),
            authorLogin = optJSONObject("user")?.optString("login", "unknown") ?: "unknown",
            commentCount = optInt("comments", 0),
            repositoryOwner = owner,
            repositoryName = repo,
            isPullRequest = has("pull_request") && !isNull("pull_request"),
            htmlUrl = optionalString("html_url")
        )
    }

    /**
     * 从 `repository_url`（形如 `https://api.github.com/repos/{owner}/{repo}`）解析 owner/repo。
     * 解析失败时返回空串对，调用方据此决定能否跳转 Issue 详情。
     */
    private fun parseOwnerRepoFromRepositoryUrl(repositoryUrl: String?): Pair<String, String> {
        if (repositoryUrl.isNullOrBlank()) return "" to ""
        val tail = repositoryUrl.substringAfterLast("/repos/", missingDelimiterValue = "")
        if (tail.isBlank()) return "" to ""
        val segments = tail.split("/")
        if (segments.size < 2) return "" to ""
        return segments[0] to segments[1]
    }

    private fun JSONObject.toCodeSearchItem(): GitHubCodeSearchItem {
        val repository = optJSONObject("repository")
        val owner = repository?.optJSONObject("owner")?.optString("login", "") ?: ""
        val repoName = repository?.optString("name", "") ?: ""
        return GitHubCodeSearchItem(
            name = optString("name", ""),
            path = optString("path", ""),
            repositoryOwner = owner,
            repositoryName = repoName,
            htmlUrl = optionalString("html_url")
        )
    }

    private fun JSONObject.toContentItem(): RepositoryContentItem {
        return when (val type = optString("type")) {
            "dir" -> RepositoryContentItem.Directory(
                name = getString("name"),
                path = getString("path"),
                htmlUrl = optionalString("html_url")
            )

            "file" -> RepositoryContentItem.File(
                name = getString("name"),
                path = getString("path"),
                sizeBytes = optLong("size", 0L),
                downloadUrl = optionalString("download_url"),
                htmlUrl = optionalString("html_url")
            )

            else -> RepositoryContentItem.Unsupported(
                name = optString("name", type.ifBlank { "Unsupported" }),
                path = optString("path", ""),
                reason = "Unsupported GitHub content type: ${type.ifBlank { "unknown" }}",
                htmlUrl = optionalString("html_url")
            )
        }
    }

    private fun JSONObject.toFilePreview(): RepositoryFilePreview {
        val name = getString("name")
        val path = getString("path")
        val sizeBytes = optLong("size", 0L)
        if (sizeBytes > FilePreviewMaxBytes) {
            throw IllegalStateException("$name 超过 ${formatByteLimit(FilePreviewMaxBytes)}，暂不在应用内预览。")
        }
        if (sizeBytes == 0L) {
            return RepositoryFilePreview(
                name = name,
                path = path,
                sizeBytes = sizeBytes,
                text = "这是一个空文件。",
                htmlUrl = optionalString("html_url")
            )
        }
        return RepositoryFilePreview(
            name = name,
            path = path,
            sizeBytes = sizeBytes,
            text = decodeContentText(name),
            htmlUrl = optionalString("html_url")
        )
    }

    private fun JSONObject.toEditableFile(): EditableRepositoryFile {
        val name = getString("name")
        val path = getString("path")
        val sizeBytes = optLong("size", 0L)
        if (sizeBytes > FilePreviewMaxBytes) {
            throw IllegalStateException("$name 超过 ${formatByteLimit(FilePreviewMaxBytes)}，暂不支持在应用内编辑。")
        }
        return EditableRepositoryFile(
            name = name,
            path = path,
            sha = getString("sha"),
            sizeBytes = sizeBytes,
            text = if (sizeBytes == 0L) "" else decodeContentText(name),
            htmlUrl = optionalString("html_url")
        )
    }

    private fun JSONObject.decodeContentText(name: String): String {
        val encoding = optString("encoding")
        if (encoding != "base64") {
            throw IllegalStateException("$name 使用 ${encoding.ifBlank { "未知" }} 编码，暂不支持预览。")
        }
        val encodedContent = optString("content").filterNot { it == '\n' || it == '\r' }
        if (encodedContent.isBlank()) {
            throw IllegalStateException("GitHub 未返回 $name 的可预览内容，可能需要通过浏览器查看。")
        }
        val bytes = Base64.decode(encodedContent, Base64.DEFAULT)
        if (bytes.isProbablyBinary()) {
            throw IllegalStateException("$name 看起来是二进制文件，暂不支持在应用内预览。")
        }
        return bytes.decodeUtf8StrictOrNull()
            ?: throw IllegalStateException("$name 不是有效的 UTF-8 文本，暂不支持预览。")
    }

    private fun ByteArray.decodeUtf8StrictOrNull(): String? {
        return runCatching {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(this))
                .toString()
        }.getOrNull()
    }

    private fun ByteArray.isProbablyBinary(): Boolean {
        if (isEmpty()) return false
        val checkedBytes = take(BinaryCheckMaxBytes)
        if (checkedBytes.any { it == 0.toByte() }) return true
        val controlBytes = checkedBytes.count { byte ->
            val value = byte.toInt() and 0xFF
            value < 0x20 && value != 0x09 && value != 0x0A && value != 0x0D
        }
        return controlBytes > checkedBytes.size / BinaryControlByteRatioDivisor
    }

    private val WorkflowTriggerNames = setOf(
    "workflow_dispatch",
    "push",
    "pull_request",
    "schedule",
    "release",
    "workflow_run",
    "repository_dispatch"
)

private fun encodeQueryValue(value: String): String {
        return URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")
    }

private fun toCompareRef(owner: String, branch: String): String {
    return "${encodeQueryValue(owner)}:${encodeQueryValue(branch)}"
}

    private fun escapeJsonKey(value: String): String = escapeJsonValue(value)

private fun escapeJsonValue(value: String): String {
    return buildString {
        value.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(char)
            }
        }
    }
}

private fun String.toGitHubPath(): String {
        if (isBlank()) return ""
        return split("/")
            .filter { it.isNotBlank() }
            .joinToString(separator = "/", prefix = "/") { segment ->
                URLEncoder.encode(segment, Charsets.UTF_8.name()).replace("+", "%20")
            }
    }

    private fun String?.toRefQuery(): String {
        val ref = this?.trim().orEmpty()
        if (ref.isBlank()) return ""
        return "?ref=${URLEncoder.encode(ref, Charsets.UTF_8.name()).replace("+", "%20") }"
    }

    private fun JSONObject.optionalString(name: String): String? {
    return if (isNull(name)) null else optString(name).takeIf { it.isNotBlank() }
}

private fun JSONObject.optionalInt(name: String): Int? {
    return if (isNull(name)) null else optInt(name, 0).takeIf { it > 0 }
}

private fun groupedDetails(title: String, items: List<String>): RepositorySecurityAlertDetailGroup? {
    return items.takeIf { it.isNotEmpty() }?.let { RepositorySecurityAlertDetailGroup(title = title, items = it) }
}

private fun String.toGitHubWebUrl(): String {
    val apiPrefix = "https://api.github.com/repos/"
    if (!startsWith(apiPrefix)) return this
    val segments = removePrefix(apiPrefix).split("/")
    if (segments.size < 3) return this
    val owner = segments[0]
    val repo = segments[1]
    return when (segments[2]) {
        "issues" -> if (segments.size >= 4) "https://github.com/$owner/$repo/issues/${segments[3]}" else "https://github.com/$owner/$repo/issues"
        "pulls" -> if (segments.size >= 4) "https://github.com/$owner/$repo/pull/${segments[3]}" else "https://github.com/$owner/$repo/pulls"
        "commits" -> if (segments.size >= 4) "https://github.com/$owner/$repo/commit/${segments[3]}" else "https://github.com/$owner/$repo/commits"
        "releases" -> if (segments.size >= 4) "https://github.com/$owner/$repo/releases/tag/${segments[3]}" else "https://github.com/$owner/$repo/releases"
        "discussions" -> if (segments.size >= 4) "https://github.com/$owner/$repo/discussions/${segments[3]}" else "https://github.com/$owner/$repo/discussions"
        else -> "https://github.com/$owner/$repo"
    }
}

private fun formatByteLimit(bytes: Long): String {
    return if (bytes % (1024L * 1024L) == 0L) {
        "${bytes / (1024L * 1024L)} MiB"
    } else {
        "${bytes / 1024L} KiB"
    }
}

    private fun NetworkResponse.toFileContentWriteResult(): FileContentWriteResult {
        val json = JSONObject(body)
        val commit = json.optJSONObject("commit")
        val commitHash = commit?.optString("sha").orEmpty()
        return FileContentWriteResult(commitHash = commitHash)
    }

    private fun NetworkResponse.isEmptyRepositoryContentsResponse(path: String): Boolean {
        if (statusCode != HttpURLConnection.HTTP_NOT_FOUND || path.isNotBlank()) return false
        val message = runCatching { JSONObject(body).optString("message") }.getOrDefault("")
        return message.equals("This repository is empty.", ignoreCase = true)
    }

    private fun NetworkResponse.toGitHubErrorMessage(fallback: String): String {
        val parsedMessage = runCatching {
            val json = JSONObject(body)
            buildString {
                append(json.optString("message", fallback))
                json.optJSONArray("errors")?.let { errors ->
                    val details = buildList {
                        for (index in 0 until errors.length()) {
                            val error = errors.optJSONObject(index) ?: continue
                            val field = error.optString("field").takeIf { it.isNotBlank() }
                            val code = error.optString("code").takeIf { it.isNotBlank() }
                            val message = error.optString("message").takeIf { it.isNotBlank() }
                            val resource = error.optString("resource").takeIf { it.isNotBlank() }
                            val detail = listOfNotNull(resource, field, code, message).joinToString(" · ")
                            if (detail.isNotBlank()) add(detail)
                        }
                    }
                    if (details.isNotEmpty()) {
                        append("\n")
                        append(details.joinToString("\n"))
                    }
                }
                json.optString("documentation_url").takeIf { it.isNotBlank() }?.let { documentationUrl ->
                    append("\n")
                    append(documentationUrl)
                }
            }
        }.getOrElse {
            body.takeIf { it.isNotBlank() } ?: fallback
        }
        val hint = when (statusCode) {
            401 -> "\n请重新登录或检查访问令牌是否有效。"
            403 -> "\n请检查 Token 是否具备创建仓库所需权限；私有仓库通常需要 repo 或对应 Fine-grained 权限。"
            422 -> "\n请检查仓库名称是否已被占用，或模板/许可证选项是否被 GitHub 接受。"
            else -> ""
        }
        return "HTTP $statusCode: $parsedMessage$hint"
    }

    private fun BinaryNetworkResponse.toGitHubErrorMessage(fallback: String): String {
        val text = body.toString(Charsets.UTF_8).takeIf { it.isNotBlank() }
        return NetworkResponse(statusCode = statusCode, body = text.orEmpty()).toGitHubErrorMessage(fallback)
    }

    private fun NetworkResponse.toActionsUnavailableException(): GitHubRepositoryFeatureUnavailableException? {
        val message = when (statusCode) {
            403 -> "当前账号没有权限读取该仓库的 Actions 运行记录，可能需要仓库权限或更高 token scope。"
            404 -> "该仓库可能未启用 Actions，或当前账号无法访问 Actions 分区。"
            451 -> "该仓库的 Actions 分区当前不可访问。"
            else -> return null
        }
        return GitHubRepositoryFeatureUnavailableException(
            feature = "actions",
            statusCode = statusCode,
            message = message
        )
    }

    private fun BinaryNetworkResponse.toActionsUnavailableException(): GitHubRepositoryFeatureUnavailableException? {
        return NetworkResponse(statusCode = statusCode, body = body.toString(Charsets.UTF_8)).toActionsUnavailableException()
    }

    private data class NetworkResponse(
        val statusCode: Int,
        val body: String
    ) {
        val isSuccessful: Boolean = statusCode in 200..299
    }

    private data class BinaryNetworkResponse(
        val statusCode: Int,
        val body: ByteArray
    ) {
        val isSuccessful: Boolean = statusCode in 200..299
    }

    private data class SecurityEndpointResult(
        val probe: RepositorySecurityProbe,
        val alerts: List<RepositorySecurityAlert> = emptyList()
    )

    private enum class SecurityProbe(
        val key: String,
        val title: String,
        val description: String,
        private val pathTemplate: String
    ) {
        SecurityPolicy(
            key = "security_policy",
            title = "安全策略",
            description = "检查仓库是否提供 SECURITY.md 或社区健康安全策略。",
            pathTemplate = ""
        ),
        Dependabot(
            key = "dependabot_alerts",
            title = "Dependabot alerts",
            description = "统计依赖漏洞告警接口是否可用以及当前告警数量。",
            pathTemplate = "/repos/%s/%s/dependabot/alerts"
        ),
        CodeScanning(
            key = "code_scanning_alerts",
            title = "Code scanning alerts",
            description = "统计代码扫描告警接口是否可用以及当前告警数量。",
            pathTemplate = "/repos/%s/%s/code-scanning/alerts"
        ),
        SecretScanning(
            key = "secret_scanning_alerts",
            title = "Secret scanning alerts",
            description = "统计密钥扫描告警接口是否可用以及当前告警数量。",
            pathTemplate = "/repos/%s/%s/secret-scanning/alerts"
        );

        fun path(
            owner: String,
            repo: String,
            page: Int = 1,
            perPage: Int = SecurityAlertPreviewLimit,
            state: String? = null
        ): String {
            val queryParameters = buildList {
                add("page=$page")
                add("per_page=$perPage")
                state?.takeIf { it.isNotBlank() }?.let { add("state=$it") }
            }.joinToString("&")
            return "${pathTemplate.format(owner, repo)}?$queryParameters"
        }

        fun detailPath(owner: String, repo: String, number: Int): String {
            return "${pathTemplate.format(owner, repo)}/$number"
        }

        fun successDetail(count: Int): String {
            return if (count > 0) {
                "接口已启用并返回告警，可继续接入筛选与详情页。"
            } else {
                "接口已启用，当前没有返回告警。"
            }
        }

        companion object {
            fun alertListProbe(key: String): SecurityProbe? {
                return entries.firstOrNull { it.key == key && it != SecurityPolicy }
            }
        }
    }

    private companion object {
        const val TimeoutMillis = 20_000
        const val MaxRetryAttempts = 3
        const val RetryBaseDelayMillis = 500L
        const val BranchesLimit = 50
        val RetryableStatusCodes = setOf(429, 500, 502, 503, 504)
        const val ContributorsAcceptedMaxAttempts = 3
        const val ContributorsAcceptedDelayMillis = 1_500L
        const val FilePreviewMaxBytes = 1024L * 1024L
        const val BinaryCheckMaxBytes = 8 * 1024
        val workflowDispatchMetadataCache = ConcurrentHashMap<String, WorkflowDispatchMetadata>()
        const val BinaryControlByteRatioDivisor = 10
        const val SidebarReleasesLimit = 3
        const val ForkLookupLimit = 100
        const val SidebarContributorsLimit = 6
        const val InsightsCommitActivityWeeks = 8
        const val ReleaseBodySummaryMaxChars = 120
        const val LabelsPerPage = 100
        const val SecurityAlertPreviewLimit = 5
        const val ActionRunLogsMaxBytes = 8 * 1024 * 1024
    }
}