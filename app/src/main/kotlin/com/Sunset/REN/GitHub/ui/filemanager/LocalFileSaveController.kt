package com.Sunset.REN.GitHub.ui.filemanager

import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.filemanager.FileContentWriteResult
import com.Sunset.REN.GitHub.domain.filemanager.FileSizeFormatter
import com.Sunset.REN.GitHub.domain.filemanager.FileTextEncodingPolicy
import com.Sunset.REN.GitHub.ui.common.CompactBlackDialog
import com.Sunset.REN.GitHub.ui.editor.TextEditorAdapter
import com.Sunset.REN.GitHub.ui.editor.TextSelection
import java.nio.charset.Charset

class LocalFileSaveController(
    private val statusView: LocalFilePreviewStatusView,
    private val editorAdapter: TextEditorAdapter,
    private val contextProvider: () -> Context,
    private val sourceUri: () -> Uri,
    private val canWrite: () -> Boolean,
    private val isSaving: () -> Boolean,
    private val setSaving: (Boolean) -> Unit,
    private val loadedContent: () -> String,
    private val setLoadedContent: (String) -> Unit,
    private val loadedCharset: () -> Charset,
    private val loadedHadBom: () -> Boolean,
    private val loadedLineEnding: () -> FileTextEncodingPolicy.LineEnding,
    private val loadedLastModifiedMillis: () -> Long?,
    private val setLoadedLastModifiedMillis: (Long?) -> Unit,
    private val setSizeBytes: (Long) -> Unit,
    private val setEditableMode: (Boolean) -> Unit,
    private val setMarkdownPreviewVisible: (Boolean) -> Unit,
    private val onCancelUnchanged: () -> Unit,
    private val lastModifiedMillis: (Uri) -> Long?,
    private val saveText: (
        sourceUri: Uri,
        content: String,
        charset: Charset,
        preserveBom: Boolean,
        lineEnding: FileTextEncodingPolicy.LineEnding,
        expectedLastModifiedMillis: Long?
    ) -> Unit,
    private val restoreSelection: (TextSelection, String) -> Unit,
    private val renderMarkdownContent: (String) -> Unit,
    private val shouldShowMarkdownPreviewByDefault: () -> Boolean,
    private val renderPreviewModeVisibility: () -> Unit,
    private val renderActionState: (String) -> Unit
) {
    private var pendingSaveSelection: TextSelection? = null

    fun saveContent() {
        if (!canWrite() || isSaving()) return
        val content = editorAdapter.getText()
        if (content == loadedContent()) {
            onCancelUnchanged()
            return
        }
        val selection = editorAdapter.getSelection()
        val loadedLastModified = loadedLastModifiedMillis()
        val currentLastModified = lastModifiedMillis(sourceUri())
        if (loadedLastModified != null && currentLastModified != null && currentLastModified != loadedLastModified) {
            showSaveConflictDialog(content, selection)
            return
        }
        performSaveContent(content, selection, expectedLastModifiedMillis = loadedLastModified)
    }

    fun handleSaveSucceeded(writeResult: FileContentWriteResult) {
        val content = editorAdapter.getText()
        val selection = pendingSaveSelection ?: editorAdapter.getSelection()
        pendingSaveSelection = null
        setSaving(false)
        setLoadedContent(content)
        setSizeBytes(writeResult.sizeBytes)
        setLoadedLastModifiedMillis(writeResult.lastModifiedMillis ?: lastModifiedMillis(sourceUri()))
        setEditableMode(false)
        editorAdapter.setReadOnly(true)
        restoreSelection(selection, content)
        renderMarkdownContent(content)
        setMarkdownPreviewVisible(shouldShowMarkdownPreviewByDefault())
        renderPreviewModeVisibility()
        statusView.stateText = context().getString(
            R.string.local_file_preview_saved,
            FileSizeFormatter.format(writeResult.sizeBytes)
        )
        renderActionState(content)
        toast(R.string.local_file_preview_saved_short)
    }

    fun handleSaveFailed(message: String) {
        val content = editorAdapter.getText()
        val selection = pendingSaveSelection ?: editorAdapter.getSelection()
        pendingSaveSelection = null
        setSaving(false)
        setEditableMode(true)
        editorAdapter.setReadOnly(false)
        restoreSelection(selection, content)
        editorAdapter.focus()
        statusView.stateText = message.ifBlank { context().getString(R.string.local_file_preview_save_failed_retry) }
        renderActionState(content)
        toast(R.string.local_file_preview_save_failed_retry)
    }

    private fun showSaveConflictDialog(content: String, selection: TextSelection) {
        CompactBlackDialog.show(
            context = context(),
            title = context().getString(R.string.local_file_preview_save_conflict_title),
            message = context().getString(R.string.local_file_preview_save_conflict_message),
            negativeText = context().getString(R.string.local_file_preview_save_conflict_keep_editing),
            positiveText = context().getString(R.string.local_file_preview_save_conflict_overwrite),
            onPositiveClick = {
                performSaveContent(content, selection, expectedLastModifiedMillis = null)
            }
        )
    }

    private fun performSaveContent(
        content: String,
        selection: TextSelection,
        expectedLastModifiedMillis: Long?
    ) {
        pendingSaveSelection = selection
        saveText(
            sourceUri(),
            content,
            loadedCharset(),
            loadedHadBom(),
            loadedLineEnding(),
            expectedLastModifiedMillis
        )
    }

    private fun toast(messageResId: Int) {
        Toast.makeText(context(), messageResId, Toast.LENGTH_SHORT).show()
    }

    private fun context(): Context = contextProvider()
}