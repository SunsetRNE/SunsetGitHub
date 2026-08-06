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
 * 列表条目组件：固定字段 { title, subtitle, description, meta, languageBar, icon, badge, trailing, actions, action }。
 * - [description]：标题区下方的第二行描述（可选，仓库卡片等富条目使用）；
 * - [meta]：条目底部 meta 行片段（可选，如语言/★/Fork/Issue/时间，以 " · " 连接）；
 * - [languageBar]：条目内嵌语言色条（可选，组合而非布局参数，渲染在 meta 下方）；
 * - [actions]：行内图标动作（可选，如置顶/收藏，渲染在 trailing 左侧）。
 * 与列表组件 [ListComponent] 配合，条目本身不持有布局参数。
 */
data class ItemComponent(
    override val id: String,
    val title: String,
    val subtitle: String? = null,
    val description: String? = null,
    val meta: List<String> = emptyList(),
    val languageBar: LanguageBarComponent? = null,
    val icon: IconId? = null,
    val badge: String? = null,
    val trailing: String? = null,
    val actions: List<ItemAction> = emptyList(),
    override val action: String = "",
) : Component

/** 条目行内图标动作（如置顶/收藏）：{ id, icon, contentDescription, active, action }。 */
data class ItemAction(
    val id: String,
    val icon: IconId,
    val contentDescription: String = "",
    val active: Boolean = false,
    val action: String = "",
)

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

/**
 * 骨架组件：Loading 占位（列表骨架卡片序列）。
 * 固定字段 { rows, compact }，渲染判断由字段驱动。
 */
data class SkeletonComponent(
    override val id: String,
    /** 骨架行数。 */
    val rows: Int = 5,
    /** 精简模式（隐藏描述行）。 */
    val compact: Boolean = false,
    override val action: String = "",
) : Component

/**
 * 语言色条组件：GitHub 仓库语言分布条（3dp 分段条）。
 * 固定字段 { segments, fallback }；颜色由渲染层 [languageColor] 映射，
 * schema 只承载 (name, percentage) 纯数据。
 */
data class LanguageBarComponent(
    override val id: String,
    val segments: List<LanguageSegment> = emptyList(),
    /** 无 segments 时的兜底语言名。 */
    val fallback: String? = null,
    override val action: String = "",
) : Component

/** 语言段：{ name, percentage }，百分比 > 0 参与分段。 */
data class LanguageSegment(
    val name: String,
    val percentage: Float,
)

/**
 * 浮层菜单组件：下拉菜单（排序/筛选）。
 * 固定字段 { triggerIcon, items, expanded, toggleAction, dismissAction }；
 * expanded 受控（页面状态驱动），点击触发项 → onAction(toggleAction)，
 * 菜单项选中 → onAction(item.action)，关闭 → onAction(dismissAction)。
 */
data class DropdownMenuComponent(
    override val id: String,
    val triggerIcon: IconId = IconId.Sort,
    val triggerContentDescription: String = "菜单",
    val items: List<MenuItemComponent> = emptyList(),
    /** 菜单是否展开（受控，页面状态驱动）。 */
    val expanded: Boolean = false,
    /** 点击触发器 → 页面切换 expanded 状态。 */
    val toggleAction: String = "",
    /** 菜单关闭（dismiss）→ 页面回写状态。 */
    val dismissAction: String = "",
    override val action: String = "",
) : Component

/** 菜单项：{ label, selected, action }。 */
data class MenuItemComponent(
    val label: String,
    val selected: Boolean = false,
    val action: String = "",
)

/**
 * 开关组件：固定字段 { title, description, checked, action }。
 * checked 受控（页面状态回写）；点击切换 → onAction(action)，由页面翻转状态。
 */
data class SwitchComponent(
    override val id: String,
    val title: String,
    val description: String? = null,
    val checked: Boolean = false,
    override val action: String = "",
) : Component