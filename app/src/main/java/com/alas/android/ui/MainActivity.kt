package com.alas.android.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.alas.android.core.Runtime
import com.alas.android.core.config.AlasConfig
import com.alas.android.core.update.AppUpdater
import com.alas.android.core.update.AssetSyncManager
import com.alas.android.service.AlasForegroundService
import com.alas.android.service.ScreenCaptureService

/**
 * 主界面：设备连接设置 + 启动/停止调度 + 数据同步/应用更新。
 */
class MainActivity : ComponentActivity() {

    private var connectedProjectionService: ScreenCaptureService? = null
    private val assetSync = AssetSyncManager(this)
    private val appUpdater = AppUpdater()
    private val projectionConn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            connectedProjectionService = service as? ScreenCaptureService
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            connectedProjectionService = null
        }
    }

    // POST_NOTIFICATIONS 运行时权限请求器 (API 33+)
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* 无论是否授予都继续 */ }

    // MediaProjection / 其他 Activity 结果统一在这里处理 (替代废弃的 onActivityResult)
    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            startProjectionService(result.resultCode, result.data!!)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
        } catch (t: Throwable) {
            // enableEdgeToEdge 在极个别 SDK 级别的设备上有兼容性问题，跳过即可，不至于闪退
        }
        // Android 13+ 前台服务/通知必须先取得 POST_NOTIFICATIONS 运行时权限，
        // 否则 startForeground 会抛出 SecurityException 直接闪退。
        ensureNotificationPermission()

        try {
            setContent {
                MaterialTheme {
                    MainScreen(
                        onStartClick = { safeRun(::startAutomation) },
                        onStopClick = { safeRun(::stopAutomation) },
                        onSyncClick = { safeRun(::syncData) },
                        onCheckUpdateClick = { safeRun(::checkUpdate) },
                    )
                }
            }
        } catch (t: Throwable) {
            // Compose 主题/资源缺失也不能闪退——降级到一个基础 Toast UI
            Toast.makeText(this, "UI 初始化失败: ${t.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun ensureNotificationPermission() {
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /** 把按钮回调包一层 try/catch：任何业务异常都 toast 出来而不是闪退。 */
    private inline fun safeRun(block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            Toast.makeText(this, "操作失败: ${t.message}", Toast.LENGTH_LONG).show()
        }
    }

    /** 从 ALAS 上游同步玩法/地图/模板数据。 */
    private fun syncData() {
        Thread({
            try {
                val config = AlasConfig(this)
                val changed = assetSync.sync(listOf(config.server))
                runOnUiThread {
                    Toast.makeText(
                        this,
                        if (changed) "数据同步完成" else "数据已是最新",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "同步失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }, "alas-sync").start()
    }

    /** 检查应用新版本。 */
    private fun checkUpdate() {
        Thread({
            try {
                val release = appUpdater.latestRelease()
                val hasUpdate = appUpdater.hasUpdate()
                runOnUiThread {
                    val msg = if (release == null) {
                        "暂无 Release，无法检查更新"
                    } else if (hasUpdate) {
                        "发现新版本 ${release.tagName}\n${release.notes.take(80)}"
                    } else {
                        "已是最新版本 (${release.tagName})"
                    }
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "检查更新失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }, "alas-update").start()
    }

    private fun startAutomation() {
        val config = AlasConfig(this)
        if (config.deviceMode == "self_control") {
            requestProjection()
        } else {
            Runtime.start(this)
        }
    }

    /** 设备端自控需先请求 MediaProjection 授权。 */
    private fun requestProjection() {
        val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projectionLauncher.launch(mpm.createScreenCaptureIntent())
    }

    private fun startProjectionService(resultCode: Int, data: Intent) {
        val intent = ScreenCaptureService.buildIntent(this, resultCode, data)
        try {
            startForegroundService(intent)
            bindService(intent, projectionConn, Context.BIND_AUTO_CREATE)
        } catch (t: Throwable) {
            Toast.makeText(this, "截图服务启动失败: ${t.message}", Toast.LENGTH_LONG).show()
            return
        }
        Runtime.start(this, projectionHolder = connectedProjectionService)
        try {
            startForegroundService(Intent(this, AlasForegroundService::class.java))
        } catch (t: Throwable) {
            Toast.makeText(this, "前台服务启动失败: ${t.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun stopAutomation() {
        Runtime.stop()
        try {
            unbindService(projectionConn)
        } catch (_: Throwable) { /* 未绑定时忽略 */ }
        Toast.makeText(this, "已停止", Toast.LENGTH_SHORT).show()
    }

    companion object {
        @Suppress("unused")
        private const val REQ_PROJECTION = 100
    }
}

@Composable
private fun MainScreen(
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onSyncClick: () -> Unit,
    onCheckUpdateClick: () -> Unit,
) {
    var running by remember { mutableStateOf(Runtime.isRunning()) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("ALAS 碧蓝航线自动化", style = MaterialTheme.typography.headlineSmall)
        Text("基于 MAA Android 技术方案构建", style = MaterialTheme.typography.bodyMedium)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("连接模式", style = MaterialTheme.typography.titleMedium)
                Text("ADB / 无线调试 / 设备端自控")
            }
        }

        Button(onClick = onStartClick, enabled = !running, modifier = Modifier.fillMaxWidth()) {
            Text("启动自动化")
        }
        OutlinedButton(onClick = onStopClick, enabled = running, modifier = Modifier.fillMaxWidth()) {
            Text("停止")
        }
        if (running) {
            Text("状态：运行中", color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("数据与更新", style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = onSyncClick, modifier = Modifier.fillMaxWidth()) {
            Text("同步 ALAS 上游数据(模板/地图)")
        }
        OutlinedButton(onClick = onCheckUpdateClick, modifier = Modifier.fillMaxWidth()) {
            Text("检查应用更新")
        }
    }
}
