//! Pull Requests API：列表/创建。

use reqwest::Method;

use crate::error::Result;
use crate::github::client::GitHubClient;
use crate::github::models::PullRequest;

/// PR 状态筛选。
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum PullRequestState {
    Open,
    Closed,
    All,
}

impl PullRequestState {
    fn as_str(self) -> &'static str {
        match self {
            PullRequestState::Open => "open",
            PullRequestState::Closed => "closed",
            PullRequestState::All => "all",
        }
    }
}

/// 创建 PR 请求。
#[derive(Debug, Clone, serde::Serialize)]
pub struct CreatePullRequestRequest {
    pub title: String,
    pub head: String,
    pub base: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub body: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub draft: Option<bool>,
}

impl GitHubClient {
    /// 列出仓库 PR。
    pub async fn list_pull_requests(
        &self,
        owner: &str,
        repo: &str,
        state: PullRequestState,
        per_page: u32,
    ) -> Result<Vec<PullRequest>> {
        self.get_json(&format!(
            "/repos/{owner}/{repo}/pulls?state={}&per_page={}",
            state.as_str(),
            per_page
        ))
        .await
    }

    /// 获取 PR 详情。
    pub async fn get_pull_request(
        &self,
        owner: &str,
        repo: &str,
        number: u64,
    ) -> Result<PullRequest> {
        self.get_json(&format!("/repos/{owner}/{repo}/pulls/{number}"))
            .await
    }

    /// 创建 PR。
    pub async fn create_pull_request(
        &self,
        owner: &str,
        repo: &str,
        request: &CreatePullRequestRequest,
    ) -> Result<PullRequest> {
        self.request_json(
            Method::POST,
            &format!("/repos/{owner}/{repo}/pulls"),
            Some(&serde_json::to_value(request)?),
        )
        .await
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn pr_state_maps() {
        assert_eq!(PullRequestState::Open.as_str(), "open");
        assert_eq!(PullRequestState::Closed.as_str(), "closed");
        assert_eq!(PullRequestState::All.as_str(), "all");
    }

    #[test]
    fn create_pr_request_serializes() {
        let req = CreatePullRequestRequest {
            title: "feat: add x".into(),
            head: "feature-branch".into(),
            base: "main".into(),
            body: None,
            draft: Some(true),
        };
        let value = serde_json::to_value(&req).unwrap();
        assert_eq!(value["head"], "feature-branch");
        assert_eq!(value["base"], "main");
        assert_eq!(value["draft"], true);
        assert!(value.get("body").is_none());
    }
}