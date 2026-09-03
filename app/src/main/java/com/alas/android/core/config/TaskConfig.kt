package com.alas.android.core.config

import org.json.JSONObject

/**
 * 单个任务的调度配置(对齐 ALAS `module/config/config.py` 中每个任务节点的
 * `Scheduler.Enable / Command / NextRun / SuccessInterval / FailureInterval` 等)。
 *
 * 通过 JSON 定义，示例：
 * ```
 * {
 *   "Research": {
 *     "enabled": true,
 *     "command": "research",
 *     "success_interval": 1800,
 *     "failure_interval": 300,
 *     "priority": 70,
 *     "next_run": 0,
 *     "server_update": true
 *   }
 * }
 * ```
 */
data class TaskConfig(
    val name: String,
    val enabled: Boolean,
    val command: String,
    /** 成功后到下次运行间隔(秒)。 */
    val successInterval: Long,
    /** 失败后到下次运行间隔(秒)。 */
    val failureInterval: Long,
    /** 调度优先级，越小越优先(对齐 ALAS SCHEDULER_PRIORITY)。 */
    val priority: Int,
    /** 下次运行时间戳(epoch 秒)。 */
    var nextRun: Long,
    /** 是否服务器更新后立即运行。 */
    val serverUpdate: Boolean,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("enabled", enabled)
        put("command", command)
        put("success_interval", successInterval)
        put("failure_interval", failureInterval)
        put("priority", priority)
        put("next_run", nextRun)
        put("server_update", serverUpdate)
    }

    /** 任务成功后推迟 nextRun。 */
    fun onSuccess(now: Long = System.currentTimeMillis() / 1000) {
        nextRun = now + successInterval
    }

    /** 任务失败后推迟 nextRun。 */
    fun onFailure(now: Long = System.currentTimeMillis() / 1000) {
        nextRun = now + failureInterval
    }

    companion object {
        fun fromJson(name: String, obj: JSONObject): TaskConfig {
            return TaskConfig(
                name = name,
                enabled = obj.optBoolean("enabled", false),
                command = obj.optString("command", name.lowercase()),
                successInterval = obj.optLong("success_interval", 600),
                failureInterval = obj.optLong("failure_interval", 120),
                priority = obj.optInt("priority", 100),
                nextRun = obj.optLong("next_run", 0),
                serverUpdate = obj.optBoolean("server_update", false),
            )
        }
    }
}
