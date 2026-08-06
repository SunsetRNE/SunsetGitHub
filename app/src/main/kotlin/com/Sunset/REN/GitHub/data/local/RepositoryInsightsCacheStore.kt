package com.Sunset.REN.GitHub.data.local

import android.content.Context
import androidx.core.content.edit
import com.Sunset.REN.GitHub.data.github.html.RepositoryHtmlMetric
import com.Sunset.REN.GitHub.data.github.html.RepositoryHtmlSectionStatus
import com.Sunset.REN.GitHub.data.github.html.RepositoryHtmlSectionSummary
import org.json.JSONArray
import org.json.JSONObject

data class RepositoryInsightsCacheSnapshot(
    val summary: RepositoryHtmlSectionSummary,
    val refreshedAtMillis: Long
)

class RepositoryInsightsCacheStore(context: Context) {
    private val sharedPreferences = context.applicationContext.getSharedPreferences(
        PreferencesName,
        Context.MODE_PRIVATE
    )

    fun getCachedInsights(owner: String, repo: String): RepositoryInsightsCacheSnapshot? {
        val rawValue = sharedPreferences.getString(buildKey(owner, repo), null).orEmpty()
        if (rawValue.isBlank()) return null
        return runCatching {
            val json = JSONObject(rawValue)
            RepositoryInsightsCacheSnapshot(
                summary = json.getJSONObject(KeySummary).toSummary(),
                refreshedAtMillis = json.optLong(KeyRefreshedAtMillis, 0L)
            )
        }.getOrNull()
    }

    fun cacheInsights(owner: String, repo: String, summary: RepositoryHtmlSectionSummary, refreshedAtMillis: Long) {
        val json = JSONObject()
            .put(KeySummary, summary.toJson())
            .put(KeyRefreshedAtMillis, refreshedAtMillis)
        sharedPreferences.edit {
            putString(buildKey(owner, repo), json.toString())
        }
    }

    private fun RepositoryHtmlSectionSummary.toJson(): JSONObject {
        return JSONObject()
            .put(KeyOwner, owner)
            .put(KeyRepo, repo)
            .put(KeySectionKey, sectionKey)
            .put(KeyTitle, title)
            .put(KeyStatus, status.name)
            .put(KeyDescription, description)
            .put(KeyMetrics, metrics.toMetricsJsonArray())
            .put(KeyNotices, notices.toStringsJsonArray())
            .put(KeyActions, actions.toStringsJsonArray())
            .put(KeySourceUrl, sourceUrl)
    }

    private fun JSONObject.toSummary(): RepositoryHtmlSectionSummary {
        return RepositoryHtmlSectionSummary(
            owner = optString(KeyOwner, ""),
            repo = optString(KeyRepo, ""),
            sectionKey = optString(KeySectionKey, "insights"),
            title = optString(KeyTitle, "Insights"),
            status = runCatching {
                RepositoryHtmlSectionStatus.valueOf(optString(KeyStatus, RepositoryHtmlSectionStatus.Available.name))
            }.getOrDefault(RepositoryHtmlSectionStatus.Available),
            description = optString(KeyDescription, ""),
            metrics = optJSONArray(KeyMetrics)?.toMetrics().orEmpty(),
            notices = optJSONArray(KeyNotices)?.toStringList().orEmpty(),
            actions = optJSONArray(KeyActions)?.toStringList().orEmpty(),
            sourceUrl = optString(KeySourceUrl, "")
        )
    }

    private fun List<RepositoryHtmlMetric>.toMetricsJsonArray(): JSONArray {
        return JSONArray().also { array ->
            forEach { metric ->
                array.put(
                    JSONObject()
                        .put(KeyMetricLabel, metric.label)
                        .put(KeyMetricValue, metric.value)
                )
            }
        }
    }

    private fun JSONArray.toMetrics(): List<RepositoryHtmlMetric> {
        return buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                add(
                    RepositoryHtmlMetric(
                        label = item.optString(KeyMetricLabel, ""),
                        value = item.optString(KeyMetricValue, "")
                    )
                )
            }
        }
    }

    private fun List<String>.toStringsJsonArray(): JSONArray {
        return JSONArray().also { array -> forEach(array::put) }
    }

    private fun JSONArray.toStringList(): List<String> {
        return buildList {
            for (index in 0 until length()) {
                optString(index).takeIf { it.isNotBlank() }?.let(::add)
            }
        }
    }

    private fun buildKey(owner: String, repo: String): String {
        return "${owner.trim().lowercase()}/${repo.trim().lowercase()}"
    }

    private companion object {
        const val PreferencesName = "repository_insights_cache"
        const val KeySummary = "summary"
        const val KeyRefreshedAtMillis = "refreshed_at_millis"
        const val KeyOwner = "owner"
        const val KeyRepo = "repo"
        const val KeySectionKey = "section_key"
        const val KeyTitle = "title"
        const val KeyStatus = "status"
        const val KeyDescription = "description"
        const val KeyMetrics = "metrics"
        const val KeyNotices = "notices"
        const val KeyActions = "actions"
        const val KeySourceUrl = "source_url"
        const val KeyMetricLabel = "label"
        const val KeyMetricValue = "value"
    }
}
