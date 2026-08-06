package com.Sunset.REN.GitHub.ui.render

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens
import com.Sunset.REN.GitHub.ui.schema.ButtonComponent
import com.Sunset.REN.GitHub.ui.schema.ButtonKind
import com.Sunset.REN.GitHub.ui.schema.Component
import com.Sunset.REN.GitHub.ui.schema.DropdownMenuComponent
import com.Sunset.REN.GitHub.ui.schema.FieldComponent
import com.Sunset.REN.GitHub.ui.schema.FieldKeyboard
import com.Sunset.REN.GitHub.ui.schema.IconId
import com.Sunset.REN.GitHub.ui.schema.ImageComponent
import com.Sunset.REN.GitHub.ui.schema.ImageSource
import com.Sunset.REN.GitHub.ui.schema.ItemAction
import com.Sunset.REN.GitHub.ui.schema.ItemComponent
import com.Sunset.REN.GitHub.ui.schema.LanguageBarComponent
import com.Sunset.REN.GitHub.ui.schema.LanguageSegment
import com.Sunset.REN.GitHub.ui.schema.ListComponent
import com.Sunset.REN.GitHub.ui.schema.MenuItemComponent
import com.Sunset.REN.GitHub.ui.schema.SectionHeaderComponent
import com.Sunset.REN.GitHub.ui.schema.SkeletonComponent
import com.Sunset.REN.GitHub.ui.schema.SpacerComponent
import com.Sunset.REN.GitHub.ui.schema.StateComponent
import com.Sunset.REN.GitHub.ui.schema.StateKind
import com.Sunset.REN.GitHub.ui.schema.TextColor
import com.Sunset.REN.GitHub.ui.schema.TextComponent
import com.Sunset.REN.GitHub.ui.schema.TextStyle

/**
 * 组件渲染器：渲染判断的唯一实现。
 *
 * - 按组件类型分发，组件只解析自身固定字段；
 * - 布局参数（宽高/间距）由坐标层注入，本层不感知；
 * - 交互统一走 [onAction]（action 标识符路由）。
 */
@Composable
fun Component.render(onAction: (String) -> Unit) {
    when (this) {
        is TextComponent -> renderText(this, onAction)
        is ButtonComponent -> renderButton(this, onAction)
        is FieldComponent -> renderField(this)
        is ImageComponent -> renderImage(this, onAction)
        is SpacerComponent -> Spacer(modifier = Modifier.height(heightDp.dp))
        is SectionHeaderComponent -> renderSectionHeader(this, onAction)
        is ItemComponent -> renderItem(this, onAction)
        is ListComponent -> Column {
            items.forEach { item -> renderItem(item, onAction) }
        }
        is StateComponent -> renderState(this, onAction)
        is SkeletonComponent -> renderSkeleton(this)
        is LanguageBarComponent -> renderLanguageBar(this)
        is DropdownMenuComponent -> renderDropdownMenu(this, onAction)
    }
}

// ---- 文本 ----

@Composable
private fun textStyle(style: TextStyle) = when (style) {
    TextStyle.Title -> MaterialTheme.typography.titleMedium
    TextStyle.Subtitle -> MaterialTheme.typography.titleSmall
    TextStyle.Body -> MaterialTheme.typography.bodyMedium
    TextStyle.Meta -> MaterialTheme.typography.bodySmall
    TextStyle.Caption -> MaterialTheme.typography.labelSmall
    TextStyle.Section -> MaterialTheme.typography.titleLarge
    TextStyle.Code -> MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
}

@Composable
private fun textColor(color: TextColor): Color = when (color) {
    TextColor.Primary -> SunsetGitHubThemeTokens.colors.textPrimary
    TextColor.Secondary -> SunsetGitHubThemeTokens.colors.textSecondary
    TextColor.Muted -> SunsetGitHubThemeTokens.colors.textMuted
    TextColor.Accent -> SunsetGitHubThemeTokens.colors.accent
    TextColor.Danger -> SunsetGitHubThemeTokens.colors.danger
    TextColor.Success -> SunsetGitHubThemeTokens.colors.success
    TextColor.OnAccent -> MaterialTheme.colorScheme.onPrimary
}

@Composable
private fun renderText(component: TextComponent, onAction: (String) -> Unit) {
    val base = Modifier
        .let { if (component.action.isNotEmpty()) it.clickable { onAction(component.action) } else it }
    Text(
        text = component.text,
        style = textStyle(component.style),
        color = textColor(component.color),
        maxLines = component.maxLines,
        overflow = if (component.ellipsis) TextOverflow.Ellipsis else TextOverflow.Clip,
        modifier = base,
    )
}

// ---- 按钮 ----

@Composable
private fun renderButton(component: ButtonComponent, onAction: (String) -> Unit) {
    val colors = SunsetGitHubThemeTokens.colors
    val spacing = SunsetGitHubThemeTokens.spacing
    val onClick = { onAction(component.action) }
    val content: @Composable () -> Unit = {
        if (component.icon != null) {
            Icon(
                painter = painterResource(iconRes(component.icon)),
                contentDescription = null,
                modifier = Modifier.size(spacing.md),
            )
            Spacer(Modifier.width(spacing.sm))
        }
        Text(component.text)
    }
    when (component.kind) {
        ButtonKind.Primary -> Button(
            onClick = onClick,
            enabled = component.enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.accent,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) { content() }

        ButtonKind.Secondary -> OutlinedButton(
            onClick = onClick,
            enabled = component.enabled,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.accent),
        ) { content() }

        ButtonKind.Ghost -> TextButton(
            onClick = onClick,
            enabled = component.enabled,
            colors = ButtonDefaults.textButtonColors(contentColor = colors.accent),
        ) { content() }

        ButtonKind.Danger -> Button(
            onClick = onClick,
            enabled = component.enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.danger,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) { content() }
    }
}

// ---- 输入框 ----

private fun keyboardOptions(keyboard: FieldKeyboard) =
    androidx.compose.foundation.text.KeyboardOptions(
        keyboardType = when (keyboard) {
            FieldKeyboard.Text -> androidx.compose.ui.text.input.KeyboardType.Text
            FieldKeyboard.Number -> androidx.compose.ui.text.input.KeyboardType.Number
            FieldKeyboard.Url -> androidx.compose.ui.text.input.KeyboardType.Uri
            FieldKeyboard.Email -> androidx.compose.ui.text.input.KeyboardType.Email
            FieldKeyboard.Password -> androidx.compose.ui.text.input.KeyboardType.Password
        },
    )

@Composable
private fun renderField(component: FieldComponent) {
    // onChange 缺失时退化为本地状态（只读展示场景）
    var localValue by remember(component.id) { mutableStateOf(component.value) }
    val value = component.onChange?.let { component.value } ?: localValue
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            component.onChange?.invoke(newValue) ?: run { localValue = newValue }
        },
        singleLine = component.singleLine,
        placeholder = if (component.hint.isNotEmpty()) {
            { Text(component.hint, color = SunsetGitHubThemeTokens.colors.textMuted) }
        } else null,
        keyboardOptions = keyboardOptions(component.keyboard),
        textStyle = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.fillMaxWidth(),
    )
}

// ---- 图片 ----

@DrawableRes
internal fun iconRes(icon: IconId): Int = when (icon) {
    IconId.Search -> R.drawable.ic_search_24
    IconId.Sort -> R.drawable.ic_sort_24
    IconId.Close -> R.drawable.ic_close_24
    IconId.Back -> R.drawable.ic_file_manager_arrow_back_24
    IconId.Refresh -> R.drawable.ic_refresh_24
    IconId.More -> R.drawable.ic_file_manager_more_vert_24
    IconId.Star -> R.drawable.ic_star_outline_24
    IconId.Fork -> R.drawable.ic_fork_24
    IconId.Eye -> R.drawable.ic_visibility_24
    IconId.Issue -> R.drawable.ic_cancel_circle_24
    IconId.PullRequest -> R.drawable.ic_branch_24
    IconId.Folder -> R.drawable.ic_folder_24
    IconId.File -> R.drawable.ic_file_24
    IconId.Image -> R.drawable.ic_mt_image_28
    IconId.Code -> R.drawable.ic_code_24
    IconId.Archive -> R.drawable.ic_file_manager_archive_24
    IconId.Settings -> R.drawable.ic_settings_black_24dp
    IconId.Person -> R.drawable.ic_people_24
    IconId.Home -> R.drawable.ic_dashboard_black_24dp
    IconId.Bell -> R.drawable.ic_notifications_black_24dp
    IconId.Terminal -> R.drawable.ic_file_manager_terminal_24
    IconId.Cloud -> R.drawable.ic_sync_24
    IconId.Warning -> R.drawable.ic_block_24
    IconId.Error -> R.drawable.ic_error_24
    IconId.Check -> R.drawable.ic_check_circle_24
}

@Composable
private fun renderImage(component: ImageComponent, onAction: (String) -> Unit) {
    val base = Modifier
        .size(component.sizeDp.dp)
        .let { if (component.action.isNotEmpty()) it.clickable { onAction(component.action) } else it }
    when (val source = component.source) {
        is ImageSource.Icon -> {
            val tint = component.tint?.let { textColor(it) }
            Icon(
                painter = painterResource(iconRes(source.icon)),
                contentDescription = null,
                tint = tint ?: Color.Unspecified,
                modifier = base,
            )
        }
        // 远程图（头像等）：无网络加载依赖，先用占位图标，阶段 6 接 Coil
        is ImageSource.Remote -> Icon(
            painter = painterResource(R.drawable.ic_people_24),
            contentDescription = null,
            tint = SunsetGitHubThemeTokens.colors.textMuted,
            modifier = base,
        )
    }
}

// ---- 列表条目 ----

@Composable
private fun renderItem(component: ItemComponent, onAction: (String) -> Unit) {
    val colors = SunsetGitHubThemeTokens.colors
    val spacing = SunsetGitHubThemeTokens.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (component.action.isNotEmpty()) it.clickable { onAction(component.action) } else it }
            .padding(vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (component.icon != null) {
            Icon(
                painter = painterResource(iconRes(component.icon)),
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(spacing.lg),
            )
            Spacer(Modifier.width(spacing.md))
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = component.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!component.badge.isNullOrBlank()) {
                    Spacer(Modifier.width(spacing.sm))
                    Text(
                        text = component.badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.accent,
                        maxLines = 1,
                    )
                }
            }
            if (!component.subtitle.isNullOrBlank()) {
                Text(
                    text = component.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!component.description.isNullOrBlank()) {
                Text(
                    text = component.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = spacing.xxs),
                )
            }
            if (component.meta.isNotEmpty()) {
                Text(
                    text = component.meta.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = spacing.xxs),
                )
            }
        }
        component.actions.forEach { itemAction ->
            Spacer(Modifier.width(spacing.sm))
            Icon(
                painter = painterResource(iconRes(itemAction.icon)),
                contentDescription = itemAction.contentDescription,
                tint = if (itemAction.active) colors.accent else colors.textMuted,
                modifier = Modifier
                    .size(spacing.lg)
                    .clickable { onAction(itemAction.action) },
            )
        }
        if (!component.trailing.isNullOrBlank()) {
            Spacer(Modifier.width(spacing.md))
            Text(
                text = component.trailing,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
                maxLines = 1,
            )
        }
    }
}

// ---- 分区标题 ----

@Composable
private fun renderSectionHeader(component: SectionHeaderComponent, onAction: (String) -> Unit) {
    val colors = SunsetGitHubThemeTokens.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = component.title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
            if (!component.subtitle.isNullOrBlank()) {
                Text(
                    text = component.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }
        if (!component.actionText.isNullOrBlank()) {
            TextButton(onClick = { onAction(component.action) }) {
                Text(component.actionText, color = colors.accent)
            }
        }
    }
}

// ---- 状态组件 ----

@Composable
private fun renderState(component: StateComponent, onAction: (String) -> Unit) {
    val colors = SunsetGitHubThemeTokens.colors
    val spacing = SunsetGitHubThemeTokens.spacing
    when (component.kind) {
        StateKind.Loading -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.xxl),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = colors.accent)
        }

        StateKind.Empty -> Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_folder_24),
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(spacing.xxl),
            )
            Text(
                text = component.message.ifBlank { "暂无内容" },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            if (!component.detail.isNullOrBlank()) {
                Text(
                    text = component.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }

        StateKind.Error -> Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_error_24),
                contentDescription = null,
                tint = colors.danger,
                modifier = Modifier.size(spacing.xxl),
            )
            Text(
                text = component.message.ifBlank { "出错了" },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            if (!component.detail.isNullOrBlank()) {
                Text(
                    text = component.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center,
                )
            }
            if (component.retryAction.isNotEmpty()) {
                TextButton(onClick = { onAction(component.retryAction) }) {
                    Text("重试", color = colors.accent)
                }
            }
        }
    }
}

// ---- 骨架 ----

@Composable
private fun renderSkeleton(component: SkeletonComponent) {
    val spacing = SunsetGitHubThemeTokens.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        repeat(component.rows) { index ->
            SkeletonCard(compact = component.compact || index >= 3)
        }
    }
}

@Composable
private fun SkeletonCard(compact: Boolean) {
    val colors = SunsetGitHubThemeTokens.colors
    val spacing = SunsetGitHubThemeTokens.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(14.dp))
            .padding(horizontal = spacing.md, vertical = spacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SkeletonBlock(widthFraction = if (compact) 0.62f else 0.82f, height = 18.dp)
            Spacer(Modifier.weight(1f))
            SkeletonBlock(widthFraction = 0.12f, height = 18.dp)
        }
        Spacer(Modifier.height(spacing.sm))
        if (!compact) {
            SkeletonBlock(widthFraction = 0.68f, height = 12.dp)
            Spacer(Modifier.height(spacing.xs))
        }
        SkeletonBlock(widthFraction = 1f, height = 3.dp)
        Spacer(Modifier.height(spacing.sm))
        Row {
            SkeletonBlock(widthFraction = 0.16f, height = 11.dp)
            Spacer(Modifier.width(spacing.sm))
            SkeletonBlock(widthFraction = 0.12f, height = 11.dp)
            Spacer(Modifier.width(spacing.sm))
            SkeletonBlock(widthFraction = 0.14f, height = 11.dp)
        }
    }
}

@Composable
private fun SkeletonBlock(widthFraction: Float, height: Dp) {
    val colors = SunsetGitHubThemeTokens.colors
    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction.coerceIn(0.05f, 1f))
            .height(height)
            .background(colors.chipBackground, RoundedCornerShape(99.dp)),
    )
}

// ---- 语言色条 ----

@Composable
private fun renderLanguageBar(component: LanguageBarComponent) {
    val colors = SunsetGitHubThemeTokens.colors
    val segments = languageBarSegments(component.segments, component.fallback)
    if (segments.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(colors.chipBackground, RoundedCornerShape(99.dp)),
    ) {
        segments.forEach { segment ->
            Box(
                modifier = Modifier
                    .weight(segment.weight)
                    .fillMaxHeight()
                    .background(segment.color),
            )
        }
    }
}

private data class LanguageBarSegment(
    val weight: Float,
    val color: Color,
)

private fun languageBarSegments(
    segments: List<LanguageSegment>,
    fallback: String?,
): List<LanguageBarSegment> {
    val known = segments
        .filter { it.percentage > 0 }
        .sortedByDescending { it.percentage }
        .take(4)
    if (known.isNotEmpty()) {
        return known.map { segment ->
            LanguageBarSegment(
                weight = segment.percentage.coerceAtLeast(1f).toFloat(),
                color = languageColor(segment.name),
            )
        }
    }
    return fallback
        ?.takeIf { it.isNotBlank() }
        ?.let { listOf(LanguageBarSegment(weight = 1f, color = languageColor(it))) }
        .orEmpty()
}

/** 语言 → 颜色（渲染层唯一实现，与现有 DashboardScreen 映射一致）。 */
internal fun languageColor(language: String): Color = when (language.trim().lowercase()) {
    "kotlin" -> Color(0xFFA97BFF)
    "java" -> Color(0xFFB07219)
    "python" -> Color(0xFF3572A5)
    "shell", "bash" -> Color(0xFF89E051)
    "javascript" -> Color(0xFFF1E05A)
    "typescript" -> Color(0xFF3178C6)
    "html" -> Color(0xFFE34C26)
    "css" -> Color(0xFF563D7C)
    "c" -> Color(0xFF555555)
    "c++", "cpp" -> Color(0xFFF34B7D)
    "go" -> Color(0xFF00ADD8)
    "rust" -> Color(0xFFDEA584)
    "ruby" -> Color(0xFF701516)
    "swift" -> Color(0xFFF05138)
    else -> Color(0xFF8C959F)
}

// ---- 浮层菜单 ----

@Composable
private fun renderDropdownMenu(component: DropdownMenuComponent, onAction: (String) -> Unit) {
    val colors = SunsetGitHubThemeTokens.colors
    Box {
        Icon(
            painter = painterResource(iconRes(component.triggerIcon)),
            contentDescription = component.triggerContentDescription,
            tint = colors.textSecondary,
            modifier = Modifier
                .size(24.dp)
                .clickable { onAction(component.toggleAction) },
        )
        DropdownMenu(
            expanded = component.expanded,
            onDismissRequest = { onAction(component.dismissAction) },
            containerColor = colors.surface,
            border = BorderStroke(1.dp, colors.border),
        ) {
            component.items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.label, color = colors.textPrimary) },
                    trailingIcon = if (item.selected) {
                        {
                            Icon(
                                painter = painterResource(R.drawable.ic_check_circle_24),
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    } else null,
                    onClick = { onAction(item.action) },
                )
            }
        }
    }
}