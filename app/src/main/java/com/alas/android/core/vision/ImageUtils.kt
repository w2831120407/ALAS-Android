package com.alas.android.core.vision

import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Rect as CvRect
import org.opencv.imgproc.Imgproc

/**
 * 图像处理工具(对齐 ALAS `module/base/utils.py` 的常用操作)。
 *
 * 所有方法接收 [Mat] 并返回新的 [Mat]，调用方负责 [Mat.release] 以避免内存泄漏。
 */
object ImageUtils {

    /** 将截图裁剪出 [roi] 区域。 */
    fun crop(src: Mat, roi: Roi): Mat {
        val w = src.cols()
        val h = src.rows()
        val r = roi.clampTo(w, h)
        if (r.empty) return Mat()
        return Mat(src, CvRect(r.x, r.y, r.width, r.height))
    }

    /** BGR 转灰度。 */
    fun toGray(src: Mat): Mat {
        val out = Mat()
        if (src.channels() == 3) {
            Imgproc.cvtColor(src, out, Imgproc.COLOR_BGR2GRAY)
        } else {
            src.copyTo(out)
        }
        return out
    }

    /** 灰度转 Y 亮度通道(等价于 rgb2luma)。 */
    fun rgb2Luma(src: Mat): Mat {
        if (src.channels() == 3) {
            val luma = Mat()
            Imgproc.cvtColor(src, luma, Imgproc.COLOR_BGR2GRAY)
            return luma
        }
        return toGray(src)
    }

    /** Otsu 二值化。 */
    fun otsuBinarize(src: Mat): Mat {
        val gray = if (src.channels() == 3) toGray(src) else src
        val out = Mat()
        Imgproc.threshold(gray, out, 0.0, 255.0, Imgproc.THRESH_BINARY or Imgproc.THRESH_OTSU)
        if (gray !== src) gray.release()
        return out
    }

    /**
     * 计算 [template] 在 [image] 中的最大归一化相关系数(CCOEFF_NORMED)。
     * 对齐 ALAS `cv2.matchTemplate(..., cv2.TM_CCOEFF_NORMED)` + minMaxLoc。
     *
     * @return 相似度(0~1)与命中点(模板左上角在 image 中的坐标)；未匹配到合理区域返回相似度 0。
     */
    fun matchTemplate(
        image: Mat,
        template: Mat,
        roi: Roi? = null,
    ): MatchResult {
        val region = roi ?: Roi.full(image.cols(), image.rows())
        val cropped = crop(image, region)
        if (cropped.empty()) {
            return MatchResult(0.0, null)
        }
        val result = Mat()
        try {
            Imgproc.matchTemplate(cropped, template, result, Imgproc.TM_CCOEFF_NORMED)
            val minMax = Core.minMaxLoc(result)
            val maxVal = minMax.maxVal
            val maxLoc = minMax.maxLoc
            if (maxVal < 0) return MatchResult(0.0, null)
            // 换算回整张截图坐标系
            val hit = Point(region.x + maxLoc.x.toInt(), region.y + maxLoc.y.toInt())
            return MatchResult(maxVal, hit)
        } finally {
            result.release()
            cropped.release()
        }
    }

    /** 区域平均颜色(用于纯色校验，对齐 Button.appear_on 的 color 检测)。 */
    fun meanColor(src: Mat, roi: Roi): Color {
        val region = crop(src, roi)
        return try {
            if (region.empty()) {
                Color(0, 0, 0)
            } else {
                val mean = Core.mean(region)
                Color(
                    mean.`val`[0].toInt(),
                    mean.`val`[1].toInt(),
                    mean.`val`[2].toInt(),
                )
            }
        } finally {
            region.release()
        }
    }

    /** 缩放图像到指定倍数。 */
    fun resize(src: Mat, fx: Double, fy: Double): Mat {
        val out = Mat()
        Imgproc.resize(src, out, org.opencv.core.Size(), fx, fy, Imgproc.INTER_LINEAR)
        return out
    }
}

/**
 * 模板匹配结果。
 *
 * @param similarity 最大归一化相关系数(0~1)，越大越相似。
 * @param point      命中点(模板左上角在整张截图中的坐标)，未命中为 null。
 */
data class MatchResult(
    val similarity: Double,
    val point: Point?,
)

/** BGR 颜色值。 */
data class Color(val b: Int, val g: Int, val r: Int) {
    /** 与另一颜色的绝对差之和是否不超过 [threshold](对齐 ALAS color_similar threshold=10)。 */
    fun similar(other: Color, threshold: Int = 10): Boolean {
        val diff = kotlin.math.abs(b - other.b) +
            kotlin.math.abs(g - other.g) +
            kotlin.math.abs(r - other.r)
        return diff <= threshold
    }
}
