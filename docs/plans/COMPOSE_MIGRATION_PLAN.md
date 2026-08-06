# Jetpack Compose 优先迁移计划

## 背景

SunsetGitHub 当前是一个基于 Kotlin、AppCompat、AndroidX Navigation、Material Components、XML Layout 与 ViewBinding 的 Android 应用。项目已经具备较多页面和资源：主导航、登录、仓库浏览、仓库详情、文件管理器、文件预览、Markdown/代码预览、Issues、Profile、Settings 等功能均已在现有 XML/Fragment 架构中落地。

后续 UI 方向希望优先使用 Jetpack Compose，以提升新页面开发效率、统一视觉组件、加强交互和动效表现，并逐步让应用界面更精致。

本计划采用渐进式迁移策略：

> 新 UI 优先 Compose，旧 XML 页面保留兼容；通过 Fragment + ComposeView 逐步迁移，而不是一次性推倒重写。

## 总体原则

1. **Compose 优先**
   - 新页面、新弹窗、新 BottomSheet、新表单、新通用组件优先使用 Jetpack Compose。
   - 原则上不再新增大型 XML 页面，除非需要和旧页面快速兼容。

2. **保留现有导航壳**
   - 短期继续使用 `MainActivity` + `NavHostFragment` + `mobile_navigation.xml`。
   - 不在第一阶段迁移到 Navigation Compose。

3. **混合架构过渡**
   - 旧页面继续使用 XML + ViewBinding。
   - 新页面或逐步迁移页面通过 `ComposeView` 嵌入 Fragment。
   - 必要时 Compose 内可通过 `AndroidView` 嵌入已有 View 或第三方控件。

4. **主题统一优先于页面重写**
   - Compose 页面必须复用或映射现有 GitHub/Primer 风格颜色、字号、圆角、间距。
   - 避免出现 XML Material Components、Compose Material2、Compose Material3 三套割裂风格。
   - Compose 侧优先采用 Material3。

5. **先低风险、后高复杂度**
   - 先迁登录、设置、空状态、说明页、简单表单等低风险页面。
   - 暂缓迁移文件管理器、编辑器、预览器、仓库详情总壳等复杂页面。

6. **每一步都要可编译**
   - 每个阶段结束至少运行 `./gradlew assembleDebug`。
   - 涉及 Release 资源策略时再运行 `./gradlew assembleRelease` 或相关检查任务。

## 目标架构

短中期目标架构：

```text
MainActivity
└── NavHostFragment
    ├── XML Fragment + ViewBinding
    ├── XML Fragment + ViewBinding
    ├── Fragment + ComposeView
    ├── Fragment + ComposeView
    └── Compose component embedded in existing XML page
```

长期可选目标：

```text
MainActivity
└── Compose Navigation / Hybrid Navigation
    ├── Compose Screen
    ├── Compose Screen
    ├── AndroidView wrapper for editor / preview
    └── compatibility fragment host if still needed
```

长期是否迁移到纯 Compose Navigation，需要在多个页面稳定迁移后再评估，不作为第一阶段目标。

## 阶段 A：Compose 基础设施接入

### 目标

让项目具备编写、预览和嵌入 Compose UI 的基础能力，但不改变现有业务页面行为。

### 主要改动

1. 修改 `gradle/libs.versions.toml`
   - 添加 Compose BOM。
   - 添加 Compose Material3、UI、Foundation、Runtime、Preview、Tooling 等依赖。
   - 添加 Kotlin Compose Compiler Gradle 插件。

2. 修改 `app/build.gradle.kts`
   - 启用 Compose：

```kotlin
buildFeatures {
    viewBinding = true
    buildConfig = true
    compose = true
}
```

   - 添加 Compose 相关依赖。
   - Kotlin 当前为 `2.1.0`，优先使用 Kotlin Compose 插件，而不是旧式 `composeOptions.kotlinCompilerExtensionVersion`。

3. 新增 Compose 基础包：

```text
app/src/main/kotlin/com/Sunset/REN/GitHub/ui/compose/
├── SunsetGitHubTheme.kt
├── SunsetGitHubColors.kt
├── SunsetGitHubTypography.kt
├── SunsetGitHubSpacing.kt
└── SunsetGitHubPreview.kt
```

4. 建立最小主题封装：
   - `SunsetGitHubTheme { ... }`
   - 映射现有 `colors.xml` / `colors_primer.xml` 中的核心颜色。
   - 建立基础 Typography、Shapes、Spacing。

5. 新增少量基础组件：

```text
app/src/main/kotlin/com/Sunset/REN/GitHub/ui/compose/components/
├── SunsetCard.kt
├── SunsetButton.kt
├── SunsetSectionHeader.kt
├── SunsetEmptyState.kt
└── SunsetLoadingState.kt
```

### 验证

```bash
./gradlew assembleDebug
```

### 交付标准

- 项目成功编译。
- 现有页面行为不变。
- 可以在 Fragment 中使用：

```kotlin
ComposeView(requireContext()).apply {
    setContent {
        SunsetGitHubTheme {
            // Compose UI
        }
    }
}
```

## 阶段 B：低风险页面试点

### 推荐优先级

1. 登录首页
2. Token 登录选择页
3. Token 权限说明/权限审查页
4. 设置页中的独立区块或子页
5. 通用空状态、错误状态、加载状态

### 原因

这些页面通常：

- 业务状态较少；
- 和文件系统、编辑器、复杂列表耦合较低；
- 视觉提升明显；
- 适合验证 Compose 主题、组件、间距和状态管理方式。

### 建议迁移方式

保留现有 Fragment 类和导航 ID，将 Fragment 的 `onCreateView` 切换为 `ComposeView`，例如：

```kotlin
override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
): View {
    return ComposeView(requireContext()).apply {
        setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        setContent {
            SunsetGitHubTheme {
                LoginHomeScreen(
                    onDeviceFlowClick = { findNavController().navigate(...) },
                    onTokenClick = { findNavController().navigate(...) }
                )
            }
        }
    }
}
```

### 验证

```bash
./gradlew assembleDebug
```

并手动检查：

- 登录入口是否可点击；
- 导航是否正常；
- 返回栈是否正常；
- 深浅色主题是否可接受；
- 字体、间距、按钮和旧页面是否风格接近。

## 阶段 C：通用组件沉淀

### 目标

把页面迁移过程中重复出现的视觉元素沉淀为可复用 Compose 组件，避免每个页面各写一套。

### 推荐组件

```text
ui/compose/components/
├── SunsetTopBar.kt
├── SunsetRepositoryCard.kt
├── SunsetFileRow.kt
├── SunsetIssueCard.kt
├── SunsetProfileHeader.kt
├── SunsetPermissionNotice.kt
├── SunsetErrorState.kt
├── SunsetTextField.kt
├── SunsetFilterChip.kt
├── SunsetListItem.kt
└── SunsetBottomSheetScaffold.kt
```

### 组件设计原则

- 组件参数应表达业务意图，而不是暴露过多 UI 细节。
- 文案继续使用 string resources，不在 Composable 中硬编码大量中文文案。
- 图标、颜色、间距从统一主题或 token 获取。
- 列表类组件优先支持 loading / empty / error 三态。

## 阶段 D：中等复杂度页面迁移

### 推荐顺序

1. Profile 页面
2. Notifications 页面
3. Dashboard 仓库列表卡片
4. Repository card / Issue card
5. 搜索 UI
6. 仓库详情中的独立 Section

### 注意事项

- 列表优先使用 `LazyColumn` / `LazyVerticalGrid`。
- 需要分页、刷新、错误重试时，先定义统一状态模型。
- 旧的 RecyclerView Adapter 可以先保留，逐块替换。
- 迁移 Dashboard 时可以先替换 item/card，不必一次重写整个页面。

## 阶段 E：复杂页面迁移

### 暂缓迁移对象

这些页面复杂度高，不建议在 Compose 基础设施刚接入时迁移：

- 本地文件管理器
- 文件预览
- 代码编辑器
- Markdown 预览
- PDF 预览
- 仓库详情总壳
- 仓库文件上传/编辑完整流程

### 迁移策略

1. 先抽离状态模型和 UI state。
2. 再迁移列表、工具栏、空状态、错误状态等外围 UI。
3. 对编辑器、PDF、Markdown 等第三方 View，优先用 `AndroidView` 包裹。
4. 最后再评估是否完全 Compose 化。

## 建议新增的包结构

```text
app/src/main/kotlin/com/Sunset/REN/GitHub/ui/compose/
├── SunsetGitHubTheme.kt
├── SunsetGitHubColors.kt
├── SunsetGitHubTypography.kt
├── SunsetGitHubSpacing.kt
├── SunsetGitHubShapes.kt
├── SunsetGitHubPreview.kt
├── components/
│   ├── SunsetButton.kt
│   ├── SunsetCard.kt
│   ├── SunsetSectionHeader.kt
│   ├── SunsetEmptyState.kt
│   ├── SunsetLoadingState.kt
│   ├── SunsetErrorState.kt
│   ├── SunsetTextField.kt
│   ├── SunsetRepositoryCard.kt
│   ├── SunsetFileRow.kt
│   └── SunsetPermissionNotice.kt
└── screens/
    ├── auth/
    ├── settings/
    ├── profile/
    ├── notifications/
    └── repo/
```

## Gradle 依赖草案

以下只是草案，实际版本应写入 `gradle/libs.versions.toml` 并通过 Version Catalog 引用；版本号以当前 Version Catalog 为准。

```toml
[versions]
composeBom = "2025.10.00"

[plugins]
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }

[libraries]
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-foundation = { group = "androidx.compose.foundation", name = "foundation" }
androidx-compose-runtime = { group = "androidx.compose.runtime", name = "runtime" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version = "1.11.0" }
```

`app/build.gradle.kts` 依赖草案：

```kotlin
implementation(platform(libs.androidx.compose.bom))
implementation(libs.androidx.compose.ui)
implementation(libs.androidx.compose.ui.tooling.preview)
implementation(libs.androidx.compose.foundation)
implementation(libs.androidx.compose.runtime)
implementation(libs.androidx.compose.material3)
implementation(libs.androidx.activity.compose)
debugImplementation(libs.androidx.compose.ui.tooling)
```

## 风险与控制

### 风险 1：包体和编译时间增加

控制方式：

- 先只接入必要 Compose 依赖。
- 避免同时引入 Material2 和 Material3。
- 每阶段跑 `assembleDebug` 观察构建变化。

### 风险 2：视觉风格割裂

控制方式：

- 先做 `SunsetGitHubTheme`。
- Compose 页面统一使用项目主题 token。
- 不允许每个页面私自定义一套颜色和间距。

### 风险 3：Fragment 生命周期和 Compose 生命周期混乱

控制方式：

- `ComposeView` 必须设置：

```kotlin
setViewCompositionStrategy(
    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
)
```

- UI 状态继续遵循 Fragment/ViewModel 生命周期。

### 风险 4：复杂页面迁移失控

控制方式：

- 文件管理器、编辑器、预览器延后迁移。
- 先迁低风险页面。
- 每次迁移只改一个页面或一个组件族。

### 风险 5：资源体系重复

控制方式：

- 用户可见文案仍然优先使用 Android string resources。
- 颜色、字号、间距从 Compose token 和现有资源映射。
- 不在 Composable 中大量硬编码正式文案。

## 第一刀建议任务清单

- [x] 在 `gradle/libs.versions.toml` 添加 Compose 版本、插件和依赖。
- [x] 在 `app/build.gradle.kts` 启用 Compose 并添加依赖。
- [x] 新建 `ui/compose` 包。
- [x] 实现 `SunsetGitHubTheme`。
- [x] 实现 `SunsetCard`、`SunsetButton`、`SunsetEmptyState`、`SunsetLoadingState`。
- [x] 新建一个仅用于编译验证的简单 Composable preview。
- [x] 运行 `./gradlew assembleDebug`。
- [x] 若成功，再选择登录页或设置页作为第一个实际迁移页面。

## 第一批实际迁移候选

优先级从高到低：

1. `LoginHomeFragment`（已迁移到 ComposeView）
2. `TokenLoginChoiceFragment`（已迁移到 ComposeView）
3. `TokenGuideFragment`（已迁移到 ComposeView）
4. `TokenPermissionReviewFragment`（已迁移到 ComposeView）
5. `SettingsFragment`（已迁移到 ComposeView）
6. 通用 empty/loading/error 状态视图（已补齐 `SunsetEmptyState`、`SunsetLoadingState`、`SunsetErrorState`）

补充：`DeviceFlowIntroFragment` 和 `DeviceFlowCodeFragment` 也已随认证低风险页面一起迁移到 ComposeView。认证 Compose screen 已拆分为 `LoginHomeScreen.kt`、`DeviceFlowScreens.kt`、`TokenScreens.kt` 和 `AuthSharedComponents.kt`。

## 暂不迁移清单

以下内容等 Compose 基础设施稳定后再处理完整页面迁移；但允许先迁移 Dialog、确认弹窗、表单弹窗、空状态等外围交互，作为低风险过渡切片：

- `LocalFileManagerFragment`
- `LocalFilePreviewFragment`
- 代码编辑器相关页面
- PDF/Markdown/图片预览相关页面
- 仓库详情总壳
- 仓库文件上传/编辑完整流程

当前仓库子页 Dialog 迁移优先级：

1. 先清理单一确认类弹窗，如 `RepositoryFileUploadFragment` 文件冲突确认。
2. 再清理说明/确认类弹窗，如 Agents / Official workflow 说明。
3. Settings 子页继续把 Fragment 内表单/确认弹窗收敛到对应 Compose Screen。
4. `RepositoryFileEditFragment` 弹窗数量多且混合编辑器工具、提交、Diff、冲突确认，应拆成多个小切片最后推进。

当前进度快照：

- 仓库设置主页面与 `RepositoryActionsSettingsFragment`、`RepositoryBranchSettingsFragment`、`RepositoryCollaboratorsSettingsFragment`、`RepositoryDangerZoneFragment` 已使用 `ComposeView + Repository*Screen` 渲染主体。
- `RepositorySettingsFragment` 的编辑字段弹窗已从 `MaterialAlertDialogBuilder + TextInputLayout` 迁移到 Compose `AlertDialog + OutlinedTextField`。
- `RepositorySettingsFragment` 的可见性变更确认与归档确认已从 `CompactBlackDialog` 迁移到 Compose `AlertDialog`。
- `RepositoryActionsSettingsFragment` 的多类确认/编辑弹窗已收敛到 Compose DialogHost。
- `RepositoryIssuesFragment` 的创建者筛选与标签筛选已使用 Compose `AlertDialog`。
- `RepositoryIssueDetailFragment` 的编辑评论与标签选择已使用 Compose `AlertDialog`。
- `RepositoryFileUploadFragment` 文件冲突确认已迁移到 Compose `AlertDialog` DialogHost。
- `RepositoryFileEditFragment` 编辑器工具/提交/Diff 等复杂弹窗仍按暂缓策略保留旧实现。

## 验证记录

初始计划写入时尚未修改代码。上一轮完整 Debug 编译结果：

```text
./gradlew assembleDebug
BUILD SUCCESSFUL in 1m 8s
39 actionable tasks: 3 executed, 36 up-to-date
```

后续每次执行 Compose 迁移任务后，应在此补充新的验证记录。

### 2026-07-21 阶段 A：Compose 基础设施接入

已完成：

- 接入 Kotlin Compose Compiler Gradle 插件。
- 启用 `buildFeatures.compose = true`，保留 ViewBinding。
- 添加 Compose BOM、UI、Foundation、Runtime、Material3、Tooling Preview、Activity Compose 依赖。
- 新增 `ui/compose` 主题包与首批基础组件。
- 未迁移现有业务页面，现有导航与 XML 页面保持不变。

验证命令：

```bash
./gradlew assembleDebug
```

验证结果：

```text
BUILD SUCCESSFUL in 6m 27s
41 actionable tasks: 17 executed, 24 up-to-date
```

备注：首次接入 Compose 后依赖解析与 dex 合并耗时明显增加，后续增量构建预计会更快。构建过程中出现 `libandroidx.graphics.path.so` 未 strip 的提示，以及既有 Gradle deprecated features 提示，均未阻断 Debug 编译。

### 2026-07-21 阶段 B：认证低风险页面第一组迁移

已完成：

- 新增 `ui/compose/screens/auth/AuthScreens.kt`。
- `LoginHomeFragment` 迁移为 `ComposeView + LoginHomeScreen`，保留原 `LoginHomeViewModel` 自动进入首页逻辑。
- `DeviceFlowIntroFragment` 迁移为 `ComposeView + DeviceFlowIntroScreen`，保留进入设备码页导航。
- `TokenLoginChoiceFragment` 迁移为 `ComposeView + TokenLoginChoiceScreen`，保留进入 Token 检查/生成指引导航。
- `TokenGuideFragment` 迁移为 `ComposeView + TokenGuideScreen`，保留打开浏览器和进入 Token 检查页逻辑。
- `TokenPermissionReviewFragment` 在后续深度推进中已迁移为 `ComposeView + TokenPermissionReviewScreen`。

验证命令：

```bash
./gradlew assembleDebug
```

验证结果：

```text
BUILD SUCCESSFUL in 1m 6s
41 actionable tasks: 6 executed, 35 up-to-date
```

备注：迁移过程中先出现 `AuthPageSurface` content scope 类型错误，已将 `Column.() -> Unit` 修正为 `ColumnScope.() -> Unit` 后编译通过。当前 Compose 认证页仍有部分等价迁移文案硬编码，后续应统一抽入 string resources。

### 2026-07-21 阶段 B：认证链路深度迁移

已完成：

- `DeviceFlowCodeFragment` 迁移为 `ComposeView + DeviceFlowCodeScreen`。
- 保留设备码页复制验证码、失败后重试、打开浏览器、取消登录、登录成功进入首页逻辑。
- `TokenPermissionReviewFragment` 迁移为 `ComposeView + TokenPermissionReviewScreen`。
- 保留 Token 输入、重新检查、权限检查列表、风险确认弹窗、重新生成 Token 选项、打开浏览器、保存成功进入首页逻辑。
- 原 XML/ViewBinding 动态渲染逻辑已改为 Fragment 观察 ViewModel 后驱动 Compose state。

验证命令：

```bash
./gradlew assembleDebug
```

验证结果：

```text
BUILD SUCCESSFUL in 1m 3s
41 actionable tasks: 4 executed, 37 up-to-date
```

备注：至此认证登录链路主要页面已经完成 ComposeView 迁移；`AuthScreens.kt` 已变得较大，下一步应拆分为 `LoginHomeScreen.kt`、`DeviceFlowScreens.kt`、`TokenScreens.kt` 等文件，并抽离硬编码文案到 string resources。

### 2026-07-21 Repository Actions Settings Dialog 迁移

已完成：

- `RepositoryActionsSettingsFragment` 中的 `MaterialAlertDialogBuilder` 交互迁移到 Compose Dialog。
- 在 `RepositoryActionsSettingsScreen.kt` 中新增 `RepositoryActionsSettingsDialogState` 和 DialogHost。
- 覆盖 Actions 启用/禁用确认、workflow write 权限确认、retention 编辑、cache 操作/删除、变量/Secret 编辑、标签选择、删除确认等弹窗。
- Fragment 仅维护 dialog state 和业务回调，输入框、多选、确认 UI 由 Compose Material3 `AlertDialog` 渲染。

验证命令：

```bash
./gradlew assembleDebug
```

验证结果：

```text
BUILD SUCCESSFUL
```

### 2026-07-21 Repository Issues 筛选 Dialog 迁移

已完成：

- `RepositoryIssuesFragment` 中创建者筛选和标签筛选的 `MaterialAlertDialogBuilder` 已迁移到 Compose Dialog。
- 页面仍保留 XML/ViewBinding + RecyclerView 主体，在根 `FrameLayout` 上叠加 `0x0 ComposeView` 作为 DialogHost。
- 创建者筛选使用 Compose `AlertDialog + RadioButton`，标签筛选使用 `AlertDialog + Checkbox`。

验证命令：

```bash
./gradlew assembleDebug
```

验证结果：

```text
BUILD SUCCESSFUL
```

### 2026-07-21 Repository Issue Detail Dialog 迁移

已完成：

- `RepositoryIssueDetailFragment` 中编辑评论、编辑标签和删除评论确认已迁移到 Compose Dialog。
- 页面根布局原本是 `ScrollView`，迁移时在 Fragment 返回层新增 `FrameLayout` 宿主，保留原内容并叠加 `0x0 ComposeView` DialogHost。
- 评论编辑使用 Compose `AlertDialog + OutlinedTextField`。
- 标签编辑使用 Compose `AlertDialog + Checkbox` 列表，并设置滚动高度约束。
- 删除评论确认从 `CompactBlackDialog` 迁移到 Compose `AlertDialog`，复用 `RepositoryIssueDetailDialogState`。

验证命令：

```bash
./gradlew assembleDebug
```

验证结果：

```text
BUILD SUCCESSFUL in 59s
41 actionable tasks: 6 executed, 35 up-to-date
```

### 2026-07-21 Compose 计划进度同步与通用错误态补齐

已完成：

- 新增 `SunsetErrorState`，补齐通用 empty/loading/error 状态组件族。
- 在 `SunsetGitHubPreview` 中加入错误态预览，覆盖基础组件渲染检查。
- 同步 `SettingsFragment`、认证 screen 拆分、仓库设置子页与 Issues Dialog 的实际迁移进度。
- 明确 `RepositoryFileUploadFragment` 冲突确认与 `RepositoryFileEditFragment` 复杂弹窗仍保留旧实现，继续按暂缓策略拆小切片推进。

验证命令：

```bash
./gradlew assembleDebug
```

验证结果：

```text
BUILD SUCCESSFUL in 57s
41 actionable tasks: 6 executed, 35 up-to-date
```

### 2026-07-21 Repository File Upload 冲突确认 Dialog 迁移

已完成：

- `RepositoryFileUploadFragment` 保留 XML/ViewBinding 主体，在返回根视图外层包裹 `FrameLayout`。
- 新增 `0x0 ComposeView` DialogHost，用 `SunsetGitHubTheme` 包裹 Compose 弹窗。
- 文件冲突确认从 `MaterialAlertDialogBuilder` 迁移为 Compose `AlertDialog`。
- 保留原三种动作语义：覆盖、重命名副本、取消并清理 pending conflict。

验证命令：

```bash
./gradlew assembleDebug
```

验证结果：

```text
BUILD SUCCESSFUL in 54s
41 actionable tasks: 6 executed, 35 up-to-date
```

### 2026-07-21 Repository Settings 编辑字段 Dialog 迁移

已完成：

- `RepositorySettingsFragment` 主体已经是 Compose，本切片继续收敛 Fragment 内编辑字段弹窗。
- 编辑字段从 `MaterialAlertDialogBuilder + TextInputLayout + TextInputEditText` 迁移到 Compose `AlertDialog + OutlinedTextField`。
- 保留原取消与保存语义，保存后调用 `viewModel.updateField(item.key, value)`。

验证命令：

```bash
./gradlew assembleDebug
```

验证结果：

```text
BUILD SUCCESSFUL in 1m 2s
41 actionable tasks: 6 executed, 35 up-to-date
```

### 2026-07-21 Repository Issue Detail 删除评论确认 Dialog 迁移

已完成：

- `RepositoryIssueDetailDialogState` 新增 `DeleteComment` 状态，删除评论确认并入既有 Compose DialogHost。
- `showDeleteCommentDialog` 从直接调用 `CompactBlackDialog` 改为设置 Compose dialog state。
- 新增 Compose `DeleteCommentDialog`，使用 Material3 `AlertDialog` 保留取消/删除语义。

验证命令：

```bash
./gradlew assembleDebug
```

验证结果：

```text
BUILD SUCCESSFUL in 40s
41 actionable tasks: 4 executed, 37 up-to-date
```

### 2026-07-21 Repository Settings 确认 Dialog 迁移

已完成：

- `RepositorySettingsFragment` 中可见性变更确认从 `CompactBlackDialog` 迁移到 Compose `AlertDialog`。
- `RepositorySettingsFragment` 中归档确认从 `CompactBlackDialog` 迁移到 Compose `AlertDialog`。
- 共用 `RepositorySettingsConfirmationDialogState` 与 `RepositorySettingsConfirmationDialogHost`，保留取消时清理 `pendingVisibilitySelection` 的行为。

验证命令：

```bash
./gradlew assembleDebug
```

验证结果：

```text
BUILD SUCCESSFUL in 40s
41 actionable tasks: 4 executed, 37 up-to-date
```

### 2026-07-24 Repository Action Run Detail 主体迁移

已完成：

- 新增 `RepositoryActionRunDetailScreen`，用 Compose 渲染 Actions 运行详情主体。
- `RepositoryActionRunDetailFragment` 从 XML/ViewBinding 主体迁移为 `ComposeView + RepositoryActionRunDetailScreen`。
- 保留原刷新、打开 GitHub、下载日志、下载 artifact 的 Fragment 行为回调。
- 移除旧 Fragment 内动态 ViewBinding 渲染、手写 artifact row 和旋转状态动画代码。

验证命令：

```bash
./gradlew assembleDebug
```

验证结果：

```text
BUILD SUCCESSFUL in 1m 58s
41 actionable tasks: 6 executed, 35 up-to-date
```

### 2026-07-24 Token Permission Review Dialog 清扫

已完成：

- 新增 `TokenDialogs.kt`，提供 Token 权限风险确认和重新生成 Token 选项的 Compose `AlertDialog`。
- `TokenPermissionReviewFragment` 移除旧 `AlertDialog.Builder` 和 `CompactBlackDialog` 调用。
- Fragment 使用 `TokenPermissionReviewDialogState` 驱动 Compose DialogHost，保留确认登录和打开 GitHub Token 设置页行为。

验证命令：

```bash
./gradlew assembleDebug
```

验证结果：

```text
BUILD SUCCESSFUL in 1m 51s
41 actionable tasks: 6 executed, 35 up-to-date
```

