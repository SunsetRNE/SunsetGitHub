//! GitHub Actions API：workflows / runs / artifacts / 日志下载。

use reqwest::Method;

use crate::error::Result;
use crate::github::client::GitHubClient;
use crate::github::models::{ActionArtifact, ActionRun, ActionWorkflow};

/// 触发 workflow dispatch（POST /actions/workflows/{id}/dispatches）。
#[derive(Debug, Clone, serde::Serialize)]
pub struct WorkflowDispatchRequest {
    pub r#ref: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub inputs: Option<serde_json::Map<String, serde_json::Value>>,
}

impl GitHubClient {
    /// 列出仓库 workflows。
    pub async fn list_workflows(&self, owner: &str, repo: &str) -> Result<Vec<ActionWorkflow>> {
        #[derive(serde::Deserialize)]
        struct WorkflowListResponse {
            #[serde(default)]
            workflows: Vec<ActionWorkflow>,
        }
        let resp: WorkflowListResponse = self
            .get_json(&format!("/repos/{owner}/{repo}/actions/workflows"))
            .await?;
        Ok(resp.workflows)
    }

    /// 触发 workflow_dispatch。
    pub async fn dispatch_workflow(
        &self,
        owner: &str,
        repo: &str,
        workflow_id: u64,
        request: &WorkflowDispatchRequest,
    ) -> Result<()> {
        self.request_empty(
            Method::POST,
            &format!("/repos/{owner}/{repo}/actions/workflows/{workflow_id}/dispatches"),
            Some(&serde_json::to_value(request)?),
        )
        .await
    }

    /// 列出 Actions 运行。
    pub async fn list_action_runs(
        &self,
        owner: &str,
        repo: &str,
        per_page: u32,
    ) -> Result<Vec<ActionRun>> {
        #[derive(serde::Deserialize)]
        struct RunListResponse {
            #[serde(default)]
            workflow_runs: Vec<ActionRun>,
        }
        let resp: RunListResponse = self
            .get_json(&format!(
                "/repos/{owner}/{repo}/actions/runs?per_page={per_page}"
            ))
            .await?;
        Ok(resp.workflow_runs)
    }

    /// 获取单次运行详情。
    pub async fn get_action_run(&self, owner: &str, repo: &str, run_id: u64) -> Result<ActionRun> {
        self.get_json(&format!("/repos/{owner}/{repo}/actions/runs/{run_id}"))
            .await
    }

    /// 列出运行的 artifacts。
    pub async fn list_action_run_artifacts(
        &self,
        owner: &str,
        repo: &str,
        run_id: u64,
    ) -> Result<Vec<ActionArtifact>> {
        #[derive(serde::Deserialize)]
        struct ArtifactListResponse {
            #[serde(default)]
            artifacts: Vec<ActionArtifact>,
        }
        let resp: ArtifactListResponse = self
            .get_json(&format!(
                "/repos/{owner}/{repo}/actions/runs/{run_id}/artifacts"
            ))
            .await?;
        Ok(resp.artifacts)
    }

    /// 下载运行日志（zip 字节流）。
    pub async fn download_action_run_logs(
        &self,
        owner: &str,
        repo: &str,
        run_id: u64,
    ) -> Result<Vec<u8>> {
        self.get_bytes(&format!("/repos/{owner}/{repo}/actions/runs/{run_id}/logs"))
            .await
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn dispatch_request_serializes() {
        let mut inputs = serde_json::Map::new();
        inputs.insert("env".into(), serde_json::Value::String("prod".into()));
        let req = WorkflowDispatchRequest {
            r#ref: "main".into(),
            inputs: Some(inputs),
        };
        let value = serde_json::to_value(&req).unwrap();
        assert_eq!(value["ref"], "main");
        assert_eq!(value["inputs"]["env"], "prod");
    }

    #[test]
    fn dispatch_without_inputs() {
        let req = WorkflowDispatchRequest {
            r#ref: "main".into(),
            inputs: None,
        };
        let value = serde_json::to_value(&req).unwrap();
        assert!(value.get("inputs").is_none());
    }
}
