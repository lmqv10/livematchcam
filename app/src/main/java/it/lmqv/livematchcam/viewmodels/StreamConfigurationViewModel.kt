package it.lmqv.livematchcam.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.lmqv.livematchcam.repositories.ServerSettingsRepository
import it.lmqv.livematchcam.repositories.StreamConfigurationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class StreamConfigurationViewModel(application: Application) : AndroidViewModel(application) {

    private var streamConfigurationRepository = StreamConfigurationRepository(application)

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
    }
}