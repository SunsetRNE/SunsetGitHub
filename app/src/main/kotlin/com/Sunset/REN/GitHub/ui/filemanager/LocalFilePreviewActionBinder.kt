package com.Sunset.REN.GitHub.ui.filemanager

class LocalFilePreviewActionBinder(
    private val actionView: LocalFilePreviewActionView,
    private val searchController: LocalFileSearchController,
    private val editorModeController: LocalFileEditorModeController,
    private val actions: Actions
) {
    fun bind() {
        actionView.bindActions(
            onToggleMarkdown = actions.onToggleMarkdown,
            onToggleSearch = { searchController.toggleSearchPanel() },
            onConvert = actions.onConvert,
            onSaveAs = actions.onSaveAs,
            onExtract = actions.onExtract,
            onSearchQueryDone = {
                searchController.findNextMatch()
                true
            },
            onReplaceTextDone = {
                searchController.replaceCurrentMatch()
                true
            },
            onRegexHelp = { searchController.showRegexHelp() },
            onFindPrevious = { searchController.findPreviousMatch() },
            onFindNext = { searchController.findNextMatch() },
            onReplaceCurrent = { searchController.replaceCurrentMatch() },
            onReplaceAll = { searchController.replaceAllMatches() },
            onUndo = { editorModeController.undoEdit() },
            onRedo = { editorModeController.redoEdit() },
            onEdit = actions.onEdit,
            onCancel = { editorModeController.cancelEditMode() },
            onSave = actions.onSave,
            onArchiveClick = actions.onArchiveClick
        )
        searchController.configureStatusRefresh()
    }

    data class Actions(
        val onToggleMarkdown: () -> Unit,
        val onConvert: () -> Unit,
        val onSaveAs: () -> Unit,
        val onExtract: () -> Unit,
        val onEdit: () -> Unit,
        val onSave: () -> Unit,
        val onArchiveClick: () -> Unit
    )
}