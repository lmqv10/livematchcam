package it.lmqv.livematchcam.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.lmqv.livematchcam.services.stream.NetworkHealth
import it.lmqv.livematchcam.services.stream.StreamPerformanceData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StatusViewModel : ViewModel() {
    private val _bitrate = MutableLiveData(0.0f)
    val bitrate: LiveData<Float> = _bitrate
    fun setBitrate(updatedBitrate: Float) {
        viewModelScope.launch(Dispatchers.Main) {
            _bitrate.value = updatedBitrate
        }
    }

    private val _fps = MutableLiveData(0)
    val fps: LiveData<Int> = _fps
    fun setFPS(updatedFps: Int) {
        viewModelScope.launch(Dispatchers.Main) {
            _fps.value = updatedFps
        }
    }
    private val _angleDegrees = MutableLiveData(IntArray(3))
    val angleDegrees: LiveData<IntArray> = _angleDegrees
    fun setAngleDegrees(updatedDegrees: IntArray) { _angleDegrees.value = updatedDegrees }

    private val _zoomLevel = MutableLiveData(0.0f)
    val zoomLevel: LiveData<Float> = _zoomLevel
    fun setZoomLevel(updatedZoomLevel: Float) { _zoomLevel.value = updatedZoomLevel }

    private val _sourceResolution = MutableLiveData(0)
    val sourceResolution: LiveData<Int> = _sourceResolution
    fun setSourceResolution(updatedResolution: Int) {
        viewModelScope.launch(Dispatchers.Main) {
            _sourceResolution.value = updatedResolution
        }
    }

    private val _sourceFps = MutableLiveData(0)
    val sourceFps: LiveData<Int> = _sourceFps
    fun setSourceFps(updatedFps: Int) {
        viewModelScope.launch(Dispatchers.Main) {
            _sourceFps.value = updatedFps
        }
    }
    private val _sourceBitrate = MutableLiveData(0)
    val sourceBitrate: LiveData<Int> = _sourceBitrate
    fun setSourceBitrate(updatedBitrate: Int) {
        viewModelScope.launch(Dispatchers.Main) {
            _sourceBitrate.value = updatedBitrate
        }
    }

    // --- Network Performance ---
    private val _streamPerformance = MutableLiveData(StreamPerformanceData())
    val streamPerformance: LiveData<StreamPerformanceData> = _streamPerformance
    fun setStreamPerformance(data: StreamPerformanceData) {
        viewModelScope.launch(Dispatchers.Main) {
            _streamPerformance.value = data
        }
    }

//    private val _networkHealth = MutableLiveData(NetworkHealth.GREEN)
//    val networkHealth: LiveData<NetworkHealth> = _networkHealth
//    fun setNetworkHealth(health: NetworkHealth) {
//        viewModelScope.launch(Dispatchers.Main) {
//            _networkHealth.value = health
//        }
//    }
}