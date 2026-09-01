package com.alas.android.core.vision

import android.graphics.Rect

/**
 * ROI(Region of Interest，识别区域)。
 *
 * 对齐 ALAS 的 `area` 概念：在整张截图上的一个矩形区域，
 * 模板匹配只在该区域内进行，既提升速度又避免误匹配。
 *
 * 采用 (左上角, 宽, 高) 的表示，与截图坐标系一致(横屏游戏，宽 > 高)。
 */
data class Roi(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
) {
    val right: Int get() = x + width
    val bottom: Int get() = y + height

    val empty: Boolean get() = width <= 0 || height <= 0

    val center: Point get() = Point(x + width / 2, y + height / 2)

    fun toRect(): Rect = Rect(x, y, x + width, y + height)

    /** 区域平移后的新 ROI(用于模板命中后换算绝对坐标)。 */
    fun offset(dx: Int, dy: Int): Roi = Roi(x + dx, y + dy, width, height)

    /** 限制在 [screenWidth]x[screenHeight] 范围内的安全裁剪。 */
    fun clampTo(screenWidth: Int, screenHeight: Int): Roi {
        val nx = x.coerceIn(0, screenWidth)
        val ny = y.coerceIn(0, screenHeight)
        val nr = right.coerceIn(nx, screenWidth)
        val nb = bottom.coerceIn(ny, screenHeight)
        return Roi(nx, ny, nr - nx, nb - ny)
    }

    companion object {
        /** 全屏区域。 */
        fun full(screenWidth: Int, screenHeight: Int): Roi =
            Roi(0, 0, screenWidth, screenHeight)
    }
}

/**
 * 二维坐标点(截图坐标系，原点在左上角)。
 */
data class Point(val x: Int, val y: Int) {
    companion object {
        val ORIGIN = Point(0, 0)
    }
}
