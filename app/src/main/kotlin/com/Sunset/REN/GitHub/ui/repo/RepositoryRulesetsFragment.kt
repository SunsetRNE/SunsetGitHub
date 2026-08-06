package com.Sunset.REN.GitHub.ui.repo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubTheme
import com.Sunset.REN.GitHub.ui.compose.screens.repo.RepositoryRulesetsScreen

class RepositoryRulesetsFragment : Fragment() {
    private val viewModel: RepositoryRulesetsViewModel by viewModels()
    private var rulesetsState by mutableStateOf(RepositoryRulesetsUiState())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        rulesetsState = viewModel.state.value ?: RepositoryRulesetsUiState()
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SunsetGitHubTheme {
                    RepositoryRulesetsScreen(
                        state = rulesetsState,
                        onRefresh = viewModel::refresh
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.state.observe(viewLifecycleOwner) { rulesetsState = it }
        viewModel.prepare(arguments?.getString(ARG_OWNER).orEmpty(), arguments?.getString(ARG_REPO).orEmpty())
    }

    companion object {
        const val ARG_OWNER = "owner"
        const val ARG_REPO = "repo"
    }
}
