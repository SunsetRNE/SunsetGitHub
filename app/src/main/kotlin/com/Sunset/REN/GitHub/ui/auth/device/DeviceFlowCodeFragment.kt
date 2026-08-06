package com.Sunset.REN.GitHub.ui.auth.device

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubTheme
import com.Sunset.REN.GitHub.ui.compose.screens.auth.DeviceFlowCodeScreen
import com.Sunset.REN.GitHub.util.BrowserLauncher

class DeviceFlowCodeFragment : Fragment() {
    private lateinit var viewModel: DeviceFlowViewModel
    private var uiState by mutableStateOf<DeviceFlowUiState>(DeviceFlowUiState.RequestingCode)
    private var browserUrl: String? = null
    private var code: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewModel = ViewModelProvider(this)[DeviceFlowViewModel::class.java]
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SunsetGitHubTheme {
                    DeviceFlowCodeScreen(
                        state = uiState,
                        onCopyOrRetryClick = ::copyCodeOrRetry,
                        onOpenBrowserClick = ::openBrowser,
                        onCancelClick = { viewModel.cancel() }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.state.observe(viewLifecycleOwner) { renderState(it) }
        viewModel.start()
    }

    private fun renderState(state: DeviceFlowUiState) {
        uiState = state
        when (state) {
            DeviceFlowUiState.RequestingCode -> {
                code = ""
                browserUrl = null
            }
            is DeviceFlowUiState.CodeReady -> {
                code = state.userCode
                browserUrl = state.verificationUriComplete ?: state.verificationUri
            }
            is DeviceFlowUiState.SignedIn -> {
                Toast.makeText(requireContext(), getString(R.string.auth_token_login_success, state.account.login), Toast.LENGTH_SHORT).show()
                findNavController().navigate(
                    R.id.navigation_home,
                    null,
                    NavOptions.Builder().setPopUpTo(R.id.navigation_login, true).build()
                )
            }
            is DeviceFlowUiState.Error -> {
                code = ""
                browserUrl = null
            }
            DeviceFlowUiState.Cancelled -> findNavController().popBackStack()
        }
    }

    private fun copyCodeOrRetry() {
        if (code.isBlank()) {
            viewModel.start()
            return
        }
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("GitHub device code", code))
        Toast.makeText(requireContext(), "验证码已复制到剪贴板", Toast.LENGTH_SHORT).show()
    }

    private fun openBrowser() {
        val url = browserUrl ?: return
        if (!BrowserLauncher.open(requireContext(), url)) {
            Toast.makeText(requireContext(), R.string.auth_verification_no_browser, Toast.LENGTH_SHORT).show()
        }
    }
}