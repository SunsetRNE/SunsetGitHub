# SunsetGitHub

> ⚠️ **项目状态：Rust 核心重写进行中（2026-08-06 启动）**
>
> - 核心逻辑正迁移至 Rust（`crates/sunset-core` + `crates/sunset-ffi`），
>   许可证切换为 **GPL-3.0-or-later**（彻底开源）。
> - 重写路线图见 `docs/plans/RUST_REWRITE_PLAN.md`。
> - 旧 Kotlin 代码完整备份于 `.backup/pre_rust_rewrite_20260806/`（397MB），
>   在阶段 7 清理前仍保留 `app/` 中的历史实现。
> - 新依赖引入必须通过 `cargo deny check`（`deny.toml`）。

SunsetGitHub 是一个使用 Kotlin 编写的 Android GitHub 客户端。项目聚焦移动端仓库浏览体验，提供登录、仓库列表、仓库详情、文件预览、README / Markdown 预览、Issues 浏览和个人页展示等基础能力。

## 功能概览

- GitHub 账号登录
- 仓库列表浏览、搜索、排序与刷新
- 仓库详情、目录浏览与文件预览
- README / Markdown 内容预览
- Issues 列表与详情导航
- 个人页面、公开信息与仓库概览展示
- Material 风格 Android 界面

## 技术栈

- Kotlin
- AndroidX
- Material Components
- AppCompat / ViewBinding / XML View
- Jetpack Compose / Material3（渐进式迁移中）
- Gradle Kotlin DSL
- Gradle Version Catalog
- Gradle Wrapper

> 说明：本仓库已接入 Jetpack Compose，但不是 Android Studio 默认 Compose 模板。请沿用现有包名、Gradle Version Catalog、Fragment/NavHost 与渐进式 `ComposeView` 架构，不要按 `java/com/java/myapplication` 或默认模板目录重建项目。

## 快速开始

首次在 Operit / proot / ARM64 Linux 环境中构建前，建议先初始化 Android 构建环境：

```bash
chmod +x ./setup_android_env.sh
./setup_android_env.sh
```

然后使用 Gradle Wrapper 构建调试包：

```bash
./gradlew assembleDebug
```

常用命令：

```bash
./gradlew assembleDebug      # 构建 Debug APK
./gradlew assembleRelease    # 构建 Release APK
./gradlew test               # 运行单元测试
./gradlew lint               # 运行 Android Lint
```

Debug APK 默认输出位置：

```text
app/build/outputs/apk/debug/app-debug.apk
```

Release APK 默认输出位置：

```text
app/build/outputs/apk/release/app-release.apk
```

## 构建配置

主要构建文件：

- `settings.gradle.kts`：仓库名、模块包含、插件仓库和依赖仓库
- `build.gradle.kts`：根项目插件声明
- `app/build.gradle.kts`：Android 应用模块配置、SDK 版本、BuildConfig、本模块依赖
- `gradle/libs.versions.toml`：插件和依赖版本目录
- `gradle.properties`：Gradle / Android 构建属性
- `setup_android_env.sh`：Operit / ARM64 Linux 环境初始化脚本

当前应用模块使用 `compileSdk = 36`，初始化脚本会安装 Android 36 平台以及构建所需工具。脚本只会更新 `local.properties` 中的 `sdk.dir`，并保留已有本地键值。

## 资源分层策略

项目采用 Android 标准变体资源合并机制管理开发版与正式版资源：

```text
app/src/main/res/       # Debug / Release 共享的主线资源
app/src/debug/res/      # 仅 Debug 构建使用的少量开发专属覆盖资源
app/src/release/res/    # 仅 Release 构建使用的少量正式专属覆盖资源，可选
```

执行 `./gradlew assembleDebug` 时，资源来源是 `src/main/res + src/debug/res`；执行 `./gradlew assembleRelease` 时，资源来源是 `src/main/res + src/release/res`。同名资源会由变体目录覆盖 `main`，例如 `src/debug/res` 会覆盖 `src/main/res` 中的同名字符串。

维护规则：

- 普通业务 UI 文案、布局、颜色、样式应放在 `src/main/res`，作为 Debug 和 Release 共享的正式主线资源。
- Debug 专属内容才放在 `src/debug/res`，例如调试入口、Mock 环境提示、实验功能提示等；开发版和正式版应用名称必须一致，`app_name` 必须保留在 `src/main/res`，不能在 `src/debug/res` 或 `src/release/res` 中覆盖。
- Release 专属内容才放在 `src/release/res`，例如正式渠道标识或正式环境专属配置。
- 不要在 `src/debug/res` 中复制整份 `src/main/res` 字符串，也不要覆盖普通 UI 文案，例如 `title_home`、`title_settings`、`repository_contents_title_root` 等。
- 如果 Debug 临时资源要转为正式功能，应把正式文案提升到 `src/main/res`，并删除 `src/debug/res` 中对应覆盖项。

当前 Debug 字符串覆盖文件为：

```text
app/src/debug/res/values/strings_debug.xml
```

该文件只应保留少量明确的开发版覆盖项。历史上的 `strings_auto_missing.xml` 属于自动生成占位资源，不能再放入 `src/main/res` 或 `src/debug/res`，否则会覆盖正式文案并导致界面显示“标题 首页”“设置 theme section 标题”等占位文本。

`app/build.gradle.kts` 中的 `checkStringResources` 任务会检查字符串资源是否符合该策略，防止 Debug 占位资源再次大面积覆盖正式文案。

## 本地配置

`local.properties` 是本机配置文件，不应提交或上传到公开仓库。它通常包含：

```properties
sdk.dir=/path/to/android/sdk
github.oauth.client.id=
```

`github.oauth.client.id` 仅用于 Device Flow。当前应用不再使用浏览器 OAuth 回调流程，也不需要 `github.oauth.client.secret` 或 `github.oauth.redirect.uri`。仓库提供 `local.properties.example` 作为可提交模板，真实本机配置只放在本地 `local.properties`。

### Release 签名

如果需要让 `./gradlew assembleRelease` 产出已签名 APK，请先生成或准备一个本地 keystore：

```bash
keytool -genkeypair \
  -v \
  -keystore sunsetgithub-release.jks \
  -alias sunsetgithub \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

然后在本机 `local.properties` 中添加：

```properties
release.store.file=sunsetgithub-release.jks
release.store.password=你的 keystore 密码
release.key.alias=sunsetgithub
release.key.password=你的 key 密码
```

`release.store.file` 可以是绝对路径，也可以是相对仓库根目录的路径。四个 `release.*` 键必须同时填写；只填写一部分时，Gradle 会直接失败，避免误产出未按预期签名的 Release 包。

签名文件、密码和真实 OAuth 配置都属于本地机密，不要提交。

## 同步、上传与拉取配置

如果你说的是代码仓库同步：

- 上传到 GitHub / Gitee 等远端：配置 Git remote，例如 `origin`，然后提交并 push 源码文件。
- 从云端拉取到本地：使用同一个 remote 执行 pull / fetch，然后按需运行 `./setup_android_env.sh` 或补齐本机 `local.properties`。
- 这个目录当前可能只是普通工作区副本；如果没有 `.git/`，需要先 `git init` 或重新 clone / 关联远端。

应该同步的内容：

- `app/src/`
- `docs/`
- `gradle/`
- `tools/`
- `build.gradle.kts`
- `settings.gradle.kts`
- `gradle.properties`
- `gradlew`
- `gradlew.bat`
- `setup_android_env.sh`
- `README.md`
- `AGENTS.md`
- `.gitignore`
- `.gitattributes`
- `local.properties.example`

不应该同步的内容：

- `local.properties`
- `local.properties.bak`
- `.gradle/`
- `.kotlin/`
- `build/`
- `app/build/`
- `.backup/`
- APK / AAB 等构建产物
- 密钥、令牌、个人账号配置

如果你说的是应用内“上传文件到 GitHub 仓库”和“从 GitHub 仓库拉取/刷新到本地 UI”：

- 认证入口在本地配置和登录流程中，构建期配置来自 `local.properties`，运行期 Token 不应写入源码。
- GitHub API 权限取决于登录账号或 Token scope；文件上传/编辑需要目标仓库写权限。
- 分支、目标路径、提交信息和远端冲突处理属于应用内仓库文件写入流程，不应通过 Gradle 配置硬编码。

## 目录结构

```text
.
├── AGENTS.md                 # AI 导航与维护规则
├── README.md                 # 项目说明
├── settings.gradle.kts       # Gradle 项目设置
├── build.gradle.kts          # 顶层 Gradle 配置
├── gradle.properties         # Gradle 构建属性
├── gradlew / gradlew.bat     # Gradle Wrapper 命令
├── local.properties.example  # 本地配置模板
├── setup_android_env.sh      # Android 环境初始化脚本
├── app/                      # Android 应用源码与资源
├── docs/                     # 项目文档
├── gradle/                   # Gradle Wrapper 与版本目录
└── tools/                    # 构建辅助工具
```

## 文档

- `docs/ARCHITECTURE.md`：项目结构、代码边界与维护原则；如果该文件不存在，以当前源码树和 `AGENTS.md` 为准。

## 上传前检查

提交或同步前建议确认以下内容没有进入版本库：

```bash
git status --short
git ls-files local.properties local.properties.bak .backup
```

如果这些本地文件已经被追踪，需要先从索引移除并保留本地文件：

```bash
git rm --cached local.properties local.properties.bak
git rm -r --cached .backup
```

## 贡献

提交改动前，请先确认项目可以正常构建，并尽量保持代码风格与现有结构一致。