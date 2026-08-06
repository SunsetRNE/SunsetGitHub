package com.Sunset.REN.GitHub.ui.repo

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.repo.RepositoryActionRun
import com.Sunset.REN.GitHub.domain.repo.RepositoryActionWorkflow
import com.Sunset.REN.GitHub.ui.common.showComposeDialog
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubTheme
import com.Sunset.REN.GitHub.ui.compose.screens.repo.RepositoryActionsScreen
import com.Sunset.REN.GitHub.ui.compose.screens.repo.RepositoryActionsWorkflowDialog
import com.Sunset.REN.GitHub.ui.compose.screens.repo.RepositoryActionsWorkflowDispatchDialog
import com.google.android.material.snackbar.Snackbar
import java.util.Locale

class RepositoryActionsFragment : RepositorySectionFragment() {

    private val viewModel: RepositoryActionsViewModel by viewModels()
    private var actionsState by mutableStateOf(RepositoryActionsUiState())
    override var repositoryOwner: String = ""
    override var repositoryName: String = ""
    override val selectedRepositorySection: RepositorySection = RepositorySection.Actions
    private var rootView: View? = null
    private var workflowDrawer: Dialog? = null
    private var workflowDispatchDialog: Dialog? = null
    private var workflowDispatchDialogWorkflowId: Long? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        actionsState = viewModel.actionsState.value ?: RepositoryActionsUiState()
        return ComposeView(requireContext()).apply {
            this@RepositoryActionsFragment.rootView = this
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SunsetGitHubTheme {
                    RepositoryActionsScreen(
                        state = actionsState,
                        onStatusSelected = viewModel::switchStatus,
                        onRetry = viewModel::reload,
                        onLoadMore = viewModel::loadNextPage,
                        onOpenActions = ::openActionsInGithub,
                        onOpenRun = ::openActionRunDetail
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repositoryOwner = arguments?.getString(ARG_OWNER).orEmpty()
        repositoryName = arguments?.getString(ARG_REPO).orEmpty()
        renderRepositorySectionNavigation()
        viewModel.actionsState.observe(viewLifecycleOwner) { actionsState = it }
        viewModel.openRunDetailEvent.observe(viewLifecycleOwner) { actionRun ->
            actionRun ?: return@observe
            openActionRunDetail(actionRun)
            viewModel.consumeOpenRunDetailEvent()
        }
        viewModel.workflowDispatchMetadataEvent.observe(viewLifecycleOwner) { workflow ->
            workflow ?: return@observe
            updateWorkflowDispatchDialog(workflow)
            viewModel.consumeWorkflowDispatchMetadataEvent()
        }
        viewModel.prepare(repositoryOwner, repositoryName)
    }

    override fun onDestroyView() {
        workflowDrawer?.dismiss()
        workflowDrawer = null
        workflowDispatchDialog?.dismiss()
        clearWorkflowDispatchDialogRefs()
        rootView = null
        super.onDestroyView()
    }

    fun showWorkflowDrawerFromToolbar() {
        val state = viewModel.actionsState.value ?: RepositoryActionsUiState()
        workflowDrawer?.dismiss()
        workflowDrawer = showComposeDialog(requireContext()) { dismiss ->
            RepositoryActionsWorkflowDialog(
                title = getString(R.string.repository_actions_workflow_title),
                message = getString(R.string.repository_actions_workflow_drawer_hint),
                allWorkflowsTitle = getString(R.string.repository_actions_workflow_all),
                allWorkflowsMeta = getString(R.string.repository_actions_workflow_all_meta),
                emptyText = getString(R.string.repository_actions_workflow_empty),
                loadingText = getString(R.string.repository_actions_loading),
                workflows = state.workflows,
                selectedWorkflowId = state.selectedWorkflowId,
                isLoading = state.isLoading,
                dispatchingWorkflowId = state.dispatchingWorkflowId,
                dispatchableText = getString(R.string.repository_actions_open_in_github),
                notDispatchableText = getString(R.string.repository_actions_workflow_not_dispatchable_button),
                dispatchingText = getString(R.string.repository_actions_workflow_dispatching_short),
                runText = getString(R.string.repository_actions_workflow_run_short_button),
                localizeWorkflowState = ::localizeWorkflowState,
                localizeWorkflowTrigger = ::localizeWorkflowTrigger,
                onDismiss = dismiss,
                onSelectAll = { viewModel.switchWorkflow(null); dismiss() },
                onSelectWorkflow = { workflow -> viewModel.switchWorkflow(workflow.id); dismiss() },
                onDispatchWorkflow = { workflow -> dismiss(); showWorkflowDispatchDialog(workflow, viewModel.actionsState.value ?: state) },
                onOpenWorkflow = ::openWorkflowInGithub,
                dismissText = getString(android.R.string.cancel)
            )
        }.also { dialog -> dialog.setOnDismissListener { workflowDrawer = null } }
    }

    private fun showWorkflowDispatchDialog(workflow: RepositoryActionWorkflow, state: RepositoryActionsUiState, requestMetadata: Boolean = true) {
        workflowDispatchDialog?.dismiss()
        workflowDispatchDialogWorkflowId = workflow.id
        workflowDispatchDialog = showComposeDialog(requireContext()) { dismiss ->
            RepositoryActionsWorkflowDispatchDialog(
                workflow = workflow,
                refOptions = buildWorkflowRefOptions(state),
                fallbackRef = fallbackWorkflowRef(state),
                title = getString(R.string.repository_actions_workflow_run_title),
                inputsTitle = getString(R.string.repository_actions_workflow_inputs_title),
                loadingText = getString(R.string.repository_actions_workflow_inputs_loading),
                notDispatchableText = getString(R.string.repository_actions_workflow_not_dispatchable_message),
                noInputsText = getString(R.string.repository_actions_workflow_no_inputs),
                refLabel = getString(R.string.repository_actions_workflow_ref_label),
                refHelper = getString(R.string.repository_actions_workflow_ref_helper),
                cancelText = getString(R.string.repository_actions_cancel),
                runText = getString(R.string.repository_actions_workflow_run_button),
                notDispatchableButtonText = getString(R.string.repository_actions_workflow_not_dispatchable_button),
                dispatchingWorkflowId = state.dispatchingWorkflowId,
                onDismiss = dismiss,
                onSubmit = { request ->
                    val missing = request.workflow.dispatchInputs.firstOrNull { it.required && request.inputs[it.name].isNullOrBlank() }
                    if (missing != null) {
                        showMessage(getString(R.string.repository_actions_workflow_required_missing, missing.name))
                    } else {
                        viewModel.dispatchWorkflow(request.workflow, request.ref, request.inputs)
                        dismiss()
                    }
                }
            )
        }.also { dialog -> dialog.setOnDismissListener { clearWorkflowDispatchDialogRefs() } }
        if (requestMetadata && !workflow.hasLoadedDispatchMetadata) viewModel.loadWorkflowDispatchMetadata(workflow)
    }

    private fun updateWorkflowDispatchDialog(workflow: RepositoryActionWorkflow) {
        if (workflowDispatchDialogWorkflowId == workflow.id) showWorkflowDispatchDialog(workflow, viewModel.actionsState.value ?: RepositoryActionsUiState(), false)
    }

    private fun clearWorkflowDispatchDialogRefs() { workflowDispatchDialogWorkflowId = null; workflowDispatchDialog = null }

    private fun buildWorkflowRefOptions(state: RepositoryActionsUiState): List<String> = buildList {
        fallbackWorkflowRef(state).takeIf { it.isNotBlank() }?.let(::add)
        state.branches.map { it.name }.filter { it.isNotBlank() && it !in this }.forEach(::add)
        state.headBranch.takeIf { it.isNotBlank() && it !in this }?.let(::add)
    }.ifEmpty { listOf("main") }

    private fun fallbackWorkflowRef(state: RepositoryActionsUiState): String = state.defaultBranch
        .ifBlank { state.branches.firstOrNull { it.isDefault }?.name.orEmpty() }
        .ifBlank { state.headBranch }
        .ifBlank { "main" }

    private fun openActionRunDetail(run: RepositoryActionRun) {
        val destinationId = resources.getIdentifier(ActionRunDetailDestinationName, "id", requireContext().packageName)
        if (destinationId == 0) { showMessage(getString(R.string.repository_action_run_detail_missing_destination)); return }
        findNavController().navigate(destinationId, bundleOf(
            RepositoryActionRunDetailFragment.ARG_OWNER to actionsState.owner,
            RepositoryActionRunDetailFragment.ARG_REPO to actionsState.repo,
            RepositoryActionRunDetailFragment.ARG_RUN_ID to run.id
        ))
    }

    private fun openWorkflowInGithub(workflow: RepositoryActionWorkflow) { workflow.htmlUrl?.let(::openUrl) }
    private fun openActionsInGithub() { actionsState.actionsHtmlUrl?.let(::openUrl) }
    private fun openUrl(url: String) {
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        catch (_: ActivityNotFoundException) { showMessage(getString(R.string.repository_actions_open_failed)) }
    }
    private fun showMessage(message: String) { rootView?.let { Snackbar.make(it, message, Snackbar.LENGTH_SHORT).show() } }

    private fun localizeWorkflowState(state: String): String = when (state.lowercase(Locale.US)) {
        "active" -> getString(R.string.repository_actions_settings_enabled)
        else -> state.ifBlank { getString(R.string.repository_action_run_status_unknown) }
    }
    private fun localizeWorkflowTrigger(trigger: String): String = when (trigger.lowercase(Locale.US)) {
        "workflow_dispatch" -> getString(R.string.repository_action_run_event_workflow_dispatch)
        "push" -> getString(R.string.repository_action_run_event_push)
        "pull_request" -> getString(R.string.repository_action_run_event_pull_request)
        "schedule" -> getString(R.string.repository_action_run_event_schedule)
        else -> trigger.replace('_', ' ').ifBlank { getString(R.string.repository_action_run_status_unknown) }
    }

    companion object {
        const val ARG_OWNER = "owner"
        const val ARG_REPO = "repo"
        private const val ActionRunDetailDestinationName = "repository_action_run_detail_fragment"
    }
}