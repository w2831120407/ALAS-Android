package com.alas.android.core.vision

import android.content.Context
import org.opencv.android.Utils
import org.opencv.core.Mat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * 模板图片资源(对齐 ALAS `module/base/template.py` 的 Template / TemplateImage)。
 *
 * 负责从 assets(或扩展目录)加载一张 PNG 截图模板，并预计算：
 *   - 原始图像 [image]（用于 CCOEFF 模板匹配）
 *   - 二值化图像 [imageBinary]（用于 matchBinary）
 *   - 亮度图 [imageLuma]（用于 matchLuma）
 *
 * 模板通常存放在 `assets/templates/<server>/<category>/XXX.png`，
 * 与 ALAS 的 `assets/<server>/<category>/` 目录结构对齐。
 */
class TemplateImage private constructor(
    val name: String,
    private val image: Mat,
    private val imageBinary: Mat?,
    private val imageLuma: Mat?,
) {

    val width: Int get() = image.cols()
    val height: Int get() = image.rows()
    val size: Roi get() = Roi(0, 0, width, height)

    /**
     * 标准模板匹配。@param similarity 默认 0.85，对齐 ALAS。
     */
    fun match(imageSrc: Mat, similarity: Double = 0.85): Boolean =
        matchResult(imageSrc, similarity) != null

    fun matchResult(imageSrc: Mat, similarity: Double = 0.85, roi: Roi? = null): MatchResult? {
        val res = ImageUtils.matchTemplate(imageSrc, image, roi)
        if (res.similarity > similarity && res.point != null) {
            return res
        }
        return null
    }

    /** 二值化模板匹配，用于模板本身色彩差异大的场景。 */
    fun matchBinary(imageSrc: Mat, similarity: Double = 0.85): MatchResult? {
        val bin = ImageUtils.otsuBinarize(imageSrc)
        return try {
            val res = ImageUtils.matchTemplate(bin, imageBinary ?: return null)
            if (res.similarity > similarity) res else null
        } finally {
            bin.release()
        }
    }

    /** 亮度通道模板匹配。 */
    fun matchLuma(imageSrc: Mat, similarity: Double = 0.85): MatchResult? {
        val luma = ImageUtils.rgb2Luma(imageSrc)
        return try {
            val res = ImageUtils.matchTemplate(luma, imageLuma ?: return null)
            if (res.similarity > similarity) res else null
        } finally {
            luma.release()
        }
    }

    fun release() {
        image.release()
        imageBinary?.release()
        imageLuma?.release()
    }

    companion object {
        /**
         * 从 assets 加载模板。
         * @param assetPath assets 内的相对路径，如 "templates/cn/handler/START.png"。
         */
        fun fromAssets(context: Context, assetPath: String): TemplateImage {
            val data = context.assets.open(assetPath).use { it.readBytes() }
            val src = Mat()
            Utils.matFromBitmap(android.graphics.BitmapFactory.decodeByteArray(data, 0, data.size), src)
            val name = File(assetPath).nameWithoutExtension.uppercase()
            val binary = ImageUtils.otsuBinarize(src)
            val luma = ImageUtils.rgb2Luma(src)
            return TemplateImage(name, src, binary, luma)
        }

        /**
         * 从文件系统加载模板(支持用户自定义 / 从网络同步的扩展模板目录)。
         */
        fun fromFile(file: File): TemplateImage {
            val src = Mat()
            Utils.matFromBitmap(android.graphics.BitmapFactory.decodeFile(file.absolutePath), src)
            val name = file.nameWithoutExtension.uppercase()
            val binary = ImageUtils.otsuBinarize(src)
            val luma = ImageUtils.rgb2Luma(src)
            return TemplateImage(name, src, binary, luma)
        }

        /** 从内存 Mat 包装一个模板(便于测试与动态生成)。 */
        fun fromMat(mat: Mat, name: String): TemplateImage {
            val binary = ImageUtils.otsuBinarize(mat)
            val luma = ImageUtils.rgb2Luma(mat)
            return TemplateImage(name, mat, binary, luma)
        }
    }
}
