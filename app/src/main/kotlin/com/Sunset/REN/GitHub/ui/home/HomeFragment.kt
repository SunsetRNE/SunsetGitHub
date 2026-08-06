package com.Sunset.REN.GitHub.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubTheme
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens
import com.Sunset.REN.GitHub.ui.search.SearchFragment
import com.Sunset.REN.GitHub.util.AppLogger

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SunsetGitHubTheme {
                    HomeScreen(onOpenSearch = ::openSearch)
                }
            }
        }
    }

    private fun openSearch() {
        runCatching {
            SearchFragment().show(parentFragmentManager, SearchFragment.TAG)
        }.onFailure { error ->
            AppLogger.e(TAG, "open search failed", error)
            Toast.makeText(requireContext(), getString(R.string.search_open_failed), Toast.LENGTH_SHORT).show()
        }
    }

    private companion object {
        const val TAG = "HomeFragment"
    }
}

@Composable
private fun HomeScreen(onOpenSearch: () -> Unit) {
    val colors = SunsetGitHubThemeTokens.colors
    val spacing = SunsetGitHubThemeTokens.spacing
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.lg, vertical = spacing.lg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(colors.surface)
                .clickable(onClick = onOpenSearch)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⌕",
                color = colors.textMuted,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.size(10.dp))
            Text(
                text = stringResource(R.string.search_entry_hint),
                color = colors.textMuted,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
