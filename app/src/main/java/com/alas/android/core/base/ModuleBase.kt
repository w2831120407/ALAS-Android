package com.alas.android.core.base

import com.alas.android.core.device.DeviceController
import com.alas.android.core.vision.Button
import com.alas.android.core.vision.Roi
import com.alas.android.core.vision.Screenshot
import com.alas.android.core.vision.TemplateImage

/**
 * 玩法模块基类(对齐 ALAS `module/base/base.py` 的 ModuleBase)。
 *
 * 提供面向游戏界面的高频原语：
 *  - [appear]：判断按钮/模板是否出现在当前屏幕
 *  - [appearThenClick]：出现即点击
 *  - [waitUntilAppear]/[waitUntilDisappear]：等待状态
 *  - [ensureClick]：等待并点击关键按钮
 *  - [loop]：命令式状态机主循环
 *
 * 与 ALAS 相同，控制流是命令式状态机(while 循环 + appear 分支)，
 * 而非独立的字节码节点引擎；[loop] 提供可中断的循环包装。
 */
abstract class ModuleBase(
    protected val device: DeviceController,
    protected val server: String = "cn",
) {
    /** 最近一帧截图，由 [refresh] 更新。 */
    protected var image: Screenshot? = null

    /** 模板资源加载器(由子类通过 [template] 惰性加载)。 */
    protected val templateLoader: TemplateLoader = TemplateLoader()

    /** 取指定路径的模板(带缓存)。 */
    protected fun template(assetPath: String): TemplateImage =
        templateLoader.get(server, assetPath)

    /** 截取新的一帧。 */
    protected fun refresh(): Screenshot {
        val prev = image
        val shot = device.screenshot()
        prev?.release()
        image = shot
        return shot
    }

    /** 判断按钮是否出现(模板或纯色)。 */
    protected fun appear(button: Button): Boolean {
        val shot = ensureShot()
        return button.appearOn(shot.mat)
    }

    /** 判断模板在指定区域是否出现。 */
    protected fun appear(template: TemplateImage, roi: Roi? = null, similarity: Double = 0.85): Boolean {
        val shot = ensureShot()
        return template.matchResult(shot.mat, similarity, roi) != null
    }

    /** 出现则点击，返回是否点击。 */
    protected fun appearThenClick(button: Button): Boolean {
        if (!appear(button)) return false
        device.click(button.randomPoint())
        return true
    }

    /** 等待按钮出现。 */
    protected fun waitUntilAppear(button: Button, timeoutMs: Long = 30_000): Boolean {
        return Waiter(timeoutMs = timeoutMs).until { appear(button) }
    }

    /** 等待按钮消失。 */
    protected fun waitUntilDisappear(button: Button, timeoutMs: Long = 30_000): Boolean {
        return Waiter(timeoutMs = timeoutMs).until { !appear(button) }
    }

    /** 等待并点击关键按钮(失败则抛 [GameStuckError])。 */
    protected fun ensureClick(button: Button, timeoutMs: Long = 30_000) {
        if (!waitUntilAppear(button, timeoutMs)) {
            throw GameStuckError("timeout waiting button: ${button.name}")
        }
        device.click(button.randomPoint())
    }

    /** 无条件等待 interval 秒。 */
    protected fun sleep(seconds: Double) {
        try {
            Thread.sleep((seconds * 1000).toLong())
        } catch (_: InterruptedException) {
        }
    }

    /**
     * 命令式状态机循环(对齐 ALAS `while 1: ... loop()` 模式)。
     * [body] 返回 true 表示继续循环，false 表示结束。
     * 捕获 [TaskEnd] 正常结束，[GameStuckError] 抛出。
     */
    protected fun loop(body: () -> Boolean) {
        while (true) {
            try {
                if (!body()) return
            } catch (e: TaskEnd) {
                return
            }
        }
    }

    private fun ensureShot(): Screenshot {
        return image ?: refresh()
    }

    fun release() {
        image?.release()
        image = null
        templateLoader.release()
    }
}

/**
 * 模板资源缓存(按 assets 相对路径惰性加载并缓存，释放时统一回收)。
 */
class TemplateLoader {
    private val cache = mutableMapOf<String, TemplateImage>()
    private val owner: Any = Any()

    /** @param assetPath assets 下相对路径，如 "templates/cn/handler/START.png"。 */
    fun get(server: String, assetPath: String): TemplateImage {
        val key = if (assetPath.startsWith("templates")) assetPath else "templates/$server/$assetPath"
        return synchronized(owner) {
            cache.getOrPut(key) {
                // 通过全局 ResourceManager 加载(见 game/handler 或 resources)
                ResourceManager.load(key)
            }
        }
    }

    fun release() {
        synchronized(owner) {
            cache.values.forEach { it.release() }
            cache.clear()
        }
    }
}
