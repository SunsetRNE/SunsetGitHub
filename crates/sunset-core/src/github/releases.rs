//! Releases API：列表/详情/创建/资产上传。

use reqwest::Method;

use crate::error::Result;
use crate::github::client::GitHubClient;
use crate::github::models::{Release, ReleaseAsset};

/// 创建 Release 请求。
#[derive(Debug, Clone, serde::Serialize)]
pub struct CreateReleaseRequest {
    pub tag_name: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub body: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub draft: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub prerelease: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub target_commitish: Option<String>,
}

impl GitHubClient {
    /// 列出仓库 Releases。
    pub async fn list_releases(
        &self,
        owner: &str,
        repo: &str,
        per_page: u32,
    ) -> Result<Vec<Release>> {
        self.get_json(&format!(
            "/repos/{owner}/{repo}/releases?per_page={per_page}"
        ))
        .await
    }

    /// 按 tag 获取 Release。
    pub async fn get_release_by_tag(&self, owner: &str, repo: &str, tag: &str) -> Result<Release> {
        let encoded = urlencode(tag);
        self.get_json(&format!("/repos/{owner}/{repo}/releases/tags/{encoded}"))
            .await
    }

    /// 创建 Release。
    pub async fn create_release(
        &self,
        owner: &str,
        repo: &str,
        request: &CreateReleaseRequest,
    ) -> Result<Release> {
        self.request_json(
            Method::POST,
            &format!("/repos/{owner}/{repo}/releases"),
            Some(&serde_json::to_value(request)?),
        )
        .await
    }

    /// 上传 Release 资产（二进制）。
    pub async fn upload_release_asset(
        &self,
        owner: &str,
        repo: &str,
        release_id: u64,
        name: &str,
        content_type: &str,
        bytes: &[u8],
    ) -> Result<ReleaseAsset> {
        let encoded_name = urlencode(name);
        let url = format!(
            "https://uploads.github.com/repos/{owner}/{repo}/releases/{release_id}/assets?name={encoded_name}"
        );

        let mut req = self
            .http()
            .post(url)
            .header(reqwest::header::CONTENT_TYPE, content_type)
            .body(bytes.to_vec());
        if let Some(token) = self.token() {
            req = req.bearer_auth(token);
        }
        let resp = req.send().await.map_err(crate::error::Error::from)?;
        let resp = Self::check_status(resp).await?;
        resp.json().await.map_err(crate::error::Error::from)
    }
}

/// URL 编码（与 repos.rs 共用，导出复用）。
pub(crate) fn urlencode(input: &str) -> String {
    let mut out = String::new();
    for byte in input.bytes() {
        match byte {
            b'A'..=b'Z' | b'a'..=b'z' | b'0'..=b'9' | b'-' | b'_' | b'.' | b'~' => {
                out.push(byte as char)
            }
            _ => out.push_str(&format!("%{byte:02X}")),
        }
    }
    out
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn create_release_serializes() {
        let req = CreateReleaseRequest {
            tag_name: "v1.0.0".into(),
            name: Some("Release 1.0".into()),
            body: None,
            draft: Some(false),
            prerelease: None,
            target_commitish: None,
        };
        let value = serde_json::to_value(&req).unwrap();
        assert_eq!(value["tag_name"], "v1.0.0");
        assert_eq!(value["name"], "Release 1.0");
        assert_eq!(value["draft"], false);
        assert!(value.get("body").is_none());
    }

    #[test]
    fn urlencode_handles_tag_chars() {
        assert_eq!(urlencode("v1.0.0"), "v1.0.0");
        assert_eq!(urlencode("a/b"), "a%2Fb");
    }
}
