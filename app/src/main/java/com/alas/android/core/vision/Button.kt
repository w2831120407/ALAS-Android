package com.alas.android.core.vision

import org.opencv.core.Mat

/**
 * 游戏按钮(对齐 ALAS `module/base/button.py` 的 Button)。
 *
 * 一个按钮 = 识别用的 [area](ROI) + 点击用的 [buttonArea](点击区域) + 可选的 [color](纯色校验)。
 * 模板匹配在 [area] 内进行；命中后点击 [buttonArea] 内的随机点(模拟人手)。
 */
class Button(
    /** 识别区域(模板匹配/颜色检测的 ROI)。 */
    val area: Roi,
    /** 点击区域(通常略大于或等于 area)。 */
    val buttonArea: Roi = area,
    /** 纯色校验可选目标颜色。 */
    val color: Color? = null,
    val name: String = "button",
) {

    /** 是否在 [image] 中出现(纯色校验优先，其次模板匹配)。 */
    fun appearOn(image: Mat): Boolean {
        val c = color
        if (c != null) {
            val mean = ImageUtils.meanColor(image, area)
            return mean.similar(c, 10)
        }
        // 无颜色定义时需由外部模板判定；此处返回 false 由调用方补充模板。
        return false
    }

    /** 返回点击点(在 [buttonArea] 内取一个随机点，模拟人类操作)。 */
    fun randomPoint(): Point {
        val w = buttonArea.width
        val h = buttonArea.height
        val px = buttonArea.x + (if (w <= 1) 0 else (0..w).random())
        val py = buttonArea.y + (if (h <= 1) 0 else (0..h).random())
        return Point(px, py)
    }

    companion object {
        /**
         * 由模板命中点构造按钮。
         * @param matchPoint 模板左上角在截图中的坐标。
         * @param templateSize 模板尺寸(用于换算点击区域)。
         */
        fun fromMatch(matchPoint: Point, templateSize: Roi, name: String = "button"): Button {
            val area = Roi(matchPoint.x, matchPoint.y, templateSize.width, templateSize.height)
            return Button(area = area, buttonArea = area, name = name)
        }
    }
}

/**
 * 规则网格按钮(对齐 ALAS ButtonGrid)：用于九宫格/列表式界面，
 * 由起点、间隔与行列数自动生成一组按钮。
 */
class ButtonGrid(
    val start: Roi,
    val stepX: Int,
    val stepY: Int,
    val rows: Int,
    val cols: Int,
    val prefix: String = "grid",
) {
    private val buttons: List<Button> = buildList {
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val x = start.x + c * stepX
                val y = start.y + r * stepY
                val area = Roi(x, y, start.width, start.height)
                add(Button(area = area, buttonArea = area, name = "$prefix_${r}_$c"))
            }
        }
    }

    operator fun get(index: Int): Button = buttons[index]
    fun size(): Int = buttons.size
    val all: List<Button> get() = buttons
}
