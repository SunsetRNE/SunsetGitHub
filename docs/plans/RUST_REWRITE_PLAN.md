# Rust 重写路线图（2026-08-06 启动）

> 状态：文件管理器双蓝本重构（Rust 核心侧）已完成 ✅（2026-08-06）
> 决策：核心逻辑 Rust 化（GPL-3.0-or-later 彻底开源），UI 层保留
> Kotlin/Compose 壳，经 UniFFI 桥接。旧代码备份于
> `.backup/pre_rust_rewrite_20260806/`（397MB，含全部源码与文档）。

## 1. 最终目标形态

```text
SunsetGitHub（GPL-3.0）
├── crates/sunset-core     # 纯 Rust 核心（无 Android 依赖）
│   ├── github/            # GitHub REST API（reqwest）
│   ├── filemanager/       # 文件引擎（条目/排序/类型/冲突/root 命令）
│   ├── markdown/          # GFM 渲染（comrak）
│   ├── archive/           # zip/tar.gz（zip/tar/flate2）+ 包内浏览
│   └── reverse/           # APK/Dex/ARSC 逆向（自研）
├── crates/sunset-ffi      # UniFFI 桥接层 → Kotlin/Swift
├── app/                   # Kotlin + Compose UI 壳（逐步精简）
├── LICENSE                # GPL-3.0（官方全文）
├── NOTICE / THIRD_PARTY_NOTICES.md
└── deny.toml              # cargo-deny 许可证门禁
```

## 2. 许可证红线

- 允许：MIT、Apache-2.0、BSD-2/3、MPL-2.0、ISC、Zlib、CC0
- 拒绝：GPL-2.0-only、EPL、LGPL-2.0、AGPL-3.0（deny.toml 已配置）
- 每次引入依赖跑 `cargo deny check`
- 不复用 MT 管理器 / GitHub 官方应用代码与资源

## 3. 阶段清单

| 阶段 | 内容 | 状态 |
|---|---|---|
| 0 | 备份旧代码（.backup/pre_rust_rewrite_20260806） | ✅ |
| 1 | Cargo workspace + sunset-core 骨架 + 测试 + GPL-3.0/NOTICE | ✅ 2026-08-06 |
| 2 | GitHub API 深化：Issues/PR/Releases/Actions/文件上传编辑 | ✅ 2026-08-06 |
| 3 | 文件引擎深化：复制/移动/回收站/递归搜索/双栏状态 | ✅ 2026-08-06 |
| 4 | 逆向工具链：Dex 类/方法/字符串解析、ARSC 解析、AXML 解码 | ✅ 2026-08-06 |
| 4.5 | 文件管理器双蓝本重构（Rust 核心侧）：FileItem 属性快照、FileJob 冲突/错误策略、压缩包内浏览、root 命令生成器 | ✅ 2026-08-06 |
| 5 | AI 工作区：gix Git 操作、工具运行时、记忆模型 | ⬜ |
| 6 | UniFFI 接入：生成 Kotlin 绑定，按双蓝本重构 Kotlin UI 壳 | ⬜ |
| 7 | UI 壳清理：删除旧 Kotlin 业务逻辑，发布 1.0 | ⬜ |

## 3.5 双蓝本重构记录（阶段 4.5）

蓝本来源（均已克隆至 `/root/research/`，GPL-3.0 兼容）：
- **Material Files**（zhanghai，8.7k★）：架构蓝本
  - `FileItem` → `entry.rs`：属性快照（modified/symlink/mime）、`load_entry()`
  - `FileJob` → `operation.rs`：`ConflictAction`（MergeOrReplace/Rename/Skip/Cancel）、
    `ErrorAction`（Retry/Skip/Cancel）、`run_with_options` 回调模型、目录合并语义
- **Amaze**（TeamAmaze，6.3k★）：root 能力蓝本
  - `showcontents` → `archive.rs`：`list_archive_dir()` 分层浏览 + 目录合成去重
  - `filesystem/root/` → `root.rs`：12 命令生成器（POSIX 单引号转义 +
    mount rw/ro 自动包装 + ls 输出解析 + 权限串双向转换）

改进点：Amaze 原版 `getCommandLineString` 用白名单字符过滤（会删路径字符），
Rust 侧改为标准 POSIX 转义；Amaze 目录 size 取首条目，Rust 侧目录统一 size=0。

## 4. 每阶段验收标准

- `cargo build --workspace` 与 `cargo test --workspace` 全绿
- `cargo clippy --workspace` 无新增警告
- `cargo deny check` 通过
- 涉及 Android 集成时：`./gradlew assembleDebug` 可安装