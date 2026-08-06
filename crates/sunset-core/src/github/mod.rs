//! GitHub REST API 客户端。
//!
//! 设计目标：替代原 Kotlin 自封装网络层 + OkHttp 职责，提供
//! 登录认证（Token / Device Flow）、仓库、Issues、PR、Releases、
//! Actions、文件读写、搜索、通知等核心 API。

pub mod actions;
pub mod auth;
pub mod branches;
pub mod client;
pub mod files;
pub mod issues;
pub mod models;
pub mod notifications;
pub mod pulls;
pub mod releases;
pub mod repos;
pub mod search;

pub use client::GitHubClient;
pub use models::*;