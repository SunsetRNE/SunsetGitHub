package com.Sunset.REN.GitHub.ui.filemanager

import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView

class ComposeLocalFilePreviewActionView(
    private val refs: LocalFilePreviewViewRefs,
    private val getState: () -> LocalFilePreviewChromeState,
    private val setState: (LocalFilePreviewChromeState) -> Unit,
    private val requestSearchFocus: () -> Unit
) : LocalFilePreviewChromeView,
    LocalFileEditorContainerView {
    private var actions = LocalFilePreviewChromeActions()
    private val statusRenderer = LocalFilePreviewStatusChromeRenderer(getState, ::updateState)
    private val searchRenderer = LocalFilePreviewSearchChromeRenderer(getState, ::updateState, requestSearchFocus)
    private val headerRenderer = LocalFilePreviewHeaderChromeRenderer(::updateState)
    private val actionRenderer = LocalFilePreviewActionChromeRenderer(getState, ::updateState)
    private val specializedRenderer = LocalFilePreviewSpecializedChromeRenderer(refs, ::updateState)

    override var stateText: String
        get() = statusRenderer.stateText
        set(value) {
            statusRenderer.stateText = value
        }

    override var isSearchPanelVisible: Boolean
        get() = searchRenderer.isSearchPanelVisible
        set(value) {
            searchRenderer.isSearchPanelVisible = value
        }

    override var searchStatus: String
        get() = searchRenderer.searchStatus
        set(value) {
            searchRenderer.searchStatus = value
        }

    override val query: String
        get() = searchRenderer.query

    override val replacement: String
        get() = searchRenderer.replacement

    override val isIgnoreCaseEnabled: Boolean
        get() = searchRenderer.isIgnoreCaseEnabled

    override val isRegexEnabled: Boolean
        get() = searchRenderer.isRegexEnabled

    override val imageView: ImageView
        get() = refs.image

    override fun editorContainer(): ViewGroup = refs.editorContainer

    override fun renderHeader(name: String, path: String, typeText: String, accessText: String, loadingText: String) {
        headerRenderer.renderHeader(
            name = name,
            path = path,
            typeText = typeText,
            accessText = accessText,
            loadingText = loadingText
        )
    }

    override fun bindActions(
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
    ) {
        actions = LocalFilePreviewChromeActions(
            onToggleMarkdown = onToggleMarkdown,
            onToggleSearch = onToggleSearch,
            onConvert = onConvert,
            onSaveAs = onSaveAs,
            onExtract = onExtract,
            onFindPrevious = onFindPrevious,
            onFindNext = onFindNext,
            onRegexHelp = onRegexHelp,
            onReplaceCurrent = onReplaceCurrent,
            onReplaceAll = onReplaceAll,
            onUndo = onUndo,
            onRedo = onRedo,
            onEdit = onEdit,
            onCancel = onCancel,
            onSave = onSave,
            onArchiveClick = onArchiveClick,
            onSearchQueryDone = onSearchQueryDone,
            onReplaceTextDone = onReplaceTextDone,
            onSearchQueryChange = { value -> searchRenderer.onSearchQueryChange(value) },
            onReplacementChange = { value -> searchRenderer.onReplacementChange(value) },
            onIgnoreCaseChange = { value -> searchRenderer.onIgnoreCaseChange(value) },
            onRegexChange = { value -> searchRenderer.onRegexChange(value) }
        )
    }

    fun currentActions(): LocalFilePreviewChromeActions = actions

    fun hideLegacyChrome() {
        refs.name.parentView()?.visibility = View.GONE
        refs.searchContainer.visibility = View.GONE
        refs.markdownToggle.parentView()?.visibility = View.GONE
        refs.root.requestLayout()
    }

    fun hideSpecializedPreviewSurfaces() {
        specializedRenderer.hideSurfaces()
    }

    override fun showImagePreview(sourceUri: Uri) {
        specializedRenderer.showImagePreview(sourceUri)
    }

    override fun showImagePreviewOnly() {
        specializedRenderer.showImagePreviewOnly()
    }

    override fun hideSpecializedPreviewLoading() {
        specializedRenderer.hideLoading()
    }

    override fun showArchiveText(text: CharSequence) {
        specializedRenderer.showArchiveText(text)
    }

    override fun setMarkdownContent(content: String) {
        specializedRenderer.setMarkdownContent(content)
    }

    override fun applyPreviewVisibility(
        showImagePreview: Boolean,
        showArchivePreview: Boolean,
        showMarkdownPreview: Boolean,
        editorRootView: View?
    ) {
        specializedRenderer.applyPreviewVisibility(
            showImagePreview = showImagePreview,
            showArchivePreview = showArchivePreview,
            showMarkdownPreview = showMarkdownPreview,
            editorRootView = editorRootView
        )
    }

    override fun configureStatusRefresh(onRefresh: () -> Unit) {
        searchRenderer.configureStatusRefresh(onRefresh)
    }

    override fun focusSearchQuery() {
        searchRenderer.focusSearchQuery()
    }

    override fun setMarkdownToggle(visible: Boolean, enabled: Boolean, text: String) {
        actionRenderer.setMarkdownToggle(visible, enabled, text)
    }

    override fun setSearchToggle(visible: Boolean, enabled: Boolean, text: String) {
        actionRenderer.setSearchToggle(visible, enabled, text)
    }

    override fun setSearchPanelVisibleWhenAvailable(available: Boolean) {
        actionRenderer.setSearchPanelVisibleWhenAvailable(available)
    }

    override fun setSearchActionsEnabled(canSearchText: Boolean, canEditText: Boolean) {
        actionRenderer.setSearchActionsEnabled(canSearchText, canEditText)
    }

    override fun setConvertAction(visible: Boolean, enabled: Boolean) {
        actionRenderer.setConvertAction(visible, enabled)
    }

    override fun setSaveAsAction(visible: Boolean, enabled: Boolean) {
        actionRenderer.setSaveAsAction(visible, enabled)
    }

    override fun setExtractAction(visible: Boolean, enabled: Boolean) {
        actionRenderer.setExtractAction(visible, enabled)
    }

    override fun setEditAction(visible: Boolean, enabled: Boolean) {
        actionRenderer.setEditAction(visible, enabled)
    }

    override fun setUndoAction(visible: Boolean, enabled: Boolean) {
        actionRenderer.setUndoAction(visible, enabled)
    }

    override fun setRedoAction(visible: Boolean, enabled: Boolean) {
        actionRenderer.setRedoAction(visible, enabled)
    }

    override fun setCancelAction(visible: Boolean, enabled: Boolean) {
        actionRenderer.setCancelAction(visible, enabled)
    }

    override fun setSaveAction(visible: Boolean, enabled: Boolean) {
        actionRenderer.setSaveAction(visible, enabled)
    }

    private fun updateState(transform: LocalFilePreviewChromeState.() -> LocalFilePreviewChromeState) {
        setState(getState().transform())
    }
}

private fun View.parentView(): View? = parent as? View
