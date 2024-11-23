package it.lmqv.livematchcam.handlers

import android.content.Context
import android.util.Log
import com.pedro.encoder.input.sources.video.Camera1Source
import com.pedro.encoder.input.sources.video.Camera2Source
import com.pedro.encoder.input.sources.video.VideoSource
import it.lmqv.livematchcam.settings.SettingsRepository
import it.lmqv.livematchcam.utils.Debouncer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sign

interface IZoomLevelHandler {
    fun withOffset(offset: Float, delegate: (value: Float) -> Unit)
}

class ZoomLevelHandler(
    context: Context,
    private val videoSource: VideoSource) : IZoomLevelHandler {

    private val debounceMs = 300L
    private val updateDebounce = Debouncer(debounceMs)

    private var offset = 0.1f
    private var lower = 0f
    private var upper = 0f
    private var current = 0f
    private var zoomOffset = 0f

    private var settingsRepository: SettingsRepository

    private var currentCameraZoom: Float = 0f
    private val scope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null

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

        this.currentCameraZoom = this.current

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
            if (current != value) {
                current = max(lower, min(upper, value))
                settingsRepository.setInitialZoom(current)
                applyZoom()
            }
        }
    }

    private fun applyZoom() : Float {
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
                    when (videoSource) {
                        is Camera1Source -> { videoSource.setZoom(startZoomLevel.toInt()) }
                        is Camera2Source -> { videoSource.setZoom(startZoomLevel) }
                    }
                    delay(50)
                }
            }
        }
        return this.currentCameraZoom
    }

    /*private fun applyZoom() : Float {
        val value = min(this.upper, this.current + this.zoomOffset)
        when (videoSource) {
            is Camera1Source -> { videoSource.setZoom(value.toInt()) }
            is Camera2Source -> { videoSource.setZoom(value) }
        }
        return value
    }*/
}