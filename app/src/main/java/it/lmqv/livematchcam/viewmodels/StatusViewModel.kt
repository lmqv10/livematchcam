package it.lmqv.livematchcam.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class StatusViewModel : ViewModel() {
    private val _bitrate = MutableLiveData(0.0f)
    val bitrate: LiveData<Float> = _bitrate
    fun setBitrate(updatedBitrate: Float) { _bitrate.value = updatedBitrate }

    private val _angleDegrees = MutableLiveData(IntArray(3))
    val angleDegrees: LiveData<IntArray> = _angleDegrees
    fun setAngleDegrees(updatedDegrees: IntArray) { _angleDegrees.value = updatedDegrees }

    private val _zoomLevel = MutableLiveData(0.0f)
    val zoomLevel: LiveData<Float> = _zoomLevel
    fun setZoomLevel(updatedZoomLevel: Float) { _zoomLevel.value = updatedZoomLevel }
}