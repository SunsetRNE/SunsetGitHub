package com.Sunset.REN.GitHub.ui.compose.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens

@Composable
fun SunsetCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(SunsetGitHubThemeTokens.spacing.lg),
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, colors.border),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}