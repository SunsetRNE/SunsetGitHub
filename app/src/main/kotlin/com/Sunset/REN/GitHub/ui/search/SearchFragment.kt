package com.Sunset.REN.GitHub.ui.search

import android.app.Dialog
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.repo.GitHubCodeSearchItem
import com.Sunset.REN.GitHub.domain.repo.GitHubIssueSearchItem
import com.Sunset.REN.GitHub.domain.repo.GitHubRepository
import com.Sunset.REN.GitHub.domain.repo.GitHubUserSearchItem
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubTheme
import com.Sunset.REN.GitHub.ui.compose.screens.search.SearchScreen
import com.Sunset.REN.GitHub.ui.profile.ProfileFragment
import com.Sunset.REN.GitHub.ui.repo.RepositoryDetailFragment
import com.Sunset.REN.GitHub.ui.repo.RepositoryFileEditFragment
import com.Sunset.REN.GitHub.ui.repo.RepositoryIssueDetailFragment
import com.Sunset.REN.GitHub.util.AppLogger

class SearchFragment : DialogFragment() {

    private lateinit var searchViewModel: SearchViewModel
    private var searchState by mutableStateOf<SearchUiState>(SearchUiState.Idle)
    private var currentQuery by mutableStateOf("")
    private var composeRootView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.ThemeOverlay_SunsetGitHub_SearchDialog)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            window?.apply {
                setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
                clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                setWindowAnimations(0)
            }
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    dismissAllowingStateLoss()
                    true
                } else {
                    false
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        searchViewModel = ViewModelProvider(this)[SearchViewModel::class.java]
        val composeView = ComposeView(requireContext()).apply {
            composeRootView = this
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SunsetGitHubTheme {
                    val keyboardController = LocalSoftwareKeyboardController.current
                    SearchScreen(
                        query = currentQuery,
                        state = searchState,
                        onQueryChange = { query ->
                            currentQuery = query
                            searchViewModel.onQueryChanged(query)
                        },
                        onSearch = { query, type ->
                            keyboardController?.hide()
                            searchViewModel.search(query, type)
                        },
                        onDismiss = {
                            keyboardController?.hide()
                            dismissAllowingStateLoss()
                        },
                        onRetry = searchViewModel::retry,
                        onPrevPage = searchViewModel::prevPage,
                        onNextPage = searchViewModel::nextPage,
                        onOpenRepository = ::openRepositoryDetail,
                        onOpenUser = ::openUserProfile,
                        onOpenIssue = ::openIssueDetail,
                        onOpenCode = ::openCodeFile
                    )
                }
            }
        }
        searchViewModel.searchState.observe(viewLifecycleOwner) { state -> searchState = state }
        return composeView
    }

    override fun onResume() {
        super.onResume()
        composeRootView?.postDelayed({
            val imm = requireContext().getSystemService(InputMethodManager::class.java)
            imm?.showSoftInput(composeRootView, InputMethodManager.SHOW_IMPLICIT)
        }, 180L)
    }

    private fun openRepositoryDetail(repository: GitHubRepository) {
        AppLogger.i(TAG, "open repository detail from search: ${repository.fullName}")
        val arguments = Bundle().apply {
            putString(RepositoryDetailFragment.ARG_OWNER, repository.ownerLogin)
            putString(RepositoryDetailFragment.ARG_REPO, repository.name)
            putString(RepositoryDetailFragment.ARG_FULL_NAME, repository.fullName)
        }
        val navController: androidx.navigation.NavController? = null
        if (navController == null) {
            Toast.makeText(requireContext(), "仓库详情页导航节点缺失。", Toast.LENGTH_SHORT).show()
            return
        }
        val destinationId = resources.getIdentifier(RepositoryDetailDestinationName, ResourceTypeId, requireContext().packageName)
        if (destinationId == 0) {
            Toast.makeText(requireContext(), "仓库详情页导航节点缺失。", Toast.LENGTH_SHORT).show()
            return
        }
        dismissAllowingStateLoss()
        runCatching { navController.navigate(destinationId, arguments) }
            .onFailure { error ->
                AppLogger.e(TAG, "open repository detail failed: ${repository.fullName}", error)
                Toast.makeText(requireContext(), "打开仓库详情失败。", Toast.LENGTH_SHORT).show()
            }
    }

    private fun openUserProfile(user: GitHubUserSearchItem) {
        if (user.login.isBlank()) return
        val navController: androidx.navigation.NavController? = null
        if (navController == null) {
            Toast.makeText(requireContext(), "用户主页导航节点缺失。", Toast.LENGTH_SHORT).show()
            return
        }
        dismissAllowingStateLoss()
        runCatching {
            navController.navigate(R.id.navigation_profile, Bundle().apply { putString(ProfileFragment.ARG_LOGIN, user.login) })
        }.onFailure { error ->
            AppLogger.e(TAG, "open user profile failed: ${user.login}", error)
            Toast.makeText(requireContext(), "打开用户主页失败。", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openIssueDetail(issue: GitHubIssueSearchItem) {
        if (issue.repositoryOwner.isBlank() || issue.repositoryName.isBlank() || issue.number <= 0) {
            Toast.makeText(requireContext(), "无法定位该 Issue 所属仓库。", Toast.LENGTH_SHORT).show()
            return
        }
        val navController: androidx.navigation.NavController? = null
        if (navController == null) {
            Toast.makeText(requireContext(), "Issue 详情导航节点缺失。", Toast.LENGTH_SHORT).show()
            return
        }
        dismissAllowingStateLoss()
        runCatching {
            navController.navigate(
                R.id.repository_issue_detail_fragment,
                Bundle().apply {
                    putString(RepositoryIssueDetailFragment.ARG_OWNER, issue.repositoryOwner)
                    putString(RepositoryIssueDetailFragment.ARG_REPO, issue.repositoryName)
                    putInt(RepositoryIssueDetailFragment.ARG_NUMBER, issue.number)
                }
            )
        }.onFailure { error ->
            AppLogger.e(TAG, "open issue detail failed: ${issue.repositoryFullName}#${issue.number}", error)
            Toast.makeText(requireContext(), "打开 Issue 详情失败。", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openCodeFile(code: GitHubCodeSearchItem) {
        if (code.repositoryOwner.isBlank() || code.repositoryName.isBlank() || code.path.isBlank()) {
            Toast.makeText(requireContext(), "无法定位该文件所属仓库。", Toast.LENGTH_SHORT).show()
            return
        }
        val navController: androidx.navigation.NavController? = null
        if (navController == null) {
            Toast.makeText(requireContext(), "文件预览导航节点缺失。", Toast.LENGTH_SHORT).show()
            return
        }
        dismissAllowingStateLoss()
        runCatching {
            navController.navigate(
                R.id.repository_file_edit_fragment,
                Bundle().apply {
                    putString(RepositoryFileEditFragment.ARG_OWNER, code.repositoryOwner)
                    putString(RepositoryFileEditFragment.ARG_REPO, code.repositoryName)
                    putString(RepositoryFileEditFragment.ARG_PATH, code.path)
                    putString(RepositoryFileEditFragment.ARG_NAME, code.name)
                    putBoolean(RepositoryFileEditFragment.ARG_PREVIEW_MODE, true)
                }
            )
        }.onFailure { error ->
            AppLogger.e(TAG, "open code file failed: ${code.repositoryFullName}/${code.path}", error)
            Toast.makeText(requireContext(), "打开文件预览失败。", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        composeRootView = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "SearchFragment"
        private const val RepositoryDetailDestinationName = "repository_detail_fragment"
        private const val ResourceTypeId = "id"
    }
}