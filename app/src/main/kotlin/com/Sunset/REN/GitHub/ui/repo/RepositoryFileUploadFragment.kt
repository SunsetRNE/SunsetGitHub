package com.Sunset.REN.GitHub.ui.repo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.repo.RepositoryUploadTargetPath
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubTheme
import com.Sunset.REN.GitHub.ui.compose.screens.repo.RepositoryFileUploadDialogState
import com.Sunset.REN.GitHub.ui.compose.screens.repo.RepositoryFileUploadScreen

class RepositoryFileUploadFragment : Fragment() {

    private lateinit var viewModel: RepositoryFileUploadViewModel
    private var repositoryOwner = ""
    private var repositoryName = ""
    private var sourceUri = ""
    private var displayName = ""
    private var visibleDirectories: List<String> = emptyList()
    private var uploadState by mutableStateOf(RepositoryFileUploadUiState())
    private var targetPathDraft by mutableStateOf("")
    private var commitMessageDraft by mutableStateOf("")
    private var generatedCommitMessage = ""
    private var shownConflictDialogKey: String? = null
    private var dialogState by mutableStateOf<RepositoryFileUploadDialogState?>(null)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this)[RepositoryFileUploadViewModel::class.java]
        repositoryOwner = requireArguments().getString(ARG_OWNER).orEmpty()
        repositoryName = requireArguments().getString(ARG_REPO).orEmpty()
        sourceUri = requireArguments().getString(ARG_SOURCE_URI).orEmpty()
        displayName = requireArguments().getString(ARG_DISPLAY_NAME).orEmpty()
        visibleDirectories = requireArguments().getStringArrayList(ARG_VISIBLE_DIRECTORIES).orEmpty()
        uploadState = RepositoryFileUploadUiState(
            owner = repositoryOwner,
            repo = repositoryName,
            sourceUri = sourceUri,
            displayName = displayName
        )
        syncGeneratedCommitMessage(displayName, force = true)

        val composeView = ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SunsetGitHubTheme {
                    RepositoryFileUploadScreen(
                        state = uploadState,
                        repositoryContext = buildRepositoryContextText(),
                        targetPath = targetPathDraft,
                        commitMessage = commitMessageDraft,
                        dialogState = dialogState,
                        onTargetPathChange = ::onTargetPathChanged,
                        onCommitMessageChange = ::onCommitMessageChanged,
                        onShowTargetPathPicker = ::showTargetPathPicker,
                        onSubmit = ::submit,
                        onDismissDialog = ::dismissDialog,
                        onTargetPathSelected = ::selectTargetPath,
                        onOverwriteConflict = ::overwriteConflictTarget,
                        onRenameConflict = ::renameConflictTarget
                    )
                }
            }
        }

        viewModel.uploadState.observe(viewLifecycleOwner) { state -> renderState(state) }
        viewModel.prepare(repositoryOwner, repositoryName, sourceUri, displayName)
        return composeView
    }

    private fun renderState(state: RepositoryFileUploadUiState) {
        uploadState = state
        if (targetPathDraft != state.targetPath) {
            targetPathDraft = state.targetPath
            syncGeneratedCommitMessage(state.targetPath)
        }
        state.pendingConflict?.let(::showConflictDialogIfNeeded)
        if (state.submitSuccess) {
            viewModel.clearSubmitSuccess()
            findNavController().previousBackStackEntry?.savedStateHandle?.set(RESULT_FILE_UPLOADED, true)
            findNavController().previousBackStackEntry?.savedStateHandle?.set(
                RESULT_FILE_UPLOADED_PATH,
                state.uploadedPath.ifBlank { state.targetPath }
            )
            Toast.makeText(requireContext(), R.string.repository_file_upload_success, Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    private fun onTargetPathChanged(value: String) {
        val sanitized = RepositoryUploadTargetPath.sanitize(value)
        targetPathDraft = sanitized
        viewModel.updateTargetPath(sanitized)
        syncGeneratedCommitMessage(sanitized)
    }

    private fun onCommitMessageChanged(value: String) {
        commitMessageDraft = value
        if (value != generatedCommitMessage) {
            generatedCommitMessage = ""
        }
    }

    private fun showTargetPathPicker() {
        dialogState = RepositoryFileUploadDialogState.TargetPathPicker(buildTargetPathOptions())
    }

    private fun selectTargetPath(path: String) {
        dialogState = null
        shownConflictDialogKey = null
        targetPathDraft = path
        viewModel.updateTargetPath(path)
        syncGeneratedCommitMessage(path, force = true)
    }

    private fun submit() {
        viewModel.submit(commitMessageDraft)
    }

    private fun dismissDialog() {
        when (dialogState) {
            is RepositoryFileUploadDialogState.Conflict -> {
                shownConflictDialogKey = null
                viewModel.clearPendingConflict()
            }
            else -> Unit
        }
        dialogState = null
    }

    private fun overwriteConflictTarget() {
        dialogState = null
        viewModel.submit(commitMessageDraft, RepositoryFileWriteConflictResolution.Overwrite)
    }

    private fun renameConflictTarget(state: RepositoryFileUploadDialogState.Conflict) {
        val renamedPath = RepositoryFileAutoRename.buildNextCopyPath(state.targetPath)
        shownConflictDialogKey = null
        dialogState = null
        targetPathDraft = renamedPath
        viewModel.updateTargetPath(renamedPath)
        syncGeneratedCommitMessage(renamedPath, force = true)
        viewModel.submit(commitMessageDraft)
    }

    private fun showConflictDialogIfNeeded(conflict: RepositoryFileWriteConflictUiState) {
        if (shownConflictDialogKey == conflict.dialogKey) return
        shownConflictDialogKey = conflict.dialogKey
        dialogState = RepositoryFileUploadDialogState.Conflict(
            dialogKey = conflict.dialogKey,
            targetPath = conflict.targetPath
        )
    }

    private fun buildTargetPathOptions(): List<String> {
        val currentDirectory = RepositoryUploadTargetPath.normalizeTargetPathAsDirectory(targetPathDraft)
        val sourceDirectory = RepositoryUploadTargetPath.defaultDirectoryForDisplayName(displayName)
        val visibleDirectoryOptions = visibleDirectories.map { RepositoryUploadTargetPath.normalizeDirectory(it) }
        val optionDirectories = listOf(currentDirectory, sourceDirectory) + visibleDirectoryOptions
            .flatMap { directory -> RepositoryUploadTargetPath.expandDirectoryOptions(directory) }
        return optionDirectories.distinct().sortedWith(compareBy<String> { it.count { char -> char == '/' } }.thenBy { it.length })
    }

    private fun syncGeneratedCommitMessage(targetPath: String, force: Boolean = false) {
        if (!force && generatedCommitMessage.isBlank() && commitMessageDraft.isNotBlank()) return
        if (!force && generatedCommitMessage.isNotBlank() && commitMessageDraft != generatedCommitMessage) return
        val displayPath = targetPath.trim().ifBlank { displayName }
        if (displayPath.isBlank()) return
        val nextMessage = getString(R.string.repository_file_edit_default_commit_message, displayPath)
        generatedCommitMessage = nextMessage
        commitMessageDraft = nextMessage
    }

    private fun buildRepositoryContextText(): String {
        return if (repositoryOwner.isBlank() || repositoryName.isBlank()) {
            getString(R.string.repository_file_preview_page_missing_repository)
        } else {
            "$repositoryOwner/$repositoryName"
        }
    }

    companion object {
        const val ARG_OWNER = "owner"
        const val ARG_REPO = "repo"
        const val ARG_SOURCE_URI = "source_uri"
        const val ARG_DISPLAY_NAME = "display_name"
        const val ARG_VISIBLE_DIRECTORIES = "visible_directories"
        const val RESULT_FILE_UPLOADED = "repository_file_uploaded"
        const val RESULT_FILE_UPLOADED_PATH = "repository_file_uploaded_path"
    }
}