package com.Sunset.REN.GitHub.ui.filemanager

import android.net.Uri

data class LocalFilePreviewChromeState(
    val name: String = "",
    val path: String = "",
    val typeText: String = "",
    val accessText: String = "",
    val stateText: String = "",
    val isSearchPanelVisible: Boolean = false,
    val query: String = "",
    val replacement: String = "",
    val isIgnoreCaseEnabled: Boolean = false,
    val isRegexEnabled: Boolean = false,
    val searchStatus: String = "",
    val canSearchText: Boolean = false,
    val canEditSearchText: Boolean = false,
    val markdownToggle: LocalFilePreviewActionState = LocalFilePreviewActionState(),
    val searchToggle: LocalFilePreviewActionState = LocalFilePreviewActionState(),
    val convert: LocalFilePreviewActionState = LocalFilePreviewActionState(),
    val saveAs: LocalFilePreviewActionState = LocalFilePreviewActionState(),
    val extract: LocalFilePreviewActionState = LocalFilePreviewActionState(),
    val edit: LocalFilePreviewActionState = LocalFilePreviewActionState(),
    val undo: LocalFilePreviewActionState = LocalFilePreviewActionState(),
    val redo: LocalFilePreviewActionState = LocalFilePreviewActionState(),
    val cancel: LocalFilePreviewActionState = LocalFilePreviewActionState(),
    val save: LocalFilePreviewActionState = LocalFilePreviewActionState(),
    val showImagePreview: Boolean = false,
    val imagePreviewUri: Uri = Uri.EMPTY,
    val showArchivePreview: Boolean = false,
    val archivePreviewText: String = "",
    val showMarkdownPreview: Boolean = false,
    val markdownPreviewText: String = ""
)

data class LocalFilePreviewActionState(
    val visible: Boolean = false,
    val enabled: Boolean = false,
    val text: String = ""
)

class LocalFilePreviewChromeActions(
    val onToggleMarkdown: () -> Unit = {},
    val onToggleSearch: () -> Unit = {},
    val onConvert: () -> Unit = {},
    val onSaveAs: () -> Unit = {},
    val onExtract: () -> Unit = {},
    val onFindPrevious: () -> Unit = {},
    val onFindNext: () -> Unit = {},
    val onRegexHelp: () -> Unit = {},
    val onReplaceCurrent: () -> Unit = {},
    val onReplaceAll: () -> Unit = {},
    val onUndo: () -> Unit = {},
    val onRedo: () -> Unit = {},
    val onEdit: () -> Unit = {},
    val onCancel: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onArchiveClick: () -> Unit = {},
    val onSearchQueryDone: () -> Boolean = { false },
    val onReplaceTextDone: () -> Boolean = { false },
    val onSearchQueryChange: (String) -> Unit = {},
    val onReplacementChange: (String) -> Unit = {},
    val onIgnoreCaseChange: (Boolean) -> Unit = {},
    val onRegexChange: (Boolean) -> Unit = {}
)