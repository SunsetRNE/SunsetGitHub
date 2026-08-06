package com.Sunset.REN.GitHub.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens
import com.Sunset.REN.GitHub.ui.render.iconRes
import com.Sunset.REN.GitHub.ui.schema.IconId

/** 壳级固定高度（设计文档 §6：TopBar/NavBar 固定高度）。 */
private val ShellTopBarHeight = 56.dp
private val ShellNavBarHeight = 56.dp

/**
 * 应用壳：唯一布局权威（UI_SHELL_REDESIGN.md §6）。
 *
 * 三区 Column 硬约束：
 * - TopBar：固定高度（壳级），标题/返回/菜单由 [ShellState] 驱动；
 * - Content：weight(1f) → 高度 = maxHeight - TopBar - NavBar，
 *   页面 schema 渲染于此，物理上不可能越界；
 * - NavBar：固定高度（壳级），按 [ShellState.navBarMode] 渲染。
 *
 * inset 只在壳消费一次（[ShellInsets]），页面与组件零 inset 逻辑。
 */
@Composable
fun AppShell(
    state: ShellState,
    onAction: (String) -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = SunsetGitHubThemeTokens.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas),
    ) {
        ShellTopBar(state = state, onAction = onAction)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            content()
        }
        when (state.navBarMode) {
            NavBarMode.Main,
            NavBarMode.RepositorySections,
            -> ShellNavBar(state = state, onAction = onAction)

            // Floating：悬浮导航预留（阶段 6 实现，不占三区布局）。
            NavBarMode.Hidden,
            NavBarMode.Floating,
            -> Unit
        }
    }
}

// ---- 顶栏 ----

@Composable
private fun ShellTopBar(state: ShellState, onAction: (String) -> Unit) {
    val colors = SunsetGitHubThemeTokens.colors
    val spacing = SunsetGitHubThemeTokens.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ShellTopBarHeight)
            .background(colors.surface)
            .shellTopBarInsets()
            .padding(horizontal = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.showBack) {
            Icon(
                painter = painterResource(iconRes(IconId.Back)),
                contentDescription = "返回",
                tint = colors.textSecondary,
                modifier = Modifier
                    .size(spacing.xl)
                    .clickable { onAction(state.backAction) },
            )
            Spacer(Modifier.width(spacing.sm))
        }
        Text(
            text = state.title,
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        state.menuItems.forEach { menuItem ->
            Icon(
                painter = painterResource(iconRes(menuItem.icon)),
                contentDescription = menuItem.id,
                tint = colors.textSecondary,
                modifier = Modifier
                    .size(spacing.xl)
                    .clickable { onAction(menuItem.action) },
            )
            Spacer(Modifier.width(spacing.xs))
        }
    }
}

// ---- 导航栏 ----

@Composable
private fun ShellNavBar(state: ShellState, onAction: (String) -> Unit) {
    val colors = SunsetGitHubThemeTokens.colors
    val spacing = SunsetGitHubThemeTokens.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ShellNavBarHeight)
            .background(colors.surface)
            .shellNavBarInsets(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        state.navItems.forEach { navItem ->
            val selected = navItem.id == state.selectedNavId
            val tint = if (selected) colors.accent else colors.textSecondary
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onAction(navItem.action) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painter = painterResource(iconRes(navItem.icon)),
                    contentDescription = navItem.label,
                    tint = tint,
                    modifier = Modifier.size(spacing.lg),
                )
                Spacer(Modifier.height(spacing.xxs))
                Text(
                    text = navItem.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = tint,
                    maxLines = 1,
                )
            }
        }
    }
}