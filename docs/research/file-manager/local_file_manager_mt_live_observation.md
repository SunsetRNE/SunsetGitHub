# MT 管理器现场功能观察记录

> 用途：记录用户现场演示过、或通过设备 UI 自动化实际观察到的 MT 管理器双栏文件管理行为。
> 原则：只记录事实与可见文本；不把推测写成已确认行为。后续代码对齐前，先按本文核实项目实现差距。

## 记录规则

- 来源分为：
  - `用户演示`：用户现场操作展示的功能。
  - `UI 自动化观察`：通过设备 UI 层级/截图识别到的界面事实。
  - `代码核实`：对 SunsetGitHub 当前代码的对应实现检查。
- 每个功能记录：入口、可见文本、操作步骤、结果、待核实点。
- 未演示的功能只能写到“待观察”，不能作为实现依据。

## 已观察事实

### 2026-07-20：MT 主双栏与右侧更多菜单

来源：UI 自动化观察。

包名/Activity：

```text
bin.mt.plus.canary
bin.mt.plus.MainLightIcon
```

主界面可见结构：

- 双栏文件列表；左右两侧均为独立 ListView。
- 顶部标题区域显示当前路径，例如：

```text
/storage/emulated/0/
```

- 顶部副信息显示统计与存储信息，例如：

```text
文件夹: 29  文件: 3  储存: 174.68G...
```

左栏可见条目示例：

```text
..
.MediaTrash
.SLOGAN
AGG
Alarms
Android
AndroidCS Native
AndroidIDEProjects
Audiobooks
backups
Browser
ColorOS
DCIM
Documents
Download
GitHub
```

右栏可见条目示例：

```text
..
Pictures
Download
Sunset
Android
GitHub
DCIM
Root
My Documents
NP
ponytail-main
Notes
AndroidIDEProjects
ColorOS
AndroidCS Native
backups
```

已识别右侧更多/抽屉菜单可见项：

```text
刷新
搜索
全选
过滤
排序方式
打开终端
隐藏文件
添加书签
设为首页
交换窗口
设置
退出
```

确认点：

- MT 有“设为首页”菜单项。
- MT 有“交换窗口”菜单项。
- MT 有“添加书签”菜单项。
- MT 有“隐藏文件”菜单项。
- MT 有“打开终端”菜单项。
- 存储根目录存在 `.MediaTrash` 条目；这暗示 MT 的回收站入口/数据位置可能与该目录相关，但具体行为仍需用户演示确认。

### 2026-07-20：设置页第一屏：启动与外观开头

来源：UI 自动化观察。

页面标题：

```text
设置
```

第一屏可见分组/条目：

```text
启动
请求 Root 权限
请求 Shell 权限
自定义 su 命令
安装 Shizuku
启动路径 - 左窗口
启动路径 - 右窗口
外观
桌面图标
主题颜色
```

可见摘要：

```text
请求 Root 权限：启动时请求 Root 权限，修改此设置后需重启生效
请求 Shell 权限：启动时如果未获取到 Root 权限，会尝试调用 Shi...
自定义 su 命令：用于请求 Root 权限的命令，自动识别请留空
安装 Shizuku：https://shizuku.rikka.app/
启动路径 - 左窗口：首页
启动路径 - 右窗口：首页
桌面图标：浅色背景（自适应）
```

确认点：

- MT 设置页明确存在独立的 `启动路径 - 左窗口` 和 `启动路径 - 右窗口`。
- 这比右侧菜单的“设为首页”更明确：启动路径是左右窗口分别配置的。
- Root/Shell 权限属于“启动”分组，且 Root 权限开关说明要求重启生效。

### 2026-07-20：设置页第二屏：文件列表显示与常规开头

来源：UI 自动化观察。

第二屏可见条目：

```text
文件列表时间偏好
文件列表不显示权限
时间日期格式
常规
生成备份文件
保留文件时间
启用输入框收藏功能
文件菜单排序
内置打开方式排序
```

可见摘要：

```text
文件列表时间偏好：隐藏秒数, 精简年份
文件列表不显示权限：非存储目录下的文件显示“权限+大小”
时间日期格式：yyyy-MM-dd HH:mm:ss
生成备份文件：保存文件时自动将原文件重命名为 .bak 备份文件
保留文件时间：在复制/解压/下载文件时保留文件原有的修改时间
启用输入框收藏功能：在带历史记录的输入框中启用我的收藏功能
文件菜单排序：长按后拖动排序
内置打开方式排序：长按后拖动排序
```

确认点：

- MT 支持保存文件时自动生成 `.bak` 备份文件。
- MT 支持复制/解压/下载时保留原修改时间。
- MT 的文件菜单和内置打开方式支持自定义排序，交互提示为“长按后拖动排序”。

### 2026-07-20：设置页第三屏：回收站与安装开头

来源：UI 自动化观察。

第三屏可见回收站相关条目：

```text
启用回收站功能
默认移动到回收站
自动清理回收站文件
显示删除警告
```

可见摘要：

```text
启用回收站功能：如果您禁用了回收站功能，删除文件时将不会显示移动到回收...
默认移动到回收站：删除文件时默认勾选移动到回收站
自动清理回收站文件：禁用
显示删除警告：删除文件时如果未选择移动到回收站，将显示红色警告，可一...
```

确认点：

- MT 回收站不是简单开关，而是一组设置。
- MT 存在回收站总开关，具体标题待微调确认。
- MT 删除确认里可能有“移动到回收站”复选项，并且 `默认移动到回收站` 控制该复选项默认状态。
- MT 支持自动清理回收站文件，当前摘要显示“禁用”。
- MT 支持未移动到回收站时显示红色删除警告。

第三屏可见安装相关条目：

```text
安装
APK 安装验证
APK 安装防自动删除
安装 APK 前二次确认
使用 Shizuku 安装 apk/apks/xapk
```

可见摘要：

```text
APK 安装验证：安装 APK 前验证签名和版本号
APK 安装防自动删除：防止 APK 文件在安装后被系统自动删除，在极少部分系...
安装 APK 前二次确认：在使用 Shizuku/Dhizuku/Root 安装...
使用 Shizuku 安装 apk/apks/xapk：需要安装并激活 Shizuku
```

### 2026-07-20：设置页第四屏：安装后续与书签底栏

来源：UI 自动化观察。

第四屏可见安装相关条目：

```text
使用 Dhizuku 安装 apk/apks/xapk
使用 Root 安装 apk/apks/xapk
自定义系统安装器
安装优先级说明
```

可见摘要：

```text
使用 Dhizuku 安装 apk/apks/xapk：需要安装并激活 Dhizuku
使用 Root 安装 apk/apks/xapk：仅在拥有 Root 权限时生效
自定义系统安装器：软件包安装程序 (com.android.packag...
安装优先级说明：使用 Shizuku 安装 > 使用 Dhizuku ...
```

第四屏可见书签/底栏相关条目：

```text
书签 & 底栏
在侧拉栏显示书签
新增书签添加到顶部
书签上滑手势区分左右
底部工具栏增加下边距
底部工具栏下边距大小
```

可见摘要：

```text
在侧拉栏显示书签：仅显示默认分组的书签，查看全部书签请从底部工具栏上滑
新增书签添加到顶部：新增书签将添加到所选分组顶部
书签上滑手势区分左右：点击书签跳转时，从当前激活窗口进行跳转
底部工具栏增加下边距：从底部工具栏上滑可拉出书签，如果上滑时经常和全面屏手势...
```

确认点：

- MT 的书签可配置是否显示在侧拉栏。
- MT 的新增书签可配置为添加到分组顶部。
- MT 有 `书签上滑手势区分左右` 设置；当前摘要显示关闭/默认语义可能是“点击书签跳转时，从当前激活窗口进行跳转”，具体开关开启后的差异待演示。
- MT 从底部工具栏上滑可拉出书签。

### 2026-07-20：设置页第五屏：外置存储、网络存储与云备份

来源：UI 自动化观察。

第五屏可见条目：

```text
优化外置存储数据传输
网络存储
加载缩略图
仅在 WiFi 网络下加载
图片大小限制
加载时间限制
云备份
备份到云端
从云端恢复
其它
```

可见摘要：

```text
优化外置存储数据传输：优化在外置存储、USB 设备上复制或移动文件的速度，少...
加载缩略图：显示图片与视频文件的缩略图
仅在 WiFi 网络下加载：开启后可节省您的数据流量
图片大小限制：超过 10MB 的图片文件不加载缩略图
加载时间限制：缩略图未在 10 秒内加载完成将会取消加载
备份到云端：将您的本地数据备份至云端
从云端恢复：将您的云端数据恢复至本地
```

确认点：

- MT 对外置存储/USB 的复制移动性能有专门开关。
- 网络存储缩略图有独立加载、WiFi 限制、大小限制、时间限制配置。
- MT 支持本地数据云备份/云恢复入口。

### 2026-07-20：设置页第六屏：其它

来源：UI 自动化观察。

第六屏可见条目：

```text
其它
语言
检测更新
特别鸣谢
备案编号
用户协议与隐私权政策
```

可见摘要：

```text
语言：自动检测
检测更新：MT官网: mt2.cn
特别鸣谢：为MT开发提供重要帮助的用户与开源项目
备案编号：苏ICP备19064627号-5A
用户协议与隐私权政策：已阅读并同意
```

确认点：

- 设置页底部主要是语言、更新、鸣谢、备案和协议类信息。
- 本屏未观察到新的文件管理核心行为设置。

### 2026-07-20：排序方式弹窗

来源：UI 自动化观察。

当前页面为弹窗，标题明确包含窗口归属：

```text
排序方式 - 右窗口
```

可见排序类型：

```text
按名称
按大小
按日期
按类型
```

可见附加选项：

```text
仅应用于此文件夹
逆向排序
```

可见按钮：

```text
管理
取消
确定
```

确认点：

- MT 的排序设置至少在弹窗标题层面区分左/右窗口。
- MT 支持“仅应用于此文件夹”，暗示可能存在全局排序规则与目录级排序规则。
- MT 支持“逆向排序”。
- MT 有“管理”入口，可能用于管理排序规则或文件夹排序配置；具体功能待继续观察。

## 待用户演示记录

### 回收站开关与删除行为

状态：待用户演示。

待观察：

- 回收站开关所在页面或菜单入口。
- 开启回收站后删除文件的确认文案。
- 关闭回收站后删除文件的确认文案。
- 删除后文件进入的位置/命名方式。
- 回收站内条目的还原、永久删除、清空入口。
- 回收站是否显示原位置、删除时间等元信息。

### 设为首页 / 启动路径

状态：待用户演示。

待观察：

- “设为首页”作用于当前窗口还是左右窗口分别保存。
- 左/右窗口是否分别有独立首页。
- 设为首页后的提示文案。
- 重启 MT 后左右窗口的初始路径。
- 是否支持 SAF/root/archive 等非普通本地路径作为首页。

### 交换窗口

状态：待用户演示。

待观察：

- 交换窗口后是否只交换路径，还是连同历史栈、选择状态、排序/过滤状态一起交换。
- 交换后焦点窗口是否变化。

### 添加书签 / 书签使用

状态：待用户演示。

待观察：

- 添加书签入口、提示文案。
- 书签列表位置。
- 书签点击/长按行为。
- 是否按点击位置/手势起点决定打开到左窗口或右窗口。

## MT 逆向代码核实区

### 2026-07-20：设置 XML key/defaultValue 核实

来源：逆向代码核实，apktool decoded 资源。

核实文件：

```text
reverse_engineering/MT管理器/decoded/res/xml/APKTOOL_RENAMED_0x7f150002.xml
reverse_engineering/MT管理器/decoded/res/values-zh-rCN/strings.xml
```

已确认设置页不是纯 UI 文案，而是 Preference XML 定义，关键 key/defaultValue 如下：

启动路径：

```xml
android:key="load_path_left" android:defaultValue="0"
android:key="load_path_right" android:defaultValue="0"
```

运行时首页路径：

```text
home_path_left
home_path_right
```

回收站：

```xml
android:key="enable_recycle_bin" android:defaultValue="true"
android:key="def_mov_recycle_bin" android:defaultValue="true"
android:key="auto_clean_recycle_bin" android:defaultValue="0"
android:key="deletion_warning" android:defaultValue="true"
```

书签/底栏：

```xml
android:key="show_bookmarks_in_sidebar" android:defaultValue="false"
android:key="bookmark_add_to_top" android:defaultValue="true"
android:key="bookmark_swipe_pos_aware" android:defaultValue="false"
android:key="bottom_toolbar_padding_type" android:defaultValue="0"
```

常规文件行为：

```xml
android:key="generate_backup_file" android:defaultValue="true"
android:key="preserve_file_time" android:defaultValue="true"
```

安装：

```xml
android:key="apk_installation_verify" android:defaultValue="true"
android:key="apk_installation_prevents_deletion" android:defaultValue="false"
android:key="apk_installation_confirm" android:defaultValue="false"
android:key="apk_installation_shizuku" android:defaultValue="true"
android:key="apk_installation_dhizuku" android:defaultValue="true"
android:key="apk_installation_root" android:defaultValue="true"
```

网络/外置存储缩略图：

```xml
android:key="use_external_storage_api"
android:key="external_storage_thumb_enable" android:defaultValue="true"
android:key="optimize_external_storage_dt" android:defaultValue="true"
android:key="network_thumb_enable" android:defaultValue="true"
android:key="network_thumb_only_load_on_wifi" android:defaultValue="true"
android:key="network_thumb_image_file_size_limit"
android:key="network_thumb_time_limit"
```

结论：

- 当前逆向材料已经包含设置页 XML，不需要重新反编译才能确认 key/defaultValue。
- `enable_recycle_bin`、`def_mov_recycle_bin`、`deletion_warning` 等 key 与用户现场观察完全一致。
- `load_path_left/right` 是启动路径选择项；`home_path_left/right` 是运行时首页实际路径存储。

### 2026-07-20：回收站运行时 smali 引用核实

来源：逆向代码核实，apktool decoded smali。

关键文件：

```text
reverse_engineering/MT管理器/decoded/smali/l/ۡۖۡ.smali
reverse_engineering/MT管理器/decoded/smali_classes2/l/᩹ۘܺ.smali
reverse_engineering/MT管理器/decoded/smali_classes2/l/᩻᩸ۛ.smali
```

确认点：

- `def_mov_recycle_bin` 在删除/操作配置初始化中被读取，默认值为 `true`，并写入一个布尔字段：

```smali
const-string v3, "def_mov_recycle_bin"
const/4 v4, 0x1
invoke-interface {v2, v3, v4}, SharedPreferences->getBoolean(...)
iput-boolean v4, p0, ...->ܶ:Z
```

- 删除确认/统计界面读取 `deletion_warning`，默认值为 `true`；当关闭时隐藏警告 TextView：

```smali
const-string v0, "deletion_warning"
const/4 v1, 0x1
invoke-interface {p1, v0, v1}, SharedPreferences->getBoolean(...)
if-nez p1, :cond_0
invoke-virtual {v3, 0x8}, View->setVisibility(I)
```

- 自动清理回收站读取 `auto_clean_recycle_bin`，默认字符串为 `"0"`。当数值大于 0 时，代码用：

```smali
const v2, 0x15180
mul-int p0, p0, v2
const-wide/16 v4, 0x3e8
mul-long v2, v2, v4
sub-long/2addr v0, v2
```

推断含义：`0x15180 = 86400` 秒，配置值按“天”换算为毫秒后得到清理阈值时间。`0` 表示禁用。

结论：

- MT 删除时“默认移动到回收站”确实是运行时代码使用的偏好，不只是设置页文案。
- MT 删除警告确实会控制警告控件显示/隐藏。
- MT 自动清理回收站按天数阈值执行，`0` 表示禁用。

### 2026-07-20：启动路径与首页运行时 smali 核实

来源：逆向代码核实。

关键文件：

```text
reverse_engineering/MT管理器/decoded/smali/l/۫᩻۠.smali
```

确认点：

- 启动时读取：

```smali
const-string v1, "load_path_left"
invoke-interface {v0, v1, "0"}, SharedPreferences->getString(...)

const-string v3, "load_path_right"
invoke-interface {v1, v3, "0"}, SharedPreferences->getString(...)
```

- 同一段代码继续读取实际首页路径：

```smali
const-string v4, "home_path_left"
invoke-interface {v3, v4, defaultPath}, ...->getString(...)

const-string v5, "home_path_right"
invoke-interface {v4, v5, defaultPath}, ...->getString(...)
```

结论：

- MT 左右窗口启动路径确实分开读取。
- 设置页 `load_path_left/right` 与右侧菜单“设为首页”保存的 `home_path_left/right` 是两层概念：前者决定启动策略，后者保存“首页”实际路径。

### 2026-07-20：书签上滑手势区分左右 smali 核实

来源：逆向代码核实。

关键文件：

```text
reverse_engineering/MT管理器/decoded/smali/l/ۡ᩶۠.smali
reverse_engineering/MT管理器/decoded/smali_classes2/l/᩺ᩳ۠.smali
```

确认点：

- 代码读取：

```smali
const-string v3, "bookmark_swipe_pos_aware"
invoke-interface {v2, v3, false}, SharedPreferences->getBoolean(...)
```

- 关闭时走当前激活窗口；开启时根据 `F` 浮点参数和当前窗口状态调用：

```smali
invoke-virtual {v1, p1, v0}, Ll/۫᩻۠;->᩵(FZ)V
```

结论：

- `bookmark_swipe_pos_aware` 确实参与运行时书签跳转逻辑。
- 现场文案中“根据上滑打开书签栏手指按下的位置，选择左边或右边窗口进行跳转”有代码支撑。

### 2026-07-20：排序方式左右窗口 smali 核实

来源：逆向代码核实。

关键文件：

```text
reverse_engineering/MT管理器/decoded/smali_classes2/l/᩷ۙ۠.smali
reverse_engineering/MT管理器/decoded/res/values-zh-rCN/strings.xml
```

确认点：

- 排序弹窗标题由：

```text
排序方式 + " - " + 左窗口/右窗口
```

拼接而成：

```smali
const v1, 0x7f120588    # 排序方式
const-string v1, " - "
invoke-virtual {p2}, ...->ۢ()Z
if-eqz v1, :cond_7
const v1, 0x7f1204da    # 左窗口
...
const v1, 0x7f120813    # 右窗口
```

- “仅用于该压缩包/仅应用于此文件夹”由同一 CheckBox 根据上下文切换文案：

```smali
const v1, 0x7f120589    # 仅用于该压缩包（重启失效）
const v1, 0x7f12058a    # 仅应用于此文件夹
```

- 确定后按左/右窗口分别保存：

```smali
file_cmp_sort_left
file_cmp_reverse_left
file_cmp_sort_right
file_cmp_reverse_right
```

- 目录级排序规则保存在：

```smali
sort_only_in_path_list
```

结论：

- MT 排序确实是双窗口独立配置。
- 排序支持 reverse 独立保存。
- 排序支持目录级 override，并通过 `sort_only_in_path_list` 持久化。

## 落实设计草案

> 目标：把已观察到的文件管理设置与功能落实到 SunsetGitHub，但只采用行为规格，不照搬 MT 的代码、key、类结构或 smali 流程。

### 设计原则

- 逆向结果只用于确认产品行为：有哪些设置、默认值倾向、入口和运行时效果。
- 项目实现使用自己的领域模型、存储 key、UI 组件和状态流。
- 设置能力优先沉到 `LocalFileManagerSettingsStore` 与 domain 数据类，Fragment 只负责展示和分发用户动作。
- 与双栏相关的功能统一以 `FileManagerPaneId.Left/Right` 为入口，不再传裸字符串或散落布尔值。
- 高风险文件操作先做窄实现：普通本地文件优先，SAF/root/archive 等能力按已有项目边界逐步接入。

### 设置模型建议

建议把本地文件管理设置拆成几组明确模型，而不是继续在 Fragment 里堆字段：

```kotlin
data class LocalFileManagerSettings(
    val recycleBin: RecycleBinSettings,
    val panes: PaneSettings,
    val bookmarks: BookmarkSettings,
    val general: GeneralFileSettings
)

data class RecycleBinSettings(
    val enabled: Boolean = true,
    val defaultMoveToRecycleBin: Boolean = true,
    val autoCleanDays: Int = 0,
    val showDeletionWarning: Boolean = true
)

data class PaneSettings(
    val left: PaneStartupSettings,
    val right: PaneStartupSettings
)

data class PaneStartupSettings(
    val startupMode: StartupPathMode = StartupPathMode.Home,
    val homePath: String? = null,
    val listOptions: FileManagerListOptions = FileManagerListOptions()
)

data class BookmarkSettings(
    val showInSidebar: Boolean = false,
    val addToTop: Boolean = true,
    val swipePositionAware: Boolean = false
)

data class GeneralFileSettings(
    val generateBackupFile: Boolean = true,
    val preserveFileTime: Boolean = true
)
```

说明：

- `startupMode` 表示启动时用首页、上次路径、默认存储根等策略；`homePath` 只保存“设为首页”的实际路径。
- `listOptions` 应进入左右窗格各自配置，而不是继续用一个全局 `listOptions`。
- `RecycleBinSettings` 要覆盖总开关、默认勾选、自动清理、删除警告四项，和已观察行为保持一一对应。
- 存储 key 使用项目自己的命名，例如 `local_file_manager.recycle_bin.enabled`，不要复用 MT 的 key。

### 双栏排序设计

当前差距最大的是排序。建议把 `FileManagerListOptions` 扩展为：

```kotlin
data class FileManagerListOptions(
    val sortMode: FileManagerSortMode = FileManagerSortMode.Name,
    val reverse: Boolean = false,
    val showHiddenFiles: Boolean = false
)
```

再新增目录级覆盖模型：

```kotlin
data class DirectoryListOptionsOverride(
    val pane: FileManagerPaneId,
    val path: String,
    val options: FileManagerListOptions
)
```

行为建议：

- 打开排序弹窗时，根据 `dualPaneState.focusedPane` 决定标题：`排序方式 - 左窗口` 或 `排序方式 - 右窗口`。
- 修改排序后只写入当前焦点窗格的 `listOptions`。
- 左/右列表渲染时分别读取自己的排序配置。
- 勾选“仅应用于此文件夹”时，把配置写入目录级覆盖；未勾选时写入窗格默认配置。
- “管理”入口可以先做成目录级排序规则管理页/弹窗，支持查看、删除规则；第一阶段也可以先隐藏，等目录级覆盖落地后再开放。

### 回收站设计

回收站建议按四层设计：

1. 设置层：`RecycleBinSettings` 保存总开关、默认移动、自动清理天数、删除警告。
2. 操作层：删除确认弹窗根据设置决定是否显示“移动到回收站”复选框及其默认勾选状态。
3. 数据层：`RecycleBinRecordStore` 记录回收路径、原路径、原名、删除时间，必要时扩展原始 Uri/Provider 信息。
4. 维护层：启动文件管理器或打开回收站时触发轻量自动清理，`autoCleanDays == 0` 时跳过。

删除确认建议行为：

- `enabled = false`：不显示“移动到回收站”，按永久删除流程提示。
- `enabled = true`：显示“移动到回收站”复选框，默认值来自 `defaultMoveToRecycleBin`。
- 用户取消勾选且 `showDeletionWarning = true`：显示明显但克制的危险提示。
- 删除进入回收站失败时，不应静默永久删除；需要提示用户选择重试或永久删除。

### 启动路径 / 设为首页设计

启动路径要区分两个概念：

- `startupMode`：启动时当前窗格打开哪里。
- `homePath`：用户通过“设为首页”保存的路径。

建议行为：

- “设为首页”只作用于当前焦点窗格。
- 设置页提供“启动路径 - 左窗口”和“启动路径 - 右窗口”，分别修改对应窗格的 `startupMode`。
- 应用启动时分别解析左/右窗格初始路径；如果保存路径不可访问，回退到默认存储根并提示一次。
- 交换窗口只交换当前运行时路径/导航栈是否交换，需要等用户演示后再定；不要默认把左右首页也交换。

### 书签设计

书签相关设置可以先只落和双栏跳转有关的最小闭环：

- `showInSidebar` 控制侧栏是否展示默认分组书签。
- `addToTop` 控制新增书签插入位置。
- `swipePositionAware = false`：书签跳转打开到当前焦点窗格。
- `swipePositionAware = true`：如果入口来自底栏上滑，按手势起点 X 坐标选择左/右窗格；普通点击仍可走当前焦点窗格。

### 推荐落地顺序

1. 整理设置存储：把现有 `LocalFileManagerSettingsStore` 从单个开关扩展成成组读写 API。
2. 引入左右窗格列表配置：实现 `reverse` 和 Pane 独立排序，先不做目录级覆盖。
3. 完善回收站设置：删除弹窗支持“移动到回收站”默认勾选、禁用回收站、删除警告。
4. 完善启动路径：用 `FileManagerPaneId` 替代 `paneName: String`，实现左右启动策略和设为首页。
5. 增加目录级排序覆盖和“管理”入口。
6. 增加书签双栏跳转设置。
7. 最后补自动清理、备份文件、保留文件时间等低频设置。

### 当前代码注意

- `LocalFileManagerSettingsStore` 已临时出现 `homePathForPane(paneName: String, ...)` 和 `setHomePathForPane(paneName: String, ...)`；正式实现时建议改为 `FileManagerPaneId` 参数。
- `FileManagerListOptions` 当前注释写的是 shared by panes，后续做双栏独立时需要同步调整注释，避免误导。
- `RecycleBinRecordStore` 已有记录结构，可作为基础继续扩展；不要为了对齐设置重写整套回收站数据层。
- Fragment 目前承担了过多状态拼接，新增设置时应优先增加 store/view-model/domain 方法，减少 UI 层直接读写 SharedPreferences。

## 当前项目代码核实区

### 2026-07-20：排序行为代码核实

来源：代码核实。

核实文件：

```text
app/src/main/kotlin/com/Sunset/REN/GitHub/ui/filemanager/LocalFileManagerFragment.kt
app/src/main/kotlin/com/Sunset/REN/GitHub/domain/filemanager/FileManagerListOptions.kt
app/src/main/kotlin/com/Sunset/REN/GitHub/domain/filemanager/FileManagerEntrySorter.kt
```

当前实现事实：

- `LocalFileManagerFragment` 只有一个全局字段：

```kotlin
private var listOptions: FileManagerListOptions = FileManagerListOptions()
```

- `showSortDialog()` 标题是固定的：

```text
排序方式
```

没有显示“左窗口/右窗口”。

- 当前排序菜单是简单动作列表：

```text
名称
修改时间
大小
类型
```

- 当前实现没有发现：

```text
仅应用于此文件夹
逆向排序
管理
```

- `FileManagerListOptions` 当前只有：

```kotlin
val sortMode: FileManagerSortMode = FileManagerSortMode.Name
val showHiddenFiles: Boolean = false
```

没有 `reverse`、`perDirectory` 或左右窗格独立字段。

差距结论：

- 与已观察到的 MT“排序方式 - 右窗口”相比，当前项目排序仍是全局列表选项，不是左/右窗口独立。
- 当前项目支持按名称/时间/大小/类型，但缺少 MT 的“逆向排序”。
- 当前项目缺少 MT 的“仅应用于此文件夹”。
- 当前项目缺少 MT 排序弹窗里的“管理”入口。
- 后续如果对齐，应优先设计 `PaneListOptions` 或把排序配置纳入独立 Pane 状态，而不是继续扩大单一 `listOptions`。

> 这里用于后续按演示结果逐项核实 SunsetGitHub 代码。暂不写结论，避免在演示前误判。

待核实文件优先级：

```text
app/src/main/kotlin/com/Sunset/REN/GitHub/ui/filemanager/LocalFileManagerFragment.kt
app/src/main/kotlin/com/Sunset/REN/GitHub/ui/filemanager/LocalFileManagerViewModel.kt
app/src/main/kotlin/com/Sunset/REN/GitHub/ui/filemanager/LocalFileManagerMenuDialog.kt
app/src/main/kotlin/com/Sunset/REN/GitHub/data/filemanager/LocalFileManagerSettingsStore.kt
app/src/main/kotlin/com/Sunset/REN/GitHub/data/filemanager/RecycleBinRecordStore.kt
app/src/main/res/values/strings_formal.xml
```

## 临时注意

- 2026-07-20 在用户要求“先演示再改”前，已开始了一点“设为首页/启动路径”的代码改动。后续需要基于用户演示结果决定：保留、修正或撤回。
