package com.Sunset.REN.GitHub.domain.repo

/**
 * Fork 仓库与上游仓库默认分支之间的同步关系。
 *
 * [aheadBy] 表示当前 fork 比上游多出的提交数，[behindBy] 表示当前 fork 落后上游的提交数。
 */
data class RepositoryForkSyncStatus(
    val upstreamFullName: String,
    val upstreamBranch: String,
    val forkBranch: String,
    val aheadBy: Int,
    val behindBy: Int
)
