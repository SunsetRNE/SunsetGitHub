//! 统一错误类型。

use thiserror::Error;

/// 核心库统一错误类型。
#[derive(Debug, Error)]
pub enum Error {
    #[error("network error: {0}")]
    Network(#[from] reqwest::Error),

    #[error("http status {status}: {message}")]
    Http { status: u16, message: String },

    #[error("authentication required")]
    Unauthorized,

    #[error("io error: {0}")]
    Io(#[from] std::io::Error),

    #[error("zip error: {0}")]
    Zip(#[from] zip::result::ZipError),

    #[error("json error: {0}")]
    Json(#[from] serde_json::Error),

    #[error("invalid data: {0}")]
    InvalidData(String),

    #[error("unsupported: {0}")]
    Unsupported(String),
}

/// 核心库统一结果类型。
pub type Result<T> = std::result::Result<T, Error>;