package com.Sunset.REN.GitHub.data.local

import android.content.Context
import androidx.core.content.edit
import com.Sunset.REN.GitHub.data.github.html.RepositorySettingsSnapshot
import org.json.JSONObject

class RepositorySettingsCacheStore(context: Context) {

    private val sharedPreferences = context.applicationContext.getSharedPreferences(
        PreferencesName,
        Context.MODE_PRIVATE
    )

    fun getCachedSettings(owner: String, repo: String): RepositorySettingsCacheSnapshot? {
        val rawValue = sharedPreferences.getString(buildKey(owner, repo), null).orEmpty()
        if (rawValue.isBlank()) return null
        return runCatching {
            val json = JSONObject(rawValue)
            RepositorySettingsCacheSnapshot(
                snapshot = json.getJSONObject(KeySnapshot).toSnapshot(),
                refreshedAtMillis = json.optLong(KeyRefreshedAtMillis, 0L)
            )
        }.getOrNull()
    }
    fun cacheSettings(owner: String, repo: String, snapshot: RepositorySettingsSnapshot, refreshedAtMillis: Long) {
        val json = JSONObject()
            .put(KeySnapshot, snapshot.toJson())
            .put(KeyRefreshedAtMillis, refreshedAtMillis)
        sharedPreferences.edit {
            putString(buildKey(owner, repo), json.toString())
        }
    }

    fun clearCachedSettings(owner: String, repo: String) {
        sharedPreferences.edit {
            remove(buildKey(owner, repo))
        }
    }


    private fun buildKey(owner: String, repo: String): String {
        return "${owner.lowercase()}/${repo.lowercase()}"
    }

    private fun JSONObject.toSnapshot(): RepositorySettingsSnapshot {
        return RepositorySettingsSnapshot(
            owner = optString(KeyOwner, ""),
            repo = optString(KeyRepo, ""),
            name = optString(KeyName, optString(KeyRepo, "")),
            fullName = optString(KeyFullName, ""),
            description = optString(KeyDescription, ""),
            homepage = optString(KeyHomepage, ""),
            defaultBranch = optString(KeyDefaultBranch, ""),
            visibilityLabel = optString(KeyVisibilityLabel, ""),
            permissionLabel = optString(KeyPermissionLabel, ""),
            licenseLabel = optString(KeyLicenseLabel, ""),
            languageLabel = optString(KeyLanguageLabel, ""),
            createdAt = optString(KeyCreatedAt, ""),
            updatedAt = optString(KeyUpdatedAt, ""),
            pushedAt = optString(KeyPushedAt, ""),
            stargazersCount = optInt(KeyStargazersCount, 0),
            forksCount = optInt(KeyForksCount, 0),
            openIssuesCount = optInt(KeyOpenIssuesCount, 0),
            hasIssues = optBoolean(KeyHasIssues, false),
            hasProjects = optBoolean(KeyHasProjects, false),
            hasWiki = optBoolean(KeyHasWiki, false),
            hasDiscussions = optBoolean(KeyHasDiscussions, false),
            allowForking = optBoolean(KeyAllowForking, false),
            archived = optBoolean(KeyArchived, false),
            allowSquashMerge = optBoolean(KeyAllowSquashMerge, false),
            allowMergeCommit = optBoolean(KeyAllowMergeCommit, false),
            allowRebaseMerge = optBoolean(KeyAllowRebaseMerge, false),
            deleteBranchOnMerge = optBoolean(KeyDeleteBranchOnMerge, false),
            allowAutoMerge = optBoolean(KeyAllowAutoMerge, false),
            canAdmin = optBoolean(KeyCanAdmin, false),
            canPush = optBoolean(KeyCanPush, false),
            sourceUrl = optString(KeySourceUrl, "")
        )
    }

    private fun RepositorySettingsSnapshot.toJson(): JSONObject {
        return JSONObject()
            .put(KeyOwner, owner)
            .put(KeyRepo, repo)
            .put(KeyName, name)
            .put(KeyFullName, fullName)
            .put(KeyDescription, description)
            .put(KeyHomepage, homepage)
            .put(KeyDefaultBranch, defaultBranch)
            .put(KeyVisibilityLabel, visibilityLabel)
            .put(KeyPermissionLabel, permissionLabel)
            .put(KeyLicenseLabel, licenseLabel)
            .put(KeyLanguageLabel, languageLabel)
            .put(KeyCreatedAt, createdAt)
            .put(KeyUpdatedAt, updatedAt)
            .put(KeyPushedAt, pushedAt)
            .put(KeyStargazersCount, stargazersCount)
            .put(KeyForksCount, forksCount)
            .put(KeyOpenIssuesCount, openIssuesCount)
            .put(KeyHasIssues, hasIssues)
            .put(KeyHasProjects, hasProjects)
            .put(KeyHasWiki, hasWiki)
            .put(KeyHasDiscussions, hasDiscussions)
            .put(KeyAllowForking, allowForking)
            .put(KeyArchived, archived)
            .put(KeyAllowSquashMerge, allowSquashMerge)
            .put(KeyAllowMergeCommit, allowMergeCommit)
            .put(KeyAllowRebaseMerge, allowRebaseMerge)
            .put(KeyDeleteBranchOnMerge, deleteBranchOnMerge)
            .put(KeyAllowAutoMerge, allowAutoMerge)
            .put(KeyCanAdmin, canAdmin)
            .put(KeyCanPush, canPush)
            .put(KeySourceUrl, sourceUrl)
    }

    private companion object {
        const val PreferencesName = "repository_settings_cache_preferences"
        const val KeySnapshot = "snapshot"
        const val KeyRefreshedAtMillis = "refreshed_at_millis"

        const val KeyOwner = "owner"
        const val KeyRepo = "repo"
        const val KeyName = "name"
        const val KeyFullName = "full_name"
        const val KeyDescription = "description"
        const val KeyHomepage = "homepage"
        const val KeyDefaultBranch = "default_branch"
        const val KeyVisibilityLabel = "visibility_label"
        const val KeyPermissionLabel = "permission_label"
        const val KeyLicenseLabel = "license_label"
        const val KeyLanguageLabel = "language_label"
        const val KeyCreatedAt = "created_at"
        const val KeyUpdatedAt = "updated_at"
        const val KeyPushedAt = "pushed_at"
        const val KeyStargazersCount = "stargazers_count"
        const val KeyForksCount = "forks_count"
        const val KeyOpenIssuesCount = "open_issues_count"
        const val KeyHasIssues = "has_issues"
        const val KeyHasProjects = "has_projects"
        const val KeyHasWiki = "has_wiki"
        const val KeyHasDiscussions = "has_discussions"
        const val KeyAllowForking = "allow_forking"
        const val KeyArchived = "archived"
        const val KeyAllowSquashMerge = "allow_squash_merge"
        const val KeyAllowMergeCommit = "allow_merge_commit"
        const val KeyAllowRebaseMerge = "allow_rebase_merge"
        const val KeyDeleteBranchOnMerge = "delete_branch_on_merge"
        const val KeyAllowAutoMerge = "allow_auto_merge"
        const val KeyCanAdmin = "can_admin"
        const val KeyCanPush = "can_push"
        const val KeySourceUrl = "source_url"
    }
}

data class RepositorySettingsCacheSnapshot(
    val snapshot: RepositorySettingsSnapshot,
    val refreshedAtMillis: Long
)
