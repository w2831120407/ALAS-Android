package com.alas.android.core.config

import android.content.Context
import com.alas.android.core.base.AlasLog
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * 配置管理器(对齐 ALAS `module/config/config.py` 的 AzurLaneConfig)。
 *
 * 负责加载/保存 `config.json`(任务调度配置 + 服务器设置)，并按优先级排序任务。
 * 持久化到 `files/config.json`。
 */
class AlasConfig(private val context: Context) {

    private val file: File = File(context.filesDir, "config.json")
    private val tasks = ConcurrentHashMap<String, TaskConfig>()

    /** 服务器：cn/en/jp/tw。 */
    var server: String = "cn"
        private set

    /** 设备控制相关设置。 */
    var deviceMode: String = "adb" // "adb" | "self_control"
    var adbAddress: String = ""
    var inputMethod: String = "adb_shell" // adb_shell | minitouch | native | accessibility

    init {
        load()
    }

    fun load() {
        if (!file.exists()) {
            createDefault()
            return
        }
        try {
            val root = JSONObject(file.readText())
            server = root.optString("server", "cn")
            deviceMode = root.optString("device_mode", "adb")
            adbAddress = root.optString("adb_address", "")
            inputMethod = root.optString("input_method", "adb_shell")
            val taskObj = root.optJSONObject("tasks") ?: JSONObject()
            tasks.clear()
            taskObj.keys().forEach { key ->
                tasks[key] = TaskConfig.fromJson(key, taskObj.getJSONObject(key))
            }
        } catch (e: Exception) {
            AlasLog.e("Failed to load config, using default", e)
            createDefault()
        }
    }

    private fun createDefault() {
        server = "cn"
        deviceMode = "adb"
        adbAddress = ""
        inputMethod = "adb_shell"
        tasks.clear()
        defaultTasks().forEach { tasks[it.name] = it }
        save()
    }

    fun save() {
        try {
            val root = JSONObject().apply {
                put("server", server)
                put("device_mode", deviceMode)
                put("adb_address", adbAddress)
                put("input_method", inputMethod)
                put("tasks", JSONObject().also { t ->
                    tasks.values.forEach { t.put(it.name, it.toJson()) }
                })
            }
            file.writeText(root.toString(2))
        } catch (e: Exception) {
            AlasLog.e("Failed to save config", e)
        }
    }

    fun getTask(name: String): TaskConfig? = tasks[name]
    fun setTask(config: TaskConfig) {
        tasks[config.name] = config
    }

    /**
     * 返回应运行的任务(按 next_run 到期 && enabled)，按 priority 升序。
     * 对齐 ALAS get_next_task()。
     */
    fun getNextTask(now: Long = System.currentTimeMillis() / 1000): TaskConfig? {
        val due = tasks.values
            .filter { it.enabled && it.nextRun <= now }
            .sortedBy { it.priority }
        return due.firstOrNull()
    }

    fun allTasks(): List<TaskConfig> = tasks.values.sortedBy { it.priority }

    companion object {
        /** 默认任务定义(可运行任务的骨架)。 */
        fun defaultTasks(): List<TaskConfig> = listOf(
            TaskConfig("Restart", false, "restart", 0, 60, 0, 0, false),
            TaskConfig("Commission", true, "commission", 900, 300, 30, 0, true),
            TaskConfig("Research", true, "research", 1800, 300, 40, 0, true),
            TaskConfig("Daily", true, "daily", 3600, 600, 20, 0, true),
            TaskConfig("Reward", true, "reward", 600, 120, 10, 0, true),
        )
    }
}
