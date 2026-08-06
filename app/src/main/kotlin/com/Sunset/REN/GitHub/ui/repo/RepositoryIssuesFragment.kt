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
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubTheme
import com.Sunset.REN.GitHub.ui.compose.screens.repo.RepositoryIssuesDialogState
import com.Sunset.REN.GitHub.ui.compose.screens.repo.RepositoryIssuesScreen
import com.google.android.material.snackbar.Snackbar

class RepositoryIssuesFragment : RepositorySectionFragment() {

    private lateinit var viewModel: RepositoryIssuesViewModel
    override var repositoryOwner: String = ""
    override var repositoryName: String = ""
    override val selectedRepositorySection: RepositorySection = RepositorySection.Issues

    private var issuesState by mutableStateOf(RepositoryIssuesUiState())
    private var isLabelFilterExpanded by mutableStateOf(false)
    private var dialogState by mutableStateOf<RepositoryIssuesDialogState?>(null)
    private var rootView: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this)[RepositoryIssuesViewModel::class.java]
        repositoryOwner = requireArguments().getString(ARG_OWNER).orEmpty()
        repositoryName = requireArguments().getString(ARG_REPO).orEmpty()
        issuesState = viewModel.issuesState.value ?: RepositoryIssuesUiState(
            owner = repositoryOwner,
            repo = repositoryName
        )
        return ComposeView(requireContext()).apply {
            this@RepositoryIssuesFragment.rootView = this
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SunsetGitHubTheme {
                    RepositoryIssuesScreen(
                        state = issuesState,
                        isLabelFilterExpanded = isLabelFilterExpanded,
                        dialogState = dialogState,
                        onOpenState = { viewModel.switchState(RepositoryIssuesUiState.OpenState) },
                        onClosedState = { viewModel.switchState(RepositoryIssuesUiState.ClosedState) },
                        onToggleLabelsExpanded = { isLabelFilterExpanded = !isLabelFilterExpanded },
                        onSelectLabels = { labels ->
                            dialogState = null
                            viewModel.switchLabels(labels)
                        },
                        onShowCreatorFilter = ::showCreatorFilterDialog,
                        onDismissDialog = { dialogState = null },
                        onCreatorSelected = { creator ->
                            dialogState = null
                            viewModel.switchCreator(creator)
                        },
                        onShowLabelsDialog = ::showLabelsFilterDialog,
                        onLoadFirstPage = viewModel::loadFirstPage,
                        onLoadMore = viewModel::loadNextPage,
                        onOpenIssue = ::openIssueDetail,
                        onCreateIssue = ::openCreateIssue
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        renderRepositorySectionNavigation()
        observeCreatedIssueResult()
        viewModel.issuesState.observe(viewLifecycleOwner) { state -> issuesState = state }
        viewModel.prepare(repositoryOwner, repositoryName)
    }

    override fun onDestroyView() {
        rootView = null
        super.onDestroyView()
    }

    /** 监听新建问题页回传的新问题号：刷新列表并跳转到该问题详情。 */
    private fun observeCreatedIssueResult() {
        val savedStateHandle = findNavController().currentBackStackEntry?.savedStateHandle ?: return
        savedStateHandle.getLiveData<Int>(RepositoryIssueCreateFragment.RESULT_ISSUE_CREATED)
            .observe(viewLifecycleOwner) { number ->
                if (number != null) {
                    savedStateHandle.remove<Int>(RepositoryIssueCreateFragment.RESULT_ISSUE_CREATED)
                    viewModel.loadFirstPage()
                    openIssueDetail(number)
                }
            }
    }

    private fun showCreatorFilterDialog() {
        val state = issuesState
        val currentUserLogin = state.currentUserLogin?.takeIf { it.isNotBlank() }
        if (currentUserLogin == null) {
            showSnackbar(R.string.repository_issues_filter_creator_requires_login)
            return
        }
        dialogState = RepositoryIssuesDialogState.CreatorFilter(
            currentUserLogin = currentUserLogin,
            selectedCreator = state.selectedCreator
        )
    }

    private fun showLabelsFilterDialog() {
        val state = issuesState
        if (state.availableLabels.isEmpty()) {
            showSnackbar(R.string.repository_issues_filter_labels_empty)
            return
        }
        dialogState = RepositoryIssuesDialogState.LabelsFilter(
            labels = state.availableLabels,
            selectedLabels = state.selectedLabels
        )
    }

    private fun openCreateIssue() {
        if (repositoryOwner.isBlank() || repositoryName.isBlank()) return
        val destinationId = resources.getIdentifier(
            IssueCreateDestinationName,
            ResourceTypeId,
            requireContext().packageName
        )
        if (destinationId == 0) {
            showSnackbar(R.string.repository_issues_missing_destination)
            return
        }
        findNavController().navigate(
            destinationId,
            Bundle().apply {
                putString(RepositoryIssueCreateFragment.ARG_OWNER, repositoryOwner)
                putString(RepositoryIssueCreateFragment.ARG_REPO, repositoryName)
            }
        )
    }

    private fun openIssueDetail(number: Int) {
        if (repositoryOwner.isBlank() || repositoryName.isBlank()) return
        val destinationId = resources.getIdentifier(
            IssueDetailDestinationName,
            ResourceTypeId,
            requireContext().packageName
        )
        if (destinationId == 0) {
            showSnackbar(R.string.repository_issue_detail_missing_destination)
            return
        }
        findNavController().navigate(
            destinationId,
            Bundle().apply {
                putString(RepositoryIssueDetailFragment.ARG_OWNER, repositoryOwner)
                putString(RepositoryIssueDetailFragment.ARG_REPO, repositoryName)
                putInt(RepositoryIssueDetailFragment.ARG_NUMBER, number)
            }
        )
    }

    private fun showSnackbar(messageResId: Int) {
        rootView?.let { view -> Snackbar.make(view, messageResId, Snackbar.LENGTH_SHORT).show() }
    }

    companion object {
        const val ARG_OWNER = "owner"
        const val ARG_REPO = "repo"
        private const val IssueDetailDestinationName = "repository_issue_detail_fragment"
        private const val IssueCreateDestinationName = "repository_issue_create_fragment"
        private const val ResourceTypeId = "id"
    }
}
