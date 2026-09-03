package com.alas.android.core.device.input

import android.accessibilityservice.AccessibilityService
import android.graphics.Path
import android.os.SystemClock
import android.view.KeyEvent
import com.alas.android.core.device.Input
import kotlin.math.max

/**
 * 基于无障碍服务的手势注入(低权限、免 root 的设备端自控方案)。
 *
 * 通过 [AccessibilityService.dispatchGesture] 注入点击/滑动/长按。
 * 兼容性最广，但速度略慢，且需要用户在系统设置中手动开启无障碍服务。
 * 作为 [NativeInput] 的替代通道，对齐 MaaTouch 的 accessibility 方案。
 */
class AccessibilityInput(
    private val service: AccessibilityService,
) : Input {

    override fun click(x: Int, y: Int) {
        val path = Path().apply {
            moveTo(x.toFloat(), y.toFloat())
        }
        dispatchStroke(path, 50)
    }

    override fun longClick(x: Int, y: Int, durationMs: Int) {
        val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
        dispatchStroke(path, max(1, durationMs))
    }

    override fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Int) {
        val path = Path().apply {
            moveTo(x1.toFloat(), y1.toFloat())
            lineTo(x2.toFloat(), y2.toFloat())
        }
        dispatchStroke(path, max(1, durationMs))
    }

    private fun dispatchStroke(path: Path, durationMs: Int) {
        val latch = java.util.concurrent.CountDownLatch(1)
        val callback = object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: android.accessibilityservice.GestureDescription?) {
                latch.countDown()
            }

            override fun onCancelled(gestureDescription: android.accessibilityservice.GestureDescription?) {
                latch.countDown()
            }
        }
        val gesture = android.accessibilityservice.GestureDescription.Builder()
            .addStroke(android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, durationMs.toLong()))
            .build()
        service.dispatchGesture(gesture, callback, null)
        // 阻塞等待完成，保证串行执行
        try {
            latch.await(2, java.util.concurrent.TimeUnit.SECONDS)
        } catch (_: InterruptedException) {
        }
    }

    override fun touchDown(contact: Int, x: Int, y: Int) {
        // 无障碍方案不区分多指；单指按下
        val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
        dispatchStroke(path, 50)
    }

    override fun touchMove(contact: Int, x: Int, y: Int) {
        // 忽略
    }

    override fun touchUp(contact: Int) {
        // 忽略
    }

    override fun keyEvent(keyCode: Int) {
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK).let { }
        // 无 keyEvent 注入能力，跳过
        SystemClock.sleep(30)
    }

    override fun keyDown(keyCode: Int) = Unit
    override fun keyUp(keyCode: Int) = Unit

    override fun inputText(text: String) {
        // 无障碍方案无文本注入；可配合 clipboard 粘贴
    }

    override fun close() = Unit
}
