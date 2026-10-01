package com.nowdex.android.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "nowdex_settings")

/** 主题模式 */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * 设置存储：外观、通知、隐私。
 * 凭证（各服务的 Cookie / API Key）应使用 EncryptedSharedPreferences，
 * 此处仅保存非敏感偏好。
 */
class SettingsStore(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SHOW_NOTIFICATION = booleanPreferencesKey("show_notification")
        val AUTO_REFRESH = booleanPreferencesKey("auto_refresh")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        runCatching { ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name) }
            .getOrDefault(ThemeMode.SYSTEM)
    }

    val showNotification: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SHOW_NOTIFICATION] ?: true
    }

    val autoRefresh: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.AUTO_REFRESH] ?: true
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setShowNotification(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_NOTIFICATION] = enabled }
    }

    suspend fun setAutoRefresh(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_REFRESH] = enabled }
    }
}
