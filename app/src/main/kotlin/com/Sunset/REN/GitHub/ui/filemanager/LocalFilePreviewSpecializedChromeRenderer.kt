package com.Sunset.REN.GitHub.ui.filemanager

import android.net.Uri
import android.view.View
import com.Sunset.REN.GitHub.ui.repo.setRepositoryMarkdown

class LocalFilePreviewSpecializedChromeRenderer(
    private val refs: LocalFilePreviewViewRefs,
    private val updateState: (LocalFilePreviewChromeState.() -> LocalFilePreviewChromeState) -> Unit
) {
    fun hideSurfaces() {
        hideLegacySpecializedSurfaces()
        updateState { copy(showImagePreview = false, showArchivePreview = false, showMarkdownPreview = false) }
    }

    fun showImagePreview(sourceUri: Uri) {
        hideLegacySpecializedSurfaces()
        updateState {
            copy(
                showImagePreview = true,
                imagePreviewUri = sourceUri,
                showArchivePreview = false,
                showMarkdownPreview = false
            )
        }
    }

    fun showImagePreviewOnly() {
        hideLegacySpecializedSurfaces()
        updateState { copy(showImagePreview = true, showArchivePreview = false, showMarkdownPreview = false) }
    }

    fun hideLoading() {
        hideLegacySpecializedSurfaces()
        updateState { copy(showImagePreview = false, showArchivePreview = false, showMarkdownPreview = false) }
    }

    fun showArchiveText(text: CharSequence) {
        refs.archiveText.text = text
        refs.archiveScroll.visibility = View.GONE
        updateState { copy(archivePreviewText = text.toString(), showArchivePreview = true, showMarkdownPreview = false) }
    }

    fun setMarkdownContent(content: String) {
        refs.markdownText.setRepositoryMarkdown(
            markdown = content,
            baseHtmlUrl = null,
            imageAccessToken = ""
        )
        refs.markdownScroll.visibility = View.GONE
        updateState { copy(markdownPreviewText = content) }
    }

    fun applyPreviewVisibility(
        showImagePreview: Boolean,
        showArchivePreview: Boolean,
        showMarkdownPreview: Boolean,
        editorRootView: View?
    ) {
        hideLegacySpecializedSurfaces()
        updateState {
            copy(
                showImagePreview = showImagePreview,
                showArchivePreview = showArchivePreview,
                showMarkdownPreview = showMarkdownPreview
            )
        }
        editorRootView?.visibility = if (showImagePreview || showArchivePreview || showMarkdownPreview) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }

    private fun hideLegacySpecializedSurfaces() {
        refs.imageContainer.visibility = View.GONE
        refs.archiveScroll.visibility = View.GONE
        refs.markdownScroll.visibility = View.GONE
    }
}