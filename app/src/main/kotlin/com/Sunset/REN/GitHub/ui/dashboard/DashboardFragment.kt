package com.Sunset.REN.GitHub.ui.dashboard

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.data.local.ThemePreferenceStore
import com.Sunset.REN.GitHub.domain.repo.GitHubRepository
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubTheme
import com.Sunset.REN.GitHub.ui.compose.screens.dashboard.DashboardScreen
import com.Sunset.REN.GitHub.ui.repo.RepositoriesUiState
import com.Sunset.REN.GitHub.ui.repo.RepositoryDetailFragment
import com.Sunset.REN.GitHub.util.AppLogger

/** Compose-first repository dashboard while retaining the existing Fragment navigation contract. */
class DashboardFragment : Fragment() {

    private lateinit var dashboardViewModel: DashboardViewModel
    private lateinit var themePreferenceStore: ThemePreferenceStore
    private var repositoriesState by mutableStateOf<RepositoriesUiState>(RepositoriesUiState.Loading)
    private var floatingNavigationEnabled by mutableStateOf(false)
    private val themePreferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == ThemePreferenceStore.KeyFloatingNavigationEnabled) {
            floatingNavigationEnabled = themePreferenceStore.isFloatingNavigationEnabled()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dashboardViewModel = ViewModelProvider(this)[DashboardViewModel::class.java]
        themePreferenceStore = ThemePreferenceStore(requireContext().applicationContext)
        floatingNavigationEnabled = themePreferenceStore.isFloatingNavigationEnabled()
        themePreferenceStore.registerListener(themePreferenceListener)
        repositoriesState = dashboardViewModel.repositoriesState.value ?: RepositoriesUiState.Loading
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SunsetGitHubTheme {
                    DashboardScreen(
                        state = repositoriesState,
                        onOpenRepository = ::openRepositoryDetail,
                        onTogglePinned = dashboardViewModel::togglePinned,
                        onToggleFavorite = dashboardViewModel::toggleFavorite,
                        onRefresh = dashboardViewModel::retryRepositories,
                        onLoadMore = dashboardViewModel::loadMoreRepositories,
                        onOpenHome = ::openHome,
                        bottomContentPadding = if (floatingNavigationEnabled) FloatingNavigationContentPadding else 0.dp
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dashboardViewModel.repositoriesState.observe(viewLifecycleOwner) { state ->
            repositoriesState = state
        }
    }

    override fun onDestroyView() {
        if (::themePreferenceStore.isInitialized) {
            themePreferenceStore.unregisterListener(themePreferenceListener)
        }
        super.onDestroyView()
    }

    /** Called by the activity toolbar; mirrors the former XML dashboard refresh behavior. */
    fun refreshRepositoriesFromToolbar() {
        if (!isAdded) return
        dashboardViewModel.retryRepositories()
    }

    private fun openRepositoryDetail(repository: GitHubRepository) {
        AppLogger.i(TAG, "open repository detail requested: ${repository.fullName}")
        val arguments = Bundle().apply {
            putString(RepositoryDetailFragment.ARG_OWNER, repository.ownerLogin)
            putString(RepositoryDetailFragment.ARG_REPO, repository.name)
            putString(RepositoryDetailFragment.ARG_FULL_NAME, repository.fullName)
        }
        runCatching {
            findNavController().navigate(R.id.repository_detail_fragment, arguments)
        }.onFailure { error ->
            AppLogger.e(TAG, "open repository detail failed: ${repository.fullName}", error)
            Toast.makeText(requireContext(), "打开仓库详情失败。", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openHome() {
        runCatching {
            findNavController().navigate(R.id.navigation_home)
        }.onFailure { error ->
            AppLogger.e(TAG, "open home failed", error)
            Toast.makeText(requireContext(), "首页导航节点缺失。", Toast.LENGTH_SHORT).show()
        }
    }

    private companion object {
        const val TAG = "DashboardFragment"
        val FloatingNavigationContentPadding = 82.dp
    }
}