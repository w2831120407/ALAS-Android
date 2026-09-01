package com.alas.android

import android.app.Application
import com.alas.android.core.base.AlasLog
import com.alas.android.core.base.ResourceManager
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.OpenCVLoader

/**
 * 应用入口：初始化 OpenCV 与全局资源管理器。
 */
class AlasApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ResourceManager.init(this)
        initOpenCV()
    }

    private fun initOpenCV() {
        val success = OpenCVLoader.initLocal()
        if (success) {
            AlasLog.i("OpenCV initialized")
        } else {
            AlasLog.e("OpenCV failed to initialize locally")
            // 可回退到 OpenCVManager 下载方案
        }
    }
}
