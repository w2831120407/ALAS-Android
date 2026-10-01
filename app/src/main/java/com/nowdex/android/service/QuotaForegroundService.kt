package com.nowdex.android.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.nowdex.android.MainActivity
import com.nowdex.android.R
import com.nowdex.android.data.AppContainer
import com.nowdex.android.data.model.ServiceQuota
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 常驻前台服务：在通知栏显示各 AI 服务额度。
 * 这是 Nowdex Mac 菜单栏在 Android 上的等价实现。
 */
class QuotaForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification(emptyList()))
        observeQuotas()
        return START_STICKY
    }

    private fun observeQuotas() {
        serviceScope.launch {
            AppContainer.usageRepository.observeQuotas().collectLatest { quotas ->
                val notification = buildNotification(quotas)
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                manager.notify(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun buildNotification(quotas: List<ServiceQuota>): android.app.Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val summary = if (quotas.isEmpty()) {
            "加载中…"
        } else {
            quotas.take(3).joinToString("  ") { q ->
                val primary = q.windows.firstOrNull()
                val remaining = primary?.remainingPercent?.toInt() ?: 0
                "${q.service.shortName} ${remaining}%"
            }
        }

        val style = NotificationCompat.InboxStyle()
            .setSummaryText("AI 用量")
        quotas.take(6).forEach { q ->
            val primary = q.windows.firstOrNull()
            val remaining = primary?.remainingPercent?.toInt() ?: 0
            style.addLine("${q.service.displayName}  剩余 ${remaining}%")
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Nowdex")
            .setContentText(summary)
            .setSmallIcon(R.drawable.ic_notification)
            .setStyle(style)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setShowWhen(false)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val CHANNEL_ID = "nowdex_quota"
        private const val NOTIFICATION_ID = 1001
    }
}
