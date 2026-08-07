//! GitHub REST API 核心数据模型（serde 反序列化）。

use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};

/// GitHub 用户（精简版）。
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct User {
    pub login: String,
    pub id: u64,
    #[serde(default)]
    pub avatar_url: String,
    #[serde(default)]
    pub html_url: String,
    #[serde(default)]
    pub name: Option<String>,
    #[serde(default)]
    pub bio: Option<String>,
    #[serde(default)]
    pub public_repos: Option<u64>,
    #[serde(default)]
    pub followers: Option<u64>,
    #[serde(default)]
    pub following: Option<u64>,
}

/// GitHub 仓库（精简版）。
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Repository {
    pub id: u64,
    pub name: String,
    #[serde(default)]
    pub full_name: String,
    #[serde(default)]
    pub owner: Option<User>,
    #[serde(default)]
    pub description: Option<String>,
    #[serde(default)]
    pub html_url: String,
    #[serde(default)]
    pub language: Option<String>,
    #[serde(default)]
    pub fork: bool,
    #[serde(default)]
    pub stargazers_count: u64,
    #[serde(default)]
    pub forks_count: u64,
    #[serde(default)]
    pub open_issues_count: u64,
    #[serde(default)]
    pub default_branch: String,
    #[serde(default)]
    pub private: bool,
    #[serde(default)]
    pub archived: bool,
    #[serde(default)]
    pub license: Option<License>,
    #[serde(default)]
    pub updated_at: Option<DateTime<Utc>>,
    #[serde(default)]
    pub pushed_at: Option<DateTime<Utc>>,
}

/// 仓库许可证信息。
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct License {
    pub key: String,
    pub name: String,
    #[serde(default)]
    pub spdx_id: Option<String>,
}

/// Issue（问题）。
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Issue {
    pub id: u64,
    pub number: u64,
    pub title: String,
    #[serde(default)]
    pub state: String,
    #[serde(default)]
    pub body: Option<String>,
    #[serde(default)]
    pub user: Option<User>,
    #[serde(default)]
    pub labels: Vec<Label>,
    #[serde(default)]
    pub comments: u64,
    #[serde(default)]
    pub created_at: Option<DateTime<Utc>>,
    #[serde(default)]
    pub updated_at: Option<DateTime<Utc>>,
    #[serde(default)]
    pub closed_at: Option<DateTime<Utc>>,
    #[serde(default)]
    pub html_url: String,
}

/// Issue 标签。
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Label {
    pub name: String,
    #[serde(default)]
    pub color: String,
}

/// 仓库内容条目（文件/目录）。
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct RepoContentEntry {
    pub name: String,
    #[serde(rename = "type")]
    pub entry_type: String, // "file" | "dir" | "submodule" | "symlink"
    #[serde(default)]
    pub path: String,
    #[serde(default)]
    pub size: u64,
    #[serde(default)]
    pub download_url: Option<String>,
    #[serde(default)]
    pub sha: Option<String>,
    #[serde(default)]
    pub html_url: Option<String>,
}

/// 认证用户信息（GET /user）。
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AuthenticatedUser {
    pub login: String,
    pub id: u64,
    #[serde(default)]
    pub avatar_url: String,
    #[serde(default)]
    pub name: Option<String>,
    #[serde(default)]
    pub email: Option<String>,
}

/// Device Flow 设备码响应。
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DeviceCodeResponse {
    pub device_code: String,
    pub user_code: String,
    pub verification_uri: String,
    #[serde(default)]
    pub expires_in: u64,
    #[serde(default)]
    pub interval: u64,
}

/// Device Flow 轮询响应。
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DeviceAccessTokenResponse {
    #[serde(default)]
    pub access_token: Option<String>,
    #[serde(default)]
    pub token_type: Option<String>,
    #[serde(default)]
    pub scope: Option<String>,
    #[serde(default)]
    pub error: Option<String>,
    #[serde(default)]
    pub error_description: Option<String>,
}

/// Pull Request（精简版）。
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PullRequest {
    pub id: u64,
    pub number: u64,
    pub title: String,
    #[serde(default)]
    pub state: String,
    #[serde(default)]
    pub body: Option<String>,
    #[serde(default)]
    pub user: Option<User>,
    #[serde(default)]
    pub draft: bool,
    #[serde(default)]
    pub merged: bool,
    #[serde(default)]
    pub mergeable: Option<bool>,
    #[serde(default)]
    pub merged_at: Option<DateTime<Utc>>,
    #[serde(default)]
    pub created_at: Option<DateTime<Utc>>,
    #[serde(default)]
    pub updated_at: Option<DateTime<Utc>>,
    #[serde(default)]
    pub html_url: String,
    #[serde(default)]
    pub head: Option<RefInfo>,
    #[serde(default)]
    pub base: Option<RefInfo>,
}

/// Git 引用信息（PR 的 head/base）。
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct RefInfo {
    pub label: String,
    #[serde(default)]
    pub r#ref: String,
    #[serde(default)]
    pub sha: String,
}

/// Issue 评论。
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct IssueComment {
    pub id: u64,
    #[serde(default)]
    pub body: String,
    #[serde(default)]
    pub user: Option<User>,
    #[serde(default)]
    pub created_at: Option<DateTime<Utc>>,
    #[serde(default)]
    pub updated_at: Option<DateTime<Utc>>,
    #[serde(default)]
    pub html_url: String,
}

/// Release 资产。
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ReleaseAsset {
    pub id: u64,
    pub name: String,
    #[serde(default)]
    pub size: u64,
    #[serde(default)]
    pub content_type: String,
    #[serde(default)]
    pub browser_download_url: String,
    #[serde(default)]
    pub created_at: Option<DateTime<Utc>>,
}

/// Release。
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Release {
    pub id: u64,
    #[serde(default)]
    pub tag_name: String,
    #[serde(default)]
    pub name: Option<String>,
    #[serde(default)]
    pub body: Option<String>,
    #[serde(default)]
    pub draft: bool,
    #[serde(default)]
    pub prerelease: bool,
    #[serde(default)]
    pub html_url: String,
    #[serde(default)]
    pub created_at: Option<DateTime<Utc>>,
    #[serde(default)]
    pub published_at: Option<DateTime<Utc>>,
    #[serde(default)]
    pub assets: Vec<ReleaseAsset>,
}

/// Actions Workflow。
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ActionWorkflow {
    pub id: u64,
    pub name: String,
    #[serde(default)]
    pub path: String,
    #[serde(default)]
    pub state: String,
    #[serde(default)]
    pub created_at: Option<DateTime<Utc>>,
    #[serde(default)]
    pub updated_at: Option<DateTime<Utc>>,
}

/// Actions Run。
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ActionRun {
    pub id: u64,
    #[serde(default)]
    pub name: Option<String>,
    #[serde(default)]
    pub display_title: Option<String>,
    #[serde(default)]
    pub status: String,
    #[serde(default)]
    pub conclusion: Option<String>,
    #[serde(default)]
    pub head_branch: Option<String>,
    #[serde(default)]
    pub event: Option<String>,
    #[serde(default)]
    pub run_number: u64,
    #[serde(default)]
    pub workflow_id: u64,
    #[serde(default)]
    pub html_url: String,
    #[serde(default)]
    pub created_at: Option<DateTime<Utc>>,
    #[serde(default)]
    pub updated_at: Option<DateTime<Utc>>,
}

/// Actions Artifact。
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ActionArtifact {
    pub id: u64,
    pub name: String,
    #[serde(default)]
    pub size_in_bytes: u64,
    #[serde(default)]
    pub archive_download_url: String,
    #[serde(default)]
    pub expired: bool,
}

/// 仓库分支。
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Branch {
    pub name: String,
    #[serde(default)]
    pub protected: bool,
    #[serde(default)]
    pub commit: Option<BranchCommit>,
}

/// 分支提交信息。
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct BranchCommit {
    pub sha: String,
    #[serde(default)]
    pub url: String,
}

/// 通知。
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Notification {
    pub id: String,
    #[serde(default)]
    pub unread: bool,
    #[serde(default)]
    pub reason: Option<String>,
    #[serde(default)]
    pub subject: Option<NotificationSubject>,
    #[serde(default)]
    pub repository: Option<Repository>,
    #[serde(default)]
    pub updated_at: Option<DateTime<Utc>>,
}

/// 通知主题。
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct NotificationSubject {
    pub title: String,
    #[serde(default)]
    pub url: Option<String>,
    #[serde(default)]
    pub r#type: Option<String>,
}

/// 仓库文件内容（含 base64 解码后的正文）。
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FileContent {
    pub name: String,
    #[serde(default)]
    pub path: String,
    #[serde(default)]
    pub sha: String,
    #[serde(default)]
    pub size: u64,
    #[serde(default)]
    pub encoding: Option<String>,
    #[serde(default)]
    pub content: Option<String>,
}

impl FileContent {
    /// 解码文件正文（base64）。
    pub fn decoded(&self) -> Option<String> {
        let content = self.content.as_ref()?;
        if self.encoding.as_deref() == Some("base64") {
            use base64::Engine as _;
            let bytes = base64::engine::general_purpose::STANDARD
                .decode(content.trim())
                .ok()?;
            Some(String::from_utf8_lossy(&bytes).into_owned())
        } else {
            Some(content.clone())
        }
    }
}

/// 文件写入目标（用于上传/编辑前确认 sha 与冲突）。
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FileWriteTarget {
    pub path: String,
    #[serde(default)]
    pub sha: Option<String>,
    #[serde(default)]
    pub exists: bool,
}

/// 文件写入结果（提交信息）。
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FileWriteResult {
    #[serde(default)]
    pub commit_sha: Option<String>,
    #[serde(default)]
    pub commit_message: Option<String>,
    #[serde(default)]
    pub content_path: Option<String>,
}
