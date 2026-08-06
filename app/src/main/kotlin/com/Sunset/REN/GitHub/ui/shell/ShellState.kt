package com.Sunset.REN.GitHub.ui.shell

import com.Sunset.REN.GitHub.ui.schema.IconId

/**
 * 壳状态（一致组件模块化坐标构建法的壳层驱动）。
 *
 * 导航栏从"补丁式渲染"改为"壳状态驱动"：壳直接按 [navBarMode] 渲染
 * 对应导航组件，不再事后修补 XML 视图——状态切换不再产生漂移。
 */
data class ShellState(
    /** 顶栏标题。 */
    val title: String = "",
    /** 是否显示返回按钮（次级页面）。 */
    val showBack: Boolean = false,
    /** 返回按钮动作标识。 */
    val backAction: String = "shell.back",
    /** 顶栏右侧菜单项（图标动作）。 */
    val menuItems: List<ShellMenuItem> = emptyList(),
    /** 导航栏模式（壳唯一渲染依据）。 */
    val navBarMode: NavBarMode = NavBarMode.Main,
    /** 导航栏条目。 */
    val navItems: List<ShellNavItem> = emptyList(),
    /** 当前选中导航条目 id（高亮）。 */
    val selectedNavId: String = "",
    /** 内容区切换键：变化时内容区重建（页面切换）。 */
    val contentKey: String = "",
)

/** 导航栏模式：Main 主导航 / RepositorySections 仓库分段导航 / Hidden 隐藏 / Floating 悬浮（预留）。 */
enum class NavBarMode {
    Main,
    RepositorySections,
    Hidden,
    Floating,
}

/** 导航条目：{ id, label, icon, action }，固定字段。 */
data class ShellNavItem(
    val id: String,
    val label: String,
    val icon: IconId,
    val action: String,
)

/** 顶栏菜单项：{ id, icon, action }，固定字段。 */
data class ShellMenuItem(
    val id: String,
    val icon: IconId,
    val action: String,
)

/** 构造辅助：主导航条目。 */
fun shellNavItem(
    id: String,
    label: String,
    icon: IconId,
    action: String = "nav.$id",
) = ShellNavItem(id = id, label = label, icon = icon, action = action)
