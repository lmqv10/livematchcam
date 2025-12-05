package it.lmqv.livematchcam.handlers.zoom

import android.content.Context
import it.lmqv.livematchcam.services.stream.IVideoSourceZoomHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sign

class SmoothZoomLevelHandler(
    context: Context,
    private val zoomHandler: IVideoSourceZoomHandler) : ZoomLevelHandler(context, zoomHandler),
    IZoomLevelHandler {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null

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
            job = scope.launch {
                while (start < end && startZoomLevel < upper) {
                    start += offset
                    startZoomLevel += (offset * signOffset)
                    zoomHandler.setZoom(startZoomLevel)
                    delay(50)
                }
            }
        }
        return this.currentCameraZoom
    }
}