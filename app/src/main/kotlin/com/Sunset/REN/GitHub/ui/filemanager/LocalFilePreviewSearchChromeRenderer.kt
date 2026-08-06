package com.Sunset.REN.GitHub.ui.filemanager

class LocalFilePreviewSearchChromeRenderer(
    private val getState: () -> LocalFilePreviewChromeState,
    private val updateState: (LocalFilePreviewChromeState.() -> LocalFilePreviewChromeState) -> Unit,
    private val requestSearchFocus: () -> Unit
) {
    private var statusRefresh: (() -> Unit)? = null

    var isSearchPanelVisible: Boolean
        get() = getState().isSearchPanelVisible
        set(value) {
            updateState { copy(isSearchPanelVisible = value) }
        }

    var searchStatus: String
        get() = getState().searchStatus
        set(value) {
            updateState { copy(searchStatus = value) }
        }

    val query: String
        get() = getState().query

    val replacement: String
        get() = getState().replacement

    val isIgnoreCaseEnabled: Boolean
        get() = getState().isIgnoreCaseEnabled

    val isRegexEnabled: Boolean
        get() = getState().isRegexEnabled

    fun configureStatusRefresh(onRefresh: () -> Unit) {
        statusRefresh = onRefresh
    }

    fun focusSearchQuery() {
        requestSearchFocus()
    }

    fun onSearchQueryChange(value: String) {
        updateState { copy(query = value) }
        statusRefresh?.invoke()
    }

    fun onReplacementChange(value: String) {
        updateState { copy(replacement = value) }
    }

    fun onIgnoreCaseChange(value: Boolean) {
        updateState { copy(isIgnoreCaseEnabled = value) }
        statusRefresh?.invoke()
    }

    fun onRegexChange(value: Boolean) {
        updateState { copy(isRegexEnabled = value) }
        statusRefresh?.invoke()
    }
}