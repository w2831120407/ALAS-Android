package com.alas.android.core.device

import com.alas.android.core.vision.Point
import com.alas.android.core.vision.Screenshot

/**
 * 触控/按键输入注入抽象(对齐 ALAS `module/device/control.py` 的 Control 与
 * MAA `ControllerAPI` 的 click/swipe/touch_down/touch_up 等动作)。
 *
 * 一套动作通过 [Input] 抽象，可替换为多种实现：
 *  - [com.alas.android.core.device.input.AdbShellInput]  ：ADB `input tap/swipe`(慢但兼容高)
 *  - [com.alas.android.core.device.input.MinitouchInput] ：minitouch 本地 socket 协议(快)
 *  - [com.alas.android.core.device.input.NativeInput]    ：设备端 InputManager 原生注入(设备端自控)
 */
interface Input {
    /** 单指点击。 */
    fun click(x: Int, y: Int)

    /** 在给定区域内的随机点点击(模拟人手)。 */
    fun click(point: Point) = click(point.x, point.y)

    /** 长按。 */
    fun longClick(x: Int, y: Int, durationMs: Int)

    /** 从 (x1,y1) 滑到 (x2,y2)，durationMs 为持续时间。 */
    fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Int = 400)

    /** 多点触控：按 contact 按下/移动/抬起。 */
    fun touchDown(contact: Int, x: Int, y: Int)
    fun touchMove(contact: Int, x: Int, y: Int)
    fun touchUp(contact: Int)

    /** 按键(键码对齐 Android KeyEvent)。 */
    fun keyEvent(keyCode: Int)
    fun keyDown(keyCode: Int)
    fun keyUp(keyCode: Int)

    /** 输入文本。 */
    fun inputText(text: String)

    /** 释放底层连接。 */
    fun close()
}

/**
 * 截图源抽象(对齐 ALAS `module/device/screenshot.py` 的 Screenshot 方法表)。
 *
 * 实现：
 *  - [com.alas.android.core.device.screencap.MediaProjectionScreencap]：设备端自控(MediaProjection)
 *  - [com.alas.android.core.device.screencap.AdbScreencap]            ：ADB `screencap`(外部/无线调试)
 */
interface Screencap {
    /** 截取一帧；失败时抛出 [ScreencapException]。 */
    fun capture(): Screenshot
    fun close()
}

/** 截图失败异常。 */
class ScreencapException(message: String, cause: Throwable? = null) : Exception(message, cause)
