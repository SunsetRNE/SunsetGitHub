package com.Sunset.REN.GitHub.ui.shell

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import com.Sunset.REN.GitHub.data.auth.AuthSessionRepository
import com.Sunset.REN.GitHub.ui.auth.LoginHomeUiState
import com.Sunset.REN.GitHub.ui.auth.LoginHomeViewModel
import com.Sunset.REN.GitHub.ui.dashboard.DashboardViewModel
import com.Sunset.REN.GitHub.ui.notifications.NotificationsUiState
import com.Sunset.REN.GitHub.ui.notifications.NotificationsViewModel
import com.Sunset.REN.GitHub.ui.pages.DashboardPage
import com.Sunset.REN.GitHub.ui.pages.DeviceFlowIntroPage
import com.Sunset.REN.GitHub.ui.pages.HomePage
import com.Sunset.REN.GitHub.ui.pages.LoginHomePage
import com.Sunset.REN.GitHub.ui.pages.NotificationsPage
import com.Sunset.REN.GitHub.ui.pages.ProfilePage
import com.Sunset.REN.GitHub.ui.pages.SettingsPage
import com.Sunset.REN.GitHub.ui.pages.TokenLoginChoicePage
import com.Sunset.REN.GitHub.ui.profile.ProfileUiState
import com.Sunset.REN.GitHub.ui.profile.ProfileViewModel
import com.Sunset.REN.GitHub.ui.render.renderPage
import com.Sunset.REN.GitHub.ui.repo.RepositoriesUiState
import com.Sunset.REN.GitHub.ui.repo.RepositorySection
import com.Sunset.REN.GitHub.ui.settings.SettingsViewModel
import com.Sunset.REN.GitHub.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 壳页路由（步骤 6：删旧壳后的运行时页面枚举）。
 *
 * 每个值对应一个已迁移的页面对象；本轮先接通壳骨架页
 * （登录 + 五个主导航），其余页面对象在后续轮次逐个接入。
 */
sealed interface ShellPage {
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

    /** 设备码登录引导页。 */
    data object DeviceFlowIntro : ShellPage

    /** Token 登录方式选择页。 */
    data object TokenLoginChoice : ShellPage
}

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
 * 壳宿主控制器（步骤 6 运行时核心）。
 *
 * - Activity-scoped，持有 5 个主导航页 + 登录页的 ViewModel（Activity 作用域，
 *   与原 Fragment-scoped 等价但跨页面存活）；
 * - LiveData → Compose 状态桥接：observe(activity) 写回 [mutableStateOf]；
 * - 唯一 action 路由：页面组件与壳导航都经 [handleAction] 分发；
 * - 页面切换 = [ShellPage] 状态 + [ShellState] 派生，无任何补丁式视图操作。
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

    private val authSessionRepository = AuthSessionRepository(activity.applicationContext)
    private val backStack = ArrayDeque<ShellPage>()

    init {
        loginViewModel.state.observe(activity) { state ->
            loginUiState = state
            if (state.shouldEnterHome) {
                navigateTo(ShellPage.Home)
            }
        }
        dashboardViewModel.repositoriesState.observe(activity) { state ->
            repositoriesState = state
        }
        notificationsViewModel.notificationsState.observe(activity) { state ->
            notificationsState = state
        }
        profileViewModel.profileState.observe(activity) { state ->
            profileState = state
        }
        settingsViewModel.isFloatingNavigationEnabled.observe(activity) { value ->
            settingsFlags = settingsFlags.copy(floatingNavigationEnabled = value)
        }
        settingsViewModel.isSoraEditorEnabled.observe(activity) { value ->
            settingsFlags = settingsFlags.copy(soraEditorEnabled = value)
        }
        settingsViewModel.isUiDebugOverlayEnabled.observe(activity) { value ->
            settingsFlags = settingsFlags.copy(uiDebugOverlayEnabled = value)
        }
        settingsViewModel.defaultSectionOrder.observe(activity) { value ->
            settingsFlags = settingsFlags.copy(sectionOrder = value)
        }

        // 启动数据流
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
        shellState = when (page) {
            ShellPage.Login -> LoginHomePage.shellState()
            ShellPage.Home -> HomePage.shellState()
            ShellPage.Dashboard -> DashboardPage.shellState()
            ShellPage.Notifications -> NotificationsPage.shellState()
            ShellPage.Profile -> ProfilePage.shellState()
            ShellPage.Settings -> SettingsPage.shellState()
            ShellPage.DeviceFlowIntro -> DeviceFlowIntroPage.shellState()
            ShellPage.TokenLoginChoice -> TokenLoginChoicePage.shellState()
        }
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

    // ---- action 路由 ----

    fun handleAction(action: String) {
        when {
            action == "shell.back" -> navigateBack()
            action == "nav.home" -> navigateTo(ShellPage.Home)
            action == "nav.dashboard" -> navigateTo(ShellPage.Dashboard)
            action == "nav.notifications" -> navigateTo(ShellPage.Notifications)
            action == "nav.profile" -> navigateTo(ShellPage.Profile)
            action == "nav.settings" -> push(ShellPage.Settings)
            action == "login.device_flow" -> push(ShellPage.DeviceFlowIntro)
            action == "login.token_login" -> push(ShellPage.TokenLoginChoice)
            action.startsWith("settings.toggle.") -> handleSettingsToggle(action)
            action.startsWith("settings.") -> handleSettingsEntry(action)
            action == "dashboard.load_more" -> dashboardViewModel.loadMoreRepositories()
            action.startsWith("dashboard.pin.") -> dashboardViewModel.togglePinned(action.removePrefix("dashboard.pin."))
            action.startsWith("dashboard.star.") -> dashboardViewModel.toggleFavorite(action.removePrefix("dashboard.star."))
            action == "dashboard.sort" -> AppLogger.w(TAG, "dashboard.sort not bridged yet")
            action.startsWith("repo.open.") -> AppLogger.w(TAG, "repo.open not bridged yet: $action")
            action.startsWith("notifications.filter.all") -> notificationsViewModel.switchAll(true)
            action.startsWith("notifications.filter.unread") -> notificationsViewModel.switchAll(false)
            action == "notifications.load_more" -> notificationsViewModel.loadNextPage()
            action.startsWith("notifications.open.") -> AppLogger.w(TAG, "notification.open not bridged yet: $action")
            action == "profile.open_github" -> AppLogger.w(TAG, "profile.open_github not bridged yet")
            action.startsWith("profile.repo.open.") -> AppLogger.w(TAG, "profile.repo.open not bridged yet: $action")
            action.startsWith("device_flow.") -> AppLogger.w(TAG, "device_flow action not bridged yet: $action")
            action.startsWith("token.") -> AppLogger.w(TAG, "token action not bridged yet: $action")
            else -> AppLogger.w(TAG, "unhandled shell action: $action")
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

    private fun handleSettingsEntry(action: String) {
        // 次级页（Account/AppLog/Sync/Terminal）与分区排序将在后续轮次桥接。
        AppLogger.w(TAG, "settings entry not bridged yet: $action")
    }

    private companion object {
        const val TAG = "ShellHost"
    }
}

/**
 * 新壳宿主 Composable：唯一内容区入口。
 * MainActivity 挂载本函数后，运行时壳 = AppShell（三区硬约束）+ 页面 schema。
 */
@Composable
fun ShellHost(controller: ShellHostController) {
    AppShell(state = controller.shellState, onAction = controller::handleAction) {
        when (controller.currentPage) {
            ShellPage.Login ->
                LoginHomePage.schemaFor(controller.loginUiState.message)
                    .renderPage(controller::handleAction)

            ShellPage.Home ->
                HomePage.schema.renderPage(controller::handleAction)

            ShellPage.Dashboard ->
                DashboardPage.schemaFor(controller.repositoriesState)
                    .renderPage(controller::handleAction)

            ShellPage.Notifications ->
                NotificationsPage.schemaFor(controller.notificationsState)
                    .renderPage(controller::handleAction)

            ShellPage.Profile ->
                ProfilePage.schemaFor(controller.profileState)
                    .renderPage(controller::handleAction)

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

            ShellPage.TokenLoginChoice ->
                TokenLoginChoicePage.schemaFor().renderPage(controller::handleAction)
        }
    }
}
