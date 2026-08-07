//! 仓库相关 API：列表、详情、内容浏览、Issues。

use crate::error::Result;
use crate::github::client::GitHubClient;
use crate::github::models::{Issue, RepoContentEntry, Repository};

/// 仓库排序方式（对应原 Kotlin Dashboard 排序）。
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum RepoSort {
    Updated,
    Stars,
    Forks,
    Name,
}

impl RepoSort {
    fn as_str(self) -> &'static str {
        match self {
            RepoSort::Updated => "updated",
            RepoSort::Stars => "stars",
            RepoSort::Forks => "forks",
            RepoSort::Name => "full_name",
        }
    }
}

/// 仓库方向。
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum RepoDirection {
    Asc,
    Desc,
}

impl GitHubClient {
    /// 获取当前用户仓库列表。
    pub async fn list_my_repositories(
        &self,
        sort: RepoSort,
        direction: RepoDirection,
        per_page: u32,
    ) -> Result<Vec<Repository>> {
        let direction = match direction {
            RepoDirection::Asc => "asc",
            RepoDirection::Desc => "desc",
        };
        self.get_json(&format!(
            "/user/repos?sort={}&direction={}&per_page={}",
            sort.as_str(),
            direction,
            per_page
        ))
        .await
    }

    /// 搜索仓库。
    pub async fn search_repositories(&self, query: &str, per_page: u32) -> Result<Vec<Repository>> {
        let encoded = urlencode(query);
        #[derive(serde::Deserialize)]
        struct SearchResponse {
            items: Vec<Repository>,
        }
        let resp: SearchResponse = self
            .get_json(&format!(
                "/search/repositories?q={}&per_page={}",
                encoded, per_page
            ))
            .await?;
        Ok(resp.items)
    }

    /// 获取仓库详情。
    pub async fn repository(&self, owner: &str, name: &str) -> Result<Repository> {
        self.get_json(&format!("/repos/{owner}/{name}")).await
    }

    /// 获取仓库内容列表（目录浏览）。
    pub async fn repository_contents(
        &self,
        owner: &str,
        name: &str,
        path: &str,
        branch: Option<&str>,
    ) -> Result<Vec<RepoContentEntry>> {
        let path = path.trim_matches('/');
        let mut url = format!("/repos/{owner}/{name}/contents/{path}");
        if let Some(branch) = branch {
            url.push_str(&format!("?ref={branch}"));
        }
        self.get_json(&url).await
    }

    /// 获取仓库 Issues 列表。
    pub async fn list_issues(
        &self,
        owner: &str,
        name: &str,
        state: &str,
        per_page: u32,
    ) -> Result<Vec<Issue>> {
        self.get_json(&format!(
            "/repos/{owner}/{name}/issues?state={state}&per_page={per_page}"
        ))
        .await
    }

    /// 获取仓库 README（原始 Markdown 文本）。
    pub async fn readme_text(&self, owner: &str, name: &str) -> Result<String> {
        self.get_raw(&format!("/repos/{owner}/{name}/readme")).await
    }
}

/// URL 编码（避免引入额外依赖的极简实现）。
fn urlencode(input: &str) -> String {
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
    fn repo_sort_maps_to_api_value() {
        assert_eq!(RepoSort::Updated.as_str(), "updated");
        assert_eq!(RepoSort::Name.as_str(), "full_name");
    }

    #[test]
    fn urlencode_handles_chinese_and_spaces() {
        assert_eq!(urlencode("a b"), "a%20b");
        assert_eq!(urlencode("你好"), "%E4%BD%A0%E5%A5%BD");
        assert_eq!(urlencode("rust"), "rust");
    }
}
