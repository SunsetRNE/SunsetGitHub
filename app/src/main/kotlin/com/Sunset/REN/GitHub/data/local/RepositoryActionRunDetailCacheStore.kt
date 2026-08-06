package com.Sunset.REN.GitHub.data.local

import android.content.Context
import androidx.core.content.edit
import com.Sunset.REN.GitHub.domain.repo.RepositoryActionArtifact
import com.Sunset.REN.GitHub.domain.repo.RepositoryActionRunDetail
import com.Sunset.REN.GitHub.domain.repo.RepositoryActionRunLogPreview
import org.json.JSONArray
import org.json.JSONObject

class RepositoryActionRunDetailCacheStore(context: Context) {

    private val sharedPreferences = context.applicationContext.getSharedPreferences(
        PreferencesName,
        Context.MODE_PRIVATE
    )

    fun getCachedActionRun(owner: String, repo: String, runId: Long): RepositoryActionRunDetailCacheSnapshot? {
        val rawValue = sharedPreferences.getString(buildKey(owner, repo, runId), null).orEmpty()
        if (rawValue.isBlank()) return null
        return runCatching {
            val json = JSONObject(rawValue)
            RepositoryActionRunDetailCacheSnapshot(
                actionRun = json.getJSONObject(KeyActionRun).toActionRun(),
                artifacts = json.optJSONArray(KeyArtifacts)?.toArtifacts().orEmpty(),
                logPreview = json.optJSONObject(KeyLogPreview)?.toLogPreview(),
                refreshedAtMillis = json.optLong(KeyRefreshedAtMillis, 0L)
            )
        }.getOrNull()
    }

    fun cacheActionRun(owner: String, repo: String, runId: Long, snapshot: RepositoryActionRunDetailCacheSnapshot) {
        val json = JSONObject()
            .put(KeyActionRun, snapshot.actionRun.toJson())
            .put(KeyArtifacts, snapshot.artifacts.toArtifactsJsonArray())
            .put(KeyLogPreview, snapshot.logPreview?.toJson())
            .put(KeyRefreshedAtMillis, snapshot.refreshedAtMillis)
        sharedPreferences.edit {
            putString(buildKey(owner, repo, runId), json.toString())
        }
    }

    private fun buildKey(owner: String, repo: String, runId: Long): String {
        return "${owner.lowercase()}/${repo.lowercase()}/actions/runs/$runId"
    }

    private fun JSONObject.toActionRun(): RepositoryActionRunDetail {
        return RepositoryActionRunDetail(
            id = optLong(KeyId, 0L),
            nodeId = optionalString(KeyNodeId),
            name = optString(KeyName, ""),
            status = optionalString(KeyStatus),
            conclusion = optionalString(KeyConclusion),
            event = optionalString(KeyEvent),
            headBranch = optionalString(KeyHeadBranch),
            headSha = optionalString(KeyHeadSha),
            htmlUrl = optionalString(KeyHtmlUrl),
            apiUrl = optionalString(KeyApiUrl),
            workflowUrl = optionalString(KeyWorkflowUrl),
            jobsUrl = optionalString(KeyJobsUrl),
            logsUrl = optionalString(KeyLogsUrl),
            artifactsUrl = optionalString(KeyArtifactsUrl),
            cancelUrl = optionalString(KeyCancelUrl),
            rerunUrl = optionalString(KeyRerunUrl),
            previousAttemptUrl = optionalString(KeyPreviousAttemptUrl),
            createdAt = optionalString(KeyCreatedAt),
            updatedAt = optionalString(KeyUpdatedAt),
            runStartedAt = optionalString(KeyRunStartedAt),
            runNumber = optionalInt(KeyRunNumber),
            runAttempt = optionalInt(KeyRunAttempt),
            workflowId = optionalLong(KeyWorkflowId),
            checkSuiteId = optionalLong(KeyCheckSuiteId),
            workflowName = optionalString(KeyWorkflowName),
            actorLogin = optionalString(KeyActorLogin),
            triggeringActorLogin = optionalString(KeyTriggeringActorLogin),
            repositoryOwner = optionalString(KeyRepositoryOwner),
            repositoryName = optionalString(KeyRepositoryName),
            headRepositoryFullName = optionalString(KeyHeadRepositoryFullName),
            headRepositoryHtmlUrl = optionalString(KeyHeadRepositoryHtmlUrl),
            path = optionalString(KeyPath),
            headCommitMessage = optionalString(KeyHeadCommitMessage),
            headCommitAuthorName = optionalString(KeyHeadCommitAuthorName),
            headCommitAuthorEmail = optionalString(KeyHeadCommitAuthorEmail),
            headCommitTimestamp = optionalString(KeyHeadCommitTimestamp),
            sourceZipUrl = optionalString(KeySourceZipUrl),
            sourceTarUrl = optionalString(KeySourceTarUrl),
            pullRequestRefs = optJSONArray(KeyPullRequestRefs)?.toStringList().orEmpty(),
            displayState = optString(KeyDisplayState, "unknown"),
            details = optJSONArray(KeyDetails)?.toStringList().orEmpty()
        )
    }

    private fun RepositoryActionRunDetail.toJson(): JSONObject {
        return JSONObject()
            .put(KeyId, id)
            .put(KeyNodeId, nodeId)
            .put(KeyName, name)
            .put(KeyStatus, status)
            .put(KeyConclusion, conclusion)
            .put(KeyEvent, event)
            .put(KeyHeadBranch, headBranch)
            .put(KeyHeadSha, headSha)
            .put(KeyHtmlUrl, htmlUrl)
            .put(KeyApiUrl, apiUrl)
            .put(KeyWorkflowUrl, workflowUrl)
            .put(KeyJobsUrl, jobsUrl)
            .put(KeyLogsUrl, logsUrl)
            .put(KeyArtifactsUrl, artifactsUrl)
            .put(KeyCancelUrl, cancelUrl)
            .put(KeyRerunUrl, rerunUrl)
            .put(KeyPreviousAttemptUrl, previousAttemptUrl)
            .put(KeyCreatedAt, createdAt)
            .put(KeyUpdatedAt, updatedAt)
            .put(KeyRunStartedAt, runStartedAt)
            .put(KeyRunNumber, runNumber)
            .put(KeyRunAttempt, runAttempt)
            .put(KeyWorkflowId, workflowId)
            .put(KeyCheckSuiteId, checkSuiteId)
            .put(KeyWorkflowName, workflowName)
            .put(KeyActorLogin, actorLogin)
            .put(KeyTriggeringActorLogin, triggeringActorLogin)
            .put(KeyRepositoryOwner, repositoryOwner)
            .put(KeyRepositoryName, repositoryName)
            .put(KeyHeadRepositoryFullName, headRepositoryFullName)
            .put(KeyHeadRepositoryHtmlUrl, headRepositoryHtmlUrl)
            .put(KeyPath, path)
            .put(KeyHeadCommitMessage, headCommitMessage)
            .put(KeyHeadCommitAuthorName, headCommitAuthorName)
            .put(KeyHeadCommitAuthorEmail, headCommitAuthorEmail)
            .put(KeyHeadCommitTimestamp, headCommitTimestamp)
            .put(KeySourceZipUrl, sourceZipUrl)
            .put(KeySourceTarUrl, sourceTarUrl)
            .put(KeyPullRequestRefs, pullRequestRefs.toJsonArray())
            .put(KeyDisplayState, displayState)
            .put(KeyDetails, details.toJsonArray())
    }

    private fun JSONArray.toArtifacts(): List<RepositoryActionArtifact> {
        return buildList {
            for (index in 0 until length()) {
                val artifact = getJSONObject(index)
                add(
                    RepositoryActionArtifact(
                        id = artifact.optLong(KeyId, 0L),
                        name = artifact.optString(KeyName, ""),
                        sizeInBytes = artifact.optLong(KeySizeInBytes, 0L),
                        archiveDownloadUrl = artifact.optionalString(KeyArchiveDownloadUrl),
                        expired = artifact.optBoolean(KeyExpired, false),
                        createdAt = artifact.optionalString(KeyCreatedAt),
                        expiresAt = artifact.optionalString(KeyExpiresAt)
                    )
                )
            }
        }
    }

    private fun List<RepositoryActionArtifact>.toArtifactsJsonArray(): JSONArray {
        return JSONArray().also { array ->
            forEach { artifact ->
                array.put(
                    JSONObject()
                        .put(KeyId, artifact.id)
                        .put(KeyName, artifact.name)
                        .put(KeySizeInBytes, artifact.sizeInBytes)
                        .put(KeyArchiveDownloadUrl, artifact.archiveDownloadUrl)
                        .put(KeyExpired, artifact.expired)
                        .put(KeyCreatedAt, artifact.createdAt)
                        .put(KeyExpiresAt, artifact.expiresAt)
                )
            }
        }
    }

    private fun JSONObject.toLogPreview(): RepositoryActionRunLogPreview {
        return RepositoryActionRunLogPreview(
            text = optString(KeyText, ""),
            fileCount = optInt(KeyFileCount, 0),
            truncated = optBoolean(KeyTruncated, false)
        )
    }

    private fun RepositoryActionRunLogPreview.toJson(): JSONObject {
        return JSONObject()
            .put(KeyText, text)
            .put(KeyFileCount, fileCount)
            .put(KeyTruncated, truncated)
    }

    private fun JSONArray.toStringList(): List<String> {
        return buildList {
            for (index in 0 until length()) {
                optString(index).takeIf { it.isNotBlank() }?.let(::add)
            }
        }
    }

    private fun List<String>.toJsonArray(): JSONArray {
        return JSONArray().also { array -> forEach(array::put) }
    }

    private fun JSONObject.optionalString(key: String): String? {
        return if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
    }

    private fun JSONObject.optionalInt(key: String): Int? {
        return if (isNull(key)) null else optInt(key, 0).takeIf { it > 0 }
    }

    private fun JSONObject.optionalLong(key: String): Long? {
        return if (isNull(key)) null else optLong(key, 0L).takeIf { it > 0L }
    }

    private companion object {
        const val PreferencesName = "repository_action_run_detail_cache_preferences"
        const val KeyActionRun = "action_run"
        const val KeyArtifacts = "artifacts"
        const val KeyLogPreview = "log_preview"
        const val KeyRefreshedAtMillis = "refreshed_at_millis"
        const val KeyId = "id"
        const val KeyNodeId = "node_id"
        const val KeyName = "name"
        const val KeyStatus = "status"
        const val KeyConclusion = "conclusion"
        const val KeyEvent = "event"
        const val KeyHeadBranch = "head_branch"
        const val KeyHeadSha = "head_sha"
        const val KeyHtmlUrl = "html_url"
        const val KeyApiUrl = "api_url"
        const val KeyWorkflowUrl = "workflow_url"
        const val KeyJobsUrl = "jobs_url"
        const val KeyLogsUrl = "logs_url"
        const val KeyArtifactsUrl = "artifacts_url"
        const val KeyCancelUrl = "cancel_url"
        const val KeyRerunUrl = "rerun_url"
        const val KeyPreviousAttemptUrl = "previous_attempt_url"
        const val KeyCreatedAt = "created_at"
        const val KeyUpdatedAt = "updated_at"
        const val KeyRunStartedAt = "run_started_at"
        const val KeyRunNumber = "run_number"
        const val KeyRunAttempt = "run_attempt"
        const val KeyWorkflowId = "workflow_id"
        const val KeyCheckSuiteId = "check_suite_id"
        const val KeyWorkflowName = "workflow_name"
        const val KeyActorLogin = "actor_login"
        const val KeyTriggeringActorLogin = "triggering_actor_login"
        const val KeyRepositoryOwner = "repository_owner"
        const val KeyRepositoryName = "repository_name"
        const val KeyHeadRepositoryFullName = "head_repository_full_name"
        const val KeyHeadRepositoryHtmlUrl = "head_repository_html_url"
        const val KeyPath = "path"
        const val KeyHeadCommitMessage = "head_commit_message"
        const val KeyHeadCommitAuthorName = "head_commit_author_name"
        const val KeyHeadCommitAuthorEmail = "head_commit_author_email"
        const val KeyHeadCommitTimestamp = "head_commit_timestamp"
        const val KeySourceZipUrl = "source_zip_url"
        const val KeySourceTarUrl = "source_tar_url"
        const val KeyPullRequestRefs = "pull_request_refs"
        const val KeyDisplayState = "display_state"
        const val KeyDetails = "details"
        const val KeySizeInBytes = "size_in_bytes"
        const val KeyArchiveDownloadUrl = "archive_download_url"
        const val KeyExpired = "expired"
        const val KeyExpiresAt = "expires_at"
        const val KeyText = "text"
        const val KeyFileCount = "file_count"
        const val KeyTruncated = "truncated"
    }
}

data class RepositoryActionRunDetailCacheSnapshot(
    val actionRun: RepositoryActionRunDetail,
    val artifacts: List<RepositoryActionArtifact>,
    val logPreview: RepositoryActionRunLogPreview?,
    val refreshedAtMillis: Long
)
