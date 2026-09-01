package com.alas.android.core.device.input

import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.MotionEvent
import com.alas.android.core.device.Input
import kotlin.math.max

/**
 * 设备端原生触控注入(对齐 MAA `MaaTouch` 的 Controller.java 思路)。
 *
 * 通过反射调用系统服务 `InputManager.injectInputEvent(...)`(INJECT_MODE_ASYNC)，
 * 向系统注入 [MotionEvent]/[KeyEvent]，从而在设备本地直接控制游戏，
 * 无需连接电脑。这是"设备端自控"模式的核心输入实现。
 *
 * 注意：`injectInputEvent` 为系统隐藏 API，需要系统签名/root 权限或
 * 通过无障碍服务替代。此处同时提供两条路径：
 *  - [NativeInput]：反射注入(高权限，真机自控)
 *  - [AccessibilityInput]：通过无障碍服务 dispatchGesture(低权限，通用)
 */
class NativeInput : Input {

    private var inputManager: Any? = null
    private var injectMethod: java.lang.reflect.Method? = null

    private val pointersState = PointersState()

    init {
        try {
            val smClass = Class.forName("android.os.ServiceManager")
            val getService = smClass.getMethod("getService", String::class.java)
            val binder = getService.invoke(null, "input")
            inputManager = Class.forName("android.hardware.input.IInputManager\$Stub")
                .getMethod("asInterface", android.os.IBinder::class.java)
                .invoke(null, binder)
            injectMethod = inputManager!!.javaClass.getMethod(
                "injectInputEvent",
                android.view.InputEvent::class.java,
                Int::class.javaPrimitiveType,
            )
        } catch (e: Throwable) {
            throw IllegalStateException("无法获取 InputManager 注入接口(可能需要 root/系统签名)", e)
        }
    }

    private fun inject(event: android.view.InputEvent) {
        val m = injectMethod ?: return
        m.invoke(inputManager, event, INJECT_MODE_ASYNC)
        event.recycle()
    }

    override fun click(x: Int, y: Int) {
        val now = SystemClock.uptimeMillis()
        val down = buildMotion(now, now, x, y, MotionEvent.ACTION_DOWN, 0)
        inject(down)
        Thread.sleep(CLICK_DELAY_MS)
        val up = buildMotion(now, now + CLICK_DELAY_MS, x, y, MotionEvent.ACTION_UP, 0)
        inject(up)
    }

    override fun longClick(x: Int, y: Int, durationMs: Int) {
        val now = SystemClock.uptimeMillis()
        inject(buildMotion(now, now, x, y, MotionEvent.ACTION_DOWN, 0))
        Thread.sleep(max(1, durationMs).toLong())
        inject(buildMotion(now, now + durationMs, x, y, MotionEvent.ACTION_UP, 0))
    }

    override fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Int) {
        val steps = max(8, durationMs / 20)
        val now = SystemClock.uptimeMillis()
        inject(buildMotion(now, now, x1, y1, MotionEvent.ACTION_DOWN, 0))
        for (i in 1..steps) {
            val t = now + (durationMs.toLong() * i / steps)
            val fx = x1 + (x2 - x1) * i / steps
            val fy = y1 + (y2 - y1) * i / steps
            inject(buildMotion(now, t, fx, fy, MotionEvent.ACTION_MOVE, 0))
            Thread.sleep(10)
        }
        inject(buildMotion(now, now + durationMs, x2, y2, MotionEvent.ACTION_UP, 0))
    }

    override fun touchDown(contact: Int, x: Int, y: Int) {
        pointersState.down(contact, x, y)
        inject(buildMultiTouch(contact, MotionEvent.ACTION_POINTER_DOWN))
    }

    override fun touchMove(contact: Int, x: Int, y: Int) {
        pointersState.move(contact, x, y)
        inject(buildMultiTouch(contact, MotionEvent.ACTION_MOVE))
    }

    override fun touchUp(contact: Int) {
        inject(buildMultiTouch(contact, MotionEvent.ACTION_POINTER_UP))
        pointersState.up(contact)
    }

    override fun keyEvent(keyCode: Int) {
        val now = SystemClock.uptimeMillis()
        inject(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0))
        inject(KeyEvent(now, now + 20, KeyEvent.ACTION_UP, keyCode, 0))
    }

    override fun keyDown(keyCode: Int) {
        inject(KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), KeyEvent.ACTION_DOWN, keyCode, 0))
    }

    override fun keyUp(keyCode: Int) {
        inject(KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, keyCode, 0))
    }

    override fun inputText(text: String) {
        val kcm = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD)
        val chars = text.toCharArray()
        val events = kcm.getEvents(chars)
        for (e in events) inject(e)
    }

    private fun buildMotion(downTime: Long, eventTime: Long, x: Int, y: Int, action: Int, contact: Int): MotionEvent {
        val pointerProps = arrayOf(pointerProperty(contact))
        val pointerCoords = arrayOf(pointerCoord(x.toFloat(), y.toFloat(), 1f))
        return MotionEvent.obtain(
            downTime, eventTime, action, 1, pointerProps, pointerCoords,
            0, 0, 1f, 1f, 0, 0,
            InputDevice.SOURCE_TOUCHSCREEN, 0,
        )
    }

    private fun buildMultiTouch(contact: Int, action: Int): MotionEvent {
        val now = SystemClock.uptimeMillis()
        val coords = pointersState.coords()
        val props = pointersState.props()
        return MotionEvent.obtain(
            0, now, action, coords.size, props, coords,
            0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0,
        )
    }

    private fun pointerProperty(contact: Int) = android.view.PointerProperties().apply {
        id = contact
        toolType = android.view.MotionEvent.TOOL_TYPE_FINGER
    }

    private fun pointerCoord(x: Float, y: Float, pressure: Float) = android.view.PointerCoords().apply {
        this.x = x
        this.y = y
        this.pressure = pressure
    }

    override fun close() {
        inputManager = null
        injectMethod = null
    }

    companion object {
        private const val INJECT_MODE_ASYNC = 0
        private const val CLICK_DELAY_MS = 50L
    }
}

/**
 * 多点触控状态跟踪(对齐 MaaTouch 的 PointersState/Pointer)。
 */
internal class PointersState {
    private data class Pointer(val id: Int, var x: Int, var y: Int, var down: Boolean = false)

    private val pointers = mutableMapOf<Int, Pointer>()

    fun down(contact: Int, x: Int, y: Int) {
        pointers[contact] = Pointer(contact, x, y, true)
    }

    fun move(contact: Int, x: Int, y: Int) {
        pointers[contact]?.let { it.x = x; it.y = y }
    }

    fun up(contact: Int) {
        pointers.remove(contact)
    }

    fun coords(): Array<android.view.PointerCoords> {
        val list = pointers.values.sortedBy { it.id }.map { p ->
            android.view.PointerCoords().apply {
                x = p.x.toFloat()
                y = p.y.toFloat()
                pressure = 1f
            }
        }
        return list.toTypedArray()
    }

    fun props(): Array<android.view.PointerProperties> {
        val list = pointers.values.sortedBy { it.id }.map { p ->
            android.view.PointerProperties().apply {
                id = p.id
                toolType = android.view.MotionEvent.TOOL_TYPE_FINGER
            }
        }
        return list.toTypedArray()
    }
}
