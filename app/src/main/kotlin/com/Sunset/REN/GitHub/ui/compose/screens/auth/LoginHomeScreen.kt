package com.Sunset.REN.GitHub.ui.compose.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens

@Composable
fun LoginHomeScreen(
    stateMessage: String,
    onDeviceFlowClick: () -> Unit,
    onTokenLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    val background = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFEAF4FF),
            Color(0xFFF8FBFF),
            colors.canvas
        )
    )

    Surface(
        modifier = modifier.fillMaxSize(),
        color = colors.canvas
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 28.dp, end = 26.dp)
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.58f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "?",
                    color = colors.textMuted,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "LOGIN",
                    color = colors.textSecondary,
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                )
                Text(
                    modifier = Modifier.padding(top = 52.dp),
                    text = stringResource(R.string.auth_login_page_title),
                    color = colors.textPrimary,
                    fontSize = 36.sp,
                    lineHeight = 42.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                LoginHeroCard(
                    title = stringResource(R.string.auth_login_home_heading),
                    subtitle = stateMessage
                )

                Spacer(modifier = Modifier.height(28.dp))

                LoginMethodCard(
                    leading = "⌁",
                    title = stringResource(R.string.login_home_device_flow_title),
                    subtitle = stringResource(R.string.login_home_device_flow_subtitle),
                    onClick = onDeviceFlowClick
                )
                Spacer(modifier = Modifier.height(16.dp))
                LoginMethodCard(
                    leading = "🔑",
                    title = stringResource(R.string.login_home_token_title),
                    subtitle = stringResource(R.string.login_home_token_subtitle),
                    onClick = onTokenLoginClick
                )

                LocalCredentialsNote()
                RecommendedDeviceFlowBanner()
            }
        }
    }
}

@Composable
private fun LoginHeroCard(
    title: String,
    subtitle: String
) {
    val colors = SunsetGitHubThemeTokens.colors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 30.dp)
            .shadow(
                elevation = 18.dp,
                shape = RoundedCornerShape(30.dp),
                ambientColor = Color(0x1A0F172A),
                spotColor = Color(0x1F0F172A)
            ),
        shape = RoundedCornerShape(30.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.86f)),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.88f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .rotate(-7f)
                    .shadow(
                        elevation = 22.dp,
                        shape = RoundedCornerShape(32.dp),
                        ambientColor = Color(0x331565D8),
                        spotColor = Color(0x421565D8)
                    )
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF1463D6), Color(0xFF0F82EF), Color(0xFF7C3AED))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_github_mark),
                    contentDescription = stringResource(R.string.auth_github_icon_content_description),
                    modifier = Modifier
                        .size(64.dp)
                        .rotate(7f),
                    colorFilter = ColorFilter.tint(Color.White)
                )
            }
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp),
                text = title,
                color = colors.textPrimary,
                fontSize = 30.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                text = subtitle,
                color = colors.textSecondary,
                fontSize = 17.sp,
                lineHeight = 28.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LoginMethodCard(
    leading: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val colors = SunsetGitHubThemeTokens.colors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color(0x120F172A),
                spotColor = Color(0x160F172A)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, colors.border.copy(alpha = 0.55f)),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFE8F3FF), Color(0xFFDDF0FF))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = leading,
                    color = colors.accent,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = title,
                    color = colors.textPrimary,
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    modifier = Modifier.padding(top = 6.dp),
                    text = subtitle,
                    color = colors.textSecondary,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                )
            }
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(colors.accentSoft),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "›",
                    color = colors.accent,
                    fontSize = 32.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun LocalCredentialsNote() {
    val colors = SunsetGitHubThemeTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(colors.success)
        )
        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = stringResource(R.string.login_home_local_credentials_note),
            color = colors.textMuted,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RecommendedDeviceFlowBanner() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 26.dp, bottom = 20.dp),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xEB1E293B))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.login_home_recommend_device_flow_title),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = stringResource(R.string.login_home_recommend_device_flow_subtitle),
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
            Box(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Safe OAuth",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
