package com.Sunset.REN.GitHub.ui.filemanager

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class LocalFilePreviewChromeHolder {
    var state by mutableStateOf(LocalFilePreviewChromeState())
    var searchFocusRequestCount by mutableIntStateOf(0)
        private set

    fun requestSearchFocus() {
        searchFocusRequestCount += 1
    }
}