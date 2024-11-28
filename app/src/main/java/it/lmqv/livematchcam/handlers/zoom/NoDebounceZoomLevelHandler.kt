package it.lmqv.livematchcam.handlers.zoom

import android.content.Context
import com.pedro.encoder.input.sources.video.Camera1Source
import com.pedro.encoder.input.sources.video.Camera2Source
import com.pedro.encoder.input.sources.video.VideoSource
import kotlin.math.min

class NoDebounceZoomLevelHandler(
    context: Context, private val videoSource: VideoSource) : ZoomLevelHandler(context, videoSource), IZoomLevelHandler {

    override fun withOffset(offset: Float, delegate: (value: Float) -> Unit) {
        if (this.zoomOffset != offset) {
            this.zoomOffset = offset
            val zoomLevel = this.applyZoom()
            delegate(zoomLevel)
        }
    }

    override fun applyZoom() : Float {
        val value = min(super.upper, this.current + this.zoomOffset)
        when (videoSource) {
            is Camera1Source -> { videoSource.setZoom(value.toInt()) }
            is Camera2Source -> { videoSource.setZoom(value) }
        }
        return value
    }
}