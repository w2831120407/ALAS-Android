package com.nowdex.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 额度进度条。与 Nowdex 官网 / iOS 小组件一致的分段式进度条。
 */
@Composable
fun QuotaBar(
    percent: Float,
    color: Color,
    modifier: Modifier = Modifier,
    trackColor: Color = Color(0xFFE5E5EA),
) {
    val safePercent = percent.coerceIn(0f, 100f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(trackColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(safePercent / 100f)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(usageColor(safePercent, color)),
        )
    }
}
