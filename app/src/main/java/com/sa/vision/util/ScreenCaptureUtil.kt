package com.sa.vision.util

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.PixelCopy
import android.view.View
import androidx.annotation.RequiresApi
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

object ScreenCaptureUtil {

    /**
     * Captures a bitmap of the target view (e.g. WebView).
     * Uses PixelCopy on API 26+ if Activity window is available, with hardware/software Canvas fallback.
     */
    suspend fun captureViewBitmap(view: View, activity: Activity? = null): Bitmap? {
        if (view.width <= 0 || view.height <= 0) return null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null && activity.window != null) {
            val pixelCopyBitmap = captureWithPixelCopy(view, activity)
            if (pixelCopyBitmap != null) {
                return pixelCopyBitmap
            }
        }

        return captureWithCanvas(view)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun captureWithPixelCopy(view: View, activity: Activity): Bitmap? {
        return suspendCancellableCoroutine { continuation ->
            try {
                val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                val locationOfViewInWindow = IntArray(2)
                view.getLocationInWindow(locationOfViewInWindow)

                val rect = Rect(
                    locationOfViewInWindow[0],
                    locationOfViewInWindow[1],
                    locationOfViewInWindow[0] + view.width,
                    locationOfViewInWindow[1] + view.height
                )

                PixelCopy.request(
                    activity.window,
                    rect,
                    bitmap,
                    { copyResult ->
                        if (copyResult == PixelCopy.SUCCESS) {
                            continuation.resume(bitmap)
                        } else {
                            continuation.resume(captureWithCanvas(view))
                        }
                    },
                    Handler(Looper.getMainLooper())
                )
            } catch (e: Exception) {
                e.printStackTrace()
                continuation.resume(captureWithCanvas(view))
            }
        }
    }

    /**
     * Captures a view's bitmap by rendering it into an ARGB_8888 Canvas.
     */
    fun captureWithCanvas(view: View): Bitmap? {
        return try {
            if (view.width <= 0 || view.height <= 0) return null
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Scales bitmap down if either width or height exceeds maxDimension, maintaining aspect ratio.
     */
    fun scaleBitmap(bitmap: Bitmap, maxDimension: Int = 1080): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxDimension && height <= maxDimension) {
            return bitmap
        }

        val aspectRatio = width.toFloat() / height.toFloat()
        val targetWidth: Int
        val targetHeight: Int

        if (width >= height) {
            targetWidth = maxDimension
            targetHeight = (maxDimension / aspectRatio).toInt()
        } else {
            targetHeight = maxDimension
            targetWidth = (maxDimension * aspectRatio).toInt()
        }

        return Bitmap.createScaledBitmap(
            bitmap,
            targetWidth.coerceAtLeast(1),
            targetHeight.coerceAtLeast(1),
            true
        )
    }

    /**
     * Converts bitmap into a compressed ByteArray.
     */
    fun bitmapToByteArray(
        bitmap: Bitmap,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        quality: Int = 90
    ): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(format, quality, outputStream)
        return outputStream.toByteArray()
    }

    /**
     * Converts bitmap into a Base64 encoded string.
     */
    fun bitmapToBase64(
        bitmap: Bitmap,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        quality: Int = 90
    ): String {
        val bytes = bitmapToByteArray(bitmap, format, quality)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /**
     * Saves bitmap to a temporary File.
     */
    fun saveBitmapToFile(
        bitmap: Bitmap,
        context: Context,
        fileName: String = "screen_capture_${System.currentTimeMillis()}.jpg",
        quality: Int = 90
    ): File {
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { out ->
            val format = if (fileName.endsWith(".png", ignoreCase = true)) {
                Bitmap.CompressFormat.PNG
            } else {
                Bitmap.CompressFormat.JPEG
            }
            bitmap.compress(format, quality, out)
        }
        return file
    }
}
