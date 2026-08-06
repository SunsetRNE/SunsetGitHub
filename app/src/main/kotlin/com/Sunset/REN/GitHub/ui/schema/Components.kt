package com.Sunset.REN.GitHub.ui.schema

import androidx.compose.ui.graphics.vector.ImageVector

// 该 import 由 IconResolver 使用（见 render 包）；schema 本身不依赖 ImageVector。
@Suppress("unused")
private typealias IconVector = ImageVector

/**
 * 文本组件：固定字段 { text, style, color, maxLines, ellipsis, action }。
 * 渲染层只解析这些字段，其余一律忽略。
 */
data class TextComponent(
    override val id: String,
    val text: String,
    val style: TextStyle = TextStyle.Body,
    val color: TextColor = TextColor.Primary,
    val maxLines: Int = Int.MAX_VALUE,
    val ellipsis: Boolean = false,
    override val action: String = "",
) : Component

/**
 * 按钮组件：固定字段 { text, kind, enabled, icon, action }。
 */
data class ButtonComponent(
    override val id: String,
    val text: String,
    val kind: ButtonKind = ButtonKind.Primary,
    val enabled: Boolean = true,
    val icon: IconId? = null,
    override val action: String = "",
) : Component

/**
 * 输入框组件：固定字段 { value, hint, singleLine, keyboard, onChange }。
 * onChange 为 Kotlin 壳内回调（字段固定）；跨进程场景由 action 路由。
 */
data class FieldComponent(
    override val id: String,
    val value: String,
    val hint: String = "",
    val singleLine: Boolean = true,
    val keyboard: FieldKeyboard = FieldKeyboard.Text,
    val onChange: ((String) -> Unit)? = null,
    override val action: String = "",
) : Component

/**
 * 图片组件：固定字段 { source, sizeDp, tint }。
 */
data class ImageComponent(
    override val id: String,
    val source: ImageSource,
    val sizeDp: Int = 20,
    val tint: TextColor? = null,
    override val action: String = "",
) : Component

/**
 * 空白占位组件：固定字段 { heightDp }（Weight 行内用 Cell.width 控制）。
 */
data class SpacerComponent(
    override val id: String,
    val heightDp: Int = 0,
    override val action: String = "",
) : Component

/**
 * 列表条目组件：固定字段 { title, subtitle, icon, badge, trailing, action }。
 * 与列表组件 [ListComponent] 配合，条目本身不持有布局参数。
 */
data class ItemComponent(
    override val id: String,
    val title: String,
    val subtitle: String? = null,
    val icon: IconId? = null,
    val badge: String? = null,
    val trailing: String? = null,
    override val action: String = "",
) : Component

/**
 * 列表组件：固定字段 { items }。items 元素为 [ItemComponent]。
 */
data class ListComponent(
    override val id: String,
    val items: List<ItemComponent>,
    override val action: String = "",
) : Component

/**
 * 分区标题组件：固定字段 { title, subtitle, actionText, action }。
 */
data class SectionHeaderComponent(
    override val id: String,
    val title: String,
    val subtitle: String? = null,
    val actionText: String? = null,
    override val action: String = "",
) : Component

/**
 * 状态组件：固定字段 { kind, message, detail, retryAction }。
 * 渲染判断由 kind 驱动：Loading 转圈 / Empty 空态 / Error 错误+重试。
 */
data class StateComponent(
    override val id: String,
    val kind: StateKind,
    val message: String = "",
    val detail: String? = null,
    val retryAction: String = "",
    override val action: String = "",
) : Component