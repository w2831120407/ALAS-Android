package com.nowdex.android.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.nowdex.android.MainActivity
import com.nowdex.android.data.AppContainer
import com.nowdex.android.data.model.AiService
import com.nowdex.android.data.model.ServiceQuota
import kotlinx.coroutines.flow.first

/**
 * 主屏幕小组件：展示单个 AI 服务的额度用量。
 * 对应 Nowdex iOS 主屏幕小组件（浅色/深色模式）。
 *
 * 小组件中为示例数据——真实数据由设备直接从各服务获取。
 */
class QuotaWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences("nowdex_widget", Context.MODE_PRIVATE)
        val serviceId = prefs.getString(KEY_SERVICE, AiService.Codex.id) ?: AiService.Codex.id
        val service = AiService.fromId(serviceId) ?: AiService.Codex

        val repository = AppContainer.usageRepository
        val quotas = repository.observeQuotas().first()
        val quota = quotas.firstOrNull { it.service == service }
            ?: repository.observeQuota(service).first()

        provideContent {
            QuotaWidgetContent(service = service, quota = quota)
        }
    }

    companion object {
        const val KEY_SERVICE = "widget_service"
    }
}

@Composable
private fun QuotaWidgetContent(service: AiService, quota: ServiceQuota?) {
    val primary = quota?.windows?.firstOrNull()
    val usedPercent = primary?.usedPercent ?: 38f
    val remaining = (100f - usedPercent).toInt()
    val totalLabel = primary?.totalLabel ?: "每周"
    val resetLabel = primary?.resetAt?.let { formatReset(it) } ?: "3天后 18:30"

    val textPrimary = ColorProvider(day = Color(0xFF111111), night = Color(0xFFFFFFFF))
    val textSecondary = ColorProvider(day = Color(0xFF8E8E93), night = Color(0xFF999999))
    val accent = ColorProvider(
        day = usageGlanceColor(usedPercent, service.color),
        night = usageGlanceColor(usedPercent, service.color),
    )
    val trackColor = ColorProvider(day = Color(0xFFE5E5EA), night = Color(0xFF3A3A3C))

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF1C1C1E)))
            .padding(14.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        // 标题行：服务名 + 剩余百分比
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = GlanceModifier
                    .width(22.dp)
                    .height(22.dp)
                    .background(ColorProvider(day = service.color, night = service.color)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = service.shortName.take(2),
                    style = TextStyle(
                        color = ColorProvider(day = Color.White, night = Color.White),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
            Spacer(GlanceModifier.width(8.dp))
            Text(
                text = service.displayName,
                style = TextStyle(
                    color = textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = GlanceModifier.defaultWeight(),
            )
            Text(
                text = "$remaining%",
                style = TextStyle(
                    color = accent,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }

        Spacer(GlanceModifier.height(10.dp))

        // 大数字：已用额度
        Text(
            text = primary?.usedLabel ?: "8分钟",
            style = TextStyle(
                color = textPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Text(
            text = totalLabel,
            style = TextStyle(
                color = textSecondary,
                fontSize = 11.sp,
            ),
        )

        Spacer(GlanceModifier.height(8.dp))

        // 进度条：用 Row + 权重实现分段
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .height(6.dp)
                    .background(accent),
                content = {},
            )
            // 剩余部分
            Box(
                modifier = GlanceModifier
                    .height(6.dp)
                    .width((((100f - usedPercent) / usedPercent.coerceAtLeast(1f)) * 60).dp)
                    .background(trackColor),
                content = {},
            )
        }

        Spacer(GlanceModifier.height(8.dp))

        // 已用 / 剩余
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Text(
                text = "已用 ${usedPercent.toInt()}%",
                style = TextStyle(
                    color = textSecondary,
                    fontSize = 10.sp,
                ),
                modifier = GlanceModifier.defaultWeight(),
            )
            Text(
                text = "剩余 ${remaining}%",
                style = TextStyle(
                    color = textSecondary,
                    fontSize = 10.sp,
                ),
            )
        }

        Spacer(GlanceModifier.height(4.dp))

        // 重置时间
        Text(
            text = "重置 $resetLabel",
            style = TextStyle(
                color = textSecondary,
                fontSize = 10.sp,
            ),
        )
    }
}

private fun usageGlanceColor(percent: Float, base: Color): Color = when {
    percent >= 85f -> Color(0xFFFF3B30)
    percent >= 60f -> Color(0xFFFF9500)
    else -> base
}

private fun formatReset(instant: java.time.Instant): String {
    val now = java.time.Instant.now()
    val duration = java.time.Duration.between(now, instant)
    val hours = duration.toHours()
    return when {
        hours >= 24 -> "${hours / 24}天后 " +
            instant.atZone(java.time.ZoneId.systemDefault())
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
        hours >= 1 -> "${hours}小时后"
        else -> "${duration.toMinutes()}分钟后"
    }
}

class QuotaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuotaWidget()
}
