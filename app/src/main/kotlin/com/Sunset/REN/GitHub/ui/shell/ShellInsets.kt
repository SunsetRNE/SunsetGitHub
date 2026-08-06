package com.Sunset.REN.GitHub.ui.shell

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 壳 inset 单一来源（唯一消费 WindowInsets 的地方）。
 *
 * 约定（UI_SHELL_REDESIGN.md §6）：
 * - 系统 inset 只在壳消费一次，页面与组件零 inset 逻辑；
 * - TopBar 顶部 padding = statusBars；NavBar 底部 padding = navigationBars。
 *
 * 页面上只允许使用 [shellTopBarInsets] / [shellNavBarInsets]，
 * 禁止任何页面/组件自行读取 WindowInsets——杜绝手动 inset 同步漂移。
 */
object ShellInsets {
    /** 顶栏 inset：状态栏高度（顶部）。 */
    val top: WindowInsets
        @Composable
        get() = WindowInsets.statusBars

    /** 导航栏 inset：系统导航条高度（底部）。 */
    val bottom: WindowInsets
        @Composable
        get() = WindowInsets.navigationBars
}

/** 顶栏修饰符：消费状态栏 inset（壳内唯一）。 */
@Composable
fun Modifier.shellTopBarInsets(): Modifier = windowInsetsPadding(ShellInsets.top)

/** 导航栏修饰符：消费系统导航条 inset（壳内唯一）。 */
@Composable
fun Modifier.shellNavBarInsets(): Modifier = windowInsetsPadding(ShellInsets.bottom)