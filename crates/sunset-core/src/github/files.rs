//! 仓库文件读写 API：预览/写入目标/创建/更新/删除。
//!
//! 对应原 Kotlin `updateFileContent` / `createFileContent` /
//! `deleteFileContent` / `getWriteTarget` 等（GitHub Contents API）。

use reqwest::Method;

use crate::error::{Error, Result};
use crate::github::client::GitHubClient;
use crate::github::models::{FileContent, FileWriteResult, FileWriteTarget};

/// 文件写入请求（创建/更新共用）。
#[derive(Debug, Clone, serde::Serialize)]
pub struct WriteFileRequest {
    pub message: String,
    /// base64 编码的文件内容。
    pub content: String,
    /// 更新已存在文件时必填（当前 blob sha）；创建新文件时省略。
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sha: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub branch: Option<String>,
}

impl GitHubClient {
    /// 获取文件内容（含 base64 解码）。
    pub async fn get_file_content(
        &self,
        owner: &str,
        repo: &str,
        path: &str,
        branch: Option<&str>,
    ) -> Result<FileContent> {
        let mut url = format!(
            "/repos/{owner}/{repo}/contents/{}",
            path.trim_start_matches('/')
        );
        if let Some(branch) = branch {
            url.push_str(&format!("?ref={branch}"));
        }
        self.get_json(&url).await
    }

    /// 获取文件写入目标：确认文件是否存在及其 sha（冲突检测基础）。
    pub async fn get_write_target(
        &self,
        owner: &str,
        repo: &str,
        path: &str,
        branch: Option<&str>,
    ) -> Result<FileWriteTarget> {
        let clean_path = path.trim_start_matches('/');
        let url = format!("/repos/{owner}/{repo}/contents/{clean_path}");

        let mut req = self.http().get(format!(
            "{}/{}",
            crate::github::client::GITHUB_API_BASE,
            url.trim_start_matches('/')
        ));
        if let Some(token) = self.token() {
            req = req.bearer_auth(token);
        }
        if let Some(branch) = branch {
            req = req.query(&[("ref", branch)]);
        }

        let resp = req.send().await.map_err(Error::from)?;
        let status = resp.status();
        if status == reqwest::StatusCode::NOT_FOUND {
            return Ok(FileWriteTarget {
                path: clean_path.to_string(),
                sha: None,
                exists: false,
            });
        }
        let resp = Self::check_status(resp).await?;
        let content: FileContent = resp.json().await.map_err(Error::from)?;
        Ok(FileWriteTarget {
            path: clean_path.to_string(),
            sha: if content.sha.is_empty() {
                None
            } else {
                Some(content.sha)
            },
            exists: true,
        })
    }

    /// 创建文件（内容自动 base64 编码）。
    pub async fn create_file(
        &self,
        owner: &str,
        repo: &str,
        path: &str,
        message: &str,
        content: &[u8],
        branch: Option<&str>,
    ) -> Result<FileWriteResult> {
        self.write_file_inner(owner, repo, path, message, content, None, branch)
            .await
    }

    /// 更新文件（需要正确 sha，防止覆盖冲突）。
    // GitHub Contents API 对创建与更新统一使用 PUT，故参数较多属 API 语义要求。
    #[allow(clippy::too_many_arguments)]
    pub async fn update_file(
        &self,
        owner: &str,
        repo: &str,
        path: &str,
        message: &str,
        content: &[u8],
        sha: &str,
        branch: Option<&str>,
    ) -> Result<FileWriteResult> {
        self.write_file_inner(owner, repo, path, message, content, Some(sha), branch)
            .await
    }

    /// 删除文件。
    pub async fn delete_file(
        &self,
        owner: &str,
        repo: &str,
        path: &str,
        message: &str,
        sha: &str,
        branch: Option<&str>,
    ) -> Result<()> {
        let mut body = serde_json::json!({
            "message": message,
            "sha": sha,
        });
        if let Some(branch) = branch {
            body["branch"] = serde_json::Value::String(branch.to_string());
        }
        self.request_json::<serde_json::Value>(
            Method::DELETE,
            &format!(
                "/repos/{owner}/{repo}/contents/{}",
                path.trim_start_matches('/')
            ),
            Some(&body),
        )
        .await?;
        Ok(())
    }

    // GitHub Contents API 对创建与更新统一使用 PUT（官方文档），
    // 通过是否携带 sha 区分创建/更新语义。
    #[allow(clippy::too_many_arguments)]
    async fn write_file_inner(
        &self,
        owner: &str,
        repo: &str,
        path: &str,
        message: &str,
        content: &[u8],
        sha: Option<&str>,
        branch: Option<&str>,
    ) -> Result<FileWriteResult> {
        use base64::Engine as _;
        let encoded = base64::engine::general_purpose::STANDARD.encode(content);

        let request = WriteFileRequest {
            message: message.to_string(),
            content: encoded,
            sha: sha.map(|s| s.to_string()),
            branch: branch.map(|b| b.to_string()),
        };

        let resp: serde_json::Value = self
            .request_json(
                Method::PUT,
                &format!(
                    "/repos/{owner}/{repo}/contents/{}",
                    path.trim_start_matches('/')
                ),
                Some(&serde_json::to_value(&request)?),
            )
            .await?;

        let commit_sha = resp
            .pointer("/commit/sha")
            .and_then(|v| v.as_str())
            .map(|s| s.to_string());
        let commit_message = resp
            .pointer("/commit/message")
            .and_then(|v| v.as_str())
            .map(|s| s.to_string());
        let content_path = resp
            .pointer("/content/path")
            .and_then(|v| v.as_str())
            .map(|s| s.to_string());

        Ok(FileWriteResult {
            commit_sha,
            commit_message,
            content_path,
        })
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn write_request_serializes_with_sha() {
        let req = WriteFileRequest {
            message: "update".into(),
            content: "aGVsbG8=".into(),
            sha: Some("abc123".into()),
            branch: None,
        };
        let value = serde_json::to_value(&req).unwrap();
        assert_eq!(value["message"], "update");
        assert_eq!(value["sha"], "abc123");
        assert!(value.get("branch").is_none());
    }

    #[test]
    fn file_content_decodes_base64() {
        use base64::Engine as _;
        let encoded = base64::engine::general_purpose::STANDARD.encode("hello rust");
        let content = FileContent {
            name: "f.txt".into(),
            path: "f.txt".into(),
            sha: "s".into(),
            size: 10,
            encoding: Some("base64".into()),
            content: Some(encoded),
        };
        assert_eq!(content.decoded().as_deref(), Some("hello rust"));
    }
}
