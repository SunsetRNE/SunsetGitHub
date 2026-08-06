package com.Sunset.REN.GitHub.ui.schema

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 组件根类型（一致组件模块化坐标构建法）。
 *
 * 约定：
 * - 每个组件是固定字段的纯数据 schema，不持有布局参数；
 * - 渲染判断完全由字段驱动（[ComponentRenderer] 按类型分发）；
 * - 交互通过 [action] 标识符（字符串）路由到页面处理器，
 *   schema 本身可序列化（未来 Rust 核心可直接下发页面 schema）。
 */
sealed interface Component {
    /** 组件唯一标识（列表 key / 测试断言）。 */
    val id: String
    /** 交互动作标识，空串表示无交互。 */
    val action: String
}

/** 文本样式（映射 MaterialTheme.typography）。 */
enum class TextStyle {
    Title,      // titleMedium
    Subtitle,   // titleSmall
    Body,       // bodyMedium
    Meta,       // bodySmall
    Caption,    // labelSmall
    Code,       // monospace bodySmall
    Section,    // titleLarge（分区标题）
}

/** 文本颜色（映射主题色板）。 */
enum class TextColor {
    Primary,    // textPrimary
    Secondary,  // textSecondary
    Muted,      // textMuted
    Accent,     // accent
    Danger,     // danger
    Success,    // success
    OnAccent,   // 强调色上的文字（按钮等）
}

/** 按钮样式。 */
enum class ButtonKind {
    Primary,    // 实心强调
    Secondary,  // 描边
    Ghost,      // 无边框文字按钮
    Danger,     // 实心危险
}

/** 图标（引用主题图标资源，渲染层解析为 ImageVector）。 */
enum class IconId {
    Search,
    Sort,
    Close,
    Back,
    Refresh,
    More,
    Star,
    Fork,
    Eye,
    Issue,
    PullRequest,
    Folder,
    File,
    Image,
    Code,
    Archive,
    Settings,
    Person,
    Home,
    Bell,
    Terminal,
    Cloud,
    Warning,
    Error,
    Check,
    Pin,
}

/** 图片加载方式：矢量图标或远程图（头像等）。 */
sealed interface ImageSource {
    data class Icon(val icon: IconId) : ImageSource
    data class Remote(val url: String) : ImageSource
}

/** 输入框键盘类型。 */
enum class FieldKeyboard {
    Text,
    Number,
    Url,
    Email,
    Password,
}

/** 状态组件类型。 */
enum class StateKind {
    Loading,
    Empty,
    Error,
}
