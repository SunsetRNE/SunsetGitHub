package com.Sunset.REN.GitHub.ui.layout

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.Sunset.REN.GitHub.ui.schema.Component

/**
 * 页面坐标 schema（一致组件模块化坐标构建法的布局层）。
 *
 * 页面 = 行序列（[RowSchema]），行内 = 单元序列（[CellSchema]）。
 * 每个单元由 (row, column, span) 唯一定位，组件渲染进该单元 bounds。
 * 组件不感知坐标，坐标由 [PageRenderer] 网格计算层解析。
 */
data class PageSchema(
    /** 页面唯一标识（导航键）。 */
    val id: String,
    /** 列数（Material 网格默认 12）。 */
    val columns: Int = 12,
    /** 纵向行序列。 */
    val rows: List<RowSchema>,
    /** 整页是否在壳内容区内滚动。 */
    val scrollable: Boolean = true,
)

/** 行高模式：Fixed 固定 dp / Weight 均分剩余 / Wrap 内容自适应。 */
sealed interface RowHeight {
    data class Fixed(val height: Dp) : RowHeight
    data class Weight(val weight: Float) : RowHeight
    data object Wrap : RowHeight
}

/** 行：固定高度模式 + 单元序列。 */
data class RowSchema(
    val height: RowHeight = RowHeight.Wrap,
    val cells: List<CellSchema>,
    /** 行内垂直 padding（dp）。 */
    val paddingVertical: Dp = 0.dp,
)

/** 单元宽度：Weight 占列权重 / Fixed 固定 dp / Wrap 内容自适应。 */
sealed interface CellWidth {
    data class Weight(val weight: Float) : CellWidth
    data class Fixed(val width: Dp) : CellWidth
    data object Wrap : CellWidth
}

/**
 * 单元：由 (column, span, width) 定位，承载一个组件。
 * column 为起始列（0 起），span 为占列数，width 为列内宽度模式。
 */
data class CellSchema(
    val column: Int = 0,
    val span: Int = 1,
    val width: CellWidth = CellWidth.Weight(1f),
    val component: Component,
)

/** 构造辅助：文本组件单元。 */
fun cell(
    component: Component,
    column: Int = 0,
    span: Int = 1,
    width: CellWidth = CellWidth.Weight(1f),
) = CellSchema(column = column, span = span, width = width, component = component)

/** 构造辅助：整行单单元。 */
fun row(
    vararg cells: CellSchema,
    height: RowHeight = RowHeight.Wrap,
) = RowSchema(height = height, cells = cells.toList())