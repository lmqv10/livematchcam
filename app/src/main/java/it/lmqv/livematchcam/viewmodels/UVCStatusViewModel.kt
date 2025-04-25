package it.lmqv.livematchcam.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UVCStatusViewModel : ViewModel() {
    private val _bitrate = MutableLiveData(0.0f)
    val bitrate: LiveData<Float> = _bitrate
    fun setBitrate(updatedBitrate: Float) { _bitrate.value = updatedBitrate }

    private val _fps = MutableLiveData(0)
    val fps: LiveData<Int> = _fps
    fun setFPS(updatedFps: Int) {
        viewModelScope.launch(Dispatchers.Main) {
            _fps.value = updatedFps
        }
    }
}