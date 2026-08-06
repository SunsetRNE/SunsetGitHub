package com.Sunset.REN.GitHub.ui.filemanager

import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide

interface LocalFilePreviewStatusView {
    var stateText: String
}

interface LocalFilePreviewHeaderView : LocalFilePreviewStatusView {
    fun renderHeader(name: String, path: String, typeText: String, accessText: String, loadingText: String)
}

interface LocalFileSpecializedPreviewView : LocalFilePreviewStatusView {
    val imageView: ImageView

    fun showImagePreview(sourceUri: Uri) {
        showImagePreviewOnly()
        Glide.with(imageView)
            .load(sourceUri)
            .fitCenter()
            .into(imageView)
    }

    fun showImagePreviewOnly()
    fun hideSpecializedPreviewLoading()
    fun showArchiveText(text: CharSequence)
    fun setMarkdownContent(content: String)
    fun applyPreviewVisibility(
        showImagePreview: Boolean,
        showArchivePreview: Boolean,
        showMarkdownPreview: Boolean,
        editorRootView: View?
    )
}

interface LocalFileSearchView {
    var isSearchPanelVisible: Boolean
    var searchStatus: String

    val query: String
    val replacement: String
    val isIgnoreCaseEnabled: Boolean
    val isRegexEnabled: Boolean

    fun configureStatusRefresh(onRefresh: () -> Unit)
    fun focusSearchQuery()
}

interface LocalFilePreviewChromeView :
    LocalFilePreviewActionView,
    LocalFileSearchView,
    LocalFileSpecializedPreviewView,
    LocalFilePreviewHeaderView

interface LocalFileEditorContainerView {
    fun editorContainer(): ViewGroup
}

interface LocalFilePreviewActionView : LocalFilePreviewStatusView {
    var isSearchPanelVisible: Boolean

    fun bindActions(
        onToggleMarkdown: () -> Unit,
        onToggleSearch: () -> Unit,
        onConvert: () -> Unit,
        onSaveAs: () -> Unit,
        onExtract: () -> Unit,
        onSearchQueryDone: () -> Boolean,
        onReplaceTextDone: () -> Boolean,
        onRegexHelp: () -> Unit,
        onFindPrevious: () -> Unit,
        onFindNext: () -> Unit,
        onReplaceCurrent: () -> Unit,
        onReplaceAll: () -> Unit,
        onUndo: () -> Unit,
        onRedo: () -> Unit,
        onEdit: () -> Unit,
        onCancel: () -> Unit,
        onSave: () -> Unit,
        onArchiveClick: () -> Unit
    )

    fun setMarkdownToggle(visible: Boolean, enabled: Boolean, text: String)
    fun setSearchToggle(visible: Boolean, enabled: Boolean, text: String)
    fun setSearchPanelVisibleWhenAvailable(available: Boolean)
    fun setSearchActionsEnabled(canSearchText: Boolean, canEditText: Boolean)
    fun setConvertAction(visible: Boolean, enabled: Boolean)
    fun setSaveAsAction(visible: Boolean, enabled: Boolean)
    fun setExtractAction(visible: Boolean, enabled: Boolean)
    fun setEditAction(visible: Boolean, enabled: Boolean)
    fun setUndoAction(visible: Boolean, enabled: Boolean)
    fun setRedoAction(visible: Boolean, enabled: Boolean)
    fun setCancelAction(visible: Boolean, enabled: Boolean)
    fun setSaveAction(visible: Boolean, enabled: Boolean)
}