# 应用内工作区与批量同步设计记录

## 背景

本轮讨论的起点是：当前应用已经具备较好的 GitHub 仓库浏览、文件编辑、单文件提交和上传能力，但当需要把大量本地内容同步到多个仓库时，逐个文件上传成本过高。

实际痛点包括：

- 上百个文件需要一次性上传或覆盖。
- 目标可能是多个 GitHub 仓库。
- 需要支持“以本地内容覆盖远端某个路径/整个仓库”的场景。
- 现阶段不希望为了这个目标引入完整 Termux、proot Linux 或复杂本地 Git 运行时。

因此本轮选择的方向是：

```text
应用内部工作区 + GitHub Git Data API 批量提交 + 实验性同步入口
```

而不是完整内置 Termux 或完整本地 Git。

---

## 路线取舍

### 不优先做完整 Termux / Linux

完整 Termux 或 proot Linux 可以提供强大的命令行能力，但对当前目标而言过重。

主要问题：

- APK / 下载体积大。
- proot/rootfs 初始化复杂。
- Android 版本和厂商 ROM 兼容性不可控。
- systemd、Docker、内核能力不可用，用户预期管理成本高。
- 和“批量上传 GitHub 文件”这个目标不完全匹配。

结论：完整 Linux 环境暂不作为当前阶段目标。

### 不立即引入本地 Git 引擎

曾讨论过路线 B 的三种选择：

```text
1. JGit
2. libgit2
3. native git 二进制
```

最终判断：当前阶段只是为了解决批量上传、覆盖同步和多仓库提交，不需要先引入完整本地 Git。

三种路线定位如下：

| 方案 | 适合场景 | 当前结论 |
|---|---|---|
| JGit | App 内部 Git SDK、UI 化 status/diff/commit/push | 未来本地 Git 后端首选候选，但暂不实现 |
| libgit2 | 专业 Git 客户端、高性能 Git 内核 | 远期方案，当前过重 |
| native git | 终端优先、用户手敲 git 命令 | 如果未来内置终端成为核心再考虑 |

当前保留 `LocalGitBackend` 抽象，但默认不实现具体本地 Git。

---

## 核心产品边界

当前阶段的边界是：

```text
外部内容 -> 导入到 App 内部工作区 -> 扫描/计划 -> GitHub 批量提交
```

而不是：

```text
直接长期操作外部目录
```

这样做的原因：

- 避免 Android 外部存储权限和 SAF 长期授权复杂度。
- 避免误删或误改用户外部文件。
- 所有待同步内容都在 App 私有目录，可控、可扫描、可忽略、可检测敏感文件。
- 后续同步、快照、历史记录、冲突处理都更容易统一。

---

## 当前实现概览

### 领域模型

新增工作区领域模型：

```text
app/src/main/kotlin/com/Sunset/REN/GitHub/domain/workspace/WorkspaceModels.kt
app/src/main/kotlin/com/Sunset/REN/GitHub/domain/workspace/WorkspaceGateway.kt
```

主要模型：

- `WorkspaceProject`
- `WorkspaceRemoteBinding`
- `WorkspaceFile`
- `WorkspaceSnapshot`
- `WorkspaceImportRequest`
- `WorkspaceImportSource`
- `WorkspaceImportOptions`
- `WorkspaceImportResult`
- `SensitiveWorkspaceFile`
- `WorkspaceFileStatus`

其中 `WorkspaceRemoteBinding` 提供：

```kotlin
val normalizedRemotePath: String
```

用于把远端路径统一成 GitHub tree API 需要的不带首尾斜杠的路径。

### 同步模型

新增同步领域模型：

```text
app/src/main/kotlin/com/Sunset/REN/GitHub/domain/sync/WorkspaceSyncModels.kt
app/src/main/kotlin/com/Sunset/REN/GitHub/domain/sync/WorkspaceSyncGateway.kt
```

主要模型：

- `WorkspaceSyncRequest`
- `WorkspaceSyncMode`
- `WorkspaceSyncOptions`
- `WorkspaceSyncPlan`
- `WorkspaceSyncOperation`
- `WorkspaceSyncConflict`
- `WorkspaceSyncProgress`
- `WorkspaceSyncResult`
- `WorkspaceSyncOperationFailure`

同步模式：

```kotlin
enum class WorkspaceSyncMode {
    Incremental,
    MirrorRemotePath,
    UploadOnly
}
```

当前实现重点支持：

- `UploadOnly`
- `MirrorRemotePath`

`Incremental` 当前在 GitHub API 后端中暂按 `UploadOnly` 处理，并记录 warning，后续接入快照后再做真正增量。

### 本地 Git 预留抽象

新增：

```text
app/src/main/kotlin/com/Sunset/REN/GitHub/domain/sync/LocalGitBackend.kt
```

该抽象预留：

- clone
- status
- diff
- commit
- pull
- push

当前默认提供 `NoOpLocalGitBackend`，避免过早绑定 JGit/libgit2/native git。

### Git Data API 抽象

新增：

```text
app/src/main/kotlin/com/Sunset/REN/GitHub/domain/sync/RemoteRepositoryCommitBackend.kt
```

它抽象 GitHub Git Data API 的最小提交能力：

- `getBranchHead`
- `getCommit`
- `getTree`
- `createBlob`
- `createTree`
- `createCommit`
- `updateBranchHead`

对应 GitHub 批量提交流程：

```text
branch ref -> commit -> tree -> blobs -> new tree -> new commit -> update ref
```

### GitHub Git Data API 实现

新增：

```text
app/src/main/kotlin/com/Sunset/REN/GitHub/data/github/GitHubApiRemoteRepositoryCommitBackend.kt
```

负责实际调用 GitHub Git Data API。

### 工作区文件扫描器

新增：

```text
app/src/main/kotlin/com/Sunset/REN/GitHub/data/workspace/AppInternalWorkspaceFileScanner.kt
```

能力：

- 递归扫描 App 内部工作区目录。
- 计算文件大小、mtime、SHA-256。
- 计算仓库相对路径。
- 应用默认忽略规则。
- 检测敏感文件。

默认忽略：

```text
.git/
.gradle/
.idea/
.kotlin/
.backup/
.DS_Store
build/
app/build/
node_modules/
dist/
target/
*.apk
*.aab
*.class
*.dex
*.tmp
*.log
local.properties
local.properties.bak
```

默认敏感文件检测：

```text
.env
.env.local
local.properties
local.properties.bak
secrets.properties
github.properties
google-services.json
*.jks
*.keystore
id_rsa
id_ed25519
id_ecdsa
```

### App 内部工作区 Gateway

新增：

```text
app/src/main/kotlin/com/Sunset/REN/GitHub/data/workspace/AppInternalWorkspaceGateway.kt
```

当前是最小实现，使用：

```text
SharedPreferences + JSON
```

保存 metadata，文件存放在：

```text
context.filesDir/workspaces/repositories/{workspaceId}
```

已支持：

- 创建工作区
- 列出工作区
- 获取工作区
- 重命名工作区
- 删除工作区
- 绑定远端仓库
- 清除远端绑定
- 导入内部路径
- 导入 ContentUri 单文件
- 扫描工作区
- 保存/读取快照

### GitHub API 工作区同步后端

新增：

```text
app/src/main/kotlin/com/Sunset/REN/GitHub/data/github/GitHubApiWorkspaceSyncBackend.kt
```

职责：

```text
工作区扫描结果 + 远端 tree -> WorkspaceSyncPlan -> GitHub commit
```

`buildPlan()` 执行：

1. 解析工作区根目录。
2. 扫描本地文件。
3. 获取远端 branch HEAD。
4. 获取 base commit。
5. 获取 recursive tree。
6. 对比本地文件和远端 blob。
7. 生成 Add / Modify / Delete 操作。
8. 返回同步计划。

`executePlan()` 执行：

1. 检查敏感文件。
2. 检查冲突。
3. 检查危险删除确认。
4. 处理 dry run。
5. 重新检查远端 HEAD 是否变化。
6. 为 Add / Modify 创建 blob。
7. 为 Delete 创建 `sha = null` tree entry。
8. 创建 tree。
9. 创建 commit。
10. 更新 branch HEAD。

### 实验性 UI 入口

新增：

```text
app/src/main/res/layout/fragment_workspace_sync.xml
app/src/main/kotlin/com/Sunset/REN/GitHub/ui/workspace/WorkspaceSyncFragment.kt
app/src/main/kotlin/com/Sunset/REN/GitHub/ui/workspace/WorkspaceSyncViewModel.kt
```

接入导航：

```text
app/src/main/res/navigation/mobile_navigation.xml
```

设置页新增入口按钮：

```text
app/src/main/res/layout/fragment_settings.xml
app/src/main/kotlin/com/Sunset/REN/GitHub/ui/settings/SettingsFragment.kt
```

当前入口路径：

```text
设置 -> 打开实验性工作区同步
```

---

## 当前可用流程

目前可以通过实验性入口执行以下流程：

1. 创建内部工作区。
2. 输入应用可访问的绝对路径，导入文件或目录。
3. 输入 GitHub owner/repo/branch/remotePath。
4. 输入 commit message。
5. 选择 UploadOnly 或 Mirror 模式。
6. 先 Dry Run 生成计划。
7. 确认计划后执行同步。

Mirror 模式如果要删除远端多余文件，必须勾选：

```text
我确认允许删除远端文件
```

---

## 同步模式说明

### UploadOnly

适合较安全的批量上传。

行为：

```text
本地有，远端没有 -> Add
本地有，远端不同 -> Modify
远端有，本地没有 -> 不处理
```

### MirrorRemotePath

适合覆盖性同步。

行为：

```text
本地有，远端没有 -> Add
本地有，远端不同 -> Modify
远端有，本地没有 -> Delete
```

危险点：可能删除远端文件。因此需要：

```kotlin
allowDeletes = true
destructiveOperationConfirmed = true
```

### Incremental

当前暂按 UploadOnly 处理。

未来应基于 `WorkspaceSnapshot` 和远端状态做更精确的增量计算。

---

## 安全边界

当前实现中特别考虑了以下安全边界：

- 不把外部路径作为长期工作区。
- 外部内容必须导入 App 私有目录。
- 默认忽略构建产物、缓存、local-only 文件。
- 默认检测敏感文件。
- Mirror/Delete 必须二次确认。
- 执行计划时会重新检查远端分支 HEAD。
- 如果远端 HEAD 已变化，默认阻止执行，要求重新生成计划。
- 更新 ref 默认 `force = false`，不做强推。

---

## 当前限制

当前版本是 MVP/实验性入口，限制如下：

1. UI 只能自动选择最近工作区，暂没有工作区列表切换。
2. 导入 UI 当前主要支持绝对路径输入。
3. 底层支持 ContentUri 单文件导入，但 UI 尚未接系统文件选择器。
4. 暂不支持 SAF 文件夹树 URI 导入。
5. 暂不支持 ZIP 自动解压导入。
6. 暂不支持后台服务保活。
7. 暂不支持同步队列。
8. 暂不支持 Room 持久化。
9. 暂不支持真正本地 Git 的 branch/merge/rebase。
10. `Incremental` 还不是真正增量。
11. 暂未提供漂亮的变更列表 UI，仅日志文本展示。
12. 暂未做分批提交，超过 `maxFilesPerCommit` 只 warning。

---

## 后续建议

### 短期

优先完善可用性：

- 工作区列表和切换。
- 系统文件选择器导入单文件。
- SAF 文件夹导入。
- ZIP 导入。
- 更清晰的 Dry Run 变更列表。
- 同步前敏感文件确认 UI。
- 执行中禁用按钮和进度条。
- 同步完成后保存快照。

### 中期

完善同步体验：

- 基于 `WorkspaceSnapshot` 实现真正 Incremental。
- 支持分批 commit。
- 支持同步历史。
- 支持多仓库绑定和批量队列。
- 支持冲突处理页面。
- 支持失败文件重试。

### 远期

如果产品方向需要，再考虑：

- JGit 后端。
- native git 后端。
- libgit2 后端。
- 内置命令终端。
- 更完整的仓库本地化体验。

---

## 已验证

本轮新增代码多次执行：

```bash
./gradlew :app:compileDebugKotlin --no-daemon --offline
```

结果均为：

```text
BUILD SUCCESSFUL
```

---

## 当前模块关系图

```text
SettingsFragment
  -> WorkspaceSyncFragment
      -> WorkspaceSyncViewModel
          -> AppInternalWorkspaceGateway
              -> AppInternalWorkspaceFileScanner
          -> GitHubApiWorkspaceSyncBackend
              -> GitHubApiRemoteRepositoryCommitBackend
                  -> GitHub Git Data API
```

领域抽象关系：

```text
WorkspaceGateway
WorkspaceSyncBackend
RemoteRepositoryCommitBackend
LocalGitBackend (reserved)
```

---

## 决策总结

本轮最终决策是：

```text
先不做完整 Termux。
先不做完整本地 Git。
先通过 App 内部工作区 + GitHub Git Data API 解决批量上传和覆盖同步。
```

这样可以最快解决当前“上百文件、多仓库、覆盖更新”的紧急需求，同时保留未来扩展到 JGit、native git、libgit2 或轻量终端的接口边界。
