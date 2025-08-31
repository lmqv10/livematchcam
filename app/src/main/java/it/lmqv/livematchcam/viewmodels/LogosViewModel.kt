package it.lmqv.livematchcam.viewmodels
/*
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.lmqv.livematchcam.preferences.LogosSettingsRepository
import it.lmqv.livematchcam.utils.KeyDescription
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LogosViewModel(application: Application) : AndroidViewModel(application) {

    private var logosSettingsRepository = LogosSettingsRepository(application)

    private val _logos = MutableStateFlow<List<KeyDescription<String>>>(emptyList())
    val logos: StateFlow<List<KeyDescription<String>>> = _logos

    fun addLogo(newLogo: KeyDescription<String>) {
        var currentLogos = _logos.value.toMutableList()
        var updatedLogos = currentLogos + newLogo
        val uniqueList = updatedLogos.distinctBy { it.key }
        viewModelScope.launch {
            logosSettingsRepository.sstLogos(uniqueList)
            _logos.value = uniqueList
        }
    }

    init {
        viewModelScope.launch {
            logosSettingsRepository.getLogos.collect { _logos.value = it }
        }
    }
}
*/