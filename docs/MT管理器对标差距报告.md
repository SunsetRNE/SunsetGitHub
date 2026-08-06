# MT 管理器 2.26.7 对标差距报告 —— 「操作」大类

> 基线来源：`reverse_engineering/MT管理器/decoded/res/values-zh-rCN/strings.xml`（MT 管理器 2.26.7 逆向资源）
> 对照实现：`app/src/main/kotlin/com/Sunset/REN/GitHub/ui/compose/screens/filemanager/LocalFileManagerScreen.kt`、`ui/filemanager/LocalFileManagerFragment.kt`、`ui/filemanager/LocalFileManagerViewModel.kt`
> 记录时间：2026-07-28

## 一、总览量化

| 状态 | 项数 | 占比 |
|---|---|---|
| ✅ 已完整对标 | ~15 | 33% |
| ⚠️ 域层已有能力、UI/Fragment 未接入（低成本补全） | 4 | 9% |
| ❌ 完全未实现 | ~22 | 49% |
| 🔒 Root 域（架构外，见第六节备注） | ~4 | 9% |

操作大类当前完成度 ≈ 35–40%（P0 低成本项接入后可达 ~50%）。

## 二、逐项对照

### 1. 选择操作 —— 50%
| MT 能力 | 状态 |
|---|---|
| 全选 / 反选 / 清除 | ✅ 已闭环（选择栏 + 更多菜单 + Fragment `invertSelection`） |
| 连选（长按两项自动选中区间） | ❌ "依次长按列表中的任意两项，将会自动选择它们中间所有的项" |
| 左右滑动文件直接选择 | ❌ "左右滑动文件可直接选择" |

### 2. 新建/重命名/删除 —— 60%
| MT 能力 | 状态 |
|---|---|
| 新建文件夹/文件、单个重命名、删除入回收站 | ✅ |
| 批量重命名（正则查找替换、编号、大小写转换） | ❌ |
| 回收站恢复 / 清空 | ⚠️ 域层 `restoreRecycleBinEntries` / `clearRecycleBin` / `isRecycleBinPath` 已存在，Fragment 未接入 |

### 3. 复制/移动/传输 —— 50%
| MT 能力 | 状态 |
|---|---|
| 复制/移动到另一窗格（确认 + 进度 + 取消）、复制路径 | ✅ |
| 传输时同步文件权限 | ❌ |
| 压缩传输 | ❌ |
| 创建软链接 | ❌ |
| 创建桌面快捷方式 | ❌ |

### 4. 压缩/解压 —— 60%
| MT 能力 | 状态 |
|---|---|
| 压缩为 ZIP、解压到当前/对侧窗格、ZIP 内浏览 | ✅ |
| 单独压缩每个文件/文件夹（批量各自成包） | ❌ |
| 压缩级别 / 压缩代码选项 | ❌ |

### 5. 属性与权限 —— 20%（差距最大）
| MT 能力 | 状态 |
|---|---|
| 属性查看 | ✅ EntryPropertiesPanel |
| 修改权限 chmod（rwx 图形化 + 八进制） | ❌ |
| 校验值计算（MD5/SHA1/SHA256，长按直出） | ❌ |
| 修改时间戳 | ❌ |
| 所有者与用户组 chown | 🔒 Root 域 |

### 6. 打开方式/分享 —— 40%
| MT 能力 | 状态 |
|---|---|
| 打开方式面板（FileToolRegistry） | ✅（部分工具为占位面板） |
| 系统分享（ACTION_SEND） | ❌ |
| 系统打开方式（ACTION_VIEW 三方应用） | ❌ |
| 默认打开方式记忆 / 内置打开方式排序 | ❌ |

### 7. 搜索 —— 25%
| MT 能力 | 状态 |
|---|---|
| 当前目录过滤 | ✅ |
| 递归搜索 | ⚠️ 域层 `searchCurrentDirectoryRecursively(options)` 已存在（含子目录/类型/大小写/隐藏开关），Fragment 未接入 |
| 通配符 `*` `?` | ❌（域层 matches 为 contains 匹配，需扩展） |
| 内容/代码搜索、二次搜索、结果移除 | ❌ |

### 8. 书签/收藏 —— 50%
收藏切换、收藏列表、打开收藏 ✅；书签分组管理 ❌；底栏上滑拉出书签手势 ❌。

### 9. 手势 —— 40%
下拉刷新 ✅、长按选择 ✅；连选手势 ❌、滑动选择 ❌、底栏上滑 ❌、长按菜单项触发单窗口操作 ❌（`FileToolId.singleWindow` 标记已有，无长按路由）。

### 10. Root 域 —— 架构外 🔒
挂载读写、ROOT/普通权限执行选择、chown、系统分区访问。详见第六节备注。

## 三、补全优先级路线图

| 梯队 | 项目 | 成本 | 状态 |
|---|---|---|---|
| **P0 接线即得** | 回收站恢复/清空、递归搜索接入（含大小写/隐藏开关 UI） | 极低 | ✅ 已完成 |
| **P1 MT 标志性交互** | 连选手势（长按 A → 长按 B 选区间） | 中 | ✅ 已完成 |
| **P1 批量操作** | 批量重命名（正则/编号）、单独压缩每个 | 中 | ⬜ |
| **P2 属性扩展** | chmod 修改权限（沙箱近似）、MD5/SHA1/SHA256 校验值 | 中 | ⬜ |
| **P2 系统桥** | ACTION_SEND 分享、ACTION_VIEW 系统打开方式 | 低 | ⬜ |
| **P3 深度对标** | 滑动选择、软链接、桌面快捷方式、传输同步权限、压缩选项、打开方式记忆 | 高 | ⬜ |
| **不做（现阶段）** | Root 域、APK 签名/Dex 编辑实操化 | — | 🔒 |

## 四、已完成里程碑

- [x] 2026-07-28 MT 风格 UI 视觉对标（条目行/顶栏/底栏/搜索栏/快捷方式/自绘图标）
- [x] 2026-07-28 面板补全：面包屑导航（`MtBreadcrumbBar`）、条目计数、下拉刷新（`PullToRefreshBox`）
- [x] 2026-07-28 功能补全：排序菜单（4 模式 + 5 开关）、`FileManagerEntrySorter.filterAndSort` 接入、双面板独立 `listOptions`
- [x] 2026-07-28 操作补全：选择栏（已选统计/全选/反选/属性/复制路径/压缩/删除）、双面板选择栏（按聚焦面板路由）
- [x] 2026-07-28 逻辑补全：Fragment 接线（`invertSelection`/`onNavigateToPath`/`zipFocusedSelection` 路由）、排序偏好经 `SettingsStore` 按面板持久化

## 五、P0 实施记录（2026-07-28，已完成 ✅）

- [x] 回收站恢复：选择栏"恢复"按钮（仅回收站路径显示）→ `viewModel.restoreRecycleBinEntries`，双面板按聚焦面板路由，更多菜单同步提供"恢复到原位置"
- [x] 回收站清空：更多菜单"清空回收站"（仅回收站路径显示）→ `viewModel.clearRecycleBin`，带确认对话框
- [x] 回收站内删除确认文案改为"彻底删除"警示（`DeleteConfirmDialogHost` 按 `isRecycleBinPath` 动态文案）
- [x] 递归搜索：搜索栏提交（IME Search / 图标点击）→ `viewModel.searchCurrentDirectoryRecursively`，`MtSearchResultRow` 结果条 + "退出搜索"，返回键优先退出搜索模式，目录切换自动退出，搜索中可取消（`searchJob`）
- [x] 编译验证：`./gradlew compileDebugKotlin` BUILD SUCCESSFUL

**P0 后操作大类完成度：≈ 50%（+回收站闭环 +递归搜索）**

## 五-B、P1-1 实施记录（2026-07-29，已完成 ✅）

- [x] 连选手势：`handleRangeLongClick` 辅助函数（Screen 层）——首次长按记录锚点（排除 Parent），再次长按不同条目计算列表索引区间，`onRangeSelect(区间子列表过滤 Parent)` 选中区间内全部条目，随后锚点清空
- [x] 锚点状态：`MtFilePane`（双面板）与 `FileManagerPane`（单面板）各自维护独立 `rangeAnchorId`（`remember(state.currentPath)`），`LaunchedEffect(selectedEntryIds)` 在选择集清空时自动重置锚点
- [x] 全链路透传：主 Screen `onRangeSelect`/`onSecondaryRangeSelect` → DualPane 左右独立路由（`onPrimaryRangeSelect`/`onSecondaryRangeSelect`）→ 各 `MtFilePane.onRangeSelect`，均带默认值零侵入
- [x] Fragment 接线：`onRangeSelect` 采用**并集语义**（`selectedEntryIds + range`，与 MT 实际行为一致——保留既有选择、追加区间），主/副面板选择集独立更新
- [x] 编译验证：`./gradlew compileDebugKotlin` BUILD SUCCESSFUL（修复 1 处 `onToggleSelected` → `onToggleEntrySelected` 命名残留）

**P1-1 后操作大类完成度：≈ 55%（+连选手势）**

## 五-C、行为模型对标修复（2026-07-29，已完成 ✅）

> 起因：真机实测发现「返回」「聚焦」两处核心行为模型与 MT 不一致（UI 已渲染 MT 双栏，但行为代码未跟上）。

- [x] **系统返回路由到聚焦窗格**：原实现 `handleOnBackPressed` 固定调用 `viewModel.navigateBack()`（左窗格），右窗格聚焦时按返回会弹左窗格历史栈（实测：右栏聚焦 Download，按返回左栏从存储根目录跳到 AppFiles，反复按返回一路"退到根目录"并退出）。现按 `focusedPane` 路由：Primary → `viewModel.navigateBack()`，Secondary → `navigateSecondaryBack()`（已改为返回 `Boolean`），聚焦窗格无历史时才退出
- [x] **面包屑点击同步聚焦**：原实现右窗格面包屑段点击只导航不聚焦，顶栏停留在左窗格路径，造成"只有左侧聚焦"的观感。现 `onNavigateToPath`/`onSecondaryNavigateToPath` 先设置 `focusedPane` 再导航（MT：与窗格的任何交互都聚焦该窗格）
- [x] **焦点视觉强化**：聚焦窗格路径栏底色由 `subtleBackground`（亮/暗主题下与画布色几乎无差）改为 `accentSoft`，聚焦下划线加粗至 2.dp。像素级验证：聚焦栏 #DDF0FF vs 非聚焦栏纯白
- [x] **启动历史栈清理**：MODE_MANAGE 启动时 `openDirectoryPath(存储根目录)` 会把初始 AppFiles 压入回退栈，导致在存储根目录按返回先跳回 AppFiles。新增 `viewModel.clearNavigationHistory()`，启动路径即历史根
- [x] 真机回归验证：右栏聚焦按返回 → 右栏回退自身历史、左栏不动 ✅；左栏面包屑点击 → 左栏导航且焦点切左 ✅；`assembleDebug` 构建 + 安装验证通过

## 六、备注：Root 相关架构（后续补全）

> **Root 权限对本项目是必然需求** —— 项目路线包含自动化能力，自动化操作（跨应用文件读写、系统目录访问、权限修改、挂载读写等）必须依赖 Root 执行通道。
>
> 当前阶段**暂缓** Root 域对标，原因：Root 执行通道（shell 进程管理、权限探测、结果回传、安全模型）属于独立基础设施，需先行建设。
>
> **补全顺序约定**：先完成 Root 基础设施（执行通道 + 权限探测 + 安全模型），再回头对标 MT 的 Root 域操作：
> - 挂载读写（系统分区 RO/RW 切换）
> - chmod 完整八进制权限修改（Root 提权后不受沙箱限制）
> - chown 所有者与用户组修改
> - "使用 ROOT 权限执行 / 使用普通权限执行"操作分流
> - Android/data、Android/obb 等受限目录的 Root 访问
>
> 后续所有新增的对标项，同样遵循"基础设施先行，完成后再对标"的原则。
