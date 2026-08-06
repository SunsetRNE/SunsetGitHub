package com.Sunset.REN.GitHub.ui.repo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.repo.RepositoryIssueComment
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubTheme
import com.Sunset.REN.GitHub.ui.compose.screens.repo.RepositoryIssueDetailDialogState
import com.Sunset.REN.GitHub.ui.compose.screens.repo.RepositoryIssueDetailScreen
import com.google.android.material.snackbar.Snackbar

class RepositoryIssueDetailFragment : Fragment() {

    private lateinit var viewModel: RepositoryIssueDetailViewModel
    private var repositoryOwner = ""
    private var repositoryName = ""
    private var issueNumber = 0
    private var detailState by mutableStateOf(RepositoryIssueDetailUiState())
    private var commentDraft by mutableStateOf("")
    private var dialogState by mutableStateOf<RepositoryIssueDetailDialogState?>(null)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this)[RepositoryIssueDetailViewModel::class.java]
        repositoryOwner = requireArguments().getString(ARG_OWNER).orEmpty()
        repositoryName = requireArguments().getString(ARG_REPO).orEmpty()
        issueNumber = requireArguments().getInt(ARG_NUMBER, 0)

        detailState = RepositoryIssueDetailUiState(
            owner = repositoryOwner,
            repo = repositoryName,
            number = issueNumber
        )

        val composeView = ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SunsetGitHubTheme {
                    RepositoryIssueDetailScreen(
                        state = detailState,
                        commentDraft = commentDraft,
                        dialogState = dialogState,
                        onCommentDraftChange = { commentDraft = it },
                        onRetry = viewModel::load,
                        onLoadMoreComments = viewModel::loadMoreComments,
                        onToggleIssueState = viewModel::toggleIssueState,
                        onShowLabelsPicker = ::showLabelPicker,
                        onCreateComment = ::submitComment,
                        onShowEditComment = ::showEditCommentDialog,
                        onShowDeleteComment = ::showDeleteCommentDialog,
                        onDismissDialog = { dialogState = null },
                        onCommentSave = { commentId, body ->
                            if (body.isBlank()) {
                                showMessage(getString(R.string.repository_issue_detail_comment_empty))
                            } else {
                                dialogState = null
                                viewModel.updateComment(commentId, body)
                            }
                        },
                        onLabelsSave = { labels ->
                            dialogState = null
                            viewModel.setLabels(labels)
                        },
                        onDeleteComment = { commentId ->
                            dialogState = null
                            viewModel.deleteComment(commentId)
                        }
                    )
                }
            }
        }

        viewModel.detailState.observe(viewLifecycleOwner) { state ->
            detailState = state
            state.statusMessage?.let { message ->
                showMessage(message)
                viewModel.consumeStatusMessage()
            }
        }
        viewModel.prepare(repositoryOwner, repositoryName, issueNumber)
        return composeView
    }

    private fun submitComment() {
        val body = commentDraft.trim()
        if (body.isBlank()) {
            showMessage(getString(R.string.repository_issue_detail_comment_empty))
            return
        }
        commentDraft = ""
        viewModel.createComment(body)
    }

    private fun showEditCommentDialog(comment: RepositoryIssueComment) {
        dialogState = RepositoryIssueDetailDialogState.EditComment(comment.id, comment.body)
    }

    private fun showDeleteCommentDialog(comment: RepositoryIssueComment) {
        dialogState = RepositoryIssueDetailDialogState.DeleteComment(comment.id)
    }

    private fun showLabelPicker() {
        val state = viewModel.detailState.value ?: return
        if (state.availableLabels.isEmpty()) {
            showMessage(getString(R.string.repository_issue_detail_labels_empty))
            return
        }
        dialogState = RepositoryIssueDetailDialogState.LabelsPicker(
            labels = state.availableLabels,
            selectedLabels = state.issue?.labels?.map { it.name }.orEmpty()
        )
    }

    private fun showMessage(message: String) {
        view?.let { Snackbar.make(it, message, Snackbar.LENGTH_SHORT).show() }
    }

    companion object {
        const val ARG_OWNER = "owner"
        const val ARG_REPO = "repo"
        const val ARG_NUMBER = "number"
    }
}