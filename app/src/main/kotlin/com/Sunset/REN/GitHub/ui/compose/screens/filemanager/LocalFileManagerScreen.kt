package com.Sunset.REN.GitHub.ui.compose.screens.filemanager

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Sunset.REN.GitHub.data.filemanager.FavoriteDirectoryRecord
import com.Sunset.REN.GitHub.data.filemanager.RecentDirectoryRecord
import com.Sunset.REN.GitHub.data.filemanager.SafDirectoryRecord
import com.Sunset.REN.GitHub.domain.filemanager.FileEntryType
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerEntry
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerEntrySorter
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerListOptions
import com.Sunset.REN.GitHub.domain.filemanager.FileManagerSortMode
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens
import com.Sunset.REN.GitHub.ui.compose.components.SunsetCard
import com.Sunset.REN.GitHub.ui.compose.components.SunsetSecondaryButton
import com.Sunset.REN.GitHub.ui.filemanager.LocalFileManagerUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class FileManagerPaneTarget { Primary, Secondary }

data class FileManagerTaskProgressState(
    val title: String,
    val message: String,
    val completedCount: Int,
    val totalCount: Int,
    val isRunning: Boolean = true,
    val isFailed: Boolean = false
)

@Composable
fun LocalFileManagerScreen(
    state: LocalFileManagerUiState,
    secondaryState: LocalFileManagerUiState? = null,
    isPickerMode: Boolean,
    selectedEntryIds: Set<String>,
    secondarySelectedEntryIds: Set<String> = emptySet(),
    searchQuery: String,
    secondarySearchQuery: String = "",
    listOptions: FileManagerListOptions = FileManagerListOptions(),
    secondaryListOptions: FileManagerListOptions = listOptions,
    onListOptionsChange: (FileManagerListOptions) -> Unit = {},
    onSecondaryListOptionsChange: (FileManagerListOptions) -> Unit = {},
    onInvertSelection: () -> Unit = {},
    onShowEntryProperties: (FileManagerEntry) -> Unit = {},
    onCopyEntryPath: (FileManagerEntry) -> Unit = {},
    onZipSelection: () -> Unit = {},
    onRestoreSelection: (() -> Unit)? = null,
    onNavigateToPath: (String) -> Unit = {},
    onSecondaryNavigateToPath: (String) -> Unit = {},
    searchResultEntries: List<FileManagerEntry>? = null,
    isSearchInProgress: Boolean = false,
    onSubmitSearch: (String) -> Unit = {},
    onExitSearchResults: () -> Unit = {},
    onRangeSelect: (List<FileManagerEntry>) -> Unit = {},
    onSecondaryRangeSelect: (List<FileManagerEntry>) -> Unit = {},
    onSearchQueryChange: (String) -> Unit,
    onSecondarySearchQueryChange: (String) -> Unit = {},
    onOpenAppFiles: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenCache: () -> Unit,
    onOpenRecycleBin: () -> Unit,
    onOpenAuthorizedDirectoryPicker: () -> Unit,
    onOpenAuthorizedDirectory: (SafDirectoryRecord) -> Unit,
    onToggleFavorite: () -> Unit,
    onCreateDirectory: () -> Unit,
    onCreateFile: () -> Unit,
    onSecondaryCreateDirectory: () -> Unit = onCreateDirectory,
    onSecondaryCreateFile: () -> Unit = onCreateFile,
    onOpenFavorite: (FavoriteDirectoryRecord) -> Unit,
    onOpenRecent: (RecentDirectoryRecord) -> Unit,
    onClearRecent: () -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onUp: () -> Unit,
    onSecondaryRefresh: () -> Unit = onRefresh,
    onSecondaryBack: () -> Unit = onBack,
    onSecondaryForward: () -> Unit = onForward,
    onSecondaryUp: () -> Unit = onUp,
    onEntryClick: (FileManagerEntry) -> Unit,
    onSecondaryEntryClick: (FileManagerEntry) -> Unit = onEntryClick,
    onEntryLongClick: (FileManagerEntry) -> Unit,
    onSecondaryEntryLongClick: (FileManagerEntry) -> Unit = onEntryLongClick,
    onToggleEntrySelected: (FileManagerEntry) -> Unit,
    onToggleSecondaryEntrySelected: (FileManagerEntry) -> Unit = onToggleEntrySelected,
    onSelectAll: () -> Unit,
    onSecondarySelectAll: () -> Unit = onSelectAll,
    onClearSelection: () -> Unit,
    onSecondaryClearSelection: () -> Unit = onClearSelection,
    onRenameSelected: () -> Unit,
    onSecondaryRenameSelected: () -> Unit = onRenameSelected,
    onDeleteSelected: () -> Unit,
    onSecondaryDeleteSelected: () -> Unit = onDeleteSelected,
    onCopyPrimaryToSecondary: () -> Unit = {},
    onMovePrimaryToSecondary: () -> Unit = {},
    onCopySecondaryToPrimary: () -> Unit = {},
    onMoveSecondaryToPrimary: () -> Unit = {},
    onSecondaryOpenDownloads: () -> Unit = onOpenDownloads,
    onSecondaryOpenAppFiles: () -> Unit = onOpenAppFiles,
    onDualPaneMenu: () -> Unit = {},
    onDualPaneMore: () -> Unit = {},
    focusedPane: FileManagerPaneTarget = FileManagerPaneTarget.Primary,
    taskProgressState: FileManagerTaskProgressState? = null,
    forceDualPane: Boolean = false,
    onTaskProgressDismiss: () -> Unit = {},
    onTaskProgressCancel: () -> Unit = {},
    onPrimaryPaneFocused: () -> Unit = {},
    onSecondaryPaneFocused: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    val normalizedQuery = searchQuery.trim()
    val visibleEntries = if (searchResultEntries != null) {
        FileManagerEntrySorter.filterAndSort(searchResultEntries, listOptions)
    } else {
        filteredEntries(
            FileManagerEntrySorter.filterAndSort(state.entries, listOptions),
            normalizedQuery
        )
    }
    val rightState = secondaryState ?: state
    val rightNormalizedQuery = secondarySearchQuery.trim()
    val rightVisibleEntries = filteredEntries(
        FileManagerEntrySorter.filterAndSort(rightState.entries, secondaryListOptions),
        rightNormalizedQuery
    )
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas)
    ) {
        val useDualPane = !isPickerMode && (forceDualPane || maxWidth >= 600.dp)
        if (useDualPane) {
            DualPaneFileManager(
                primaryState = state,
                secondaryState = rightState,
                primaryVisibleEntries = visibleEntries,
                secondaryVisibleEntries = rightVisibleEntries,
                primarySelectedEntryIds = selectedEntryIds,
                secondarySelectedEntryIds = secondarySelectedEntryIds,
                focusedPane = focusedPane,
                onPrimaryPaneFocused = onPrimaryPaneFocused,
                onSecondaryPaneFocused = onSecondaryPaneFocused,
                onPrimaryEntryClick = onEntryClick,
                onSecondaryEntryClick = onSecondaryEntryClick,
                onPrimaryEntryLongClick = onEntryLongClick,
                onSecondaryEntryLongClick = onSecondaryEntryLongClick,
                onTogglePrimarySelected = onToggleEntrySelected,
                onToggleSecondarySelected = onToggleSecondaryEntrySelected,
                onPrimaryBack = onBack,
                onSecondaryBack = onSecondaryBack,
                onPrimaryForward = onForward,
                onSecondaryForward = onSecondaryForward,
                onPrimaryUp = onUp,
                onSecondaryUp = onSecondaryUp,
                onPrimaryRefresh = onRefresh,
                onSecondaryRefresh = onSecondaryRefresh,
                onPrimaryCreateDirectory = onCreateDirectory,
                onSecondaryCreateDirectory = onSecondaryCreateDirectory,
                onPrimaryCreateFile = onCreateFile,
                onSecondaryCreateFile = onSecondaryCreateFile,
                onCopyPrimaryToSecondary = onCopyPrimaryToSecondary,
                onMovePrimaryToSecondary = onMovePrimaryToSecondary,
                onCopySecondaryToPrimary = onCopySecondaryToPrimary,
                onMoveSecondaryToPrimary = onMoveSecondaryToPrimary,
                onOpenDownloads = onOpenDownloads,
                onOpenAppFiles = onOpenAppFiles,
                onSecondaryOpenDownloads = onSecondaryOpenDownloads,
                onSecondaryOpenAppFiles = onSecondaryOpenAppFiles,
                onDualPaneMenu = onDualPaneMenu,
                onDualPaneMore = onDualPaneMore,
                listOptions = listOptions,
                secondaryListOptions = secondaryListOptions,
                onListOptionsChange = onListOptionsChange,
                onSecondaryListOptionsChange = onSecondaryListOptionsChange,
                onNavigateToPath = onNavigateToPath,
                onSecondaryNavigateToPath = onSecondaryNavigateToPath,
                onSelectAll = onSelectAll,
                onSecondarySelectAll = onSecondarySelectAll,
                onClearSelection = onClearSelection,
                onSecondaryClearSelection = onSecondaryClearSelection,
                onRenameSelected = onRenameSelected,
                onSecondaryRenameSelected = onSecondaryRenameSelected,
                onDeleteSelected = onDeleteSelected,
                onSecondaryDeleteSelected = onSecondaryDeleteSelected,
                onInvertSelection = onInvertSelection,
                onShowEntryProperties = onShowEntryProperties,
                onCopyEntryPath = onCopyEntryPath,
                onZipSelection = onZipSelection,
                onRestoreSelection = onRestoreSelection,
                onPrimaryRangeSelect = onRangeSelect,
                onSecondaryRangeSelect = onSecondaryRangeSelect,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            FileManagerPane(
                state = state,
                paneTitle = if (isPickerMode) "选择本地文件" else "本地文件管理",
                isPickerMode = isPickerMode,
                showGlobalShortcuts = true,
                selectedEntryIds = selectedEntryIds,
                searchQuery = searchQuery,
                visibleEntries = visibleEntries,
                normalizedQuery = normalizedQuery,
                onSearchQueryChange = onSearchQueryChange,
                searchResultMode = searchResultEntries != null,
                isSearchInProgress = isSearchInProgress,
                onSubmitSearch = onSubmitSearch,
                onExitSearchResults = onExitSearchResults,
                onRangeSelect = onRangeSelect,
                onOpenAppFiles = onOpenAppFiles,
                onOpenDownloads = onOpenDownloads,
                onOpenCache = onOpenCache,
                onOpenRecycleBin = onOpenRecycleBin,
                onOpenAuthorizedDirectoryPicker = onOpenAuthorizedDirectoryPicker,
                onOpenAuthorizedDirectory = onOpenAuthorizedDirectory,
                onToggleFavorite = onToggleFavorite,
                onCreateDirectory = onCreateDirectory,
                onCreateFile = onCreateFile,
                onOpenFavorite = onOpenFavorite,
                onOpenRecent = onOpenRecent,
                onClearRecent = onClearRecent,
                onRefresh = onRefresh,
                onBack = onBack,
                onForward = onForward,
                onUp = onUp,
                onEntryClick = onEntryClick,
                onEntryLongClick = onEntryLongClick,
                onToggleEntrySelected = onToggleEntrySelected,
                onSelectAll = onSelectAll,
                onClearSelection = onClearSelection,
                onRenameSelected = onRenameSelected,
                onDeleteSelected = onDeleteSelected,
                listOptions = listOptions,
                onListOptionsChange = onListOptionsChange,
                onInvertSelection = onInvertSelection,
                onShowEntryProperties = onShowEntryProperties,
                onCopyEntryPath = onCopyEntryPath,
                onZipSelection = onZipSelection,
                onNavigateToPath = onNavigateToPath,
                isFocused = true,
                compactRows = false,
                onPaneFocused = onPrimaryPaneFocused,
                modifier = Modifier.fillMaxSize()
            )
        }
        if (taskProgressState != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.canvas.copy(alpha = 0.92f))
            ) {
                TaskProgressPage(
                    state = taskProgressState,
                    onDismiss = onTaskProgressDismiss,
                    onCancel = onTaskProgressCancel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Dual pane (MT manager style)
// ---------------------------------------------------------------------------

@Composable
private fun DualPaneFileManager(
    primaryState: LocalFileManagerUiState,
    secondaryState: LocalFileManagerUiState,
    primaryVisibleEntries: List<FileManagerEntry>,
    secondaryVisibleEntries: List<FileManagerEntry>,
    primarySelectedEntryIds: Set<String>,
    secondarySelectedEntryIds: Set<String>,
    focusedPane: FileManagerPaneTarget,
    onPrimaryPaneFocused: () -> Unit,
    onSecondaryPaneFocused: () -> Unit,
    onPrimaryEntryClick: (FileManagerEntry) -> Unit,
    onSecondaryEntryClick: (FileManagerEntry) -> Unit,
    onPrimaryEntryLongClick: (FileManagerEntry) -> Unit,
    onSecondaryEntryLongClick: (FileManagerEntry) -> Unit,
    onTogglePrimarySelected: (FileManagerEntry) -> Unit,
    onToggleSecondarySelected: (FileManagerEntry) -> Unit,
    onPrimaryBack: () -> Unit,
    onSecondaryBack: () -> Unit,
    onPrimaryForward: () -> Unit,
    onSecondaryForward: () -> Unit,
    onPrimaryUp: () -> Unit,
    onSecondaryUp: () -> Unit,
    onPrimaryRefresh: () -> Unit,
    onSecondaryRefresh: () -> Unit,
    onPrimaryCreateDirectory: () -> Unit,
    onSecondaryCreateDirectory: () -> Unit,
    onPrimaryCreateFile: () -> Unit,
    onSecondaryCreateFile: () -> Unit,
    onCopyPrimaryToSecondary: () -> Unit,
    onMovePrimaryToSecondary: () -> Unit,
    onCopySecondaryToPrimary: () -> Unit,
    onMoveSecondaryToPrimary: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenAppFiles: () -> Unit,
    onSecondaryOpenDownloads: () -> Unit,
    onSecondaryOpenAppFiles: () -> Unit,
    onDualPaneMenu: () -> Unit,
    onDualPaneMore: () -> Unit,
    listOptions: FileManagerListOptions,
    secondaryListOptions: FileManagerListOptions,
    onListOptionsChange: (FileManagerListOptions) -> Unit,
    onSecondaryListOptionsChange: (FileManagerListOptions) -> Unit,
    onNavigateToPath: (String) -> Unit,
    onSecondaryNavigateToPath: (String) -> Unit,
    onSelectAll: () -> Unit,
    onSecondarySelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onSecondaryClearSelection: () -> Unit,
    onRenameSelected: () -> Unit,
    onSecondaryRenameSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    onSecondaryDeleteSelected: () -> Unit,
    onInvertSelection: () -> Unit,
    onShowEntryProperties: (FileManagerEntry) -> Unit,
    onCopyEntryPath: (FileManagerEntry) -> Unit,
    onZipSelection: () -> Unit,
    onRestoreSelection: (() -> Unit)? = null,
    onPrimaryRangeSelect: (List<FileManagerEntry>) -> Unit = {},
    onSecondaryRangeSelect: (List<FileManagerEntry>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    val isPrimaryFocused = focusedPane == FileManagerPaneTarget.Primary
    val primarySelectedEntries = primaryState.entries.filter {
        it.id in primarySelectedEntryIds && it.type != FileEntryType.Parent
    }
    val secondarySelectedEntries = secondaryState.entries.filter {
        it.id in secondarySelectedEntryIds && it.type != FileEntryType.Parent
    }
    val focusedSelectedEntries = if (isPrimaryFocused) primarySelectedEntries else secondarySelectedEntries
    Column(modifier = modifier.background(colors.canvas)) {
        MtTopBar(
            state = if (isPrimaryFocused) primaryState else secondaryState,
            title = if (isPrimaryFocused) "左窗格" else "右窗格",
            onMenu = onDualPaneMenu,
            onMore = onDualPaneMore
        )
        if (focusedSelectedEntries.isNotEmpty()) {
            MtSelectionBar(
                selectedEntries = focusedSelectedEntries,
                onSelectAll = if (isPrimaryFocused) onSelectAll else onSecondarySelectAll,
                onInvertSelection = onInvertSelection,
                onRenameSelected = if (isPrimaryFocused) onRenameSelected else onSecondaryRenameSelected,
                onClearSelection = if (isPrimaryFocused) onClearSelection else onSecondaryClearSelection,
                onDeleteSelected = if (isPrimaryFocused) onDeleteSelected else onSecondaryDeleteSelected,
                onShowEntryProperties = onShowEntryProperties,
                onCopyEntryPath = onCopyEntryPath,
                onZipSelection = onZipSelection,
                transferCopyLabel = null,
                transferMoveLabel = null,
                onTransferCopy = {},
                onTransferMove = {},
                onRestoreSelection = onRestoreSelection
            )
        }
        Row(modifier = Modifier.weight(1f)) {
            MtFilePane(
                state = primaryState,
                entries = primaryVisibleEntries,
                selectedEntryIds = primarySelectedEntryIds,
                isFocused = isPrimaryFocused,
                onPaneFocused = onPrimaryPaneFocused,
                onEntryClick = onPrimaryEntryClick,
                onEntryLongClick = onPrimaryEntryLongClick,
                onToggleSelected = onTogglePrimarySelected,
                listOptions = listOptions,
                onNavigateToPath = onNavigateToPath,
                onRefresh = onPrimaryRefresh,
                onRangeSelect = onPrimaryRangeSelect,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(colors.border)
            )
            MtFilePane(
                state = secondaryState,
                entries = secondaryVisibleEntries,
                selectedEntryIds = secondarySelectedEntryIds,
                isFocused = !isPrimaryFocused,
                onPaneFocused = onSecondaryPaneFocused,
                onEntryClick = onSecondaryEntryClick,
                onEntryLongClick = onSecondaryEntryLongClick,
                onToggleSelected = onToggleSecondarySelected,
                listOptions = secondaryListOptions,
                onNavigateToPath = onSecondaryNavigateToPath,
                onRefresh = onSecondaryRefresh,
                onRangeSelect = onSecondaryRangeSelect,
                modifier = Modifier.weight(1f)
            )
        }
        DualPaneBottomBar(
            focusedPane = focusedPane,
            primaryState = primaryState,
            secondaryState = secondaryState,
            primarySelectedCount = primarySelectedEntryIds.size,
            secondarySelectedCount = secondarySelectedEntryIds.size,
            onPrimaryBack = onPrimaryBack,
            onSecondaryBack = onSecondaryBack,
            onPrimaryForward = onPrimaryForward,
            onSecondaryForward = onSecondaryForward,
            onPrimaryUp = onPrimaryUp,
            onSecondaryUp = onSecondaryUp,
            onPrimaryRefresh = onPrimaryRefresh,
            onSecondaryRefresh = onSecondaryRefresh,
            onPrimaryCreateDirectory = onPrimaryCreateDirectory,
            onSecondaryCreateDirectory = onSecondaryCreateDirectory,
            onPrimaryCreateFile = onPrimaryCreateFile,
            onSecondaryCreateFile = onSecondaryCreateFile,
            onCopyPrimaryToSecondary = onCopyPrimaryToSecondary,
            onMovePrimaryToSecondary = onMovePrimaryToSecondary,
            onCopySecondaryToPrimary = onCopySecondaryToPrimary,
            onMoveSecondaryToPrimary = onMoveSecondaryToPrimary,
            onOpenDownloads = onOpenDownloads,
            onOpenAppFiles = onOpenAppFiles,
            onSecondaryOpenDownloads = onSecondaryOpenDownloads,
            onSecondaryOpenAppFiles = onSecondaryOpenAppFiles,
            listOptions = listOptions,
            secondaryListOptions = secondaryListOptions,
            onListOptionsChange = onListOptionsChange,
            onSecondaryListOptionsChange = onSecondaryListOptionsChange
        )
    }
}

@Composable
private fun MtTopBar(
    state: LocalFileManagerUiState,
    title: String,
    onMenu: () -> Unit,
    onMore: () -> Unit
) {
    Surface(modifier = Modifier.statusBarsPadding(), color = MtTopBarColor, shadowElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MtTopBarIcon(MtIcons.Menu, "菜单", onMenu)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    readableDualPanePath(state.currentPath.ifBlank { state.locationHint.ifBlank { title } }),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    paneCountText(state.entries),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            MtTopBarIcon(MtIcons.MoreVert, "更多", onMore)
        }
    }
}

@Composable
private fun MtTopBarIcon(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tint: Color = Color.White
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = if (enabled) tint else tint.copy(alpha = 0.35f),
            modifier = Modifier.size(22.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MtFilePane(
    state: LocalFileManagerUiState,
    entries: List<FileManagerEntry>,
    selectedEntryIds: Set<String>,
    isFocused: Boolean,
    onPaneFocused: () -> Unit,
    onEntryClick: (FileManagerEntry) -> Unit,
    onEntryLongClick: (FileManagerEntry) -> Unit,
    onToggleSelected: (FileManagerEntry) -> Unit,
    listOptions: FileManagerListOptions,
    onNavigateToPath: (String) -> Unit,
    onRefresh: () -> Unit,
    onRangeSelect: (List<FileManagerEntry>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    var rangeAnchorId by remember(state.currentPath) { mutableStateOf<String?>(null) }
    LaunchedEffect(selectedEntryIds) {
        if (selectedEntryIds.isEmpty()) rangeAnchorId = null
    }
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(colors.canvas)
            .clickable(onClick = onPaneFocused)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    // MT 聚焦模型：聚焦窗格路径栏要有明确高亮（accentSoft 底色），
                    // 原先 subtleBackground 与画布色在亮/暗主题下都几乎不可分辨
                    .background(if (isFocused) colors.accentSoft else colors.canvas)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MtBreadcrumbBar(
                    path = state.currentPath.ifBlank { state.locationHint },
                    fallback = "尚未打开目录",
                    textColor = if (isFocused) colors.textPrimary else colors.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal,
                    onNavigateToPath = onNavigateToPath,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "${entries.count { it.type != FileEntryType.Parent }} 项",
                    color = colors.textMuted,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isFocused) 2.dp else 1.5.dp)
                    .background(if (isFocused) colors.accent else colors.border.copy(alpha = 0.4f))
            )
        }
        when {
            state.isLoading && entries.isEmpty() -> MtCenterState("加载中…", Modifier.fillMaxSize())
            state.errorMessage?.isNotBlank() == true -> MtCenterState(state.errorMessage, Modifier.fillMaxSize())
            entries.isEmpty() -> MtCenterState("目录为空", Modifier.fillMaxSize())
            else -> PullToRefreshBox(
                isRefreshing = state.isLoading,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(entries, key = { it.id }) { entry ->
                        MtEntryRow(
                            entry = entry,
                            selected = entry.id in selectedEntryIds,
                            onClick = { onEntryClick(entry) },
                            onLongClick = {
                                rangeAnchorId = handleRangeLongClick(
                                    anchorId = rangeAnchorId,
                                    entry = entry,
                                    entries = entries,
                                    onRangeSelect = onRangeSelect,
                                    onEntryLongClick = onEntryLongClick
                                )
                            },
                            onToggleSelected = { onToggleSelected(entry) },
                            listOptions = listOptions
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DualPaneBottomBar(
    focusedPane: FileManagerPaneTarget,
    primaryState: LocalFileManagerUiState,
    secondaryState: LocalFileManagerUiState,
    primarySelectedCount: Int,
    secondarySelectedCount: Int,
    onPrimaryBack: () -> Unit,
    onSecondaryBack: () -> Unit,
    onPrimaryForward: () -> Unit,
    onSecondaryForward: () -> Unit,
    onPrimaryUp: () -> Unit,
    onSecondaryUp: () -> Unit,
    onPrimaryRefresh: () -> Unit,
    onSecondaryRefresh: () -> Unit,
    onPrimaryCreateDirectory: () -> Unit,
    onSecondaryCreateDirectory: () -> Unit,
    onPrimaryCreateFile: () -> Unit,
    onSecondaryCreateFile: () -> Unit,
    onCopyPrimaryToSecondary: () -> Unit,
    onMovePrimaryToSecondary: () -> Unit,
    onCopySecondaryToPrimary: () -> Unit,
    onMoveSecondaryToPrimary: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenAppFiles: () -> Unit,
    onSecondaryOpenDownloads: () -> Unit,
    onSecondaryOpenAppFiles: () -> Unit,
    listOptions: FileManagerListOptions,
    secondaryListOptions: FileManagerListOptions,
    onListOptionsChange: (FileManagerListOptions) -> Unit,
    onSecondaryListOptionsChange: (FileManagerListOptions) -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    val isPrimary = focusedPane == FileManagerPaneTarget.Primary
    val focusedState = if (isPrimary) primaryState else secondaryState
    val selectedCount = if (isPrimary) primarySelectedCount else secondarySelectedCount
    val focusedOptions = if (isPrimary) listOptions else secondaryListOptions
    val onFocusedOptionsChange = if (isPrimary) onListOptionsChange else onSecondaryListOptionsChange
    Surface(modifier = Modifier.navigationBarsPadding(), color = colors.surface, shadowElevation = 4.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MtBarAction(MtIcons.ArrowBack, "后退", if (isPrimary) onPrimaryBack else onSecondaryBack, focusedState.canGoBack)
            MtBarAction(MtIcons.ArrowForward, "前进", if (isPrimary) onPrimaryForward else onSecondaryForward, focusedState.canGoForward)
            MtBarAction(MtIcons.ArrowUp, "上级", if (isPrimary) onPrimaryUp else onSecondaryUp, focusedState.canGoUp)
            MtBarAction(MtIcons.Refresh, "刷新", if (isPrimary) onPrimaryRefresh else onSecondaryRefresh)
            MtSortBarAction(options = focusedOptions, onOptionsChange = onFocusedOptionsChange)
            MtBarAction(MtIcons.FolderNew, "建夹", if (isPrimary) onPrimaryCreateDirectory else onSecondaryCreateDirectory)
            MtBarAction(MtIcons.FileNew, "建文", if (isPrimary) onPrimaryCreateFile else onSecondaryCreateFile)
            MtBarAction(
                MtIcons.Transfer,
                if (isPrimary) "复制到右" else "复制到左",
                if (isPrimary) onCopyPrimaryToSecondary else onCopySecondaryToPrimary,
                selectedCount > 0,
                tint = colors.accent
            )
            MtBarAction(
                MtIcons.Move,
                if (isPrimary) "移到右" else "移到左",
                if (isPrimary) onMovePrimaryToSecondary else onMoveSecondaryToPrimary,
                selectedCount > 0,
                tint = colors.accent
            )
            MtBarAction(MtIcons.Download, "下载", if (isPrimary) onOpenDownloads else onSecondaryOpenDownloads)
            MtBarAction(MtIcons.Home, "应用", if (isPrimary) onOpenAppFiles else onSecondaryOpenAppFiles)
        }
    }
}

@Composable
private fun MtBarAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tint: Color? = null
) {
    val colors = SunsetGitHubThemeTokens.colors
    val contentColor = when {
        !enabled -> colors.textMuted.copy(alpha = 0.4f)
        tint != null -> tint
        else -> colors.textSecondary
    }
    Column(
        modifier = Modifier
            .width(56.dp)
            .fillMaxHeight()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            label,
            color = contentColor,
            fontSize = 9.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ---------------------------------------------------------------------------
// Breadcrumb bar (MT manager style path segments)
// ---------------------------------------------------------------------------

@Composable
private fun MtBreadcrumbBar(
    path: String,
    fallback: String,
    textColor: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    onNavigateToPath: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val segments = remember(path) { path.split('/').filter { it.isNotEmpty() } }
    if (path.isBlank() || segments.isEmpty() && !path.startsWith("/")) {
        Text(
            fallback,
            color = textColor,
            fontSize = fontSize,
            fontWeight = fontWeight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier
        )
        return
    }
    val isAbsolute = path.startsWith("/")
    val scrollState = rememberScrollState()
    LaunchedEffect(path) { scrollState.scrollTo(scrollState.maxValue) }
    Row(
        modifier = modifier.horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isAbsolute) {
            Text(
                "/",
                color = textColor,
                fontSize = fontSize,
                fontWeight = fontWeight,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.clickable { onNavigateToPath("/") }
            )
        }
        segments.forEachIndexed { index, segment ->
            val target = buildString {
                if (isAbsolute) append('/')
                append(segments.take(index + 1).joinToString("/"))
            }
            val isLast = index == segments.lastIndex
            Text(
                segment,
                color = if (isLast) textColor else textColor.copy(alpha = 0.75f),
                fontSize = fontSize,
                fontWeight = if (isLast) fontWeight else FontWeight.Normal,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.clickable { onNavigateToPath(target) }
            )
            if (!isLast) {
                Text(
                    "/",
                    color = textColor.copy(alpha = 0.5f),
                    fontSize = fontSize,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun MtSortBarAction(
    options: FileManagerListOptions,
    onOptionsChange: (FileManagerListOptions) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Box {
        MtBarAction(MtIcons.Sort, "排序", { menuExpanded = true })
        MtSortMenu(
            expanded = menuExpanded,
            onDismiss = { menuExpanded = false },
            options = options,
            onOptionsChange = onOptionsChange
        )
    }
}

@Composable
private fun MtSortMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    options: FileManagerListOptions,
    onOptionsChange: (FileManagerListOptions) -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = colors.surface
    ) {
        Text(
            "排序方式",
            color = colors.textMuted,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
        MtSortModeItem("按名称", FileManagerSortMode.Name, options, onOptionsChange, onDismiss)
        MtSortModeItem("按时间", FileManagerSortMode.Time, options, onOptionsChange, onDismiss)
        MtSortModeItem("按大小", FileManagerSortMode.Size, options, onOptionsChange, onDismiss)
        MtSortModeItem("按类型", FileManagerSortMode.Type, options, onOptionsChange, onDismiss)
        HorizontalDivider(color = colors.divider)
        MtSortCheckItem(
            label = "倒序",
            checked = options.reverse,
            onToggle = { onOptionsChange(options.copy(reverse = !options.reverse)) }
        )
        MtSortCheckItem(
            label = "显示隐藏文件",
            checked = options.showHiddenFiles,
            onToggle = { onOptionsChange(options.copy(showHiddenFiles = !options.showHiddenFiles)) }
        )
        MtSortCheckItem(
            label = "紧凑年份(YY)",
            checked = options.useShortYear,
            onToggle = { onOptionsChange(options.copy(useShortYear = !options.useShortYear)) }
        )
        MtSortCheckItem(
            label = "时间显示秒",
            checked = options.showSeconds,
            onToggle = { onOptionsChange(options.copy(showSeconds = !options.showSeconds)) }
        )
        MtSortCheckItem(
            label = "显示权限位",
            checked = options.showPermissions,
            onToggle = { onOptionsChange(options.copy(showPermissions = !options.showPermissions)) }
        )
    }
}

@Composable
private fun MtSortModeItem(
    label: String,
    mode: FileManagerSortMode,
    options: FileManagerListOptions,
    onOptionsChange: (FileManagerListOptions) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    val selected = options.sortMode == mode
    DropdownMenuItem(
        text = {
            Text(
                label,
                color = if (selected) colors.accent else colors.textPrimary,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        },
        onClick = {
            onOptionsChange(options.copy(sortMode = mode))
            onDismiss()
        },
        trailingIcon = {
            if (selected) {
                Icon(MtIcons.Check, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
            }
        }
    )
}

@Composable
private fun MtSortCheckItem(label: String, checked: Boolean, onToggle: () -> Unit) {
    val colors = SunsetGitHubThemeTokens.colors
    DropdownMenuItem(
        text = {
            Text(
                label,
                color = if (checked) colors.accent else colors.textPrimary,
                fontSize = 13.sp
            )
        },
        onClick = onToggle,
        trailingIcon = {
            if (checked) {
                Icon(MtIcons.Check, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
            }
        }
    )
}

// ---------------------------------------------------------------------------
// MT style entry row: vector icon + name + modified time + permission/size meta
// ---------------------------------------------------------------------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MtEntryRow(
    entry: FileManagerEntry,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleSelected: () -> Unit,
    listOptions: FileManagerListOptions = FileManagerListOptions()
) {
    val colors = SunsetGitHubThemeTokens.colors
    val timeText = formatModifiedTime(entry.modifiedAtMillis, listOptions.useShortYear, listOptions.showSeconds)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(if (selected) colors.accentSoft else Color.Transparent)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(26.dp)) {
                MtEntryIcon(
                    type = entry.type,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Center)
                )
                if (selected) {
                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(colors.accent)
                            .clickable(onClick = onToggleSelected),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            MtIcons.Check,
                            contentDescription = "取消选择",
                            tint = Color.White,
                            modifier = Modifier.size(9.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        entry.name,
                        color = colors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (timeText.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            timeText,
                            color = colors.textMuted,
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    entryPermissionMeta(entry, listOptions.showPermissions),
                    color = colors.textSecondary,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 46.dp)
                .height(0.5.dp)
                .background(colors.divider)
        )
    }
}

@Composable
private fun MtEntryIcon(type: FileEntryType, modifier: Modifier = Modifier) {
    val colors = SunsetGitHubThemeTokens.colors
    val (icon, tint) = when (type) {
        FileEntryType.Parent -> MtIcons.ArrowUp to colors.textSecondary
        FileEntryType.Directory -> MtIcons.Folder to MtFolderAmber
        FileEntryType.Apk -> MtIcons.Apk to MtApkGreen
        FileEntryType.Archive -> MtIcons.Archive to MtArchiveOrange
        FileEntryType.Image -> MtIcons.Image to MtImagePurple
        FileEntryType.Code -> MtIcons.Code to MtCodeBlue
        FileEntryType.Text, FileEntryType.Markdown -> MtIcons.TextFile to MtTextBlue
        FileEntryType.Binary, FileEntryType.Unknown -> MtIcons.File to MtFileGray
    }
    Icon(icon, contentDescription = null, tint = tint, modifier = modifier)
}

// ---------------------------------------------------------------------------
// Single pane (MT manager style)
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileManagerPane(
    state: LocalFileManagerUiState,
    paneTitle: String,
    isPickerMode: Boolean,
    showGlobalShortcuts: Boolean,
    selectedEntryIds: Set<String>,
    searchQuery: String,
    visibleEntries: List<FileManagerEntry>,
    normalizedQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchResultMode: Boolean = false,
    isSearchInProgress: Boolean = false,
    onSubmitSearch: (String) -> Unit = {},
    onExitSearchResults: () -> Unit = {},
    onRangeSelect: (List<FileManagerEntry>) -> Unit = {},
    onOpenAppFiles: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenCache: () -> Unit,
    onOpenRecycleBin: () -> Unit,
    onOpenAuthorizedDirectoryPicker: () -> Unit,
    onOpenAuthorizedDirectory: (SafDirectoryRecord) -> Unit,
    onToggleFavorite: () -> Unit,
    onCreateDirectory: () -> Unit,
    onCreateFile: () -> Unit,
    onOpenFavorite: (FavoriteDirectoryRecord) -> Unit,
    onOpenRecent: (RecentDirectoryRecord) -> Unit,
    onClearRecent: () -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onUp: () -> Unit,
    onEntryClick: (FileManagerEntry) -> Unit,
    onEntryLongClick: (FileManagerEntry) -> Unit,
    onToggleEntrySelected: (FileManagerEntry) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onRenameSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    transferCopyLabel: String? = null,
    transferMoveLabel: String? = null,
    onTransferCopy: () -> Unit = {},
    onTransferMove: () -> Unit = {},
    listOptions: FileManagerListOptions = FileManagerListOptions(),
    onListOptionsChange: (FileManagerListOptions) -> Unit = {},
    onInvertSelection: () -> Unit = {},
    onShowEntryProperties: (FileManagerEntry) -> Unit = {},
    onCopyEntryPath: (FileManagerEntry) -> Unit = {},
    onZipSelection: () -> Unit = {},
    onRestoreSelection: (() -> Unit)? = null,
    onNavigateToPath: (String) -> Unit = {},
    isFocused: Boolean = false,
    compactRows: Boolean = false,
    onPaneFocused: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    val selectedEntries = state.entries.filter { it.id in selectedEntryIds && it.type != FileEntryType.Parent }
    var rangeAnchorId by remember(state.currentPath) { mutableStateOf<String?>(null) }
    LaunchedEffect(selectedEntryIds) {
        if (selectedEntryIds.isEmpty()) rangeAnchorId = null
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas)
            .clickable(onClick = onPaneFocused)
    ) {
        MtSingleTopBar(
            state = state,
            paneTitle = paneTitle,
            onBack = onBack,
            onForward = onForward,
            onUp = onUp,
            onToggleFavorite = onToggleFavorite,
            onRefresh = onRefresh,
            onNavigateToPath = onNavigateToPath
        )
        MtSearchRow(
            query = searchQuery,
            totalCount = state.entries.size,
            visibleCount = visibleEntries.size,
            onQueryChange = onSearchQueryChange,
            onSubmit = onSubmitSearch,
            isSearching = isSearchInProgress
        )
        if (searchResultMode) {
            MtSearchResultRow(
                resultCount = visibleEntries.size,
                query = searchQuery,
                onExit = onExitSearchResults
            )
        }
        if (selectedEntries.isNotEmpty()) {
            MtSelectionBar(
                selectedEntries = selectedEntries,
                onSelectAll = onSelectAll,
                onInvertSelection = onInvertSelection,
                onRenameSelected = onRenameSelected,
                onClearSelection = onClearSelection,
                onDeleteSelected = onDeleteSelected,
                onShowEntryProperties = onShowEntryProperties,
                onCopyEntryPath = onCopyEntryPath,
                onZipSelection = onZipSelection,
                onRestoreSelection = onRestoreSelection,
                transferCopyLabel = transferCopyLabel,
                transferMoveLabel = transferMoveLabel,
                onTransferCopy = onTransferCopy,
                onTransferMove = onTransferMove
            )
        }
        if (showGlobalShortcuts) {
            MtShortcutRow(
                state = state,
                isPickerMode = isPickerMode,
                onOpenAppFiles = onOpenAppFiles,
                onOpenDownloads = onOpenDownloads,
                onOpenCache = onOpenCache,
                onOpenRecycleBin = onOpenRecycleBin,
                onOpenAuthorizedDirectoryPicker = onOpenAuthorizedDirectoryPicker,
                onOpenFavorite = onOpenFavorite,
                onOpenRecent = onOpenRecent,
                onClearRecent = onClearRecent,
                onOpenAuthorizedDirectory = onOpenAuthorizedDirectory
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            when {
                state.isLoading && visibleEntries.isEmpty() -> MtLoadingState(Modifier.fillMaxSize())
                state.errorMessage?.isNotBlank() == true ->
                    MtCenterState(state.errorMessage, Modifier.fillMaxSize())
                visibleEntries.isEmpty() -> {
                    if (normalizedQuery.isBlank()) {
                        MtEmptyState(
                            onCreateDirectory = onCreateDirectory,
                            onCreateFile = onCreateFile,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        MtCenterState("没有匹配的文件。", Modifier.fillMaxSize())
                    }
                }
                else -> PullToRefreshBox(
                    isRefreshing = state.isLoading,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(visibleEntries, key = { it.id }) { entry ->
                            MtEntryRow(
                                entry = entry,
                                selected = entry.id in selectedEntryIds,
                                onClick = { onEntryClick(entry) },
                                onLongClick = {
                                    rangeAnchorId = handleRangeLongClick(
                                        anchorId = rangeAnchorId,
                                        entry = entry,
                                        entries = visibleEntries,
                                        onRangeSelect = onRangeSelect,
                                        onEntryLongClick = onEntryLongClick
                                    )
                                },
                                onToggleSelected = { onToggleEntrySelected(entry) },
                                listOptions = listOptions
                            )
                        }
                    }
                }
            }
        }
        MtSingleBottomBar(
            state = state,
            onBack = onBack,
            onForward = onForward,
            onUp = onUp,
            onRefresh = onRefresh,
            onCreateDirectory = onCreateDirectory,
            onCreateFile = onCreateFile,
            onOpenDownloads = onOpenDownloads,
            onOpenAppFiles = onOpenAppFiles,
            listOptions = listOptions,
            onListOptionsChange = onListOptionsChange
        )
    }
}

@Composable
private fun MtSingleTopBar(
    state: LocalFileManagerUiState,
    paneTitle: String,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onUp: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRefresh: () -> Unit,
    onNavigateToPath: (String) -> Unit
) {
    Surface(modifier = Modifier.statusBarsPadding(), color = MtTopBarColor, shadowElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MtTopBarIcon(MtIcons.ArrowBack, "后退", onBack, enabled = state.canGoBack)
            MtTopBarIcon(MtIcons.ArrowForward, "前进", onForward, enabled = state.canGoForward)
            MtTopBarIcon(MtIcons.ArrowUp, "上级", onUp, enabled = state.canGoUp)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp)
            ) {
                MtBreadcrumbBar(
                    path = state.currentPath.ifBlank { state.locationHint },
                    fallback = paneTitle,
                    textColor = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    onNavigateToPath = onNavigateToPath,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    paneCountText(state.entries),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            MtTopBarIcon(
                if (state.isCurrentFavorite) MtIcons.Star else MtIcons.StarBorder,
                if (state.isCurrentFavorite) "取消收藏" else "收藏",
                onToggleFavorite,
                tint = if (state.isCurrentFavorite) MtStarYellow else Color.White
            )
            MtTopBarIcon(MtIcons.Refresh, "刷新", onRefresh)
        }
    }
}

@Composable
private fun MtSearchRow(
    query: String,
    totalCount: Int,
    visibleCount: Int,
    onQueryChange: (String) -> Unit,
    onSubmit: (String) -> Unit = {},
    isSearching: Boolean = false
) {
    val colors = SunsetGitHubThemeTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(colors.subtleBackground)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                MtIcons.Search,
                contentDescription = "递归搜索",
                tint = if (isSearching) colors.accent else colors.textMuted,
                modifier = Modifier
                    .size(17.dp)
                    .clickable(enabled = query.isNotBlank() && !isSearching) { onSubmit(query) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text("搜索当前目录，回车递归搜索", color = colors.textMuted, fontSize = 12.sp)
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(color = colors.textPrimary, fontSize = 12.sp),
                    cursorBrush = SolidColor(colors.accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { if (query.isNotBlank()) onSubmit(query) }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (query.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .clickable { onQueryChange("") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        MtIcons.Close,
                        contentDescription = "清除",
                        tint = colors.textMuted,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            if (isSearching) "搜索中…" else if (query.isBlank()) "$totalCount 项" else "$visibleCount/$totalCount",
            color = colors.textSecondary,
            fontSize = 10.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun MtSearchResultRow(
    resultCount: Int,
    query: String,
    onExit: () -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.accentSoft)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "递归搜索「$query」：$resultCount 个结果",
            color = colors.accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            "退出搜索",
            color = colors.accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable(onClick = onExit)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun MtSelectionBar(
    selectedEntries: List<FileManagerEntry>,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    onRenameSelected: () -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    onShowEntryProperties: (FileManagerEntry) -> Unit,
    onCopyEntryPath: (FileManagerEntry) -> Unit,
    onZipSelection: () -> Unit,
    transferCopyLabel: String?,
    transferMoveLabel: String?,
    onTransferCopy: () -> Unit,
    onTransferMove: () -> Unit,
    onRestoreSelection: (() -> Unit)? = null
) {
    val colors = SunsetGitHubThemeTokens.colors
    val count = selectedEntries.size
    val totalSize = selectedEntries.sumOf { it.sizeBytes ?: 0L }
    val singleEntry = selectedEntries.singleOrNull()
    Surface(color = colors.accentSoft) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (totalSize > 0L) "已选 $count 项 · ${formatSizeMt(totalSize)}" else "已选 $count 项",
                color = colors.accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.padding(end = 8.dp)
            )
            MtSelectionAction("全选", onSelectAll)
            MtSelectionAction("反选", onInvertSelection)
            if (onRestoreSelection != null) {
                MtSelectionAction("恢复", onRestoreSelection)
            }
            MtSelectionAction("重命名", onRenameSelected, enabled = count == 1)
            if (singleEntry != null) {
                MtSelectionAction("属性", { onShowEntryProperties(singleEntry) })
                MtSelectionAction("复制路径", { onCopyEntryPath(singleEntry) })
            }
            MtSelectionAction("压缩", onZipSelection)
            transferCopyLabel?.let { MtSelectionAction(it, onTransferCopy) }
            transferMoveLabel?.let { MtSelectionAction(it, onTransferMove) }
            MtSelectionAction("删除", onDeleteSelected, tint = colors.danger)
            MtSelectionAction("清除", onClearSelection)
        }
    }
}

@Composable
private fun MtSelectionAction(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tint: Color? = null
) {
    val colors = SunsetGitHubThemeTokens.colors
    Text(
        label,
        color = if (enabled) (tint ?: colors.accent) else colors.textMuted.copy(alpha = 0.5f),
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    )
}

@Composable
private fun MtShortcutRow(
    state: LocalFileManagerUiState,
    isPickerMode: Boolean,
    onOpenAppFiles: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenCache: () -> Unit,
    onOpenRecycleBin: () -> Unit,
    onOpenAuthorizedDirectoryPicker: () -> Unit,
    onOpenFavorite: (FavoriteDirectoryRecord) -> Unit,
    onOpenRecent: (RecentDirectoryRecord) -> Unit,
    onClearRecent: () -> Unit,
    onOpenAuthorizedDirectory: (SafDirectoryRecord) -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MtChip(MtIcons.Home, "应用文件", onOpenAppFiles)
            MtChip(MtIcons.Download, "下载", onOpenDownloads)
            MtChip(MtIcons.Folder, "缓存", onOpenCache)
            if (!isPickerMode) {
                MtChip(MtIcons.RecycleBin, "回收站", onOpenRecycleBin, enabled = state.isRecycleBinEnabled)
            }
            MtChip(MtIcons.FolderSpecial, "授权目录", onOpenAuthorizedDirectoryPicker)
            state.favoriteDirectories.take(6).forEach { record ->
                MtChip(MtIcons.Star, record.label, { onOpenFavorite(record) }, tint = MtFolderAmber)
            }
            state.recentDirectories.take(6).forEach { record ->
                MtChip(MtIcons.History, record.directory.label, { onOpenRecent(record) })
            }
            if (state.recentDirectories.isNotEmpty()) {
                MtChip(MtIcons.Close, "清空最近", onClearRecent)
            }
            state.authorizedDirectories.take(4).forEach { record ->
                MtChip(MtIcons.FolderSpecial, record.label.ifBlank { "授权目录" }, { onOpenAuthorizedDirectory(record) })
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(colors.divider)
        )
    }
}

@Composable
private fun MtChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tint: Color? = null
) {
    val colors = SunsetGitHubThemeTokens.colors
    val contentColor = if (enabled) colors.textPrimary else colors.textMuted.copy(alpha = 0.4f)
    val iconColor = if (enabled) (tint ?: colors.textSecondary) else colors.textMuted.copy(alpha = 0.4f)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.chipBackground)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(5.dp))
        Text(label, color = contentColor, fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
private fun MtSingleBottomBar(
    state: LocalFileManagerUiState,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onUp: () -> Unit,
    onRefresh: () -> Unit,
    onCreateDirectory: () -> Unit,
    onCreateFile: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenAppFiles: () -> Unit,
    listOptions: FileManagerListOptions,
    onListOptionsChange: (FileManagerListOptions) -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    Surface(modifier = Modifier.navigationBarsPadding(), color = colors.surface, shadowElevation = 4.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MtBarAction(MtIcons.ArrowBack, "后退", onBack, state.canGoBack)
            MtBarAction(MtIcons.ArrowForward, "前进", onForward, state.canGoForward)
            MtBarAction(MtIcons.ArrowUp, "上级", onUp, state.canGoUp)
            MtBarAction(MtIcons.Refresh, "刷新", onRefresh)
            MtSortBarAction(options = listOptions, onOptionsChange = onListOptionsChange)
            MtBarAction(MtIcons.FolderNew, "建夹", onCreateDirectory)
            MtBarAction(MtIcons.FileNew, "建文", onCreateFile)
            MtBarAction(MtIcons.Download, "下载", onOpenDownloads)
            MtBarAction(MtIcons.Home, "应用", onOpenAppFiles)
        }
    }
}

// ---------------------------------------------------------------------------
// State placeholders
// ---------------------------------------------------------------------------

@Composable
private fun MtCenterState(message: String, modifier: Modifier = Modifier) {
    val colors = SunsetGitHubThemeTokens.colors
    Box(modifier = modifier.padding(16.dp), contentAlignment = Alignment.Center) {
        Text(message, color = colors.textSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun MtLoadingState(modifier: Modifier = Modifier) {
    val colors = SunsetGitHubThemeTokens.colors
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LinearProgressIndicator(
                modifier = Modifier.width(120.dp),
                color = colors.accent,
                trackColor = colors.border.copy(alpha = 0.35f)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text("加载中…", color = colors.textSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun MtEmptyState(
    onCreateDirectory: () -> Unit,
    onCreateFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                MtIcons.Folder,
                contentDescription = null,
                tint = MtFolderAmber.copy(alpha = 0.55f),
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("目录为空", color = colors.textSecondary, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row {
                Text(
                    "新建文件夹",
                    color = colors.accent,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onCreateDirectory)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "新建文件",
                    color = colors.accent,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onCreateFile)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Task progress
// ---------------------------------------------------------------------------

@Composable
private fun TaskProgressPage(
    state: FileManagerTaskProgressState,
    onDismiss: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    val safeTotal = state.totalCount.coerceAtLeast(1)
    val progress = state.completedCount.coerceIn(0, safeTotal).toFloat() / safeTotal.toFloat()
    val footnote = when {
        state.isFailed -> "任务失败，关闭后可返回文件管理器检查目录状态。"
        state.isRunning -> "任务进行中，完成后将保留结果供确认。"
        else -> "任务已完成，关闭后返回文件管理器。"
    }
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        SunsetCard(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TaskStatusBadge(isRunning = state.isRunning, isFailed = state.isFailed)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(state.title, color = colors.textPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(state.message, color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                    if (state.isRunning) {
                        SunsetSecondaryButton("取消", onClick = onCancel)
                    } else {
                        SunsetSecondaryButton("关闭", onClick = onDismiss)
                    }
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.accent,
                    trackColor = colors.border.copy(alpha = 0.35f)
                )
                Text("${state.completedCount} / ${state.totalCount}", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                Text(footnote, color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun TaskStatusBadge(isRunning: Boolean, isFailed: Boolean) {
    val colors = SunsetGitHubThemeTokens.colors
    val (label, background, foreground) = when {
        isFailed -> Triple("!", Color(0xFFFFE4E6), Color(0xFFBE123C))
        isRunning -> Triple("RUN", colors.accentSoft, colors.accent)
        else -> Triple("OK", Color(0xFFD1FAE5), Color(0xFF047857))
    }
    Surface(
        modifier = Modifier.size(44.dp),
        shape = MaterialTheme.shapes.medium,
        color = background,
        border = BorderStroke(1.dp, foreground.copy(alpha = 0.28f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = foreground, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

// ---------------------------------------------------------------------------
// MT style vector icon set (self-drawn, no material-icons dependency)
// ---------------------------------------------------------------------------

private val MtTopBarColor = Color(0xFF2F2F2F)
private val MtFolderAmber = Color(0xFFE0A94E)
private val MtApkGreen = Color(0xFF3FB950)
private val MtArchiveOrange = Color(0xFFE0883B)
private val MtImagePurple = Color(0xFFA371F7)
private val MtCodeBlue = Color(0xFF6E9EEF)
private val MtTextBlue = Color(0xFF7A93B5)
private val MtFileGray = Color(0xFF9AA4AE)
private val MtStarYellow = Color(0xFFF2CC60)

private object MtIcons {
    private fun icon(name: String, pathData: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).addPath(
            pathData = addPathNodes(pathData),
            fill = SolidColor(Color.Black)
        ).build()

    val Menu by lazy { icon("Menu", "M3,18h18v-2H3v2zM3,13h18v-2H3v2zM3,6v2h18V6H3z") }
    val MoreVert by lazy { icon("MoreVert", "M12,8c1.1,0 2,-0.9 2,-2s-0.9,-2 -2,-2 -2,0.9 -2,2 0.9,2 2,2zM12,10c-1.1,0 -2,0.9 -2,2s0.9,2 2,2 2,-0.9 2,-2 -0.9,-2 -2,-2zM12,16c-1.1,0 -2,0.9 -2,2s0.9,2 2,2 2,-0.9 2,-2 -0.9,-2 -2,-2z") }
    val ArrowBack by lazy { icon("ArrowBack", "M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13H20v-2z") }
    val ArrowForward by lazy { icon("ArrowForward", "M12,4l-1.41,1.41L16.17,11H4v2h12.17l-5.58,5.59L12,20l8,-8 -8,-8z") }
    val ArrowUp by lazy { icon("ArrowUp", "M4,12l1.41,1.41L11,7.83V20h2V7.83l5.58,5.59L20,12l-8,-8 -8,8z") }
    val Refresh by lazy { icon("Refresh", "M17.65,6.35C16.2,4.9 14.21,4 12,4c-4.42,0 -7.99,3.58 -7.99,8s3.57,8 7.99,8c3.73,0 6.84,-2.55 7.73,-6h-2.08c-0.82,2.33 -3.04,4 -5.65,4 -3.31,0 -6,-2.69 -6,-6s2.69,-6 6,-6c1.66,0 3.14,0.69 4.22,1.78L13,11h7V4l-2.35,2.35z") }
    val Search by lazy { icon("Search", "M15.5,14h-0.79l-0.28,-0.27C15.41,12.59 16,11.11 16,9.5 16,5.91 13.09,3 9.5,3S3,5.91 3,9.5 5.91,16 9.5,16c1.61,0 3.09,-0.59 4.23,-1.57l0.27,0.28v0.79l5,4.99L20.49,19l-4.99,-5zM9.5,14C7.01,14 5,11.99 5,9.5S7.01,5 9.5,5 14,7.01 14,9.5 11.99,14 9.5,14z") }
    val Close by lazy { icon("Close", "M19,6.41L17.59,5 12,10.59 6.41,5 5,6.41 10.59,12 5,17.59 6.41,19 12,13.41 17.59,19 19,17.59 13.41,12z") }
    val Check by lazy { icon("Check", "M9,16.17L4.83,12l-1.42,1.41L9,19 21,7l-1.41,-1.41z") }
    val Star by lazy { icon("Star", "M12,17.27L18.18,21l-1.64,-7.03L22,9.24l-7.19,-0.61L12,2 9.19,8.63 2,9.24l5.46,4.73L5.82,21z") }
    val StarBorder by lazy { icon("StarBorder", "M22,9.24l-7.19,-0.62L12,2 9.19,8.63 2,9.24l5.46,4.73L5.82,21 12,17.27 18.18,21l-1.63,-7.03L22,9.24zM12,15.4l-3.76,2.27 1,-4.28 -3.32,-2.88 4.38,-0.38L12,6.1l1.71,4.04 4.38,0.38 -3.32,2.88 1,4.28L12,15.4z") }
    val Home by lazy { icon("Home", "M10,20v-6h4v6h5v-8h3L12,3 2,12h3v8z") }
    val Download by lazy { icon("Download", "M19,9h-4V3H9v6H5l7,7 7,-7zM5,18v2h14v-2H5z") }
    val FolderNew by lazy { icon("FolderNew", "M20,6h-8l-2,-2H4C2.9,4 2,4.9 2,6v12c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V8c0,-1.1 -0.9,-2 -2,-2zM19,14h-3v3h-2v-3h-3v-2h3V9h2v3h3V14z") }
    val FileNew by lazy { icon("FileNew", "M14,2H6C4.9,2 4.01,2.9 4.01,4L4,20c0,1.1 0.89,2 1.99,2H18c1.1,0 2,-0.9 2,-2V8L14,2zM16,16h-3v3h-2v-3H8v-2h3v-3h2v3h3V16zM13,9V3.5L18.5,9H13z") }
    val Folder by lazy { icon("Folder", "M10,4H4c-1.1,0 -1.99,0.9 -1.99,2L2,18c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V8c0,-1.1 -0.9,-2 -2,-2h-8L10,4z") }
    val FolderSpecial by lazy { icon("FolderSpecial", "M20,6h-8l-2,-2H4C2.9,4 2,4.9 2,6v12c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V8c0,-1.1 -0.9,-2 -2,-2zM17.94,17L15,15.28 12.06,17l0.78,-3.33 -2.59,-2.24 3.41,-0.29L15,8l1.34,3.14 3.41,0.29 -2.59,2.24 0.78,3.33z") }
    val File by lazy { icon("File", "M6,2c-1.1,0 -1.99,0.9 -1.99,2L4,20c0,1.1 0.89,2 1.99,2H18c1.1,0 2,-0.9 2,-2V8l-6,-6H6zM13,9V3.5L18.5,9H13z") }
    val TextFile by lazy { icon("TextFile", "M14,2H6C4.9,2 4.01,2.9 4.01,4L4,20c0,1.1 0.89,2 1.99,2H18c1.1,0 2,-0.9 2,-2V8L14,2zM16,18H8v-2h8V18zM16,14H8v-2h8V14zM13,9V3.5L18.5,9H13z") }
    val Image by lazy { icon("Image", "M21,19V5c0,-1.1 -0.9,-2 -2,-2H5C3.9,3 3,3.9 3,5v14c0,1.1 0.9,2 2,2h14c1.1,0 2,-0.9 2,-2zM8.5,13.5l2.5,3.01L14.5,12l4.5,6H5l3.5,-4.5z") }
    val Archive by lazy { icon("Archive", "M20.54,5.23l-1.39,-1.68C18.88,3.21 18.47,3 18,3H6c-0.47,0 -0.88,0.21 -1.16,0.55L3.46,5.23C3.17,5.57 3,6.02 3,6.5V19c0,1.1 0.9,2 2,2h14c1.1,0 2,-0.9 2,-2V6.5c0,-0.48 -0.17,-0.93 -0.46,-1.27zM12,17.5L6.5,12H10v-2h4v2h3.5L12,17.5zM5.12,5l0.81,-1h12l0.94,1H5.12z") }
    val Apk by lazy { icon("Apk", "M17.6,9.48l1.84,-3.18c0.16,-0.31 0.04,-0.69 -0.26,-0.85 -0.29,-0.15 -0.65,-0.06 -0.83,0.22l-1.88,3.24c-2.86,-1.21 -6.08,-1.21 -8.94,0L5.65,5.67c-0.19,-0.29 -0.58,-0.38 -0.87,-0.2 -0.28,0.18 -0.37,0.54 -0.22,0.83L6.4,9.48C3.3,11.25 1.28,14.44 1,18h22c-0.28,-3.56 -2.3,-6.75 -5.4,-8.52zM7,15.25c-0.69,0 -1.25,-0.56 -1.25,-1.25 0,-0.69 0.56,-1.25 1.25,-1.25s1.25,0.56 1.25,1.25c0,0.69 -0.56,1.25 -1.25,1.25zM17,15.25c-0.69,0 -1.25,-0.56 -1.25,-1.25 0,-0.69 0.56,-1.25 1.25,-1.25s1.25,0.56 1.25,1.25c0,0.69 -0.56,1.25 -1.25,1.25z") }
    val Code by lazy { icon("Code", "M9.4,16.6L4.8,12l4.6,-4.6L8,6l-6,6 6,6 1.4,-1.4zM14.6,16.6l4.6,-4.6 -4.6,-4.6L16,6l6,6 -6,6 -1.4,-1.4z") }
    val Transfer by lazy { icon("Transfer", "M6.99,11L3,15l3.99,4v-3H14v-2H6.99v-3zM21,9l-3.99,-4v3H10v2h7.01v3L21,9z") }
    val Move by lazy { icon("Move", "M20,6h-8l-2,-2H4C2.9,4 2,4.9 2,6v12c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V8c0,-1.1 -0.9,-2 -2,-2zM18,12v4h-2v-3h-2v-2h2V8l4,4 -4,4z") }
    val RecycleBin by lazy { icon("RecycleBin", "M19,4h-3.5l-1,-1h-5l-1,1H5v2h14V4zM6,7v12c0,1.1 0.9,2 2,2h8c1.1,0 2,-0.9 2,-2V7H6zM14,14v4h-4v-4H8l4,-4 4,4H14z") }
    val History by lazy { icon("History", "M13,3c-4.97,0 -9,4.03 -9,9H1l3.89,3.89 0.07,0.14L9,12H6c0,-3.87 3.13,-7 7,-7s7,3.13 7,7 -3.13,7 -7,7c-1.93,0 -3.68,-0.79 -4.94,-2.06l-1.42,1.42C8.27,19.99 10.51,21 13,21c4.97,0 9,-4.03 9,-9s-4.03,-9 -9,-9zM12,8v5l4.28,2.54 0.72,-1.21 -3.5,-2.08V8H12z") }
    val Sort by lazy { icon("Sort", "M3,18h6v-2H3v2zM3,6v2h18V6H3zM3,13h12v-2H3v2z") }
}

// ---------------------------------------------------------------------------
// Formatting helpers (MT manager style)
// ---------------------------------------------------------------------------

private fun filteredEntries(entries: List<FileManagerEntry>, normalizedQuery: String): List<FileManagerEntry> {
    if (normalizedQuery.isBlank()) return entries
    return entries.filter { entry ->
        entry.name.contains(normalizedQuery, ignoreCase = true) ||
            entry.displayPath.contains(normalizedQuery, ignoreCase = true) ||
            typeLabel(entry.type).contains(normalizedQuery, ignoreCase = true)
    }
}

/**
 * MT-style range selection: long-press an anchor entry, then long-press a second
 * entry to select everything between them (inclusive). Returns the new anchor id.
 */
private fun handleRangeLongClick(
    anchorId: String?,
    entry: FileManagerEntry,
    entries: List<FileManagerEntry>,
    onRangeSelect: (List<FileManagerEntry>) -> Unit,
    onEntryLongClick: (FileManagerEntry) -> Unit
): String? {
    if (anchorId != null && anchorId != entry.id && entry.type != FileEntryType.Parent) {
        val ids = entries.map { it.id }
        val from = ids.indexOf(anchorId)
        val to = ids.indexOf(entry.id)
        if (from >= 0 && to >= 0) {
            val range = entries
                .subList(minOf(from, to), maxOf(from, to) + 1)
                .filter { it.type != FileEntryType.Parent }
            if (range.isNotEmpty()) onRangeSelect(range)
        }
        return null
    }
    onEntryLongClick(entry)
    return if (entry.type != FileEntryType.Parent) entry.id else null
}

private fun readableDualPanePath(path: String): String {
    if (path.isBlank()) return "本地文件"
    val normalized = path.trimEnd('/').ifBlank { path }
    return when {
        normalized == "/storage/emulated/0" -> normalized
        normalized.startsWith("/storage/emulated/0/") -> "..." + normalized.removePrefix("/storage/emulated/0")
        normalized.length > 34 -> "..." + normalized.takeLast(31)
        else -> normalized
    }
}

private fun paneCountText(entries: List<FileManagerEntry>): String {
    val realEntries = entries.filterNot { it.type == FileEntryType.Parent }
    val folderCount = realEntries.count { it.type == FileEntryType.Directory }
    val fileCount = realEntries.size - folderCount
    return "文件夹: $folderCount  文件: $fileCount"
}

private fun typeLabel(type: FileEntryType): String = when (type) {
    FileEntryType.Parent -> "上级"
    FileEntryType.Directory -> "目录"
    FileEntryType.Text -> "文本"
    FileEntryType.Markdown -> "Markdown"
    FileEntryType.Code -> "代码"
    FileEntryType.Image -> "图片"
    FileEntryType.Archive -> "压缩包"
    FileEntryType.Apk -> "APK"
    FileEntryType.Binary -> "二进制"
    FileEntryType.Unknown -> "文件"
}

private fun entryPermissionMeta(entry: FileManagerEntry, showPermissions: Boolean = true): String {
    if (entry.type == FileEntryType.Parent) return "返回上级目录"
    val size = when (entry.type) {
        FileEntryType.Directory -> null
        else -> entry.sizeBytes?.let(::formatSizeMt)
    }
    if (!showPermissions) return size ?: ""
    val permission = buildString {
        append(if (entry.type == FileEntryType.Directory) 'd' else '-')
        append(if (entry.capabilities.canRead) 'r' else '-')
        append(if (entry.capabilities.canWrite) 'w' else '-')
        append("------")
    }
    return if (size != null) "$permission $size" else permission
}

private fun formatModifiedTime(
    millis: Long?,
    useShortYear: Boolean = true,
    showSeconds: Boolean = false
): String {
    if (millis == null || millis <= 0L) return ""
    val pattern = buildString {
        append(if (useShortYear) "yy-MM-dd" else "yyyy-MM-dd")
        append(if (showSeconds) " HH:mm:ss" else " HH:mm")
    }
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))
}

private fun formatSizeMt(sizeBytes: Long): String {
    if (sizeBytes < 1024L) return "${sizeBytes}B"
    val kb = sizeBytes / 1024.0
    if (kb < 1024.0) return String.format(Locale.US, "%.2fK", kb)
    val mb = kb / 1024.0
    if (mb < 1024.0) return String.format(Locale.US, "%.2fM", mb)
    val gb = mb / 1024.0
    return String.format(Locale.US, "%.2fG", gb)
}
