package com.Sunset.REN.GitHub.ui.shell
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import com.Sunset.REN.GitHub.data.auth.AuthSessionRepository
import com.Sunset.REN.GitHub.domain.repo.RepositorySecurityAlert
import com.Sunset.REN.GitHub.ui.account.AccountViewModel
import com.Sunset.REN.GitHub.ui.auth.AuthUiState
import com.Sunset.REN.GitHub.ui.auth.LoginHomeUiState
import com.Sunset.REN.GitHub.ui.auth.LoginHomeViewModel
import com.Sunset.REN.GitHub.ui.auth.TokenPermissionReviewUiState
import com.Sunset.REN.GitHub.ui.auth.TokenPermissionReviewViewModel
import com.Sunset.REN.GitHub.ui.auth.device.DeviceFlowUiState
import com.Sunset.REN.GitHub.ui.auth.device.DeviceFlowViewModel
import com.Sunset.REN.GitHub.ui.dashboard.DashboardViewModel
import com.Sunset.REN.GitHub.ui.filemanager.LocalFileManagerUiState
import com.Sunset.REN.GitHub.ui.filemanager.LocalFileManagerViewModel
import com.Sunset.REN.GitHub.ui.notifications.NotificationsUiState
import com.Sunset.REN.GitHub.ui.notifications.NotificationsViewModel
import com.Sunset.REN.GitHub.ui.pages.AccountPage
import com.Sunset.REN.GitHub.ui.pages.ActionsPage
import com.Sunset.REN.GitHub.ui.pages.AppLogPage
import com.Sunset.REN.GitHub.ui.pages.RustCorePage
import com.Sunset.REN.GitHub.ui.pages.DashboardPage
import com.Sunset.REN.GitHub.ui.pages.DeviceFlowCodePage
import com.Sunset.REN.GitHub.ui.pages.DeviceFlowIntroPage
import com.Sunset.REN.GitHub.ui.pages.FileManagerPage
import com.Sunset.REN.GitHub.ui.pages.HomePage
import com.Sunset.REN.GitHub.ui.pages.IssueDetailPage
import com.Sunset.REN.GitHub.ui.pages.IssuesPage
import com.Sunset.REN.GitHub.ui.pages.LocalFilePreviewPage
import com.Sunset.REN.GitHub.ui.pages.LoginHomePage
import com.Sunset.REN.GitHub.ui.pages.NotificationDetailPage
import com.Sunset.REN.GitHub.ui.pages.NotificationsPage
import com.Sunset.REN.GitHub.ui.pages.ProfilePage
import com.Sunset.REN.GitHub.ui.pages.PullRequestsPage
import com.Sunset.REN.GitHub.ui.pages.ReleasesPage
import com.Sunset.REN.GitHub.ui.pages.RepositoryActionRunDeveloperInfoPage
import com.Sunset.REN.GitHub.ui.pages.RepositoryActionRunDetailPage
import com.Sunset.REN.GitHub.ui.pages.RepositoryActionsSettingsPage
import com.Sunset.REN.GitHub.ui.pages.RepositoryBranchSettingsPage
import com.Sunset.REN.GitHub.ui.pages.RepositoryCollaboratorsSettingsPage
import com.Sunset.REN.GitHub.ui.pages.RepositoryCreatePage
import com.Sunset.REN.GitHub.ui.pages.RepositoryDangerZonePage
import com.Sunset.REN.GitHub.ui.pages.RepositoryDeployKeysPage
import com.Sunset.REN.GitHub.ui.pages.RepositoryDetailPage
import com.Sunset.REN.GitHub.ui.pages.RepositoryFileUploadPage
import com.Sunset.REN.GitHub.ui.pages.RepositoryForkPage
import com.Sunset.REN.GitHub.ui.pages.RepositoryRulesetsPage
import com.Sunset.REN.GitHub.ui.pages.RepositorySectionStubPage
import com.Sunset.REN.GitHub.ui.pages.RepositorySecurityAlertDetailPage
import com.Sunset.REN.GitHub.ui.pages.RepositoryWebhooksPage
import com.Sunset.REN.GitHub.ui.pages.SearchPage
import com.Sunset.REN.GitHub.ui.pages.SettingsPage
import com.Sunset.REN.GitHub.ui.pages.TerminalPage
import com.Sunset.REN.GitHub.ui.pages.TokenGuidePage
import com.Sunset.REN.GitHub.ui.pages.TokenLoginChoicePage
import com.Sunset.REN.GitHub.ui.pages.TokenPermissionReviewPage
import com.Sunset.REN.GitHub.ui.pages.WorkspacePullPage
import com.Sunset.REN.GitHub.ui.pages.WorkspacePushPage
import com.Sunset.REN.GitHub.ui.pages.WorkspaceSyncPage
import com.Sunset.REN.GitHub.ui.profile.ProfileUiState
import com.Sunset.REN.GitHub.ui.profile.ProfileViewModel
import com.Sunset.REN.GitHub.ui.render.renderPage
import com.Sunset.REN.GitHub.ui.repo.RepositoriesUiState
import com.Sunset.REN.GitHub.ui.repo.RepositoryActionRunDetailUiState
import com.Sunset.REN.GitHub.ui.repo.RepositoryActionRunDetailViewModel
import com.Sunset.REN.GitHub.ui.repo.RepositoryActionsSettingsUiState
import com.Sunset.REN.GitHub.ui.repo.RepositoryActionsSettingsViewModel
import com.Sunset.REN.GitHub.ui.repo.RepositoryActionsUiState
import com.Sunset.REN.GitHub.ui.repo.RepositoryActionsViewModel
import com.Sunset.REN.GitHub.ui.repo.RepositoryBranchSettingsUiState
import com.Sunset.REN.GitHub.ui.repo.RepositoryBranchSettingsViewModel
import com.Sunset.REN.GitHub.ui.repo.RepositoryCollaboratorsSettingsUiState
import com.Sunset.REN.GitHub.ui.repo.RepositoryCollaboratorsSettingsViewModel
import com.Sunset.REN.GitHub.ui.repo.RepositoryCreateUiState
import com.Sunset.REN.GitHub.ui.repo.RepositoryCreateViewModel
import com.Sunset.REN.GitHub.ui.repo.RepositoryDangerZoneUiState
import com.Sunset.REN.GitHub.ui.repo.RepositoryDangerZoneViewModel
import com.Sunset.REN.GitHub.ui.repo.RepositoryDeployKeysUiState
import com.Sunset.REN.GitHub.ui.repo.RepositoryDeployKeysViewModel
import com.Sunset.REN.GitHub.ui.repo.RepositoryDetailUiState
import com.Sunset.REN.GitHub.ui.repo.RepositoryDetailViewModel
import com.Sunset.REN.GitHub.ui.repo.RepositoryFileUploadUiState
import com.Sunset.REN.GitHub.ui.repo.RepositoryFileUploadViewModel
import com.Sunset.REN.GitHub.ui.repo.RepositoryForkUiState
import com.Sunset.REN.GitHub.ui.repo.RepositoryForkViewModel
import com.Sunset.REN.GitHub.ui.repo.RepositoryIssueDetailUiState
import com.Sunset.REN.GitHub.ui.repo.RepositoryIssueDetailViewModel
import com.Sunset.REN.GitHub.ui.repo.RepositoryIssuesUiState
import com.Sunset.REN.GitHub.ui.repo.RepositoryIssuesViewModel
import com.Sunset.REN.GitHub.ui.repo.RepositoryPullRequestsUiState
import com.Sunset.REN.GitHub.ui.repo.RepositoryPullRequestsViewModel
import com.Sunset.REN.GitHub.ui.repo.RepositoryReleasesUiState
import com.Sunset.REN.GitHub.ui.repo.RepositoryReleasesViewModel
import com.Sunset.REN.GitHub.ui.repo.RepositoryRulesetsUiState
import com.Sunset.REN.GitHub.ui.repo.RepositoryRulesetsViewModel
import com.Sunset.REN.GitHub.ui.repo.RepositorySection
import com.Sunset.REN.GitHub.ui.repo.RepositorySectionNativeStubUiState
import com.Sunset.REN.GitHub.ui.repo.RepositorySectionNativeStubViewModel
import com.Sunset.REN.GitHub.ui.repo.RepositorySecurityAlertDetailUiState
import com.Sunset.REN.GitHub.ui.repo.RepositorySecurityAlertDetailViewModel
import com.Sunset.REN.GitHub.ui.repo.RepositoryWebhooksUiState
import com.Sunset.REN.GitHub.ui.repo.RepositoryWebhooksViewModel
import com.Sunset.REN.GitHub.ui.schema.IconId
import com.Sunset.REN.GitHub.ui.search.SearchType
import com.Sunset.REN.GitHub.ui.search.SearchUiState
import com.Sunset.REN.GitHub.ui.search.SearchViewModel
import com.Sunset.REN.GitHub.ui.settings.SettingsViewModel
import com.Sunset.REN.GitHub.ui.terminal.TerminalUiState
import com.Sunset.REN.GitHub.ui.terminal.TerminalViewModel
import com.Sunset.REN.GitHub.ui.workspace.WorkspacePullUiState
import com.Sunset.REN.GitHub.ui.workspace.WorkspacePullViewModel
import com.Sunset.REN.GitHub.ui.workspace.WorkspaceSyncUiState
import com.Sunset.REN.GitHub.ui.workspace.WorkspaceSyncViewModel
import com.Sunset.REN.GitHub.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 设置页字段快照（SettingsPage.schemaFor 固定字段）。
 */
data class SettingsFlags(
    val floatingNavigationEnabled: Boolean = false,
    val soraEditorEnabled: Boolean = false,
    val uiDebugOverlayEnabled: Boolean = false,
    val showUiDebugOverlaySetting: Boolean = false,
    val sectionOrder: List<RepositorySection> = emptyList(),
)

/**
 * 壳宿主控制器（步骤 6 运行时核心，全量页面路由）。
 *
 * - Activity-scoped，持有全部已迁移页面的 ViewModel（Activity 作用域）；
 * - LiveData → Compose 状态桥接：observe(activity) 写回 [mutableStateOf]；
 * - 唯一 action 路由：页面组件与壳导航都经 [handleAction] 分发；
 * - 页面切换 = [ShellPage] 状态 + [ShellState] 派生 + 页面进入回调（prepare/load），
 *   无任何补丁式视图操作。
 */
class ShellHostController(
    private val activity: AppCompatActivity,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    var currentPage by mutableStateOf<ShellPage>(ShellPage.Login)
        private set

    var shellState by mutableStateOf(ShellState())
        private set

    // ---- 页面 UiState（LiveData 桥接） ----
    var loginUiState by mutableStateOf(LoginHomeUiState())
        private set
    var repositoriesState by mutableStateOf<RepositoriesUiState>(RepositoriesUiState.Loading)
        private set
    var notificationsState by mutableStateOf(NotificationsUiState())
        private set
    var profileState by mutableStateOf<ProfileUiState>(ProfileUiState.Loading)
        private set
    var settingsFlags by mutableStateOf(SettingsFlags())
        private set

    var detailState by mutableStateOf<RepositoryDetailUiState>(RepositoryDetailUiState.Loading)
        private set
    var issuesState by mutableStateOf<RepositoryIssuesUiState>(RepositoryIssuesUiState())
        private set
    var pullRequestsState by mutableStateOf<RepositoryPullRequestsUiState>(RepositoryPullRequestsUiState())
        private set
    var issueDetailState by mutableStateOf<RepositoryIssueDetailUiState>(RepositoryIssueDetailUiState())
        private set
    var releasesState by mutableStateOf<RepositoryReleasesUiState>(RepositoryReleasesUiState())
        private set
    var actionsState by mutableStateOf<RepositoryActionsUiState>(RepositoryActionsUiState())
        private set
    var stubState by mutableStateOf(RepositorySectionNativeStubUiState())
        private set
    var runDetailState by mutableStateOf(RepositoryActionRunDetailUiState())
        private set
    var searchState by mutableStateOf<SearchUiState>(SearchUiState.Idle)
        private set
    var fileManagerState by mutableStateOf<LocalFileManagerUiState>(LocalFileManagerUiState())
        private set
    var authState by mutableStateOf<AuthUiState>(AuthUiState.Loading)
        private set
    var rememberedAccounts by mutableStateOf<List<com.Sunset.REN.GitHub.domain.auth.RememberedAccount>>(emptyList())
        private set
    var terminalState by mutableStateOf(TerminalUiState())
        private set
    var workspaceSyncState by mutableStateOf(WorkspaceSyncUiState())
        private set
    var workspacePullState by mutableStateOf(WorkspacePullUiState())
        private set
    var createState by mutableStateOf<RepositoryCreateUiState>(RepositoryCreateUiState.Idle)
        private set
    var forkState by mutableStateOf<RepositoryForkUiState>(RepositoryForkUiState.Loading)
        private set
    var uploadState by mutableStateOf<RepositoryFileUploadUiState>(RepositoryFileUploadUiState())
        private set
    var branchSettingsState by mutableStateOf<RepositoryBranchSettingsUiState>(RepositoryBranchSettingsUiState())
        private set
    var collaboratorsState by mutableStateOf<RepositoryCollaboratorsSettingsUiState>(RepositoryCollaboratorsSettingsUiState())
        private set
    var dangerZoneState by mutableStateOf<RepositoryDangerZoneUiState>(RepositoryDangerZoneUiState())
        private set
    var rulesetsState by mutableStateOf<RepositoryRulesetsUiState>(RepositoryRulesetsUiState())
        private set
    var deployKeysState by mutableStateOf<RepositoryDeployKeysUiState>(RepositoryDeployKeysUiState())
        private set
    var webhooksState by mutableStateOf<RepositoryWebhooksUiState>(RepositoryWebhooksUiState())
        private set
    var actionsSettingsState by mutableStateOf<RepositoryActionsSettingsUiState>(RepositoryActionsSettingsUiState())
        private set
    var securityAlertState by mutableStateOf(RepositorySecurityAlertDetailUiState())
        private set
    var deviceFlowState by mutableStateOf<DeviceFlowUiState>(DeviceFlowUiState.RequestingCode)
        private set
    var tokenReviewState by mutableStateOf(TokenPermissionReviewUiState())
        private set

    // ---- 表单/本地状态 ----
    var searchQuery by mutableStateOf("")
    var terminalCommand by mutableStateOf("")
    var issueCommentDraft by mutableStateOf("")
    var createForm by mutableStateOf(RepositoryCreatePage.FormState())
    var forkOwner by mutableStateOf("")
    var forkName by mutableStateOf("")
    var forkDescription by mutableStateOf("")
    var forkDefaultBranchOnly by mutableStateOf(false)
    var uploadTargetPath by mutableStateOf("")
    var uploadCommitMessage by mutableStateOf("")
    var pullFields by mutableStateOf(WorkspacePullPage.Fields())
    var pushFields by mutableStateOf(WorkspacePushPage.Fields())
    var appLogText by mutableStateOf("")

    // ---- ViewModels（Activity 作用域） ----
    private val loginViewModel: LoginHomeViewModel =
        ViewModelProvider(activity)[LoginHomeViewModel::class.java]
    private val dashboardViewModel: DashboardViewModel =
        ViewModelProvider(activity)[DashboardViewModel::class.java]
    private val notificationsViewModel: NotificationsViewModel =
        ViewModelProvider(activity)[NotificationsViewModel::class.java]
    private val profileViewModel: ProfileViewModel =
        ViewModelProvider(activity)[ProfileViewModel::class.java]
    private val settingsViewModel: SettingsViewModel =
        ViewModelProvider(activity)[SettingsViewModel::class.java]
    private val detailViewModel: RepositoryDetailViewModel =
        ViewModelProvider(activity)[RepositoryDetailViewModel::class.java]
    private val issuesViewModel: RepositoryIssuesViewModel =
        ViewModelProvider(activity)[RepositoryIssuesViewModel::class.java]
    private val pullRequestsViewModel: RepositoryPullRequestsViewModel =
        ViewModelProvider(activity)[RepositoryPullRequestsViewModel::class.java]
    private val issueDetailViewModel: RepositoryIssueDetailViewModel =
        ViewModelProvider(activity)[RepositoryIssueDetailViewModel::class.java]
    private val releasesViewModel: RepositoryReleasesViewModel =
        ViewModelProvider(activity)[RepositoryReleasesViewModel::class.java]
    private val actionsViewModel: RepositoryActionsViewModel =
        ViewModelProvider(activity)[RepositoryActionsViewModel::class.java]
    private val stubViewModel: RepositorySectionNativeStubViewModel =
        ViewModelProvider(activity)[RepositorySectionNativeStubViewModel::class.java]
    private val runDetailViewModel: RepositoryActionRunDetailViewModel =
        ViewModelProvider(activity)[RepositoryActionRunDetailViewModel::class.java]
    private val searchViewModel: SearchViewModel =
        ViewModelProvider(activity)[SearchViewModel::class.java]
    private val fileManagerViewModel: LocalFileManagerViewModel =
        ViewModelProvider(activity)[LocalFileManagerViewModel::class.java]
    private val accountViewModel: AccountViewModel =
        ViewModelProvider(activity)[AccountViewModel::class.java]
    private val terminalViewModel: TerminalViewModel =
        ViewModelProvider(activity)[TerminalViewModel::class.java]
    private val workspaceSyncViewModel: WorkspaceSyncViewModel =
        ViewModelProvider(activity)[WorkspaceSyncViewModel::class.java]
    private val workspacePullViewModel: WorkspacePullViewModel =
        ViewModelProvider(activity)[WorkspacePullViewModel::class.java]
    private val createViewModel: RepositoryCreateViewModel =
        ViewModelProvider(activity)[RepositoryCreateViewModel::class.java]
    private val forkViewModel: RepositoryForkViewModel =
        ViewModelProvider(activity)[RepositoryForkViewModel::class.java]
    private val uploadViewModel: RepositoryFileUploadViewModel =
        ViewModelProvider(activity)[RepositoryFileUploadViewModel::class.java]
    private val branchSettingsViewModel: RepositoryBranchSettingsViewModel =
        ViewModelProvider(activity)[RepositoryBranchSettingsViewModel::class.java]
    private val collaboratorsViewModel: RepositoryCollaboratorsSettingsViewModel =
        ViewModelProvider(activity)[RepositoryCollaboratorsSettingsViewModel::class.java]
    private val dangerZoneViewModel: RepositoryDangerZoneViewModel =
        ViewModelProvider(activity)[RepositoryDangerZoneViewModel::class.java]
    private val rulesetsViewModel: RepositoryRulesetsViewModel =
        ViewModelProvider(activity)[RepositoryRulesetsViewModel::class.java]
    private val deployKeysViewModel: RepositoryDeployKeysViewModel =
        ViewModelProvider(activity)[RepositoryDeployKeysViewModel::class.java]
    private val webhooksViewModel: RepositoryWebhooksViewModel =
        ViewModelProvider(activity)[RepositoryWebhooksViewModel::class.java]
    private val actionsSettingsViewModel: RepositoryActionsSettingsViewModel =
        ViewModelProvider(activity)[RepositoryActionsSettingsViewModel::class.java]
    private val securityAlertViewModel: RepositorySecurityAlertDetailViewModel =
        ViewModelProvider(activity)[RepositorySecurityAlertDetailViewModel::class.java]
    private val deviceFlowViewModel: DeviceFlowViewModel =
        ViewModelProvider(activity)[DeviceFlowViewModel::class.java]
    private val tokenReviewViewModel: TokenPermissionReviewViewModel =
        ViewModelProvider(activity)[TokenPermissionReviewViewModel::class.java]

    private val authSessionRepository = AuthSessionRepository(activity.applicationContext)
    private val backStack = ArrayDeque<ShellPage>()
    private var lastRepoOwner = ""
    private var lastRepoName = ""

    init {
        loginViewModel.state.observe(activity) { state ->
            loginUiState = state
            if (state.shouldEnterHome) {
                navigateTo(ShellPage.Home)
            }
        }
        dashboardViewModel.repositoriesState.observe(activity) { repositoriesState = it }
        notificationsViewModel.notificationsState.observe(activity) { notificationsState = it }
        profileViewModel.profileState.observe(activity) { profileState = it }
        settingsViewModel.isFloatingNavigationEnabled.observe(activity) { settingsFlags = settingsFlags.copy(floatingNavigationEnabled = it) }
        settingsViewModel.isSoraEditorEnabled.observe(activity) { settingsFlags = settingsFlags.copy(soraEditorEnabled = it) }
        settingsViewModel.isUiDebugOverlayEnabled.observe(activity) { settingsFlags = settingsFlags.copy(uiDebugOverlayEnabled = it) }
        settingsViewModel.defaultSectionOrder.observe(activity) { settingsFlags = settingsFlags.copy(sectionOrder = it) }
        detailViewModel.repositoryState.observe(activity) { detailState = it }
        issuesViewModel.issuesState.observe(activity) { issuesState = it }
        pullRequestsViewModel.pullRequestsState.observe(activity) { pullRequestsState = it }
        issueDetailViewModel.detailState.observe(activity) { issueDetailState = it }
        releasesViewModel.releasesState.observe(activity) { releasesState = it }
        actionsViewModel.actionsState.observe(activity) { actionsState = it }
        stubViewModel.sectionState.observe(activity) { stubState = it }
        runDetailViewModel.detailState.observe(activity) { runDetailState = it }
        searchViewModel.searchState.observe(activity) { searchState = it }
        fileManagerViewModel.state.observe(activity) { fileManagerState = it }
        accountViewModel.rememberedAccounts.observe(activity) { rememberedAccounts = it }
        terminalViewModel.state.observe(activity) { terminalState = it }
        workspaceSyncViewModel.state.observe(activity) { workspaceSyncState = it }
        workspacePullViewModel.state.observe(activity) { workspacePullState = it }
        createViewModel.uiState.observe(activity) { createState = it }
        forkViewModel.forkState.observe(activity) { forkState = it }
        uploadViewModel.uploadState.observe(activity) { uploadState = it }
        branchSettingsViewModel.branchSettingsState.observe(activity) { branchSettingsState = it }
        collaboratorsViewModel.collaboratorsState.observe(activity) { collaboratorsState = it }
        dangerZoneViewModel.state.observe(activity) { dangerZoneState = it }
        rulesetsViewModel.state.observe(activity) { rulesetsState = it }
        deployKeysViewModel.state.observe(activity) { deployKeysState = it }
        webhooksViewModel.state.observe(activity) { webhooksState = it }
        actionsSettingsViewModel.actionsSettingsState.observe(activity) { actionsSettingsState = it }
        securityAlertViewModel.detailState.observe(activity) { securityAlertState = it }
        deviceFlowViewModel.state.observe(activity) { state ->
            deviceFlowState = state
            // 设备码登录成功 → 清栈回首页（此前缺失，用户会卡在等待授权页）
            if (state is DeviceFlowUiState.SignedIn) {
                enterHomeAfterLogin()
            }
        }
        tokenReviewViewModel.reviewState.observe(activity) { state ->
            tokenReviewState = state
            // Token 登录成功 → 清栈回首页
            if (state.signedInLogin != null) {
                enterHomeAfterLogin()
            }
        }
        accountViewModel.authState.observe(activity) { state ->
            authState = state
            // 退出登录 → 清栈回登录页（仅当当前不在登录页时触发，避免启动时重复跳转）
            if (state is AuthUiState.SignedOut && currentPage != ShellPage.Login) {
                backStack.clear()
                navigateTo(ShellPage.Login)
            }
        }
        loginViewModel.refresh(autoEnterCurrent = true)
        scope.launch {
            val login = authSessionRepository.getCurrentAccount()?.login
            profileViewModel.start(login)
        }
    }

    fun dispose() {
        scope.cancel()
    }

    // ---- 页面切换 ----

    private fun navigateTo(page: ShellPage) {
        currentPage = page
        shellState = deriveShellState(page)
        onPageEnter(page)
        AppLogger.d(TAG, "navigate to ${page::class.simpleName}")
    }

    private fun push(page: ShellPage) {
        backStack.addLast(currentPage)
        navigateTo(page)
    }

    private fun navigateBack() {
        val previous = backStack.removeLastOrNull()
        if (previous != null) {
            navigateTo(previous)
        } else {
            AppLogger.w(TAG, "shell.back with empty back stack")
        }
    }

    /** 登录成功统一出口：清空返回栈并进入首页，避免返回键退回登录页。 */
    private fun enterHomeAfterLogin() {
        backStack.clear()
        navigateTo(ShellPage.Home)
    }

    private fun deriveShellState(page: ShellPage): ShellState = when (page) {
        ShellPage.Login -> LoginHomePage.shellState()
        ShellPage.Home -> HomePage.shellState()
        ShellPage.Dashboard -> DashboardPage.shellState()
        ShellPage.Notifications -> NotificationsPage.shellState()
        ShellPage.Profile -> ProfilePage.shellState()
        ShellPage.Settings -> SettingsPage.shellState()
        ShellPage.DeviceFlowIntro -> DeviceFlowIntroPage.shellState()
        ShellPage.DeviceFlowCode -> DeviceFlowCodePage.shellState()
        ShellPage.TokenLoginChoice -> TokenLoginChoicePage.shellState()
        ShellPage.TokenGuide -> TokenGuidePage.shellState()
        ShellPage.TokenPermissionReview -> TokenPermissionReviewPage.shellState()
        is ShellPage.RepositoryDetail -> repositoryShellState(page.fullName, "code", "repo_detail")
        is ShellPage.RepositoryStub -> repositoryShellState("$lastRepoOwner/$lastRepoName", page.section.storageKey, "repo_stub")
        is ShellPage.Issues -> repositoryShellState("${page.owner}/${page.repo}", "issues", "issues")
        is ShellPage.PullRequests -> repositoryShellState("${page.owner}/${page.repo}", "pull_requests", "pull_requests")
        is ShellPage.Actions -> repositoryShellState("${page.owner}/${page.repo}", "actions", "actions")
        is ShellPage.Releases -> repositoryShellState("${page.owner}/${page.repo}", "releases", "releases")
        is ShellPage.IssueDetail -> ShellState(title = "#${page.number}", showBack = true, navBarMode = NavBarMode.Hidden, contentKey = "issue_detail")
        is ShellPage.ActionRunDetail -> ShellState(title = "运行详情", showBack = true, navBarMode = NavBarMode.Hidden, contentKey = "action_run_detail")
        is ShellPage.ActionRunDevInfo -> ShellState(title = "运行开发信息", showBack = true, navBarMode = NavBarMode.Hidden, contentKey = "action_run_dev_info")
        is ShellPage.RepoCreate -> ShellState(title = "新建仓库", showBack = true, navBarMode = NavBarMode.Hidden, contentKey = "repo_create")
        is ShellPage.RepoFork -> ShellState(title = "Fork 仓库", showBack = true, navBarMode = NavBarMode.Hidden, contentKey = "repo_fork")
        is ShellPage.FileUpload -> ShellState(title = "上传文件", showBack = true, navBarMode = NavBarMode.Hidden, contentKey = "file_upload")
        is ShellPage.BranchSettings -> ShellState(title = "分支设置", showBack = true, navBarMode = NavBarMode.Hidden, contentKey = "branch_settings")
        is ShellPage.Collaborators -> ShellState(title = "协作者", showBack = true, navBarMode = NavBarMode.Hidden, contentKey = "collaborators")
        is ShellPage.DangerZone -> ShellState(title = "危险区", showBack = true, navBarMode = NavBarMode.Hidden, contentKey = "danger_zone")
        is ShellPage.Rulesets -> ShellState(title = "规则集", showBack = true, navBarMode = NavBarMode.Hidden, contentKey = "rulesets")
        is ShellPage.DeployKeys -> ShellState(title = "部署密钥", showBack = true, navBarMode = NavBarMode.Hidden, contentKey = "deploy_keys")
        is ShellPage.Webhooks -> ShellState(title = "Webhooks", showBack = true, navBarMode = NavBarMode.Hidden, contentKey = "webhooks")
        is ShellPage.ActionsSettings -> ShellState(title = "Actions 设置", showBack = true, navBarMode = NavBarMode.Hidden, contentKey = "actions_settings")
        is ShellPage.SecurityAlertDetail -> ShellState(title = "安全告警", showBack = true, navBarMode = NavBarMode.Hidden, contentKey = "security_alert_detail")
        is ShellPage.SearchPage -> ShellState(title = "搜索", showBack = true, navBarMode = NavBarMode.Hidden, contentKey = "search")
        ShellPage.FileManager -> ShellState(title = "文件管理器", showBack = true, navBarMode = NavBarMode.Hidden, contentKey = "file_manager")
        ShellPage.Account -> ShellState(title = "账户", showBack = true, navBarMode = NavBarMode.Hidden, contentKey = "account")
        is ShellPage.NotificationDetail -> ShellState(title = "通知详情", showBack = true, navBarMode = NavBarMode.Hidden, contentKey = "notification_detail")
        ShellPage.Terminal -> ShellState(title = "终端", showBack = true, navBarMode = NavBarMode.Hidden, contentKey = "terminal")
        ShellPage.WorkspaceSync -> ShellState(title = "工作区同步", showBack = true, navBarMode = NavBarMode.Hidden, contentKey = "workspace_sync")
        ShellPage.WorkspacePull -> ShellState(title = "拉取到工作区", showBack = true, navBarMode = NavBarMode.Hidden, contentKey = "workspace_pull")
        ShellPage.WorkspacePush -> ShellState(title = "推送到远端", showBack = true, navBarMode = NavBarMode.Hidden, contentKey = "workspace_push")
        ShellPage.AppLog -> ShellState(title = "应用日志", showBack = true, navBarMode = NavBarMode.Hidden, contentKey = "app_log")
        ShellPage.RustCore -> ShellState(title = "Rust 核心", showBack = true, navBarMode = NavBarMode.Hidden, contentKey = "rust_core")
    }

    private fun repositoryShellState(title: String, selectedId: String, contentKey: String): ShellState {
        val fullName = "$lastRepoOwner/$lastRepoName"
        val navItems = if (lastRepoOwner.isNotBlank()) {
            detailViewModel.sectionOrder(fullName)
                .filter { it != RepositorySection.More }
                .map { s ->
                    ShellNavItem(
                        id = s.storageKey,
                        label = activity.getString(s.titleResId),
                        icon = sectionIcon(s),
                        action = "repo_section.open.${s.storageKey}",
                    )
                }
        } else {
            emptyList()
        }
        return ShellState(
            title = title,
            showBack = true,
            navBarMode = NavBarMode.RepositorySections,
            navItems = navItems,
            selectedNavId = selectedId,
            contentKey = contentKey,
        )
    }

    private fun sectionIcon(section: RepositorySection): IconId = when (section) {
        RepositorySection.Code -> IconId.Code
        RepositorySection.Issues -> IconId.Issue
        RepositorySection.PullRequests -> IconId.PullRequest
        RepositorySection.Actions -> IconId.Refresh
        RepositorySection.Projects -> IconId.Folder
        RepositorySection.SecurityQuality -> IconId.Eye
        RepositorySection.Insights -> IconId.ArrowUp
        RepositorySection.Wiki -> IconId.File
        RepositorySection.Agents -> IconId.Person
        RepositorySection.Settings -> IconId.Settings
        RepositorySection.Fork -> IconId.Fork
        RepositorySection.More -> IconId.More
    }

    private fun onPageEnter(page: ShellPage) {
        when (page) {
            is ShellPage.RepositoryDetail -> {
                lastRepoOwner = page.owner
                lastRepoName = page.repo
                detailViewModel.loadRepository(page.owner, page.repo)
            }
            is ShellPage.RepositoryStub -> {
                stubViewModel.prepare(lastRepoOwner, lastRepoName, page.section.storageKey)
                stubViewModel.loadSection()
            }
            is ShellPage.Issues -> issuesViewModel.prepare(page.owner, page.repo)
            is ShellPage.PullRequests -> pullRequestsViewModel.prepare(page.owner, page.repo)
            is ShellPage.IssueDetail -> issueDetailViewModel.prepare(page.owner, page.repo, page.number)
            is ShellPage.Releases -> releasesViewModel.prepare(page.owner, page.repo)
            is ShellPage.Actions -> actionsViewModel.prepare(page.owner, page.repo)
            is ShellPage.ActionRunDetail -> {
                runDetailViewModel.prepare(page.owner, page.repo, page.runId)
                runDetailViewModel.load()
            }
            is ShellPage.ActionRunDevInfo -> {
                runDetailViewModel.prepare(page.owner, page.repo, page.runId)
            }
            is ShellPage.RepoFork -> forkViewModel.prepare(page.owner, page.repo)
            is ShellPage.FileUpload -> uploadViewModel.prepare(page.owner, page.repo, "", "")
            is ShellPage.BranchSettings -> branchSettingsViewModel.prepare(page.owner, page.repo)
            is ShellPage.Collaborators -> collaboratorsViewModel.prepare(page.owner, page.repo)
            is ShellPage.DangerZone -> dangerZoneViewModel.prepare(page.owner, page.repo)
            is ShellPage.Rulesets -> rulesetsViewModel.prepare(page.owner, page.repo)
            is ShellPage.DeployKeys -> deployKeysViewModel.prepare(page.owner, page.repo)
            is ShellPage.Webhooks -> webhooksViewModel.prepare(page.owner, page.repo)
            is ShellPage.ActionsSettings -> actionsSettingsViewModel.prepare(page.owner, page.repo)
            is ShellPage.SecurityAlertDetail -> securityAlertViewModel.prepare(page.owner, page.repo, "", page.alertNumber, null)
            is ShellPage.SearchPage -> {
                if (page.initialQuery.isNotBlank()) {
                    searchQuery = page.initialQuery
                    searchViewModel.search(page.initialQuery, SearchType.Repositories)
                }
            }
            ShellPage.Account -> accountViewModel.refreshAccountState()
            ShellPage.WorkspaceSync -> workspaceSyncViewModel.loadInitialWorkspace()
            ShellPage.WorkspacePull -> workspacePullViewModel.loadInitialWorkspace()
            ShellPage.WorkspacePush -> workspaceSyncViewModel.loadInitialWorkspace()
            ShellPage.Terminal -> terminalViewModel.loadInitialWorkspace()
            ShellPage.DeviceFlowCode -> deviceFlowViewModel.start()
            ShellPage.AppLog -> appLogText = AppLogger.readLogText()
            ShellPage.RustCore -> refreshRustCore()
            else -> Unit
        }
    }

    // ---- action 路由 ----

    fun handleAction(action: String) {
        when {
            action == "shell.back" -> navigateBack()
            action == "nav.home" -> navigateTo(ShellPage.Home)
            action == "nav.dashboard" -> navigateTo(ShellPage.Dashboard)
            action == "nav.notifications" -> navigateTo(ShellPage.Notifications)
            action == "nav.profile" -> navigateTo(ShellPage.Profile)
            action == "nav.settings" -> push(ShellPage.Settings)
            action == "nav.search" -> push(ShellPage.SearchPage())
            action == "login.device_flow" -> push(ShellPage.DeviceFlowIntro)
            action == "login.token_login" -> push(ShellPage.TokenLoginChoice)
            action.startsWith("settings.toggle.") -> handleSettingsToggle(action)
            action.startsWith("settings.section.") -> handleSettingsSectionMove(action)
            action == "settings.open_account" -> push(ShellPage.Account)
            action == "settings.open_app_log" -> push(ShellPage.AppLog)
            action == "settings.open_rust_core" -> push(ShellPage.RustCore)
            action == "settings.open_sync" -> push(ShellPage.WorkspaceSync)
            action == "settings.open_terminal" -> push(ShellPage.Terminal)
            action == "dashboard.load_more" -> dashboardViewModel.loadMoreRepositories()
            action.startsWith("dashboard.pin.") -> dashboardViewModel.togglePinned(action.removePrefix("dashboard.pin."))
            action.startsWith("dashboard.star.") -> dashboardViewModel.toggleFavorite(action.removePrefix("dashboard.star."))
            action == "dashboard.sort" -> AppLogger.w(TAG, "dashboard.sort not bridged yet")
            action.startsWith("repo.open.") -> openRepositoryFromAction(action.removePrefix("repo.open."))
            action.startsWith("profile.repo.open.") -> openRepositoryFromAction(action.removePrefix("profile.repo.open."))
            action.startsWith("repo_section.open.") -> openRepositorySection(action.removePrefix("repo_section.open."))
            action == "profile.open_github" -> AppLogger.w(TAG, "profile.open_github not bridged yet")
            action.startsWith("notifications.filter.all") -> notificationsViewModel.switchAll(true)
            action.startsWith("notifications.filter.unread") -> notificationsViewModel.switchAll(false)
            action == "notifications.load_more" -> notificationsViewModel.loadNextPage()
            action.startsWith("notifications.open.") -> push(ShellPage.NotificationDetail(action.removePrefix("notifications.open.")))
            action.startsWith("notifications.mark_read.") -> notificationsViewModel.markAsRead(action.removePrefix("notifications.mark_read."))
            action.startsWith("issues.open.") -> openIssueFromAction(action.removePrefix("issues.open."))
            action == "issues.retry" -> issuesViewModel.loadFirstPage()
            action == "issues.load_more" -> issuesViewModel.loadNextPage()
            action == "prs.retry" -> pullRequestsViewModel.loadFirstPage()
            action == "prs.load_more" -> pullRequestsViewModel.loadNextPage()
            action == "releases.retry" -> releasesViewModel.loadFirstPage()
            action == "releases.load_more" -> releasesViewModel.loadNextPage()
            action.startsWith("releases.download.") -> AppLogger.w(TAG, "releases.download not bridged yet: $action")
            action == "actions.retry" -> actionsViewModel.reload()
            action == "actions.load_more" -> actionsViewModel.loadNextPage()
            action.startsWith("actions.run.open.") -> {
                action.removePrefix("actions.run.open.").toLongOrNull()?.let { runId ->
                    val repo = currentRepoContext()
                    push(ShellPage.ActionRunDetail(repo.first, repo.second, runId))
                }
            }
            action == "action_run_detail.retry" -> runDetailViewModel.load()
            action == "action_run_detail.open_actions" -> {
                val repo = currentRepoContext()
                push(ShellPage.Actions(repo.first, repo.second))
            }
            action == "action_run_detail.open_run" -> AppLogger.w(TAG, "action_run_detail.open_run not bridged yet")
            action == "action_run_detail.refresh_logs" -> runDetailViewModel.refreshLogs()
            action == "action_run_detail.download_logs" -> AppLogger.w(TAG, "action_run_detail.download_logs not bridged yet")
            action.startsWith("action_run_detail.download_artifact.") -> AppLogger.w(TAG, "action_run_detail.download_artifact not bridged yet: $action")
            action == "action_run_dev_info.retry" -> runDetailViewModel.load()
            action == "action_run_dev_info.open_actions" -> {
                val repo = currentRepoContext()
                push(ShellPage.Actions(repo.first, repo.second))
            }
            action == "search.retry" -> searchViewModel.retry()
            action == "search.prev" -> searchViewModel.prevPage()
            action == "search.next" -> searchViewModel.nextPage()
            action.startsWith("search.open.") -> AppLogger.w(TAG, "search.open not bridged yet: $action")
            action.startsWith("filemanager.") -> handleFileManagerAction(action)
            action.startsWith("workspace_pull.") -> handleWorkspacePullAction(action)
            action.startsWith("workspace_push.") -> handleWorkspacePushAction(action)
            action == "app_log.copy" -> handleAppLogCopy()
            action == "app_log.refresh" -> appLogText = AppLogger.readLogText()
            action == "rust_core.refresh" -> refreshRustCore()
            action.startsWith("terminal.") -> handleTerminalAction(action)
            action == "account.switch" -> AppLogger.w(TAG, "account.switch not bridged yet")
            action == "account.sign_out" -> accountViewModel.signOut()
            action.startsWith("account.remove.") -> AppLogger.w(TAG, "account.remove not bridged yet: $action")
            action.startsWith("device_flow.") -> handleDeviceFlowAction(action)
            action.startsWith("token.") -> handleTokenAction(action)
            action.startsWith("branch_settings.") -> handleBranchSettingsAction(action)
            action.startsWith("collaborators.") -> handleCollaboratorsAction(action)
            action.startsWith("danger_zone.") -> handleDangerZoneAction(action)
            action.startsWith("rulesets.") -> handleRulesetsAction(action)
            action.startsWith("deploy_keys.") -> handleDeployKeysAction(action)
            action.startsWith("webhooks.") -> handleWebhooksAction(action)
            action.startsWith("actions_settings.") -> handleActionsSettingsAction(action)
            action.startsWith("security_alert.") -> handleSecurityAlertAction(action)
            action.startsWith("create.") -> handleCreateAction(action)
            action.startsWith("fork.") -> handleForkAction(action)
            action.startsWith("upload.") -> handleUploadAction(action)
            else -> AppLogger.w(TAG, "unhandled shell action: $action")
        }
    }

    private fun currentRepoContext(): Pair<String, String> = lastRepoOwner to lastRepoName

    private fun openRepositoryFromAction(fullName: String) {
        val parts = fullName.split("/")
        if (parts.size != 2) {
            AppLogger.w(TAG, "invalid repo full name: $fullName")
            return
        }
        lastRepoOwner = parts[0]
        lastRepoName = parts[1]
        push(ShellPage.RepositoryDetail(parts[0], parts[1], fullName))
    }

    private fun openRepositorySection(key: String) {
        if (lastRepoOwner.isBlank()) return
        when (key) {
            "code" -> navigateTo(ShellPage.RepositoryDetail(lastRepoOwner, lastRepoName, "$lastRepoOwner/$lastRepoName"))
            "issues" -> navigateTo(ShellPage.Issues(lastRepoOwner, lastRepoName))
            "pull_requests" -> navigateTo(ShellPage.PullRequests(lastRepoOwner, lastRepoName))
            "actions" -> navigateTo(ShellPage.Actions(lastRepoOwner, lastRepoName))
            "releases" -> navigateTo(ShellPage.Releases(lastRepoOwner, lastRepoName))
            "fork" -> navigateTo(ShellPage.RepoFork(lastRepoOwner, lastRepoName, "$lastRepoOwner/$lastRepoName"))
            "settings" -> navigateTo(ShellPage.BranchSettings(lastRepoOwner, lastRepoName))
            "projects", "security_quality", "insights", "wiki", "agents" -> {
                val section = RepositorySection.entries.firstOrNull { it.storageKey == key }
                if (section != null) {
                    navigateTo(ShellPage.RepositoryStub(lastRepoOwner, lastRepoName, section))
                }
            }
            else -> AppLogger.w(TAG, "unknown repo section: $key")
        }
    }

    private fun openIssueFromAction(issueRef: String) {
        val parts = issueRef.split("/")
        val number = parts.lastOrNull()?.toIntOrNull() ?: return
        if (lastRepoOwner.isNotBlank()) {
            push(ShellPage.IssueDetail(lastRepoOwner, lastRepoName, number))
        }
    }

    private fun handleSettingsToggle(action: String) {
        when (action) {
            "settings.toggle.floating_nav" ->
                settingsViewModel.setFloatingNavigationEnabled(!settingsFlags.floatingNavigationEnabled)
            "settings.toggle.sora_editor" ->
                settingsViewModel.setSoraEditorEnabled(!settingsFlags.soraEditorEnabled)
            "settings.toggle.ui_debug_overlay" ->
                settingsViewModel.setUiDebugOverlayEnabled(!settingsFlags.uiDebugOverlayEnabled)
            else -> AppLogger.w(TAG, "unhandled settings toggle: $action")
        }
    }

    private fun handleSettingsSectionMove(action: String) {
        val rest = action.removePrefix("settings.section.")
        val (direction, storageKey) = if (rest.startsWith("up.")) "up" to rest.removePrefix("up.") else "down" to rest.removePrefix("down.")
        val sections = settingsFlags.sectionOrder.toMutableList()
        val index = sections.indexOfFirst { it.storageKey == storageKey }
        if (index < 0) return
        val target = if (direction == "up") index - 1 else index + 1
        if (target !in sections.indices) return
        val item = sections.removeAt(index)
        sections.add(target, item)
        settingsViewModel.setDefaultSectionOrder(sections)
    }

    private fun handleFileManagerAction(action: String) {
        AppLogger.w(TAG, "filemanager action not bridged yet: $action")
    }

    private fun handleWorkspacePullAction(action: String) {
        when {
            action == "workspace_pull.create_workspace" ->
                workspacePullViewModel.createWorkspace(pullFields.workspaceName)
            action == "workspace_pull.toggle_overwrite" ->
                pullFields = pullFields.copy(overwriteLocal = !pullFields.overwriteLocal)
            action == "workspace_pull.preview" -> workspacePullViewModel.preview(
                workspacePullInput()
            )
            action == "workspace_pull.execute" -> workspacePullViewModel.pull(
                workspacePullInput()
            )
            action.startsWith("workspace_pull.field.") -> {
                val key = action.removePrefix("workspace_pull.field.")
                updatePullField(key)
            }
            else -> AppLogger.w(TAG, "unhandled workspace_pull action: $action")
        }
    }

    private fun workspacePullInput(): com.Sunset.REN.GitHub.ui.workspace.WorkspacePullInput {
        val f = pullFields
        return com.Sunset.REN.GitHub.ui.workspace.WorkspacePullInput(
            owner = f.owner,
            repo = f.repo,
            branch = f.branch,
            remotePath = f.remotePath,
            localTarget = f.localTarget,
            overwriteLocal = f.overwriteLocal,
        )
    }

    private fun updatePullField(key: String) {
        when (key) {
            "workspace_name" -> Unit
            "owner" -> Unit
            "repo" -> Unit
            "branch" -> Unit
            "remote_path" -> Unit
            "local_target" -> Unit
        }
    }

    private fun handleWorkspacePushAction(action: String) {
        when {
            action == "workspace_push.create_workspace" ->
                workspaceSyncViewModel.createWorkspace(pushFields.workspaceName)
            action == "workspace_push.import" ->
                workspaceSyncViewModel.importPath(pushFields.importPath, pushFields.importTarget)
            action == "workspace_push.dry_run" -> workspaceSyncViewModel.dryRun(workspaceSyncInput())
            action == "workspace_push.execute" -> workspaceSyncViewModel.execute(workspaceSyncInput())
            action == "workspace_push.toggle.mirror_mode" ->
                pushFields = pushFields.copy(mirrorMode = !pushFields.mirrorMode)
            action == "workspace_push.toggle.destructive_confirmed" ->
                pushFields = pushFields.copy(destructiveConfirmed = !pushFields.destructiveConfirmed)
            action == "workspace_push.toggle.allow_overwrite_remote" ->
                pushFields = pushFields.copy(allowOverwriteRemoteChanges = !pushFields.allowOverwriteRemoteChanges)
            else -> AppLogger.w(TAG, "unhandled workspace_push action: $action")
        }
    }

    private fun workspaceSyncInput(): com.Sunset.REN.GitHub.ui.workspace.WorkspaceSyncInput {
        val f = pushFields
        return com.Sunset.REN.GitHub.ui.workspace.WorkspaceSyncInput(
            owner = f.owner,
            repo = f.repo,
            branch = f.branch,
            remotePath = f.remotePath,
            commitMessage = f.commitMessage,
            mirrorMode = f.mirrorMode,
            destructiveConfirmed = f.destructiveConfirmed,
            allowOverwriteRemoteChanges = f.allowOverwriteRemoteChanges,
        )
    }

    private fun handleAppLogCopy() {
        val logText = appLogText
        if (logText.isBlank()) return
        val clipboard = activity.getSystemService(android.content.ClipboardManager::class.java)
        clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("SunsetGitHub 日志", logText))
        AppLogger.i(TAG, "app log copied (${logText.length} chars)")
    }

    private fun handleTerminalAction(action: String) {
        when (action) {
            "terminal.run" -> terminalViewModel.runCommand(terminalCommand)
            "terminal.clear" -> terminalCommand = ""
            else -> AppLogger.w(TAG, "terminal action not bridged yet: $action")
        }
    }

    private fun handleDeviceFlowAction(action: String) {
        when (action) {
            "device_flow.start" -> push(ShellPage.DeviceFlowCode)
            "device_flow.cancel" -> navigateBack()
            "device_flow.retry" -> deviceFlowViewModel.start()
            else -> AppLogger.w(TAG, "device_flow action not bridged yet: $action")
        }
    }

    private fun handleTokenAction(action: String) {
        when (action) {
            "token.start" -> push(ShellPage.TokenGuide)
            "token.guide_next" -> push(ShellPage.TokenPermissionReview)
            "token.retry" -> tokenReviewViewModel.inspectToken()
            "token.confirm" -> tokenReviewViewModel.confirmLogin()
            "token.cancel" -> navigateBack()
            else -> AppLogger.w(TAG, "token action not bridged yet: $action")
        }
    }

    private fun handleBranchSettingsAction(action: String) {
        when {
            action == "branch_settings.retry" -> branchSettingsViewModel.refresh()
            action.startsWith("branch_settings.select.") -> {
                branchSettingsViewModel.loadProtection(action.removePrefix("branch_settings.select."))
            }
            else -> AppLogger.w(TAG, "branch_settings action not bridged yet: $action")
        }
    }

    private fun handleCollaboratorsAction(action: String) {
        when {
            action == "collaborators.retry" -> collaboratorsViewModel.refresh()
            action.startsWith("collaborators.cancel_invite.") -> {
                action.removePrefix("collaborators.cancel_invite.").toIntOrNull()
                    ?.let { collaboratorsViewModel.cancelInvitation(it.toLong()) }
            }
            else -> AppLogger.w(TAG, "collaborators action not bridged yet: $action")
        }
    }

    private fun handleDangerZoneAction(action: String) {
        when (action) {
            "danger_zone.retry" -> dangerZoneViewModel.refresh()
            "danger_zone.archive" -> dangerZoneViewModel.setArchived(true)
            "danger_zone.unarchive" -> dangerZoneViewModel.setArchived(false)
            "danger_zone.delete" -> AppLogger.w(TAG, "danger_zone.delete 需确认弹窗，未桥接")
            "danger_zone.transfer" -> AppLogger.w(TAG, "danger_zone.transfer 需表单弹窗，未桥接")
            else -> AppLogger.w(TAG, "danger_zone action not bridged yet: $action")
        }
    }

    private fun handleRulesetsAction(action: String) {
        when (action) {
            "rulesets.refresh" -> rulesetsViewModel.refresh()
            "rulesets.retry" -> rulesetsViewModel.refresh()
            else -> AppLogger.w(TAG, "rulesets action not bridged yet: $action")
        }
    }

    private fun handleDeployKeysAction(action: String) {
        when {
            action == "deploy_keys.refresh" || action == "deploy_keys.retry" -> deployKeysViewModel.refresh()
            action == "deploy_keys.add" -> AppLogger.w(TAG, "deploy_keys.add dialog not bridged yet")
            action.startsWith("deploy_keys.delete.") -> {
                deployKeysState.snapshot?.keys?.firstOrNull { it.id.toString() == action.removePrefix("deploy_keys.delete.") }
                    ?.let { deployKeysViewModel.delete(it) }
            }
            else -> AppLogger.w(TAG, "deploy_keys action not bridged yet: $action")
        }
    }

    private fun handleWebhooksAction(action: String) {
        when {
            action == "webhooks.refresh" || action == "webhooks.retry" -> webhooksViewModel.refresh()
            action == "webhooks.create" -> AppLogger.w(TAG, "webhooks.create dialog not bridged yet")
            action.startsWith("webhooks.ping.") -> {
                webhooksState.snapshot?.hooks?.firstOrNull { it.id.toString() == action.removePrefix("webhooks.ping.") }
                    ?.let { webhooksViewModel.ping(it) }
            }
            action.startsWith("webhooks.delete.") -> {
                webhooksState.snapshot?.hooks?.firstOrNull { it.id.toString() == action.removePrefix("webhooks.delete.") }
                    ?.let { webhooksViewModel.delete(it) }
            }
            else -> AppLogger.w(TAG, "webhooks action not bridged yet: $action")
        }
    }

    private fun handleActionsSettingsAction(action: String) {
        when {
            action == "actions_settings.retry" -> actionsSettingsViewModel.refresh()
            action.startsWith("actions_settings.set_enabled.") -> {
                actionsSettingsViewModel.setActionsEnabled(action.removePrefix("actions_settings.set_enabled.") == "true")
            }
            action.startsWith("actions_settings.set_allowed.") -> {
                when (action.removePrefix("actions_settings.set_allowed.")) {
                    "all" -> actionsSettingsViewModel.setAllowedActions("all")
                    "local_only" -> actionsSettingsViewModel.setAllowedActions("local_only")
                    "selected" -> actionsSettingsViewModel.setAllowedActions("selected")
                }
            }
            action == "actions_settings.toggle_github_owned" -> {
                val selected = actionsSettingsState.snapshot?.selectedActions
                actionsSettingsViewModel.setSelectedActions(
                    githubOwnedAllowed = !(selected?.githubOwnedAllowed ?: false),
                    verifiedAllowed = selected?.verifiedAllowed ?: false,
                    patterns = selected?.patternsAllowed ?: emptyList(),
                )
            }
            action == "actions_settings.toggle_verified" -> {
                val selected = actionsSettingsState.snapshot?.selectedActions
                actionsSettingsViewModel.setSelectedActions(
                    githubOwnedAllowed = selected?.githubOwnedAllowed ?: false,
                    verifiedAllowed = !(selected?.verifiedAllowed ?: false),
                    patterns = selected?.patternsAllowed ?: emptyList(),
                )
            }
            action == "actions_settings.edit_patterns" -> AppLogger.w(TAG, "actions_settings.edit_patterns dialog not bridged yet")
            action.startsWith("actions_settings.set_workflow.") -> {
                when (action.removePrefix("actions_settings.set_workflow.")) {
                    "read" -> actionsSettingsViewModel.setWorkflowDefaultPermission("read")
                    "write" -> actionsSettingsViewModel.setWorkflowDefaultPermission("write")
                }
            }
            action == "actions_settings.toggle_pr_approval" ->
                actionsSettingsViewModel.setWorkflowPullRequestApproval(
                    canApprovePullRequestReviews = !(actionsSettingsState.snapshot?.workflowPermissions?.canApprovePullRequestReviews ?: false)
                )
            action == "actions_settings.edit_retention" -> AppLogger.w(TAG, "actions_settings.edit_retention dialog not bridged yet")
            else -> AppLogger.w(TAG, "actions_settings action not bridged yet: $action")
        }
    }

    private fun handleSecurityAlertAction(action: String) {
        when {
            action == "security_alert.retry" -> securityAlertViewModel.load()
            action.startsWith("security_alert.open_in_github") -> AppLogger.w(TAG, "security_alert.open_in_github not bridged yet")
            else -> AppLogger.w(TAG, "security_alert action not bridged yet: $action")
        }
    }

    private fun handleCreateAction(action: String) {
        when {
            action.startsWith("create.field.") -> {
                updateCreateField(action.removePrefix("create.field."), "")
            }
            action == "create.toggle.private" -> createForm = createForm.copy(isPrivate = !createForm.isPrivate)
            action == "create.toggle.readme" -> createForm = createForm.copy(createReadme = !createForm.createReadme)
            action == "create.submit" -> createViewModel.createRepository(
                name = createForm.name,
                description = createForm.description,
                homepage = createForm.homepage,
                isPrivate = createForm.isPrivate,
                autoInit = createForm.createReadme,
                gitignoreTemplate = null,
                licenseTemplate = null,
                hasIssues = createForm.hasIssues,
                hasProjects = createForm.hasProjects,
                hasWiki = createForm.hasWiki,
            )
            else -> AppLogger.w(TAG, "create action not bridged yet: $action")
        }
    }

    fun updateCreateField(key: String, value: String) {
        createForm = when (key) {
            "name" -> createForm.copy(name = value)
            "description" -> createForm.copy(description = value)
            "homepage" -> createForm.copy(homepage = value)
            else -> createForm
        }
    }

    private fun handleForkAction(action: String) {
        when {
            action == "fork.retry" -> forkViewModel.prepare(lastRepoOwner, lastRepoName)
            action.startsWith("fork.owner.") -> forkOwner = action.removePrefix("fork.owner.")
            action.startsWith("fork.name.") -> forkName = action.removePrefix("fork.name.")
            action == "fork.toggle.default_branch_only" ->
                forkDefaultBranchOnly = !forkDefaultBranchOnly
            action == "fork.submit" -> forkViewModel.createFork(forkOwner, forkName, forkDescription, forkDefaultBranchOnly)
            else -> AppLogger.w(TAG, "fork action not bridged yet: $action")
        }
    }

    private fun handleUploadAction(action: String) {
        when {
            action.startsWith("upload.target_path.") -> uploadTargetPath = action.removePrefix("upload.target_path.")
            action.startsWith("upload.commit_message.") -> uploadCommitMessage = action.removePrefix("upload.commit_message.")
            action == "upload.submit" -> uploadViewModel.submit(
                message = uploadCommitMessage,
                conflictResolution = com.Sunset.REN.GitHub.ui.repo.RepositoryFileWriteConflictResolution.Prompt,
            )
            else -> AppLogger.w(TAG, "upload action not bridged yet: $action")
        }
    }

    /** 从宿主 Activity 解析字符串资源。 */
    fun string(@StringRes resId: Int): String = activity.getString(resId)

    private companion object {
        const val TAG = "ShellHost"
    }

    // ---- Rust 核心自检（阶段 6：UniFFI 桥接） ----
    var rustCoreStatus by mutableStateOf("未加载")
        private set
    var rustCoreSizeLines by mutableStateOf(listOf<String>())
        private set
    var rustCoreCategoryLines by mutableStateOf(listOf<String>())
        private set
    var rustCoreMarkdownHtml by mutableStateOf("")
        private set

    /** 调用 sunset-ffi（UniFFI Kotlin 绑定）刷新自检数据。 */
    fun refreshRustCore() {
        try {
            rustCoreStatus = uniffi.sunset_ffi.hello()
            rustCoreSizeLines = listOf(
                "0 B → ${uniffi.sunset_ffi.fileSizeLabel(0UL)}",
                "1 KB → ${uniffi.sunset_ffi.fileSizeLabel(1024UL)}",
                "1.5 MB → ${uniffi.sunset_ffi.fileSizeLabel(1572864UL)}",
                "2.4 GB → ${uniffi.sunset_ffi.fileSizeLabel(2576980378UL)}",
            )
            rustCoreCategoryLines = listOf(
                "README.md → ${uniffi.sunset_ffi.fileCategory("README.md")}",
                "app.apk → ${uniffi.sunset_ffi.fileCategory("app.apk")}",
                "photo.jpg → ${uniffi.sunset_ffi.fileCategory("photo.jpg")}",
                "script.sh → ${uniffi.sunset_ffi.fileCategory("script.sh")}",
            )
            rustCoreMarkdownHtml = uniffi.sunset_ffi.markdownToHtml(
                "# SunsetGitHub\n\nRust 核心 **UniFFI** 桥接验证：`markdown_to_html()` 由 sunset-core 渲染。\n\n- 项目：SunsetGitHub\n- 状态：OK\n"
            )
        } catch (t: Throwable) {
            rustCoreStatus = "加载失败：${t.message ?: t.javaClass.simpleName}"
            rustCoreSizeLines = emptyList()
            rustCoreCategoryLines = emptyList()
            rustCoreMarkdownHtml = ""
        }
    }

    /** 当前账号判断（委托 AccountViewModel）。 */
    fun isCurrentAccount(account: com.Sunset.REN.GitHub.domain.auth.GitHubAccount): Boolean =
        accountViewModel.isCurrentAccount(account)
}

/**
 * 新壳宿主 Composable：唯一内容区入口。
 * MainActivity 挂载本函数后，运行时壳 = AppShell（三区硬约束）+ 页面 schema。
 */
@Composable
fun ShellHost(controller: ShellHostController) {
    AppShell(state = controller.shellState, onAction = controller::handleAction) {
        when (val page = controller.currentPage) {
            ShellPage.Login ->
                LoginHomePage.schemaFor(controller.loginUiState.message).renderPage(controller::handleAction)

            ShellPage.Home ->
                HomePage.schema.renderPage(controller::handleAction)

            ShellPage.Dashboard ->
                DashboardPage.schemaFor(controller.repositoriesState).renderPage(controller::handleAction)

            ShellPage.Notifications ->
                NotificationsPage.schemaFor(controller.notificationsState).renderPage(controller::handleAction)

            ShellPage.Profile ->
                ProfilePage.schemaFor(controller.profileState).renderPage(controller::handleAction)

            ShellPage.Settings ->
                SettingsPage.schemaFor(
                    floatingNavigationEnabled = controller.settingsFlags.floatingNavigationEnabled,
                    soraEditorEnabled = controller.settingsFlags.soraEditorEnabled,
                    uiDebugOverlayEnabled = controller.settingsFlags.uiDebugOverlayEnabled,
                    showUiDebugOverlaySetting = controller.settingsFlags.showUiDebugOverlaySetting,
                    repositorySectionOrder = controller.settingsFlags.sectionOrder,
                ).renderPage(controller::handleAction)

            ShellPage.DeviceFlowIntro ->
                DeviceFlowIntroPage.schemaFor().renderPage(controller::handleAction)

            ShellPage.DeviceFlowCode ->
                DeviceFlowCodePage.schemaFor(controller.deviceFlowState).renderPage(controller::handleAction)

            ShellPage.TokenLoginChoice ->
                TokenLoginChoicePage.schemaFor().renderPage(controller::handleAction)

            ShellPage.TokenGuide ->
                TokenGuidePage.schemaFor().renderPage(controller::handleAction)

            ShellPage.TokenPermissionReview ->
                TokenPermissionReviewPage.schemaFor(controller.tokenReviewState).renderPage(controller::handleAction)

            is ShellPage.RepositoryDetail ->
                RepositoryDetailPage.schemaFor(controller.detailState).renderPage(controller::handleAction)

            is ShellPage.RepositoryStub -> {
                val sectionTitle = controller.string(page.section.titleResId)
                RepositorySectionStubPage.schemaFor(
                    state = controller.stubState,
                    sectionTitle = sectionTitle,
                    fallbackDescription = "该分区暂不支持原生渲染，请在 GitHub 中打开。",
                    repositoryLabel = "${page.owner}/${page.repo}",
                    initialSectionUrl = controller.stubState.sourceUrl.orEmpty(),
                ).renderPage(controller::handleAction)
            }

            is ShellPage.Issues ->
                IssuesPage.schemaFor(controller.issuesState).renderPage(controller::handleAction)

            is ShellPage.PullRequests ->
                PullRequestsPage.schemaFor(controller.pullRequestsState).renderPage(controller::handleAction)

            is ShellPage.IssueDetail ->
                IssueDetailPage.schemaFor(
                    state = controller.issueDetailState,
                    commentDraft = controller.issueCommentDraft,
                    onCommentDraftChange = { controller.issueCommentDraft = it },
                ).renderPage(controller::handleAction)

            is ShellPage.Releases ->
                ReleasesPage.schemaFor(controller.releasesState, downloads = emptyList()).renderPage(controller::handleAction)

            is ShellPage.Actions ->
                ActionsPage.schemaFor(controller.actionsState).renderPage(controller::handleAction)

            is ShellPage.ActionRunDetail ->
                RepositoryActionRunDetailPage.schemaFor(controller.runDetailState).renderPage(controller::handleAction)

            is ShellPage.ActionRunDevInfo ->
                RepositoryActionRunDeveloperInfoPage.schemaFor(controller.runDetailState).renderPage(controller::handleAction)

            is ShellPage.RepoCreate ->
                RepositoryCreatePage.schemaFor(
                    state = controller.createState,
                    form = controller.createForm,
                    initialFilesHint = "README.md",
                    onFieldChange = { key, value -> controller.updateCreateField(key, value) },
                ).renderPage(controller::handleAction)

            is ShellPage.RepoFork ->
                RepositoryForkPage.schemaFor(
                    state = controller.forkState,
                    targetOwner = controller.forkOwner,
                    targetName = controller.forkName,
                    description = controller.forkDescription,
                    defaultBranchOnly = controller.forkDefaultBranchOnly,
                    onOwnerChange = { controller.forkOwner = it },
                    onNameChange = { controller.forkName = it },
                    onDescriptionChange = { controller.forkDescription = it },
                ).renderPage(controller::handleAction)

            is ShellPage.FileUpload ->
                RepositoryFileUploadPage.schemaFor(
                    state = controller.uploadState,
                    repositoryContext = "${page.owner}/${page.repo}",
                    targetPath = controller.uploadTargetPath,
                    commitMessage = controller.uploadCommitMessage,
                    onTargetPathChange = { controller.uploadTargetPath = it },
                    onCommitMessageChange = { controller.uploadCommitMessage = it },
                ).renderPage(controller::handleAction)

            is ShellPage.BranchSettings ->
                RepositoryBranchSettingsPage.schemaFor(controller.branchSettingsState).renderPage(controller::handleAction)

            is ShellPage.Collaborators ->
                RepositoryCollaboratorsSettingsPage.schemaFor(controller.collaboratorsState).renderPage(controller::handleAction)

            is ShellPage.DangerZone ->
                RepositoryDangerZonePage.schemaFor(controller.dangerZoneState).renderPage(controller::handleAction)

            is ShellPage.Rulesets ->
                RepositoryRulesetsPage.schemaFor(controller.rulesetsState).renderPage(controller::handleAction)

            is ShellPage.DeployKeys ->
                RepositoryDeployKeysPage.schemaFor(controller.deployKeysState).renderPage(controller::handleAction)

            is ShellPage.Webhooks ->
                RepositoryWebhooksPage.schemaFor(controller.webhooksState).renderPage(controller::handleAction)

            is ShellPage.ActionsSettings ->
                RepositoryActionsSettingsPage.schemaFor(controller.actionsSettingsState).renderPage(controller::handleAction)

            is ShellPage.SecurityAlertDetail ->
                RepositorySecurityAlertDetailPage.schemaFor(
                    state = controller.securityAlertState,
                    initialAlert = controller.securityAlertState.alert
                        ?: RepositorySecurityAlert(source = "", title = "", state = ""),
                ).renderPage(controller::handleAction)

            is ShellPage.SearchPage ->
                SearchPage.schemaFor(
                    state = controller.searchState,
                    query = controller.searchQuery,
                    onQueryChange = { controller.searchQuery = it },
                ).renderPage(controller::handleAction)

            ShellPage.FileManager ->
                FileManagerPage.schemaFor(controller.fileManagerState).renderPage(controller::handleAction)

            ShellPage.Account ->
                AccountPage.schemaFor(
                    authState = controller.authState,
                    rememberedAccounts = controller.rememberedAccounts,
                    isCurrentAccount = { controller.isCurrentAccount(it) },
                ).renderPage(controller::handleAction)

            is ShellPage.NotificationDetail -> {
                val notification = controller.notificationsState.notifications
                    .firstOrNull { it.id == page.notificationId }
                if (notification != null) {
                    NotificationDetailPage.schemaFor(
                        repositoryFullName = notification.repositoryFullName,
                        subjectTitle = notification.subjectTitle,
                        subjectType = notification.subjectType,
                        reason = notification.reason,
                        unread = notification.unread,
                        updatedAt = notification.updatedAt.orEmpty(),
                        htmlUrl = notification.htmlUrl.orEmpty(),
                        repositoryHtmlUrl = notification.repositoryHtmlUrl.orEmpty(),
                        latestCommentHtmlUrl = notification.latestCommentUrl.orEmpty(),
                    ).renderPage(controller::handleAction)
                } else {
                    com.Sunset.REN.GitHub.ui.schema.TextComponent(
                        id = "notification_detail.missing",
                        text = "未找到通知（id=${page.notificationId}）。",
                    ).let {
                        com.Sunset.REN.GitHub.ui.layout.PageSchema(
                            id = "notification_detail.missing",
                            columns = 12,
                            scrollable = false,
                            rows = listOf(com.Sunset.REN.GitHub.ui.layout.row(com.Sunset.REN.GitHub.ui.layout.cell(it))),
                        ).renderPage(controller::handleAction)
                    }
                }
            }

            ShellPage.Terminal ->
                TerminalPage.schemaFor(
                    state = controller.terminalState,
                    commandText = controller.terminalCommand,
                ).renderPage(controller::handleAction)

            ShellPage.WorkspaceSync ->
                WorkspaceSyncPage.schemaFor().renderPage(controller::handleAction)

            ShellPage.WorkspacePull ->
                WorkspacePullPage.schemaFor(
                    state = controller.workspacePullState,
                    fields = controller.pullFields,
                    onFieldChange = { key, value ->
                        controller.pullFields = controller.pullFields.updateField(key, value)
                    },
                    onToggleOverwrite = { controller.pullFields = controller.pullFields.copy(overwriteLocal = it) },
                ).renderPage(controller::handleAction)

            ShellPage.WorkspacePush ->
                WorkspacePushPage.schemaFor(
                    state = controller.workspaceSyncState,
                    fields = controller.pushFields,
                    onFieldChange = { key, value ->
                        controller.pushFields = controller.pushFields.updateField(key, value)
                    },
                    onToggle = { key, value ->
                        controller.pushFields = controller.pushFields.updateField(key, if (value) "true" else "false")
                    },
                ).renderPage(controller::handleAction)

            ShellPage.AppLog ->
                AppLogPage.schemaFor(
                    logText = controller.appLogText,
                    onRefresh = {
                        controller.appLogText = AppLogger.readLogText()
                        controller.appLogText
                    },
                ).renderPage(controller::handleAction)
            ShellPage.RustCore ->
                RustCorePage.schemaFor(
                    rustStatus = controller.rustCoreStatus,
                    sizeLines = controller.rustCoreSizeLines,
                    categoryLines = controller.rustCoreCategoryLines,
                    markdownHtml = controller.rustCoreMarkdownHtml,
                ).renderPage(controller::handleAction)
        }
    }
}

private fun WorkspacePullPage.Fields.updateField(key: String, value: String): WorkspacePullPage.Fields = when (key) {
    "workspaceName" -> copy(workspaceName = value)
    "owner" -> copy(owner = value)
    "repo" -> copy(repo = value)
    "branch" -> copy(branch = value)
    "remotePath" -> copy(remotePath = value)
    "localTarget" -> copy(localTarget = value)
    else -> this
}

private fun WorkspacePushPage.Fields.updateField(key: String, value: String): WorkspacePushPage.Fields = when (key) {
    "workspaceName" -> copy(workspaceName = value)
    "importPath" -> copy(importPath = value)
    "importTarget" -> copy(importTarget = value)
    "owner" -> copy(owner = value)
    "repo" -> copy(repo = value)
    "branch" -> copy(branch = value)
    "remotePath" -> copy(remotePath = value)
    "commitMessage" -> copy(commitMessage = value)
    "mirrorMode" -> copy(mirrorMode = value.toBoolean())
    "destructiveConfirmed" -> copy(destructiveConfirmed = value.toBoolean())
    "allowOverwriteRemoteChanges" -> copy(allowOverwriteRemoteChanges = value.toBoolean())
    else -> this
}