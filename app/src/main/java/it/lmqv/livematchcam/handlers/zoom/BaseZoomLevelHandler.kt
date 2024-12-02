package it.lmqv.livematchcam.handlers.zoom

import android.content.Context
import com.pedro.encoder.input.sources.video.Camera1Source
import com.pedro.encoder.input.sources.video.Camera2Source
import com.pedro.encoder.input.sources.video.VideoSource
import it.lmqv.livematchcam.repositories.SettingsRepository
import it.lmqv.livematchcam.utils.Debouncer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

interface IZoomLevelHandler {
    fun lower()
    fun upper()
    fun increase()
    fun decrease()
    fun withOffset(offset: Float, delegate: (value: Float) -> Unit)
}

abstract class ZoomLevelHandler(
    context: Context,
    videoSource: VideoSource) : IZoomLevelHandler {

    private val debounceMs = 300L
    private val updateDebounce = Debouncer(debounceMs)

    protected var offset = 0.1f
    private var lower = 0f
    protected var upper = 0f
    protected var current = 0f
    protected var zoomOffset = 0f

    private var settingsRepository: SettingsRepository

    protected var currentCameraZoom: Float = 0f

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

    override fun lower() { setCurrentZoom(lower) }
    override fun upper() { setCurrentZoom(upper) }
    override fun increase() { setCurrentZoom(this.current + this.offset) }
    override fun decrease() { setCurrentZoom(this.current - this.offset) }

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

    abstract fun applyZoom() : Float
}