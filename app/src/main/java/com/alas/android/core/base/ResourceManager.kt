package com.alas.android.core.base

import android.content.Context
import android.graphics.BitmapFactory
import com.alas.android.core.vision.TemplateImage
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * 全局模板资源管理(对齐 ALAS `module/template/assets.py` 的资源清单与
 * `module/base/resource.py` 的按需加载/释放)。
 *
 * 模板来源优先级：
 *  1. 用户扩展目录 `files/templates/`(可通过脚本/网络同步更新，免重装)
 *  2. assets 内置 `templates/`
 */
object ResourceManager {

    private var appContext: Context? = null
    private val cache = ConcurrentHashMap<String, TemplateImage>()

    fun init(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
        }
    }

    /**
     * 加载模板。
     * @param assetRelativePath assets 或扩展目录内的相对路径，如 "templates/cn/handler/START.png"。
     */
    fun load(assetRelativePath: String): TemplateImage {
        return cache.getOrPut(assetRelativePath) {
            val extFile = extTemplateFile(assetRelativePath)
            if (extFile.exists()) {
                TemplateImage.fromFile(extFile)
            } else {
                val ctx = appContext ?: throw IllegalStateException("ResourceManager not initialized")
                TemplateImage.fromAssets(ctx, assetRelativePath)
            }
        }
    }

    fun get(server: String, category: String, name: String): TemplateImage =
        load("templates/$server/$category/$name.png")

    /** 判断某模板是否存在于 assets 或扩展目录。 */
    fun exists(assetRelativePath: String): Boolean {
        val ctx = appContext ?: return false
        return extTemplateFile(assetRelativePath).exists() ||
            ctx.assets.list(assetRelativePath.substringBeforeLast('/'))?.contains(assetRelativePath.substringAfterLast('/')) == true
    }

    private fun extTemplateFile(assetRelativePath: String): File {
        val ctx = appContext ?: throw IllegalStateException("ResourceManager not initialized")
        return File(ctx.filesDir, assetRelativePath)
    }

    fun releaseAll() {
        cache.values.forEach { it.release() }
        cache.clear()
    }
}
