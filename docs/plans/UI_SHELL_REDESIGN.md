# UI 壳重绘设计：一致组件模块化坐标构建法

> 状态：设计定稿（2026-08-06）；骨架（schema/layout/render/shell）与
> Home、Dashboard 垂直切片已完成，`assembleDebug` 编译通过 ✅
> 组件补全：Skeleton/LanguageBar/DropdownMenu/行内动作（步骤 5 前置）✅
> 迁移进度：步骤 1（骨架）✅ 步骤 2（Home 切片）✅ 步骤 3（Dashboard 切片）✅
> 步骤 4（仓库详情分段导航）✅ 步骤 5（逐页迁移：Issues/PR/Notifications/Profile/Settings/Search/Account/
> IssueDetail/Releases/HTML 摘要分区页族（Wiki/Projects/Insights/Agents/SecurityQuality/仓库 Settings 六分区共用一页）/
> Actions/本地文件管理器/登录首页（LoginHome 认证入口）已迁移，Dashboard 已用语言色条+行内动作升级；Settings/Search/Account/Releases 验证 Hidden+返回）→ 进行中
> 认证链路五页已全部迁移（设备流两页 DeviceFlowIntro/DeviceFlowCode + Token 三页 TokenLoginChoice/TokenGuide/TokenPermissionReview），
> FieldComponent 新增 enabled 字段（默认 true 向后兼容）→ 进行中
> 仓库写入流两页已迁移（IssueCreate 新建议题 + ReleaseCreate 新建发布），FieldComponent 新增 isError/supportingText 表单校验 → 进行中
> 通知详情页已迁移（NotificationDetail：快捷操作 + 打开链接，本地化复用 NotificationsPage 纯函数）→ 进行中
> 终端/工作区同步两页已迁移（Terminal 工作区终端 + WorkspaceSync 同步方向入口），任务 1-4 全部完成 → 步骤 6（删旧壳）待统筹
> 组 A 仓库写入/文件流四页已迁移（RepositoryFileEditPage 混合布局：schema+原生 Sora 编辑器嵌入、
> renderPage 新增 fillMaxSize 参数默认 true 向后兼容；RepositoryFileUploadPage 双卡五态；
> RepositoryCreatePage Hero+表单卡六态状态徽章；RepositoryForkPage 四态全映射+名称可用性提示）→ 组 B 待迁移
> 触发原因：UI 壳长期存在漂移、导航栏约束不一致、渲染越界问题。
> 方法：**一致组件模块化坐标构建法**——所有 UI 组件化、模块化，只解析
> 固定字段，通过坐标固定在相应页面，渲染层按字段状态做判断。

## 1. 问题诊断（现有壳的病灶）

### 1.1 壳无唯一布局权威
`activity_main.xml` 中内容区 `nav_host_fragment` 直接
`layout_constraintBottom_toBottomOf=parent`，底栏容器
`nav_view_container` 为 `wrap_content` + `clipChildren=false`。
后果：页面渲染区域与底栏可能重叠；`clipChildren=false` 允许内容
物理溢出壳外——"渲染无法约束在壳内"的直接来源。

### 1.2 导航栏是补丁式系统（13 个文件）
XML `BottomNavigationView` + Compose `MaterialBottomNavigationBarRenderer`
双轨并存，通过 `NavigationBarHostController.render(state)` 事后修补
可见性/菜单/inset。状态机庞大（主导航/仓库分段导航/浮动模式/隐藏），
每次状态切换都是一次"打补丁"，任何补丁遗漏即产生约束不一致（漂移）。

### 1.3 页面各自为政
52 个 Fragment，布局根随意（LinearLayout/ScrollView/ConstraintLayout/
ComposeView），各自处理 padding/inset/fadingEdge，无统一 schema。
同一种 UI（搜索框、列表项、空状态）在不同页面有不同实现，约束不统一。

### 1.4 inset 手动同步
`updateSystemNavigationBottomInset` 手动推进系统导航栏 inset，
与 Compose Surface 渲染不同步即漂移。

## 2. 设计目标

1. **唯一壳**：所有页面渲染在同一个 AppShell 内容区内，物理上不可能越界。
2. **组件唯一实现**：每种 UI 只有一个组件模块（schema + 渲染器绑定），
   页面只声明 schema，不实现 UI。
3. **坐标确定**：页面 = 坐标网格（行 × 列），组件占据网格单元，位置确定。
4. **字段固定**：组件只解析自身 schema 的固定字段，渲染判断完全由字段驱动。
5. **inset 单一来源**：壳统一消费 WindowInsets，页面/组件不再自行处理。

## 3. 架构总览

```text
com.Sunset.REN.GitHub.ui
├── shell/                     # 壳层（唯一布局权威）
│   ├── AppShell.kt            # 三区 Box：TopBar / Content / NavBar
│   ├── ShellInsets.kt         # inset 单一来源（唯一消费 WindowInsets 处）
│   └── ShellState.kt          # 壳状态（标题、导航栏模式、菜单项）
├── schema/                    # 组件 schema（固定字段，纯数据）
│   ├── Component.kt           # sealed interface Component（组件根类型）
│   ├── TextComponent.kt       # { text, style, color, maxLines, ... }
│   ├── ButtonComponent.kt     # { text, kind, enabled, action, ... }
│   ├── ListComponent.kt       # { items: [ItemSchema], ... }
│   ├── ItemComponent.kt       # { title, subtitle, icon, badge, ... }
│   ├── ImageComponent.kt      # { source, size, tint, ... }
│   ├── FieldComponent.kt      # 输入框 { value, hint, singleLine, ... }
│   ├── SpacerComponent.kt     # { weight / height }
│   └── StateComponent.kt      # 状态组件 { kind: Loading/Empty/Error, ... }
├── layout/                    # 坐标系统
│   ├── Grid.kt                # 网格：行 × 列，dp 基准 + 权重
│   ├── PageSchema.kt          # PageSchema { id, rows: [RowSchema] }
│   ├── RowSchema.kt           # RowSchema { cells: [CellSchema] }
│   └── CellSchema.kt          # CellSchema { row, column, span, component }
├── render/                    # 渲染器（渲染判断的唯一实现）
│   ├── ComponentRenderer.kt   # @Composable Render(component, ...) 分发
│   └── PageRenderer.kt        # @Composable Render(page, state) → 网格布局
└── pages/                     # 页面 = 纯 schema 声明（数据驱动）
    ├── HomePage.kt            # 主页 schema
    ├── DashboardPage.kt       # 仓库列表页 schema
    └── ...（随迁移增加）
```

## 4. 坐标系统定义（Grid）

```text
PageSchema
├── rows: List<RowSchema>           # 纵向行序列
│   ├── height: RowHeight           # Fixed(dp) | Weight(1f) | Wrap
│   └── cells: List<CellSchema>     # 行内单元序列
│       ├── column: Int             # 起始列（从 0）
│       ├── span: Int               # 占列数（默认 1）
│       ├── width: CellWidth        # Weight(1f) | Fixed(dp) | Wrap
│       └── component: Component    # 该单元渲染的组件
└── scrollable: Boolean             # 整页是否可滚动（内容区壳内滚动）
```

规则：
1. 页面宽度固定切分为 N 列（`PageSchema.columns`，默认 12，Material 网格）。
2. 每个 Cell 由（row, column, span）唯一定位——坐标确定，无流式歧义。
3. 行高三种模式：Fixed 固定 dp / Weight 均分剩余 / Wrap 内容自适应。
4. 组件渲染由 `ComponentRenderer` 按类型分发，**组件不感知坐标**，
   坐标由网格计算层解析——组件与布局解耦，杜绝"组件自己跑偏"。

## 5. 组件 schema 约定（固定字段）

- 每个组件一个 `data class`，字段固定（见 §3 各组件注释）。
- 组件**只解析自身字段**：`ComponentRenderer` 的 when 分发保证
  类型安全，未知字段不解析、不渲染。
- 组件**不持有布局参数**（宽高/间距/权重都在 Cell/Row 坐标层），
  渲染器只负责把组件画进给定 bounds。
- 状态组件（Loading/Empty/Error）由 `StateComponent.kind` 字段驱动，
  页面零逻辑。

## 6. 壳约束（AppShell 三区）

```text
┌──────────────────────────────┐
│ TopBar（固定高度，壳级）        │  ← 标题/返回/菜单（ShellState 驱动）
├──────────────────────────────┤
│ Content（唯一可滚动区）        │  ← 页面 schema 渲染于此，壳内滚动
│   └── PageRenderer            │
├──────────────────────────────┤
│ NavBar（固定高度，壳级）        │  ← 主导航/仓库分段导航（ShellState 驱动）
└──────────────────────────────┘
```

- 三区由 Compose `Box` + `Column` 硬约束：Content 高度 =
  `maxHeight - TopBar - NavBar`，**内容不可能越界**。
- inset 只在 Shell 消费一次：TopBar 顶部 padding = statusBars；
  NavBar 底部 padding = navigationBars。页面与组件零 inset 逻辑。
- 导航栏从"补丁式渲染"改为"壳状态驱动"：`ShellState.navBar` 枚举
  （Main / RepositorySections / Hidden / Floating），壳直接按状态
  渲染对应导航组件，不再事后修补 XML 视图。

## 7. 迁移路径（增量，不爆破）

| 步骤 | 内容 | 验收 |
|---|---|---|
| 1 | 骨架：shell/ + schema/ + layout/ + render/ | `assembleDebug` 通过 |
| 2 | 垂直切片：Home 页用 AppShell + schema 重绘 | 主页渲染与现有一致 |
| 3 | 迁移 Dashboard（列表 + 状态组件） | 列表/空态/加载态一致 |
| 4 | 迁移仓库详情（分段导航 → ShellState 驱动） | 分段导航无漂移 |
| 5 | 逐页迁移，删除旧布局与补丁导航 | 52 页全部走 schema |
| 6 | 删除旧壳 XML / 补丁导航 13 文件 | 无残留双轨 |

## 8. 验收标准（针对历史问题）

- 页面渲染区域 = 壳内容区，截图对比无重叠、无越界（历史：渲染越界）。
- 导航栏在任意页面/状态切换后位置像素级一致（历史：漂移）。
- 深色/浅色、键盘弹出、旋转后组件位置不变（历史：约束不一致）。
- 新增页面 = 声明 schema，不写布局代码（一致性保障）。

## 9. 与 Rust 核心的关系

- 页面数据（仓库列表、文件条目等）来自 `sunset-core`（经 UniFFI，阶段 6）。
- `PageSchema` 的字段命名与 Rust `FileEntry`/`RepositorySummary` 等
  模型对齐，后续 UniFFI 绑定后 schema 可直接映射核心数据。
- 本阶段 UI 壳重绘不依赖 UniFFI，先用现有 ViewModel/Repo 数据源。
