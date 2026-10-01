package com.nowdex.android.data.repository

import com.nowdex.android.data.model.AiService
import com.nowdex.android.data.model.DailyUsage
import com.nowdex.android.data.model.ServiceQuota
import com.nowdex.android.data.model.UsageStats
import kotlinx.coroutines.flow.Flow

/**
 * 用量数据仓库。
 * 真实实现会通过各服务的 OAuth / Cookie / API Key 直接请求服务端，
 * 凭证保存在设备本地（EncryptedSharedPreferences），Nowdex 不存储任何密码或用量。
 */
interface UsageRepository {

    /** 获取所有已启用服务的额度数据 */
    fun observeQuotas(): Flow<List<ServiceQuota>>

    /** 手动触发刷新（从各服务拉取最新额度） */
    suspend fun refreshQuotas()

    /** 获取指定服务的额度 */
    fun observeQuota(service: AiService): Flow<ServiceQuota?>

    /** 获取今日用量 */
    fun observeTodayUsage(): Flow<DailyUsage?>

    /** 获取每日用量历史（用于热力图） */
    fun observeDailyHistory(days: Int): Flow<List<DailyUsage>>

    /** 获取连续活跃与月度预估 */
    fun observeUsageStats(): Flow<UsageStats>

    /** 获取/设置已启用的服务列表 */
    fun observeEnabledServices(): Flow<List<AiService>>
    suspend fun setEnabledServices(services: List<AiService>)
}
