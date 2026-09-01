package com.alas.android.service

import android.app.Activity
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder

/**
 * MediaProjection 前台服务：持有截图授权结果，供
 * [com.alas.android.core.device.screencap.MediaProjectionScreencap] 使用。
 *
 * 使用流程：
 *  1. UI 发起 [MediaProjectionManager.createScreenCaptureIntent] 授权；
 *  2. onActivityResult 拿到 data 后，构造本服务并传入，startForeground；
 *  3. 取出 [projection] 构建设备端自控的 DeviceController。
 */
class ScreenCaptureService : Service() {

    private var projection: MediaProjection? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        val data = intent?.getParcelableExtra<Intent>(EXTRA_DATA)
        val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        if (resultCode == Activity.RESULT_OK && data != null) {
            projection = mpm.getMediaProjection(resultCode, data)
            // 启动前台以维持 MediaProjection
            startForegroundCompat()
        }
        return START_NOT_STICKY
    }

    private fun startForegroundCompat() {
        // 简化：使用最小通知。正式实现应构建 Notification。
        val channelId = "alas_projection"
        val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                android.app.NotificationChannel(channelId, "屏幕捕获", android.app.NotificationManager.IMPORTANCE_LOW)
            )
        }
        val notification = android.app.Notification.Builder(this, channelId)
            .setContentTitle("ALAS 屏幕捕获")
            .setContentText("设备端自控运行中")
            .build()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            startForeground(2, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(2, notification)
        }
    }

    fun getProjection(): MediaProjection? = projection

    override fun onDestroy() {
        projection?.stop()
        projection = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_DATA = "data"

        fun buildIntent(context: Context, resultCode: Int, data: Intent): Intent =
            Intent(context, ScreenCaptureService::class.java)
                .putExtra(EXTRA_RESULT_CODE, resultCode)
                .putExtra(EXTRA_DATA, data)
    }
}
