package com.Sunset.REN.GitHub.ui.filemanager

class LocalFilePreviewHeaderChromeRenderer(
    private val updateState: (LocalFilePreviewChromeState.() -> LocalFilePreviewChromeState) -> Unit
) {
    fun renderHeader(
        name: String,
        path: String,
        typeText: String,
        accessText: String,
        loadingText: String
    ) {
        updateState {
            copy(
                name = name,
                path = path,
                typeText = typeText,
                accessText = accessText,
                stateText = loadingText
            )
        }
    }
}