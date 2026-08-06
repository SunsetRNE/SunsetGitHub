# 内置文件管理器模块化改造计划

> 状态：实施收尾版。  
> 旧计划处理：已废弃并删除原内容。  
> 本文用途：作为“内置文件管理器 / 仿 MT 管理器体验”的设计边界、实施顺序、验收依据与收尾清单。  
> 当前阶段：主体架构改造已落地；Root 写入类高危能力继续保守关闭。

## 1. 背景与问题

当前内置文件管理器已经具备较多功能：

- 首页右上角独立入口；
- 管理模式默认进入外部存储根目录；
- 单栏 / 双栏浏览；
- 最近目录、收藏目录、授权目录；
- 本地文件、SAF、压缩包浏览；
- 文件预览、搜索、多选；
- 复制、移动、删除、压缩、转 TXT 等操作；
- APK / DEX / ARSC 等工程工具方向的预留。

但当前实现仍存在明显结构问题：

1. Root 权限逻辑与普通 Android 权限逻辑没有清晰分层。
2. 操作按钮和提示逻辑没有基于“当前能力”动态渲染。
3. Root 未授权时仍容易暴露不应常态显示的高级逻辑入口。
4. SAF 授权、普通本地访问、压缩包浏览、未来 Root 访问之间边界不够清晰。
5. 顶部明确加载条不符合目标 MT 风格体验，普通目录读取不应强提示。
6. `LocalFileManagerFragment` 与 `LocalFileManagerViewModel` 堆积了过多职责。
7. 当前是“外部目录上的模块”，但内部还不是彻底模块化的文件管理器内核。

本计划目标是将其从“堆积功能的大页面”演进为“可独立维护、可独立扩展、能力分层清晰的文件管理器模块”。

## 2. 产品定位

内置文件管理器应定位为 SunsetGitHub 内置的模块化文件工作台，并尽量提供 MT 风格的高密度文件管理体验。

它应支持：

- 普通外部存储浏览；
- 应用私有目录浏览；
- SAF 授权目录浏览；
- 压缩包浏览；
- 文件预览与文本编辑衔接；
- 批量操作；
- 双栏复制 / 移动；
- 可选 Root 能力；
- 工程向文件工具扩展。

它不应在未授权状态下假装拥有系统级文件管理能力。

Root、SAF、普通本地文件访问应分别建模、分别提示、分别控制 UI 行为。

## 3. 核心原则

### 3.1 能力驱动 UI

后续 UI 不应采用：

```text
先展示按钮 -> 用户点击 -> 操作失败 -> 弹权限错误
```

而应采用：

```text
识别当前位置和授权状态 -> 计算可用能力 -> UI 只显示或启用合适操作
```

示例：

- 未 Root：隐藏 Root 专属路径和高级操作。
- Root 已授权：显示 `/data`、`/system`、权限修改、所有者修改等高级入口。
- SAF 目录：只显示 SAF 能力允许的操作。
- 压缩包内部：隐藏普通新建、权限修改等不合适操作，显示解压、复制到另一栏、工具分析等动作。

### 3.2 Root 与普通授权分离

Root 不是普通文件权限的补丁，也不应混入普通 `File` / `DocumentFile` 流程。

Root 应有独立状态：

```kotlin
sealed class RootAccessState {
    data object Unknown : RootAccessState()
    data object NotAvailable : RootAccessState()
    data object AvailableButNotGranted : RootAccessState()
    data object Requesting : RootAccessState()
    data object Granted : RootAccessState()
    data class Denied(val reason: String) : RootAccessState()
    data class Error(val message: String) : RootAccessState()
}
```

UI 行为：

| Root 状态 | UI 行为 |
|---|---|
| Unknown | 不显示 Root 操作，仅在更多菜单提供“检测 Root” |
| NotAvailable | 隐藏 Root 操作 |
| AvailableButNotGranted | 显示“启用 Root 模式”入口 |
| Requesting | 显示轻量申请中状态 |
| Granted | 显示 Root 路径和高级操作 |
| Denied | 轻提示，不污染主界面 |
| Error | 提供错误详情入口 |

### 3.3 提示克制化

MT 风格不是到处弹窗，而是上下文明确。

提示策略：

| 场景 | 推荐提示 |
|---|---|
| 普通受限目录 | 行内提示 + SAF 授权入口 |
| 系统私有路径 | Root 模式入口 |
| Root 拒绝 | Toast 或错误详情 |
| 删除 / 覆盖 | 明确确认对话框 |
| 长任务 | 任务进度面板 |
| 普通目录加载 | 尽量不显示明显加载条 |

### 3.4 Provider 化文件源

不同文件源不能继续靠大量字符串判断混在 ViewModel 中。

目标抽象：

```kotlin
interface FileSystemProvider {
    val id: FileSystemProviderId
    val displayName: String
    val capabilities: FileSystemCapabilities

    suspend fun list(path: FileManagerPath): FileListResult
    suspend fun stat(path: FileManagerPath): FileStatResult
    suspend fun read(path: FileManagerPath): FileReadResult
    suspend fun write(path: FileManagerPath, content: ByteArray): FileOperationResult
    suspend fun createDirectory(path: FileManagerPath): FileOperationResult
    suspend fun rename(path: FileManagerPath, newName: String): FileOperationResult
    suspend fun delete(path: FileManagerPath): FileOperationResult
}
```

目标 Provider：

- `LocalFileSystemProvider`
- `SafFileSystemProvider`
- `ArchiveFileSystemProvider`
- `RootFileSystemProvider`
- `ShizukuFileSystemProvider`，可选后续能力

## 4. 目标模块结构

建议先在 app 内做包级模块化，稳定后再考虑 Gradle 多模块。

```text
ui/filemanager/
├── LocalFileManagerFragment.kt
├── FileManagerViewModel.kt
├── adapter/
├── controller/
│   ├── FileManagerTopBarController.kt
│   ├── FileManagerBottomBarController.kt
│   ├── FileManagerSelectionBarController.kt
│   ├── FileManagerDrawerController.kt
│   ├── FileManagerPaneController.kt
│   ├── FileManagerDualPaneController.kt
│   └── FileManagerDialogCoordinator.kt
├── model/
└── navigation/

domain/filemanager/
├── path/
├── provider/
├── capability/
├── operation/
├── root/
├── archive/
├── tools/
└── safety/

data/filemanager/
├── FileManagerPreferences.kt
├── SafDirectoryStore.kt
├── FavoriteDirectoryStore.kt
├── RecentDirectoryStore.kt
├── FileTaskHistoryStore.kt
└── DirectoryListingCache.kt
```

## 5. 核心模型设计

### 5.1 FileManagerPath

避免继续用 `String` 表达所有路径。

```kotlin
sealed class FileManagerPath {
    data class Local(val absolutePath: String) : FileManagerPath()
    data class Saf(val uri: Uri) : FileManagerPath()
    data class Archive(val archivePath: String, val innerPath: String) : FileManagerPath()
    data class Root(val absolutePath: String) : FileManagerPath()
}
```

这样可以避免：

- `content://` 到处判断；
- 压缩包路径拼接混乱；
- Root 路径和普通路径误用；
- SAF 被当成普通 `File` 操作。

### 5.2 FileManagerEntry

后续条目应携带 Provider 与能力信息。

```kotlin
data class FileManagerEntry(
    val id: String,
    val name: String,
    val path: FileManagerPath,
    val type: FileEntryType,
    val sizeBytes: Long?,
    val modifiedAtMillis: Long?,
    val providerId: FileSystemProviderId,
    val capabilities: FileEntryCapabilities
)
```

### 5.3 FileEntryCapabilities

```kotlin
data class FileEntryCapabilities(
    val canOpen: Boolean,
    val canPreview: Boolean,
    val canRename: Boolean,
    val canDelete: Boolean,
    val canCopy: Boolean,
    val canMove: Boolean,
    val canCompress: Boolean,
    val canExtract: Boolean,
    val canEditPermission: Boolean,
    val canEditOwner: Boolean
)
```

该能力不是静态文件类型能力，而是“当前上下文 + 当前授权状态 + 当前 Provider”共同计算的能力。

## 6. Capability 设计

新增：

```text
domain/filemanager/capability/
├── FileManagerCapabilitySet.kt
├── FileManagerCapabilityResolver.kt
├── FileActionVisibilityPolicy.kt
└── FileManagerActionSet.kt
```

`FileManagerCapabilityResolver` 负责根据：

- 当前路径；
- 当前 Provider；
- 当前选择项；
- Root 状态；
- SAF 状态；
- 是否压缩包内部；
- 当前模式，管理 / 选择；

输出 UI 可渲染的动作集合。

```kotlin
data class FileManagerActionUiModel(
    val id: FileManagerActionId,
    val title: String,
    val iconRes: Int?,
    val visible: Boolean,
    val enabled: Boolean,
    val disabledReason: String? = null
)
```

UI 层只消费 action model，不再散落写权限判断。

## 7. Root 模块设计

新增：

```text
domain/filemanager/root/
├── RootAccessState.kt
├── RootAccessManager.kt
├── RootCommandRunner.kt
├── RootPromptPolicy.kt
└── RootPathPolicy.kt
```

职责：

- 检测 `su`；
- 请求 Root 授权；
- 缓存授权状态；
- 执行 Root 命令；
- 将 Root 失败转换为文件管理器可理解的错误；
- 不直接控制 UI；
- 不污染普通 Provider。

Root 高危操作必须保守：

- 递归删除需二次确认；
- chmod / chown 需展示路径；
- 挂载读写需明确提示；
- shell 命令参数必须转义；
- 失败时提供错误详情；
- 默认不自动进入高危路径。

## 8. Operation 任务系统

复制、移动、删除、压缩、解压、递归搜索应统一为任务。

```kotlin
interface FileOperation {
    val id: FileOperationId
    val title: String

    fun validate(context: FileOperationContext): FileOperationValidation
    suspend fun execute(context: FileOperationContext): Flow<FileOperationEvent>
}
```

事件：

```kotlin
sealed class FileOperationEvent {
    data class Started(val title: String) : FileOperationEvent()
    data class Progress(val current: Long, val total: Long?, val message: String) : FileOperationEvent()
    data class ConflictDetected(val source: FileManagerPath, val target: FileManagerPath) : FileOperationEvent()
    data class Completed(val summary: String) : FileOperationEvent()
    data class Failed(val message: String, val throwable: Throwable? = null) : FileOperationEvent()
    data object Cancelled : FileOperationEvent()
}
```

目标：

- 长任务可取消；
- 复制 / 移动有进度；
- 压缩 / 解压有进度；
- 搜索有状态；
- 冲突处理统一；
- 任务历史可选记录。

## 9. UI 拆分方向

`LocalFileManagerFragment` 最终应只负责：

- inflate binding；
- 创建 controller；
- 订阅 ViewModel 状态；
- 生命周期转发；
- 导航到预览 / 编辑页。

具体 UI 行为拆给：

| Controller | 职责 |
|---|---|
| `FileManagerTopBarController` | 路径显示、路径编辑、更多菜单、抽屉入口 |
| `FileManagerBottomBarController` | 后退、前进、新建、双栏、上级目录 |
| `FileManagerSelectionBarController` | 多选数量、复制、移动、删除、压缩、更多 |
| `FileManagerDrawerController` | 最近、书签、授权目录、抽屉管理 |
| `FileManagerPaneController` | 单个文件栏列表、点击、长按、选择 |
| `FileManagerDualPaneController` | 左右栏、焦点栏、目标栏、双栏传输 |
| `FileManagerDialogCoordinator` | 新建、重命名、删除确认、Root 授权、SAF 授权、错误详情 |

## 10. 加载反馈策略

当前明确水平加载条不作为目标体验。

后续规则：

| 场景 | 反馈方式 |
|---|---|
| 普通目录读取 | 默认不显示明显加载条 |
| 慢 SAF 目录 | 列表内轻量“读取中” |
| 递归搜索 | 搜索状态栏，可停止 |
| 复制 / 移动 | 任务面板 / 对话框 |
| 压缩 / 解压 | 任务面板 / 对话框 |
| Root 命令 | 小型状态 + 错误详情 |
| 调试模式 | 可选显示顶部 ProgressBar |

因此 `progress_local_file_manager` 后续应默认隐藏，不参与常态目录加载反馈。

## 11. 分阶段实施计划

### 阶段一：设计落地与低风险体验修正

目标：不大改文件访问逻辑，先统一方向。

任务：

- 保留本文作为新计划；
- 普通目录加载隐藏明显顶部加载条；
- 整理现有 Root / 权限相关入口；
- 标注哪些按钮属于基础能力，哪些属于高级能力；
- 将硬编码提示逐步迁移为资源字符串。

验收：

- 首页入口不变；
- 文件管理器可正常打开；
- 普通目录读取不出现突兀加载条；
- 不新增 Root 假能力入口。

### 阶段二：Root 状态模型与提示策略

目标：Root 能力独立建模，但不急于实现完整 Root 文件系统。

任务：

- 新增 `RootAccessState`；
- 新增 `RootAccessManager`；
- 新增 `RootPromptPolicy`；
- 更多菜单中加入 Root 模式入口；
- 未授权不展示 Root 专属操作；
- 授权后才展示 Root 入口与高级动作。

验收：

- 无 Root 设备不显示高级 Root 操作；
- 有 Root 未授权时只显示启用入口；
- Root 授权失败为轻提示；
- Root 授权成功后能力集发生变化。

### 阶段三：CapabilityResolver

目标：按钮和菜单由能力模型驱动。

任务：

- 新增 `FileManagerCapabilityResolver`；
- 新增 `FileActionVisibilityPolicy`；
- 将底部栏、多选栏、更多菜单迁移为读取 `FileManagerActionUiModel`；
- 区分 Local / SAF / Archive / Root 的可用操作。

验收：

- 压缩包内部不显示无意义写入操作；
- SAF 目录只显示 SAF 可执行操作；
- 未 Root 不显示 Root 高级操作；
- 已 Root 后出现对应高级操作。

### 阶段四：Provider 化

目标：统一文件源访问方式。

任务：

- 新增 `FileManagerPath`；
- 新增 `FileSystemProvider`；
- 迁移 Local list；
- 迁移 SAF list；
- 迁移 Archive list；
- 预留 Root Provider。

验收：

- ViewModel 不再直接依赖大量路径字符串判断；
- Local / SAF / Archive 可以通过 ProviderRegistry 分发；
- 现有浏览、预览、收藏、最近不回退。

### 阶段五：Operation 任务化

目标：统一长任务和危险操作。

任务：

- 新增 `FileOperationRunner`；
- 迁移复制、移动、删除；
- 迁移压缩、解压；
- 迁移递归搜索；
- 增加冲突处理事件；
- 增加任务进度 UI。

验收：

- 长任务可取消；
- 冲突处理一致；
- 失败提示一致；
- 不再依赖顶部加载条表达长任务状态。

### 阶段六：UI Controller 拆分

目标：降低 Fragment 体积。

任务：

- 拆 TopBarController；
- 拆 BottomBarController；
- 拆 SelectionBarController；
- 拆 DrawerController；
- 拆 PaneController；
- 拆 DialogCoordinator。

验收：

- Fragment 只承担页面容器职责；
- UI 行为可单独维护；
- 不引入无关格式化 churn；
- 现有双栏、多选、抽屉行为不回退。

### 阶段七：Root Provider 与高级工具

目标：在能力模型稳定后实现真实 Root 文件访问。

任务：

- 实现 Root list；
- 实现 Root stat；
- 实现 Root read；
- 谨慎实现 Root write / delete / chmod / chown；
- 增加危险操作确认；
- 接入工程工具高级能力。

验收：

- Root 未授权不可使用；
- Root 授权后可访问 Root 路径；
- 高危操作均有确认；
- Root 命令失败有错误详情；
- 普通模式不受 Root 代码影响。

## 12. 风险控制

### 12.1 不做一刀切重写

当前功能较多，直接重写风险高。后续应采用渐进迁移：

```text
先能力模型 -> 再 Root 状态 -> 再 Provider -> 再 Operation -> 最后拆 UI
```

### 12.2 Root 高危操作必须保守

Root 相关操作必须防止：

- 递归删除系统目录；
- 命令注入；
- 未确认 chmod / chown；
- 挂载系统分区读写误操作；
- 符号链接循环；
- 错误路径展开。

### 12.3 SAF 与 Root 不混用

普通受限目录优先提示 SAF 授权。  
系统私有路径再提示 Root。  
不要把所有权限失败都解释为需要 Root。

### 12.4 许可证边界

可以参考 MT 管理器、Amaze、Fossify 等产品的交互和边界处理，但不得复制 GPL 或未知许可证实现代码。

## 13. 实施进度与收尾清单

截至当前工作区版本，7 个阶段的主体目标已基本完成：

- 阶段一：普通目录加载反馈已克制化，顶部 ProgressBar 不再作为常态目录加载表达。
- 阶段二：RootAccess 状态模型、检测/授权入口与提示策略已落地，未授权状态不暴露 Root 假能力。
- 阶段三：CapabilityResolver / ActionVisibilityPolicy 已接入主要操作栏、更多菜单和选择态动作。
- 阶段四：Local / SAF / Archive / Root Provider 抽象与 Registry 已建立，核心 list/stat/read 边界明确。
- 阶段五：复制、移动、删除、压缩、解压、递归搜索等长操作已进入统一 UI 进度流程，并在 ViewModel 深层 IO 中补齐取消检查。
- 阶段六：Top/Bottom/Selection/Pane/DualPane/RootAction/OperationUI/Drawer/Search/PathEdit 等 controller 或 renderer 已拆出，Fragment 仍保留页面编排职责。
- 阶段七：Root list/stat/read 已安全开放；Root read 仅支持小文件只读预览缓存，write/delete/chmod/chown/remount 继续关闭并走危险操作拦截提示。

保守暂缓项：

- Root 写入、删除、chmod/chown、remount 等高危能力未开放；如后续开放，必须先通过 RootOperationSafetyDialog、路径风险策略、命令转义和二次确认验收。
- `FileOperation` / `FileOperationEvent` 的完整 Flow 任务历史系统尚未一刀切替换全部 legacy 方法；当前以统一进度 UI + cancellable 深层 IO 达成本阶段可用验收。
- `LocalFileManagerFragment` 已明显瘦身，但仍可继续按 DialogCoordinator 方向拆分剩余弹窗编排。

## 14. 验收总目标

最终文件管理器应满足：

- 首页右上角入口稳定；
- 普通用户不会看到大量不可用 Root 按钮；
- Root 授权后高级能力自然出现；
- 普通本地、SAF、Archive、Root 文件源边界清晰；
- 按钮显示由能力模型控制；
- 普通目录读取不出现突兀加载条；
- 长任务进入任务系统；
- Fragment / ViewModel 体积逐步下降；
- 文件管理器可作为独立模块持续更新。

## 15. 决策结论

废弃旧的“内置文件管理器完整落实方案”。

后续采用新的模块化方向：

```text
Provider 驱动文件源
Capability 驱动 UI
RootAccess 独立授权
Operation 统一任务
Controller 拆分界面
PromptPolicy 管理提示
```

第一优先级不是继续堆功能，而是补齐：

```text
权限能力矩阵 + Root 状态模型 + 操作可见性策略
```

这将作为后续所有文件管理器改造的基础。