package com.Sunset.REN.GitHub.ui.auth.token

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubTheme
import com.Sunset.REN.GitHub.ui.compose.screens.auth.TokenGuideScreen
import com.Sunset.REN.GitHub.util.BrowserLauncher

class TokenGuideFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SunsetGitHubTheme {
                    TokenGuideScreen(
                        onOpenBrowserClick = ::openTokenSettings,
                        onTokenAcquiredClick = { findNavController().navigate(R.id.navigation_token_permission_review) }
                    )
                }
            }
        }
    }

    private fun openTokenSettings() {
        if (!BrowserLauncher.open(requireContext(), GITHUB_TOKEN_SETTINGS_URL)) {
            Toast.makeText(requireContext(), R.string.auth_verification_no_browser, Toast.LENGTH_SHORT).show()
        }
    }

    companion object { private const val GITHUB_TOKEN_SETTINGS_URL = "https://github.com/settings/tokens" }
}