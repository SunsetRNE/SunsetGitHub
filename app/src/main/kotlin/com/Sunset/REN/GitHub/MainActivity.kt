package com.Sunset.REN.GitHub

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.Sunset.REN.GitHub.data.local.ThemePreferenceStore
import com.Sunset.REN.GitHub.data.local.RepositoryNavigationPreferencesRepository
import com.Sunset.REN.GitHub.databinding.ActivityMainBinding
import com.Sunset.REN.GitHub.ui.account.AccountFragment
import com.Sunset.REN.GitHub.ui.dashboard.DashboardFragment
import com.Sunset.REN.GitHub.ui.debug.UiDebugConfig
import com.Sunset.REN.GitHub.ui.debug.UiDebugOverlay
import com.Sunset.REN.GitHub.ui.debug.UiRenderSnapshot
import com.Sunset.REN.GitHub.ui.filemanager.LocalFileManagerFragment
import com.Sunset.REN.GitHub.ui.filemanager.LocalFilePreviewFragment
import com.Sunset.REN.GitHub.ui.navigation.MaterialBottomNavigationBarSurface
import com.Sunset.REN.GitHub.ui.navigation.NavigationBarHostController
import com.Sunset.REN.GitHub.ui.profile.ProfileFragment
import com.Sunset.REN.GitHub.ui.repo.RepositoryActionRunDetailFragment
import com.Sunset.REN.GitHub.ui.repo.RepositoryActionsFragment
import com.Sunset.REN.GitHub.ui.repo.RepositoryCreateFragment
import com.Sunset.REN.GitHub.ui.repo.RepositoryIssueCreateFragment
import com.Sunset.REN.GitHub.ui.repo.RepositoryMoreSectionsBottomSheet
import com.Sunset.REN.GitHub.ui.repo.RepositorySection
import com.Sunset.REN.GitHub.ui.repo.RepositorySectionNavigationHost
import com.Sunset.REN.GitHub.util.AppLogger

class MainActivity : AppCompatActivity(),
    AccountFragment.AuthStateListener,
    RepositorySectionNavigationHost,
    RepositoryMoreSectionsBottomSheet.Host {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navController: NavController
    private lateinit var themePreferenceStore: ThemePreferenceStore
    private lateinit var repositoryNavigationPreferencesRepository: RepositoryNavigationPreferencesRepository
    private lateinit var navigationBarHostController: NavigationBarHostController
    private lateinit var uiDebugConfig: UiDebugConfig
    private var uiDebugOverlay: UiDebugOverlay? = null
    private var latestStatusBarInsets = Rect()
    private var latestNavigationBarInsets = Rect()
    private var isAuthorized = false
    private var isFloatingNavigationEnabled = false
    private var lastUiLayoutLogText = ""
    private val themePreferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == ThemePreferenceStore.KeyFloatingNavigationEnabled) {
            isFloatingNavigationEnabled = themePreferenceStore.isFloatingNavigationEnabled()
            navigationBarHostController.updatePreferences(isAuthorized, isFloatingNavigationEnabled)
            navigationBarHostController.renderCurrentDestination()
        }
    }
    private val uiDebugPreferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == UiDebugConfig.KeyOverlayEnabled) {
            applyUiDebugOverlayPreference()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.initialize(applicationContext)
        AppLogger.i(TAG, "main activity created")
        themePreferenceStore = ThemePreferenceStore(applicationContext)
        uiDebugConfig = UiDebugConfig(applicationContext)
        repositoryNavigationPreferencesRepository = RepositoryNavigationPreferencesRepository(applicationContext)
        isFloatingNavigationEnabled = themePreferenceStore.isFloatingNavigationEnabled()
        themePreferenceStore.registerListener(themePreferenceListener)
        uiDebugConfig.registerListener(uiDebugPreferenceListener)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        attachUiDebugLayoutTracking()
        applyUiDebugOverlayPreference()
        configureSystemBars()
        applyTopSystemBarInsets()
        setSupportActionBar(binding.topAppBar)

        val navigationBarSurface = MaterialBottomNavigationBarSurface(
            container = binding.navViewContainer,
            navView = binding.navView,
            divider = binding.navViewDivider,
            navHostViewProvider = { findViewById(R.id.nav_host_fragment_activity_main) },
            resources = resources,
            getTitle = { titleResId -> getString(titleResId) }
        )
        navigationBarHostController = NavigationBarHostController(
            surface = navigationBarSurface,
            repositoryNavigationPreferencesRepository = repositoryNavigationPreferencesRepository,
            showRepositoryMoreSections = ::showRepositoryMoreSectionsSheet,
            logTag = TAG
        )
        navigationBarHostController.updatePreferences(isAuthorized, isFloatingNavigationEnabled)
        navigationBarHostController.updateSystemNavigationBottomInset(latestNavigationBarInsets.bottom)

        navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_login,
                R.id.navigation_home,
                R.id.navigation_dashboard,
                R.id.navigation_notifications,
                R.id.navigation_settings
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navigationBarHostController.attach(navController)
        navController.addOnDestinationChangedListener { _, destination, arguments ->
            navigationBarHostController.onDestinationChanged(destination.id, arguments)
            applyAppChromeForDestination(destination.id, arguments)
            updateUiDebugOverlay()
            recordUiLayoutSnapshot()
            invalidateOptionsMenu()
        }
        navigationBarHostController.renderCurrentDestination(
            navController.currentBackStackEntry?.arguments
        )
        applyAppChromeForDestination(
            navController.currentDestination?.id,
            navController.currentBackStackEntry?.arguments
        )
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }


    override fun onAuthStateChanged(isAuthorized: Boolean) {
        if (!isAuthorized && this.isAuthorized && navController.currentDestination?.id != R.id.navigation_login) {
            navController.navigate(R.id.navigation_login)
        }
        this.isAuthorized = isAuthorized
        navigationBarHostController.updatePreferences(isAuthorized, isFloatingNavigationEnabled)
        navigationBarHostController.renderCurrentDestination()
        invalidateOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.top_app_bar_menu, menu)
        updateProfileActionVisibility(menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        updateProfileActionVisibility(menu)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_open_profile -> {
                openProfilePage()
                true
            }

            // No global toolbar entry for the local file manager.

            R.id.action_refresh_repositories -> {
                refreshDashboardRepositories()
                true
            }

            R.id.action_profile_open_github -> {
                findProfileFragment()?.openProfileInGitHubFromToolbar()
                true
            }

            R.id.action_profile_refresh -> {
                findProfileFragment()?.refreshProfileFromToolbar()
                true
            }

            R.id.action_create_repository -> {
                openRepositoryCreatePage()
                true
            }

            R.id.action_submit_issue -> {
                findRepositoryIssueCreateFragment()?.submitFromToolbar()
                true
            }

            R.id.action_repository_workflows -> {
                findRepositoryActionsFragment()?.showWorkflowDrawerFromToolbar()
                true
            }

            R.id.action_repository_action_run_developer_info -> {
                openActionRunDeveloperInfoPage()
                true
            }

            R.id.action_open_file_manager -> {
                openLocalFileManagerPage()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateProfileActionVisibility(menu: Menu) {
        val isHomeDestination = navController.currentDestination?.id == R.id.navigation_home
        menu.findItem(R.id.action_open_profile)?.isVisible = isHomeDestination
        menu.findItem(R.id.action_open_file_manager)?.let { item ->
            item.isVisible = isHomeDestination
            item.actionView?.findViewById<View>(R.id.button_open_file_manager_action)?.setOnClickListener {
                openLocalFileManagerPage()
            }
        }

        menu.findItem(R.id.action_profile_open_github)?.isVisible =
            navController.currentDestination?.id == R.id.navigation_profile
        menu.findItem(R.id.action_profile_refresh)?.isVisible =
            navController.currentDestination?.id == R.id.navigation_profile
        menu.findItem(R.id.action_refresh_repositories)?.isVisible =
            navController.currentDestination?.id == R.id.navigation_dashboard
        menu.findItem(R.id.action_create_repository)?.isVisible =
            navController.currentDestination?.id == R.id.navigation_dashboard
        menu.findItem(R.id.action_repository_workflows)?.isVisible =
            navController.currentDestination?.id == R.id.repository_actions_fragment
        menu.findItem(R.id.action_repository_action_run_developer_info)?.isVisible =
            navController.currentDestination?.id == R.id.repository_action_run_detail_fragment
        menu.findItem(R.id.action_submit_issue)?.let { item ->
            val isCreateIssuePage = navController.currentDestination?.id == R.id.repository_issue_create_fragment
            val issueCreateFragment = findRepositoryIssueCreateFragment()
            item.isVisible = isCreateIssuePage
            item.isEnabled = issueCreateFragment?.isSubmittingIssue() != true
            item.actionView?.findViewById<TextView>(R.id.text_submit_issue_action)?.apply {
                text = if (issueCreateFragment?.isSubmittingIssue() == true) {
                    getString(R.string.repository_issues_create_submitting)
                } else {
                    getString(R.string.repository_issues_create_submit)
                }
                alpha = if (issueCreateFragment?.isSubmittingIssue() == true) 0.7f else 1f
                setOnClickListener {
                    if (item.isEnabled) {
                        findRepositoryIssueCreateFragment()?.submitFromToolbar()
                    }
                }
            }
        }
    }

    /**
     * 顶栏刷新动作：在当前 NavHost 里找到正在展示的 [DashboardFragment] 并触发强制刷新。
     * 找不到时静默忽略（理论上仅在 dashboard 目的地时菜单项才可见）。
     */
    private fun refreshDashboardRepositories() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main)
        val dashboardFragment = navHostFragment
            ?.childFragmentManager
            ?.fragments
            ?.firstOrNull { fragment -> fragment is DashboardFragment } as? DashboardFragment
        dashboardFragment?.refreshRepositoriesFromToolbar()
    }

    private fun findProfileFragment(): ProfileFragment? {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main)
        return navHostFragment
            ?.childFragmentManager
            ?.fragments
            ?.firstOrNull { fragment -> fragment is ProfileFragment } as? ProfileFragment
    }

    private fun findRepositoryIssueCreateFragment(): RepositoryIssueCreateFragment? {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main)
        return navHostFragment
            ?.childFragmentManager
            ?.fragments
            ?.firstOrNull { fragment -> fragment is RepositoryIssueCreateFragment } as? RepositoryIssueCreateFragment
    }

    private fun findRepositoryActionsFragment(): RepositoryActionsFragment? {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main)
        return navHostFragment
            ?.childFragmentManager
            ?.fragments
            ?.firstOrNull { fragment -> fragment is RepositoryActionsFragment } as? RepositoryActionsFragment
    }

    private fun openActionRunDeveloperInfoPage() {
        if (navController.currentDestination?.id != R.id.repository_action_run_detail_fragment) return
        val arguments = navController.currentBackStackEntry?.arguments ?: return
        navController.navigate(
            R.id.repository_action_run_developer_info_fragment,
            bundleOf(
                RepositoryActionRunDetailFragment.ARG_OWNER to arguments.getString(RepositoryActionRunDetailFragment.ARG_OWNER).orEmpty(),
                RepositoryActionRunDetailFragment.ARG_REPO to arguments.getString(RepositoryActionRunDetailFragment.ARG_REPO).orEmpty(),
                RepositoryActionRunDetailFragment.ARG_RUN_ID to (arguments.getLong(RepositoryActionRunDetailFragment.ARG_RUN_ID))
            )
        )
    }

    private fun openRepositoryCreatePage() {
        if (navController.currentDestination?.id != R.id.navigation_dashboard) return
        navController.navigate(R.id.repository_create_fragment)
    }

    private fun openProfilePage() {
        if (navController.currentDestination?.id != R.id.navigation_profile) {
            navController.navigate(R.id.navigation_profile)
        }
    }

    private fun openLocalFileManagerPage() {
        if (navController.currentDestination?.id == R.id.local_file_manager_fragment) return
        navController.navigate(
            R.id.local_file_manager_fragment,
            bundleOf(LocalFileManagerFragment.ARG_MODE to LocalFileManagerFragment.MODE_MANAGE)
        )
    }

    private fun applyAppChromeForDestination(destinationId: Int?, arguments: Bundle? = null) {
        val isFullScreenFileManager = destinationId == R.id.local_file_manager_fragment
        binding.topAppBar.visibility = if (isFullScreenFileManager) View.GONE else View.VISIBLE
        supportActionBar?.let { actionBar ->
            if (isFullScreenFileManager) actionBar.hide() else actionBar.show()
        }
        if (isFullScreenFileManager) return
        applyRepositoryTitleForDestination(destinationId, arguments)
    }
    private fun applyRepositoryTitleForDestination(destinationId: Int?, arguments: Bundle? = null) {
        if (destinationId == R.id.navigation_login) {
            binding.topAppBar.title = getString(R.string.auth_login_page_title)
            supportActionBar?.title = getString(R.string.auth_login_page_title)
            supportActionBar?.subtitle = null
            binding.topAppBar.subtitle = null
            return
        }
        if (destinationId == R.id.navigation_home) {
            binding.topAppBar.title = getString(R.string.title_home)
            supportActionBar?.title = getString(R.string.title_home)
            supportActionBar?.subtitle = null
            binding.topAppBar.subtitle = null
            return
        }
        if (destinationId == R.id.account_fragment) {
            binding.topAppBar.title = getString(R.string.title_account)
            supportActionBar?.title = getString(R.string.title_account)
            supportActionBar?.subtitle = null
            binding.topAppBar.subtitle = null
            return
        }
        if (destinationId == R.id.repository_create_fragment) {
            binding.topAppBar.title = getString(R.string.title_repository_create)
            supportActionBar?.title = getString(R.string.title_repository_create)
            supportActionBar?.subtitle = null
            binding.topAppBar.subtitle = null
            return
        }
        if (destinationId != R.id.repository_detail_fragment) return
        val fullName = arguments?.getString("full_name").orEmpty()
        val title = if (fullName.isNotBlank()) fullName else getString(R.string.title_repository_detail)
        binding.topAppBar.title = title
        supportActionBar?.title = title
        supportActionBar?.subtitle = null
        binding.topAppBar.subtitle = null
    }

    private fun applyUiDebugOverlayPreference() {
        if (uiDebugConfig.isOverlayEnabled()) {
            if (uiDebugOverlay == null) {
                val contentRoot = findViewById<ViewGroup>(android.R.id.content)
                uiDebugOverlay = UiDebugOverlay(contentRoot, this).also { overlay ->
                    overlay.attach()
                }
            }
            updateUiDebugOverlay()
        } else {
            uiDebugOverlay?.detach()
            uiDebugOverlay = null
        }
    }

    private fun updateUiDebugOverlay() {
        val overlay = uiDebugOverlay ?: return
        overlay.updateSnapshot(buildUiRenderSnapshot())
    }

    private fun recordUiLayoutSnapshot() {
        val snapshot = buildUiRenderSnapshot()
        val logText = snapshot.logText()
        if (logText == lastUiLayoutLogText) return
        lastUiLayoutLogText = logText
        AppLogger.d("UiLayout", logText)
    }

    private fun attachUiDebugLayoutTracking() {
        val listener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateUiDebugOverlay()
            recordUiLayoutSnapshot()
        }
        binding.root.addOnLayoutChangeListener(listener)
        binding.navViewContainer.addOnLayoutChangeListener(listener)
        findViewById<View>(R.id.nav_host_fragment_activity_main)?.addOnLayoutChangeListener(listener)
    }

    private fun buildUiRenderSnapshot(): UiRenderSnapshot {
        val metrics = resources.displayMetrics
        val configuration = resources.configuration
        val destination = if (::navController.isInitialized) navController.currentDestination else null
        val navHostView = findViewById<View>(R.id.nav_host_fragment_activity_main)
        val navContainer = binding.navViewContainer
        return UiRenderSnapshot(
            destinationIdName = destination?.id?.let(::resourceEntryName).orEmpty().ifBlank { "initializing" },
            destinationLabel = destination?.label?.toString().orEmpty(),
            fragmentClassName = currentPrimaryFragmentClassName(),
            screenWidthPx = metrics.widthPixels,
            screenHeightPx = metrics.heightPixels,
            density = metrics.density,
            fontScale = configuration.fontScale,
            isNightMode = UiRenderSnapshot.isNightMode(configuration),
            statusBarInsets = Rect(latestStatusBarInsets),
            navigationBarInsets = Rect(latestNavigationBarInsets),
            rootHeightPx = binding.root.height,
            navHostTopPx = navHostView?.top ?: 0,
            navHostBottomPx = navHostView?.bottom ?: 0,
            navHostHeightPx = navHostView?.height ?: 0,
            navContainerTopPx = navContainer.top,
            navContainerBottomPx = navContainer.bottom,
            navContainerHeightPx = navContainer.height,
            navContainerVisibility = visibilityName(navContainer.visibility)
        )
    }

    private fun visibilityName(visibility: Int): String {
        return when (visibility) {
            View.VISIBLE -> "VISIBLE"
            View.INVISIBLE -> "INVISIBLE"
            View.GONE -> "GONE"
            else -> visibility.toString()
        }
    }

    private fun currentPrimaryFragmentClassName(): String {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main)
        return navHostFragment
            ?.childFragmentManager
            ?.fragments
            ?.firstOrNull { fragment -> fragment.isVisible }
            ?.javaClass
            ?.name
            ?: "unknown"
    }

    private fun resourceEntryName(resourceId: Int): String {
        return runCatching { resources.getResourceEntryName(resourceId) }
            .getOrDefault(resourceId.toString())
    }


    private fun applyTopSystemBarInsets() {
        val topAppBar = binding.topAppBar
        val initialHeight = topAppBar.layoutParams.height
        val initialPaddingLeft = topAppBar.paddingLeft
        val initialPaddingTop = topAppBar.paddingTop
        val initialPaddingRight = topAppBar.paddingRight
        val initialPaddingBottom = topAppBar.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(topAppBar) { view, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navigationBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            latestStatusBarInsets = Rect(
                statusBarInsets.left,
                statusBarInsets.top,
                statusBarInsets.right,
                statusBarInsets.bottom
            )
            latestNavigationBarInsets = Rect(
                navigationBarInsets.left,
                navigationBarInsets.top,
                navigationBarInsets.right,
                navigationBarInsets.bottom
            )
            if (::navigationBarHostController.isInitialized) {
                navigationBarHostController.updateSystemNavigationBottomInset(navigationBarInsets.bottom)
            }
            val statusBarTop = statusBarInsets.top
            view.setPadding(
                initialPaddingLeft,
                initialPaddingTop + statusBarTop,
                initialPaddingRight,
                initialPaddingBottom
            )
            view.layoutParams = view.layoutParams.apply {
                height = initialHeight + statusBarTop
            }
            updateUiDebugOverlay()
            insets
        }
        ViewCompat.requestApplyInsets(topAppBar)
    }

    private fun configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }

    override fun showRepositorySectionNavigation(
        sections: List<RepositorySection>,
        selectedSection: RepositorySection,
        onSectionSelected: (RepositorySection) -> Unit
    ) {
        navigationBarHostController.showRepositorySectionNavigation(sections, selectedSection, onSectionSelected)
    }

    private fun showRepositoryMoreSectionsSheet() {
        if (supportFragmentManager.isStateSaved) return
        if (supportFragmentManager.findFragmentByTag(RepositoryMoreSectionsBottomSheet.FRAGMENT_TAG) != null) return
        RepositoryMoreSectionsBottomSheet.newInstance()
            .show(supportFragmentManager, RepositoryMoreSectionsBottomSheet.FRAGMENT_TAG)
    }

    override fun currentShortcutSections(): List<RepositorySection> {
        return navigationBarHostController.currentShortcutSections()
    }

    override fun currentSectionOrder(): List<RepositorySection> {
        return navigationBarHostController.currentSectionOrder()
    }

    override fun isSectionSupportedInApp(section: RepositorySection): Boolean {
        return section != RepositorySection.More
    }

    override fun onSectionChosen(section: RepositorySection) {
        navigationBarHostController.chooseRepositorySection(section)
    }

    override fun onSectionPinned(section: RepositorySection): Boolean {
        return navigationBarHostController.pinRepositorySection(section)
    }

    override fun onSectionUnpinned(section: RepositorySection): Boolean {
        return navigationBarHostController.unpinRepositorySection(section)
    }

    override fun onSectionOrderChanged(sections: List<RepositorySection>): Boolean {
        return navigationBarHostController.reorderRepositorySections(sections)
    }

    override fun updateRepositorySectionSelection(selectedSection: RepositorySection) {
        navigationBarHostController.updateRepositorySectionSelection(selectedSection)
    }

    override fun clearRepositorySectionNavigation() {
        navigationBarHostController.clearRepositorySectionNavigation()
    }

    override fun onDestroy() {
        themePreferenceStore.unregisterListener(themePreferenceListener)
        super.onDestroy()
    }

    override fun onSupportNavigateUp(): Boolean {
        if (navController.currentDestination?.id == R.id.repository_create_fragment) {
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment_activity_main)
            val createFragment = navHostFragment
                ?.childFragmentManager
                ?.fragments
                ?.firstOrNull { fragment -> fragment is RepositoryCreateFragment } as? RepositoryCreateFragment
            if (createFragment?.requestNavigateUp() == true) return true
        }
        if (navController.currentDestination?.id == R.id.local_file_preview_fragment) {
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment_activity_main)
            val previewFragment = navHostFragment
                ?.childFragmentManager
                ?.fragments
                ?.firstOrNull { fragment -> fragment is LocalFilePreviewFragment } as? LocalFilePreviewFragment
            if (previewFragment?.requestNavigateUp() == true) return true
        }
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private companion object {
        const val TAG = "MainActivity"
    }
}