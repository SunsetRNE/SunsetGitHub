package com.Sunset.REN.GitHub.ui.settings

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
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.Sunset.REN.GitHub.BuildConfig
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubTheme
import com.Sunset.REN.GitHub.ui.compose.screens.settings.SettingsScreen
import com.Sunset.REN.GitHub.ui.repo.RepositorySection

class SettingsFragment : Fragment() {

    private lateinit var viewModel: SettingsViewModel

    private var floatingNavigationEnabled by mutableStateOf(false)
    private var soraEditorEnabled by mutableStateOf(false)
    private var uiDebugOverlayEnabled by mutableStateOf(false)
    private var repositorySectionOrder by mutableStateOf(emptyList<RepositorySection>())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this)[SettingsViewModel::class.java]
        syncStateFromViewModelSnapshot()

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SunsetGitHubTheme {
                    SettingsScreen(
                        floatingNavigationEnabled = floatingNavigationEnabled,
                        soraEditorEnabled = soraEditorEnabled,
                        uiDebugOverlayEnabled = uiDebugOverlayEnabled,
                        showUiDebugOverlaySetting = BuildConfig.DEBUG,
                        repositorySectionOrder = repositorySectionOrder,
                        onFloatingNavigationChange = viewModel::setFloatingNavigationEnabled,
                        onSoraEditorChange = viewModel::setSoraEditorEnabled,
                        onUiDebugOverlayChange = viewModel::setUiDebugOverlayEnabled,
                        onRepositorySectionOrderChange = viewModel::setDefaultSectionOrder,
                        onOpenAccountPage = { findNavController().navigate(R.id.account_fragment) },
                        onOpenWorkspaceSync = { findNavController().navigate(R.id.workspace_sync_fragment) },
                        onOpenWorkspaceTerminal = { findNavController().navigate(R.id.workspace_terminal_fragment) },
                        onOpenAppLog = { findNavController().navigate(R.id.app_log_fragment) }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.isFloatingNavigationEnabled.observe(viewLifecycleOwner) { isEnabled ->
            floatingNavigationEnabled = isEnabled
        }
        viewModel.isSoraEditorEnabled.observe(viewLifecycleOwner) { isEnabled ->
            soraEditorEnabled = isEnabled
        }
        viewModel.isUiDebugOverlayEnabled.observe(viewLifecycleOwner) { isEnabled ->
            uiDebugOverlayEnabled = isEnabled
        }
        viewModel.defaultSectionOrder.observe(viewLifecycleOwner) { sections ->
            repositorySectionOrder = sections
        }
    }

    private fun syncStateFromViewModelSnapshot() {
        floatingNavigationEnabled = viewModel.isFloatingNavigationEnabled.value ?: false
        soraEditorEnabled = viewModel.isSoraEditorEnabled.value ?: false
        uiDebugOverlayEnabled = viewModel.isUiDebugOverlayEnabled.value ?: false
        repositorySectionOrder = viewModel.defaultSectionOrder.value.orEmpty()
    }
}
