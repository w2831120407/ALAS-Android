package com.nowdex.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.nowdex.android.data.SettingsStore
import com.nowdex.android.service.QuotaForegroundService

/**
 * Nowdex 应用入口。
 */
class NowdexApplication : Application() {

    lateinit var settingsStore: SettingsStore
        private set

    override fun onCreate() {
        super.onCreate()
        settingsStore = SettingsStore(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                QuotaForegroundService.CHANNEL_ID,
                "AI 用量",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "在通知栏常驻显示各 AI 服务的额度用量"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    companion object {
        fun from(context: android.content.Context): NowdexApplication =
            context.applicationContext as NowdexApplication
    }
}
