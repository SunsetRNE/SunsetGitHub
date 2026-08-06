package com.Sunset.REN.GitHub.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.notification.GitHubNotification
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubTheme
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens

class NotificationsFragment : Fragment() {

    private val viewModel: NotificationsViewModel by activityViewModels()
    private var uiState by mutableStateOf(NotificationsUiState())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SunsetGitHubTheme {
                    NotificationsScreen(
                        state = uiState,
                        onFilterSelected = viewModel::switchAll,
                        onRetry = viewModel::loadFirstPage,
                        onLoadMore = viewModel::loadNextPage,
                        onOpenNotification = ::openNotification,
                        localizeSubjectType = ::localizeSubjectType,
                        localizeReason = ::localizeReason
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.notificationsState.observe(viewLifecycleOwner) { state -> uiState = state }
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

    private fun openNotification(notification: GitHubNotification) {
        findNavController().navigate(
            R.id.notification_detail_fragment,
            Bundle().apply {
                putString(NotificationDetailFragment.ARG_ID, notification.id)
                putString(NotificationDetailFragment.ARG_REPOSITORY, notification.repositoryFullName)
                putString(NotificationDetailFragment.ARG_TITLE, notification.subjectTitle)
                putString(NotificationDetailFragment.ARG_TYPE, notification.subjectType)
                putString(NotificationDetailFragment.ARG_REASON, notification.reason)
                putBoolean(NotificationDetailFragment.ARG_UNREAD, notification.unread)
                putString(NotificationDetailFragment.ARG_UPDATED_AT, notification.updatedAt.orEmpty())
                putString(NotificationDetailFragment.ARG_HTML_URL, notification.htmlUrl.orEmpty())
                putString(NotificationDetailFragment.ARG_REPOSITORY_HTML_URL, notification.repositoryHtmlUrl.orEmpty())
                putString(
                    NotificationDetailFragment.ARG_LATEST_COMMENT_HTML_URL,
                    notification.latestCommentUrl?.toGitHubWebUrl().orEmpty()
                )
            }
        )
    }

    private fun String.toGitHubWebUrl(): String {
        val apiPrefix = "https://api.github.com/repos/"
        if (!startsWith(apiPrefix)) return this
        val segments = removePrefix(apiPrefix).split("/")
        if (segments.size < 3) return this
        val owner = segments[0]
        val repo = segments[1]
        return when (segments[2]) {
            "issues" -> if (segments.size >= 4) "https://github.com/$owner/$repo/issues/${segments[3]}" else "https://github.com/$owner/$repo/issues"
            "pulls" -> if (segments.size >= 4) "https://github.com/$owner/$repo/pull/${segments[3]}" else "https://github.com/$owner/$repo/pulls"
            "commits" -> if (segments.size >= 4) "https://github.com/$owner/$repo/commit/${segments[3]}" else "https://github.com/$owner/$repo/commits"
            "releases" -> if (segments.size >= 4) "https://github.com/$owner/$repo/releases/tag/${segments[3]}" else "https://github.com/$owner/$repo/releases"
            "discussions" -> if (segments.size >= 4) "https://github.com/$owner/$repo/discussions/${segments[3]}" else "https://github.com/$owner/$repo/discussions"
            else -> "https://github.com/$owner/$repo"
        }
    }
}

@Composable
private fun NotificationsScreen(
    state: NotificationsUiState,
    onFilterSelected: (Boolean) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenNotification: (GitHubNotification) -> Unit,
    localizeSubjectType: (String) -> String,
    localizeReason: (String) -> String
) {
    val colors = SunsetGitHubThemeTokens.colors
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !state.all,
                    onClick = { onFilterSelected(false) },
                    label = { Text(stringResource(R.string.notifications_filter_unread)) }
                )
                FilterChip(
                    selected = state.all,
                    onClick = { onFilterSelected(true) },
                    label = { Text(stringResource(R.string.notifications_filter_all)) }
                )
            }
        }

        when {
            state.errorMessage != null && state.notifications.isEmpty() -> item {
                StateCard(message = state.errorMessage, actionText = stringResource(R.string.notifications_retry), onAction = onRetry)
            }
            state.isEmpty -> item {
                StateCard(message = stringResource(if (state.all) R.string.notifications_empty_all else R.string.notifications_empty_unread))
            }
            state.isInitialLoad -> item {
                StateCard(message = stringResource(R.string.home_account_state_refreshing))
            }
            else -> items(state.notifications, key = { it.id }) { notification ->
                NotificationRow(
                    notification = notification,
                    onClick = { onOpenNotification(notification) },
                    localizeSubjectType = localizeSubjectType,
                    localizeReason = localizeReason
                )
            }
        }

        if (state.hasMore && state.notifications.isNotEmpty()) {
            item {
                Button(
                    onClick = onLoadMore,
                    enabled = !state.isLoadingMore,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(if (state.isLoadingMore) R.string.notifications_loading_more else R.string.notifications_load_more))
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(
    notification: GitHubNotification,
    onClick: () -> Unit,
    localizeSubjectType: (String) -> String,
    localizeReason: (String) -> String
) {
    val colors = SunsetGitHubThemeTokens.colors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 5.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (notification.unread) colors.accent else Color.Transparent)
            )
            Spacer(modifier = Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.repositoryFullName,
                    color = colors.textMuted,
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = notification.subjectTitle,
                    color = if (notification.unread) colors.textPrimary else colors.textSecondary,
                    fontWeight = if (notification.unread) FontWeight.Bold else FontWeight.Normal,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(onClick = {}, label = { Text(localizeSubjectType(notification.subjectType)) })
                    Text(
                        text = listOfNotNull(
                            localizeReason(notification.reason).takeIf { it.isNotBlank() },
                            notification.updatedAt?.takeIf { it.isNotBlank() }
                        ).joinToString(" · "),
                        color = colors.textMuted,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun StateCard(message: String, actionText: String? = null, onAction: (() -> Unit)? = null) {
    val colors = SunsetGitHubThemeTokens.colors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(124.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = message, color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
            if (actionText != null && onAction != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(onClick = onAction) { Text(actionText) }
            }
        }
    }
}
