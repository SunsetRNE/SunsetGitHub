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
import com.Sunset.REN.GitHub.data.github.html.RepositoryDeployKeyItem
import com.Sunset.REN.GitHub.ui.common.showComposeDialog
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubTheme
import com.Sunset.REN.GitHub.ui.compose.screens.repo.RepositoryConfirmDialog
import com.Sunset.REN.GitHub.ui.compose.screens.repo.RepositoryDeployKeyAddDialog
import com.Sunset.REN.GitHub.ui.compose.screens.repo.RepositoryDeployKeysScreen
import com.Sunset.REN.GitHub.ui.compose.screens.repo.RepositoryTextInputDialog
import com.google.android.material.snackbar.Snackbar

class RepositoryDeployKeysFragment : Fragment() {
    private val viewModel: RepositoryDeployKeysViewModel by viewModels()
    private var deployKeysState by mutableStateOf(RepositoryDeployKeysUiState())
    private var rootView: View? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        deployKeysState = viewModel.state.value ?: RepositoryDeployKeysUiState()
        return ComposeView(requireContext()).apply {
            this@RepositoryDeployKeysFragment.rootView = this
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SunsetGitHubTheme {
                    RepositoryDeployKeysScreen(
                        state = deployKeysState,
                        onRefresh = viewModel::refresh,
                        onAddKey = ::showAddDialog,
                        onDeleteKey = ::confirmDelete
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.state.observe(viewLifecycleOwner) { deployKeysState = it }
        viewModel.prepare(arguments?.getString(ARG_OWNER).orEmpty(), arguments?.getString(ARG_REPO).orEmpty())
    }

    override fun onDestroyView() {
        rootView = null
        super.onDestroyView()
    }

    private fun showAddDialog() {
        showComposeDialog(requireContext()) { dismiss ->
            RepositoryDeployKeyAddDialog(
                dismissText = getString(android.R.string.cancel),
                onDismiss = dismiss,
                onConfirmReadOnly = { title, key ->
                    dismiss()
                    viewModel.add(title, key, readOnly = true)
                },
                onConfirmWriteAccess = { title, key ->
                    dismiss()
                    confirmWriteAccessAdd(title, key)
                }
            )
        }
    }

    private fun confirmWriteAccessAdd(title: String, key: String) {
        showComposeDialog(requireContext()) { dismiss ->
            RepositoryTextInputDialog(
                title = "确认写权限部署密钥",
                label = "确认文本",
                helperText = "写权限部署密钥拥有向仓库推送代码的能力。请输入 WRITE 后继续。",
                initialValue = "",
                confirmText = "添加写权限密钥",
                dismissText = getString(android.R.string.cancel),
                requiredErrorText = "请输入 WRITE",
                onDismiss = dismiss,
                onConfirm = { value ->
                    dismiss()
                    if (value == "WRITE") {
                        viewModel.add(title, key, readOnly = false)
                    } else {
                        rootView?.let { Snackbar.make(it, "确认文本不匹配，已取消添加写权限部署密钥。", Snackbar.LENGTH_SHORT).show() }
                    }
                }
            )
        }
    }

    private fun confirmDelete(item: RepositoryDeployKeyItem) {
        showComposeDialog(requireContext()) { dismiss ->
            RepositoryConfirmDialog(
                title = "删除部署密钥",
                message = "即将删除 ${item.title.ifBlank { "#${item.id}" }}。删除后，使用该密钥的部署、拉取或推送流程可能立即失败。",
                confirmText = "继续",
                dismissText = getString(android.R.string.cancel),
                onDismiss = dismiss,
                onConfirm = {
                    dismiss()
                    confirmDeleteSecond(item)
                }
            )
        }
    }

    private fun confirmDeleteSecond(item: RepositoryDeployKeyItem) {
        showComposeDialog(requireContext()) { dismiss ->
            RepositoryConfirmDialog(
                title = "再次确认删除",
                message = "这是永久操作。确认删除部署密钥：${item.title.ifBlank { "#${item.id}" }}？",
                confirmText = "永久删除",
                dismissText = getString(android.R.string.cancel),
                onDismiss = dismiss,
                onConfirm = {
                    dismiss()
                    viewModel.delete(item)
                }
            )
        }
    }

    companion object {
        const val ARG_OWNER = "owner"
        const val ARG_REPO = "repo"
    }
}
