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
import com.Sunset.REN.GitHub.data.github.html.RepositoryWebhookItem
import com.Sunset.REN.GitHub.ui.common.showComposeDialog
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubTheme
import com.Sunset.REN.GitHub.ui.compose.screens.repo.RepositoryConfirmDialog
import com.Sunset.REN.GitHub.ui.compose.screens.repo.RepositoryWebhookCreateDialog
import com.Sunset.REN.GitHub.ui.compose.screens.repo.RepositoryWebhooksScreen

class RepositoryWebhooksFragment : Fragment() {
    private val viewModel: RepositoryWebhooksViewModel by viewModels()
    private var webhooksState by mutableStateOf(RepositoryWebhooksUiState())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        webhooksState = viewModel.state.value ?: RepositoryWebhooksUiState()
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SunsetGitHubTheme {
                    RepositoryWebhooksScreen(
                        state = webhooksState,
                        onRefresh = viewModel::refresh,
                        onCreateWebhook = ::showCreateDialog,
                        onPingWebhook = viewModel::ping,
                        onDeleteWebhook = ::confirmDelete
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.state.observe(viewLifecycleOwner) { webhooksState = it }
        viewModel.prepare(arguments?.getString(ARG_OWNER).orEmpty(), arguments?.getString(ARG_REPO).orEmpty())
    }

    private fun showCreateDialog() {
        showComposeDialog(requireContext()) { dismiss ->
            RepositoryWebhookCreateDialog(
                dismissText = getString(android.R.string.cancel),
                onDismiss = dismiss,
                onConfirm = { url, secret, events ->
                    dismiss()
                    viewModel.create(url, secret, events)
                }
            )
        }
    }

    private fun confirmDelete(item: RepositoryWebhookItem) {
        showComposeDialog(requireContext()) { dismiss ->
            RepositoryConfirmDialog(
                title = "删除 Webhook",
                message = "删除后 GitHub 将不再向该 Payload URL 发送事件。此操作不会删除外部服务中的配置。\n\n${item.url}",
                confirmText = "删除",
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
