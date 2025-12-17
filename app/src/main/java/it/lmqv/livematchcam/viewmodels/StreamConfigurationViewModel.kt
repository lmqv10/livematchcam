package it.lmqv.livematchcam.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.lmqv.livematchcam.repositories.StreamConfigurationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class VideoSourceKind(val label: String) {
    CAMERA2("Video Camera"),
    UVC_SONY("HandyCam Sony");
    //UVC_CAMERA("UVC Camera Generic")

    override fun toString(): String = label
}

class StreamConfigurationViewModel(application: Application) : AndroidViewModel(application) {

    private var streamConfigurationRepository = StreamConfigurationRepository(application)

    private val _videoSourceKind = MutableStateFlow<VideoSourceKind>(VideoSourceKind.CAMERA2)
    val videoSourceKind: StateFlow<VideoSourceKind> = _videoSourceKind

    private val _fps = MutableStateFlow<Int?>(null)
    val fps: StateFlow<Int?> = _fps

    private val _resolution = MutableStateFlow<Int?>(null)
    val resolution: StateFlow<Int?> = _resolution

    fun setFps(updatedFps: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            streamConfigurationRepository.setFps(updatedFps)
        }
    }

    fun setResolution(updatedResolution: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            streamConfigurationRepository.setResolution(updatedResolution)
        }
    }

    fun setVideoSourceKind(updatedVideoSourceKind: VideoSourceKind) {
        viewModelScope.launch(Dispatchers.IO) {
            streamConfigurationRepository.setVideoSourceKind(updatedVideoSourceKind)
        }
    }

    init {
        viewModelScope.launch {
            streamConfigurationRepository.fps.collect {
                _fps.value = it
            }
        }
        viewModelScope.launch {
            streamConfigurationRepository.resolution.collect {
                _resolution.value = it
            }
        }
        viewModelScope.launch {
            streamConfigurationRepository.videoSourceKind.collect {
                _videoSourceKind.value = it
            }
        }
    }
}