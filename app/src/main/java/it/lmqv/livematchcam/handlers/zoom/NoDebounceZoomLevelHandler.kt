package it.lmqv.livematchcam.handlers.zoom

import android.content.Context
import it.lmqv.livematchcam.services.stream.IVideoSourceZoomHandler
import kotlin.math.min

class NoDebounceZoomLevelHandler(
    context: Context, private val zoomHandler: IVideoSourceZoomHandler) : ZoomLevelHandler(context, zoomHandler), IZoomLevelHandler {

    override fun withOffset(offset: Float, delegate: (value: Float) -> Unit) {
        if (this.zoomOffset != offset) {
            this.zoomOffset = offset
            val zoomLevel = this.applyZoom()
            delegate(zoomLevel)
        }
    }

    override fun applyZoom() : Float {
        val value = min(super.upper, this.current + this.zoomOffset)
        zoomHandler.setZoom(value)
        return value
    }
}