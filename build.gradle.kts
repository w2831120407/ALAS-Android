// ALAS-Android 根构建脚本
// 碧蓝航线自动化脚本的安卓版。参照 MAA(明日方舟小助手) Android 端技术方案构建：
//   - 原生 Android 控制单元(MaaAndroidNativeControlUnit 思路)：设备端自控截图 + 触控注入
//   - ADB / 无线调试(adb tcpip / adb pair) 远程控制
//   - OpenCV 模板匹配识别层(对齐 ALAS module.base.template)
//   - 配置驱动的任务调度器(对齐 ALAS config + alas.py 中央调度)
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    // Kotlin 2.0 起，启用 Compose 需要单独的 Compose Compiler Gradle 插件
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
}
