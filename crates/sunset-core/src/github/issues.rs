//! Issues API：列表/详情/创建/状态/标签/评论 CRUD。

use reqwest::Method;

use crate::error::Result;
use crate::github::client::GitHubClient;
use crate::github::models::{Issue, IssueComment};

/// 创建 Issue 请求。
#[derive(Debug, Clone, serde::Serialize)]
pub struct CreateIssueRequest {
    pub title: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub body: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub labels: Option<Vec<String>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub assignees: Option<Vec<String>>,
}

/// Issue 状态筛选。
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum IssueState {
    Open,
    Closed,
    All,
}

impl IssueState {
    fn as_str(self) -> &'static str {
        match self {
            IssueState::Open => "open",
            IssueState::Closed => "closed",
            IssueState::All => "all",
        }
    }
}

impl GitHubClient {
    /// 获取 Issue 详情。
    pub async fn get_issue(&self, owner: &str, repo: &str, number: u64) -> Result<Issue> {
        self.get_json(&format!("/repos/{owner}/{repo}/issues/{number}"))
            .await
    }

    /// 创建 Issue。
    pub async fn create_issue(
        &self,
        owner: &str,
        repo: &str,
        request: &CreateIssueRequest,
    ) -> Result<Issue> {
        self.request_json(
            Method::POST,
            &format!("/repos/{owner}/{repo}/issues"),
            Some(&serde_json::to_value(request)?),
        )
        .await
    }

    /// 更新 Issue 状态（open/closed）。
    pub async fn update_issue_state(
        &self,
        owner: &str,
        repo: &str,
        number: u64,
        state: IssueState,
    ) -> Result<Issue> {
        self.request_json(
            Method::PATCH,
            &format!("/repos/{owner}/{repo}/issues/{number}"),
            Some(&serde_json::json!({ "state": state.as_str() })),
        )
        .await
    }

    /// 设置 Issue 标签（整体替换）。
    pub async fn set_issue_labels(
        &self,
        owner: &str,
        repo: &str,
        number: u64,
        labels: &[String],
    ) -> Result<Vec<crate::github::models::Label>> {
        self.request_json(
            Method::PUT,
            &format!("/repos/{owner}/{repo}/issues/{number}/labels"),
            Some(&serde_json::json!({ "labels": labels })),
        )
        .await
    }

    /// 列出 Issue 评论。
    pub async fn list_issue_comments(
        &self,
        owner: &str,
        repo: &str,
        number: u64,
    ) -> Result<Vec<IssueComment>> {
        self.get_json(&format!(
            "/repos/{owner}/{repo}/issues/{number}/comments?per_page=100"
        ))
        .await
    }

    /// 创建 Issue 评论。
    pub async fn create_issue_comment(
        &self,
        owner: &str,
        repo: &str,
        number: u64,
        body: &str,
    ) -> Result<IssueComment> {
        self.request_json(
            Method::POST,
            &format!("/repos/{owner}/{repo}/issues/{number}/comments"),
            Some(&serde_json::json!({ "body": body })),
        )
        .await
    }

    /// 更新 Issue 评论。
    pub async fn update_issue_comment(
        &self,
        owner: &str,
        repo: &str,
        comment_id: u64,
        body: &str,
    ) -> Result<IssueComment> {
        self.request_json(
            Method::PATCH,
            &format!("/repos/{owner}/{repo}/issues/comments/{comment_id}"),
            Some(&serde_json::json!({ "body": body })),
        )
        .await
    }

    /// 删除 Issue 评论。
    pub async fn delete_issue_comment(
        &self,
        owner: &str,
        repo: &str,
        comment_id: u64,
    ) -> Result<()> {
        self.request_empty(
            Method::DELETE,
            &format!("/repos/{owner}/{repo}/issues/comments/{comment_id}"),
            None,
        )
        .await
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn issue_state_maps() {
        assert_eq!(IssueState::Open.as_str(), "open");
        assert_eq!(IssueState::Closed.as_str(), "closed");
        assert_eq!(IssueState::All.as_str(), "all");
    }

    #[test]
    fn create_request_serializes_omitting_none() {
        let req = CreateIssueRequest {
            title: "Bug".into(),
            body: None,
            labels: Some(vec!["bug".into()]),
            assignees: None,
        };
        let value = serde_json::to_value(&req).unwrap();
        assert_eq!(value["title"], "Bug");
        assert!(value.get("body").is_none());
        assert_eq!(value["labels"][0], "bug");
        assert!(value.get("assignees").is_none());
    }
}
