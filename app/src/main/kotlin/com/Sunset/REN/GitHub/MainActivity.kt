package com.Sunset.REN.GitHub

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.Sunset.REN.GitHub.data.local.ThemePreferenceStore
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubTheme
import com.Sunset.REN.GitHub.ui.shell.ShellHost
import com.Sunset.REN.GitHub.ui.shell.ShellHostController
import com.Sunset.REN.GitHub.util.AppLogger

/**
 * 应用入口（步骤 6：新壳运行时）。
 *
 * 旧壳（activity_main.xml + NavHostFragment + BottomNavigationView 补丁系统）已移除，
 * 运行时壳 = Compose AppShell（三区硬约束）+ 页面 schema（一致组件模块化坐标构建法）。
 * 页面切换、导航栏渲染、inset 消费全部由 [ShellHostController] / AppShell 承担，
 * Activity 不再持有任何视图接线。
 */
class MainActivity : AppCompatActivity() {

    private lateinit var themePreferenceStore: ThemePreferenceStore
    private lateinit var shellHost: ShellHostController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.initialize(applicationContext)
        AppLogger.i(TAG, "main activity created (shell v2)")
        themePreferenceStore = ThemePreferenceStore(applicationContext)
        configureSystemBars()

        shellHost = ShellHostController(this)
        setContent {
            SunsetGitHubTheme {
                ShellHost(shellHost)
            }
        }
    }

    override fun onDestroy() {
        if (::shellHost.isInitialized) {
            shellHost.dispose()
        }
        super.onDestroy()
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

    private companion object {
        const val TAG = "MainActivity"
    }
}