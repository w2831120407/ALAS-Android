package com.alas.android.ui

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alas.android.core.Runtime
import com.alas.android.core.config.AlasConfig
import com.alas.android.service.AlasForegroundService
import com.alas.android.service.ScreenCaptureService

/**
 * 主界面：设备连接设置 + 启动/停止调度。
 */
class MainActivity : ComponentActivity() {

    private var connectedProjectionService: ScreenCaptureService? = null
    private val projectionConn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            connectedProjectionService = service as? ScreenCaptureService
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            connectedProjectionService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                MainScreen(onStartClick = { startAutomation() }, onStopClick = { stopAutomation() })
            }
        }
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
        startActivityForResult(mpm.createScreenCaptureIntent(), REQ_PROJECTION)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_PROJECTION && resultCode == Activity.RESULT_OK && data != null) {
            startProjectionService(resultCode, data)
        }
    }

    private fun startProjectionService(resultCode: Int, data: Intent) {
        val intent = ScreenCaptureService.buildIntent(this, resultCode, data)
        startForegroundService(intent)
        bindService(intent, projectionConn, BIND_AUTO_CREATE)
        Runtime.start(this, projectionHolder = connectedProjectionService)
        startForegroundService(Intent(this, AlasForegroundService::class.java))
    }

    private fun stopAutomation() {
        Runtime.stop()
        Toast.makeText(this, "已停止", Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val REQ_PROJECTION = 100
    }
}

@Composable
private fun MainScreen(
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
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
    }
}
