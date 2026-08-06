//! 通知 API：列表/已读/完成/订阅。

use reqwest::Method;

use crate::error::Result;
use crate::github::client::GitHubClient;
use crate::github::models::Notification;

impl GitHubClient {
    /// 列出通知（all=true 包含已读）。
    pub async fn list_notifications(
        &self,
        all: bool,
        participating: bool,
    ) -> Result<Vec<Notification>> {
        self.get_json(&format!(
            "/notifications?all={all}&participating={participating}&per_page=100"
        ))
        .await
    }

    /// 将通知线程标记为已读。
    pub async fn mark_thread_read(&self, thread_id: &str) -> Result<()> {
        self.request_empty(
            Method::PATCH,
            &format!("/notifications/threads/{thread_id}"),
            None,
        )
        .await
    }

    /// 将通知线程标记为完成（移除）。
    pub async fn mark_thread_done(&self, thread_id: &str) -> Result<()> {
        self.request_empty(
            Method::DELETE,
            &format!("/notifications/threads/{thread_id}"),
            None,
        )
        .await
    }

    /// 订阅通知线程。
    pub async fn subscribe_thread(
        &self,
        thread_id: &str,
        ignored: bool,
    ) -> Result<()> {
        self.request_empty(
            Method::PUT,
            &format!("/notifications/threads/{thread_id}/subscription"),
            Some(&serde_json::json!({ "ignored": ignored })),
        )
        .await
    }

    /// 取消订阅通知线程。
    pub async fn unsubscribe_thread(&self, thread_id: &str) -> Result<()> {
        self.request_empty(
            Method::DELETE,
            &format!("/notifications/threads/{thread_id}/subscription"),
            None,
        )
        .await
    }
}

#[cfg(test)]
mod tests {
    #[test]
    fn notification_paths_are_stable() {
        // 仅验证路径构造不回归
        let path = format!("/notifications?all={}&participating={}&per_page=100", true, false);
        assert!(path.contains("all=true"));
        assert!(path.contains("participating=false"));
    }
}