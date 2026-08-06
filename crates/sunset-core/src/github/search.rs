//! 搜索 API：仓库/用户/Issue/代码。

use crate::error::Result;
use crate::github::client::GitHubClient;
use crate::github::models::{Issue, Repository, User};

/// 仓库搜索结果页。
#[derive(Debug, Clone)]
pub struct RepositorySearchPage {
    pub total_count: u64,
    pub items: Vec<Repository>,
}

/// 用户搜索结果页。
#[derive(Debug, Clone)]
pub struct UserSearchPage {
    pub total_count: u64,
    pub items: Vec<User>,
}

/// Issue 搜索结果页。
#[derive(Debug, Clone)]
pub struct IssueSearchPage {
    pub total_count: u64,
    pub items: Vec<Issue>,
}

/// 代码搜索结果条目。
#[derive(Debug, Clone, serde::Deserialize)]
pub struct CodeSearchItem {
    pub name: String,
    #[serde(default)]
    pub path: String,
    #[serde(default)]
    pub sha: String,
    #[serde(default)]
    pub html_url: String,
}

/// 代码搜索结果页。
#[derive(Debug, Clone)]
pub struct CodeSearchPage {
    pub total_count: u64,
    pub items: Vec<CodeSearchItem>,
}

#[derive(serde::Deserialize)]
#[serde(bound(deserialize = "T: serde::de::DeserializeOwned"))]
struct SearchResponse<T> {
    #[serde(default)]
    total_count: u64,
    #[serde(default)]
    items: Vec<T>,
}

impl GitHubClient {
    /// 搜索仓库。
    pub async fn search_repositories_page(
        &self,
        query: &str,
        page: u32,
        per_page: u32,
    ) -> Result<RepositorySearchPage> {
        let resp: SearchResponse<Repository> = self
            .get_json(&format!(
                "/search/repositories?q={}&page={}&per_page={}",
                crate::github::releases::urlencode(query),
                page,
                per_page
            ))
            .await?;
        Ok(RepositorySearchPage {
            total_count: resp.total_count,
            items: resp.items,
        })
    }

    /// 搜索用户。
    pub async fn search_users(
        &self,
        query: &str,
        per_page: u32,
    ) -> Result<UserSearchPage> {
        let resp: SearchResponse<User> = self
            .get_json(&format!(
                "/search/users?q={}&per_page={}",
                crate::github::releases::urlencode(query),
                per_page
            ))
            .await?;
        Ok(UserSearchPage {
            total_count: resp.total_count,
            items: resp.items,
        })
    }

    /// 搜索 Issue / PR。
    pub async fn search_issues(
        &self,
        query: &str,
        per_page: u32,
    ) -> Result<IssueSearchPage> {
        let resp: SearchResponse<Issue> = self
            .get_json(&format!(
                "/search/issues?q={}&per_page={}",
                crate::github::releases::urlencode(query),
                per_page
            ))
            .await?;
        Ok(IssueSearchPage {
            total_count: resp.total_count,
            items: resp.items,
        })
    }

    /// 搜索代码。
    pub async fn search_code(
        &self,
        query: &str,
        per_page: u32,
    ) -> Result<CodeSearchPage> {
        let resp: SearchResponse<CodeSearchItem> = self
            .get_json(&format!(
                "/search/code?q={}&per_page={}",
                crate::github::releases::urlencode(query),
                per_page
            ))
            .await?;
        Ok(CodeSearchPage {
            total_count: resp.total_count,
            items: resp.items,
        })
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn search_response_deserializes() {
        let json = r#"{"total_count": 1, "items": [{"name": "a", "path": "p", "sha": "s", "html_url": "u"}]}"#;
        let resp: SearchResponse<CodeSearchItem> = serde_json::from_str(json).unwrap();
        assert_eq!(resp.total_count, 1);
        assert_eq!(resp.items[0].name, "a");
    }
}