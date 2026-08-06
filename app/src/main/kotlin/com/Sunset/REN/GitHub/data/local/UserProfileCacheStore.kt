package com.Sunset.REN.GitHub.data.local

import android.content.Context
import androidx.core.content.edit
import com.Sunset.REN.GitHub.domain.profile.GitHubUserProfile
import org.json.JSONObject

class UserProfileCacheStore(context: Context) {

    private val sharedPreferences = context.applicationContext.getSharedPreferences(
        PreferencesName,
        Context.MODE_PRIVATE
    )

    fun getCachedProfile(accountId: Long): CachedUserProfile? {
        val rawValue = sharedPreferences.getString(buildAccountKey(accountId), null).orEmpty()
        if (rawValue.isBlank()) return null
        return runCatching {
            val json = JSONObject(rawValue)
            CachedUserProfile(
                profile = json.getJSONObject(KeyProfile).toUserProfile(),
                refreshedAtMillis = json.optLong(KeyRefreshedAtMillis, 0L)
            )
        }.getOrNull()
    }

    fun cacheProfile(accountId: Long, profile: GitHubUserProfile, refreshedAtMillis: Long) {
        val json = JSONObject()
            .put(KeyProfile, profile.toJson())
            .put(KeyRefreshedAtMillis, refreshedAtMillis)
        sharedPreferences.edit {
            putString(buildAccountKey(accountId), json.toString())
        }
    }

    private fun buildAccountKey(accountId: Long): String {
        return "$KeyProfile:$accountId"
    }

    private fun JSONObject.toUserProfile(): GitHubUserProfile {
        return GitHubUserProfile(
            id = optLong(KeyId, 0L),
            login = optString(KeyLogin, ""),
            name = optionalString(KeyName),
            avatarUrl = optionalString(KeyAvatarUrl),
            bio = optionalString(KeyBio),
            company = optionalString(KeyCompany),
            location = optionalString(KeyLocation),
            blog = optionalString(KeyBlog),
            email = optionalString(KeyEmail),
            twitterUsername = optionalString(KeyTwitterUsername),
            publicRepos = optInt(KeyPublicRepos, 0),
            publicGists = optInt(KeyPublicGists, 0),
            followers = optInt(KeyFollowers, 0),
            following = optInt(KeyFollowing, 0),
            htmlUrl = optString(KeyHtmlUrl, ""),
            createdAt = optionalString(KeyCreatedAt),
            updatedAt = optionalString(KeyUpdatedAt)
        )
    }

    private fun GitHubUserProfile.toJson(): JSONObject {
        return JSONObject()
            .put(KeyId, id)
            .put(KeyLogin, login)
            .put(KeyName, name)
            .put(KeyAvatarUrl, avatarUrl)
            .put(KeyBio, bio)
            .put(KeyCompany, company)
            .put(KeyLocation, location)
            .put(KeyBlog, blog)
            .put(KeyEmail, email)
            .put(KeyTwitterUsername, twitterUsername)
            .put(KeyPublicRepos, publicRepos)
            .put(KeyPublicGists, publicGists)
            .put(KeyFollowers, followers)
            .put(KeyFollowing, following)
            .put(KeyHtmlUrl, htmlUrl)
            .put(KeyCreatedAt, createdAt)
            .put(KeyUpdatedAt, updatedAt)
    }

    private fun JSONObject.optionalString(key: String): String? {
        return if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
    }

    data class CachedUserProfile(
        val profile: GitHubUserProfile,
        val refreshedAtMillis: Long
    )

    private companion object {
        const val PreferencesName = "user_profile_cache_preferences"
        const val KeyProfile = "profile"
        const val KeyRefreshedAtMillis = "refreshed_at_millis"
        const val KeyId = "id"
        const val KeyLogin = "login"
        const val KeyName = "name"
        const val KeyAvatarUrl = "avatar_url"
        const val KeyBio = "bio"
        const val KeyCompany = "company"
        const val KeyLocation = "location"
        const val KeyBlog = "blog"
        const val KeyEmail = "email"
        const val KeyTwitterUsername = "twitter_username"
        const val KeyPublicRepos = "public_repos"
        const val KeyPublicGists = "public_gists"
        const val KeyFollowers = "followers"
        const val KeyFollowing = "following"
        const val KeyHtmlUrl = "html_url"
        const val KeyCreatedAt = "created_at"
        const val KeyUpdatedAt = "updated_at"
    }
}