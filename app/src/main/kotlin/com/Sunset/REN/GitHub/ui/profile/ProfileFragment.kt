package com.Sunset.REN.GitHub.ui.profile

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
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
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.repo.GitHubRepository
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubTheme
import com.Sunset.REN.GitHub.ui.compose.screens.profile.ProfileScreen
import com.Sunset.REN.GitHub.ui.repo.GitHubInternalLinkParser
import com.Sunset.REN.GitHub.ui.repo.GitHubInternalLinkTarget
import com.Sunset.REN.GitHub.ui.repo.RepositoryDetailFragment

class ProfileFragment : Fragment() {

    private lateinit var profileViewModel: ProfileViewModel
    private var profileState by mutableStateOf<ProfileUiState>(ProfileUiState.Loading)
    private var currentProfileUrl: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        profileViewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
        val login = arguments?.getString(ARG_LOGIN)?.takeIf { it.isNotBlank() }
        val composeView = ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SunsetGitHubTheme {
                    ProfileScreen(
                        state = profileState,
                        onRetry = profileViewModel::retryProfile,
                        onOpenGitHub = ::openGitHubUrl,
                        onOpenRepository = ::openRepository
                    )
                }
            }
        }
        profileViewModel.profileState.observe(viewLifecycleOwner) { state ->
            profileState = state
            currentProfileUrl = (state as? ProfileUiState.Content)?.profile?.htmlUrl.orEmpty()
        }
        profileViewModel.start(login)
        return composeView
    }

    fun openProfileInGitHubFromToolbar() {
        openGitHubUrl(currentProfileUrl)
    }

    fun refreshProfileFromToolbar() {
        profileViewModel.retryProfile()
    }

    private fun openRepository(repository: GitHubRepository) {
        runCatching {
            findNavController().navigate(
                R.id.repository_detail_fragment,
                Bundle().apply {
                    putString(RepositoryDetailFragment.ARG_OWNER, repository.ownerLogin)
                    putString(RepositoryDetailFragment.ARG_REPO, repository.name)
                    putString(RepositoryDetailFragment.ARG_FULL_NAME, repository.fullName)
                }
            )
        }.onFailure {
            openGitHubUrl(repository.htmlUrl)
        }
    }

    private fun openGitHubUrl(url: String) {
        if (url.isBlank()) return
        val repositoryTarget = GitHubInternalLinkParser.parse(url) as? GitHubInternalLinkTarget.Repository
        if (repositoryTarget != null) {
            runCatching {
                findNavController().navigate(
                    R.id.repository_detail_fragment,
                    Bundle().apply {
                        putString(RepositoryDetailFragment.ARG_OWNER, repositoryTarget.owner)
                        putString(RepositoryDetailFragment.ARG_REPO, repositoryTarget.repo)
                        putString(RepositoryDetailFragment.ARG_FULL_NAME, "${repositoryTarget.owner}/${repositoryTarget.repo}")
                    }
                )
            }.onSuccess { return }
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            startActivity(intent)
        } catch (exception: ActivityNotFoundException) {
            Toast.makeText(requireContext(), R.string.profile_open_url_failed, Toast.LENGTH_SHORT).show()
        } catch (exception: SecurityException) {
            Toast.makeText(requireContext(), R.string.profile_open_url_failed, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val ARG_LOGIN = "login"
    }
}