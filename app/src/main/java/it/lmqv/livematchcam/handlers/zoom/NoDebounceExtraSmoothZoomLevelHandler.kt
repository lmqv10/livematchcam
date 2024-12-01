package it.lmqv.livematchcam.handlers.zoom

import android.content.Context
import com.pedro.encoder.input.sources.video.Camera1Source
import com.pedro.encoder.input.sources.video.Camera2Source
import com.pedro.encoder.input.sources.video.VideoSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sign

class NoDebounceExtraSmoothZoomLevelHandler(
    context: Context,
    private val videoSource: VideoSource) : ZoomLevelHandler(context, videoSource),
    IZoomLevelHandler {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null

    override fun withOffset(offset: Float, delegate: (value: Float) -> Unit) {
        if (this.zoomOffset != offset) {
            this.zoomOffset = offset
            val zoomLevel = this.applyZoom()
            delegate(zoomLevel)
        }
    }

    override fun applyZoom() : Float {
        val targetZoomLevel = min(this.upper, this.current + this.zoomOffset)
        val stepOffset = targetZoomLevel - this.currentCameraZoom

        if (stepOffset != 0f) {
            var startZoomLevel = this.currentCameraZoom
            this.currentCameraZoom = targetZoomLevel
            var signOffset = stepOffset.sign
            var start = 0f
            var end = (round( abs(stepOffset) * 10) / 10)

            job?.cancel()
            val smoothOffset = offset / 10
            job = scope.launch {
                while (start < end && startZoomLevel < upper) {
                    start += smoothOffset
                    startZoomLevel += (smoothOffset * signOffset)
                    when (videoSource) {
                        is Camera1Source -> { videoSource.setZoom(startZoomLevel.toInt()) }
                        is Camera2Source -> { videoSource.setZoom(startZoomLevel) }
                    }
                    delay(0)
                }
            }
        }
        return this.currentCameraZoom
    }
}