package com.nowdex.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowdex.android.data.model.AiService
import com.nowdex.android.data.model.QuotaWindow
import com.nowdex.android.data.model.QuotaWindowType
import com.nowdex.android.data.model.ServiceQuota
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 单个服务的额度卡片。
 * 对应 Nowdex Mac 菜单栏 / iOS 应用中的服务展示。
 */
@Composable
fun ServiceCard(
    quota: ServiceQuota,
    modifier: Modifier = Modifier,
) {
    val service = quota.service
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        // 服务标题行
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            ServiceIcon(service = service)
            Spacer(Modifier.width(10.dp))
            Text(
                text = service.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (service.planTier != null) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = service.planTier,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            quota.windows.firstOrNull()?.let { primary ->
                Text(
                    text = "${primary.remainingPercent.toInt()}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = usageColor(primary.usedPercent, service.color),
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // 每个额度窗口
        quota.windows.forEachIndexed { index, window ->
            QuotaWindowRow(window = window, color = service.color)
            if (index < quota.windows.lastIndex) {
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun QuotaWindowRow(window: QuotaWindow, color: Color) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = window.totalLabel ?: window.type.label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "剩余 ${window.remainingPercent.toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(6.dp))
        QuotaBar(percent = window.usedPercent, color = color)
        Spacer(Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "重置 ${formatResetTime(window.resetAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (window.resetCount != null && window.resetCount > 1) {
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "重置次数 ${window.resetCount}x",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** 服务图标：用首字母 + 主题色圆形背景（无外部图标资源依赖） */
@Composable
fun ServiceIcon(service: AiService, size: Int = 28) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(service.color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = service.shortName.take(2),
            color = Color.White,
            fontSize = (size * 0.4).sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private val QuotaWindowType.label: String
    get() = when (this) {
        QuotaWindowType.FIVE_HOUR -> "5小时"
        QuotaWindowType.WEEKLY -> "每周"
        QuotaWindowType.MONTHLY -> "每月"
        QuotaWindowType.DAILY -> "每日"
        QuotaWindowType.SESSION -> "会话"
    }

private fun formatResetTime(instant: Instant?): String {
    if (instant == null) return "—"
    val now = Instant.now()
    val duration = Duration.between(now, instant)
    if (duration.isNegative || duration.isZero) return "已重置"
    val hours = duration.toHours()
    return when {
        hours >= 24 -> {
            val timeStr = instant.atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("HH:mm"))
            "${hours / 24}天后 $timeStr"
        }
        hours >= 1 -> "${hours}小时后"
        else -> "${duration.toMinutes()}分钟后"
    }
}
