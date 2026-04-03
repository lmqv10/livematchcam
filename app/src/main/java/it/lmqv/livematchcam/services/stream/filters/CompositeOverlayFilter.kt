package it.lmqv.livematchcam.services.stream.filters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import androidx.core.graphics.createBitmap
import com.pedro.encoder.input.gl.render.filters.`object`.ImageObjectFilterRender
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.extensions.Loge
import it.lmqv.livematchcam.extensions.loadBitmapOffscreen
import it.lmqv.livematchcam.extensions.toast
import it.lmqv.livematchcam.factories.FilterPosition
import it.lmqv.livematchcam.repositories.MatchRepository
import it.lmqv.livematchcam.services.firebase.FilterOverlay
import it.lmqv.livematchcam.services.stream.IVideoStreamData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * A single GL filter that composites all overlay filter bitmaps into one texture.
 *
 * Instead of N separate ImageObjectFilterRender instances (each with its own GL draw call,
 * texture bind, and shader switch per frame), this class:
 * 1. Observes MatchRepository.filters in a single collector
 * 2. Downloads bitmaps for each active filter slot
 * 3. Composites all visible bitmaps onto a single full-resolution Canvas (CPU-side)
 * 4. Injects the result as 1 GL texture → 1 draw call per frame
 *
 * The recompose() runs ONLY when filter data changes (rare, order of seconds/minutes),
 * NOT every frame. Double buffering ensures thread safety between IO and GL threads.
 */
class CompositeOverlayFilter(
    private val context: Context
) : ImageObjectFilterRender(), IOverlayObjectFilterRender {

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var filtersJob: Job? = null

    private var streamWidth: Int = 1
    private var streamHeight: Int = 1

    // Per-slot cache of downloaded bitmaps and their associated data
    private data class FilterSlot(
        val position: FilterPosition,
        var bitmap: Bitmap? = null,
        var filterData: FilterOverlay? = null,
        var lastUrl: String? = null
    )

    private val slots = listOf(
        FilterSlot(FilterPosition.TOP_LEFT),
        FilterSlot(FilterPosition.TOP),
        FilterSlot(FilterPosition.TOP_RIGHT),
        FilterSlot(FilterPosition.CENTER),
        FilterSlot(FilterPosition.BOTTOM_LEFT),
        FilterSlot(FilterPosition.BOTTOM),
        FilterSlot(FilterPosition.BOTTOM_RIGHT)
    )

    // Single work bitmap for compositing (never passed to setImage directly)
    private var workBitmap: Bitmap? = null
    private val composeLock = Any()

    init {
        // Start with a transparent 1x1 — will be replaced on setVideoStreamData
        setImage(createBitmap(1, 1, Bitmap.Config.ARGB_8888))
    }

    override fun setVideoStreamData(videoStreamData: IVideoStreamData) {
        streamWidth = videoStreamData.width
        streamHeight = videoStreamData.height

        // Allocate work bitmap at stream resolution
        synchronized(composeLock) {
            workBitmap?.recycle()
            workBitmap = createBitmap(streamWidth, streamHeight, Bitmap.Config.ARGB_8888)
        }

        // Full-screen overlay: the composite covers the entire frame
        setScale(100f, 100f)
        setPosition(0f, 0f)

        initializeCollector()
    }

    private fun initializeCollector() {
        filtersJob?.cancel()
        filtersJob = coroutineScope.launch {
            MatchRepository.filters.collect { filterEvents ->
                var needsRecompose = false

                for (slot in slots) {
                    val updated = filterEvents
                        .firstOrNull { it.position == slot.position }
                        ?.filter

                    if (updated != slot.filterData) {
                        slot.filterData = updated
                        val url = updated?.urls?.firstOrNull()

                        if (!url.isNullOrEmpty() && url != slot.lastUrl) {
                            // New or changed URL → download bitmap
                            try {
                                slot.bitmap = loadBitmapOffscreen(context, url)
                                slot.lastUrl = url
                            } catch (e: Exception) {
                                e.printStackTrace()
                                slot.bitmap = null
                                slot.lastUrl = null
                                CoroutineScope(Dispatchers.Main).launch {
                                    context.toast("CompositeFilter: Can't load $url")
                                }
                            }
                        } else if (url.isNullOrEmpty()) {
                            slot.bitmap = null
                            slot.lastUrl = null
                        }
                        needsRecompose = true
                    } else {
                        // filterData unchanged, but check visibility toggle
                        val currentVisible = updated?.visible == true
                        val slotVisible = slot.filterData?.visible == true
                        if (currentVisible != slotVisible) {
                            slot.filterData = updated
                            needsRecompose = true
                        }
                    }
                }

                if (needsRecompose) {
                    recompose()
                }
            }
        }
    }

    /**
     * Compose all visible filter bitmaps onto the work bitmap, then pass a copy to GL.
     * Called ONLY when filter data changes — NOT per-frame.
     *
     * We never pass [workBitmap] directly to [setImage] because RootEncoder may
     * recycle previously-supplied bitmaps. Instead we create a copy each time.
     */
    private fun recompose() {
        synchronized(composeLock) {
            val work = workBitmap ?: return
            if (work.isRecycled) return

            // Clear to fully transparent
            work.eraseColor(Color.TRANSPARENT)
            val canvas = Canvas(work)

            var anyVisible = false

            for (slot in slots) {
                val data = slot.filterData ?: continue
                val bmp = slot.bitmap ?: continue
                if (!data.visible) continue
                if (bmp.isRecycled) continue

                anyVisible = true

                // Calculate destination size (same logic as OverlayFilterRender.scaleSprite)
                // filter.size is in percentage of stream width
                val scaleFactor = data.size.toFloat() / 100f
                val dstWidth = (streamWidth * scaleFactor).toInt().coerceAtLeast(1)
                val dstHeight = (bmp.height.toFloat() / bmp.width.toFloat() * dstWidth)
                    .toInt().coerceAtLeast(1)

                // Calculate position (same logic as OverlayFilterRender.translateSprite)
                val margin = (streamWidth * 0.01f).toInt()
                val (x, y) = calculatePosition(data.position, dstWidth, dstHeight, margin)

                // Draw with the same alpha as OverlayFilterRender (0.75f)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                    alpha = (0.75f * 255).toInt()
                }

                val srcRect = Rect(0, 0, bmp.width, bmp.height)
                val dstRect = Rect(x, y, x + dstWidth, y + dstHeight)
                canvas.drawBitmap(bmp, srcRect, dstRect, paint)
            }

            // Create a copy for GL — setImage() owns this bitmap's lifecycle
            val snapshot = work.copy(Bitmap.Config.ARGB_8888, false)
            setImage(snapshot)
            alpha = if (anyVisible) 1f else 0f
        }
    }

    private fun calculatePosition(
        position: FilterPosition,
        w: Int,
        h: Int,
        margin: Int
    ): Pair<Int, Int> {
        return when (position) {
            FilterPosition.TOP_LEFT -> margin to margin
            FilterPosition.TOP -> (streamWidth - w) / 2 to margin
            FilterPosition.TOP_RIGHT -> (streamWidth - w - margin) to margin
            FilterPosition.CENTER -> (streamWidth - w) / 2 to (streamHeight - h) / 2
            FilterPosition.BOTTOM_LEFT -> margin to (streamHeight - h - margin)
            FilterPosition.BOTTOM -> (streamWidth - w) / 2 to (streamHeight - h - margin)
            FilterPosition.BOTTOM_RIGHT -> (streamWidth - w - margin) to (streamHeight - h - margin)
        }
    }

    override fun release() {
        super.release()
        Logd("CompositeOverlayFilter :: release")
        filtersJob?.cancel()
        synchronized(composeLock) {
            workBitmap?.recycle()
            workBitmap = null
        }
        for (slot in slots) {
            slot.bitmap = null
            slot.filterData = null
            slot.lastUrl = null
        }
    }

    override fun getBitmap(): Bitmap? {
        return if (streamObject.bitmaps.size > 0) streamObject.bitmaps[0] else null
    }

    override fun getOverflowRatio(): Float = 0f

    override fun hide() {
        this.alpha = 0f
    }

    override fun show() {
        this.alpha = 1f
    }
}
