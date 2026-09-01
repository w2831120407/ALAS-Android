package com.alas.android.core.device.screencap

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import com.alas.android.core.device.Screencap
import com.alas.android.core.device.ScreencapException
import com.alas.android.core.vision.Screenshot
import org.opencv.android.Utils
import org.opencv.core.Mat

/**
 * 设备端自控截图(MediaProjection)。
 *
 * 通过 MediaProjection API 创建 VirtualDisplay + ImageReader 截取屏幕。
 * 这是"设备端自控"模式的核心截图实现，无需连接电脑，
 * 对齐 MAA 设备端原生截图(MaaAndroidNativeControlUnit)的思路。
 *
 * 使用方法：
 *   1. 通过 [MediaProjectionManager.createScreenCaptureIntent] 发起授权；
 *   2. 拿到 [MediaProjection] 后构造本类。
 */
class MediaProjectionScreencap(
    private val context: Context,
    private val mediaProjection: MediaProjection,
    private val width: Int,
    private val height: Int,
) : Screencap {

    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var thread: HandlerThread? = null
    private var handler: Handler? = null

    init {
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        thread = HandlerThread("alas-screencap").also { it.start() }
        handler = Handler(thread!!.looper)

        val dm = context.resources.displayMetrics
        val density = dm.densityDpi

        virtualDisplay = mediaProjection.createVirtualDisplay(
            "alas-capture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface,
            null,
            handler,
        )
    }

    override fun capture(): Screenshot {
        val reader = imageReader ?: throw ScreencapException("ImageReader not initialized")
        var image: Image? = null
        try {
            image = reader.acquireLatestImage() ?: throw ScreencapException("acquireLatestImage returned null")
            val plane = image.planes[0]
            val buffer = plane.buffer
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            val rowPadding = rowStride - pixelStride * width

            val bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888)
            bitmap.copyPixelsFromBuffer(buffer)

            // 去掉行填充
            val cropped = Bitmap.createBitmap(bitmap, 0, 0, width, height)
            bitmap.recycle()

            val mat = Mat()
            Utils.bitmapToMat(cropped, mat)
            cropped.recycle()
            return Screenshot.wrap(mat)
        } catch (e: ScreencapException) {
            throw e
        } catch (e: Throwable) {
            throw ScreencapException("MediaProjection capture failed", e)
        } finally {
            image?.close()
        }
    }

    override fun close() {
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        thread?.quitSafely()
        thread = null
        try {
            mediaProjection.stop()
        } catch (_: Exception) {
        }
    }
}
