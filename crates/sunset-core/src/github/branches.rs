//! 仓库分支 API。

use crate::error::Result;
use crate::github::client::GitHubClient;
use crate::github::models::Branch;

impl GitHubClient {
    /// 列出仓库分支。
    pub async fn list_branches(
        &self,
        owner: &str,
        repo: &str,
        per_page: u32,
    ) -> Result<Vec<Branch>> {
        self.get_json(&format!(
            "/repos/{owner}/{repo}/branches?per_page={per_page}"
        ))
        .await
    }

    /// 获取单个分支。
    pub async fn get_branch(&self, owner: &str, repo: &str, branch: &str) -> Result<Branch> {
        let encoded = crate::github::releases::urlencode(branch);
        self.get_json(&format!("/repos/{owner}/{repo}/branches/{encoded}"))
            .await
    }
}

#[cfg(test)]
mod tests {
    #[test]
    fn branch_list_path_format() {
        let path = format!("/repos/o/r/branches?per_page={}", 100);
        assert_eq!(path, "/repos/o/r/branches?per_page=100");
    }
}
