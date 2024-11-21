package it.lmqv.livematchcam.handlers

import android.content.Context
import com.pedro.encoder.input.sources.video.Camera1Source
import com.pedro.encoder.input.sources.video.Camera2Source
import com.pedro.encoder.input.sources.video.VideoSource
import it.lmqv.livematchcam.settings.SettingsRepository
import it.lmqv.livematchcam.utils.Debouncer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

interface IZoomLevelHandler {
    fun withOffset(offset: Float, delegate: (value: Float) -> Unit)
}

class ZoomLevelHandler(
    context: Context,
    private val videoSource: VideoSource) : IZoomLevelHandler {

    private val updateDebounce = Debouncer(500)

    private var offset = 0.1f
    private var lower = 0f
    private var upper = 0f
    private var current = 0f
    private var zoomOffset = 0.0f

    private var settingsRepository: SettingsRepository

    init {
        when (videoSource) {
            is Camera1Source -> {
                this.current = videoSource.getZoom().toFloat()
                val rangeInt = videoSource.getZoomRange()
                this.lower = rangeInt.lower.toFloat()
                this.upper = rangeInt.upper.toFloat()
            }
            is Camera2Source -> {
                this.current = videoSource.getZoom()
                val rangeFloat = videoSource.getZoomRange()
                this.lower = rangeFloat.lower
                this.upper = rangeFloat.upper
            }
        }

        settingsRepository = SettingsRepository(context)
        CoroutineScope(Dispatchers.Main).launch {
            settingsRepository.initialZoom.collect { initialZoom ->
                current = initialZoom
                applyZoom()
            }
        }
    }

    fun lower() { setCurrentZoom(lower) }
    fun upper() { setCurrentZoom(upper) }
    fun increase() { setCurrentZoom(this.current + this.offset) }
    fun decrease() { setCurrentZoom(this.current - this.offset) }

    override fun withOffset(offset: Float, delegate: (value: Float) -> Unit) {
        updateDebounce.submit {
            this.zoomOffset = offset
            val zoomLevel = this.applyZoom()
            delegate(zoomLevel)
        }
    }

    private fun setCurrentZoom(value: Float) {
        CoroutineScope(Dispatchers.Main).launch {
            current = max(lower, min(upper, value))
            settingsRepository.setInitialZoom(current)
            applyZoom()
        }
    }

    private fun applyZoom() : Float {
        val value = min(this.upper, this.current + this.zoomOffset)
        when (videoSource) {
            is Camera1Source -> { videoSource.setZoom(value.toInt()) }
            is Camera2Source -> { videoSource.setZoom(value) }
        }
        return value
    }
}