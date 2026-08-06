package com.Sunset.REN.GitHub.ui.auth.token

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubTheme
import com.Sunset.REN.GitHub.ui.compose.screens.auth.TokenLoginChoiceScreen

class TokenLoginChoiceFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SunsetGitHubTheme {
                    TokenLoginChoiceScreen(
                        onHaveTokenClick = { findNavController().navigate(R.id.navigation_token_permission_review) },
                        onNeedTokenGuideClick = { findNavController().navigate(R.id.navigation_token_guide) }
                    )
                }
            }
        }
    }
}