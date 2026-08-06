package com.Sunset.REN.GitHub.domain.repo

/**
 * 仓库浏览与轻管理的领域入口。
 */
interface GitHubRepositoryGateway {
    suspend fun listCurrentUserRepositories(page: Int, perPage: Int): List<GitHubRepository>

    suspend fun createCurrentUserRepository(request: RepositoryCreateRequest): GitHubRepository

    suspend fun searchRepositories(query: String, page: Int, perPage: Int): RepositorySearchPage

    suspend fun searchUsers(query: String, page: Int, perPage: Int): UserSearchPage

    suspend fun searchIssues(query: String, page: Int, perPage: Int): IssueSearchPage

    suspend fun searchCode(query: String, page: Int, perPage: Int): CodeSearchPage

    suspend fun getRepository(owner: String, repo: String, includeLanguages: Boolean = true): GitHubRepository

    suspend fun listContents(owner: String, repo: String, path: String): List<RepositoryContentItem>

    suspend fun getFilePreview(owner: String, repo: String, path: String): RepositoryFilePreview

    suspend fun getRepositorySidebarInfo(owner: String, repo: String): RepositorySidebarInfo

    suspend fun listRepositoryReleases(owner: String, repo: String, page: Int, perPage: Int): List<RepositoryRelease>

    suspend fun listRepositoryPullRequests(
        owner: String,
        repo: String,
        state: String,
        page: Int,
        perPage: Int
    ): List<RepositoryPullRequest>

    suspend fun createPullRequest(
        owner: String,
        repo: String,
        title: String,
        head: String,
        base: String,
        body: String
    ): RepositoryPullRequest

    suspend fun listRepositoryActionWorkflows(owner: String, repo: String): List<RepositoryActionWorkflow>

    suspend fun getRepositoryActionWorkflowDispatchMetadata(
        owner: String,
        repo: String,
        workflow: RepositoryActionWorkflow
    ): RepositoryActionWorkflow

    suspend fun dispatchRepositoryWorkflow(
        owner: String,
        repo: String,
        workflowIdOrFileName: String,
        ref: String,
        inputs: Map<String, String> = emptyMap()
    )

    suspend fun listRepositoryActionRuns(
        owner: String,
        repo: String,
        page: Int,
        perPage: Int,
        status: String? = null,
        workflowId: Long? = null
    ): List<RepositoryActionRun>

    suspend fun listRepositorySecurityAlerts(
        owner: String,
        repo: String,
        alertType: String,
        alertState: String? = null,
        page: Int,
        perPage: Int
    ): List<RepositorySecurityAlert>

    suspend fun getRepositorySecurityAlert(
        owner: String,
        repo: String,
        alertType: String,
        number: Int
    ): RepositorySecurityAlert

    suspend fun listRepositoryBranches(owner: String, repo: String): List<RepositoryBranch>

    suspend fun createRelease(
        owner: String,
        repo: String,
        tagName: String,
        targetCommitish: String?,
        name: String,
        body: String,
        draft: Boolean,
        prerelease: Boolean,
        makeLatest: Boolean
    ): RepositoryRelease

    suspend fun uploadReleaseAsset(
        uploadUrl: String,
        fileName: String,
        mimeType: String,
        fileBytes: ByteArray
    )

    suspend fun listRepositoryIssues(
        owner: String,
        repo: String,
        state: String,
        page: Int,
        perPage: Int,
        creator: String? = null,
        labels: List<String> = emptyList()
    ): List<RepositoryIssue>

    suspend fun getRepositoryIssue(owner: String, repo: String, number: Int): RepositoryIssueDetail

    suspend fun listRepositoryIssueComments(
        owner: String,
        repo: String,
        number: Int,
        page: Int,
        perPage: Int
    ): List<RepositoryIssueComment>

    suspend fun getRepositoryPermissions(owner: String, repo: String): RepositoryPermissions

    suspend fun listRepositoryLabels(owner: String, repo: String): List<RepositoryLabel>

    suspend fun createIssue(
        owner: String,
        repo: String,
        title: String,
        body: String,
        labels: List<String> = emptyList()
    ): RepositoryIssueDetail

    suspend fun updateIssueState(
        owner: String,
        repo: String,
        number: Int,
        state: String
    ): RepositoryIssueDetail

    suspend fun setIssueLabels(
        owner: String,
        repo: String,
        number: Int,
        labelNames: List<String>
    ): RepositoryIssueDetail

    suspend fun createIssueComment(
        owner: String,
        repo: String,
        number: Int,
        body: String
    ): RepositoryIssueComment

    suspend fun updateIssueComment(
        owner: String,
        repo: String,
        commentId: Long,
        body: String
    ): RepositoryIssueComment

    suspend fun deleteIssueComment(owner: String, repo: String, commentId: Long)

    suspend fun getEditableFile(owner: String, repo: String, path: String): EditableRepositoryFile

    suspend fun getWriteTarget(owner: String, repo: String, path: String): RepositoryFileWriteTarget?

    suspend fun createFileContent(
        owner: String,
        repo: String,
        path: String,
        message: String,
        content: String,
        branch: String? = null
    ): FileContentWriteResult

    suspend fun createFileContentBytes(
        owner: String,
        repo: String,
        path: String,
        message: String,
        contentBytes: ByteArray,
        branch: String? = null
    ): FileContentWriteResult

    suspend fun updateFileContent(
        owner: String,
        repo: String,
        path: String,
        message: String,
        content: String,
        sha: String,
        branch: String? = null
    ): FileContentWriteResult

    suspend fun updateFileContentBytes(
        owner: String,
        repo: String,
        path: String,
        message: String,
        contentBytes: ByteArray,
        sha: String,
        branch: String? = null
    ): FileContentWriteResult
    suspend fun deleteFileContent(
        owner: String,
        repo: String,
        path: String,
        message: String,
        sha: String,
        branch: String? = null
    ): FileContentWriteResult

    suspend fun createFork(owner: String, repo: String): GitHubRepository

    suspend fun createFork(
        owner: String,
        repo: String,
        targetOwner: String? = null,
        targetName: String? = null,
        defaultBranchOnly: Boolean = false
    ): GitHubRepository

    suspend fun findCurrentAccountFork(owner: String, repo: String, currentAccountLogin: String): GitHubRepository?

    suspend fun isRepositoryNameAvailable(owner: String, repo: String): Boolean

    suspend fun updateRepositoryDescription(owner: String, repo: String, description: String?): GitHubRepository

    suspend fun getForkSyncStatus(owner: String, repo: String): RepositoryForkSyncStatus

    suspend fun syncFork(owner: String, repo: String, branch: String): RepositoryForkSyncStatus

    suspend fun isStarred(owner: String, repo: String): Boolean


    suspend fun isWatching(owner: String, repo: String): Boolean

    suspend fun star(owner: String, repo: String)

    suspend fun unstar(owner: String, repo: String)

    suspend fun watch(owner: String, repo: String)

    suspend fun unwatch(owner: String, repo: String)
}