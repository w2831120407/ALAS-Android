package com.alas.android.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.alas.android.R
import com.alas.android.core.base.AlasLog
import com.alas.android.ui.MainActivity

/**
 * 调度守护前台服务：保持自动化常驻运行，并提供状态通知。
 */
class AlasForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startInForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startInForeground()
        return START_STICKY
    }

    private fun startInForeground() {
        try {
            val channelId = "alas_foreground"
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                nm.createNotificationChannel(
                    NotificationChannel(channelId, "ALAS 自动化", NotificationManager.IMPORTANCE_LOW)
                )
            }
            val pi = PendingIntent.getActivity(
                this, 0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val notification: Notification = NotificationCompat.Builder(this, channelId)
                .setContentTitle("ALAS 自动化运行中")
                .setContentText("碧蓝航线脚本正在后台调度")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pi)
                .setOngoing(true)
                .build()
            startForeground(1, notification)
        } catch (t: Throwable) {
            // POST_NOTIFICATIONS 权限未授予、前台服务类型权限缺失 等情况下也不要 crash
            AlasLog.w("startForeground skipped: ${t.message}")
        }
    }

    override fun onDestroy() {
        AlasLog.i("AlasForegroundService destroyed")
        super.onDestroy()
    }
}
