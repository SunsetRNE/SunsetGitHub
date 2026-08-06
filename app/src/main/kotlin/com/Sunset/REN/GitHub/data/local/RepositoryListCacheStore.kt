package com.Sunset.REN.GitHub.data.local

import android.content.Context
import androidx.core.content.edit
import com.Sunset.REN.GitHub.domain.repo.GitHubRepository
import com.Sunset.REN.GitHub.domain.repo.RepositoryLanguage
import com.Sunset.REN.GitHub.ui.repo.RepositoriesCacheEntry
import org.json.JSONArray
import org.json.JSONObject

class RepositoryListCacheStore(context: Context) {

    private val sharedPreferences = context.applicationContext.getSharedPreferences(
        PreferencesName,
        Context.MODE_PRIVATE
    )

    fun getCachedRepositories(accountId: Long): RepositoriesCacheEntry? {
        val rawValue = sharedPreferences.getString(buildAccountKey(accountId), null).orEmpty()
        if (rawValue.isBlank()) return null
        return runCatching {
            val json = JSONObject(rawValue)
            RepositoriesCacheEntry(
                repositories = json.getJSONArray(KeyRepositories).toRepositories(),
                currentPage = json.optInt(KeyCurrentPage, 1),
                canLoadMore = json.optBoolean(KeyCanLoadMore, false),
                refreshedAtMillis = json.optLong(KeyRefreshedAtMillis, 0L)
            )
        }.getOrNull()?.takeIf { it.repositories.isNotEmpty() }
    }

    fun cacheRepositories(accountId: Long, entry: RepositoriesCacheEntry) {
        val json = JSONObject()
            .put(KeyRepositories, entry.repositories.toJsonArray())
            .put(KeyCurrentPage, entry.currentPage)
            .put(KeyCanLoadMore, entry.canLoadMore)
            .put(KeyRefreshedAtMillis, entry.refreshedAtMillis)
        sharedPreferences.edit {
            putString(buildAccountKey(accountId), json.toString())
        }
    }

    private fun buildAccountKey(accountId: Long): String {
        return "$KeyRepositories:$accountId"
    }

    private fun JSONArray.toRepositories(): List<GitHubRepository> {
        return buildList {
            for (index in 0 until length()) {
                add(getJSONObject(index).toRepository())
            }
        }
    }

    private fun List<GitHubRepository>.toJsonArray(): JSONArray {
        return JSONArray().also { array ->
            forEach { repository ->
                array.put(repository.toJson())
            }
        }
    }

    private fun JSONObject.toRepository(): GitHubRepository {
        return GitHubRepository(
            id = optLong(KeyId, 0L),
            name = optString(KeyName, ""),
            fullName = optString(KeyFullName, ""),
            ownerLogin = optString(KeyOwnerLogin, ""),
            description = optionalString(KeyDescription),
            isPrivate = optBoolean(KeyIsPrivate, false),
            fork = optBoolean(KeyFork, false),
            archived = optBoolean(KeyArchived, false),
            defaultBranch = optString(KeyDefaultBranch, ""),
            stargazersCount = optInt(KeyStargazersCount, 0),
            watchersCount = optInt(KeyWatchersCount, 0),
            forksCount = optInt(KeyForksCount, 0),
            openIssuesCount = optInt(KeyOpenIssuesCount, 0),
            language = optionalString(KeyLanguage),
            languages = optJSONArray(KeyLanguages)?.toRepositoryLanguages().orEmpty(),
            ownerAvatarUrl = optionalString(KeyOwnerAvatarUrl),
            ownerName = optionalString(KeyOwnerName),
            ownerType = optionalString(KeyOwnerType),
            parentFullName = optionalString(KeyParentFullName),
            parentDefaultBranch = optionalString(KeyParentDefaultBranch),
            sourceFullName = optionalString(KeySourceFullName),
            updatedAt = optionalString(KeyUpdatedAt),
            pushedAt = optionalString(KeyPushedAt),
            htmlUrl = optString(KeyHtmlUrl, "")
        )
    }

    private fun GitHubRepository.toJson(): JSONObject {
        return JSONObject()
            .put(KeyId, id)
            .put(KeyName, name)
            .put(KeyFullName, fullName)
            .put(KeyOwnerLogin, ownerLogin)
            .put(KeyDescription, description)
            .put(KeyIsPrivate, isPrivate)
            .put(KeyFork, fork)
            .put(KeyArchived, archived)
            .put(KeyDefaultBranch, defaultBranch)
            .put(KeyStargazersCount, stargazersCount)
            .put(KeyWatchersCount, watchersCount)
            .put(KeyForksCount, forksCount)
            .put(KeyOpenIssuesCount, openIssuesCount)
            .put(KeyLanguage, language)
            .put(KeyLanguages, languages.toRepositoryLanguagesJsonArray())
            .put(KeyOwnerAvatarUrl, ownerAvatarUrl)
            .put(KeyOwnerName, ownerName)
            .put(KeyOwnerType, ownerType)
            .put(KeyParentFullName, parentFullName)
            .put(KeyParentDefaultBranch, parentDefaultBranch)
            .put(KeySourceFullName, sourceFullName)
            .put(KeyUpdatedAt, updatedAt)
            .put(KeyPushedAt, pushedAt)
            .put(KeyHtmlUrl, htmlUrl)
    }

    private fun JSONArray.toRepositoryLanguages(): List<RepositoryLanguage> {
        return buildList {
            for (index in 0 until length()) {
                val language = getJSONObject(index)
                add(
                    RepositoryLanguage(
                        name = language.optString(KeyLanguageName, ""),
                        bytes = language.optLong(KeyLanguageBytes, 0L),
                        percentage = language.optInt(KeyLanguagePercentage, 0)
                    )
                )
            }
        }.filter { it.name.isNotBlank() }
    }

    private fun List<RepositoryLanguage>.toRepositoryLanguagesJsonArray(): JSONArray {
        return JSONArray().also { array ->
            forEach { language ->
                array.put(
                    JSONObject()
                        .put(KeyLanguageName, language.name)
                        .put(KeyLanguageBytes, language.bytes)
                        .put(KeyLanguagePercentage, language.percentage)
                )
            }
        }
    }

    private fun JSONObject.optionalString(key: String): String? {
        return if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
    }

    private companion object {
        const val PreferencesName = "repository_list_cache_preferences"
        const val KeyRepositories = "repositories"
        const val KeyCurrentPage = "current_page"
        const val KeyCanLoadMore = "can_load_more"
        const val KeyRefreshedAtMillis = "refreshed_at_millis"
        const val KeyId = "id"
        const val KeyName = "name"
        const val KeyFullName = "full_name"
        const val KeyOwnerLogin = "owner_login"
        const val KeyDescription = "description"
        const val KeyIsPrivate = "is_private"
        const val KeyFork = "fork"
        const val KeyArchived = "archived"
        const val KeyDefaultBranch = "default_branch"
        const val KeyStargazersCount = "stargazers_count"
        const val KeyWatchersCount = "watchers_count"
        const val KeyForksCount = "forks_count"
        const val KeyOpenIssuesCount = "open_issues_count"
        const val KeyLanguage = "language"
        const val KeyLanguages = "languages"
        const val KeyLanguageName = "name"
        const val KeyLanguageBytes = "bytes"
        const val KeyLanguagePercentage = "percentage"
        const val KeyOwnerAvatarUrl = "owner_avatar_url"
        const val KeyOwnerName = "owner_name"
        const val KeyOwnerType = "owner_type"
        const val KeyParentFullName = "parent_full_name"
        const val KeyParentDefaultBranch = "parent_default_branch"
        const val KeySourceFullName = "source_full_name"
        const val KeyUpdatedAt = "updated_at"
        const val KeyPushedAt = "pushed_at"
        const val KeyHtmlUrl = "html_url"
    }
}