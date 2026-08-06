package com.Sunset.REN.GitHub.ui.auth

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
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubTheme
import com.Sunset.REN.GitHub.ui.compose.screens.auth.LoginHomeScreen

class LoginHomeFragment : Fragment() {

    private lateinit var viewModel: LoginHomeViewModel
    private var uiState by mutableStateOf(LoginHomeUiState())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewModel = ViewModelProvider(this)[LoginHomeViewModel::class.java]
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SunsetGitHubTheme {
                    LoginHomeScreen(
                        stateMessage = uiState.message,
                        onDeviceFlowClick = { findNavController().navigate(R.id.navigation_device_flow_intro) },
                        onTokenLoginClick = { findNavController().navigate(R.id.navigation_token_choice) }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.state.observe(viewLifecycleOwner) { state ->
            uiState = state
            if (state.shouldEnterHome) {
                findNavController().navigate(
                    R.id.navigation_home,
                    null,
                    NavOptions.Builder()
                        .setPopUpTo(R.id.navigation_login, true)
                        .build()
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val fromAccountManager = arguments?.getBoolean(ARG_ADD_ACCOUNT_MODE) == true
        viewModel.refresh(autoEnterCurrent = !fromAccountManager)
    }

    companion object {
        const val ARG_ADD_ACCOUNT_MODE = "add_account_mode"
    }
}
