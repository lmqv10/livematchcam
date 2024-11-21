package it.lmqv.livematchcam.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class StatusViewModel : ViewModel() {
    private val _bitrate = MutableLiveData(0.0f)
    val bitrate: LiveData<Float> = _bitrate
    fun setBitrate(updatedBitrate: Float) { _bitrate.value = updatedBitrate }

    private val _angleDegree = MutableLiveData(0)
    val angleDegree: LiveData<Int> = _angleDegree
    fun setAngleDegree(updatedAngle: Int) { _angleDegree.value = updatedAngle }

    private val _zoomLevel = MutableLiveData(0.0f)
    val zoomLevel: LiveData<Float> = _zoomLevel
    fun setZoomLevel(updatedZoomLevel: Float) { _zoomLevel.value = updatedZoomLevel }
}