package com.Sunset.REN.GitHub.domain.repo

import com.Sunset.REN.GitHub.data.github.GitHubRepositoryApiGateway
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull

class RepositoryDetailLoadUseCase {

    suspend fun loadPrimary(
        gateway: GitHubRepositoryApiGateway,
        owner: String,
        repo: String,
        rootPath: String
    ): RepositoryDetailPrimaryResult = coroutineScope {
        val repositoryDeferred = async { gateway.getRepository(owner, repo, includeLanguages = false) }
        val contentsDeferred = async { runCatching { gateway.listContents(owner, repo, rootPath) } }
        RepositoryDetailPrimaryResult(
            repository = repositoryDeferred.await(),
            contents = contentsDeferred.await()
        )
    }

    suspend fun loadSecondary(
        gateway: GitHubRepositoryApiGateway,
        owner: String,
        repo: String,
        baseRepository: GitHubRepository,
        rootPath: String,
        contents: List<RepositoryContentItem>,
        contentsError: String?
    ): RepositoryDetailSecondaryResult = coroutineScope {
        val starredDeferred = async {
            runTimedCatching(SecondaryFastRequestTimeoutMillis) { gateway.isStarred(owner, repo) }
        }
        val watchingDeferred = async {
            runTimedCatching(SecondaryFastRequestTimeoutMillis) { gateway.isWatching(owner, repo) }
        }
        val sidebarInfoDeferred = async {
            runTimedCatching(SecondarySidebarRequestTimeoutMillis) { gateway.getRepositorySidebarInfo(owner, repo) }
        }
        val permissionsDeferred = async {
            runTimedCatching(SecondaryFastRequestTimeoutMillis) { gateway.getRepositoryPermissions(owner, repo) }
        }
        val starredResult = starredDeferred.await()
        val watchingResult = watchingDeferred.await()
        val sidebarInfoResult = sidebarInfoDeferred.await()
        val permissionsResult = permissionsDeferred.await()
        val sidebarInfo = sidebarInfoResult.getOrElse { exception ->
            RepositorySidebarInfo(error = exception.message)
        }
        val repository = if (sidebarInfo.languages.isNotEmpty()) {
            baseRepository.copy(languages = sidebarInfo.languages)
        } else {
            baseRepository
        }
        RepositoryDetailSecondaryResult(
            repository = repository,
            currentPath = rootPath,
            contents = contents,
            contentsError = contentsError,
            isStarred = starredResult.getOrNull(),
            isWatching = watchingResult.getOrNull(),
            lightManagementStateError = listOfNotNull(
                starredResult.exceptionOrNull()?.message,
                watchingResult.exceptionOrNull()?.message
            ).joinToString(separator = "\n").takeIf { it.isNotBlank() },
            sidebarInfo = sidebarInfo,
            canPush = permissionsResult.getOrNull()?.canPush ?: false
        )
    }

    private suspend fun <T> runTimedCatching(timeoutMillis: Long, block: suspend () -> T): Result<T> {
        return runCatching {
            withTimeoutOrNull(timeoutMillis) { block() }
                ?: throw IllegalStateException(SecondaryTimeoutMessage)
        }
    }

    private companion object {
        const val SecondaryFastRequestTimeoutMillis = 6_000L
        const val SecondarySidebarRequestTimeoutMillis = 8_000L
        const val SecondaryTimeoutMessage = "仓库侧栏信息加载超时"
    }
}

data class RepositoryDetailPrimaryResult(
    val repository: GitHubRepository,
    val contents: Result<List<RepositoryContentItem>>
)

data class RepositoryDetailSecondaryResult(
    val repository: GitHubRepository,
    val currentPath: String,
    val contents: List<RepositoryContentItem>,
    val contentsError: String?,
    val isStarred: Boolean?,
    val isWatching: Boolean?,
    val lightManagementStateError: String?,
    val sidebarInfo: RepositorySidebarInfo?,
    val canPush: Boolean = false
)
