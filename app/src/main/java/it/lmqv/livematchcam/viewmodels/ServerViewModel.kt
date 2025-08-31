package it.lmqv.livematchcam.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.lmqv.livematchcam.repositories.ServerSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ServerViewModel(application: Application) : AndroidViewModel(application) {

    private var serverSettingsRepository = ServerSettingsRepository(application)

    private val _currentKey = MutableStateFlow<String?>(null)
    val currentKey: StateFlow<String?> = _currentKey

    private val _currentServer = MutableStateFlow<String?>(null)
    val currentServer: StateFlow<String?> = _currentServer

    private val _serverURL = MutableStateFlow<String?>(null)
    val serverURI: StateFlow<String?> = _serverURL

    fun setCurrentKey(currentKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            serverSettingsRepository.setCurrentKey(currentKey)
            updateServerURI()
        }
    }

    fun setCurrentServer(currentServer: String) {
        viewModelScope.launch(Dispatchers.IO) {
            serverSettingsRepository.setCurrentServer(currentServer)
            updateServerURI()
        }
    }

    init {
        viewModelScope.launch {
            serverSettingsRepository.getCurrentKey.collect {
                _currentKey.value = it
                updateServerURI()
            }
        }
        viewModelScope.launch {
            serverSettingsRepository.getCurrentServer.collect {
                _currentServer.value = it
                updateServerURI()
            }
        }
    }

    private fun updateServerURI() {
        if (_currentServer.value != null && _currentKey.value != null) {
            _serverURL.value = "${_currentServer.value}/${_currentKey.value}"
        } else {
            _serverURL.value = null
        }
    }
}