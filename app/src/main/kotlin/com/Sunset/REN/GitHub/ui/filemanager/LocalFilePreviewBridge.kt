package com.Sunset.REN.GitHub.ui.filemanager

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.Sunset.REN.GitHub.ui.editor.ReleasableTextEditor
import com.Sunset.REN.GitHub.ui.editor.TextEditorAdapter
import com.Sunset.REN.GitHub.ui.editor.TextEditorConfig
import com.Sunset.REN.GitHub.ui.editor.TextEditorFactory
import com.Sunset.REN.GitHub.ui.editor.TextEditorHost

/**
 * Owns the remaining legacy preview/editor View island embedded by the Compose preview screen.
 *
 * The Fragment still coordinates file loading, search, save, and specialized renderers, but it no
 * longer needs to know how refs, action view, and editor host are assembled.
 */
class LocalFilePreviewBridge private constructor(
    val rootView: View,
    val actionView: ComposeLocalFilePreviewActionView,
    val editorContainerView: LocalFileEditorContainerView,
    val editorHost: TextEditorHost
) {
    val editorAdapter: TextEditorAdapter = editorHost.adapter
    val editorRootView: View = editorHost.rootView

    fun dispose() {
        editorAdapter.setOnTextChangedListener(null)
        editorAdapter.setOnSelectionChangedListener(null)
        (editorAdapter as? ReleasableTextEditor)?.release()
        (editorRootView.parent as? ViewGroup)?.removeView(editorRootView)
    }

    companion object {
        fun create(
            context: Context,
            inflater: LayoutInflater,
            editorConfig: TextEditorConfig,
            getChromeState: () -> LocalFilePreviewChromeState,
            setChromeState: (LocalFilePreviewChromeState) -> Unit,
            requestSearchFocus: () -> Unit
        ): LocalFilePreviewBridge {
            val rootView = LocalFilePreviewViewFactory.create(context)
            val refs = LocalFilePreviewViewRefs(rootView)
            val actionView = ComposeLocalFilePreviewActionView(
                refs = refs,
                getState = getChromeState,
                setState = setChromeState,
                requestSearchFocus = requestSearchFocus
            )
            actionView.hideLegacyChrome()
            val editorContainerView: LocalFileEditorContainerView = actionView
            val editorHost = TextEditorFactory.create(
                inflater = inflater,
                parent = editorContainerView.editorContainer(),
                config = editorConfig
            )
            editorContainerView.editorContainer().addView(editorHost.rootView)
            editorHost.adapter.setReadOnly(true)
            return LocalFilePreviewBridge(
                rootView = rootView,
                actionView = actionView,
                editorContainerView = editorContainerView,
                editorHost = editorHost
            )
        }
    }
}
