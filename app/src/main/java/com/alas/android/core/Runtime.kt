package com.alas.android.core

import android.accessibilityservice.AccessibilityService
import android.content.Context
import com.alas.android.core.base.AlasLog
import com.alas.android.core.config.AlasConfig
import com.alas.android.core.device.AdbRunner
import com.alas.android.core.device.DeviceController
import com.alas.android.core.device.screencap.AdbScreencap
import com.alas.android.core.device.screencap.MediaProjectionScreencap
import com.alas.android.core.device.input.AccessibilityInput
import com.alas.android.core.device.input.MinitouchInput
import com.alas.android.core.device.input.NativeInput
import com.alas.android.core.game.commission.CommissionTask
import com.alas.android.core.game.daily.DailyTask
import com.alas.android.core.game.research.ResearchTask
import com.alas.android.core.game.reward.RewardTask
import com.alas.android.core.scheduler.Scheduler
import com.alas.android.core.scheduler.Task
import com.alas.android.service.ScreenCaptureService

/**
 * 运行时引导(对齐 ALAS 根 `alas.py` 的 AzurLaneAutoScript)。
 *
 * 将 [AlasConfig]、[DeviceController]、[Scheduler] 与各玩法 [Task] 装配起来，
 * 并根据配置选择连接模式：
 *  - ADB / 无线调试：构建 [AdbRunner] + [AdbScreencap]
 *  - 设备端自控：MediaProjection 截图 + 原生/无障碍输入
 */
object Runtime {

    private var device: DeviceController? = null
    private var scheduler: Scheduler? = null

    fun isRunning(): Boolean = scheduler?.isRunning() == true

    /**
     * 启动调度。
     * @param context          应用上下文。
     * @param accessibilityService 已连接的无障碍服务(设备端自控低权限模式用)。
     * @param projectionHolder MediaProjection 持有者(设备端自控模式用)。
     */
    fun start(
        context: Context,
        projectionHolder: ScreenCaptureService? = null,
        accessibilityService: AccessibilityService? = null,
    ) {
        if (isRunning()) return
        appContext = context.applicationContext
        val config = AlasConfig(context)
        val dev = buildDevice(context, config, projectionHolder, accessibilityService)
        device = dev

        val tasks: Map<String, Task> = mapOf(
            "commission" to CommissionTask(dev, config.server),
            "research" to ResearchTask(dev, config.server),
            "daily" to DailyTask(dev, config.server),
            "reward" to RewardTask(dev, config.server),
        )
        scheduler = Scheduler(config, dev, tasks).also { it.start() }
    }

    fun stop() {
        scheduler?.stop()
        scheduler = null
        device?.close()
        device = null
    }

    private fun buildDevice(
        context: Context,
        config: AlasConfig,
        projectionHolder: ScreenCaptureService?,
        accessibilityService: AccessibilityService?,
    ): DeviceController {
        return when (config.deviceMode) {
            "self_control" -> buildSelfControl(projectionHolder, accessibilityService)
            else -> buildAdb(config)
        }
    }

    private fun buildSelfControl(
        projectionHolder: ScreenCaptureService?,
        accessibilityService: AccessibilityService?,
    ): DeviceController {
        val projection = projectionHolder?.getProjection()
            ?: throw IllegalStateException("MediaProjection required for self-control mode")
        // 以游戏目标分辨率 1280x720 作为识别坐标系
        val screencap = MediaProjectionScreencap(applicationContext(), projection, 1280, 720)
        val input = when {
            accessibilityService != null -> AccessibilityInput(accessibilityService)
            else -> NativeInput()
        }
        return DeviceController.selfControl(screencap, input, 1280, 720)
    }

    private fun buildAdb(config: AlasConfig): DeviceController {
        val address = config.adbAddress
        val adb = AdbRunner(serial = if (address.isEmpty()) null else address)
        val screencap = AdbScreencap(adb, 1280, 720)
        val inputKind = when (config.inputMethod) {
            "minitouch" -> DeviceController.InputKind.MINITOUCH
            else -> DeviceController.InputKind.ADB_SHELL
        }
        val minitouchHost = if (inputKind == DeviceController.InputKind.MINITOUCH) {
            config.adbAddress.substringBefore(":") ?: "127.0.0.1"
        } else null
        return DeviceController.adb(adb, screencap, inputKind, minitouchHost, 1111, 1280, 720)
    }

    @Volatile
    private var appContext: Context? = null

    private fun applicationContext(): Context {
        return appContext ?: throw IllegalStateException("Runtime not initialized with context")
    }
}
