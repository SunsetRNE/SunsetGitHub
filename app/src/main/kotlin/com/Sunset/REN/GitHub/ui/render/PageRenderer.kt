package com.Sunset.REN.GitHub.ui.render

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubThemeTokens
import com.Sunset.REN.GitHub.ui.layout.CellSchema
import com.Sunset.REN.GitHub.ui.layout.CellWidth
import com.Sunset.REN.GitHub.ui.layout.PageSchema
import com.Sunset.REN.GitHub.ui.layout.RowHeight
import com.Sunset.REN.GitHub.ui.layout.RowSchema

/**
 * 页面渲染器：坐标网格计算层（一致组件模块化坐标构建法的布局执行）。
 *
 * - [PageSchema.renderPage] 为页面渲染唯一入口（modifier 由壳内容区注入）；
 * - 行高 [RowHeight] 三态映射（Fixed→height / Weight→weight / Wrap→内容），
 *   需在 [ColumnScope] 内执行（weight 为 Column 作用域扩展）；
 * - 单元宽 [CellWidth] 三态映射（Weight→weight / Fixed→width / Wrap→内容），
 *   需在 [RowScope] 内执行；
 * - 组件渲染委托给 [Component.render]，坐标层不感知组件内部；
 * - 页面 scrollable 控制内容区壳内滚动（滚动容器内 Weight 行退化为内容高，
 *   Weight 行用于非滚动页面的填充场景）。
 */
@Composable
fun PageSchema.renderPage(
    onAction: (String) -> Unit,
    modifier: Modifier = Modifier,
    /** 是否填充父容器（默认 true 向后兼容）。混合布局页（schema + 原生编辑器）
     *  传 false：页面高度由内容决定，由调用端 Column 组合排版。 */
    fillMaxSize: Boolean = true,
) {
    val spacing = SunsetGitHubThemeTokens.spacing
    val scrollModifier = if (scrollable) {
        Modifier.verticalScroll(rememberScrollState())
    } else {
        Modifier
    }
    Column(
        modifier = modifier
            .then(if (fillMaxSize) Modifier.fillMaxSize() else Modifier)
            .then(scrollModifier),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.lg),
        ) {
            rows.forEach { row -> renderRow(row, onAction) }
        }
    }
}

@Composable
private fun ColumnScope.renderRow(row: RowSchema, onAction: (String) -> Unit) {
    val base = Modifier
        .fillMaxWidth()
        .padding(vertical = row.paddingVertical)
    val rowModifier = when (val height = row.height) {
        is RowHeight.Fixed -> base.height(height.height)
        is RowHeight.Weight -> base.weight(height.weight)
        RowHeight.Wrap -> base
    }
    Row(modifier = rowModifier) {
        row.cells.forEach { cell -> renderCell(cell, onAction) }
    }
}

@Composable
private fun RowScope.renderCell(cell: CellSchema, onAction: (String) -> Unit) {
    val cellModifier = when (val width = cell.width) {
        is CellWidth.Weight -> Modifier.weight(width.weight * cell.span)
        is CellWidth.Fixed -> Modifier.width(width.width)
        CellWidth.Wrap -> Modifier
    }
    Box(modifier = cellModifier) {
        cell.component.render(onAction)
    }
}