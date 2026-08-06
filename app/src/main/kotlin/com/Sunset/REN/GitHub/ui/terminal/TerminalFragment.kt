package com.Sunset.REN.GitHub.ui.terminal

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.Sunset.REN.GitHub.ui.common.CompactBlackDialog
import com.Sunset.REN.GitHub.ui.common.showComposeSingleChoiceDialog
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubTheme
import com.Sunset.REN.GitHub.ui.compose.screens.terminal.TerminalScreen
import com.Sunset.REN.GitHub.ui.filemanager.SelectionActionItem
import com.Sunset.REN.GitHub.ui.filemanager.SelectionActionSheetDialog
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TerminalFragment : Fragment() {
    private lateinit var viewModel: TerminalViewModel
    private var uiState by mutableStateOf(TerminalUiState())
    private var commandText by mutableStateOf("")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this)[TerminalViewModel::class.java]
        uiState = viewModel.state.value ?: TerminalUiState()
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SunsetGitHubTheme {
                    TerminalScreen(
                        state = uiState,
                        commandText = commandText,
                        onCommandTextChange = { commandText = it },
                        onRunCommand = ::submitCommand,
                        onQuickHelp = { runQuickCommand("help") },
                        onQuickStatus = { runQuickCommand("status") },
                        onQuickDryRun = { runQuickCommand("dry-run") },
                        onSelectWorkspace = ::showWorkspaceDialog,
                        onOpenCommandPanel = ::showCommandPanel,
                        onManageExports = ::showExportManager,
                        onHistoryPrevious = ::showPreviousHistoryCommand,
                        onHistoryNext = ::showNextHistoryCommand,
                        onCopyOutput = ::copyOutputToClipboard,
                        onExportOutput = ::exportOutputToFile,
                        onShareOutput = ::shareOutputFile
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeState()
        viewModel.loadInitialWorkspace(
            preferredWorkspaceId = arguments?.getString(ARG_WORKSPACE_ID),
            preferredOwner = arguments?.getString(ARG_REPOSITORY_OWNER),
            preferredRepo = arguments?.getString(ARG_REPOSITORY_NAME),
            seedCommand = arguments?.getString(ARG_SEED_COMMAND),
            autoRunSeedCommand = arguments?.getBoolean(ARG_AUTO_RUN_SEED_COMMAND, false) ?: false
        )
    }

    private fun observeState() {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            uiState = state
            viewModel.consumeSeedCommand()?.let { seed ->
                commandText = seed.command
                if (seed.autoRun) {
                    viewModel.runCommand(seed.command)
                    commandText = ""
                }
            }
        }
    }

    private fun showWorkspaceDialog() {
        val state = viewModel.state.value ?: return
        if (state.workspaces.isEmpty()) {
            Toast.makeText(requireContext(), "暂无可选工作区", Toast.LENGTH_SHORT).show()
            return
        }
        showComposeSingleChoiceDialog(
            context = requireContext(),
            title = "选择工作区",
            items = state.workspaces,
            selected = state.selectedWorkspace ?: state.workspaces.first(),
            label = { workspace ->
                val remote = workspace.remoteBinding?.repositoryFullName ?: "未绑定远端"
                "${workspace.name}\n${workspace.id.take(8)} · $remote"
            },
            onSelected = { workspace -> viewModel.selectWorkspace(workspace.id) }
        )
    }

    private fun showCommandPanel() {
        val favorites = viewModel.favoriteCommands()
        val templates = viewModel.commandTemplates()
        val actions = buildList {
            addAll(favorites.map { command ->
                SelectionActionItem(label = "★ ${command.title}\n${command.command}") {
                    commandText = command.command
                }
            })
            addAll(templates.map { command ->
                SelectionActionItem(label = "模板 · ${command.title}\n${command.command}") {
                    commandText = command.command
                }
            })
        }
        SelectionActionSheetDialog.show(
            context = requireContext(),
            title = "命令收藏 / 模板",
            actions = actions
        )
    }

    private fun showExportManager() {
        val files = viewModel.listExportedOutputs()
        val message = if (files.isEmpty()) {
            "暂无已导出的终端输出文件。"
        } else {
            files.take(12).joinToString("\n") { file ->
                "${file.name} · ${Formatter.formatShortFileSize(requireContext(), file.length())} · ${formatTimestamp(file.lastModified())}"
            } + if (files.size > 12) "\n… 还有 ${files.size - 12} 个" else ""
        }
        CompactBlackDialog.show(
            context = requireContext(),
            title = "导出文件管理",
            message = message,
            negativeText = getString(android.R.string.cancel),
            positiveText = "清理全部",
            onPositiveClick = {
                val deleted = viewModel.clearExportedOutputs()
                Toast.makeText(requireContext(), "已清理 $deleted 个导出文件", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun submitCommand() {
        val command = commandText
        viewModel.runCommand(command)
        commandText = ""
    }

    private fun runQuickCommand(command: String) {
        commandText = command
        viewModel.runCommand(command)
        commandText = ""
    }

    private fun showPreviousHistoryCommand() {
        viewModel.previousHistory()?.let { command -> commandText = command }
    }

    private fun showNextHistoryCommand() {
        commandText = viewModel.nextHistory().orEmpty()
    }

    private fun copyOutputToClipboard() {
        val output = viewModel.state.value?.output.orEmpty()
        if (output.isBlank()) return
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("SunsetGitHub terminal output", output))
        Toast.makeText(requireContext(), "终端输出已复制", Toast.LENGTH_SHORT).show()
    }

    private fun exportOutputToFile() {
        val file = viewModel.exportOutput()
        if (file == null) {
            Toast.makeText(requireContext(), "暂无可导出的终端输出", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "已导出：${file.absolutePath}", Toast.LENGTH_LONG).show()
        }
    }

    private fun shareOutputFile() {
        val file = viewModel.exportOutput()
        if (file == null) {
            Toast.makeText(requireContext(), "暂无可分享的终端输出", Toast.LENGTH_SHORT).show()
            return
        }
        shareFile(file)
    }

    private fun shareFile(file: File) {
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.terminal.export.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "SunsetGitHub terminal output")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "分享终端输出"))
    }

    private fun formatTimestamp(timestamp: Long): String {
        return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    companion object {
        const val ARG_WORKSPACE_ID = "workspace_id"
        const val ARG_REPOSITORY_OWNER = "repository_owner"
        const val ARG_REPOSITORY_NAME = "repository_name"
        const val ARG_SEED_COMMAND = "seed_command"
        const val ARG_AUTO_RUN_SEED_COMMAND = "auto_run_seed_command"
    }
}