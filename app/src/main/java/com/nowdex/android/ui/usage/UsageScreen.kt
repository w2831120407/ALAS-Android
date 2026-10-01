package com.nowdex.android.ui.usage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nowdex.android.data.model.DailyUsage
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.max

/**
 * 用量页：Token 用量、费用、模型明细、每日活动热力图。
 * 对应 Nowdex iOS 应用底部导航的"用量"。
 * Token 用量由用量统计工具（如 tokens.ci）提供支持。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageScreen(
    viewModel: UsageViewModel = viewModel(),
) {
    val today by viewModel.todayUsage.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("用量", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (today == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        val usage = today!!

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 今日用量大数字
            item {
                TodayUsageHeader(usage = usage)
            }

            // Token 构成：输入/输出/缓存/缓存写入
            item {
                TokenBreakdownCard(usage = usage)
            }

            // 模型费用明细
            item {
                ModelBreakdownCard(usage = usage)
            }

            // 每日活动热力图
            item {
                ActivityHeatmap(history = history, stats = stats)
            }

            // 连续活跃 & 月度预估
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatCard(
                        title = "连续活跃",
                        value = "${stats.streakDays} 天",
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        title = "月度预估",
                        value = "${'$'}${"%.2f".format(stats.monthlyEstimateUsd)}",
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Token 用量由用量统计工具提供支持",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TodayUsageHeader(usage: DailyUsage) {
    val dateText = LocalDate.now().format(DateTimeFormatter.ofPattern("M月d日"))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(20.dp),
    ) {
        Text(
            text = "今天 · $dateText",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = formatTokens(usage.totalTokens),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "今日 Token",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${'$'}${"%.2f".format(usage.costUsd)}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "费用",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TokenBreakdownCard(usage: DailyUsage) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        Text(
            text = "Token 构成",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(12.dp))
        TokenRow(label = "输入", tokens = usage.inputTokens, color = Color(0xFFFF9500))
        Spacer(Modifier.height(8.dp))
        TokenRow(label = "输出", tokens = usage.outputTokens, color = Color(0xFFFF2D92))
        Spacer(Modifier.height(8.dp))
        TokenRow(label = "缓存命中", tokens = usage.cachedTokens, color = Color(0xFF34C759))
        Spacer(Modifier.height(8.dp))
        TokenRow(label = "缓存写入", tokens = usage.cacheWriteTokens, color = Color(0xFF007AFF))
    }
}

@Composable
private fun TokenRow(label: String, tokens: Long, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(8.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(64.dp),
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = formatTokens(tokens),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ModelBreakdownCard(usage: DailyUsage) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        Text(
            text = "模型费用",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(12.dp))
        usage.breakdown.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.modelName,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${'$'}${"%.2f".format(item.costUsd)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(70.dp),
                )
                Text(
                    text = formatTokens(item.tokens),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(80.dp),
                )
            }
        }
    }
}

/** 每日活动热力图（类似 GitHub contribution graph，按周排列） */
@Composable
private fun ActivityHeatmap(history: List<DailyUsage>, stats: com.nowdex.android.data.model.UsageStats) {
    if (history.isEmpty()) return
    val maxTokens = history.maxOfOrNull { it.totalTokens } ?: 1L
    // 取最近约 53 周 * 7 天，按周分组
    val days = history.takeLast(371)
    val weeks = days.chunked(7)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "每日活动",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "${stats.activeDays} 个活跃日",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            weeks.forEach { week ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    week.forEach { day ->
                        val level = when {
                            day.totalTokens == 0L -> 0
                            day.totalTokens < maxTokens * 0.25 -> 1
                            day.totalTokens < maxTokens * 0.5 -> 2
                            day.totalTokens < maxTokens * 0.75 -> 3
                            else -> 4
                        }
                        Box(
                            modifier = Modifier
                                .width(11.dp)
                                .height(11.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(heatColor(level)),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "低",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(6.dp))
            (0..4).forEach { level ->
                Box(
                    modifier = Modifier
                        .width(11.dp)
                        .height(11.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(heatColor(level)),
                )
                Spacer(Modifier.width(2.dp))
            }
            Text(
                text = "高",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun heatColor(level: Int): Color = when (level) {
    0 -> Color(0xFFE5E5EA)
    1 -> Color(0xFFB7E4C7)
    2 -> Color(0xFF74C69D)
    3 -> Color(0xFF40916C)
    else -> Color(0xFF2D6A4F)
}

private fun formatTokens(tokens: Long): String = when {
    tokens >= 100_000_000L -> "${tokens / 10_000_000L}千万"
    tokens >= 10_000L -> "${tokens / 10_000L}万"
    else -> tokens.toString()
}
