package com.Sunset.REN.GitHub.domain.sync

/**
 * Decides how a prepared sync plan should be applied against the current remote branch head.
 *
 * The safe default is to stop when the remote branch changed after planning. When the user
 * explicitly allows overwriting remote changes, the GitHub API backend creates a new commit on
 * top of the current remote HEAD and writes the planned local file entries into that latest tree.
 * This overwrites matching remote paths with local content while avoiding an unnecessary force push.
 */
object WorkspaceSyncExecutionStrategy {
    fun resolve(
        planBaseRevision: String?,
        currentHeadSha: String,
        allowOverwriteRemoteChanges: Boolean
    ): WorkspaceSyncExecutionBase {
        val remoteChanged = !planBaseRevision.isNullOrBlank() && currentHeadSha != planBaseRevision
        if (remoteChanged && !allowOverwriteRemoteChanges) {
            throw IllegalStateException(
                "远端分支已变化：计划基于 $planBaseRevision，当前为 $currentHeadSha。请重新生成计划，或明确允许以本地变更覆盖远端同路径内容。"
            )
        }
        return WorkspaceSyncExecutionBase(
            currentHeadSha = currentHeadSha,
            treeBaseCommitSha = currentHeadSha,
            commitParentSha = currentHeadSha,
            forceUpdate = false,
            remoteChangedSincePlan = remoteChanged,
            overwritingRemoteChanges = remoteChanged && allowOverwriteRemoteChanges
        )
    }
}

data class WorkspaceSyncExecutionBase(
    val currentHeadSha: String,
    val treeBaseCommitSha: String,
    val commitParentSha: String,
    val forceUpdate: Boolean,
    val remoteChangedSincePlan: Boolean,
    val overwritingRemoteChanges: Boolean
)