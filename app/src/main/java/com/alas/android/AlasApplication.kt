package com.alas.android

import android.app.Application
import com.alas.android.core.base.AlasLog
import com.alas.android.core.base.ResourceManager
import org.opencv.android.OpenCVLoader

/**
 * 应用入口：初始化 OpenCV 与全局资源管理器。
 * 所有初始化步骤都必须用 try/catch 包裹，避免 Application.onCreate 抛异常导致进程直接被杀(秒闪退)。
 */
class AlasApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        try {
            ResourceManager.init(this)
        } catch (t: Throwable) {
            AlasLog.e("ResourceManager.init failed", t)
        }
        try {
            initOpenCV()
        } catch (t: Throwable) {
            AlasLog.e("OpenCV init crashed (non-fatal, disable CV features)", t)
        }
    }

    private fun initOpenCV() {
        val success = try {
            OpenCVLoader.initLocal()
        } catch (t: UnsatisfiedLinkError) {
            AlasLog.e("OpenCV native lib load failed", t)
            false
        } catch (t: Throwable) {
            AlasLog.e("OpenCVLoader.initLocal exception", t)
            false
        }
        if (success) {
            AlasLog.i("OpenCV initialized")
        } else {
            AlasLog.w("OpenCV failed to initialize locally — CV features disabled")
            // 此处不中断应用：自动化的连接设置/调度/配置 UI 仍可工作
        }
    }
}
