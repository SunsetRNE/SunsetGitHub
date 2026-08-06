package com.Sunset.REN.GitHub.ui.workspace

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
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubTheme
import com.Sunset.REN.GitHub.ui.compose.screens.workspace.WorkspacePushScreen

class WorkspacePushFragment : Fragment() {
    private lateinit var viewModel: WorkspaceSyncViewModel
    private var uiState by mutableStateOf(WorkspaceSyncUiState())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this)[WorkspaceSyncViewModel::class.java]
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SunsetGitHubTheme {
                    WorkspacePushScreen(
                        state = uiState,
                        onCreateWorkspace = viewModel::createWorkspace,
                        onImportPath = viewModel::importPath,
                        onDryRun = viewModel::dryRun,
                        onExecuteSync = viewModel::execute
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.state.observe(viewLifecycleOwner) { state -> uiState = state }
        viewModel.loadInitialWorkspace()
    }
}