package com.Sunset.REN.GitHub.ui.compose.screens.debug

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens
import com.Sunset.REN.GitHub.ui.compose.components.SunsetCard
import com.Sunset.REN.GitHub.ui.compose.components.SunsetPrimaryButton
import com.Sunset.REN.GitHub.ui.compose.components.SunsetSecondaryButton

@Composable
fun AppLogScreen(
    logText: String,
    onRefresh: () -> String,
    modifier: Modifier = Modifier
) {
    val colors = SunsetGitHubThemeTokens.colors
    val context = LocalContext.current
    var currentLogText by remember(logText) { mutableStateOf(logText.ifBlank { "暂无日志。" }) }
    Surface(modifier = modifier.fillMaxSize(), color = colors.canvas) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "应用日志",
                color = colors.textPrimary,
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "用于排查 UI 渲染、导航栏边界和运行时异常。复制后可直接发给开发者。",
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SunsetPrimaryButton(
                    text = "复制日志",
                    onClick = { copyLogToClipboard(context, currentLogText) },
                    modifier = Modifier.weight(1f),
                    enabled = currentLogText.isNotBlank() && currentLogText != "暂无日志。"
                )
                SunsetSecondaryButton(
                    text = "刷新",
                    onClick = { currentLogText = onRefresh().ifBlank { "暂无日志。" } },
                    modifier = Modifier.weight(1f)
                )
            }
            SunsetCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val verticalScroll = rememberScrollState()
                val horizontalScroll = rememberScrollState()
                Text(
                    text = currentLogText,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.canvas)
                        .horizontalScroll(horizontalScroll)
                        .verticalScroll(verticalScroll)
                        .padding(10.dp),
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

private fun copyLogToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("SunsetGitHub logs", text))
    Toast.makeText(context, "日志已复制", Toast.LENGTH_SHORT).show()
}
