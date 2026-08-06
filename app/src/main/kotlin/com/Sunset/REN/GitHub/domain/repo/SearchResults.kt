package com.Sunset.REN.GitHub.domain.repo

/**
 * 全局搜索中「用户」结果的一页。
 *
 * 对应 GitHub `/search/users` 接口。GitHub 搜索结果总数上限为 1000 条，[totalCount] 仅作展示参考。
 */
data class UserSearchPage(
    val users: List<GitHubUserSearchItem>,
    val totalCount: Int,
    val incompleteResults: Boolean,
    val page: Int,
    val perPage: Int
) {
    /** 是否可能还有下一页：本页填满且未触及 GitHub 搜索 1000 条上限。 */
    val canLoadMore: Boolean
        get() = users.size >= perPage && page * perPage < SearchResultHardLimit

    companion object {
        const val SearchResultHardLimit = 1000
    }
}

/** 用户搜索结果项：GitHub `/search/users` 返回的轻量用户对象。 */
data class GitHubUserSearchItem(
    val login: String,
    val avatarUrl: String?,
    val type: String?,
    val htmlUrl: String?
)

/**
 * 全局搜索中「Issue / PR」结果的一页。
 *
 * 对应 GitHub `/search/issues` 接口（同时覆盖 issue 与 pull request）。
 * GitHub 搜索结果总数上限为 1000 条，[totalCount] 仅作展示参考。
 */
data class IssueSearchPage(
    val issues: List<GitHubIssueSearchItem>,
    val totalCount: Int,
    val incompleteResults: Boolean,
    val page: Int,
    val perPage: Int
) {
    val canLoadMore: Boolean
        get() = issues.size >= perPage && page * perPage < SearchResultHardLimit

    companion object {
        const val SearchResultHardLimit = 1000
    }
}

/**
 * Issue 搜索结果项。
 *
 * [repositoryOwner] / [repositoryName] 由接口返回的 `repository_url`
 * （形如 `https://api.github.com/repos/{owner}/{repo}`）解析得出，用于点击后跳转 Issue 详情。
 */
data class GitHubIssueSearchItem(
    val number: Int,
    val title: String,
    val state: String,
    val authorLogin: String,
    val commentCount: Int,
    val repositoryOwner: String,
    val repositoryName: String,
    val isPullRequest: Boolean,
    val htmlUrl: String?
) {
    val repositoryFullName: String
        get() = if (repositoryOwner.isNotBlank() && repositoryName.isNotBlank()) {
            "$repositoryOwner/$repositoryName"
        } else {
            ""
        }
}

/**
 * 全局搜索中「代码」结果的一页。
 *
 * 对应 GitHub `/search/code` 接口。该接口速率限制较严（约 10 次/分钟），且仅索引部分仓库的默认分支，
 * [totalCount] 仅作展示参考。
 */
data class CodeSearchPage(
    val items: List<GitHubCodeSearchItem>,
    val totalCount: Int,
    val incompleteResults: Boolean,
    val page: Int,
    val perPage: Int
) {
    val canLoadMore: Boolean
        get() = items.size >= perPage && page * perPage < SearchResultHardLimit

    companion object {
        const val SearchResultHardLimit = 1000
    }
}

/**
 * 代码搜索结果项：命中文件的名称、路径与所属仓库。
 *
 * GitHub `/search/code` 默认不返回代码片段内容，点击后跳转到该文件的预览页查看完整内容。
 */
data class GitHubCodeSearchItem(
    val name: String,
    val path: String,
    val repositoryOwner: String,
    val repositoryName: String,
    val htmlUrl: String?
) {
    val repositoryFullName: String
        get() = if (repositoryOwner.isNotBlank() && repositoryName.isNotBlank()) {
            "$repositoryOwner/$repositoryName"
        } else {
            ""
        }
}