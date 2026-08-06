package com.Sunset.REN.GitHub.ui.filemanager

import android.content.Context
import android.widget.Toast
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.ui.editor.TextEditorAdapter
import com.Sunset.REN.GitHub.ui.editor.TextSelection
import com.Sunset.REN.GitHub.ui.editor.UndoRedoCapableEditor

class LocalFileEditorModeController(
    private val statusView: LocalFilePreviewStatusView,
    private val editorAdapter: TextEditorAdapter,
    private val contextProvider: () -> Context,
    private val canWrite: () -> Boolean,
    private val isSaving: () -> Boolean,
    private val isEditableMode: () -> Boolean,
    private val setEditableMode: (Boolean) -> Unit,
    private val setMarkdownPreviewVisible: (Boolean) -> Unit,
    private val openMode: () -> String,
    private val setOpenMode: (String) -> Unit,
    private val modeEdit: String,
    private val modePreview: String,
    private val loadedContent: () -> String,
    private val shouldShowMarkdownPreviewByDefault: () -> Boolean,
    private val setEditorTextSilently: (String) -> Unit,
    private val renderMarkdownContent: (String) -> Unit,
    private val renderPreviewModeVisibility: () -> Unit,
    private val buildReadyStateText: (String) -> String,
    private val renderActionState: (String) -> Unit
) {
    fun enterEditMode() {
        if (!canWrite()) {
            if (openMode() == modeEdit) {
                setOpenMode(modePreview)
                statusView.stateText = context().getString(R.string.local_file_preview_edit_downgraded_read_only)
                toast(R.string.local_file_preview_edit_downgraded_read_only)
            } else {
                statusView.stateText = context().getString(R.string.local_file_preview_read_only)
                toast(R.string.local_file_preview_read_only)
            }
            renderActionState(editorAdapter.getText())
            return
        }
        setEditableMode(true)
        setMarkdownPreviewVisible(false)
        renderPreviewModeVisibility()
        editorAdapter.setReadOnly(false)
        editorAdapter.focus()
        renderActionState(editorAdapter.getText())
    }

    fun cancelEditMode() {
        val content = loadedContent()
        setEditableMode(false)
        editorAdapter.setReadOnly(true)
        setEditorTextSilently(content)
        renderMarkdownContent(content)
        setMarkdownPreviewVisible(shouldShowMarkdownPreviewByDefault())
        renderPreviewModeVisibility()
        statusView.stateText = buildReadyStateText(content)
        renderActionState(content)
    }

    fun restoreSelection(selection: TextSelection, content: String) {
        val start = selection.start.coerceIn(0, content.length)
        val end = selection.end.coerceIn(start, content.length)
        editorAdapter.setSelection(start, end)
        editorAdapter.scrollToSelectionEnd()
    }

    fun undoEdit() {
        if (!isEditableMode() || isSaving()) return
        val handled = undoRedoEditor()?.undo() == true
        if (!handled) {
            toast(R.string.local_file_preview_undo_unavailable)
        }
        renderActionState(editorAdapter.getText())
    }

    fun redoEdit() {
        if (!isEditableMode() || isSaving()) return
        val handled = undoRedoEditor()?.redo() == true
        if (!handled) {
            toast(R.string.local_file_preview_redo_unavailable)
        }
        renderActionState(editorAdapter.getText())
    }

    fun canUseUndoRedo(): Boolean = undoRedoEditor() != null

    private fun undoRedoEditor(): UndoRedoCapableEditor? {
        return editorAdapter as? UndoRedoCapableEditor
    }

    private fun toast(messageResId: Int) {
        Toast.makeText(context(), messageResId, Toast.LENGTH_SHORT).show()
    }

    private fun context(): Context = contextProvider()
}