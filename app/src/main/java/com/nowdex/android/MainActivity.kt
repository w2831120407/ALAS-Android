package com.nowdex.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nowdex.android.data.ThemeMode
import com.nowdex.android.service.QuotaForegroundService
import com.nowdex.android.ui.settings.SettingsScreen
import com.nowdex.android.ui.status.StatusScreen
import com.nowdex.android.ui.theme.NowdexTheme
import com.nowdex.android.ui.usage.UsageScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = NowdexApplication.from(this)
            val themeMode by app.settingsStore.themeMode.collectAsStateWithLifecycle(
                initialValue = runBlocking { app.settingsStore.themeMode.first() }
            )
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            NowdexTheme(darkTheme = darkTheme) {
                NowdexApp(settingsStore = app.settingsStore)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // 启动时根据设置决定是否启通知服务
        val app = NowdexApplication.from(this)
        val showNotification = runBlocking { app.settingsStore.showNotification.first() }
        if (showNotification) {
            startService(Intent(this, QuotaForegroundService::class.java))
        }
    }
}

private enum class Screen(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    Status("status", "状态", Icons.Outlined.Analytics, Icons.Filled.BarChart),
    Usage("usage", "用量", Icons.Outlined.BarChart, Icons.Filled.BarChart),
    Settings("settings", "设置", Icons.Outlined.Settings, Icons.Filled.Settings),
}

@Composable
private fun NowdexApp(settingsStore: com.nowdex.android.data.SettingsStore) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                Screen.entries.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                if (selected) screen.selectedIcon else screen.icon,
                                contentDescription = screen.label,
                            )
                        },
                        label = { Text(screen.label) },
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Status.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Screen.Status.route) { StatusScreen() }
            composable(Screen.Usage.route) { UsageScreen() }
            composable(Screen.Settings.route) { SettingsScreen(settingsStore = settingsStore) }
        }
    }
}
