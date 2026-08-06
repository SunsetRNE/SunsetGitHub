# THIRD-PARTY NOTICES

本文件记录 SunsetGitHub 使用的第三方开源组件及其许可证。
许可证与 GPL-3.0 完全兼容（MIT / Apache-2.0 / BSD / MPL-2.0）。

## Rust 依赖（crates/sunset-core 与 crates/sunset-ffi）

| 组件 | 用途 | 许可证 |
|---|---|---|
| reqwest | HTTP 客户端（rustls） | MIT / Apache-2.0 |
| serde / serde_json | 序列化 | MIT / Apache-2.0 |
| thiserror | 错误类型 | MIT / Apache-2.0 |
| tracing | 日志 | MIT / Apache-2.0 |
| comrak | Markdown 渲染（GFM） | BSD-2-Clause |
| zip | ZIP 读写 | MIT |
| tar / flate2 | tar.gz 处理 | MIT / Apache-2.0 |
| chrono | 时间 | MIT / Apache-2.0 |
| base64 | Base64 编解码 | MIT / Apache-2.0 |
| rusty-axml | AXML（Android 二进制 XML）解析 | Apache-2.0 |
| quick-xml（rusty-axml 传递依赖） | XML 写出 | MIT |
| byteorder（rusty-axml 传递依赖） | 字节序读写 | MIT / Unlicense |

> 完整依赖树请见 `Cargo.lock`。引入新依赖前必须通过
> `cargo deny check`（配置见 `deny.toml`）。

## 历史 Kotlin 依赖（备份于 .backup/pre_rust_rewrite_20260806，仅参考）

| 组件 | 许可证 |
|---|---|
| Sora Editor (io.github.rosemoe) | MIT |
| Markwon (io.noties.markwon) | Apache-2.0 |
| Glide | BSD-2-Clause |
| AndroidSVG | Apache-2.0 |
| Apache Commons Compress | Apache-2.0 |
| PDFBox-Android | Apache-2.0 |
| AndroidX / Material / Compose | Apache-2.0 |
| JUnit 4（仅测试） | EPL-1.0（不进入分发物） |

## 参考来源（仅架构/行为参考，未复制代码）

- MT Manager 2.26.7（闭源）：交互行为参考，见 docs/MT管理器对标差距报告.md
- GitHub for Android（开源）：许可证清单结构参考
- 灵_AI记忆体 / AAswordman/Operit：架构概念参考（见 docs/ai-workspace/）