package com.Sunset.REN.GitHub.ui.repo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.repo.RepositoryContentItem
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubTheme
import com.Sunset.REN.GitHub.ui.compose.screens.repo.RepositoryDetailScreen
import com.Sunset.REN.GitHub.ui.filemanager.LocalFileManagerFragment

class RepositoryDetailFragment : Fragment(), RepositoryMoreSectionsBottomSheet.Host {

    private lateinit var viewModel: RepositoryDetailViewModel
    private var repositoryOwner: String = ""
    private var repositoryName: String = ""
    private var repositoryFullName: String = ""
    private var currentState by mutableStateOf<RepositoryDetailUiState>(RepositoryDetailUiState.Loading)

    private val uploadFilePicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri -> openUploadFilePage(uri, resolveDisplayName(uri)) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this)[RepositoryDetailViewModel::class.java]
        val args = arguments ?: Bundle.EMPTY
        repositoryOwner = args.getString(ARG_OWNER).orEmpty()
        repositoryName = args.getString(ARG_REPO).orEmpty()
        repositoryFullName = args.getString(ARG_FULL_NAME).orEmpty()
            .ifBlank { currentRepositoryFullName() }
        applyRepositoryPageTitle(repositoryFullName)

        val composeView = ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SunsetGitHubTheme {
                    RepositoryDetailScreen(
                        state = currentState,
                        onRetry = { loadRepository(forceRefresh = true) },
                        onRetryContents = { viewModel.retryCurrentContents() },
                        onRetryPreview = { viewModel.retryLastFilePreview() },
                        onOpenGitHub = ::openUrl,
                        onOpenOwner = ::openOwnerProfile,
                        onOpenFile = ::openFile,
                        onOpenDirectory = ::openDirectory,
                        onSelectBranch = { branch -> viewModel.selectBranch(branch) },
                        onRefreshBranches = { viewModel.refreshBranches() },
                        onSelectReadmePreview = ::selectReadmePreview,
                        onEditPreviewFile = ::openPreviewFileEditor,
                        onCreateFile = ::openCreateFilePage,
                        onUploadFile = ::openUploadFilePicker,
                        onToggleStar = { viewModel.toggleStarRepository() },
                        onToggleWatch = { viewModel.toggleWatchRepository() },
                        onOpenSection = ::openRepositorySection,
                        onOpenReleases = ::openReleases
                    )
                }
            }
        }

        if (repositoryOwner.isBlank() || repositoryName.isBlank()) {
            currentState = RepositoryDetailUiState.Error(getString(R.string.repository_detail_missing_args))
            return composeView
        }

        observeFileWriteResults()
        viewModel.repositoryState.observe(viewLifecycleOwner) { state ->
            currentState = state
            (state as? RepositoryDetailUiState.Content)?.let { content ->
                repositoryFullName = content.repository.fullName
                applyRepositoryPageTitle(content.repository.fullName)
            }
        }
        loadRepository(forceRefresh = false)
        return composeView
    }

    private fun loadRepository(forceRefresh: Boolean) {
        if (repositoryOwner.isBlank() || repositoryName.isBlank()) return
        viewModel.loadRepository(repositoryOwner, repositoryName, forceRefresh = forceRefresh)
    }

    private fun observeFileWriteResults() {
        val savedStateHandle = findNavController().currentBackStackEntry?.savedStateHandle ?: return
        savedStateHandle.getLiveData<Boolean>(RepositoryFileEditFragment.RESULT_FILE_UPDATED)
            .observe(viewLifecycleOwner) { updated ->
                if (updated == true) {
                    val path = savedStateHandle.get<String>(RepositoryFileEditFragment.RESULT_FILE_UPDATED_PATH).orEmpty()
                    savedStateHandle.set(RepositoryFileEditFragment.RESULT_FILE_UPDATED, false)
                    savedStateHandle.set(RepositoryFileEditFragment.RESULT_FILE_UPDATED_PATH, "")
                    refreshContentsAfterFileWrite(path)
                }
            }
        savedStateHandle.getLiveData<Boolean>(RepositoryFileUploadFragment.RESULT_FILE_UPLOADED)
            .observe(viewLifecycleOwner) { uploaded ->
                if (uploaded == true) {
                    val path = savedStateHandle.get<String>(RepositoryFileUploadFragment.RESULT_FILE_UPLOADED_PATH).orEmpty()
                    savedStateHandle.set(RepositoryFileUploadFragment.RESULT_FILE_UPLOADED, false)
                    savedStateHandle.set(RepositoryFileUploadFragment.RESULT_FILE_UPLOADED_PATH, "")
                    refreshContentsAfterFileWrite(path)
                }
            }
        savedStateHandle.getLiveData<Boolean>(LocalFileManagerFragment.RESULT_FILE_SELECTED)
            .observe(viewLifecycleOwner) { selected ->
                if (selected == true) {
                    val selectedUri = savedStateHandle.get<String>(LocalFileManagerFragment.RESULT_FILE_SELECTED_URI).orEmpty()
                    val displayName = savedStateHandle.get<String>(LocalFileManagerFragment.RESULT_FILE_SELECTED_DISPLAY_NAME).orEmpty()
                    savedStateHandle.set(LocalFileManagerFragment.RESULT_FILE_SELECTED, false)
                    savedStateHandle.set(LocalFileManagerFragment.RESULT_FILE_SELECTED_URI, "")
                    savedStateHandle.set(LocalFileManagerFragment.RESULT_FILE_SELECTED_DISPLAY_NAME, "")
                    if (selectedUri.isNotBlank()) openUploadFilePage(Uri.parse(selectedUri), displayName)
                }
            }
    }

    private fun refreshContentsAfterFileWrite(path: String) {
        if (path.isBlank()) viewModel.retryCurrentContents() else viewModel.refreshParentDirectoryForFile(path)
    }

    private fun openFile(file: RepositoryContentItem.File) {
        if (repositoryOwner.isBlank() || repositoryName.isBlank() || file.path.isBlank()) return
        findNavController().navigate(
            R.id.repository_file_edit_fragment,
            Bundle().apply {
                putString(RepositoryFileEditFragment.ARG_OWNER, repositoryOwner)
                putString(RepositoryFileEditFragment.ARG_REPO, repositoryName)
                putString(RepositoryFileEditFragment.ARG_PATH, file.path)
                putString(RepositoryFileEditFragment.ARG_NAME, file.name)
                putBoolean(RepositoryFileEditFragment.ARG_PREVIEW_MODE, true)
            }
        )
    }

    private fun selectReadmePreview(file: RepositoryContentItem.File) {
        viewModel.openFile(file.path, file.name)
    }

    private fun openPreviewFileEditor(preview: com.Sunset.REN.GitHub.domain.repo.RepositoryFilePreview) {
        if (repositoryOwner.isBlank() || repositoryName.isBlank() || preview.path.isBlank()) return
        findNavController().navigate(
            R.id.repository_file_edit_fragment,
            Bundle().apply {
                putString(RepositoryFileEditFragment.ARG_OWNER, repositoryOwner)
                putString(RepositoryFileEditFragment.ARG_REPO, repositoryName)
                putString(RepositoryFileEditFragment.ARG_PATH, preview.path)
                putString(RepositoryFileEditFragment.ARG_NAME, preview.name)
                putString(RepositoryFileEditFragment.ARG_INITIAL_CONTENT, preview.text)
                putBoolean(RepositoryFileEditFragment.ARG_PREVIEW_MODE, !hasWritableRepository())
            }
        )
    }

    private fun openDirectory(directory: RepositoryContentItem.Directory) {
        viewModel.openDirectory(directory.path)
    }

    private fun openCreateFilePage() {
        if (!hasWritableRepository()) {
            Toast.makeText(requireContext(), "当前账号没有仓库写入权限。", Toast.LENGTH_SHORT).show()
            return
        }
        findNavController().navigate(
            R.id.repository_file_edit_fragment,
            Bundle().apply {
                putString(RepositoryFileEditFragment.ARG_OWNER, repositoryOwner)
                putString(RepositoryFileEditFragment.ARG_REPO, repositoryName)
                putString(RepositoryFileEditFragment.ARG_PATH, DefaultNewFileName)
                putString(RepositoryFileEditFragment.ARG_NAME, DefaultNewFileName)
            }
        )
    }

    private fun openUploadFilePicker() {
        if (!hasWritableRepository()) {
            Toast.makeText(requireContext(), "当前账号没有仓库写入权限。", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        uploadFilePicker.launch(Intent.createChooser(intent, getString(R.string.repository_file_upload_title)))
    }

    private fun openUploadFilePage(uri: Uri, displayName: String) {
        if (repositoryOwner.isBlank() || repositoryName.isBlank()) return
        findNavController().navigate(
            R.id.repository_file_upload_fragment,
            Bundle().apply {
                putString(RepositoryFileUploadFragment.ARG_OWNER, repositoryOwner)
                putString(RepositoryFileUploadFragment.ARG_REPO, repositoryName)
                putString(RepositoryFileUploadFragment.ARG_SOURCE_URI, uri.toString())
                putString(RepositoryFileUploadFragment.ARG_DISPLAY_NAME, displayName)
                putStringArrayList(
                    RepositoryFileUploadFragment.ARG_VISIBLE_DIRECTORIES,
                    ArrayList(visibleDirectoryPaths())
                )
            }
        )
    }

    private fun visibleDirectoryPaths(): List<String> {
        val content = currentState as? RepositoryDetailUiState.Content ?: return emptyList()
        return content.contents
            .filterIsInstance<RepositoryContentItem.Directory>()
            .map { it.path }
    }

    private fun hasWritableRepository(): Boolean {
        return (currentState as? RepositoryDetailUiState.Content)?.canPush == true
    }

    private fun resolveDisplayName(uri: Uri): String {
        return uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: DefaultNewFileName
    }

    private fun openUrl(url: String) {
        if (url.isBlank()) return
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun openOwnerProfile(login: String) {
        if (login.isBlank()) return
        findNavController().navigate(
            R.id.navigation_profile,
            Bundle().apply { putString(com.Sunset.REN.GitHub.ui.profile.ProfileFragment.ARG_LOGIN, login) }
        )
    }

    private fun applyRepositoryPageTitle(title: String) {
        (activity as? AppCompatActivity)?.supportActionBar?.title = title.ifBlank { getString(R.string.title_repository_detail) }
    }

    private fun currentRepositoryFullName(): String {
        return if (repositoryOwner.isNotBlank() && repositoryName.isNotBlank()) {
            "$repositoryOwner/$repositoryName"
        } else {
            repositoryName.ifBlank { repositoryOwner }
        }
    }

    private fun showRepositoryStatusToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun showMoreSections() {
        RepositoryMoreSectionsBottomSheet().show(childFragmentManager, "repository_more_sections")
    }

    private fun openRepositorySection(section: RepositorySection) {
        when (section) {
            RepositorySection.Code -> Unit
            RepositorySection.More -> showMoreSections()
            else -> openRepositorySectionDestination(section)
        }
    }

    private fun openReleases() {
        if (repositoryOwner.isBlank() || repositoryName.isBlank()) {
            showRepositoryStatusToast(getString(R.string.repository_section_missing_repository))
            return
        }
        findNavController().navigate(
            R.id.repository_releases_fragment,
            Bundle().apply {
                putString(RepositoryReleasesFragment.ARG_OWNER, repositoryOwner)
                putString(RepositoryReleasesFragment.ARG_REPO, repositoryName)
            }
        )
    }

    private fun openRepositorySectionDestination(section: RepositorySection): Boolean {
        val destinationId = section.destinationIdResId ?: return false
        if (repositoryOwner.isBlank() || repositoryName.isBlank()) {
            showRepositoryStatusToast(getString(R.string.repository_section_missing_repository))
            return true
        }
        findNavController().navigate(
            destinationId,
            Bundle().apply {
                putString(ARG_OWNER, repositoryOwner)
                putString(ARG_REPO, repositoryName)
            }
        )
        return true
    }

    override fun currentShortcutSections(): List<RepositorySection> {
        return viewModel.shortcutSections(currentRepositoryFullName())
    }

    override fun currentSectionOrder(): List<RepositorySection> {
        return viewModel.sectionOrder(currentRepositoryFullName())
    }

    override fun isSectionSupportedInApp(section: RepositorySection): Boolean {
        return section == RepositorySection.Code || section.destinationIdResId != null
    }

    override fun onSectionChosen(section: RepositorySection) {
        openRepositorySection(section)
    }

    override fun onSectionPinned(section: RepositorySection): Boolean {
        val fullName = currentRepositoryFullName()
        val before = viewModel.shortcutSections(fullName)
        val after = viewModel.pinShortcutSection(fullName, section)
        val changed = after != before
        if (!changed) showRepositoryStatusToast(getString(R.string.repository_section_pin_limit_reached))
        return changed
    }

    override fun onSectionUnpinned(section: RepositorySection): Boolean {
        val fullName = currentRepositoryFullName()
        val before = viewModel.shortcutSections(fullName)
        val after = viewModel.unpinShortcutSection(fullName, section)
        val changed = after != before
        if (!changed) showRepositoryStatusToast(getString(R.string.repository_section_unpin_limit_reached))
        return changed
    }

    override fun onSectionOrderChanged(sections: List<RepositorySection>): Boolean {
        viewModel.setRepositorySectionOrder(currentRepositoryFullName(), sections)
        return true
    }

    companion object {
        const val ARG_OWNER = "owner"
        const val ARG_REPO = "repo"
        const val ARG_FULL_NAME = "full_name"
        private const val DefaultNewFileName = "new-file.txt"
    }
}