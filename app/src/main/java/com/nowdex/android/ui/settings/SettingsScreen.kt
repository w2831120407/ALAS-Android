package com.nowdex.android.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nowdex.android.data.AppContainer
import com.nowdex.android.data.SettingsStore
import com.nowdex.android.data.ThemeMode
import com.nowdex.android.data.model.AiService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 设置页：服务管理、外观、通知、隐私。
 * 对应 Nowdex iOS 应用底部导航的"设置"。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsStore: SettingsStore,
) {
    val scope = rememberCoroutineScope()
    val themeMode by settingsStore.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
    val showNotification by settingsStore.showNotification.collectAsStateWithLifecycle(initialValue = true)
    val autoRefresh by settingsStore.autoRefresh.collectAsStateWithLifecycle(initialValue = true)
    val enabledServices by AppContainer.usageRepository.observeEnabledServices()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // 服务管理
            item {
                SettingsSection(title = "服务") {
                    AiService.entries.forEach { service ->
                        val enabled = service in enabledServices
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            com.nowdex.android.ui.components.ServiceIcon(service = service, size = 24)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = service.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                            Switch(
                                checked = enabled,
                                onCheckedChange = { checked ->
                                    scope.launch {
                                        val current = AppContainer.usageRepository.observeEnabledServices().first()
                                        val updated = if (checked) {
                                            current + service
                                        } else {
                                            current - service
                                        }
                                        AppContainer.usageRepository.setEnabledServices(updated)
                                    }
                                },
                            )
                        }
                    }
                }
            }

            // 外观
            item {
                SettingsSection(title = "外观") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("主题", modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeChip("跟随系统", themeMode == ThemeMode.SYSTEM) {
                            scope.launch { settingsStore.setThemeMode(ThemeMode.SYSTEM) }
                        }
                        ThemeChip("浅色", themeMode == ThemeMode.LIGHT) {
                            scope.launch { settingsStore.setThemeMode(ThemeMode.LIGHT) }
                        }
                        ThemeChip("深色", themeMode == ThemeMode.DARK) {
                            scope.launch { settingsStore.setThemeMode(ThemeMode.DARK) }
                        }
                    }
                }
            }

            // 通知
            item {
                SettingsSection(title = "通知") {
                    SwitchRow(
                        title = "常驻通知",
                        subtitle = "在通知栏显示额度，类似 Mac 菜单栏",
                        checked = showNotification,
                        onCheckedChange = { scope.launch { settingsStore.setShowNotification(it) } },
                    )
                    Spacer(Modifier.height(8.dp))
                    SwitchRow(
                        title = "自动刷新",
                        subtitle = "定时更新各服务额度",
                        checked = autoRefresh,
                        onCheckedChange = { scope.launch { settingsStore.setAutoRefresh(it) } },
                    )
                }
            }

            // 隐私
            item {
                SettingsSection(title = "隐私") {
                    Text(
                        text = "• 凭证保存在设备本地（加密存储）\n" +
                            "• 由设备直接向各服务请求用量\n" +
                            "• Nowdex 不在自己的服务器上保存密码或用量\n" +
                            "• 无需注册 Nowdex 账号",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp,
                    )
                }
            }

            item {
                Text(
                    text = "Nowdex Android · v0.1.0\n灵感来自 CodexBar",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ThemeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}
