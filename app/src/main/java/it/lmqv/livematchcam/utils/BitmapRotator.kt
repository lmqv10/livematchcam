package it.lmqv.livematchcam.utils

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import androidx.core.graphics.drawable.toBitmap
import java.io.IOException
import androidx.core.graphics.scale
import coil.Coil
import coil.request.ImageRequest
import it.lmqv.livematchcam.services.stream.filters.RotatorDescriptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BitmapRotator(
    private val context: Context,
    private val rotatorDescriptor: RotatorDescriptor = RotatorDescriptor()
) {
    interface BitmapRotationListener {
        fun onBitmapAvailable(bitmap: Bitmap)
    }

    private val scope = CoroutineScope(Dispatchers.Default)
    private var targetWidth: Int = dpToPx(rotatorDescriptor.targetWidthDp)

    private val bitmapCache = mutableMapOf<String, Bitmap>()
    private var urls: List<String> = emptyList()
    private var currentIndex = 0
    private var listener: BitmapRotationListener? = null
    private val handler = Handler(Looper.getMainLooper())

    private var isRunning = false
    private val rotationRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return

            val bitmaps = synchronized(bitmapCache) {
                bitmapCache.values.toList()
            }
            if (bitmaps.isNotEmpty()) {
                listener?.onBitmapAvailable(bitmaps[currentIndex].copy(Bitmap.Config.ARGB_8888, true))
                currentIndex = (currentIndex + 1) % bitmaps.size
                if (bitmaps.size == 1) {
                    stop()
                }
            }
            handler.postDelayed(this, rotatorDescriptor.intervalMillis)
        }
    }

    fun setBitmapRotationListener(listener: BitmapRotationListener) {
        this.listener = listener
    }

    fun setUrls(urlList: List<String>) {
        this.urls = urlList
        bitmapCache.clear()
        currentIndex = 0
        preloadBitmaps()
    }

    fun start() {
        //if (urls.isEmpty() || isRunning) return
        if (!isRunning) {
            isRunning = true
            handler.post(rotationRunnable)
        }
    }

    fun stop() {
        isRunning = false
        handler.removeCallbacks(rotationRunnable)
    }

    private fun preloadBitmaps() {
        for (url in urls) {
            scope.launch {
                try {
                    val bitmap = loadAndResizeBitmap(url)
                    if (bitmap != null) {
                        synchronized(bitmapCache) {
                            bitmapCache[url] = bitmap
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    synchronized(bitmapCache) {
                        bitmapCache.remove(url)
                    }
                }
            }
        }
    }

    private suspend fun loadAndResizeBitmap(urlString: String): Bitmap? {
        return Coil.imageLoader(context).execute(
            ImageRequest.Builder(context).data(urlString).build()
        ).drawable?.toBitmap()?.let {
            val targetHeight = targetWidth * it.height / it.width
            return it.scale(targetWidth, targetHeight)
                .copy(Bitmap.Config.ARGB_8888, true)
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }
}
