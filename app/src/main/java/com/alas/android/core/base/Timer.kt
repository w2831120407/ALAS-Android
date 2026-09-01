package com.alas.android.core.base

import kotlin.math.abs

/**
 * 定时器(对齐 ALAS `module/base/timer.py`)。
 * 用于等待类逻辑：记录起点，判断距起点是否已过 [interval] 秒。
 */
class Timer(private var interval: Double = 0.0) {
    private var start: Long = 0

    init {
        reset()
    }

    /** 重置起点为当前时间。 */
    fun reset() {
        start = System.currentTimeMillis()
    }

    /** 是否已超过 interval 秒。 */
    fun reached(): Boolean {
        return elapsed() >= interval
    }

    /** 距起点已过去的秒数。 */
    fun elapsed(): Double {
        return (System.currentTimeMillis() - start) / 1000.0
    }

    /** 剩余等待秒数(<=0 表示已到)。 */
    fun remain(): Double {
        return interval - elapsed()
    }

    fun setInterval(interval: Double): Timer {
        this.interval = interval
        return this
    }

    /** 对齐 ALAS 的 `timer.wait()`：阻塞直到到达 interval。 */
    fun wait() {
        val remainMs = (remain() * 1000).toLong()
        if (remainMs > 0) {
            try {
                Thread.sleep(remainMs)
            } catch (_: InterruptedException) {
            }
        }
    }

    companion object {
        fun start(interval: Double): Timer = Timer(interval)
    }
}

/**
 * 等待至图像条件满足/消失的工具(对齐 ALAS ModuleBase.wait_until_* 语义)。
 */
class Waiter(
    private val pollIntervalMs: Long = 500,
    private val timeoutMs: Long = 30_000,
) {
    /** 持续调用 [condition] 直到返回 true 或超时。返回是否成功。 */
    fun until(condition: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return true
            try {
                Thread.sleep(pollIntervalMs)
            } catch (_: InterruptedException) {
            }
        }
        return false
    }

    /** 等待条件满足，不满足则抛出 [GameStuckError]。 */
    fun require(what: String, condition: () -> Boolean) {
        if (!until(condition)) {
            throw GameStuckError("timeout waiting for $what")
        }
    }
}
