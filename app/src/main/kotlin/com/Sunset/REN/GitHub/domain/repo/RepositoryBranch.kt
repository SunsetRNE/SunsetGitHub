package com.Sunset.REN.GitHub.domain.repo

/** 仓库分支摘要，用于创建 Release 时选择目标分支。 */
data class RepositoryBranch(
    val name: String,
    val isDefault: Boolean = false,
    val isProtected: Boolean = false
)
