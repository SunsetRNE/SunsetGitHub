package com.Sunset.REN.GitHub.data.local

import android.content.Context
import com.Sunset.REN.GitHub.domain.repo.GitHubRepository
import com.Sunset.REN.GitHub.domain.repo.RepositoryContentItem
import com.Sunset.REN.GitHub.domain.repo.RepositoryDetailSecondaryResult
import com.Sunset.REN.GitHub.ui.repo.RepositoriesCacheEntry
import com.Sunset.REN.GitHub.ui.repo.SharedRepositoriesCache

class RepositoryCacheRepository(context: Context) {

    private val repositoryListCacheStore = RepositoryListCacheStore(context)
    private val repositoryDetailCacheStore = RepositoryDetailCacheStore(context)
    private val repositoryActionRunDetailCacheStore = RepositoryActionRunDetailCacheStore(context)

    fun getRepositoryList(accountId: Long): RepositoriesCacheEntry? {
        return SharedRepositoriesCache[accountId]
            ?: repositoryListCacheStore.getCachedRepositories(accountId)?.also { cachedEntry ->
                SharedRepositoriesCache[accountId] = cachedEntry
            }
    }

    fun cacheRepositoryList(accountId: Long, entry: RepositoriesCacheEntry) {
        SharedRepositoriesCache[accountId] = entry
        repositoryListCacheStore.cacheRepositories(accountId, entry)
    }

    fun addOrUpdateRepository(accountId: Long, repository: GitHubRepository) {
        val updatedEntry = SharedRepositoriesCache.addOrUpdateRepository(accountId, repository)
            ?: repositoryListCacheStore.getCachedRepositories(accountId)
                ?.let { cachedEntry ->
                    val updatedRepositories = listOf(repository) + cachedEntry.repositories.filterNot { cached ->
                        cached.fullName.equals(repository.fullName, ignoreCase = true)
                    }
                    cachedEntry.copy(repositories = updatedRepositories)
                }
        if (updatedEntry != null) {
            cacheRepositoryList(accountId, updatedEntry)
        }
    }

    fun findRepository(accountId: Long, owner: String, repo: String): GitHubRepository? {
        return SharedRepositoriesCache.findRepository(accountId, owner, repo)
            ?: repositoryListCacheStore.getCachedRepositories(accountId)
                ?.also { SharedRepositoriesCache[accountId] = it }
                ?.repositories
                ?.firstOrNull { repository ->
                    repository.ownerLogin.equals(owner, ignoreCase = true) &&
                        repository.name.equals(repo, ignoreCase = true)
                }
    }

    fun getDetail(owner: String, repo: String): RepositoryDetailSecondaryResult? {
        return detailEntries[buildRepositoryKey(owner, repo)]
    }

    fun cacheDetail(owner: String, repo: String, detail: RepositoryDetailSecondaryResult) {
        detailEntries[buildRepositoryKey(owner, repo)] = detail
    }

    fun getPersistedDetail(owner: String, repo: String): RepositoryDetailCacheSnapshot? {
        return repositoryDetailCacheStore.getCachedDetail(owner, repo)
    }

    fun cachePersistedDetail(owner: String, repo: String, snapshot: RepositoryDetailCacheSnapshot) {
        repositoryDetailCacheStore.cacheDetail(owner, repo, snapshot)
    }

    fun getContents(owner: String, repo: String, path: String): List<RepositoryContentItem>? {
        return contentsEntries[buildContentsKey(owner, repo, path)]
            ?: repositoryDetailCacheStore.getCachedContents(owner, repo, path)?.contents?.also { contents ->
                contentsEntries[buildContentsKey(owner, repo, path)] = contents
            }
    }

    fun cacheContents(owner: String, repo: String, path: String, contents: List<RepositoryContentItem>) {
        contentsEntries[buildContentsKey(owner, repo, path)] = contents
        repositoryDetailCacheStore.cacheContents(owner, repo, path, contents)
    }

    fun removeContents(owner: String, repo: String, path: String) {
        contentsEntries.remove(buildContentsKey(owner, repo, path))
        repositoryDetailCacheStore.removeContents(owner, repo, path)
    }

    fun getActionRunDetail(owner: String, repo: String, runId: Long): RepositoryActionRunDetailCacheSnapshot? {
        return actionRunDetailEntries[buildActionRunKey(owner, repo, runId)]
            ?: repositoryActionRunDetailCacheStore.getCachedActionRun(owner, repo, runId)?.also { snapshot ->
                actionRunDetailEntries[buildActionRunKey(owner, repo, runId)] = snapshot
            }
    }

    fun cacheActionRunDetail(owner: String, repo: String, runId: Long, snapshot: RepositoryActionRunDetailCacheSnapshot) {
        actionRunDetailEntries[buildActionRunKey(owner, repo, runId)] = snapshot
        repositoryActionRunDetailCacheStore.cacheActionRun(owner, repo, runId, snapshot)
    }

    fun buildRepositoryKey(owner: String, repo: String): String {
        return "${owner.lowercase()}/${repo.lowercase()}"
    }

    fun buildContentsKey(owner: String, repo: String, path: String): String {
        return "${buildRepositoryKey(owner, repo)}:${path.trim('/')}"
    }

    fun buildActionRunKey(owner: String, repo: String, runId: Long): String {
        return "${buildRepositoryKey(owner, repo)}:actions/runs/$runId"
    }

    private companion object {
        val detailEntries = mutableMapOf<String, RepositoryDetailSecondaryResult>()
        val contentsEntries = mutableMapOf<String, List<RepositoryContentItem>>()
        val actionRunDetailEntries = mutableMapOf<String, RepositoryActionRunDetailCacheSnapshot>()
    }
}
