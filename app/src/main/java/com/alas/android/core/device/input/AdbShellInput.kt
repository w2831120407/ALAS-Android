package com.alas.android.core.device.input

import com.alas.android.core.device.Input
import kotlin.math.max

/**
 * ADB `shell input` 触控输入。
 *
 * 通过执行 `adb shell input tap/swipe/keyevent/text` 注入动作。
 * 兼容性最高(任何 adb 可用即可)，但速度慢，适合作为兜底方案，
 * 对齐 MAA 的 AdbShell 输入方法。
 */
class AdbShellInput(
    private val adb: AdbShellRunner,
) : Input {

    override fun click(x: Int, y: Int) {
        adb.shell("input tap $x $y")
    }

    override fun longClick(x: Int, y: Int, durationMs: Int) {
        // input swipe 到同一点并指定时长近似长按
        adb.shell("input swipe $x $y $x $y ${max(1, durationMs)}")
    }

    override fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Int) {
        adb.shell("input swipe $x1 $y1 $x2 $y2 ${max(1, durationMs)}")
    }

    override fun touchDown(contact: Int, x: Int, y: Int) {
        // ADB shell input 不区分多指；单指按下用 tap 近似
        click(x, y)
    }

    override fun touchMove(contact: Int, x: Int, y: Int) {
        // 忽略：ADB shell 无独立 move
    }

    override fun touchUp(contact: Int) {
        // 忽略：ADB shell 无独立 up
    }

    override fun keyEvent(keyCode: Int) {
        adb.shell("input keyevent $keyCode")
    }

    override fun keyDown(keyCode: Int) {
        adb.shell("input keyevent ${keyCode}_DOWN")
    }

    override fun keyUp(keyCode: Int) {
        adb.shell("input keyevent ${keyCode}_UP")
    }

    override fun inputText(text: String) {
        adb.shell("input text ${sanitize(text)}")
    }

    private fun sanitize(text: String): String =
        text.replace(" ", "%s").replace("\"", "%22").replace("'", "%27")

    override fun close() {
        // 无连接需关闭
    }
}

/** 供 ADB 命令执行的最小抽象(便于测试注入)。 */
fun interface AdbShellRunner {
    fun shell(command: String): String
}
