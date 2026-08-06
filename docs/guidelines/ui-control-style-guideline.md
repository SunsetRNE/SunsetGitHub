# UI Control Style Guideline

## 批注

默认优先使用项目内已注册的内置统一控件样式；如果现有 Material/AppCompat 控件无法表达目标 UI，再额外创建可复用的全局统一控件或全局 style，并注册到 `Theme.AppTheme` 后统一使用。

## 当前统一控件

- Switch：`@style/Widget.SunsetGitHub.Material.Switch`
- CheckBox：`@style/Widget.SunsetGitHub.Material.CheckBox`
- RadioButton：`@style/Widget.SunsetGitHub.Material.RadioButton`
- Filter Chip：`@style/Widget.SunsetGitHub.Material.FilterChip`

## 使用原则

- XML 中优先使用 `SwitchMaterial`、`MaterialCheckBox`、`RadioButton`、`Chip` 并套用项目 style。
- Kotlin 动态创建控件时，也要显式套用同一套 tint / style 资源，避免回落到系统默认外观。
- 页面确有专用卡片式选择控件时，可以保留页面专用样式，但颜色应继续引用项目色板，避免硬编码不可适配的颜色。
- 不要在单个页面临时手写控件颜色；若出现第三种重复控件样式，应沉淀为全局 style 或统一 helper。
