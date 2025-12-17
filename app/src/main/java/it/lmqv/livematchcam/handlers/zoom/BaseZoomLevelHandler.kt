package it.lmqv.livematchcam.handlers.zoom

import android.content.Context
import it.lmqv.livematchcam.repositories.SettingsRepository
import it.lmqv.livematchcam.services.stream.IVideoSourceZoomHandler
import it.lmqv.livematchcam.utils.Debouncer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    zoomHandler: IVideoSourceZoomHandler) : IZoomLevelHandler {

    private val debounceMs = 300L
    private val updateDebounce = Debouncer(debounceMs)

    protected var offset = 0.1f
    private var lower = 0f
    protected var upper = 0f
    protected var current = 0f
    protected var zoomOffset = 0f

    private var settingsRepository: SettingsRepository

    protected var currentCameraZoom: Float = 0f

    private var initialJob: Job? = null
    private var zoomJob: Job? = null

    init {
//        when (videoSource) {
//            is Camera1Source -> {
//                val rangeInt = videoSource.getZoomRange()
//                this.current = videoSource.getZoom().toFloat()
//                this.lower = rangeInt.lower.toFloat()
//                this.upper = rangeInt.upper.toFloat()
//            }
//            is Camera2Source -> {
//            }
//        }
        val rangeFloat = zoomHandler.getZoomRange()
        //Logd("ZoomLevelHandler:: ${rangeFloat.lower} - ${rangeFloat.upper}")

        this.current = zoomHandler.getZoom()
        //Logd("ZoomLevelHandler:: $current")

        this.lower = rangeFloat.lower
        this.upper = rangeFloat.upper

        if (this.current < this.lower) {
            this.lower = 0.5f
            this.current = 0.5f
        }

        this.currentCameraZoom = this.current

        settingsRepository = SettingsRepository(context)
        initialJob = CoroutineScope(Dispatchers.Main).launch {
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
        zoomJob = CoroutineScope(Dispatchers.Main).launch {
            if (current != value) {
                current = max(lower, min(upper, value))
                settingsRepository.setInitialZoom(current)
                applyZoom()
            }
        }
    }

    abstract fun applyZoom() : Float
}