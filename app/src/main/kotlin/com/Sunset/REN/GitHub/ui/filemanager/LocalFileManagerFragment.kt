package com.Sunset.REN.GitHub.ui.filemanager

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.data.filemanager.SafDirectoryRecord
import com.Sunset.REN.GitHub.domain.filemanager.ArchiveFormatResolver
import com.Sunset.REN.GitHub.domain.filemanager.FileEntrySource
import com.Sunset.REN.GitHub.domain.filemanager.FileEntryType
import com.Sunset.REN.GitHub.domain.filemanager.FileLocation
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerEntry
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerListOptions
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerPaneId
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerSearchOptions
import com.Sunset.REN.GitHub.domain.filemanager.FileToolAction
import com.Sunset.REN.GitHub.domain.filemanager.FileToolId
import com.Sunset.REN.GitHub.domain.filemanager.FileToolRegistry
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubTheme
import com.Sunset.REN.GitHub.ui.compose.screens.filemanager.FileManagerPaneTarget
import com.Sunset.REN.GitHub.ui.compose.screens.filemanager.FileManagerTaskProgressState
import com.Sunset.REN.GitHub.ui.compose.screens.filemanager.LocalFileManagerScreen
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException
import java.io.File
import java.util.ArrayDeque

class LocalFileManagerFragment : Fragment() {

    private lateinit var viewModel: LocalFileManagerViewModel
    private var mode: String = MODE_PICK_FILE
    private var currentState by mutableStateOf(LocalFileManagerUiState())
    private var secondaryState by mutableStateOf<LocalFileManagerUiState?>(null)
    private var selectedEntryIds by mutableStateOf<Set<String>>(emptySet())
    private var secondarySelectedEntryIds by mutableStateOf<Set<String>>(emptySet())
    private var inputDialog by mutableStateOf<FileManagerInputDialog?>(null)
    private var batchRenameDialog by mutableStateOf<BatchRenameRequest?>(null)
    private var confirmDeleteDialog by mutableStateOf(false)
    private var confirmSecondaryDeleteDialog by mutableStateOf(false)
    private var confirmClearRecycleBinDialog by mutableStateOf(false)
    private var pendingTransferDialog by mutableStateOf<DualPaneTransferRequest?>(null)
    private var searchQuery by mutableStateOf("")
    private var secondarySearchQuery by mutableStateOf("")
    private var searchResultEntries by mutableStateOf<List<FileManagerEntry>?>(null)
    private var isSearching by mutableStateOf(false)
    private var searchBasePath by mutableStateOf("")
    private var searchJob: Job? = null
    private var focusedPane by mutableStateOf(FileManagerPaneTarget.Primary)
    private var primaryListOptions by mutableStateOf(FileManagerListOptions())
    private var secondaryListOptions by mutableStateOf(FileManagerListOptions())
    private var secondaryCurrentPath: String = ""
    private val secondaryBackStack = ArrayDeque<String>()
    private val secondaryForwardStack = ArrayDeque<String>()
    private var secondaryDirectoryJob: Job? = null
    private var activeTransferJob: Job? = null
    private var taskProgressState by mutableStateOf<FileManagerTaskProgressState?>(null)
    private val openAuthorizedDirectory = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri ?: return@registerForActivityResult
        addAuthorizedDirectory(uri)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this)[LocalFileManagerViewModel::class.java]
        mode = arguments?.getString(ARG_MODE, MODE_PICK_FILE) ?: MODE_PICK_FILE
        primaryListOptions = viewModel.listOptionsForPane(FileManagerPaneId.Left)
        secondaryListOptions = viewModel.listOptionsForPane(FileManagerPaneId.Right)

        val composeView = ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SunsetGitHubTheme {
                    LocalFileManagerScreen(
                        state = currentState,
                        secondaryState = secondaryState,
                        isPickerMode = mode == MODE_PICK_FILE,
                        selectedEntryIds = selectedEntryIds,
                        secondarySelectedEntryIds = secondarySelectedEntryIds,
                        searchQuery = searchQuery,
                        secondarySearchQuery = secondarySearchQuery,
                        onSearchQueryChange = { searchQuery = it; clearSelection() },
                        onSecondarySearchQueryChange = { secondarySearchQuery = it; clearSecondarySelection() },
                        searchResultEntries = searchResultEntries,
                        isSearchInProgress = isSearching,
                        onSubmitSearch = { performRecursiveSearch(it) },
                        onExitSearchResults = { exitSearchResults() },
                        onRangeSelect = { range ->
                            selectedEntryIds = selectedEntryIds + range.map { it.id }.toSet()
                        },
                        onSecondaryRangeSelect = { range ->
                            secondarySelectedEntryIds = secondarySelectedEntryIds + range.map { it.id }.toSet()
                        },
                        listOptions = primaryListOptions,
                        secondaryListOptions = secondaryListOptions,
                        onListOptionsChange = { options ->
                            primaryListOptions = options
                            viewModel.setListOptionsForPane(FileManagerPaneId.Left, options)
                        },
                        onSecondaryListOptionsChange = { options ->
                            secondaryListOptions = options
                            viewModel.setListOptionsForPane(FileManagerPaneId.Right, options)
                        },
                        onInvertSelection = { invertSelection(focusedPane == FileManagerPaneTarget.Primary) },
                        onShowEntryProperties = ::showEntryProperties,
                        onCopyEntryPath = ::copyEntryPath,
                        onZipSelection = { zipFocusedSelection(isPrimary = focusedPane == FileManagerPaneTarget.Primary) },
                        onRestoreSelection = run {
                            val focusedPath = if (focusedPane == FileManagerPaneTarget.Primary) currentState.currentPath else secondaryCurrentPath
                            if (viewModel.isRecycleBinPath(focusedPath)) {
                                { restoreRecycleBinSelection(focusedPane == FileManagerPaneTarget.Primary) }
                            } else {
                                null
                            }
                        },
                        onNavigateToPath = { path ->
                            focusedPane = FileManagerPaneTarget.Primary
                            searchQuery = ""
                            if (viewModel.openDirectoryPath(path)) clearSelection()
                        },
                        onSecondaryNavigateToPath = { path ->
                            // 点击右窗格面包屑属于与右窗格的交互，MT 模型下应同步聚焦右窗格
                            focusedPane = FileManagerPaneTarget.Secondary
                            openSecondaryDirectory(path)
                        },
                        onOpenAppFiles = { openLocation(FileLocation.AppFiles) },
                        onOpenDownloads = { openLocation(FileLocation.Downloads) },
                        onOpenCache = { openLocation(FileLocation.AppCache) },
                        onOpenRecycleBin = { viewModel.openRecycleBin(); clearSelection() },
                        onOpenAuthorizedDirectoryPicker = { openAuthorizedDirectory.launch(null) },
                        onOpenAuthorizedDirectory = ::openAuthorizedDirectoryRecord,
                        onToggleFavorite = ::toggleCurrentFavorite,
                        onCreateDirectory = { showInputDialog(FileManagerInputDialog.CreateDirectory()) },
                        onCreateFile = { showInputDialog(FileManagerInputDialog.CreateFile()) },
                        onSecondaryCreateDirectory = { showInputDialog(FileManagerInputDialog.CreateDirectory(PaneTarget.Secondary)) },
                        onSecondaryCreateFile = { showInputDialog(FileManagerInputDialog.CreateFile(PaneTarget.Secondary)) },
                        onOpenFavorite = { viewModel.openFavorite(it); clearSelection() },
                        onOpenRecent = { viewModel.openRecent(it); clearSelection() },
                        onClearRecent = { viewModel.clearRecentDirectories(); clearSelection() },
                        onRefresh = { viewModel.refresh(); clearSelection(); refreshSecondaryDirectory() },
                        onBack = { viewModel.navigateBack(); clearSelection() },
                        onForward = { viewModel.navigateForward(); clearSelection() },
                        onUp = { viewModel.openParent(); clearSelection() },
                        onSecondaryRefresh = { refreshSecondaryDirectory() },
                        onSecondaryBack = { navigateSecondaryBack() },
                        onSecondaryForward = { navigateSecondaryForward() },
                        onSecondaryUp = { navigateSecondaryUp() },
                        onEntryClick = ::handleEntryClick,
                        onSecondaryEntryClick = ::handleSecondaryEntryClick,
                        onEntryLongClick = ::handleEntryLongClick,
                        onSecondaryEntryLongClick = ::handleSecondaryEntryLongClick,
                        onToggleEntrySelected = ::toggleEntrySelected,
                        onToggleSecondaryEntrySelected = ::toggleSecondaryEntrySelected,
                        onSelectAll = ::selectAllEntries,
                        onSecondarySelectAll = ::selectAllSecondaryEntries,
                        onClearSelection = ::clearSelection,
                        onSecondaryClearSelection = ::clearSecondarySelection,
                        onRenameSelected = ::showRenameSelectedDialog,
                        onSecondaryRenameSelected = ::showRenameSecondarySelectedDialog,
                        onDeleteSelected = { if (selectedEntries().isNotEmpty()) confirmDeleteDialog = true },
                        onSecondaryDeleteSelected = { if (secondarySelectedEntries().isNotEmpty()) confirmSecondaryDeleteDialog = true },
                        onCopyPrimaryToSecondary = { requestPrimaryTransferToSecondary(move = false) },
                        onMovePrimaryToSecondary = { requestPrimaryTransferToSecondary(move = true) },
                        onCopySecondaryToPrimary = { requestSecondaryTransferToPrimary(move = false) },
                        onMoveSecondaryToPrimary = { requestSecondaryTransferToPrimary(move = true) },
                        onSecondaryOpenDownloads = { openSecondaryDownloads() },
                        onSecondaryOpenAppFiles = { openSecondaryAppFiles() },
                        onDualPaneMenu = ::showDualPaneLocationMenu,
                        onDualPaneMore = ::showDualPaneMoreMenu,
                        focusedPane = focusedPane,
                        taskProgressState = taskProgressState,
                        forceDualPane = mode == MODE_MANAGE,
                        onTaskProgressDismiss = { taskProgressState = null },
                        onTaskProgressCancel = ::cancelActiveTransfer,
                        onPrimaryPaneFocused = { focusedPane = FileManagerPaneTarget.Primary },
                        onSecondaryPaneFocused = { focusedPane = FileManagerPaneTarget.Secondary }
                    )
                    InputDialogHost()
                    BatchRenameDialogHost()
                    DeleteConfirmDialogHost()
                    ClearRecycleBinDialogHost()
                    SecondaryDeleteConfirmDialogHost()
                    TransferConfirmDialogHost()
                }
            }
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            currentState = state
            selectedEntryIds = selectedEntryIds.intersect(state.entries.map { it.id }.toSet())
            if (searchResultEntries != null && state.currentPath != searchBasePath) {
                exitSearchResults()
            }
            ensureSecondaryDirectoryLoaded(state.currentPath)
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (searchResultEntries != null) {
                    exitSearchResults()
                    return
                }
                // MT 管理器行为模型：系统返回作用于“当前聚焦窗格”的历史栈，
                // 而不是固定作用于左（主）窗格。
                val handled = when (focusedPane) {
                    FileManagerPaneTarget.Primary -> viewModel.navigateBack()
                    FileManagerPaneTarget.Secondary -> navigateSecondaryBack()
                }
                if (!handled) {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        })
        composeView.post {
            if (mode == MODE_MANAGE) {
                val storageRoot = Environment.getExternalStorageDirectory() ?: File("/storage/emulated/0")
                if (!viewModel.openDirectoryPath(storageRoot.absolutePath)) {
                    viewModel.loadInitialLocation()
                }
                // 启动路径是历史根：避免按返回时先跳回打开管理器前的初始位置（如 AppFiles）
                viewModel.clearNavigationHistory()
            } else {
                viewModel.loadInitialLocation()
            }
        }
        return composeView
    }

    private fun handleEntryClick(entry: FileManagerEntry) {
        focusedPane = FileManagerPaneTarget.Primary
        if (selectedEntryIds.isNotEmpty() && entry.type != FileEntryType.Parent) {
            toggleEntrySelected(entry)
            return
        }
        when (entry.type) {
            FileEntryType.Parent, FileEntryType.Directory -> openDirectoryEntry(entry)
            else -> {
                if (mode == MODE_PICK_FILE) finishPickingFile(entry) else showOpenWithTools(entry, PaneTarget.Primary)
            }
        }
    }

    private fun handleEntryLongClick(entry: FileManagerEntry) {
        focusedPane = FileManagerPaneTarget.Primary
        if (entry.type != FileEntryType.Parent) toggleEntrySelected(entry)
    }

    private fun handleSecondaryEntryClick(entry: FileManagerEntry) {
        focusedPane = FileManagerPaneTarget.Secondary
        if (secondarySelectedEntryIds.isNotEmpty() && entry.type != FileEntryType.Parent) {
            toggleSecondaryEntrySelected(entry)
            return
        }
        when (entry.type) {
            FileEntryType.Parent, FileEntryType.Directory -> openSecondaryDirectoryEntry(entry)
            else -> showOpenWithTools(entry, PaneTarget.Secondary)
        }
    }

    private fun handleSecondaryEntryLongClick(entry: FileManagerEntry) {
        focusedPane = FileManagerPaneTarget.Secondary
        if (entry.type != FileEntryType.Parent) toggleSecondaryEntrySelected(entry)
    }

    private fun openLocation(location: FileLocation) {
        searchQuery = ""
        viewModel.openLocation(location)
        clearSelection()
    }

    private fun openSecondaryDownloads() {
        focusedPane = FileManagerPaneTarget.Secondary
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        openSecondaryDirectory(downloads.absolutePath)
    }

    private fun openSecondaryAppFiles() {
        focusedPane = FileManagerPaneTarget.Secondary
        openSecondaryDirectory(requireContext().filesDir.absolutePath)
    }

    private fun showDualPaneLocationMenu() {
        SelectionActionSheetDialog.show(
            context = requireContext(),
            title = if (focusedPane == FileManagerPaneTarget.Primary) "左窗格位置" else "右窗格位置",
            actions = listOf(
                SelectionActionItem("应用文件") {
                    if (focusedPane == FileManagerPaneTarget.Primary) openLocation(FileLocation.AppFiles) else openSecondaryAppFiles()
                },
                SelectionActionItem("下载") {
                    if (focusedPane == FileManagerPaneTarget.Primary) openLocation(FileLocation.Downloads) else openSecondaryDownloads()
                },
                SelectionActionItem("缓存") {
                    if (focusedPane == FileManagerPaneTarget.Primary) openLocation(FileLocation.AppCache) else openSecondaryDirectory(requireContext().cacheDir.absolutePath)
                },
                SelectionActionItem("外部存储根目录") {
                    val storageRoot = Environment.getExternalStorageDirectory() ?: File("/storage/emulated/0")
                    if (focusedPane == FileManagerPaneTarget.Primary) viewModel.openDirectoryPath(storageRoot.absolutePath) else openSecondaryDirectory(storageRoot.absolutePath)
                }
            )
        )
    }

    private fun showDualPaneMoreMenu() {
        val isPrimary = focusedPane == FileManagerPaneTarget.Primary
        val selectedCount = if (isPrimary) selectedEntryIds.size else secondarySelectedEntryIds.size
        val actions = mutableListOf<SelectionActionItem>()
        val currentPanePath = if (isPrimary) currentState.currentPath else secondaryCurrentPath
        val oppositePanePath = if (isPrimary) secondaryCurrentPath else currentState.currentPath
        val currentPaneWritable = isWritableFileManagerTarget(currentPanePath)
        val oppositePaneWritable = isWritableFileManagerTarget(oppositePanePath)
        actions += SelectionActionItem(
            label = "新建文件夹",
            enabled = currentPaneWritable,
            disabledReason = writeDisabledReason(currentPanePath),
            onClick = {
                showInputDialog(FileManagerInputDialog.CreateDirectory(if (isPrimary) PaneTarget.Primary else PaneTarget.Secondary))
            }
        )
        actions += SelectionActionItem(
            label = "新建文件",
            enabled = currentPaneWritable,
            disabledReason = writeDisabledReason(currentPanePath),
            onClick = {
                showInputDialog(FileManagerInputDialog.CreateFile(if (isPrimary) PaneTarget.Primary else PaneTarget.Secondary))
            }
        )
        actions += SelectionActionItem("刷新") {
            if (isPrimary) viewModel.refresh() else refreshSecondaryDirectory()
        }
        actions += SelectionActionItem("上级") {
            if (isPrimary) viewModel.openParent() else navigateSecondaryUp()
        }
        actions += SelectionActionItem("全选") {
            if (isPrimary) selectAllEntries() else selectAllSecondaryEntries()
        }
        if (selectedCount > 0) {
            val selectedEntries = if (isPrimary) selectedEntries() else secondarySelectedEntries()
            actions += SelectionActionItem("清除选择") {
                if (isPrimary) clearSelection() else clearSecondarySelection()
            }
            if (selectedCount == 1) {
                val entry = selectedEntries.singleOrNull()
                if (entry != null) {
                    actions += SelectionActionItem("打开方式") {
                        showOpenWithTools(entry, if (isPrimary) PaneTarget.Primary else PaneTarget.Secondary)
                    }
                    actions += SelectionActionItem("属性") {
                        showEntryProperties(entry)
                    }
                    if (entry.capabilities.canRename) {
                        actions += SelectionActionItem("重命名") {
                            showInputDialog(
                                FileManagerInputDialog.Rename(
                                    entry = entry,
                                    initialName = entry.name,
                                    target = if (isPrimary) PaneTarget.Primary else PaneTarget.Secondary
                                )
                            )
                        }
                    }
                }
            }
            if (selectedCount >= 2) {
                val renamableEntries = selectedEntries.filter { it.capabilities.canRename }
                if (renamableEntries.isNotEmpty()) {
                    actions += SelectionActionItem("批量重命名") {
                        batchRenameDialog = BatchRenameRequest(
                            entries = renamableEntries,
                            target = if (isPrimary) PaneTarget.Primary else PaneTarget.Secondary
                        )
                    }
                }
            }
            actions += SelectionActionItem(
                label = "压缩为 ZIP",
                enabled = currentPaneWritable,
                disabledReason = writeDisabledReason(currentPanePath),
                onClick = { zipFocusedSelection(isPrimary = isPrimary) }
            )
            actions += SelectionActionItem(
                label = "单独压缩每个",
                enabled = currentPaneWritable,
                disabledReason = writeDisabledReason(currentPanePath),
                onClick = { zipEachFocusedSelectionSeparately(isPrimary = isPrimary) }
            )
            val archiveEntries = selectedEntries.filter { ArchiveFormatResolver.resolve(it.name)?.supportsExtraction == true }
            if (archiveEntries.isNotEmpty()) {
                actions += SelectionActionItem(
                    label = "解压到当前窗格",
                    enabled = currentPaneWritable,
                    disabledReason = writeDisabledReason(currentPanePath),
                    onClick = {
                        unzipEntriesToPane(
                            entries = archiveEntries,
                            targetPath = currentPanePath,
                            targetPaneLabel = if (isPrimary) "左窗格" else "右窗格"
                        )
                    }
                )
                actions += SelectionActionItem(
                    label = if (isPrimary) "解压到右窗格" else "解压到左窗格",
                    enabled = oppositePaneWritable,
                    disabledReason = writeDisabledReason(oppositePanePath),
                    onClick = {
                        unzipEntriesToPane(
                            entries = archiveEntries,
                            targetPath = oppositePanePath,
                            targetPaneLabel = if (isPrimary) "右窗格" else "左窗格"
                        )
                    }
                )
            }
            val canMoveSelection = selectedEntries.all { it.capabilities.canDelete }
            actions += SelectionActionItem(
                label = if (isPrimary) "复制到右窗格" else "复制到左窗格",
                enabled = oppositePaneWritable,
                disabledReason = writeDisabledReason(oppositePanePath),
                onClick = {
                    if (isPrimary) requestPrimaryTransferToSecondary(move = false) else requestSecondaryTransferToPrimary(move = false)
                }
            )
            actions += SelectionActionItem(
                label = if (isPrimary) "移动到右窗格" else "移动到左窗格",
                enabled = oppositePaneWritable && canMoveSelection,
                disabledReason = if (!canMoveSelection) "所选项中包含不可删除来源" else writeDisabledReason(oppositePanePath),
                onClick = {
                    if (isPrimary) requestPrimaryTransferToSecondary(move = true) else requestSecondaryTransferToPrimary(move = true)
                }
            )
            actions += SelectionActionItem("删除") {
                if (isPrimary) confirmDeleteDialog = true else confirmSecondaryDeleteDialog = true
            }
            if (viewModel.isRecycleBinPath(currentPanePath)) {
                actions += SelectionActionItem("恢复到原位置") {
                    restoreRecycleBinSelection(isPrimary)
                }
            }
        }
        if (viewModel.isRecycleBinPath(currentPanePath)) {
            actions += SelectionActionItem("清空回收站") {
                confirmClearRecycleBinDialog = true
            }
        }
        SelectionActionSheetDialog.show(
            context = requireContext(),
            title = if (isPrimary) "左窗格操作" else "右窗格操作",
            actions = actions
        )
    }

    private fun addAuthorizedDirectory(uri: Uri) {
        runCatching {
            requireContext().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        val label = uri.lastPathSegment?.substringAfterLast(':')?.ifBlank { null } ?: "授权目录"
        searchQuery = ""
        viewModel.addAuthorizedDirectory(uri, label)
        clearSelection()
        Toast.makeText(requireContext(), "已授权并打开目录", Toast.LENGTH_SHORT).show()
    }

    private fun openAuthorizedDirectoryRecord(record: SafDirectoryRecord) {
        searchQuery = ""
        viewModel.openSafDirectory(record.uri, record.label)
        clearSelection()
    }

    private fun toggleCurrentFavorite() {
        val added = viewModel.toggleCurrentFavorite()
        clearSelection()
        Toast.makeText(requireContext(), if (added) "已收藏当前目录" else "已取消收藏", Toast.LENGTH_SHORT).show()
    }

    private fun toggleEntrySelected(entry: FileManagerEntry) {
        focusedPane = FileManagerPaneTarget.Primary
        if (entry.type == FileEntryType.Parent) return
        selectedEntryIds = if (entry.id in selectedEntryIds) selectedEntryIds - entry.id else selectedEntryIds + entry.id
    }

    private fun toggleSecondaryEntrySelected(entry: FileManagerEntry) {
        focusedPane = FileManagerPaneTarget.Secondary
        if (entry.type == FileEntryType.Parent) return
        secondarySelectedEntryIds = if (entry.id in secondarySelectedEntryIds) {
            secondarySelectedEntryIds - entry.id
        } else {
            secondarySelectedEntryIds + entry.id
        }
    }

    private fun selectAllEntries() {
        selectedEntryIds = currentState.entries
            .filterNot { it.type == FileEntryType.Parent }
            .map { it.id }
            .toSet()
    }

    private fun selectAllSecondaryEntries() {
        secondarySelectedEntryIds = secondaryState?.entries.orEmpty()
            .filterNot { it.type == FileEntryType.Parent }
            .map { it.id }
            .toSet()
    }

    private fun invertSelection(isPrimary: Boolean) {
        if (isPrimary) {
            val allIds = currentState.entries
                .filterNot { it.type == FileEntryType.Parent }
                .map { it.id }
                .toSet()
            selectedEntryIds = allIds - selectedEntryIds
        } else {
            val allIds = secondaryState?.entries.orEmpty()
                .filterNot { it.type == FileEntryType.Parent }
                .map { it.id }
                .toSet()
            secondarySelectedEntryIds = allIds - secondarySelectedEntryIds
        }
    }

    private fun clearSelection() {
        selectedEntryIds = emptySet()
    }

    private fun clearSecondarySelection() {
        secondarySelectedEntryIds = emptySet()
    }

    private fun selectedEntries(): List<FileManagerEntry> {
        return currentState.entries.filter { it.id in selectedEntryIds && it.type != FileEntryType.Parent }
    }

    private fun secondarySelectedEntries(): List<FileManagerEntry> {
        return secondaryState?.entries.orEmpty().filter { it.id in secondarySelectedEntryIds && it.type != FileEntryType.Parent }
    }

    private fun deleteSelectedEntries() {
        val entries = selectedEntries()
        if (entries.isEmpty()) return
        var successCount = 0
        val failures = mutableListOf<String>()
        entries.forEach { entry ->
            viewModel.deleteEntry(entry)
                .onSuccess { successCount++ }
                .onFailure { failures += "${entry.name}: ${it.message.orEmpty()}" }
        }
        clearSelection()
        val message = if (failures.isEmpty()) {
            "已删除 $successCount 项"
        } else {
            "删除完成 $successCount/${entries.size} 项，失败 ${failures.size} 项"
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun deleteSecondarySelectedEntries() {
        val entries = secondarySelectedEntries()
        if (entries.isEmpty()) return
        var successCount = 0
        val failures = mutableListOf<String>()
        entries.forEach { entry ->
            viewModel.deleteEntryWithoutRefresh(entry)
                .onSuccess { successCount++ }
                .onFailure { failures += "${entry.name}: ${it.message.orEmpty()}" }
        }
        clearSecondarySelection()
        refreshSecondaryDirectory()
        val message = if (failures.isEmpty()) {
            "右窗格已删除 $successCount 项"
        } else {
            "右窗格删除完成 $successCount/${entries.size} 项，失败 ${failures.size} 项"
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun performRecursiveSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty() || isSearching) return
        searchJob?.cancel()
        isSearching = true
        searchBasePath = currentState.currentPath
        searchJob = lifecycleScope.launch {
            try {
                viewModel.searchCurrentDirectoryRecursively(FileManagerSearchOptions(query = trimmed))
                    .onSuccess { results ->
                        searchResultEntries = results
                        clearSelection()
                    }
                    .onFailure { error ->
                        Toast.makeText(requireContext(), "搜索失败：${error.message.orEmpty()}", Toast.LENGTH_SHORT).show()
                    }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } finally {
                isSearching = false
            }
        }
    }

    private fun exitSearchResults() {
        searchJob?.cancel()
        searchJob = null
        searchResultEntries = null
        isSearching = false
        searchQuery = ""
        clearSelection()
    }

    private fun restoreRecycleBinSelection(isPrimary: Boolean) {
        val entries = if (isPrimary) selectedEntries() else secondarySelectedEntries()
        if (entries.isEmpty()) return
        val result = viewModel.restoreRecycleBinEntries(entries)
        if (isPrimary) clearSelection() else clearSecondarySelection()
        if (!isPrimary) refreshSecondaryDirectory()
        val message = if (result.failures.isEmpty()) {
            "已恢复 ${result.successCount} 项到原位置"
        } else {
            "恢复完成 ${result.successCount}/${result.requestedCount} 项，失败 ${result.failures.size} 项"
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun clearRecycleBin() {
        viewModel.clearRecycleBin()
            .onSuccess {
                clearSelection()
                refreshSecondaryDirectory()
                Toast.makeText(requireContext(), "回收站已清空", Toast.LENGTH_SHORT).show()
            }
            .onFailure {
                Toast.makeText(requireContext(), "清空失败：${it.message.orEmpty()}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun requestPrimaryTransferToSecondary(move: Boolean) {
        val targetPath = secondaryCurrentPath.takeIf { it.isNotBlank() } ?: return
        val entries = selectedEntries()
        if (entries.isEmpty()) return
        pendingTransferDialog = DualPaneTransferRequest(
            entries = entries,
            sourcePath = currentState.currentPath,
            targetPath = targetPath,
            sourcePane = PaneTarget.Primary,
            targetPaneLabel = "右窗格",
            move = move
        )
    }

    private fun requestSecondaryTransferToPrimary(move: Boolean) {
        val targetPath = currentState.currentPath.takeIf { it.isNotBlank() } ?: return
        val entries = secondarySelectedEntries()
        if (entries.isEmpty()) return
        pendingTransferDialog = DualPaneTransferRequest(
            entries = entries,
            sourcePath = secondaryCurrentPath,
            targetPath = targetPath,
            sourcePane = PaneTarget.Secondary,
            targetPaneLabel = "左窗格",
            move = move
        )
    }

    private fun startDualPaneTransfer(request: DualPaneTransferRequest, conflictPolicy: TransferConflictPolicy) {
        activeTransferJob?.cancel()
        activeTransferJob = viewLifecycleOwner.lifecycleScope.launch {
            val title = request.title
            taskProgressState = FileManagerTaskProgressState(
                title = title,
                message = "准备传输到${request.targetPaneLabel}",
                completedCount = 0,
                totalCount = request.entries.size
            )
            try {
                val successCount = if (request.move) {
                    moveEntriesToDirectoryPath(request.entries, request.targetPath, conflictPolicy, title).successCount
                } else {
                    copyEntriesToDirectoryPath(request.entries, request.targetPath, conflictPolicy, title).successCount
                }
                if (request.sourcePane == PaneTarget.Primary) clearSelection() else clearSecondarySelection()
                refreshPanesForChangedPaths(sourcePath = request.sourcePath, targetPath = request.targetPath)
                taskProgressState = FileManagerTaskProgressState(
                    title = title,
                    message = "传输完成：$successCount/${request.entries.size} 项",
                    completedCount = successCount,
                    totalCount = request.entries.size,
                    isRunning = false
                )
                showTransferToast(successCount, request.entries.size, request.move, targetPane = request.targetPaneLabel)
            } catch (cancellation: CancellationException) {
                taskProgressState = null
                throw cancellation
            } catch (throwable: Throwable) {
                taskProgressState = FileManagerTaskProgressState(
                    title = title,
                    message = throwable.message?.takeIf { it.isNotBlank() } ?: "传输失败，请稍后重试。",
                    completedCount = 0,
                    totalCount = request.entries.size,
                    isRunning = false,
                    isFailed = true
                )
                refreshPanesForChangedPaths(sourcePath = request.sourcePath, targetPath = request.targetPath)
            } finally {
                activeTransferJob = null
            }
        }
    }

    private suspend fun copyEntriesToDirectoryPath(
        entries: List<FileManagerEntry>,
        targetPath: String,
        conflictPolicy: TransferConflictPolicy,
        title: String
    ): BatchCopyResult {
        return when (conflictPolicy) {
            TransferConflictPolicy.Fail -> viewModel.copyEntriesToDirectoryPath(entries, targetPath) { completedCount, totalCount ->
                updateTransferProgress(title, completedCount, totalCount)
            }
            TransferConflictPolicy.KeepBoth -> viewModel.copyEntriesToDirectoryPathKeepingBoth(entries, targetPath) { completedCount, totalCount ->
                updateTransferProgress(title, completedCount, totalCount)
            }
            TransferConflictPolicy.Replace -> viewModel.copyEntriesToDirectoryPathReplacingExisting(entries, targetPath) { completedCount, totalCount ->
                updateTransferProgress(title, completedCount, totalCount)
            }
        }
    }

    private suspend fun moveEntriesToDirectoryPath(
        entries: List<FileManagerEntry>,
        targetPath: String,
        conflictPolicy: TransferConflictPolicy,
        title: String
    ): BatchMoveResult {
        return when (conflictPolicy) {
            TransferConflictPolicy.Fail -> viewModel.moveEntriesToDirectoryPath(entries, targetPath) { completedCount, totalCount ->
                updateTransferProgress(title, completedCount, totalCount)
            }
            TransferConflictPolicy.KeepBoth -> viewModel.moveEntriesToDirectoryPathKeepingBoth(entries, targetPath) { completedCount, totalCount ->
                updateTransferProgress(title, completedCount, totalCount)
            }
            TransferConflictPolicy.Replace -> viewModel.moveEntriesToDirectoryPathReplacingExisting(entries, targetPath) { completedCount, totalCount ->
                updateTransferProgress(title, completedCount, totalCount)
            }
        }
    }

    private fun updateTransferProgress(title: String, completedCount: Int, totalCount: Int) {
        taskProgressState = FileManagerTaskProgressState(
            title = title,
            message = "正在处理文件传输",
            completedCount = completedCount,
            totalCount = totalCount
        )
    }

    private fun zipFocusedSelection(isPrimary: Boolean) {
        val entries = if (isPrimary) selectedEntries() else secondarySelectedEntries()
        val targetPath = if (isPrimary) currentState.currentPath else secondaryCurrentPath
        val targetPaneLabel = if (isPrimary) "左窗格" else "右窗格"
        if (entries.isEmpty() || targetPath.isBlank()) return
        activeTransferJob?.cancel()
        activeTransferJob = viewLifecycleOwner.lifecycleScope.launch {
            val title = "压缩为 ZIP"
            taskProgressState = FileManagerTaskProgressState(
                title = title,
                message = "准备在$targetPaneLabel 创建压缩包",
                completedCount = 0,
                totalCount = entries.size
            )
            try {
                val result = viewModel.zipEntriesInDirectoryPath(targetPath, entries) { completedCount, totalCount ->
                    taskProgressState = FileManagerTaskProgressState(
                        title = title,
                        message = "正在写入 ZIP 压缩包",
                        completedCount = completedCount,
                        totalCount = totalCount
                    )
                }
                if (isPrimary) clearSelection() else clearSecondarySelection()
                refreshPanesForChangedPaths(targetPath = targetPath)
                taskProgressState = FileManagerTaskProgressState(
                    title = title,
                    message = if (result.failures.isEmpty()) {
                        "压缩完成：${result.archiveName}"
                    } else {
                        "压缩完成 ${result.successCount}/${result.requestedCount} 项，失败 ${result.failures.size} 项"
                    },
                    completedCount = result.successCount,
                    totalCount = result.requestedCount,
                    isRunning = false,
                    isFailed = result.failures.isNotEmpty()
                )
                BatchOperationResultPresenter.showZip(requireContext(), result, ::copyFailureReport)
            } catch (cancellation: CancellationException) {
                taskProgressState = null
                throw cancellation
            } catch (throwable: Throwable) {
                taskProgressState = FileManagerTaskProgressState(
                    title = title,
                    message = throwable.message ?: "压缩失败",
                    completedCount = 0,
                    totalCount = entries.size,
                    isRunning = false,
                    isFailed = true
                )
            } finally {
                activeTransferJob = null
            }
        }
    }

    private fun zipEachFocusedSelectionSeparately(isPrimary: Boolean) {
        val entries = if (isPrimary) selectedEntries() else secondarySelectedEntries()
        val targetPath = if (isPrimary) currentState.currentPath else secondaryCurrentPath
        val targetPaneLabel = if (isPrimary) "左窗格" else "右窗格"
        if (entries.isEmpty() || targetPath.isBlank()) return
        activeTransferJob?.cancel()
        activeTransferJob = viewLifecycleOwner.lifecycleScope.launch {
            val title = "单独压缩每个"
            taskProgressState = FileManagerTaskProgressState(
                title = title,
                message = "准备在$targetPaneLabel 创建压缩包",
                completedCount = 0,
                totalCount = entries.size
            )
            try {
                val result = viewModel.zipEachEntryInDirectoryPath(targetPath, entries) { completedCount, totalCount ->
                    taskProgressState = FileManagerTaskProgressState(
                        title = title,
                        message = "正在写入 ZIP 压缩包",
                        completedCount = completedCount,
                        totalCount = totalCount
                    )
                }
                if (isPrimary) clearSelection() else clearSecondarySelection()
                refreshPanesForChangedPaths(targetPath = targetPath)
                taskProgressState = FileManagerTaskProgressState(
                    title = title,
                    message = if (result.failures.isEmpty()) {
                        "压缩完成：已生成 ${result.archiveNames.size} 个压缩包"
                    } else {
                        "压缩完成 ${result.successCount}/${result.requestedCount} 项，失败 ${result.failures.size} 项"
                    },
                    completedCount = result.successCount,
                    totalCount = result.requestedCount,
                    isRunning = false,
                    isFailed = result.failures.isNotEmpty()
                )
                BatchOperationResultPresenter.showSeparateZip(requireContext(), result, ::copyFailureReport)
            } catch (cancellation: CancellationException) {
                taskProgressState = null
                throw cancellation
            } catch (throwable: Throwable) {
                taskProgressState = FileManagerTaskProgressState(
                    title = title,
                    message = throwable.message ?: "压缩失败",
                    completedCount = 0,
                    totalCount = entries.size,
                    isRunning = false,
                    isFailed = true
                )
            } finally {
                activeTransferJob = null
            }
        }
    }

    private fun unzipEntriesToPane(entries: List<FileManagerEntry>, targetPath: String, targetPaneLabel: String) {
        val archives = entries.filter { ArchiveFormatResolver.resolve(it.name)?.supportsExtraction == true }
        if (archives.isEmpty() || targetPath.isBlank()) return
        activeTransferJob?.cancel()
        activeTransferJob = viewLifecycleOwner.lifecycleScope.launch {
            val title = "解压到$targetPaneLabel"
            taskProgressState = FileManagerTaskProgressState(
                title = title,
                message = "准备解压到$targetPaneLabel",
                completedCount = 0,
                totalCount = archives.size
            )
            try {
                val result = viewModel.unzipEntriesToDirectoryPath(archives, targetPath) { completedCount, totalCount ->
                    taskProgressState = FileManagerTaskProgressState(
                        title = title,
                        message = "正在解压压缩包",
                        completedCount = completedCount,
                        totalCount = totalCount
                    )
                }
                refreshPanesForChangedPaths(targetPath = targetPath)
                taskProgressState = FileManagerTaskProgressState(
                    title = title,
                    message = "解压完成：${result.successCount}/${result.requestedCount} 项",
                    completedCount = result.successCount,
                    totalCount = result.requestedCount,
                    isRunning = false,
                    isFailed = result.failures.isNotEmpty()
                )
                BatchOperationResultPresenter.showUnzip(requireContext(), result, ::copyFailureReport)
            } catch (cancellation: CancellationException) {
                taskProgressState = null
                throw cancellation
            } catch (throwable: Throwable) {
                taskProgressState = FileManagerTaskProgressState(
                    title = title,
                    message = throwable.message ?: "解压失败",
                    completedCount = 0,
                    totalCount = archives.size,
                    isRunning = false,
                    isFailed = true
                )
            } finally {
                activeTransferJob = null
            }
        }
    }

    private fun copyFailureReport(title: String, body: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(title, body))
        Toast.makeText(requireContext(), "已复制报告", Toast.LENGTH_SHORT).show()
    }

    private fun cancelActiveTransfer() {
        activeTransferJob?.cancel()
        activeTransferJob = null
        val state = taskProgressState ?: return
        taskProgressState = state.copy(
            message = "任务已取消：${state.completedCount}/${state.totalCount} 项",
            isRunning = false,
            isFailed = true
        )
        Toast.makeText(requireContext(), "已取消文件传输任务", Toast.LENGTH_SHORT).show()
    }

    private fun refreshPanesForChangedPaths(sourcePath: String? = null, targetPath: String? = null) {
        val changedPaths = listOfNotNull(sourcePath, targetPath)
            .filter { it.isNotBlank() }
            .map(::normalizeComparablePath)
            .toSet()
        if (changedPaths.isEmpty()) return
        val primaryPath = normalizeComparablePath(currentState.currentPath)
        val secondaryPath = normalizeComparablePath(secondaryCurrentPath)
        if (primaryPath in changedPaths) viewModel.refresh()
        if (secondaryPath in changedPaths) refreshSecondaryDirectory()
    }

    private fun normalizeComparablePath(path: String): String {
        return path.trim().trimEnd('/').ifBlank { path.trim() }
    }

    private fun isWritableFileManagerTarget(path: String): Boolean {
        if (path.isBlank()) return false
        if (path.contains("!/")) return false
        if (path.startsWith("root://", ignoreCase = true)) return false
        if (path.startsWith("content://", ignoreCase = true)) return true
        return runCatching {
            val file = File(path)
            file.exists() && file.isDirectory && file.canWrite()
        }.getOrDefault(false)
    }

    private fun writeDisabledReason(path: String): String? {
        if (isWritableFileManagerTarget(path)) return null
        return when {
            path.isBlank() -> "目标窗格尚未打开目录"
            path.contains("!/") -> "压缩包内目录不能直接写入"
            path.startsWith("root://", ignoreCase = true) -> "Root 路径写入暂未开放"
            path.startsWith("content://", ignoreCase = true) -> null
            else -> "当前目录不可写或不存在"
        }
    }

    private fun showTransferToast(successCount: Int, totalCount: Int, move: Boolean, targetPane: String) {
        val action = if (move) "移动" else "复制"
        Toast.makeText(requireContext(), "已${action}到$targetPane：$successCount/$totalCount 项", Toast.LENGTH_SHORT).show()
    }

    private fun showInputDialog(dialog: FileManagerInputDialog) {
        inputDialog = dialog
    }

    private fun showRenameSelectedDialog() {
        val entry = selectedEntries().singleOrNull() ?: return
        inputDialog = FileManagerInputDialog.Rename(entry, entry.name)
    }

    private fun showRenameSecondarySelectedDialog() {
        val entry = secondarySelectedEntries().singleOrNull() ?: return
        inputDialog = FileManagerInputDialog.Rename(entry, entry.name, PaneTarget.Secondary)
    }

    private fun submitInputDialog(value: String) {
        val dialog = inputDialog ?: return
        val name = value.trim()
        val result = when (dialog) {
            is FileManagerInputDialog.CreateDirectory -> if (dialog.target == PaneTarget.Secondary) {
                viewModel.createDirectoryInDirectoryPath(secondaryCurrentPath, name).onSuccess { refreshSecondaryDirectory() }
            } else {
                viewModel.createDirectory(name)
            }
            is FileManagerInputDialog.CreateFile -> if (dialog.target == PaneTarget.Secondary) {
                viewModel.createFileInDirectoryPath(secondaryCurrentPath, name).onSuccess { refreshSecondaryDirectory() }
            } else {
                viewModel.createFile(name)
            }
            is FileManagerInputDialog.Rename -> if (dialog.target == PaneTarget.Secondary) {
                viewModel.renameEntryWithoutRefresh(dialog.entry, name).onSuccess { refreshSecondaryDirectory() }
            } else {
                viewModel.renameEntry(dialog.entry, name)
            }
        }
        result
            .onSuccess {
                inputDialog = null
                if (dialog.target == PaneTarget.Secondary) clearSecondarySelection() else clearSelection()
                Toast.makeText(requireContext(), "操作完成", Toast.LENGTH_SHORT).show()
            }
            .onFailure { Toast.makeText(requireContext(), it.message ?: "操作失败", Toast.LENGTH_SHORT).show() }
    }

    @Composable
    private fun InputDialogHost() {
        val dialog = inputDialog ?: return
        var value by androidx.compose.runtime.remember(dialog) { mutableStateOf(dialog.initialValue) }
        AlertDialog(
            onDismissRequest = { inputDialog = null },
            title = { Text(dialog.title) },
            text = {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(dialog.label) },
                    singleLine = true
                )
            },
            confirmButton = { TextButton(onClick = { submitInputDialog(value) }) { Text("确定") } },
            dismissButton = { TextButton(onClick = { inputDialog = null }) { Text("取消") } }
        )
    }

    @Composable
    private fun BatchRenameDialogHost() {
        val request = batchRenameDialog ?: return
        var mode by androidx.compose.runtime.remember(request) { mutableStateOf(BatchRenameMode.FindReplace) }
        var findText by androidx.compose.runtime.remember(request) { mutableStateOf("") }
        var replaceText by androidx.compose.runtime.remember(request) { mutableStateOf("") }
        var useRegex by androidx.compose.runtime.remember(request) { mutableStateOf(false) }
        var prefix by androidx.compose.runtime.remember(request) { mutableStateOf("") }
        var startNumberText by androidx.compose.runtime.remember(request) { mutableStateOf("1") }
        var suffix by androidx.compose.runtime.remember(request) { mutableStateOf("") }

        val startNumber = startNumberText.toIntOrNull()
        val regexInvalid = mode == BatchRenameMode.FindReplace && useRegex && findText.isNotEmpty() &&
            runCatching { Regex(findText) }.isFailure
        val inputsValid = when (mode) {
            BatchRenameMode.FindReplace -> findText.isNotEmpty() && !regexInvalid
            BatchRenameMode.Sequence -> startNumber != null
        }
        val computedRenames = if (!inputsValid) {
            emptyList()
        } else {
            request.entries.mapIndexedNotNull { index, entry ->
                val newName = runCatching {
                    batchRenameTargetName(entry, index, mode, findText, replaceText, useRegex, prefix, startNumber ?: 1, suffix)
                }.getOrNull()?.trim().orEmpty()
                if (newName.isBlank() || newName == entry.name) null else entry to newName
            }
        }
        val duplicateNames = computedRenames.groupBy { it.second }.filterValues { it.size > 1 }.keys
        val confirmEnabled = inputsValid && computedRenames.isNotEmpty() && duplicateNames.isEmpty()

        AlertDialog(
            onDismissRequest = { batchRenameDialog = null },
            title = { Text("批量重命名（${request.entries.size} 项）") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = mode == BatchRenameMode.FindReplace,
                            onClick = { mode = BatchRenameMode.FindReplace }
                        )
                        Text("查找替换")
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(
                            selected = mode == BatchRenameMode.Sequence,
                            onClick = { mode = BatchRenameMode.Sequence }
                        )
                        Text("序列编号")
                    }
                    when (mode) {
                        BatchRenameMode.FindReplace -> {
                            OutlinedTextField(
                                value = findText,
                                onValueChange = { findText = it },
                                label = { Text("查找内容") },
                                singleLine = true,
                                isError = regexInvalid
                            )
                            OutlinedTextField(
                                value = replaceText,
                                onValueChange = { replaceText = it },
                                label = { Text("替换为") },
                                singleLine = true
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = useRegex, onCheckedChange = { useRegex = it })
                                Text("使用正则表达式")
                            }
                            if (regexInvalid) {
                                Text("正则表达式无效", color = MaterialTheme.colorScheme.error)
                            }
                        }
                        BatchRenameMode.Sequence -> {
                            OutlinedTextField(
                                value = prefix,
                                onValueChange = { prefix = it },
                                label = { Text("前缀") },
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = startNumberText,
                                onValueChange = { startNumberText = it.filter(Char::isDigit).take(9) },
                                label = { Text("起始编号") },
                                singleLine = true,
                                isError = startNumber == null
                            )
                            OutlinedTextField(
                                value = suffix,
                                onValueChange = { suffix = it },
                                label = { Text("后缀") },
                                singleLine = true
                            )
                            Text("文件扩展名保持不变", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (inputsValid) {
                        when {
                            duplicateNames.isNotEmpty() -> Text(
                                "存在重复的新名称：${duplicateNames.take(3).joinToString("、")}",
                                color = MaterialTheme.colorScheme.error
                            )
                            computedRenames.isEmpty() -> Text("所有名称保持不变")
                            else -> {
                                Text("预览：", style = MaterialTheme.typography.labelLarge)
                                computedRenames.take(5).forEach { (entry, newName) ->
                                    Text("${entry.name} → $newName", style = MaterialTheme.typography.bodySmall)
                                }
                                if (computedRenames.size > 5) {
                                    Text("……共 ${computedRenames.size} 项", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { submitBatchRename(request.target, computedRenames) },
                    enabled = confirmEnabled
                ) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { batchRenameDialog = null }) { Text("取消") } }
        )
    }

    private fun batchRenameTargetName(
        entry: FileManagerEntry,
        index: Int,
        mode: BatchRenameMode,
        findText: String,
        replaceText: String,
        useRegex: Boolean,
        prefix: String,
        startNumber: Int,
        suffix: String
    ): String {
        return when (mode) {
            BatchRenameMode.FindReplace -> if (useRegex) {
                Regex(findText).replace(entry.name, replaceText)
            } else {
                entry.name.replace(findText, replaceText)
            }
            BatchRenameMode.Sequence -> {
                val extension = if (entry.type == FileEntryType.Directory) {
                    ""
                } else {
                    val dotIndex = entry.name.lastIndexOf('.')
                    if (dotIndex > 0) entry.name.substring(dotIndex) else ""
                }
                "$prefix${startNumber + index}$suffix$extension"
            }
        }
    }

    private fun submitBatchRename(target: PaneTarget, renames: List<Pair<FileManagerEntry, String>>) {
        if (renames.isEmpty()) return
        val result = if (target == PaneTarget.Secondary) {
            viewModel.renameEntriesWithoutRefresh(renames).also { refreshSecondaryDirectory() }
        } else {
            viewModel.renameEntries(renames)
        }
        batchRenameDialog = null
        if (target == PaneTarget.Secondary) clearSecondarySelection() else clearSelection()
        BatchOperationResultPresenter.showRename(requireContext(), result, ::copyFailureReport)
    }

    @Composable
    private fun DeleteConfirmDialogHost() {
        if (!confirmDeleteDialog) return
        val count = selectedEntryIds.size
        val inRecycleBin = viewModel.isRecycleBinPath(currentState.currentPath)
        AlertDialog(
            onDismissRequest = { confirmDeleteDialog = false },
            title = { Text(if (inRecycleBin) "彻底删除文件" else "删除文件") },
            text = {
                Text(
                    if (inRecycleBin) "选中的 $count 项位于回收站，删除后将无法恢复。确定彻底删除吗？"
                    else "确定删除选中的 $count 项吗？"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteDialog = false
                    deleteSelectedEntries()
                }) { Text(if (inRecycleBin) "彻底删除" else "删除") }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteDialog = false }) { Text("取消") } }
        )
    }

    @Composable
    private fun ClearRecycleBinDialogHost() {
        if (!confirmClearRecycleBinDialog) return
        AlertDialog(
            onDismissRequest = { confirmClearRecycleBinDialog = false },
            title = { Text("清空回收站") },
            text = { Text("回收站内的所有文件将被彻底删除且无法恢复。确定清空吗？") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClearRecycleBinDialog = false
                    clearRecycleBin()
                }) { Text("清空") }
            },
            dismissButton = { TextButton(onClick = { confirmClearRecycleBinDialog = false }) { Text("取消") } }
        )
    }

    @Composable
    private fun SecondaryDeleteConfirmDialogHost() {
        if (!confirmSecondaryDeleteDialog) return
        val count = secondarySelectedEntryIds.size
        AlertDialog(
            onDismissRequest = { confirmSecondaryDeleteDialog = false },
            title = { Text("删除右窗格文件") },
            text = { Text("确定删除右窗格选中的 $count 项吗？") },
            confirmButton = {
                TextButton(onClick = {
                    confirmSecondaryDeleteDialog = false
                    deleteSecondarySelectedEntries()
                }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { confirmSecondaryDeleteDialog = false }) { Text("取消") } }
        )
    }

    @Composable
    private fun TransferConfirmDialogHost() {
        val request = pendingTransferDialog ?: return
        AlertDialog(
            onDismissRequest = { pendingTransferDialog = null },
            title = { Text(request.title) },
            text = {
                Text(
                    "目标：${request.targetPaneLabel}\n" +
                        "路径：${request.targetPath}\n" +
                        "数量：${request.entries.size} 项\n\n" +
                        "选择遇到同名文件时的处理方式。"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingTransferDialog = null
                    startDualPaneTransfer(request, TransferConflictPolicy.KeepBoth)
                }) { Text("保留两者") }
            },
            dismissButton = {
                TextButton(onClick = { pendingTransferDialog = null }) { Text("取消") }
                TextButton(onClick = {
                    pendingTransferDialog = null
                    startDualPaneTransfer(request, TransferConflictPolicy.Fail)
                }) { Text("冲突停止") }
                TextButton(onClick = {
                    pendingTransferDialog = null
                    startDualPaneTransfer(request, TransferConflictPolicy.Replace)
                }) { Text("覆盖") }
            }
        )
    }

    private fun ensureSecondaryDirectoryLoaded(primaryPath: String) {
        if (mode == MODE_PICK_FILE || primaryPath.isBlank() || secondaryCurrentPath.isNotBlank()) return
        secondaryCurrentPath = primaryPath
        loadSecondaryDirectory(primaryPath, replaceHistory = true)
    }

    private fun refreshSecondaryDirectory() {
        secondaryCurrentPath.takeIf { it.isNotBlank() }?.let { loadSecondaryDirectory(it, replaceHistory = true) }
    }

    private fun loadSecondaryDirectory(path: String, replaceHistory: Boolean = false) {
        if (path.isBlank()) return
        secondaryDirectoryJob?.cancel()
        secondaryDirectoryJob = viewLifecycleOwner.lifecycleScope.launch {
            val previous = secondaryState
            secondaryState = (previous ?: currentState).copy(
                isLoading = true,
                currentPath = path,
                locationHint = "目标窗格",
                entries = if (previous?.currentPath == path) previous.entries else emptyList(),
                errorMessage = null,
                canGoUp = viewModel.parentPathForDirectoryPath(path) != null,
                canGoBack = secondaryBackStack.isNotEmpty(),
                canGoForward = secondaryForwardStack.isNotEmpty()
            )
            val result = viewModel.listDirectoryPath(path)
            secondaryState = result.fold(
                onSuccess = { entries ->
                    (secondaryState ?: previous ?: currentState).copy(
                        isLoading = false,
                        currentPath = path,
                        locationHint = "目标窗格",
                        entries = entries,
                        errorMessage = null,
                        canGoUp = viewModel.parentPathForDirectoryPath(path) != null,
                        canGoBack = secondaryBackStack.isNotEmpty(),
                        canGoForward = secondaryForwardStack.isNotEmpty()
                    )
                },
                onFailure = { throwable ->
                    (secondaryState ?: currentState).copy(
                        isLoading = false,
                        currentPath = path,
                        locationHint = "目标窗格",
                        entries = emptyList(),
                        errorMessage = throwable.message ?: "读取目录失败",
                        canGoUp = viewModel.parentPathForDirectoryPath(path) != null,
                        canGoBack = secondaryBackStack.isNotEmpty(),
                        canGoForward = secondaryForwardStack.isNotEmpty()
                    )
                }
            )
            secondarySelectedEntryIds = secondarySelectedEntryIds.intersect(secondaryState?.entries.orEmpty().map { it.id }.toSet())
        }
    }

    private fun openSecondaryDirectory(path: String, addToBackStack: Boolean = true) {
        if (path.isBlank() || path == secondaryCurrentPath) return
        if (addToBackStack && secondaryCurrentPath.isNotBlank()) secondaryBackStack.addLast(secondaryCurrentPath)
        if (addToBackStack) secondaryForwardStack.clear()
        secondaryCurrentPath = path
        secondarySearchQuery = ""
        clearSecondarySelection()
        loadSecondaryDirectory(path)
    }

    private fun navigateSecondaryBack(): Boolean {
        if (secondaryBackStack.isEmpty()) return false
        secondaryCurrentPath.takeIf { it.isNotBlank() }?.let { secondaryForwardStack.addLast(it) }
        secondaryCurrentPath = secondaryBackStack.removeLast()
        clearSecondarySelection()
        loadSecondaryDirectory(secondaryCurrentPath)
        return true
    }

    private fun navigateSecondaryForward() {
        if (secondaryForwardStack.isEmpty()) return
        secondaryCurrentPath.takeIf { it.isNotBlank() }?.let { secondaryBackStack.addLast(it) }
        secondaryCurrentPath = secondaryForwardStack.removeLast()
        clearSecondarySelection()
        loadSecondaryDirectory(secondaryCurrentPath)
    }

    private fun navigateSecondaryUp() {
        val parent = viewModel.parentPathForDirectoryPath(secondaryCurrentPath) ?: return
        openSecondaryDirectory(parent)
    }

    private fun openSecondaryDirectoryEntry(entry: FileManagerEntry) {
        val targetPath = when (val source = entry.source) {
            is FileEntrySource.ParentDirectory -> source.targetPath
            is FileEntrySource.LocalFile -> source.file.takeIf { it.isDirectory }?.absolutePath
            is FileEntrySource.DocumentUri -> source.uri.toString()
            is FileEntrySource.ArchiveEntry -> if (source.isDirectory) "${source.archiveFile.absolutePath}!/${source.innerPath.trim('/')}" else null
            is FileEntrySource.RootPath -> if (source.isDirectory) "root://${source.absolutePath}" else null
            is FileEntrySource.ContentUri -> null
        }
        if (targetPath == null) {
            Toast.makeText(requireContext(), "无法打开该目录", Toast.LENGTH_SHORT).show()
            return
        }
        openSecondaryDirectory(targetPath)
    }

    private fun showOpenWithTools(entry: FileManagerEntry, paneTarget: PaneTarget) {
        val tools = FileToolRegistry.toolsFor(entry)
        if (tools.isEmpty()) {
            openPreview(entry)
            return
        }
        OpenWithToolGridDialog.show(
            context = requireContext(),
            entry = entry,
            tools = tools,
            onToolSelected = { selectedEntry, tool -> handleFileTool(selectedEntry, tool, paneTarget) }
        )
    }

    private fun handleFileTool(entry: FileManagerEntry, tool: FileToolAction, paneTarget: PaneTarget) {
        if (!tool.implemented) {
            EngineeringToolPlaceholderPanel.show(requireContext(), entry, tool)
            return
        }
        when (tool.id) {
            FileToolId.ArchiveBrowse -> openArchiveEntryInPane(entry, paneTarget)
            FileToolId.ApkInfo -> ApkInstallAndSignaturePanel.show(
                context = requireContext(),
                entry = entry,
                typeName = FileEntryPropertiesFormatter.typeDisplayName(requireContext(), entry),
                onCopyPath = { copyEntryPath(entry) }
            )
            FileToolId.ApkSignature,
            FileToolId.DexTools,
            FileToolId.ArscTools,
            FileToolId.XmlTools,
            FileToolId.HexViewer -> EngineeringToolFactPanel.show(
                context = requireContext(),
                entry = entry,
                tool = tool,
                onCopyPath = { copyEntryPath(entry) }
            )
            FileToolId.TextEdit,
            FileToolId.ImagePreview -> openPreview(entry)
            FileToolId.CopyPath -> copyEntryPath(entry)
            FileToolId.Properties -> showEntryProperties(entry)
            FileToolId.ArchiveExtract -> unzipEntryToFocusedPane(entry, paneTarget)
            else -> EngineeringToolPlaceholderPanel.show(requireContext(), entry, tool)
        }
    }

    private fun openArchiveEntryInPane(entry: FileManagerEntry, paneTarget: PaneTarget) {
        val opened = when (paneTarget) {
            PaneTarget.Primary -> viewModel.openArchiveEntry(entry).also { if (it) clearSelection() }
            PaneTarget.Secondary -> archiveEntryPath(entry)?.let { path ->
                openSecondaryDirectory(path)
                true
            } ?: false
        }
        if (!opened) Toast.makeText(requireContext(), "无法浏览该压缩包", Toast.LENGTH_SHORT).show()
    }

    private fun archiveEntryPath(entry: FileManagerEntry): String? {
        return when (val source = entry.source) {
            is FileEntrySource.LocalFile -> source.file.takeIf { it.isFile }?.absolutePath?.let { "$it!/" }
            is FileEntrySource.ArchiveEntry -> if (source.isDirectory) {
                "${source.archiveFile.absolutePath}!/${source.innerPath.trim('/')}"
            } else {
                null
            }
            else -> null
        }
    }

    private fun unzipEntryToFocusedPane(entry: FileManagerEntry, paneTarget: PaneTarget) {
        val targetPath = when (paneTarget) {
            PaneTarget.Primary -> currentState.currentPath
            PaneTarget.Secondary -> secondaryCurrentPath
        }.takeIf { it.isNotBlank() } ?: return
        unzipEntriesToPane(
            entries = listOf(entry),
            targetPath = targetPath,
            targetPaneLabel = if (paneTarget == PaneTarget.Primary) "左窗格" else "右窗格"
        )
    }

    private fun showEntryProperties(entry: FileManagerEntry) {
        EntryPropertiesPanel.show(
            context = requireContext(),
            title = entry.name,
            message = FileEntryPropertiesFormatter.format(requireContext(), entry),
            onCopyPath = { copyEntryPath(entry) }
        )
    }

    private fun copyEntryPath(entry: FileManagerEntry) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(entry.name, entry.displayPath))
        Toast.makeText(requireContext(), "已复制路径", Toast.LENGTH_SHORT).show()
    }

    private fun openDirectoryEntry(entry: FileManagerEntry) {
        val opened = when (val source = entry.source) {
            is FileEntrySource.ParentDirectory -> viewModel.openDirectoryPath(source.targetPath)
            is FileEntrySource.LocalFile -> if (source.file.isDirectory) viewModel.openDirectoryPath(source.file.absolutePath) else false
            is FileEntrySource.DocumentUri -> {
                viewModel.openSafDirectory(source.uri, entry.displayPath.ifBlank { entry.name })
                true
            }
            is FileEntrySource.ArchiveEntry -> if (source.isDirectory) viewModel.openArchiveDirectory(source.archiveFile, source.innerPath) else false
            is FileEntrySource.RootPath -> if (source.isDirectory) {
                viewModel.openRootDirectory(source.absolutePath)
                true
            } else false
            is FileEntrySource.ContentUri -> false
        }
        if (!opened) Toast.makeText(requireContext(), "无法打开该目录", Toast.LENGTH_SHORT).show()
    }

    private fun finishPickingFile(entry: FileManagerEntry) {
        val uri = entryUri(entry)
        if (uri == null) {
            Toast.makeText(requireContext(), "该文件暂不支持直接选择", Toast.LENGTH_SHORT).show()
            return
        }
        findNavController().previousBackStackEntry?.savedStateHandle?.set(RESULT_FILE_SELECTED, true)
        findNavController().previousBackStackEntry?.savedStateHandle?.set(RESULT_FILE_SELECTED_URI, uri.toString())
        findNavController().previousBackStackEntry?.savedStateHandle?.set(RESULT_FILE_SELECTED_DISPLAY_NAME, entry.name)
        findNavController().navigateUp()
    }

    private fun openPreview(entry: FileManagerEntry) {
        val uri = entryUri(entry)
        if (uri == null) {
            Toast.makeText(requireContext(), "该文件暂不支持预览", Toast.LENGTH_SHORT).show()
            return
        }
        findNavController().navigate(
            R.id.local_file_preview_fragment,
            Bundle().apply {
                putString(LocalFilePreviewFragment.ARG_SOURCE_URI, uri.toString())
                putString(LocalFilePreviewFragment.ARG_DISPLAY_NAME, entry.name)
                putString(LocalFilePreviewFragment.ARG_DISPLAY_PATH, entry.displayPath)
                putLong(LocalFilePreviewFragment.ARG_SIZE_BYTES, entry.sizeBytes ?: -1L)
                putString(LocalFilePreviewFragment.ARG_ENTRY_TYPE, entry.type.name)
                putBoolean(LocalFilePreviewFragment.ARG_CAN_WRITE, entry.capabilities.canWrite)
                putString(LocalFilePreviewFragment.ARG_OPEN_MODE, LocalFilePreviewFragment.MODE_PREVIEW)
            }
        )
    }

    private fun entryUri(entry: FileManagerEntry): Uri? {
        return when (val source = entry.source) {
            is FileEntrySource.LocalFile -> Uri.fromFile(source.file)
            is FileEntrySource.DocumentUri -> source.uri
            is FileEntrySource.ContentUri -> source.uri
            is FileEntrySource.ParentDirectory,
            is FileEntrySource.ArchiveEntry,
            is FileEntrySource.RootPath -> null
        }
    }

    private enum class PaneTarget { Primary, Secondary }

    private enum class BatchRenameMode { FindReplace, Sequence }

    private data class BatchRenameRequest(
        val entries: List<FileManagerEntry>,
        val target: PaneTarget = PaneTarget.Primary
    )

    private data class DualPaneTransferRequest(
        val entries: List<FileManagerEntry>,
        val sourcePath: String,
        val targetPath: String,
        val sourcePane: PaneTarget,
        val targetPaneLabel: String,
        val move: Boolean
    ) {
        val title: String get() = if (move) "移动到$targetPaneLabel" else "复制到$targetPaneLabel"
    }

    private sealed class FileManagerInputDialog(
        val title: String,
        val label: String,
        val initialValue: String = "",
        val target: PaneTarget = PaneTarget.Primary
    ) {
        class CreateDirectory(target: PaneTarget = PaneTarget.Primary) : FileManagerInputDialog("新建文件夹", "文件夹名称", target = target)
        class CreateFile(target: PaneTarget = PaneTarget.Primary) : FileManagerInputDialog("新建文件", "文件名", target = target)
        class Rename(val entry: FileManagerEntry, initialName: String, target: PaneTarget = PaneTarget.Primary) : FileManagerInputDialog("重命名", "新名称", initialName, target)
    }

    companion object {
        const val ARG_MODE = "mode"
        const val MODE_PICK_FILE = "pick_file"
        const val MODE_MANAGE = "manage"
        const val RESULT_FILE_SELECTED = "local_file_manager_result_file_selected"
        const val RESULT_FILE_SELECTED_URI = "local_file_manager_result_file_selected_uri"
        const val RESULT_FILE_SELECTED_DISPLAY_NAME = "local_file_manager_result_file_selected_display_name"
        const val RESULT_FILES_SELECTED = "local_file_manager_result_files_selected"
        const val RESULT_FILES_SELECTED_URIS = "local_file_manager_result_files_selected_uris"
        const val RESULT_FILES_SELECTED_DISPLAY_NAMES = "local_file_manager_result_files_selected_display_names"
        const val RESULT_FILES_SELECTED_SKIPPED_COUNT = "local_file_manager_result_files_selected_skipped_count"
    }
}
