package com.Sunset.REN.GitHub.ui.repo

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.data.local.ThemePreferenceStore
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubTheme
import com.Sunset.REN.GitHub.ui.compose.screens.repo.RepositoryFileEditScreen
import com.Sunset.REN.GitHub.ui.editor.ReleasableTextEditor
import com.Sunset.REN.GitHub.ui.editor.TextEditorAdapter
import com.Sunset.REN.GitHub.ui.editor.TextEditorConfig
import com.Sunset.REN.GitHub.ui.editor.TextEditorEngine
import com.Sunset.REN.GitHub.ui.editor.TextEditorFactory
import com.Sunset.REN.GitHub.ui.editor.TextEditorHost
import java.io.File

class RepositoryFileEditFragment : Fragment() {

    private lateinit var viewModel: RepositoryFileEditViewModel
    private lateinit var editorAdapter: TextEditorAdapter
    private lateinit var editorHost: TextEditorHost
    private lateinit var backCallback: OnBackPressedCallback

    private var repositoryOwner: String = ""
    private var repositoryName: String = ""
    private var filePath: String = ""
    private var fileName: String = ""
    private var previewMode by mutableStateOf(false)
    private var isCreateMode: Boolean = false
    private var isRenderingContent: Boolean = false
    private var latestState by mutableStateOf(RepositoryFileEditUiState())
    private var targetPathDraft by mutableStateOf("")
    private var commitMessageDraft by mutableStateOf("")
    private var dialogState by mutableStateOf<RepositoryFileEditDialogState?>(null)
    private var shownConflictDialogKey: String? = null
    private var hasPositionedInitialContent: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this)[RepositoryFileEditViewModel::class.java]
        repositoryOwner = requireArguments().getString(ARG_OWNER).orEmpty()
        repositoryName = requireArguments().getString(ARG_REPO).orEmpty()
        filePath = requireArguments().getString(ARG_PATH).orEmpty()
        fileName = requireArguments().getString(ARG_NAME).orEmpty()
        previewMode = requireArguments().getBoolean(ARG_PREVIEW_MODE, false)
        val initialContent = requireArguments().getString(ARG_INITIAL_CONTENT).orEmpty()
        val initialCommitMessage = requireArguments().getString(ARG_INITIAL_COMMIT_MESSAGE).orEmpty()
        isCreateMode = filePath.isBlank()
        targetPathDraft = if (isCreateMode) fileName else filePath
        commitMessageDraft = initialCommitMessage.ifBlank { buildDefaultCommitMessage(targetPathDraft.ifBlank { filePath.ifBlank { fileName } }) }

        val editorContainer = FrameLayout(requireContext())
        editorHost = TextEditorFactory.create(
            inflater = inflater,
            parent = editorContainer,
            config = TextEditorConfig(
                preferredEngine = resolvePreferredEditorEngine(),
                languageMode = resolveEditorLanguageMode(),
                softWrap = shouldUseSoftWrapEditor()
            )
        )
        editorAdapter = editorHost.adapter
        editorAdapter.setReadOnly(previewMode)
        editorAdapter.setOnTextChangedListener { text ->
            if (!isRenderingContent) viewModel.updateContent(text)
        }
        editorAdapter.setOnSelectionChangedListener { }

        val composeView = ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SunsetGitHubTheme {
                    RepositoryFileEditScreen(
                        state = latestState,
                        owner = repositoryOwner,
                        repo = repositoryName,
                        previewMode = previewMode,
                        targetPathDraft = targetPathDraft,
                        commitMessageDraft = commitMessageDraft,
                        editorHost = editorHost,
                        onTargetPathChange = ::updateTargetPath,
                        onCommitMessageChange = { commitMessageDraft = it },
                        onEnterEditMode = ::enterEditMode,
                        onSubmit = ::showSubmitConfirmation,
                        onDelete = ::showDeleteConfirmation,
                        onCopy = ::copyCurrentContentToClipboard,
                        onFocusEditor = { editorAdapter.focus() }
                    )
                    DialogHost()
                }
            }
        }

        setupBackConfirmation()
        viewModel.editState.observe(viewLifecycleOwner) { state -> renderState(state) }
        if (isCreateMode) {
            viewModel.prepareNewFile(repositoryOwner, repositoryName, fileName, initialContent = initialContent)
        } else {
            viewModel.loadFile(repositoryOwner, repositoryName, filePath, fileName)
        }
        return composeView
    }

    private fun renderState(state: RepositoryFileEditUiState) {
        latestState = state
        val displayTitle = state.fileName.ifBlank { fileName.ifBlank { getString(R.string.repository_file_create_title) } }
        applyToolbarTitle(displayTitle)
        if (targetPathDraft.isBlank() || state.isCreateMode) {
            val statePath = state.filePath.ifBlank { targetPathDraft }
            if (statePath.isNotBlank() && statePath != targetPathDraft) targetPathDraft = statePath
        }
        editorAdapter.setReadOnly(previewMode || !canEditContent(state))
        if (editorAdapter.getText() != state.content) {
            isRenderingContent = true
            editorAdapter.setText(state.content)
            if (!hasPositionedInitialContent) {
                val selection = if (previewMode) 0 else state.content.length
                editorAdapter.setSelection(selection, selection)
                if (previewMode) editorAdapter.scrollToTop() else editorAdapter.scrollToSelectionEnd()
                hasPositionedInitialContent = true
            }
            isRenderingContent = false
        }
        backCallback.isEnabled = state.hasUnsavedChanges
        state.pendingConflict?.let(::showConflictDialogIfNeeded)
        if (state.deleteSuccess) {
            viewModel.clearDeleteSuccess()
            notifyFileUpdated(state.filePath.ifBlank { filePath })
            Toast.makeText(requireContext(), R.string.repository_file_delete_success, Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }
        if (state.submitSuccess) {
            viewModel.clearSubmitSuccess()
            notifyFileUpdated(state.filePath)
            Toast.makeText(
                requireContext(),
                if (state.isCreateMode) R.string.repository_file_create_submit_success else R.string.repository_file_edit_submit_success,
                Toast.LENGTH_SHORT
            ).show()
            findNavController().navigateUp()
        }
    }

    private fun canEditContent(state: RepositoryFileEditUiState): Boolean {
        return !state.isLoading && !state.isSubmitting && !state.isDeleting && (state.originalSha.isNotBlank() || state.isCreateMode)
    }

    private fun updateTargetPath(path: String) {
        targetPathDraft = path
        viewModel.updateTargetPath(path)
        if (commitMessageDraft.isBlank() || commitMessageDraft == buildDefaultCommitMessage(fileName)) {
            commitMessageDraft = buildDefaultCommitMessage(path)
        }
    }

    private fun enterEditMode() {
        previewMode = false
        editorAdapter.setReadOnly(!canEditContent(latestState))
        editorAdapter.focus()
    }

    private fun showSubmitConfirmation() {
        val state = latestState
        if (!state.canSubmit || state.isSubmitting || state.isDeleting) return
        dialogState = RepositoryFileEditDialogState.Submit
    }

    private fun submitCurrent(conflictResolution: RepositoryFileWriteConflictResolution = RepositoryFileWriteConflictResolution.Prompt) {
        dialogState = null
        viewModel.submit(commitMessageDraft, conflictResolution)
    }

    private fun showDeleteConfirmation() {
        val state = latestState
        if (state.isCreateMode || state.originalSha.isBlank() || state.isDeleting || state.isSubmitting) return
        dialogState = RepositoryFileEditDialogState.Delete
    }

    private fun deleteCurrentFile() {
        dialogState = null
        viewModel.deleteFile(getString(R.string.repository_file_delete_default_commit_message, latestState.fileName.ifBlank { filePath }))
    }

    private fun showConflictDialogIfNeeded(conflict: RepositoryFileWriteConflictUiState) {
        if (shownConflictDialogKey == conflict.dialogKey) return
        shownConflictDialogKey = conflict.dialogKey
        dialogState = RepositoryFileEditDialogState.Conflict(conflict)
    }

    private fun copyCurrentContentToClipboard() {
        val text = editorAdapter.getText().ifBlank { latestState.content }
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(latestState.fileName.ifBlank { fileName }, text))
        Toast.makeText(requireContext(), R.string.repository_file_tools_content_copied, Toast.LENGTH_SHORT).show()
    }

    private fun notifyFileUpdated(path: String) {
        findNavController().previousBackStackEntry?.savedStateHandle?.set(RESULT_FILE_UPDATED, true)
        findNavController().previousBackStackEntry?.savedStateHandle?.set(RESULT_FILE_UPDATED_PATH, path)
    }

    private fun setupBackConfirmation() {
        backCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                dialogState = RepositoryFileEditDialogState.Discard
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backCallback)
    }

    private fun discardAndGoBack() {
        dialogState = null
        backCallback.isEnabled = false
        findNavController().navigateUp()
    }

    private fun applyToolbarTitle(title: String) {
        (activity as? AppCompatActivity)?.supportActionBar?.title = title
        (activity as? AppCompatActivity)?.supportActionBar?.subtitle = "$repositoryOwner/$repositoryName"
    }

    private fun resolvePreferredEditorEngine(): TextEditorEngine {
        return if (ThemePreferenceStore(requireContext()).isSoraEditorEnabled()) TextEditorEngine.Sora else TextEditorEngine.SoraFallback
    }

    private fun resolveEditorLanguageMode(): String? {
        val path = filePath.ifBlank { fileName }.lowercase()
        if (path.substringAfterLast('/').startsWith("readme")) return "markdown"
        return when (path.substringAfterLast('.', missingDelimiterValue = "")) {
            "kt", "kts" -> "kotlin"
            "java" -> "java"
            "js", "mjs", "cjs" -> "javascript"
            "ts", "tsx" -> "typescript"
            "py" -> "python"
            "go" -> "go"
            "rs" -> "rust"
            "c", "h" -> "c"
            "cc", "cpp", "cxx", "hpp" -> "cpp"
            "json" -> "json"
            "xml" -> "xml"
            "html", "htm" -> "html"
            "svg" -> "svg"
            "css" -> "css"
            "md", "markdown", "mdown", "mkdn" -> "markdown"
            "yml", "yaml" -> "yaml"
            "sh", "bash", "zsh" -> "shell"
            else -> null
        }
    }

    private fun shouldUseSoftWrapEditor(): Boolean {
        return resolveEditorLanguageMode() in setOf("markdown", "html", "svg")
    }

    private fun buildDefaultCommitMessage(path: String): String {
        val display = path.ifBlank { fileName.ifBlank { getString(R.string.repository_file_create_title) } }
        return if (isCreateMode) {
            getString(R.string.repository_file_edit_default_commit_message, display)
        } else {
            getString(R.string.repository_file_edit_default_commit_message, display)
        }
    }

    @Composable
    private fun DialogHost() {
        when (val dialog = dialogState) {
            null -> Unit
            RepositoryFileEditDialogState.Submit -> AlertDialog(
                onDismissRequest = { dialogState = null },
                title = { Text("提交文件") },
                text = { Text("确定提交当前文件改动吗？") },
                confirmButton = { TextButton(onClick = { submitCurrent() }) { Text("提交") } },
                dismissButton = { TextButton(onClick = { dialogState = null }) { Text("取消") } }
            )
            RepositoryFileEditDialogState.Delete -> AlertDialog(
                onDismissRequest = { dialogState = null },
                title = { Text("删除文件") },
                text = { Text("确定删除 ${latestState.fileName.ifBlank { fileName }} 吗？该操作会提交到仓库。") },
                confirmButton = { TextButton(onClick = ::deleteCurrentFile) { Text("删除") } },
                dismissButton = { TextButton(onClick = { dialogState = null }) { Text("取消") } }
            )
            RepositoryFileEditDialogState.Discard -> AlertDialog(
                onDismissRequest = { dialogState = null },
                title = { Text("放弃更改") },
                text = { Text("当前文件有未保存更改，确定返回吗？") },
                confirmButton = { TextButton(onClick = ::discardAndGoBack) { Text("放弃") } },
                dismissButton = { TextButton(onClick = { dialogState = null }) { Text("继续编辑") } }
            )
            is RepositoryFileEditDialogState.Conflict -> AlertDialog(
                onDismissRequest = {
                    viewModel.clearPendingConflict()
                    dialogState = null
                },
                title = { Text("文件已存在") },
                text = { Text("目标路径 ${dialog.conflict.targetPath} 已存在，是否覆盖现有文件？") },
                confirmButton = { TextButton(onClick = { submitCurrent(RepositoryFileWriteConflictResolution.Overwrite) }) { Text("覆盖") } },
                dismissButton = {
                    TextButton(onClick = {
                        viewModel.clearPendingConflict()
                        dialogState = null
                    }) { Text("取消") }
                }
            )
        }
    }

    override fun onDestroyView() {
        (activity as? AppCompatActivity)?.supportActionBar?.subtitle = null
        if (::editorAdapter.isInitialized) {
            editorAdapter.setOnTextChangedListener(null)
            editorAdapter.setOnSelectionChangedListener(null)
            (editorAdapter as? ReleasableTextEditor)?.release()
        }
        if (::backCallback.isInitialized) backCallback.remove()
        super.onDestroyView()
    }

    private sealed class RepositoryFileEditDialogState {
        data object Submit : RepositoryFileEditDialogState()
        data object Delete : RepositoryFileEditDialogState()
        data object Discard : RepositoryFileEditDialogState()
        data class Conflict(val conflict: RepositoryFileWriteConflictUiState) : RepositoryFileEditDialogState()
    }

    companion object {
        const val ARG_OWNER = "owner"
        const val ARG_REPO = "repo"
        const val ARG_PATH = "path"
        const val ARG_NAME = "name"
        const val ARG_PREVIEW_MODE = "preview_mode"
        const val ARG_INITIAL_CONTENT = "initial_content"
        const val ARG_INITIAL_COMMIT_MESSAGE = "initial_commit_message"
        const val RESULT_FILE_UPDATED = "repository_file_updated"
        const val RESULT_FILE_UPDATED_PATH = "repository_file_updated_path"
    }
}
