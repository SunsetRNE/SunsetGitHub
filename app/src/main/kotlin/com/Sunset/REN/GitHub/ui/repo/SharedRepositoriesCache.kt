package com.Sunset.REN.GitHub.ui.repo

import com.Sunset.REN.GitHub.domain.repo.GitHubRepository

data class RepositoriesCacheEntry(
    val repositories: List<GitHubRepository>,
    val currentPage: Int,
    val canLoadMore: Boolean,
    val refreshedAtMillis: Long
)

object SharedRepositoriesCache {
    private val entries = mutableMapOf<Long, RepositoriesCacheEntry>()

    operator fun get(accountId: Long): RepositoriesCacheEntry? {
        return entries[accountId]
    }

    operator fun set(accountId: Long, entry: RepositoriesCacheEntry) {
        entries[accountId] = entry
    }

    fun addOrUpdateRepository(accountId: Long, repository: GitHubRepository): RepositoriesCacheEntry? {
        val current = entries[accountId] ?: return null
        val updatedRepositories = listOf(repository) + current.repositories.filterNot { cached ->
            cached.fullName.equals(repository.fullName, ignoreCase = true)
        }
        return current.copy(repositories = updatedRepositories).also { entries[accountId] = it }
    }

    fun findRepository(accountId: Long, owner: String, repo: String): GitHubRepository? {
        return entries[accountId]
            ?.repositories
            ?.firstOrNull { repository ->
                repository.ownerLogin.equals(owner, ignoreCase = true) &&
                    repository.name.equals(repo, ignoreCase = true)
            }
    }
}
