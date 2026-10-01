package com.nowdex.android.ui.components

import androidx.compose.ui.graphics.Color

/**
 * 根据用量百分比返回进度条颜色。
 * - < 60%: 服务主题色（正常）
 * - 60% - 85%: 橙色（注意）
 * - >= 85%: 红色（即将用尽）
 */
fun usageColor(percent: Float, base: Color): Color = when {
    percent >= 85f -> Color(0xFFFF3B30)
    percent >= 60f -> Color(0xFFFF9500)
    else -> base
}

/** 将颜色转为透明度较低的背景色 */
fun Color.muted(alpha: Float = 0.15f): Color = copy(alpha = alpha)
