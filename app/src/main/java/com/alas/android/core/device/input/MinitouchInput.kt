package com.alas.android.core.device.input

import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import kotlin.math.max

/**
 * Minitouch 协议客户端。
 *
 * Minitouch 是常见的 Android 触控注入方案(ALAS 与 MAA 均支持)，通过一个本地
 * TCP socket 与设备上的 minitouch 守护进程通信，协议为简单的文本行：
 *
 *  ```
 *  v <version>          # 握手
 *  d <contact> <x> <y>  # 按下
 *  m <contact> <x> <y>  # 移动
 *  u <contact>          # 抬起
 *  c                    # 提交一组动作
 *  r                    # 重启
 *  ```
 *
 * 坐标空间为 maxX/maxY(通常为短边 32767 或 65535，见握手返回)。
 */
class MinitouchInput(
    private val host: String,
    private val port: Int,
    private val maxX: Int,
    private val maxY: Int,
) : com.alas.android.core.device.Input {

    private var socket: Socket? = null
    private var out: DataOutputStream? = null
    private var version = 0

    fun connect() {
        val s = Socket(host, port)
        socket = s
        val reader = DataInputStream(s.getInputStream())
        out = DataOutputStream(s.getOutputStream())

        // minitouch 启动后先输出 "v <version> <max-contacts> <max-x> <max-y>"
        val header = readLine(reader) ?: throw IllegalStateException("minitouch handshake failed")
        val parts = header.split(" ")
        version = parts[1].toIntOrNull() ?: 1
        // 若握手未给出 maxX/maxY(版本不同)，则用构造参数。
        if (parts.size >= 4) {
            val mx = parts[2].toIntOrNull() ?: maxX
            val my = parts[3].toIntOrNull() ?: maxY
            @Suppress("UNUSED_VARIABLE")
            val _ignored = mx
            @Suppress("UNUSED_VARIABLE")
            val _ignored2 = my
        }
    }

    private fun readLine(r: DataInputStream): String? {
        val buf = StringBuilder()
        while (true) {
            val b = r.read()
            if (b == -1) return if (buf.isEmpty()) null else buf.toString()
            if (b == '\n'.code) return buf.toString()
            buf.append(b.toChar())
        }
    }

    private fun scaleX(x: Int): Int = ((x.toLong() * maxX) / 1280).toInt().coerceIn(0, maxX)
    private fun scaleY(y: Int): Int = ((y.toLong() * maxY) / 720).toInt().coerceIn(0, maxY)

    private fun send(cmd: String) {
        val o = out ?: throw IllegalStateException("minitouch not connected")
        o.write((cmd + "\n").toByteArray(Charsets.US_ASCII))
        o.flush()
    }

    override fun touchDown(contact: Int, x: Int, y: Int) {
        send("d $contact ${scaleX(x)} ${scaleY(y)}")
    }

    override fun touchMove(contact: Int, x: Int, y: Int) {
        send("m $contact ${scaleX(x)} ${scaleY(y)}")
    }

    override fun touchUp(contact: Int) {
        send("u $contact")
        send("c") // 提交
    }

    override fun click(x: Int, y: Int) {
        touchDown(0, x, y)
        send("c")
        Thread.sleep(50) // 按下短暂停留，避免连点被吞
        touchUp(0)
    }

    override fun longClick(x: Int, y: Int, durationMs: Int) {
        touchDown(0, x, y)
        send("c")
        Thread.sleep(max(1, durationMs).toLong())
        touchUp(0)
    }

    override fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Int) {
        val steps = max(8, durationMs / 20)
        touchDown(0, x1, y1)
        send("c")
        for (i in 1..steps) {
            val fx = x1 + (x2 - x1) * i / steps
            val fy = y1 + (y2 - y1) * i / steps
            touchMove(0, fx, fy)
            send("c")
            Thread.sleep(20)
        }
        touchUp(0)
    }

    override fun keyEvent(keyCode: Int) = throw UnsupportedOperationException("minitouch has no key input")

    override fun keyDown(keyCode: Int) = throw UnsupportedOperationException("minitouch has no key input")

    override fun keyUp(keyCode: Int) = throw UnsupportedOperationException("minitouch has no key input")

    override fun inputText(text: String) = throw UnsupportedOperationException("minitouch has no text input")

    override fun close() {
        try {
            out?.write("r\n".toByteArray())
            out?.flush()
        } catch (_: Exception) {
        }
        try {
            socket?.close()
        } catch (_: Exception) {
        }
        socket = null
        out = null
    }
}
