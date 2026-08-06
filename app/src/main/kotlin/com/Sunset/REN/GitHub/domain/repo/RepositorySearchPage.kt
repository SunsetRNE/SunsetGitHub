package com.Sunset.REN.GitHub.domain.repo

/**
 * 仓库搜索的一页结果。
 *
 * 对应 GitHub `/search/repositories` 接口：返回总命中数与本页条目。
 * GitHub 搜索结果总数上限为 1000 条，超出部分接口不会再返回，[totalCount] 仅作展示参考。
 */
data class RepositorySearchPage(
    val repositories: List<GitHubRepository>,
    val totalCount: Int,
    val incompleteResults: Boolean,
    val page: Int,
    val perPage: Int
) {
    /** 是否可能还有下一页：本页填满且未触及 GitHub 搜索 1000 条上限。 */
    val canLoadMore: Boolean
        get() = repositories.size >= perPage && page * perPage < SearchResultHardLimit

    companion object {
        const val SearchResultHardLimit = 1000
    }
}
