package com.alas.android.core.device.screencap

import com.alas.android.core.device.Input
import com.alas.android.core.device.Screencap
import com.alas.android.core.device.ScreencapException
import com.alas.android.core.vision.Screenshot
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import org.opencv.android.Utils
import android.graphics.Bitmap
import android.graphics.BitmapFactory

/**
 * ADB 截图(`adb exec-out screencap -p`)。
 *
 * 用于"ADB 控制 / 无线调试"模式：从外部(或本机 adbd)执行 screencap 拉取帧。
 * 对齐 ALAS `module/device/screenshot.py` 的 ADB 方法与 MAA 的 ADB screencap。
 */
class AdbScreencap(
    private val runner: AdbScreencapRunner,
    private val expectedWidth: Int = 1280,
    private val expectedHeight: Int = 720,
) : Screencap {

    override fun capture(): Screenshot {
        val png = runner.execOut("screencap -p")
        if (png.isEmpty()) throw ScreencapException("adb screencap returned empty")
        val bmp = BitmapFactory.decodeByteArray(png, 0, png.size)
            ?: throw ScreencapException("failed to decode screenshot png")
        // 处理旋转/尺寸校正：将画面统一校正为 expectedWidth x expectedHeight
        val corrected = orientAndScale(bmp, expectedWidth, expectedHeight)
        val mat = Mat()
        Utils.bitmapToMat(corrected, mat)
        corrected.recycle()
        bmp.recycle()
        return Screenshot.wrap(mat)
    }

    /**
     * 若截图宽高与期望不一致(旋转或异形屏)，先旋转 90° 再缩放。
     * 对齐 ALAS Device._handle_orientated_image 的语义。
     */
    private fun orientAndScale(bmp: Bitmap, targetW: Int, targetH: Int): Bitmap {
        var b = bmp
        val needRotate = (bmp.width < bmp.height) != (targetW < targetH)
        if (needRotate) {
            b = rotate90(bmp)
        }
        if (b.width != targetW || b.height != targetH) {
            b = Bitmap.createScaledBitmap(b, targetW, targetH, true)
        }
        return b
    }

    private fun rotate90(bmp: Bitmap): Bitmap {
        val matrix = android.graphics.Matrix()
        matrix.postRotate(90f)
        val out = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
        bmp.recycle()
        return out
    }

    override fun close() = Unit
}

/** 供 ADB exec-out 执行的最小抽象。 */
fun interface AdbScreencapRunner {
    /** 执行 exec-out 命令，返回原始字节流(stdout)。 */
    fun execOut(command: String): ByteArray
}
