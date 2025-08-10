package it.lmqv.livematchcam.handlers.offset

import android.content.Context
import it.lmqv.livematchcam.repositories.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

enum class ManualZoomLevel {
    In,
    None,
    Out
}

interface IOffsetDegreeHandler {
    //fun addOffsetAtDegree(offset: Float, degree: Int)
    //fun removeOffset(degree: Int)
    //fun clear()
    //fun getWithZoom(zoomLevel: Float) : Float
    //fun apply(degree: Int, delegate: (value: Float) -> Unit)

    fun getOffsetByDegrees(degrees: IntArray) : Float
    fun initialize()
    fun destroy()
    fun manualZoomLevel(zoomLevel: ManualZoomLevel)
}

abstract class BaseOffsetDegreeHandler(protected val context: Context) : IOffsetDegreeHandler {
    //private val updateDebouncer = Debouncer(500)
    //private val degreesOffset: MutableMap<Int, Float> = mutableMapOf()

    protected var offset: Float = 0.0f
    protected var leftDegree: Int = 0
    protected var rightDegree: Int = 0
    protected var degreeOffset: Float = 0.0f

    private var zoomJob: Job? = null
    private var leftDegreeJob: Job? = null
    private var rightDegreeJob: Job? = null

    //addOffsetAtDegree(0.2f, 20)
    private var settingsRepository: SettingsRepository = SettingsRepository(context)

    init {
        zoomJob = CoroutineScope(Dispatchers.Main).launch {
            settingsRepository.zoomOffset.collect { zoomOffset ->
                offset = zoomOffset
            }
        }
        leftDegreeJob = CoroutineScope(Dispatchers.Main).launch {
            settingsRepository.leftDegree.collect { degree ->
                leftDegree = -degree
            }
        }
        rightDegreeJob = CoroutineScope(Dispatchers.Main).launch {
            settingsRepository.rightDegree.collect { degree ->
                rightDegree = degree
            }
        }
        initialize()
    }


    /*override fun addOffsetAtDegree(offset: Float, degree: Int) {
        degreesOffset[degree] = offset
    }

    override fun removeOffset(degree: Int) {
        if (degree in degreesOffset) {
            degreesOffset.remove(degree)
        }
    }

    override fun clear() { degreesOffset.clear() }*/

    /*override fun getWithZoom(zoomLevel: Float) : Float {
        return zoomLevel + this.degreeOffset
    }*/

    /*override fun apply(degree: Int, delegate: (value: Float) -> Unit) {
        updateDebouncer.submit {
            val offset = getOffsetByDegree(degree)
            val value = zoomLevelHandler.withOffset(offset)
            delegate(value)
        }
    }*/
    abstract override fun initialize()

    override fun destroy() {
        zoomJob?.cancel()
        leftDegreeJob?.cancel()
        rightDegreeJob?.cancel()
    }

    abstract override fun manualZoomLevel(zoomLevel: ManualZoomLevel)

    override fun getOffsetByDegrees(degrees: IntArray) : Float {
        var offset = 0.0f
        var degree = degrees[0]

        if (degree < this.leftDegree || degree > this.rightDegree) {
            offset += this.offset
        }
        /*for ((key, value) in degreesOffset) {
            if (absDegree >= key) {
                offset += value
            }
        }*/
        this.degreeOffset = offset
        return this.degreeOffset
    }
}