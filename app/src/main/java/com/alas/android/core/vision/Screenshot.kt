package com.alas.android.core.vision

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Mat

/**
 * 截图帧容器。
 *
 * 每次从控制层取得一帧截图后包装为 [Screenshot]，供识别层使用。
 * 持有 [mat] 的释放责任，识别完成后调用 [release]。
 */
class Screenshot private constructor(
    val mat: Mat,
    val width: Int,
    val height: Int,
    val timestamp: Long,
) {
    fun release() = mat.release()

    /** 模板匹配到任意一个模板即返回 true。 */
    fun appearsAny(templates: List<TemplateImage>, similarity: Double = 0.85): Boolean {
        for (t in templates) {
            if (t.matchResult(mat, similarity) != null) return true
        }
        return false
    }

    /** 在 [roi] 区域内的平均颜色。 */
    fun meanColor(roi: Roi): Color = ImageUtils.meanColor(mat, roi)

    companion object {
        fun wrap(mat: Mat, timestamp: Long = System.currentTimeMillis()): Screenshot =
            Screenshot(mat, mat.cols(), mat.rows(), timestamp)

        fun fromBitmap(bmp: Bitmap, timestamp: Long = System.currentTimeMillis()): Screenshot {
            val mat = Mat()
            Utils.bitmapToMat(bmp, mat)
            return Screenshot(mat, mat.cols(), mat.rows(), timestamp)
        }
    }
}
