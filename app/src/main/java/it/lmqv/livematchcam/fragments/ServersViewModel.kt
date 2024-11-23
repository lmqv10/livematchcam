package it.lmqv.livematchcam.fragments

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import it.lmqv.livematchcam.utils.KeyValue

class ServersViewModel : ViewModel() {
    private val _servers = MutableLiveData<List<KeyValue<String>>>(emptyList())
    val servers: LiveData<List<KeyValue<String>>> = _servers
    fun setServers(updatedServers: List<KeyValue<String>>) {
        _servers.value = updatedServers
    }
    private val _keys = MutableLiveData<List<KeyValue<String>>>(emptyList())
    val keys: LiveData<List<KeyValue<String>>> = _keys
    fun setKeys(updatedKeys: List<KeyValue<String>>) {
        _keys.value = updatedKeys
    }
}