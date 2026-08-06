package com.Sunset.REN.GitHub.ui.compose.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens
import com.Sunset.REN.GitHub.ui.compose.components.SunsetPrimaryButton
import com.Sunset.REN.GitHub.ui.compose.components.SunsetSecondaryButton

@Composable
fun AuthPageSurface(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 20.dp,
    verticalPadding: Dp = 32.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    Surface(
        modifier = modifier.fillMaxSize(),
        color = colors.canvas
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}

@Composable
fun AuthTitle(title: String, subtitle: String) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        text = title,
        color = SunsetGitHubThemeTokens.colors.textPrimary,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        text = subtitle,
        color = SunsetGitHubThemeTokens.colors.textSecondary,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center
    )
}

@Composable
fun AuthLogo(size: Int) {
    val colors = SunsetGitHubThemeTokens.colors
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(colors.accent),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "GH",
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = (size / 3).sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AuthMethodCard(
    leading: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, colors.border),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StepBubble(text = leading, size = 44)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = title,
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    modifier = Modifier.padding(top = 2.dp),
                    text = subtitle,
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = "›",
                color = colors.textMuted,
                fontSize = 30.sp
            )
        }
    }
}

@Composable
fun AuthStepCard(number: String, title: String, description: String) {
    val colors = SunsetGitHubThemeTokens.colors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, colors.border),
        colors = CardDefaults.cardColors(containerColor = colors.subtleBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            StepBubble(text = number, size = 36)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = title,
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    modifier = Modifier.padding(top = 2.dp),
                    text = description,
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun AuthActionCard(
    title: String,
    description: String,
    primaryActionText: String,
    onPrimaryActionClick: () -> Unit,
    primary: Boolean = true
) {
    val colors = SunsetGitHubThemeTokens.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, colors.border),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                color = colors.textPrimary,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                modifier = Modifier.padding(top = 6.dp),
                text = description,
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            if (primary) {
                SunsetPrimaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    text = primaryActionText,
                    onClick = onPrimaryActionClick
                )
            } else {
                SunsetSecondaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    text = primaryActionText,
                    onClick = onPrimaryActionClick
                )
            }
        }
    }
}

@Composable
fun InfoCard(title: String, description: String) {
    val colors = SunsetGitHubThemeTokens.colors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, colors.border),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                color = colors.textPrimary,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                modifier = Modifier.padding(top = 6.dp),
                text = description,
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun StepBubble(text: String, size: Int) {
    val colors = SunsetGitHubThemeTokens.colors
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(colors.accentSoft),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = colors.accent,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}
