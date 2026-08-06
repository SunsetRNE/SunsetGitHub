package com.Sunset.REN.GitHub.domain.notification

/** GitHub 通知线程列表项。 */
data class GitHubNotification(
    val id: String,
    val repositoryFullName: String,
    val subjectTitle: String,
    val subjectType: String,
    val reason: String,
    val unread: Boolean,
    val updatedAt: String?,
    val lastReadAt: String?,
    val url: String?,
    val htmlUrl: String?,
    val repositoryHtmlUrl: String?,
    val subjectUrl: String?,
    val latestCommentUrl: String?
)