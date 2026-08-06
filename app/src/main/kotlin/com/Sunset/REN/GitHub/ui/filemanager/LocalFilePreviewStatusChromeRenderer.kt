package com.Sunset.REN.GitHub.ui.filemanager

class LocalFilePreviewStatusChromeRenderer(
    private val getState: () -> LocalFilePreviewChromeState,
    private val updateState: (LocalFilePreviewChromeState.() -> LocalFilePreviewChromeState) -> Unit
) {
    var stateText: String
        get() = getState().stateText
        set(value) {
            updateState { copy(stateText = value) }
        }
}