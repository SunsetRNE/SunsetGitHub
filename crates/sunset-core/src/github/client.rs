//! GitHub REST API 客户端（reqwest 实现）。
//!
//! 支持两种认证方式：Personal Access Token 与 Device Flow。

use reqwest::Method;

use crate::error::{Error, Result};
use crate::github::auth::{Credentials, OAuthClientInfo};
use crate::github::models::{AuthenticatedUser, DeviceCodeResponse};

/// GitHub REST API 基础地址。
pub const GITHUB_API_BASE: &str = "https://api.github.com";
/// GitHub 设备授权端点。
pub const GITHUB_LOGIN_BASE: &str = "https://github.com/login/device";

/// GitHub API 客户端。
///
/// 使用 rustls TLS，无 OpenSSL 依赖，便于 Android 交叉编译。
#[derive(Debug, Clone)]
pub struct GitHubClient {
    http: reqwest::Client,
    /// 可选 token（Bearer 认证）。
    token: Option<String>,
    /// OAuth 客户端信息（Device Flow 需要）。
    oauth: Option<OAuthClientInfo>,
}

impl GitHubClient {
    /// 创建匿名客户端。
    pub fn anonymous() -> Result<Self> {
        Ok(Self {
            http: Self::build_http()?,
            token: None,
            oauth: None,
        })
    }

    /// 创建带 Token 的客户端。
    pub fn with_token(credentials: &Credentials) -> Result<Self> {
        let mut client = Self::anonymous()?;
        client.token = Some(credentials.token.clone());
        Ok(client)
    }

    /// 设置 OAuth 客户端信息（Device Flow）。
    pub fn with_oauth(mut self, oauth: OAuthClientInfo) -> Self {
        self.oauth = Some(oauth);
        self
    }

    fn build_http() -> Result<reqwest::Client> {
        reqwest::Client::builder()
            .user_agent("SunsetGitHub-Rust/0.1")
            .build()
            .map_err(Error::from)
    }

    /// 访问底层 HTTP 客户端（供同 crate 高级请求使用）。
    pub(crate) fn http(&self) -> &reqwest::Client {
        &self.http
    }

    /// 访问当前 token（供同 crate 高级请求使用）。
    pub(crate) fn token(&self) -> Option<&str> {
        self.token.as_deref()
    }

    /// 获取当前认证用户（GET /user）。
    pub async fn current_user(&self) -> Result<AuthenticatedUser> {
        self.get_json("/user").await
    }

    /// 发起 Device Flow：获取设备码。
    pub async fn request_device_code(&self) -> Result<DeviceCodeResponse> {
        let client_id = self
            .oauth
            .as_ref()
            .map(|o| o.client_id.clone())
            .ok_or_else(|| Error::InvalidData("oauth client info missing".into()))?;

        let resp = self
            .http
            .post(format!("{GITHUB_LOGIN_BASE}/code"))
            .json(&serde_json::json!({ "client_id": client_id, "scope": "repo user" }))
            .send()
            .await
            .map_err(Error::from)?;

        let resp = Self::check_status(resp).await?;
        resp.json().await.map_err(Error::from)
    }

    /// 通用 GET 请求并反序列化为 JSON。
    pub(crate) async fn get_json<T: serde::de::DeserializeOwned>(
        &self,
        path: &str,
    ) -> Result<T> {
        let mut req = self.http.get(format!("{GITHUB_API_BASE}{path}"));
        if let Some(token) = &self.token {
            req = req.bearer_auth(token);
        }
        let resp = req.send().await.map_err(Error::from)?;
        let resp = Self::check_status(resp).await?;
        resp.json().await.map_err(Error::from)
    }

    /// 获取 GitHub 原始格式内容（如 README 的 Markdown 原文）。
    ///
    /// 优先请求 `application/vnd.github.raw`；若服务端返回 JSON
    /// （base64 content），则自动解码回退。
    pub(crate) async fn get_raw(&self, path: &str) -> Result<String> {
        let mut req = self
            .http
            .get(format!("{GITHUB_API_BASE}{path}"))
            .header(reqwest::header::ACCEPT, "application/vnd.github.raw");
        if let Some(token) = &self.token {
            req = req.bearer_auth(token);
        }
        let resp = req.send().await.map_err(Error::from)?;
        let resp = Self::check_status(resp).await?;
        let text = resp.text().await.map_err(Error::from)?;

        // 若返回 JSON 且含 base64 content 字段，解码回退
        if let Ok(value) = serde_json::from_str::<serde_json::Value>(&text) {
            if let Some(content) = value.get("content").and_then(|c| c.as_str()) {
                use base64::Engine as _;
                let bytes = base64::engine::general_purpose::STANDARD
                    .decode(content)
                    .map_err(|e| Error::InvalidData(e.to_string()))?;
                return Ok(String::from_utf8_lossy(&bytes).into_owned());
            }
        }
        Ok(text)
    }

    /// 通用 JSON 请求（POST/PUT/PATCH/DELETE），带可选 JSON body。
    pub(crate) async fn request_json<T: serde::de::DeserializeOwned>(
        &self,
        method: Method,
        path: &str,
        body: Option<&serde_json::Value>,
    ) -> Result<T> {
        let mut req = self
            .http
            .request(method, format!("{GITHUB_API_BASE}{path}"));
        if let Some(token) = &self.token {
            req = req.bearer_auth(token);
        }
        if let Some(body) = body {
            req = req.json(body);
        }
        let resp = req.send().await.map_err(Error::from)?;
        let resp = Self::check_status(resp).await?;
        // 204 等无内容响应直接返回空 JSON
        if resp.status() == reqwest::StatusCode::NO_CONTENT {
            return serde_json::from_value(serde_json::Value::Null)
                .map_err(Error::Json);
        }
        resp.json().await.map_err(Error::from)
    }

    /// 无返回体请求（POST/PUT/PATCH/DELETE），成功返回 `()`。
    pub(crate) async fn request_empty(
        &self,
        method: Method,
        path: &str,
        body: Option<&serde_json::Value>,
    ) -> Result<()> {
        let mut req = self
            .http
            .request(method, format!("{GITHUB_API_BASE}{path}"));
        if let Some(token) = &self.token {
            req = req.bearer_auth(token);
        }
        if let Some(body) = body {
            req = req.json(body);
        }
        let resp = req.send().await.map_err(Error::from)?;
        Self::check_status(resp).await?;
        Ok(())
    }

    /// 获取二进制内容（如 Actions 日志、Release 资产）。
    pub(crate) async fn get_bytes(&self, path: &str) -> Result<Vec<u8>> {
        let mut req = self.http.get(format!("{GITHUB_API_BASE}{path}"));
        if let Some(token) = &self.token {
            req = req.bearer_auth(token);
        }
        let resp = req.send().await.map_err(Error::from)?;
        let resp = Self::check_status(resp).await?;
        Ok(resp.bytes().await.map_err(Error::from)?.to_vec())
    }

    pub(crate) async fn check_status(resp: reqwest::Response) -> Result<reqwest::Response> {
        let status = resp.status();
        if status.is_success() {
            return Ok(resp);
        }
        if status == reqwest::StatusCode::UNAUTHORIZED {
            return Err(Error::Unauthorized);
        }
        let message = resp
            .text()
            .await
            .unwrap_or_else(|_| "unknown error".into());
        Err(Error::Http {
            status: status.as_u16(),
            message,
        })
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn client_builds_with_token() {
        let creds = Credentials::new("ghp_fake");
        let client = GitHubClient::with_token(&creds).expect("client build");
        assert_eq!(client.token.as_deref(), Some("ghp_fake"));
    }

    #[test]
    fn anonymous_has_no_token() {
        let client = GitHubClient::anonymous().expect("client build");
        assert!(client.token.is_none());
    }
}