package it.lmqv.livematchcam.handlers.offset

import android.content.Context
import it.lmqv.livematchcam.repositories.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

interface IOffsetDegreeHandler {
    //fun addOffsetAtDegree(offset: Float, degree: Int)
    //fun removeOffset(degree: Int)
    //fun clear()
    //fun getWithZoom(zoomLevel: Float) : Float
    //fun apply(degree: Int, delegate: (value: Float) -> Unit)
    fun getOffsetByDegree(degree: Int) : Float
}

abstract class BaseOffsetDegreeHandler(context: Context) : IOffsetDegreeHandler {
    //private val updateDebouncer = Debouncer(500)
    //private val degreesOffset: MutableMap<Int, Float> = mutableMapOf()

    protected var offset: Float = 0.0f
    protected var leftDegree: Int = 0
    protected var rightDegree: Int = 0
    protected var degreeOffset: Float = 0.0f

    //addOffsetAtDegree(0.2f, 20)
    private var settingsRepository: SettingsRepository = SettingsRepository(context)

    init {
        CoroutineScope(Dispatchers.Main).launch {
            settingsRepository.zoomOffset.collect { zoomOffset ->
                offset = zoomOffset
            }
        }
        CoroutineScope(Dispatchers.Main).launch {
            settingsRepository.leftDegree.collect { degree ->
                leftDegree = -degree
            }
        }
        CoroutineScope(Dispatchers.Main).launch {
            settingsRepository.rightDegree.collect { degree ->
                rightDegree = degree
            }
        }
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

    override fun getOffsetByDegree(degree: Int) : Float {
        //val absDegree = abs(degree)
        var offset = 0.0f

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