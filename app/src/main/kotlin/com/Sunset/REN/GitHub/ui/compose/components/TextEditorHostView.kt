package com.Sunset.REN.GitHub.ui.compose.components

import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.Sunset.REN.GitHub.ui.editor.ReleasableTextEditor
import com.Sunset.REN.GitHub.ui.editor.TextEditorHost

/**
 * Compose bridge for the app-owned text editor engine abstraction.
 *
 * The editor engine is still a View-backed component because both the Sora editor and
 * the EditText fallback are Android Views. This composable keeps that interop in one
 * place so Compose screens depend on [TextEditorHost] instead of wiring raw AndroidView
 * blocks themselves.
 */
@Composable
fun TextEditorHostView(
    host: TextEditorHost,
    modifier: Modifier = Modifier,
    releaseOnDispose: Boolean = false
) {
    AndroidView(
        factory = {
            host.rootView.detachFromParentIfNeeded()
            host.rootView
        },
        modifier = modifier,
        update = { view -> view.detachFromParentIfNeeded() }
    )
    if (releaseOnDispose) {
        DisposableEffect(host) {
            onDispose {
                (host.adapter as? ReleasableTextEditor)?.release()
            }
        }
    }
}

private fun View.detachFromParentIfNeeded() {
    (parent as? ViewGroup)?.removeView(this)
}
