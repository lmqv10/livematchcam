package it.lmqv.livematchcam.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.lmqv.livematchcam.repositories.FirebaseDataRepository
import it.lmqv.livematchcam.repositories.FirebaseSettingsRepository
import it.lmqv.livematchcam.repositories.MatchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FirebaseViewModel(application: Application) : AndroidViewModel(application) {

    private var firebaseSettingsRepository = FirebaseSettingsRepository(application)
    private val firebaseDataRepository = FirebaseDataRepository(application)

    private val _currentKey = MutableStateFlow<String?>(null)
    val currentKey: StateFlow<String?> = _currentKey

    private val _currentServer = MutableStateFlow<String?>(null)
//    val currentServer: StateFlow<String?> = _currentServer
//
//    private val _servers = MutableStateFlow<List<OptionItem>>(emptyList())
//    val servers: StateFlow<List<OptionItem>> = _servers
//
//    private val _keys = MutableStateFlow<List<OptionItem>>(emptyList())
//    val keys: StateFlow<List<OptionItem>> = _keys

//    private val _serverURL = MutableStateFlow<String?>(null)
//    val serverURI: StateFlow<String?> = _serverURL

    fun setCurrentKey(currentKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            firebaseSettingsRepository.setCurrentKey(currentKey)
            updateServerURI()
        }
    }

    fun setCurrentServer(currentServer: String) {
        viewModelScope.launch(Dispatchers.IO) {
            firebaseSettingsRepository.setCurrentServer(currentServer)
            updateServerURI()
        }
    }

    init {
        viewModelScope.launch {
            firebaseSettingsRepository.getCurrentKey.collect {
                _currentKey.value = it
                updateServerURI()
            }
        }
        viewModelScope.launch {
            firebaseSettingsRepository.getCurrentServer.collect {
                _currentServer.value = it
                updateServerURI()
            }
        }
//        viewModelScope.launch {
//            firebaseSettingsRepository.getServers.collect {
//                _servers.value = it
//            }
//        }
//        viewModelScope.launch {
//            firebaseSettingsRepository.getKeys.collect {
//                _keys.value = it
//            }
//        }
    }

    private fun updateServerURI() {
        var serverURI: String? = null
        if (_currentServer.value != null && _currentKey.value != null) {
            serverURI = "${_currentServer.value}/${_currentKey.value}"
            firebaseDataRepository.setStreamName(_currentKey.value!!)
        }
        MatchRepository.setRTMPServer(serverURI)
    }
}