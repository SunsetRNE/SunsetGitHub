package com.Sunset.REN.GitHub.ui.filemanager

class LocalFilePreviewActionChromeRenderer(
    private val getState: () -> LocalFilePreviewChromeState,
    private val updateState: (LocalFilePreviewChromeState.() -> LocalFilePreviewChromeState) -> Unit
) {
    fun setMarkdownToggle(visible: Boolean, enabled: Boolean, text: String) {
        updateState { copy(markdownToggle = LocalFilePreviewActionState(visible, enabled, text)) }
    }

    fun setSearchToggle(visible: Boolean, enabled: Boolean, text: String) {
        updateState { copy(searchToggle = LocalFilePreviewActionState(visible, enabled, text)) }
    }

    fun setSearchPanelVisibleWhenAvailable(available: Boolean) {
        val currentState = getState()
        updateState { copy(isSearchPanelVisible = available && currentState.isSearchPanelVisible) }
    }

    fun setSearchActionsEnabled(canSearchText: Boolean, canEditText: Boolean) {
        updateState {
            copy(
                canSearchText = canSearchText,
                canEditSearchText = canEditText,
                searchToggle = searchToggle.copy(enabled = canSearchText)
            )
        }
    }

    fun setConvertAction(visible: Boolean, enabled: Boolean) {
        updateState { copy(convert = convert.copy(visible = visible, enabled = enabled)) }
    }

    fun setSaveAsAction(visible: Boolean, enabled: Boolean) {
        updateState { copy(saveAs = saveAs.copy(visible = visible, enabled = enabled)) }
    }

    fun setExtractAction(visible: Boolean, enabled: Boolean) {
        updateState { copy(extract = extract.copy(visible = visible, enabled = enabled)) }
    }

    fun setEditAction(visible: Boolean, enabled: Boolean) {
        updateState { copy(edit = edit.copy(visible = visible, enabled = enabled)) }
    }

    fun setUndoAction(visible: Boolean, enabled: Boolean) {
        updateState { copy(undo = undo.copy(visible = visible, enabled = enabled)) }
    }

    fun setRedoAction(visible: Boolean, enabled: Boolean) {
        updateState { copy(redo = redo.copy(visible = visible, enabled = enabled)) }
    }

    fun setCancelAction(visible: Boolean, enabled: Boolean) {
        updateState { copy(cancel = cancel.copy(visible = visible, enabled = enabled)) }
    }

    fun setSaveAction(visible: Boolean, enabled: Boolean) {
        updateState { copy(save = save.copy(visible = visible, enabled = enabled)) }
    }
}