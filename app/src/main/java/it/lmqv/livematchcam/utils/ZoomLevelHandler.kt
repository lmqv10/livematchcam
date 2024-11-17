package it.lmqv.livematchcam.utils

import com.pedro.encoder.input.sources.video.Camera1Source
import com.pedro.encoder.input.sources.video.Camera2Source
import com.pedro.encoder.input.sources.video.VideoSource
import kotlin.math.max
import kotlin.math.min

class ZoomLevelHandler(private val videoSource: VideoSource)  {
    private val updateDebouncer = Debouncer(750)
    private var offset = 0f
    private var isAutoZoomOn = false

    var lower = 0f
    var upper = 0f
    var current = 0f

    var autoOffsetValue = 0.3f


    init {
        when (videoSource) {
            is Camera1Source -> {
                this.current = videoSource.getZoom().toFloat()
                val rangeInt = videoSource.getZoomRange()
                this.lower = rangeInt.lower.toFloat()
                this.upper = rangeInt.upper.toFloat()
                this.offset = 0.1f
            }
            is Camera2Source -> {
                this.current = videoSource.getZoom()
                val rangeFloat = videoSource.getZoomRange()
                this.lower = rangeFloat.lower
                this.upper = rangeFloat.upper
                this.offset = 1.0f
            }
        }
    }

    fun lower() {
        this.current = this.lower
        updateZoom()
    }

    fun upper() {
        this.current = this.upper
        updateZoom()
    }

    fun increase() {
        this.current += this.offset
        this.current = min(this.upper, this.current)
        updateZoom()
    }

    fun decrease() {
        this.current -= this.offset
        this.current = max(this.lower, this.current)
        updateZoom()
    }

    fun autoZoomOn() {
        isAutoZoomOn = true
        updateDebouncer.submit {
            updateZoom()
        }
    }

    fun autoZoomOff() {
        isAutoZoomOn = false
        updateDebouncer.submit {
            updateZoom()
        }
    }


    private fun updateZoom() {
        val offsetValue = if (this.isAutoZoomOn) this.autoOffsetValue else 0.0f
        val value = min(this.upper, this.current + offsetValue)

        when (videoSource) {
            is Camera1Source -> { videoSource.setZoom(value.toInt()) }
            is Camera2Source -> { videoSource.setZoom(value) }
        }
    }
}