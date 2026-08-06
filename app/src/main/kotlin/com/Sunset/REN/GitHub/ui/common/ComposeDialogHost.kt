package com.Sunset.REN.GitHub.ui.common

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubTheme

/**
 * Lightweight bridge for showing Compose Material3 dialogs from legacy View/XML fragments.
 * New Compose-first screens should prefer state-driven dialog hosts directly in their Compose tree.
 */
fun showComposeDialog(
    context: Context,
    content: @Composable (dismiss: () -> Unit) -> Unit
): Dialog {
    val dialog = Dialog(context).apply {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }
    val composeView = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        setContent {
            SunsetGitHubTheme {
                content { dialog.dismiss() }
            }
        }
    }
    dialog.setContentView(composeView)
    dialog.show()
    return dialog
}
