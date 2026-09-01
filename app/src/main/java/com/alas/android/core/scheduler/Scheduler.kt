package com.alas.android.core.scheduler

import com.alas.android.core.base.AlasLog
import com.alas.android.core.base.AlasException
import com.alas.android.core.base.GameStuckError
import com.alas.android.core.base.RequestHumanTakeover
import com.alas.android.core.config.AlasConfig
import com.alas.android.core.config.TaskConfig
import com.alas.android.core.device.DeviceController
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 任务执行单元。
 * 每个玩法模块(如 Commission/Research/Daily)实现 [run]，由 [Scheduler] 统一调度。
 * 对齐 ALAS 的玩法 Task 类(独立运行、执行后自动设置下次运行时间)。
 */
interface Task {
    val name: String
    /** 执行一次任务；抛出异常表示失败。 */
    fun run()
}

/**
 * 中央调度器(对齐 ALAS 根 `alas.py` 的 AzurLaneAutoScript.loop/get_next_task)。
 *
 * 循环逻辑：
 *  ```
 *  loop:
 *    task = config.getNextTask()        // 到期 && 启用 && 按优先级
 *    if task == null: wait & continue
 *    run(task)                          // 执行
 *    task.onSuccess() / onFailure()     // 写回 NextRun 实现"无缝续跑"
 *    失败计数达阈值 -> RequestHumanTakeover
 *  ```
 */
class Scheduler(
    private val config: AlasConfig,
    private val device: DeviceController,
    private val taskRegistry: Map<String, Task>,
) {
    private val running = AtomicBoolean(false)
    private val stopped = AtomicBoolean(false)
    private val failureCount = mutableMapOf<String, Int>()

    /** 供 UI/服务观察的状态。 */
    var currentTask: String? = null
        private set
    var statusListener: ((String) -> Unit)? = null

    fun isRunning(): Boolean = running.get()

    fun start() {
        if (!running.compareAndSet(false, true)) return
        stopped.set(false)
        Thread({ mainLoop() }, "alas-scheduler").start()
    }

    fun stop() {
        stopped.set(true)
    }

    fun join() {
        // 等待主线程退出(供服务 onDestroy 使用)
    }

    private fun mainLoop() {
        AlasLog.i("Scheduler started")
        while (!stopped.get()) {
            try {
                val now = System.currentTimeMillis() / 1000
                val task = config.getNextTask(now)
                if (task == null) {
                    // 无到期任务，休眠后重查
                    sleepUntilNextTask()
                    continue
                }
                runOne(task)
            } catch (e: RequestHumanTakeover) {
                statusListener?.invoke("需要人工接管：${e.message}")
                stop()
            } catch (e: Throwable) {
                AlasLog.e("Scheduler loop error", e)
                sleepMs(1000)
            }
        }
        running.set(false)
        AlasLog.i("Scheduler stopped")
    }

    private fun runOne(taskConfig: TaskConfig) {
        val task = taskRegistry[taskConfig.command]
        if (task == null) {
            AlasLog.w("No task implementation for command: ${taskConfig.command}")
            taskConfig.onFailure()
            config.save()
            return
        }
        currentTask = taskConfig.name
        statusListener?.invoke("开始任务: ${taskConfig.name}")
        AlasLog.i("Run task: ${taskConfig.name}")
        try {
            task.run()
            taskConfig.onSuccess()
            failureCount[taskConfig.command] = 0
            statusListener?.invoke("任务完成: ${taskConfig.name}")
        } catch (e: GameStuckError) {
            // 卡死：记失败，可能触发重启逻辑
            AlasLog.e("Task stuck: ${taskConfig.name}", e)
            recordFailure(taskConfig)
        } catch (e: AlasException) {
            AlasLog.e("Task failed: ${taskConfig.name}", e)
            recordFailure(taskConfig)
        } finally {
            currentTask = null
            config.save()
        }
    }

    private fun recordFailure(taskConfig: TaskConfig) {
        val n = (failureCount[taskConfig.command] ?: 0) + 1
        failureCount[taskConfig.command] = n
        taskConfig.onFailure()
        if (n >= MAX_CONSECUTIVE_FAILURES) {
            throw RequestHumanTakeover("连续失败 $n 次: ${taskConfig.name}")
        }
    }

    private fun sleepUntilNextTask() {
        val now = System.currentTimeMillis() / 1000
        val next = config.allTasks()
            .filter { it.enabled }
            .minOfOrNull { it.nextRun } ?: (now + IDLE_WAIT_S)
        var wait = next - now
        if (wait < 0) wait = 0
        if (wait > IDLE_WAIT_S) wait = IDLE_WAIT_S
        sleepMs((wait * 1000).toLong())
    }

    private fun sleepMs(ms: Long) {
        val end = System.currentTimeMillis() + ms
        while (System.currentTimeMillis() < end && !stopped.get()) {
            try {
                Thread.sleep(minOf(200, end - System.currentTimeMillis()))
            } catch (_: InterruptedException) {
            }
        }
    }

    companion object {
        private const val MAX_CONSECUTIVE_FAILURES = 3
        private const val IDLE_WAIT_S = 10L
    }
}
