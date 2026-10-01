package com.nowdex.android.data.model

import java.time.Instant

/**
 * 额度窗口类型。
 * 不同服务有不同的额度周期：5 小时窗口、每周、每月等。
 */
enum class QuotaWindowType {
    /** 5 小时滚动窗口（如 Codex、Claude） */
    FIVE_HOUR,
    /** 每周额度 */
    WEEKLY,
    /** 每月额度 */
    MONTHLY,
    /** 每日额度 */
    DAILY,
    /** 会话额度 */
    SESSION,
}

/**
 * 单个额度窗口的数据。
 */
data class QuotaWindow(
    val type: QuotaWindowType,
    /** 已用百分比 (0-100) */
    val usedPercent: Float,
    /** 已用额度的可读描述，如 "8分钟"、"320万" */
    val usedLabel: String? = null,
    /** 总额度的可读描述，如 "每周"、"5小时" */
    val totalLabel: String? = null,
    /** 下次重置时间 */
    val resetAt: Instant? = null,
    /** 重置次数（如 Codex 的 2x） */
    val resetCount: Int? = null,
) {
    val remainingPercent: Float get() = (100f - usedPercent).coerceIn(0f, 100f)
}

/**
 * 单个服务的完整额度数据。
 */
data class ServiceQuota(
    val service: AiService,
    val windows: List<QuotaWindow>,
    /** 数据最后更新时间 */
    val updatedAt: Instant,
    /** 是否有错误（如凭证过期） */
    val error: String? = null,
)

/**
 * Token 用量明细（来自用量统计工具，如 tokens.ci）。
 */
data class UsageBreakdown(
    val modelName: String,
    /** 该模型消耗的费用（美元） */
    val costUsd: Double,
    /** 该模型消耗的 token 数 */
    val tokens: Long,
)

/**
 * 单日用量汇总。
 */
data class DailyUsage(
    val date: String, // yyyy-MM-dd
    val totalTokens: Long,
    val costUsd: Double,
    val inputTokens: Long = 0,
    val outputTokens: Long = 0,
    val cachedTokens: Long = 0,
    val cacheWriteTokens: Long = 0,
    val breakdown: List<UsageBreakdown> = emptyList(),
)

/**
 * 连续活跃天数与月度预估。
 */
data class UsageStats(
    val streakDays: Int,
    val monthlyEstimateUsd: Double,
    val activeDays: Int,
    val isAverageMultiple: Double,
)
