package com.alas.android.core.device

import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * ADB 命令执行器。
 *
 * 支持"无线调试"与"外部 ADB"两种场景：
 *  - 无线调试：通过 `adb connect <ip>:<port>` 连接本机或远程设备后执行命令；
 *  - 外部控制：ADB 指向目标模拟器/设备。
 *
 * 封装 [AdbShellRunner] 与 [AdbScreencapRunner] 所需的两类调用。
 */
class AdbRunner(
    /** adb 可执行文件路径，留空则用 PATH 中的 adb。 */
    private val adbPath: String = "adb",
    /** 目标设备序列号或 `ip:port`，留空表示唯一设备。 */
    private val serial: String? = null,
    private val timeoutMs: Long = 10_000,
) : AdbShellRunner, com.alas.android.core.device.screencap.AdbScreencapRunner {

    private val baseArgs: List<String> = buildList {
        add(adbPath)
        if (serial != null) {
            add("-s")
            add(serial)
        }
    }

    /** 连接远程(无线调试)设备。 */
    fun connect(address: String): String = exec(listOf("adb", "connect", address))

    /** 断开连接。 */
    fun disconnect(address: String): String = exec(listOf("adb", "disconnect", address))

    override fun shell(command: String): String =
        exec(baseArgs + listOf("shell", command))

    override fun execOut(command: String): ByteArray {
        val cmd = baseArgs + listOf("exec-out", command)
        val process = ProcessBuilder(cmd).redirectErrorStream(false).start()
        val out = process.inputStream.use { it.readBytes() }
        if (!process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) {
            process.destroyForcibly()
        }
        return out
    }

    private fun exec(cmd: List<String>): String {
        val process = ProcessBuilder(cmd).redirectErrorStream(true).start()
        val text = StringBuilder()
        BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            var line = reader.readLine()
            while (line != null) {
                text.append(line).append('\n')
                line = reader.readLine()
            }
        }
        if (!process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) {
            process.destroyForcibly()
        }
        return text.toString().trim()
    }
}
