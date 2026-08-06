package com.Sunset.REN.GitHub.ui.notifications

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubTheme
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens
import com.Sunset.REN.GitHub.ui.profile.ProfileFragment
import com.Sunset.REN.GitHub.ui.repo.GitHubInternalLinkParser
import com.Sunset.REN.GitHub.ui.repo.GitHubInternalLinkTarget
import com.Sunset.REN.GitHub.ui.repo.RepositoryDetailFragment
import com.Sunset.REN.GitHub.ui.repo.RepositoryFileEditFragment
import com.Sunset.REN.GitHub.ui.repo.RepositoryIssueDetailFragment
import com.google.android.material.snackbar.Snackbar

class NotificationDetailFragment : Fragment() {

    private val viewModel: NotificationsViewModel by activityViewModels()
    private lateinit var args: NotificationDetailArgs
    private var rootView: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        args = NotificationDetailArgs.from(requireArguments())
        return ComposeView(requireContext()).apply {
            this@NotificationDetailFragment.rootView = this
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SunsetGitHubTheme {
                    NotificationDetailScreen(
                        args = args,
                        subjectType = localizeSubjectType(args.subjectType),
                        reason = localizeReason(args.reason),
                        onMarkRead = {
                            viewModel.markAsRead(args.id)
                            showMessage(R.string.notification_action_mark_read_started)
                        },
                        onDone = {
                            viewModel.markAsDone(args.id)
                            showMessage(R.string.notification_action_done_started)
                        },
                        onSubscribe = {
                            viewModel.subscribe(args.id)
                            showMessage(R.string.notification_action_subscribe_started)
                        },
                        onUnsubscribe = {
                            viewModel.unsubscribe(args.id)
                            showMessage(R.string.notification_action_unsubscribe_started)
                        },
                        onOpenSubject = { openGitHubUrl(args.htmlUrl) },
                        onOpenLatest = { openGitHubUrl(args.latestCommentHtmlUrl) },
                        onOpenRepo = { openGitHubUrl(args.repositoryHtmlUrl) }
                    )
                }
            }
        }
    }

    override fun onDestroyView() {
        rootView = null
        super.onDestroyView()
    }

    private fun openGitHubUrl(url: String) {
        if (url.isBlank()) {
            showMessage(R.string.notifications_open_failed)
            return
        }
        if (openInternalGitHubTarget(url)) return
        openExternalUrl(url)
    }

    private fun openInternalGitHubTarget(url: String): Boolean {
        return when (val target = GitHubInternalLinkParser.parse(url)) {
            is GitHubInternalLinkTarget.Repository -> navigateToRepository(target.owner, target.repo)
            is GitHubInternalLinkTarget.File -> navigateToFile(target.owner, target.repo, target.path, target.name)
            is GitHubInternalLinkTarget.Issue -> navigateToIssue(target.owner, target.repo, target.number)
            is GitHubInternalLinkTarget.PullRequest -> navigateToIssue(target.owner, target.repo, target.number)
            is GitHubInternalLinkTarget.User -> navigateToProfile(target.login)
            null -> false
        }
    }

    private fun navigateToRepository(owner: String, repo: String): Boolean {
        if (owner.isBlank() || repo.isBlank()) return false
        return runCatching {
            findNavController().navigate(
                R.id.repository_detail_fragment,
                Bundle().apply {
                    putString(RepositoryDetailFragment.ARG_OWNER, owner)
                    putString(RepositoryDetailFragment.ARG_REPO, repo)
                    putString(RepositoryDetailFragment.ARG_FULL_NAME, "$owner/$repo")
                }
            )
        }.isSuccess
    }

    private fun navigateToFile(owner: String, repo: String, path: String, name: String): Boolean {
        if (owner.isBlank() || repo.isBlank() || path.isBlank()) return false
        return runCatching {
            findNavController().navigate(
                R.id.repository_file_edit_fragment,
                Bundle().apply {
                    putString(RepositoryFileEditFragment.ARG_OWNER, owner)
                    putString(RepositoryFileEditFragment.ARG_REPO, repo)
                    putString(RepositoryFileEditFragment.ARG_PATH, path)
                    putString(RepositoryFileEditFragment.ARG_NAME, name.ifBlank { path.substringAfterLast('/') })
                    putBoolean(RepositoryFileEditFragment.ARG_PREVIEW_MODE, true)
                }
            )
        }.isSuccess
    }

    private fun navigateToIssue(owner: String, repo: String, number: Int): Boolean {
        if (owner.isBlank() || repo.isBlank() || number <= 0) return false
        return runCatching {
            findNavController().navigate(
                R.id.repository_issue_detail_fragment,
                Bundle().apply {
                    putString(RepositoryIssueDetailFragment.ARG_OWNER, owner)
                    putString(RepositoryIssueDetailFragment.ARG_REPO, repo)
                    putInt(RepositoryIssueDetailFragment.ARG_NUMBER, number)
                }
            )
        }.isSuccess
    }

    private fun navigateToProfile(login: String): Boolean {
        if (login.isBlank()) return false
        return runCatching {
            findNavController().navigate(
                R.id.navigation_profile,
                Bundle().apply { putString(ProfileFragment.ARG_LOGIN, login) }
            )
        }.isSuccess
    }

    private fun openExternalUrl(url: String) {
        if (url.isBlank()) {
            showMessage(R.string.notifications_open_failed)
            return
        }
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            showMessage(R.string.notifications_open_failed)
        }
    }

    private fun showMessage(messageResId: Int) {
        rootView?.let { Snackbar.make(it, messageResId, Snackbar.LENGTH_SHORT).show() }
    }

    private fun localizeSubjectType(type: String): String {
        return when (type.lowercase()) {
            "issue" -> getString(R.string.notification_type_issue)
            "pullrequest" -> getString(R.string.notification_type_pull_request)
            "release" -> getString(R.string.notification_type_release)
            "commit" -> getString(R.string.notification_type_commit)
            "discussion" -> getString(R.string.notification_type_discussion)
            "checksuite" -> getString(R.string.notification_type_check_suite)
            else -> type.ifBlank { getString(R.string.notification_type_notification) }
        }
    }

    private fun localizeReason(reason: String): String {
        return when (reason.lowercase()) {
            "assign" -> getString(R.string.notification_reason_assign)
            "author" -> getString(R.string.notification_reason_author)
            "comment" -> getString(R.string.notification_reason_comment)
            "invitation" -> getString(R.string.notification_reason_invitation)
            "manual" -> getString(R.string.notification_reason_manual)
            "mention" -> getString(R.string.notification_reason_mention)
            "review_requested" -> getString(R.string.notification_reason_review_requested)
            "security_alert" -> getString(R.string.notification_reason_security_alert)
            "state_change" -> getString(R.string.notification_reason_state_change)
            "subscribed" -> getString(R.string.notification_reason_subscribed)
            "team_mention" -> getString(R.string.notification_reason_team_mention)
            "ci_activity" -> getString(R.string.notification_reason_ci_activity)
            else -> reason.ifBlank { getString(R.string.notification_reason_unknown) }
        }
    }

    companion object {
        const val ARG_ID = "notification_id"
        const val ARG_REPOSITORY = "notification_repository"
        const val ARG_TITLE = "notification_title"
        const val ARG_TYPE = "notification_type"
        const val ARG_REASON = "notification_reason"
        const val ARG_UNREAD = "notification_unread"
        const val ARG_UPDATED_AT = "notification_updated_at"
        const val ARG_HTML_URL = "notification_html_url"
        const val ARG_REPOSITORY_HTML_URL = "notification_repository_html_url"
        const val ARG_LATEST_COMMENT_HTML_URL = "notification_latest_comment_html_url"
    }
}

@Composable
private fun NotificationDetailScreen(
    args: NotificationDetailArgs,
    subjectType: String,
    reason: String,
    onMarkRead: () -> Unit,
    onDone: () -> Unit,
    onSubscribe: () -> Unit,
    onUnsubscribe: () -> Unit,
    onOpenSubject: () -> Unit,
    onOpenLatest: () -> Unit,
    onOpenRepo: () -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(args.repositoryFullName, color = colors.textMuted, style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(args.subjectTitle, color = colors.textPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(14.dp))
        Card(colors = CardDefaults.cardColors(containerColor = colors.surface), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.notification_detail_type, subjectType), color = colors.textPrimary)
                Text(stringResource(R.string.notification_detail_reason, reason), color = colors.textSecondary)
                Text(
                    stringResource(
                        R.string.notification_detail_updated,
                        args.updatedAt.ifBlank { stringResource(R.string.notification_detail_unknown_time) }
                    ),
                    color = colors.textSecondary
                )
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(stringResource(R.string.notification_detail_quick_actions), color = colors.textPrimary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onMarkRead, enabled = args.unread) { Text(stringResource(R.string.notification_action_mark_read)) }
            Button(onClick = onDone) { Text(stringResource(R.string.notification_action_done)) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onSubscribe) { Text(stringResource(R.string.notification_action_subscribe)) }
                OutlinedButton(onClick = onUnsubscribe) { Text(stringResource(R.string.notification_action_unsubscribe)) }
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(stringResource(R.string.notification_detail_open_links), color = colors.textPrimary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onOpenSubject, enabled = args.htmlUrl.isNotBlank()) { Text(stringResource(R.string.notification_action_open_subject)) }
            Button(onClick = onOpenLatest, enabled = args.latestCommentHtmlUrl.isNotBlank()) { Text(stringResource(R.string.notification_action_open_latest_comment)) }
            Button(onClick = onOpenRepo, enabled = args.repositoryHtmlUrl.isNotBlank()) { Text(stringResource(R.string.notification_action_open_repository)) }
        }
    }
}

private data class NotificationDetailArgs(
    val id: String,
    val repositoryFullName: String,
    val subjectTitle: String,
    val subjectType: String,
    val reason: String,
    val unread: Boolean,
    val updatedAt: String,
    val htmlUrl: String,
    val repositoryHtmlUrl: String,
    val latestCommentHtmlUrl: String
) {
    companion object {
        fun from(bundle: Bundle): NotificationDetailArgs {
            return NotificationDetailArgs(
                id = bundle.getString(NotificationDetailFragment.ARG_ID).orEmpty(),
                repositoryFullName = bundle.getString(NotificationDetailFragment.ARG_REPOSITORY).orEmpty(),
                subjectTitle = bundle.getString(NotificationDetailFragment.ARG_TITLE).orEmpty(),
                subjectType = bundle.getString(NotificationDetailFragment.ARG_TYPE).orEmpty(),
                reason = bundle.getString(NotificationDetailFragment.ARG_REASON).orEmpty(),
                unread = bundle.getBoolean(NotificationDetailFragment.ARG_UNREAD),
                updatedAt = bundle.getString(NotificationDetailFragment.ARG_UPDATED_AT).orEmpty(),
                htmlUrl = bundle.getString(NotificationDetailFragment.ARG_HTML_URL).orEmpty(),
                repositoryHtmlUrl = bundle.getString(NotificationDetailFragment.ARG_REPOSITORY_HTML_URL).orEmpty(),
                latestCommentHtmlUrl = bundle.getString(NotificationDetailFragment.ARG_LATEST_COMMENT_HTML_URL).orEmpty()
            )
        }
    }
}
