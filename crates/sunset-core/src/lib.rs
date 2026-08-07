//! SunsetGitHub 核心库。
//!
//! 纯 Rust 实现，不依赖 Android SDK。UI 层（Kotlin/Compose）通过
//! `sunset-ffi`（UniFFI）调用本库。
//!
//! 模块布局：
//! - [`github`]     GitHub REST API 客户端（登录、仓库、Issues 等）
//! - [`filemanager`] 本地文件引擎（条目、排序、复制/移动/压缩）
//! - [`markdown`]   Markdown 渲染（GFM，comrak）
//! - [`archive`]    压缩包处理（zip/tar/gzip）
//! - [`reverse`]    APK/Dex/ARSC 逆向工具（后续深化）
//! - [`error`]      统一错误类型

pub mod archive;
pub mod error;
pub mod filemanager;
pub mod github;
pub mod markdown;
pub mod reverse;

pub use error::{Error, Result};
