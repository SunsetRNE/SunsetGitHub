# SunsetGitHub

> 第三方 GitHub 安卓客户端 · GPL-3.0 彻底开源 · Rust 核心重写进行中

[![License: GPL-3.0](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](LICENSE)
[![Language: Kotlin](https://img.shields.io/badge/UI-Kotlin%2FCompose-orange)](app/src/main/kotlin)
[![Language: Rust](https://img.shields.io/badge/Core-Rust-green)](crates/sunset-core)
[![Platform: Android](https://img.shields.io/badge/Platform-Android-3DDC84)](app/src/main/AndroidManifest.xml)

SunsetGitHub 是一款聚焦移动端仓库浏览体验的 Android GitHub 客户端，提供登录、仓库浏览、文件预览、README / Markdown 预览、Issues、文件上传编辑等功能。

项目正在进行 **Rust 核心重写**：业务逻辑逐步迁移至纯 Rust（`crates/sunset-core`），UI 保留 Kotlin/Compose 壳，经 UniFFI 桥接（路线图见 [`docs/plans/RUST_REWRITE_PLAN.md`](docs/plans/RUST_REWRITE_PLAN.md)）。

## ✨ 功能概览

- 🔐 GitHub 登录（Token + Device Flow）
- 📚 仓库列表、搜索、排序与刷新
- 📁 仓库目录浏览、文件预览与编辑上传（Contents API）
- 📝 README / Markdown 预览（GFM）
- 🐛 Issues 列表、详情与评论
- 🔀 Pull Requests 创建与浏览
- 🚀 Releases 列表与资产上传
- ⚙️ Actions：工作流触发、运行状态、日志下载
- 👤 个人页与仓库概览

## 🏗️ 架构（双栈）

```text
SunsetGitHub（GPL-3.0）
├── crates/sunset-core     # 纯 Rust 核心（无 Android 依赖）
│   ├── github/            # GitHub REST API（reqwest + rustls）
│   ├── filemanager/       # 文件引擎（条目/排序/类型）
│   ├── markdown/          # GFM 渲染（comrak）
│   ├── archive/           # zip/tar.gz（zip/tar/flate2）
│   └── reverse/           # APK/Dex/ARSC 逆向（自研）
├── crates/sunset-ffi      # UniFFI 桥接层 → Kotlin/Swift
├── app/                   # Kotlin + Compose UI 壳（渐进精简中）
├── LICENSE                # GPL-3.0（官方全文）
├── NOTICE / THIRD_PARTY_NOTICES.md
└── deny.toml              # cargo-deny 许可证门禁
```

## 📈 重写进度

| 阶段 | 内容 | 状态 |
|---|---|---|
| 0 | 备份旧代码（`.backup/pre_rust_rewrite_20260806/`） | ✅ |
| 1 | Cargo workspace + sunset-core 骨架 + GPL-3.0/NOTICE | ✅ 2026-08-06 |
| 2 | GitHub API 深化：Issues/PR/Releases/Actions/文件上传编辑 | ✅ 2026-08-06 |
| 3 | 文件引擎深化：复制/移动/回收站/递归搜索/双栏状态 | ⬜ |
| 4 | 逆向工具链：Dex/ARSC/AXML 解析 | ⬜ |
| 5 | AI 工作区：gix Git 操作、工具运行时 | ⬜ |
| 6 | UniFFI 接入：生成 Kotlin 绑定替换 UI 层调用 | ⬜ |
| 7 | UI 壳清理，发布 1.0 | ⬜ |

## 🛠️ 构建

### Rust 核心（推荐先验证）

```bash
cargo build --workspace
cargo test --workspace
cargo clippy --workspace --all-targets
cargo deny check licenses   # 许可证门禁（需 cargo-deny）
```

### Android 应用

首次在 Operit / proot / ARM64 Linux 环境构建前，先初始化环境：

```bash
chmod +x ./setup_android_env.sh
./setup_android_env.sh
./gradlew assembleDebug     # Debug APK → app/build/outputs/apk/debug/
./gradlew assembleRelease   # Release APK
```

技术栈：Kotlin · AndroidX · Material Components · AppCompat/ViewBinding · Jetpack Compose（渐进迁移）· Gradle Kotlin DSL · Version Catalog。

## 🔒 许可证与合规

- 项目以 **GPL-3.0-or-later** 发布（见 [`LICENSE`](LICENSE)）。
- 第三方依赖经 `cargo-deny` 门禁校验：允许 MIT / Apache-2.0 / BSD / MPL-2.0 / ISC / Zlib / CC0 等宽松许可，拒绝 GPL-2.0-only / EPL / LGPL-2.0 / AGPL-3.0（见 [`deny.toml`](deny.toml)）。
- 第三方声明见 [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md)。
- **不复用** MT 管理器 / GitHub 官方应用的代码与资源。

## ⚙️ 本地配置

`local.properties` 为本机配置，**禁止提交**（已 gitignore）：

```properties
sdk.dir=/path/to/android/sdk
github.oauth.client.id=      # Device Flow 客户端 ID
```

Release 签名请在本地 `local.properties` 配置 `release.store.file` / `release.store.password` / `release.key.alias` / `release.key.password` 四个键。模板见 [`local.properties.example`](local.properties.example)。

## 📂 目录结构

```text
.
├── AGENTS.md                 # AI 导航与维护规则
├── README.md                 # 本文件
├── Cargo.toml                # Rust workspace 配置
├── crates/                   # Rust 核心（sunset-core / sunset-ffi）
├── app/                      # Android 应用（Kotlin + Compose）
├── docs/                     # 项目文档与规划
├── gradle/                   # Gradle Wrapper 与版本目录
├── tools/                    # 构建辅助工具
├── LICENSE / NOTICE / THIRD_PARTY_NOTICES.md
├── deny.toml                 # cargo-deny 许可门禁
└── setup_android_env.sh      # Android 环境初始化脚本
```

## 🤝 贡献

提交改动前请确认：

1. `cargo build --workspace` 与 `cargo test --workspace` 全绿
2. `cargo clippy` 无新增警告
3. 新依赖通过 `cargo deny check`
4. 不提交本地机密（`local.properties`、`*.jks`、token 等）

保持代码风格与现有结构一致，改动范围聚焦在对应模块。
