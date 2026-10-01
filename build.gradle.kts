// Nowdex-Android 根构建脚本
// Nowdex 安卓版：在 Android 上查看 Codex、Claude、Cursor 等 AI 智能体的用量与额度。
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    // Kotlin 2.0 起，启用 Compose 需要单独的 Compose Compiler Gradle 插件
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
}
