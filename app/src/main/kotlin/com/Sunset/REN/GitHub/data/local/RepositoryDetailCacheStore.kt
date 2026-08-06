package com.Sunset.REN.GitHub.data.local

import android.content.Context
import androidx.core.content.edit
import com.Sunset.REN.GitHub.domain.repo.GitHubRepository
import com.Sunset.REN.GitHub.domain.repo.RepositoryContentItem
import com.Sunset.REN.GitHub.domain.repo.RepositoryContributor
import com.Sunset.REN.GitHub.domain.repo.RepositoryLanguage
import com.Sunset.REN.GitHub.domain.repo.RepositoryRelease
import com.Sunset.REN.GitHub.domain.repo.RepositorySidebarInfo
import org.json.JSONArray
import org.json.JSONObject

/**
 * 仓库详情页首屏快照的持久化缓存。
 *
 * 内存缓存只在进程存活期间有效，冷启动后会丢失，导致再次进入详情页需要全量网络请求。
 * 该存储把上一次成功加载的详情快照写入 SharedPreferences，使冷启动后能立即展示缓存内容，
 * 同时在后台静默刷新最新数据。
 */
class RepositoryDetailCacheStore(context: Context) {

    private val sharedPreferences = context.applicationContext.getSharedPreferences(
        PreferencesName,
        Context.MODE_PRIVATE
    )

    fun getCachedDetail(owner: String, repo: String): RepositoryDetailCacheSnapshot? {
        val rawValue = sharedPreferences.getString(buildKey(owner, repo), null).orEmpty()
        if (rawValue.isBlank()) return null
        return runCatching {
            val json = JSONObject(rawValue)
            RepositoryDetailCacheSnapshot(
                repository = json.getJSONObject(KeyRepository).toRepository(),
                contents = json.optJSONArray(KeyContents)?.toContentItems().orEmpty(),
                isStarred = if (json.isNull(KeyIsStarred)) null else json.optBoolean(KeyIsStarred),
                isWatching = if (json.isNull(KeyIsWatching)) null else json.optBoolean(KeyIsWatching),
                sidebarInfo = json.optJSONObject(KeySidebarInfo)?.toSidebarInfo(),
                refreshedAtMillis = json.optLong(KeyRefreshedAtMillis, 0L)
            )
        }.getOrNull()
    }

    fun cacheDetail(owner: String, repo: String, snapshot: RepositoryDetailCacheSnapshot) {
        val json = JSONObject()
            .put(KeyRepository, snapshot.repository.toJson())
            .put(KeyContents, snapshot.contents.toContentsJsonArray())
            .put(KeyIsStarred, snapshot.isStarred)
            .put(KeyIsWatching, snapshot.isWatching)
            .put(KeySidebarInfo, snapshot.sidebarInfo?.toJson())
            .put(KeyRefreshedAtMillis, snapshot.refreshedAtMillis)
        sharedPreferences.edit {
            putString(buildKey(owner, repo), json.toString())
        }
    }

    fun getCachedContents(owner: String, repo: String, path: String): RepositoryContentsCacheSnapshot? {
        val rawValue = sharedPreferences.getString(buildContentsKey(owner, repo, path), null).orEmpty()
        if (rawValue.isBlank()) return null
        return runCatching {
            val json = JSONObject(rawValue)
            RepositoryContentsCacheSnapshot(
                path = json.optString(KeyPath, normalizePath(path)),
                contents = json.optJSONArray(KeyContents)?.toContentItems().orEmpty(),
                refreshedAtMillis = json.optLong(KeyRefreshedAtMillis, 0L)
            )
        }.getOrNull()
    }

    fun cacheContents(owner: String, repo: String, path: String, contents: List<RepositoryContentItem>) {
        val normalizedPath = normalizePath(path)
        val json = JSONObject()
            .put(KeyPath, normalizedPath)
            .put(KeyContents, contents.toContentsJsonArray())
            .put(KeyRefreshedAtMillis, System.currentTimeMillis())
        sharedPreferences.edit {
            putString(buildContentsKey(owner, repo, normalizedPath), json.toString())
        }
    }

    fun removeContents(owner: String, repo: String, path: String) {
        sharedPreferences.edit {
            remove(buildContentsKey(owner, repo, path))
        }
    }

    private fun buildKey(owner: String, repo: String): String {
        return "${owner.lowercase()}/${repo.lowercase()}"
    }

    private fun buildContentsKey(owner: String, repo: String, path: String): String {
        return "contents:${buildKey(owner, repo)}:${normalizePath(path)}"
    }

    private fun normalizePath(path: String): String {
        return path.trim('/')
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
            .put(KeyHtmlUrl, htmlUrl)
    }

    private fun JSONArray.toContentItems(): List<RepositoryContentItem> {
        return buildList {
            for (index in 0 until length()) {
                val item = getJSONObject(index)
                when (item.optString(KeyContentType)) {
                    ContentTypeDirectory -> add(
                        RepositoryContentItem.Directory(
                            name = item.optString(KeyContentName, ""),
                            path = item.optString(KeyContentPath, ""),
                            htmlUrl = item.optionalString(KeyContentHtmlUrl)
                        )
                    )

                    ContentTypeFile -> add(
                        RepositoryContentItem.File(
                            name = item.optString(KeyContentName, ""),
                            path = item.optString(KeyContentPath, ""),
                            sizeBytes = item.optLong(KeyContentSizeBytes, 0L),
                            downloadUrl = item.optionalString(KeyContentDownloadUrl),
                            htmlUrl = item.optionalString(KeyContentHtmlUrl)
                        )
                    )

                    ContentTypeUnsupported -> add(
                        RepositoryContentItem.Unsupported(
                            name = item.optString(KeyContentName, ""),
                            path = item.optString(KeyContentPath, ""),
                            reason = item.optString(KeyContentReason, ""),
                            htmlUrl = item.optionalString(KeyContentHtmlUrl)
                        )
                    )
                }
            }
        }
    }

    private fun List<RepositoryContentItem>.toContentsJsonArray(): JSONArray {
        return JSONArray().also { array ->
            forEach { item ->
                val json = JSONObject()
                    .put(KeyContentName, item.name)
                    .put(KeyContentPath, item.path)
                    .put(KeyContentHtmlUrl, item.htmlUrl)
                when (item) {
                    is RepositoryContentItem.Directory -> json.put(KeyContentType, ContentTypeDirectory)
                    is RepositoryContentItem.File -> json
                        .put(KeyContentType, ContentTypeFile)
                        .put(KeyContentSizeBytes, item.sizeBytes)
                        .put(KeyContentDownloadUrl, item.downloadUrl)

                    is RepositoryContentItem.Unsupported -> json
                        .put(KeyContentType, ContentTypeUnsupported)
                        .put(KeyContentReason, item.reason)
                }
                array.put(json)
            }
        }
    }

    private fun JSONObject.toSidebarInfo(): RepositorySidebarInfo {
        return RepositorySidebarInfo(
            releases = optJSONArray(KeyReleases)?.toReleases().orEmpty(),
            contributors = optJSONArray(KeyContributors)?.toContributors().orEmpty(),
            languages = optJSONArray(KeyLanguages)?.toRepositoryLanguages().orEmpty(),
            error = optionalString(KeyError)
        )
    }

    private fun RepositorySidebarInfo.toJson(): JSONObject {
        return JSONObject()
            .put(KeyReleases, releases.toReleasesJsonArray())
            .put(KeyContributors, contributors.toContributorsJsonArray())
            .put(KeyLanguages, languages.toRepositoryLanguagesJsonArray())
            .put(KeyError, error)
    }

    private fun JSONArray.toReleases(): List<RepositoryRelease> {
        return buildList {
            for (index in 0 until length()) {
                val release = getJSONObject(index)
                add(
                    RepositoryRelease(
                        name = release.optString(KeyReleaseName, ""),
                        tagName = release.optString(KeyReleaseTagName, ""),
                        htmlUrl = release.optionalString(KeyReleaseHtmlUrl),
                        publishedAt = release.optionalString(KeyReleasePublishedAt),
                        isLatest = release.optBoolean(KeyReleaseIsLatest, false),
                        isPrerelease = release.optBoolean(KeyReleaseIsPrerelease, false),
                        isDraft = release.optBoolean(KeyReleaseIsDraft, false),
                        bodySummary = release.optionalString(KeyReleaseBodySummary)
                    )
                )
            }
        }
    }

    private fun List<RepositoryRelease>.toReleasesJsonArray(): JSONArray {
        return JSONArray().also { array ->
            forEach { release ->
                array.put(
                    JSONObject()
                        .put(KeyReleaseName, release.name)
                        .put(KeyReleaseTagName, release.tagName)
                        .put(KeyReleaseHtmlUrl, release.htmlUrl)
                        .put(KeyReleasePublishedAt, release.publishedAt)
                        .put(KeyReleaseIsLatest, release.isLatest)
                        .put(KeyReleaseIsPrerelease, release.isPrerelease)
                        .put(KeyReleaseIsDraft, release.isDraft)
                        .put(KeyReleaseBodySummary, release.bodySummary)
                )
            }
        }
    }

    private fun JSONArray.toContributors(): List<RepositoryContributor> {
        return buildList {
            for (index in 0 until length()) {
                val contributor = getJSONObject(index)
                add(
                    RepositoryContributor(
                        login = contributor.optString(KeyContributorLogin, ""),
                        contributions = contributor.optInt(KeyContributorContributions, 0),
                        htmlUrl = contributor.optionalString(KeyContributorHtmlUrl)
                    )
                )
            }
        }
    }

    private fun List<RepositoryContributor>.toContributorsJsonArray(): JSONArray {
        return JSONArray().also { array ->
            forEach { contributor ->
                array.put(
                    JSONObject()
                        .put(KeyContributorLogin, contributor.login)
                        .put(KeyContributorContributions, contributor.contributions)
                        .put(KeyContributorHtmlUrl, contributor.htmlUrl)
                )
            }
        }
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
        const val PreferencesName = "repository_detail_cache_preferences"
        const val KeyRepository = "repository"
        const val KeyContents = "contents"
        const val KeyIsStarred = "is_starred"
        const val KeyIsWatching = "is_watching"
        const val KeySidebarInfo = "sidebar_info"
        const val KeyRefreshedAtMillis = "refreshed_at_millis"
        const val KeyPath = "path"

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
        const val KeyOwnerAvatarUrl = "owner_avatar_url"
        const val KeyOwnerName = "owner_name"
        const val KeyOwnerType = "owner_type"
        const val KeyParentFullName = "parent_full_name"
        const val KeyParentDefaultBranch = "parent_default_branch"
        const val KeySourceFullName = "source_full_name"
        const val KeyHtmlUrl = "html_url"

        const val KeyContentType = "content_type"
        const val KeyContentName = "content_name"
        const val KeyContentPath = "content_path"
        const val KeyContentHtmlUrl = "content_html_url"
        const val KeyContentSizeBytes = "content_size_bytes"
        const val KeyContentDownloadUrl = "content_download_url"
        const val KeyContentReason = "content_reason"
        const val ContentTypeDirectory = "directory"
        const val ContentTypeFile = "file"
        const val ContentTypeUnsupported = "unsupported"

        const val KeyReleases = "releases"
        const val KeyReleaseName = "release_name"
        const val KeyReleaseTagName = "release_tag_name"
        const val KeyReleaseHtmlUrl = "release_html_url"
        const val KeyReleasePublishedAt = "release_published_at"
        const val KeyReleaseIsLatest = "release_is_latest"
        const val KeyReleaseIsPrerelease = "release_is_prerelease"
        const val KeyReleaseIsDraft = "release_is_draft"
        const val KeyReleaseBodySummary = "release_body_summary"
        const val KeyContributors = "contributors"
        const val KeyContributorLogin = "contributor_login"
        const val KeyContributorContributions = "contributor_contributions"
        const val KeyContributorHtmlUrl = "contributor_html_url"
        const val KeyError = "error"
        const val KeyLanguageName = "language_name"
        const val KeyLanguageBytes = "language_bytes"
        const val KeyLanguagePercentage = "language_percentage"
    }
}

data class RepositoryDetailCacheSnapshot(
    val repository: GitHubRepository,
    val contents: List<RepositoryContentItem>,
    val isStarred: Boolean?,
    val isWatching: Boolean?,
    val sidebarInfo: RepositorySidebarInfo?,
    val refreshedAtMillis: Long
)

data class RepositoryContentsCacheSnapshot(
    val path: String,
    val contents: List<RepositoryContentItem>,
    val refreshedAtMillis: Long
)