package com.Sunset.REN.GitHub.ui.shell

import com.Sunset.REN.GitHub.ui.repo.RepositorySection

/**
 * 壳页路由（步骤 6：删旧壳后的运行时页面枚举，全量）。
 *
 * 每个值对应一个已迁移的页面对象；主 Tab / 登录页为无参路由，
 * 仓库系页面携带 owner/repo 等参数，认证流与次级页为独立路由。
 */
sealed interface ShellPage {
    // ---- 壳骨架 ----
    /** 登录首页（认证入口）。 */
    data object Login : ShellPage

    /** 主页（静态聚合页）。 */
    data object Home : ShellPage

    /** 仓库列表。 */
    data object Dashboard : ShellPage

    /** 通知。 */
    data object Notifications : ShellPage

    /** 我的。 */
    data object Profile : ShellPage

    /** 设置（次级页：showBack + Hidden）。 */
    data object Settings : ShellPage

    // ---- 认证流 ----
    /** 设备码登录引导页。 */
    data object DeviceFlowIntro : ShellPage

    /** 设备码展示/等待授权页。 */
    data object DeviceFlowCode : ShellPage

    /** Token 登录方式选择页。 */
    data object TokenLoginChoice : ShellPage

    /** Token 使用引导页。 */
    data object TokenGuide : ShellPage

    /** Token 权限审查页。 */
    data object TokenPermissionReview : ShellPage

    // ---- 仓库详情流 ----
    /** 仓库详情（Code 分区，RepositorySections 导航）。 */
    data class RepositoryDetail(
        val owner: String,
        val repo: String,
        val fullName: String,
    ) : ShellPage

    /** 仓库 HTML 摘要分区（六分区共用 stub 页）。 */
    data class RepositoryStub(
        val owner: String,
        val repo: String,
        val section: RepositorySection,
    ) : ShellPage

    /** Issues 分区。 */
    data class Issues(val owner: String, val repo: String) : ShellPage

    /** Pull Requests 分区。 */
    data class PullRequests(val owner: String, val repo: String) : ShellPage

    /** Issue 详情。 */
    data class IssueDetail(val owner: String, val repo: String, val number: Int) : ShellPage

    /** Releases 分区。 */
    data class Releases(val owner: String, val repo: String) : ShellPage

    /** Actions 分区。 */
    data class Actions(val owner: String, val repo: String) : ShellPage

    /** Action 运行详情。 */
    data class ActionRunDetail(
        val owner: String,
        val repo: String,
        val runId: Long,
    ) : ShellPage

    /** Action 运行开发信息。 */
    data class ActionRunDevInfo(
        val owner: String,
        val repo: String,
        val runId: Long,
    ) : ShellPage

    // ---- 仓库写入流 ----
    /** 新建仓库。 */
    data object RepoCreate : ShellPage

    /** Fork 仓库。 */
    data class RepoFork(
        val owner: String,
        val repo: String,
        val fullName: String,
    ) : ShellPage

    /** 文件上传。 */
    data class FileUpload(val owner: String, val repo: String) : ShellPage

    // ---- 仓库设置/管理（组 B） ----
    /** 分支设置。 */
    data class BranchSettings(val owner: String, val repo: String) : ShellPage

    /** 协作者设置。 */
    data class Collaborators(val owner: String, val repo: String) : ShellPage

    /** 危险区。 */
    data class DangerZone(val owner: String, val repo: String) : ShellPage

    /** 规则集（只读）。 */
    data class Rulesets(val owner: String, val repo: String) : ShellPage

    /** 部署密钥。 */
    data class DeployKeys(val owner: String, val repo: String) : ShellPage

    /** Webhooks。 */
    data class Webhooks(val owner: String, val repo: String) : ShellPage

    /** Actions 设置。 */
    data class ActionsSettings(val owner: String, val repo: String) : ShellPage

    /** 安全告警详情。 */
    data class SecurityAlertDetail(
        val owner: String,
        val repo: String,
        val alertNumber: Int,
    ) : ShellPage

    // ---- 搜索/文件/账号 ----
    /** 搜索。 */
    data class SearchPage(val initialQuery: String = "") : ShellPage

    /** 本地文件管理器。 */
    data object FileManager : ShellPage

    /** 账户管理。 */
    data object Account : ShellPage

    /** 通知详情（纯参数页）。 */
    data class NotificationDetail(val notificationId: String) : ShellPage

    // ---- 终端/工作区 ----
    /** 工作区终端。 */
    data object Terminal : ShellPage

    /** 工作区同步方向入口。 */
    data object WorkspaceSync : ShellPage

    /** 工作区拉取。 */
    data object WorkspacePull : ShellPage

    /** 工作区推送。 */
    data object WorkspacePush : ShellPage

    /** 应用日志。 */
    data object AppLog : ShellPage
}