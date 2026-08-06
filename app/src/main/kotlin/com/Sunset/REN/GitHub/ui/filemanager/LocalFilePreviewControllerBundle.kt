package com.Sunset.REN.GitHub.ui.filemanager

import android.content.Context
import android.net.Uri
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.Sunset.REN.GitHub.domain.filemanager.FileTextEncodingPolicy
import com.Sunset.REN.GitHub.ui.editor.TextEditorAdapter
import com.Sunset.REN.GitHub.ui.editor.TextSelection
import java.nio.charset.Charset

class LocalFilePreviewControllerBundle private constructor(
    val searchController: LocalFileSearchController,
    val specializedPreviewRenderer: LocalFileSpecializedPreviewRenderer,
    val actionRenderer: LocalFilePreviewActionRenderer,
    val editorModeController: LocalFileEditorModeController,
    val saveController: LocalFileSaveController
) {
    companion object {
        fun create(
            fragment: Fragment,
            previewView: LocalFilePreviewChromeView,
            editorAdapter: TextEditorAdapter,
            editorRootViewProvider: () -> View?,
            contextProvider: () -> Context,
            state: LocalFilePreviewControllerState,
            callbacks: LocalFilePreviewControllerCallbacks
        ): LocalFilePreviewControllerBundle {
            val searchController = LocalFileSearchController(
                searchView = previewView,
                editorAdapter = editorAdapter,
                contextProvider = contextProvider,
                isLoaded = state.isLoaded,
                isSpecializedPreview = state.isSpecializedPreview,
                isEditableMode = state.isEditableMode,
                canWrite = state.canWrite,
                isSaving = state.isSaving,
                onRenderActionState = callbacks.renderActionState,
                onRestoreSelection = callbacks.restoreSelection
            )
            val specializedPreviewRenderer = LocalFileSpecializedPreviewRenderer(
                fragment = fragment,
                previewView = previewView,
                editorAdapter = editorAdapter,
                editorRootViewProvider = editorRootViewProvider,
                isLoaded = state.isLoaded,
                isEditableMode = state.isEditableMode,
                isMarkdownPreviewVisible = state.isMarkdownPreviewVisible,
                canPreviewAsImage = state.canPreviewAsImage,
                canPreviewAsApk = state.canPreviewAsApk,
                canPreviewAsZipArchive = state.canPreviewAsZipArchive,
                canPreviewAsMarkdown = state.canPreviewAsMarkdown
            )
            val editorModeController = LocalFileEditorModeController(
                statusView = previewView,
                editorAdapter = editorAdapter,
                contextProvider = contextProvider,
                canWrite = state.canWrite,
                isSaving = state.isSaving,
                isEditableMode = state.isEditableMode,
                setEditableMode = state.setEditableMode,
                setMarkdownPreviewVisible = state.setMarkdownPreviewVisible,
                openMode = state.openMode,
                setOpenMode = state.setOpenMode,
                modeEdit = callbacks.modeEdit,
                modePreview = callbacks.modePreview,
                loadedContent = state.loadedContent,
                shouldShowMarkdownPreviewByDefault = callbacks.shouldShowMarkdownPreviewByDefault,
                setEditorTextSilently = callbacks.setEditorTextSilently,
                renderMarkdownContent = callbacks.renderMarkdownContent,
                renderPreviewModeVisibility = callbacks.renderPreviewModeVisibility,
                buildReadyStateText = callbacks.buildReadyStateText,
                renderActionState = callbacks.renderActionState
            )
            val saveController = LocalFileSaveController(
                statusView = previewView,
                editorAdapter = editorAdapter,
                contextProvider = contextProvider,
                sourceUri = state.sourceUri,
                canWrite = state.canWrite,
                isSaving = state.isSaving,
                setSaving = state.setSaving,
                loadedContent = state.loadedContent,
                setLoadedContent = state.setLoadedContent,
                loadedCharset = state.loadedCharset,
                loadedHadBom = state.loadedHadBom,
                loadedLineEnding = state.loadedLineEnding,
                loadedLastModifiedMillis = state.loadedLastModifiedMillis,
                setLoadedLastModifiedMillis = state.setLoadedLastModifiedMillis,
                setSizeBytes = state.setSizeBytes,
                setEditableMode = state.setEditableMode,
                setMarkdownPreviewVisible = state.setMarkdownPreviewVisible,
                onCancelUnchanged = { editorModeController.cancelEditMode() },
                lastModifiedMillis = callbacks.lastModifiedMillis,
                saveText = callbacks.saveText,
                restoreSelection = callbacks.restoreSelection,
                renderMarkdownContent = callbacks.renderMarkdownContent,
                shouldShowMarkdownPreviewByDefault = callbacks.shouldShowMarkdownPreviewByDefault,
                renderPreviewModeVisibility = callbacks.renderPreviewModeVisibility,
                renderActionState = callbacks.renderActionState
            )
            val actionRenderer = LocalFilePreviewActionRenderer(
                actionView = previewView,
                editorAdapter = editorAdapter,
                contextProvider = contextProvider,
                renderPreviewModeVisibility = callbacks.renderPreviewModeVisibility,
                backCallbackProvider = callbacks.backCallbackProvider,
                hasUnsavedChanges = callbacks.hasUnsavedChanges,
                loadedContent = state.loadedContent,
                isLoaded = state.isLoaded,
                isEditableMode = state.isEditableMode,
                isSaving = state.isSaving,
                canWrite = state.canWrite,
                isMarkdownPreviewVisible = state.isMarkdownPreviewVisible,
                canPreviewAsMarkdown = state.canPreviewAsMarkdown,
                isSpecializedPreview = state.isSpecializedPreview,
                canPreviewAsImage = state.canPreviewAsImage,
                canPreviewAsApk = state.canPreviewAsApk,
                canPreviewAsZipArchive = state.canPreviewAsZipArchive,
                canUseUndoRedo = { editorModeController.canUseUndoRedo() }
            )
            return LocalFilePreviewControllerBundle(
                searchController = searchController,
                specializedPreviewRenderer = specializedPreviewRenderer,
                actionRenderer = actionRenderer,
                editorModeController = editorModeController,
                saveController = saveController
            )
        }
    }
}

class LocalFilePreviewControllerState(
    private val session: LocalFilePreviewSession,
    val isSpecializedPreview: () -> Boolean,
    val canPreviewAsImage: () -> Boolean,
    val canPreviewAsApk: () -> Boolean,
    val canPreviewAsZipArchive: () -> Boolean,
    val canPreviewAsMarkdown: () -> Boolean
) {
    val sourceUri: () -> Uri = { session.sourceUri }
    val isLoaded: () -> Boolean = { session.isLoaded }
    val isEditableMode: () -> Boolean = { session.isEditableMode }
    val setEditableMode: (Boolean) -> Unit = { value -> session.isEditableMode = value }
    val canWrite: () -> Boolean = { session.canWrite }
    val isSaving: () -> Boolean = { session.isSaving }
    val setSaving: (Boolean) -> Unit = { value -> session.isSaving = value }
    val openMode: () -> String = { session.openMode }
    val setOpenMode: (String) -> Unit = { value -> session.openMode = value }
    val loadedContent: () -> String = { session.loadedContent }
    val setLoadedContent: (String) -> Unit = { value -> session.loadedContent = value }
    val loadedCharset: () -> Charset = { session.loadedCharset }
    val loadedHadBom: () -> Boolean = { session.loadedHadBom }
    val loadedLineEnding: () -> FileTextEncodingPolicy.LineEnding = { session.loadedLineEnding }
    val loadedLastModifiedMillis: () -> Long? = { session.loadedLastModifiedMillis }
    val setLoadedLastModifiedMillis: (Long?) -> Unit = { value -> session.loadedLastModifiedMillis = value }
    val setSizeBytes: (Long) -> Unit = { value -> session.sizeBytes = value }
    val isMarkdownPreviewVisible: () -> Boolean = { session.isMarkdownPreviewVisible }
    val setMarkdownPreviewVisible: (Boolean) -> Unit = { value -> session.isMarkdownPreviewVisible = value }
}

data class LocalFilePreviewControllerCallbacks(
    val modeEdit: String,
    val modePreview: String,
    val restoreSelection: (TextSelection, String) -> Unit,
    val setEditorTextSilently: (String) -> Unit,
    val renderMarkdownContent: (String) -> Unit,
    val renderPreviewModeVisibility: () -> Unit,
    val renderActionState: (String) -> Unit,
    val buildReadyStateText: (String) -> String,
    val shouldShowMarkdownPreviewByDefault: () -> Boolean,
    val hasUnsavedChanges: () -> Boolean,
    val backCallbackProvider: () -> OnBackPressedCallback?,
    val lastModifiedMillis: (Uri) -> Long?,
    val saveText: (
        sourceUri: Uri,
        content: String,
        charset: Charset,
        preserveBom: Boolean,
        lineEnding: FileTextEncodingPolicy.LineEnding,
        expectedLastModifiedMillis: Long?
    ) -> Unit
)
