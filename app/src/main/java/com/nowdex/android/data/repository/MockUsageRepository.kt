package com.nowdex.android.data.repository

import com.nowdex.android.data.model.AiService
import com.nowdex.android.data.model.DailyUsage
import com.nowdex.android.data.model.QuotaWindow
import com.nowdex.android.data.model.QuotaWindowType
import com.nowdex.android.data.model.ServiceQuota
import com.nowdex.android.data.model.UsageBreakdown
import com.nowdex.android.data.model.UsageStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.sin

/**
 * Mock 数据仓库。
 *
 * 真实场景下，每个服务的 Provider 会通过以下方式之一获取额度：
 * - OAuth API（Codex、Claude）
 * - 浏览器 Cookie（Cursor、Qoder、MiniMax）
 * - API Key（DeepSeek、OpenRouter）
 * - CLI 会话（Grok CLI、OpenCode）
 *
 * 凭证保存在 EncryptedSharedPreferences / Android Keystore 中，
 * 由设备直接向各服务请求，Nowdex 服务器不存储任何密码或用量。
 */
class MockUsageRepository : UsageRepository {

    private val enabledServices = MutableStateFlow(
        listOf(
            AiService.Codex,
            AiService.Claude,
            AiService.Cursor,
            AiService.MiniMax,
            AiService.Qoder,
            AiService.DeepSeek,
        )
    )

    private val quotas = MutableStateFlow(buildMockQuotas())
    private val todayUsage = MutableStateFlow(buildTodayUsage())
    private val dailyHistory = MutableStateFlow(buildDailyHistory())
    private val stats = MutableStateFlow(
        UsageStats(
            streakDays = 12,
            monthlyEstimateUsd = 318.40,
            activeDays = 316,
            isAverageMultiple = 1.2,
        )
    )

    override fun observeQuotas(): StateFlow<List<ServiceQuota>> = quotas.asStateFlow()

    override suspend fun refreshQuotas() {
        // 模拟刷新：微小随机扰动，模拟真实数据变化
        val current = quotas.value
        val jittered = current.map { quota ->
            quota.copy(
                windows = quota.windows.map { w ->
                    val delta = (sin(Instant.now().epochSecond.toDouble() / 100.0) * 2).toFloat()
                    w.copy(usedPercent = (w.usedPercent + delta).coerceIn(0f, 100f))
                },
                updatedAt = Instant.now(),
            )
        }
        quotas.value = jittered
    }

    override fun observeQuota(service: AiService): StateFlow<ServiceQuota?> =
        MutableStateFlow(quotas.value.firstOrNull { it.service == service }).asStateFlow()

    override fun observeTodayUsage(): StateFlow<DailyUsage?> = todayUsage.asStateFlow()

    override fun observeDailyHistory(days: Int): StateFlow<List<DailyUsage>> {
        val list = dailyHistory.value.takeLast(days)
        return MutableStateFlow(list).asStateFlow()
    }

    override fun observeUsageStats(): StateFlow<UsageStats> = stats.asStateFlow()

    override fun observeEnabledServices(): StateFlow<List<AiService>> = enabledServices.asStateFlow()

    override suspend fun setEnabledServices(services: List<AiService>) {
        enabledServices.value = services
        // 同步刷新额度列表，只保留已启用的服务
        val enabledIds = services.map { it.id }.toSet()
        val existing = quotas.value.associateBy { it.service.id }
        val base = buildMockQuotas().associateBy { it.service.id }
        quotas.value = services.mapNotNull { svc ->
            existing[svc.id] ?: base[svc.id]
        }
    }

    // ---- Mock 数据构造 ----

    private fun nowPlusHours(hours: Long): Instant =
        Instant.now().plusSeconds(hours * 3600)

    private fun nowPlusDays(days: Long): Instant =
        Instant.now().plusSeconds(days * 86400)

    private fun buildMockQuotas(): List<ServiceQuota> {
        val now = Instant.now()
        return listOf(
            ServiceQuota(
                service = AiService.Codex,
                updatedAt = now,
                windows = listOf(
                    QuotaWindow(
                        type = QuotaWindowType.FIVE_HOUR,
                        usedPercent = 64f,
                        usedLabel = "5小时",
                        totalLabel = "5小时",
                        resetAt = nowPlusHours(4),
                        resetCount = 2,
                    ),
                    QuotaWindow(
                        type = QuotaWindowType.WEEKLY,
                        usedPercent = 38f,
                        usedLabel = "8分钟",
                        totalLabel = "每周",
                        resetAt = nowPlusDays(3),
                    ),
                ),
            ),
            ServiceQuota(
                service = AiService.Claude,
                updatedAt = now,
                windows = listOf(
                    QuotaWindow(
                        type = QuotaWindowType.FIVE_HOUR,
                        usedPercent = 17f,
                        usedLabel = "5小时",
                        totalLabel = "5小时",
                        resetAt = nowPlusHours(1),
                    ),
                    QuotaWindow(
                        type = QuotaWindowType.WEEKLY,
                        usedPercent = 58f,
                        usedLabel = "每周",
                        totalLabel = "每周",
                        resetAt = nowPlusDays(4),
                    ),
                ),
            ),
            ServiceQuota(
                service = AiService.Cursor,
                updatedAt = now,
                windows = listOf(
                    QuotaWindow(
                        type = QuotaWindowType.MONTHLY,
                        usedPercent = 72f,
                        usedLabel = "240万",
                        totalLabel = "每月",
                        resetAt = nowPlusDays(12),
                    ),
                ),
            ),
            ServiceQuota(
                service = AiService.Grok,
                updatedAt = now,
                windows = listOf(
                    QuotaWindow(
                        type = QuotaWindowType.WEEKLY,
                        usedPercent = 45f,
                        usedLabel = "每周",
                        totalLabel = "每周",
                        resetAt = nowPlusDays(2),
                    ),
                ),
            ),
            ServiceQuota(
                service = AiService.Kimi,
                updatedAt = now,
                windows = listOf(
                    QuotaWindow(
                        type = QuotaWindowType.FIVE_HOUR,
                        usedPercent = 30f,
                        usedLabel = "5小时",
                        totalLabel = "5小时",
                        resetAt = nowPlusHours(3),
                    ),
                    QuotaWindow(
                        type = QuotaWindowType.WEEKLY,
                        usedPercent = 82f,
                        usedLabel = "每周",
                        totalLabel = "每周",
                        resetAt = nowPlusDays(1),
                    ),
                ),
            ),
            ServiceQuota(
                service = AiService.GLM,
                updatedAt = now,
                windows = listOf(
                    QuotaWindow(
                        type = QuotaWindowType.WEEKLY,
                        usedPercent = 28f,
                        usedLabel = "每周",
                        totalLabel = "每周",
                        resetAt = nowPlusDays(5),
                    ),
                ),
            ),
            ServiceQuota(
                service = AiService.MiniMax,
                updatedAt = now,
                windows = listOf(
                    QuotaWindow(
                        type = QuotaWindowType.WEEKLY,
                        usedPercent = 100f,
                        usedLabel = "100万",
                        totalLabel = "每周",
                        resetAt = nowPlusDays(1),
                    ),
                    QuotaWindow(
                        type = QuotaWindowType.FIVE_HOUR,
                        usedPercent = 75f,
                        usedLabel = "5小时",
                        totalLabel = "5小时",
                        resetAt = nowPlusHours(2),
                    ),
                ),
            ),
            ServiceQuota(
                service = AiService.Qoder,
                updatedAt = now,
                windows = listOf(
                    QuotaWindow(
                        type = QuotaWindowType.MONTHLY,
                        usedPercent = 98f,
                        usedLabel = "299/300",
                        totalLabel = "每月",
                        resetAt = nowPlusDays(15),
                    ),
                ),
            ),
            ServiceQuota(
                service = AiService.DeepSeek,
                updatedAt = now,
                windows = listOf(
                    QuotaWindow(
                        type = QuotaWindowType.MONTHLY,
                        usedPercent = 38f,
                        usedLabel = "¥38.0",
                        totalLabel = "额度",
                        resetAt = nowPlusDays(20),
                    ),
                ),
            ),
            ServiceQuota(
                service = AiService.OpenCodeGo,
                updatedAt = now,
                windows = listOf(
                    QuotaWindow(
                        type = QuotaWindowType.WEEKLY,
                        usedPercent = 53f,
                        usedLabel = "每周",
                        totalLabel = "每周",
                        resetAt = nowPlusDays(3),
                    ),
                ),
            ),
        )
    }

    private fun buildTodayUsage(): DailyUsage {
        val today = LocalDate.now().toString()
        return DailyUsage(
            date = today,
            totalTokens = 48_600_000L,
            costUsd = 31.84,
            inputTokens = 3_200_000L,
            outputTokens = 1_100_000L,
            cachedTokens = 41_000_000L,
            cacheWriteTokens = 2_100_000L,
            breakdown = listOf(
                UsageBreakdown("gpt-6-astra", 9.52, 13_900_000L),
                UsageBreakdown("gpt-6-mini", 0.92, 3_500_000L),
                UsageBreakdown("claude-opus-4", 8.31, 9_200_000L),
                UsageBreakdown("claude-sonnet-4.5", 1.40, 4_600_000L),
                UsageBreakdown("codex-models", 11.69, 17_400_000L),
            ),
        )
    }

    private fun buildDailyHistory(): List<DailyUsage> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        return (365 downTo 1).map { offset ->
            val date = today.minusDays(offset.toLong())
            // 用伪随机但稳定的方式生成活跃度
            val seed = abs(date.hashCode())
            val isActive = seed % 7 != 0 // 约 6/7 天活跃
            val base = if (isActive) {
                5_000_000L + (seed % 50_000_000L)
            } else 0L
            val cost = if (base > 0) (base / 1_000_000.0) * 0.65 else 0.0
            DailyUsage(
                date = date.toString(),
                totalTokens = base,
                costUsd = Math.round(cost * 100.0) / 100.0,
            )
        }
    }
}
