package com.nowdex.android.data

import com.nowdex.android.data.repository.MockUsageRepository
import com.nowdex.android.data.repository.UsageRepository

/**
 * 简易应用容器（不引入 Hilt 以保持构建简单）。
 * 真实项目中可替换为 Hilt / Koin。
 */
object AppContainer {
    val usageRepository: UsageRepository by lazy { MockUsageRepository() }
}
