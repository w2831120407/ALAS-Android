package com.alas.android.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.alas.android.core.base.AlasLog
import com.alas.android.core.device.input.AccessibilityInput

/**
 * 无障碍服务：作为"设备端自控"的低权限输入通道。
 * 用户需在系统设置中手动开启本服务，随后 [AccessibilityInput] 可通过
 * [dispatchGesture] 注入点击/滑动。
 */
class AlasAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        AlasLog.i("Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 不处理窗口内容，仅提供手势注入能力
    }

    override fun onInterrupt() = Unit
}
