package com.Sunset.REN.GitHub.ui.filemanager

import android.net.Uri
import androidx.fragment.app.Fragment
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.filemanager.ApkPreview
import com.Sunset.REN.GitHub.domain.filemanager.ArchivePreview
import com.Sunset.REN.GitHub.domain.filemanager.FileSizeFormatter
import com.Sunset.REN.GitHub.ui.editor.TextEditorAdapter
import com.Sunset.REN.GitHub.ui.filemanager.preview.ApkPreviewTextFormatter
import com.Sunset.REN.GitHub.ui.filemanager.preview.ArchivePreviewTextFormatter

class LocalFileSpecializedPreviewRenderer(
    private val fragment: Fragment,
    private val previewView: LocalFileSpecializedPreviewView,
    private val editorAdapter: TextEditorAdapter,
    private val editorRootViewProvider: () -> android.view.View?,
    private val isLoaded: () -> Boolean,
    private val isEditableMode: () -> Boolean,
    private val isMarkdownPreviewVisible: () -> Boolean,
    private val canPreviewAsImage: () -> Boolean,
    private val canPreviewAsApk: () -> Boolean,
    private val canPreviewAsZipArchive: () -> Boolean,
    private val canPreviewAsMarkdown: () -> Boolean
) {
    fun renderImagePreview(
        sourceUri: Uri,
        sizeBytes: Long,
        onMissingSource: () -> Unit,
        onRenderActionState: (String) -> Unit
    ) {
        if (sourceUri == Uri.EMPTY) {
            onMissingSource()
            return
        }
        editorAdapter.setReadOnly(true)
        previewView.showImagePreviewOnly()
        editorRootViewProvider()?.visibility = android.view.View.GONE
        val readyText = fragment.getString(
            R.string.local_file_preview_image_ready,
            FileSizeFormatter.format(sizeBytes.coerceAtLeast(0L))
        )
        previewView.showImagePreview(sourceUri)
        previewView.stateText = readyText
        onRenderActionState("")
    }

    fun prepareSpecializedPreviewLoading() {
        editorAdapter.setText("")
        editorAdapter.setReadOnly(true)
        previewView.hideSpecializedPreviewLoading()
        editorRootViewProvider()?.visibility = android.view.View.GONE
    }

    fun renderLoadedArchive(preview: ArchivePreview, fallbackSizeBytes: Long) {
        previewView.showArchiveText(ArchivePreviewTextFormatter.format(fragment.requireContext(), preview, fallbackSizeBytes))
        previewView.stateText = fragment.getString(
            R.string.local_file_preview_archive_ready,
            preview.entryCount,
            FileSizeFormatter.format(fallbackSizeBytes.coerceAtLeast(0L))
        )
    }

    fun renderLoadedApk(preview: ApkPreview, fallbackSizeBytes: Long) {
        previewView.showArchiveText(ApkPreviewTextFormatter.format(fragment.requireContext(), preview, fallbackSizeBytes))
        previewView.stateText = fragment.getString(
            R.string.local_file_preview_apk_ready,
            preview.entryCount,
            FileSizeFormatter.format(fallbackSizeBytes.coerceAtLeast(0L))
        )
    }

    fun renderMarkdownContent(content: String) {
        if (!canPreviewAsMarkdown()) return
        previewView.setMarkdownContent(content)
    }

    fun renderPreviewModeVisibility() {
        val showImagePreview = isLoaded() && canPreviewAsImage()
        val showArchivePreview = isLoaded() && (canPreviewAsApk() || canPreviewAsZipArchive())
        val showMarkdownPreview = isLoaded() &&
            !showImagePreview &&
            !showArchivePreview &&
            canPreviewAsMarkdown() &&
            isMarkdownPreviewVisible() &&
            !isEditableMode()
        previewView.applyPreviewVisibility(
            showImagePreview = showImagePreview,
            showArchivePreview = showArchivePreview,
            showMarkdownPreview = showMarkdownPreview,
            editorRootView = editorRootViewProvider()
        )
    }
}