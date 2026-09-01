package com.alas.android.core.device

import com.alas.android.core.device.input.AdbShellInput
import com.alas.android.core.device.input.MinitouchInput
import com.alas.android.core.device.input.NativeInput
import com.alas.android.core.device.screencap.AdbScreencap
import com.alas.android.core.device.screencap.MediaProjectionScreencap
import com.alas.android.core.vision.Point
import com.alas.android.core.vision.Screenshot

/**
 * 设备控制器(对齐 ALAS `module/device/device.py` 的 Device 与
 * MAA `ControllerAPI`)。组合 [Screencap] 与 [Input] 提供统一的高层动作。
 *
 * 支持三种连接模式(见 [ConnectionMode])：
 *  - SELF_CONTROL(设备端自控)：MediaProjection 截图 + Native/Accessibility 输入
 *  - ADB(有线或无线调试)：AdbScreencap + AdbShellInput/MinitouchInput
 */
class DeviceController private constructor(
    private val screencap: Screencap,
    private val input: Input,
    private val screenWidth: Int,
    private val screenHeight: Int,
) {

    /** 单次截图(带节流，避免过快拉帧)。 */
    @Volatile
    private var lastCaptureAt = 0L

    val resolution: Pair<Int, Int> get() = screenWidth to screenHeight

    /** 截取一帧用于识别。 */
    fun screenshot(): Screenshot {
        throttle()
        return screencap.capture()
    }

    /** 仅在指定区域出现指定模板时点击。返回是否点击成功。 */
    fun appearAndClick(roi: com.alas.android.core.vision.Roi, template: com.alas.android.core.vision.TemplateImage): Boolean {
        val shot = screenshot()
        return try {
            val hit = template.matchResult(shot.mat, 0.85, roi)
            if (hit != null && hit.point != null) {
                val btn = com.alas.android.core.vision.Button.fromMatch(hit.point!!, template.size)
                click(btn.randomPoint())
                true
            } else false
        } finally {
            shot.release()
        }
    }

    fun click(p: Point) = input.click(p.x, p.y)
    fun click(x: Int, y: Int) = input.click(x, y)
    fun longClick(x: Int, y: Int, ms: Int) = input.longClick(x, y, ms)
    fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, ms: Int = 400) = input.swipe(x1, y1, x2, y2, ms)
    fun keyEvent(keyCode: Int) = input.keyEvent(keyCode)
    fun inputText(text: String) = input.inputText(text)

    fun close() {
        screencap.close()
        input.close()
    }

    private fun throttle() {
        val now = System.currentTimeMillis()
        val diff = now - lastCaptureAt
        if (diff < SCREENSHOT_INTERVAL_MS) {
            try {
                Thread.sleep(SCREENSHOT_INTERVAL_MS - diff)
            } catch (_: InterruptedException) {
            }
        }
        lastCaptureAt = System.currentTimeMillis()
    }

    companion object {
        private const val SCREENSHOT_INTERVAL_MS = 100L

        /** 连接模式。 */
        enum class ConnectionMode { SELF_CONTROL, ADB }

        /** 输入实现选择。 */
        enum class InputKind { ADB_SHELL, MINITOUCH, NATIVE, ACCESSIBILITY }

        /** 设备端自控模式构造(MediaProjection 截图 + 原生输入)。 */
        fun selfControl(
            screencap: MediaProjectionScreencap,
            input: Input,
            width: Int,
            height: Int,
        ): DeviceController = DeviceController(screencap, input, width, height)

        /**
         * ADB 模式构造(无线调试/外部控制)。
         * @param adbShellRunner  执行 adb shell 的可调用对象。
         * @param adbScreencap    截图源。
         * @param inputKind       输入实现。
         */
        fun adb(
            adbShellRunner: AdbShellRunner,
            screencap: Screencap,
            inputKind: InputKind = InputKind.ADB_SHELL,
            minitouchHost: String? = null,
            minitouchPort: Int = 1111,
            width: Int = 1280,
            height: Int = 720,
        ): DeviceController {
            val input: Input = when (inputKind) {
                InputKind.ADB_SHELL -> AdbShellInput(adbShellRunner)
                InputKind.MINITOUCH -> MinitouchInput(
                    minitouchHost ?: throw IllegalArgumentException("minitouchHost required"),
                    minitouchPort, 1280, 720,
                ).also { it.connect() }
                InputKind.NATIVE -> NativeInput()
                InputKind.ACCESSIBILITY -> throw IllegalArgumentException("accessibility requires a service instance")
            }
            return DeviceController(screencap, input, width, height)
        }
    }
}
