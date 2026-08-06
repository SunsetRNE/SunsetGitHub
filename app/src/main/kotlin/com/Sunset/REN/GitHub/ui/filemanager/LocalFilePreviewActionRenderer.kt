package com.Sunset.REN.GitHub.ui.filemanager

import android.content.Context
import androidx.activity.OnBackPressedCallback
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.filemanager.TextCursorPositionPolicy
import com.Sunset.REN.GitHub.ui.editor.TextEditorAdapter

class LocalFilePreviewActionRenderer(
    private val actionView: LocalFilePreviewActionView,
    private val editorAdapter: TextEditorAdapter,
    private val contextProvider: () -> Context,
    private val renderPreviewModeVisibility: () -> Unit,
    private val backCallbackProvider: () -> OnBackPressedCallback?,
    private val hasUnsavedChanges: () -> Boolean,
    private val loadedContent: () -> String,
    private val isLoaded: () -> Boolean,
    private val isEditableMode: () -> Boolean,
    private val isSaving: () -> Boolean,
    private val canWrite: () -> Boolean,
    private val isMarkdownPreviewVisible: () -> Boolean,
    private val canPreviewAsMarkdown: () -> Boolean,
    private val isSpecializedPreview: () -> Boolean,
    private val canPreviewAsImage: () -> Boolean,
    private val canPreviewAsApk: () -> Boolean,
    private val canPreviewAsZipArchive: () -> Boolean,
    private val canUseUndoRedo: () -> Boolean
) {
    fun render(content: String) {
        renderPreviewModeVisibility()
        val hasChanges = content != loadedContent()
        backCallbackProvider()?.isEnabled = hasUnsavedChanges()

        val canShowMarkdownToggle = canPreviewAsMarkdown() && !isEditableMode()
        actionView.setMarkdownToggle(
            visible = canShowMarkdownToggle,
            enabled = isLoaded() && !isSaving(),
            text = context().getString(
                if (isMarkdownPreviewVisible()) R.string.local_file_preview_markdown_source else R.string.local_file_preview_markdown_preview
            )
        )

        val canSearchText = isLoaded() && !isSpecializedPreview()
        val isSearchVisible = actionView.isSearchPanelVisible
        actionView.setSearchToggle(
            visible = canSearchText,
            enabled = canSearchText && !isSaving(),
            text = context().getString(
                if (isSearchVisible) R.string.local_file_preview_search_close else R.string.local_file_preview_search
            )
        )
        actionView.setSearchPanelVisibleWhenAvailable(canSearchText)
        val canEditSearchText = canSearchText && isEditableMode() && canWrite() && !isSaving()
        actionView.setSearchActionsEnabled(
            canSearchText = canSearchText && !isSaving(),
            canEditText = canEditSearchText
        )

        val canConvertText = canSearchText && canWrite()
        actionView.setConvertAction(canConvertText, canConvertText && !isSaving())

        val canSaveAsText = canSearchText
        actionView.setSaveAsAction(canSaveAsText, canSaveAsText && !isSaving())

        val canExtractArchive = isLoaded() && canPreviewAsZipArchive() && !isEditableMode()
        actionView.setExtractAction(canExtractArchive, canExtractArchive && !isSaving())

        val canEditText = !isEditableMode() && !canPreviewAsImage() && !canPreviewAsApk() && !canPreviewAsZipArchive()
        actionView.setEditAction(canEditText, isLoaded() && canWrite() && !isSaving() && canEditText)

        val canUseUndoRedoNow = isEditableMode() && canWrite() && canUseUndoRedo()
        actionView.setUndoAction(canUseUndoRedoNow, canUseUndoRedoNow && !isSaving())
        actionView.setRedoAction(canUseUndoRedoNow, canUseUndoRedoNow && !isSaving())
        actionView.setCancelAction(isEditableMode(), !isSaving())
        actionView.setSaveAction(isEditableMode(), hasChanges && !isSaving())

        if (!isEditableMode() && isLoaded() && !isSpecializedPreview()) {
            actionView.stateText = context().getString(R.string.repository_file_cursor_status, 1, 1)
        } else if (isEditableMode() && !isSaving()) {
            actionView.stateText = buildEditingStateText(content, hasChanges)
        } else if (isSaving()) {
            actionView.stateText = context().getString(R.string.local_file_preview_saving)
        }
    }

    // Android View visibility details live in LocalFilePreviewActionView implementations.

    private fun buildEditingStateText(content: String, hasChanges: Boolean): String {
        val prefix = context().getString(if (hasChanges) R.string.local_file_preview_modified else R.string.local_file_preview_editing)
        val selection = editorAdapter.getSelection()
        val position = TextCursorPositionPolicy.calculate(
            content = content,
            selectionStart = selection.start,
            selectionEnd = selection.end
        )
        return if (position.selectionLength > 0) {
            context().getString(
                R.string.local_file_preview_editor_position_with_selection,
                prefix,
                position.line,
                position.column,
                position.selectionLength
            )
        } else {
            context().getString(
                R.string.local_file_preview_editor_position,
                prefix,
                position.line,
                position.column
            )
        }
    }

    private fun context(): Context = contextProvider()
}