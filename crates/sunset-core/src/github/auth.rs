//! 认证模块：Token 与 Device Flow（对应原 Kotlin 登录链路）。

use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};

/// 认证凭证。
///
/// 只承载内存中的 token；持久化由上层（Android Keystore /
/// SharedPreferences 等价物）负责，本库不落盘。
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Credentials {
    pub token: String,
    /// token 创建时间，用于过期提示。
    pub created_at: DateTime<Utc>,
}

impl Credentials {
    pub fn new(token: impl Into<String>) -> Self {
        Self {
            token: token.into(),
            created_at: Utc::now(),
        }
    }
}

/// GitHub OAuth 客户端信息（来自构建期注入）。
#[derive(Debug, Clone)]
pub struct OAuthClientInfo {
    pub client_id: String,
}

/// Device Flow 状态机。
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum DeviceFlowState {
    /// 等待用户输入设备码。
    WaitingForUser,
    /// 已授权，token 就绪。
    Authorized,
    /// 用户拒绝或超时。
    Denied,
    /// 授权轮询失败。
    Failed,
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn credentials_carries_token() {
        let c = Credentials::new("ghp_test_token");
        assert_eq!(c.token, "ghp_test_token");
        // created_at 应接近当前时间
        let age = Utc::now() - c.created_at;
        assert!(age.num_seconds() < 10);
    }
}