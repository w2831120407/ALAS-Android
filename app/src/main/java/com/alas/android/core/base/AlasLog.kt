package com.alas.android.core.base

import android.util.Log

/**
 * 轻量日志封装(对齐 ALAS `module/logger.py`)。
 * 生产可替换为 slf4j/androidx。时间戳格式对齐 ALAS 的 `%H:%M:%S.%3f`。
 */
object AlasLog {
    private const val TAG = "ALAS"
    var minLevel: Level = Level.INFO

    enum class Level(val code: Int) { TRACE(0), INFO(1), WARN(2), ERROR(3) }

    fun t(msg: String) = log(Level.TRACE, msg)
    fun i(msg: String) = log(Level.INFO, msg)
    fun w(msg: String) = log(Level.WARN, msg)
    fun e(msg: String, tr: Throwable? = null) {
        log(Level.ERROR, msg)
        tr?.let { Log.e(TAG, msg, it) }
    }

    private fun log(level: Level, msg: String) {
        if (level.code < minLevel.code) return
        val ts = String.format("%tT.%<tL", System.currentTimeMillis())
        val line = "$ts | ${level.name.padStart(5)} | $msg"
        when (level) {
            Level.TRACE, Level.INFO -> Log.i(TAG, line)
            Level.WARN -> Log.w(TAG, line)
            Level.ERROR -> Log.e(TAG, line)
        }
    }
}
