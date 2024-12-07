package it.lmqv.livematchcam.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.lmqv.livematchcam.repositories.StreamersSettingsRepository
import it.lmqv.livematchcam.utils.KeyValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class StreamersViewModel(application: Application) : AndroidViewModel(application) {

    private var streamersSettingsRepository = StreamersSettingsRepository(application)

    private val _servers = MutableStateFlow<List<KeyValue<String>>>(emptyList())
    val servers: StateFlow<List<KeyValue<String>>> = _servers
    private val _keys = MutableStateFlow<List<KeyValue<String>>>(emptyList())
    val keys: StateFlow<List<KeyValue<String>>> = _keys
    private val _currentKey = MutableStateFlow<String?>(null)
    val currentKey: StateFlow<String?> = _currentKey

    /*fun setServers(updatedServers: List<KeyValue<String>>) {
        updateData(_servers, streamersSettingsRepository::setServers, updatedServers)
    }
    fun setKeys(updatedKeys: List<KeyValue<String>>) {
        updateData(_keys, streamersSettingsRepository::setKeys, updatedKeys)
    }*/

    fun setCurrentKey(currentKey: String) {
        viewModelScope.launch {
            streamersSettingsRepository.setCurrentKey(currentKey)
        }
    }

    init {
        observeRepositoryFlow(streamersSettingsRepository.getServers, _servers)
        observeRepositoryFlow(streamersSettingsRepository.getKeys, _keys)
        viewModelScope.launch {
            streamersSettingsRepository.getCurrentKey.collect { _currentKey.value = it }
        }
    }

    fun getCurrentServer() : String {
        return _servers.value.firstOrNull()?.key ?: ""
    }

    fun getServerURI() : String {
        return "${getCurrentServer()}/${currentKey.value}"
    }

    private fun <T> updateData(
        data: MutableStateFlow<List<KeyValue<T>>>,
        repositoryAction: suspend (List<KeyValue<T>>) -> Unit,
        updatedList: List<KeyValue<T>>
    ) {
        val uniqueList = updatedList.distinctBy { it.key }
        viewModelScope.launch {
            repositoryAction(updatedList)
            data.value = uniqueList
        }
    }

    private fun <T> observeRepositoryFlow(
        repositoryFlow: Flow<List<KeyValue<T>>>,
        stateFlow: MutableStateFlow<List<KeyValue<T>>>
    ) {
        viewModelScope.launch {
            repositoryFlow.collect { stateFlow.value = it }
        }
    }
}