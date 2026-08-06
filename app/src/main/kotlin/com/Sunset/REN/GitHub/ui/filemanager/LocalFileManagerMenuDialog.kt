package com.Sunset.REN.GitHub.ui.filemanager

import android.content.Context
import android.view.View
import com.Sunset.REN.GitHub.R

/** Top-level local-file-manager menu dialogs. */
object LocalFileManagerMenuDialog {
    fun showLocations(
        context: Context,
        title: String,
        onAppFiles: () -> Unit,
        onCache: () -> Unit,
        onDownloads: () -> Unit
    ) {
        SelectionActionSheetDialog.show(
            context = context,
            title = title,
            actions = listOf(
                SelectionActionItem(context.getString(R.string.local_file_manager_location_app_files), onAppFiles),
                SelectionActionItem(context.getString(R.string.local_file_manager_location_cache), onCache),
                SelectionActionItem(context.getString(R.string.local_file_manager_location_downloads), onDownloads)
            )
        )
    }

    fun showMore(
        context: Context,
        title: String,
        dualPaneLabel: String,
        refreshLabel: String,
        anchor: View? = null,
        onToggleDualPane: () -> Unit,
        onRefresh: () -> Unit,
        onSearch: () -> Unit,
        onSelectAll: (() -> Unit)? = null,
        onFilter: (() -> Unit)? = null,
        onOpenTerminal: (() -> Unit)? = null,
        onRoot: (() -> Unit)? = null,
        rootLabel: String? = null,
        onAddBookmark: (() -> Unit)? = null,
        onSettings: (() -> Unit)? = null,
        onExit: (() -> Unit)? = null,
        onJumpPath: () -> Unit,
        onSort: () -> Unit,
        onToggleHidden: () -> Unit,
        hiddenLabel: String,
        onSetHome: () -> Unit,
        onStartupPathSettings: () -> Unit,
        onBookmarkSettings: () -> Unit,
        onRecycleBinSettings: () -> Unit,
        recycleBinLabel: String,
        onToggleRecycleBin: () -> Unit,
        onOpenRecycleBin: () -> Unit,
        onClearRecycleBin: () -> Unit,
        onComparePanes: (() -> Unit)? = null
    ) {
        val actions = if (anchor != null) {
            buildList {
                add(SelectionActionItem(context.getString(R.string.local_file_manager_refresh), onRefresh))
                add(SelectionActionItem(context.getString(R.string.local_file_manager_search_hint), onSearch))
                onSelectAll?.let { add(SelectionActionItem(context.getString(R.string.local_file_manager_selection_select_all), it)) }
                add(SelectionActionItem(context.getString(R.string.local_file_manager_menu_filter), onFilter ?: onSearch))
                add(SelectionActionItem(context.getString(R.string.local_file_manager_menu_sort_mode), onSort))
                onOpenTerminal?.let { add(SelectionActionItem(context.getString(R.string.local_file_manager_menu_open_terminal), it)) }
                if (onRoot != null && rootLabel != null) add(SelectionActionItem(rootLabel, onRoot))
                add(SelectionActionItem(hiddenLabel, onToggleHidden))
                onAddBookmark?.let { add(SelectionActionItem(context.getString(R.string.local_file_manager_mt_action_add_bookmark), it)) }
                add(SelectionActionItem(context.getString(R.string.local_file_manager_menu_set_home), onSetHome))
                add(SelectionActionItem(context.getString(R.string.local_file_manager_menu_swap_panes), onToggleDualPane))
                add(SelectionActionItem(context.getString(R.string.local_file_manager_menu_settings), onSettings ?: onBookmarkSettings))
                onExit?.let { add(SelectionActionItem(context.getString(R.string.local_file_manager_menu_exit), it)) }
            }
        } else {
            buildList {
                add(SelectionActionItem(context.getString(R.string.local_file_manager_menu_search_current_directory), onSearch))
                add(SelectionActionItem(context.getString(R.string.local_file_manager_menu_jump_path), onJumpPath))
                add(SelectionActionItem(refreshLabel, onRefresh))
                onComparePanes?.let { add(SelectionActionItem(context.getString(R.string.local_file_manager_menu_compare_folders), it)) }
                add(SelectionActionItem(context.getString(R.string.local_file_manager_menu_sort_mode), onSort))
                if (onRoot != null && rootLabel != null) add(SelectionActionItem(rootLabel, onRoot))
                add(SelectionActionItem(hiddenLabel, onToggleHidden))
                add(SelectionActionItem(context.getString(R.string.local_file_manager_menu_set_home), onSetHome))
                add(SelectionActionItem(context.getString(R.string.local_file_manager_menu_startup_path_settings), onStartupPathSettings))
                add(SelectionActionItem(context.getString(R.string.local_file_manager_menu_bookmark_settings), onBookmarkSettings))
                add(SelectionActionItem(context.getString(R.string.local_file_manager_menu_recycle_bin_settings), onRecycleBinSettings))
                add(SelectionActionItem(recycleBinLabel, onToggleRecycleBin))
                add(SelectionActionItem(context.getString(R.string.local_file_manager_menu_open_recycle_bin), onOpenRecycleBin))
                add(SelectionActionItem(context.getString(R.string.local_file_manager_menu_clear_recycle_bin), onClearRecycleBin))
                add(SelectionActionItem(dualPaneLabel, onToggleDualPane))
            }
        }
        SelectionActionSheetDialog.show(context, title, actions)
    }

    fun showCreateEntryType(
        context: Context,
        title: String,
        fileLabel: String,
        directoryLabel: String,
        onCreateFile: () -> Unit,
        onCreateDirectory: () -> Unit
    ) {
        SelectionActionSheetDialog.show(
            context = context,
            title = title,
            actions = listOf(
                SelectionActionItem(fileLabel, onCreateFile),
                SelectionActionItem(directoryLabel, onCreateDirectory)
            )
        )
    }

    fun showActions(
        context: Context,
        title: String,
        actions: List<Pair<String, () -> Unit>>
    ) {
        SelectionActionSheetDialog.show(
            context = context,
            title = title,
            actions = actions.map { (label, action) -> SelectionActionItem(label, action) }
        )
    }
}