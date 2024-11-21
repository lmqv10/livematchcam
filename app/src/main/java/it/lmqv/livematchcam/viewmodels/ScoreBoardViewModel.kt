package it.lmqv.livematchcam.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlin.math.max

class HomeScoreBoardViewModel : ViewModel() {
    private val _name = MutableLiveData("")
    val name: LiveData<String> = _name
    fun setName(updatedName: String) { _name.value = updatedName }

    private val _score = MutableLiveData(0)
    val score: LiveData<Int> = _score
    fun incrementScore(step: Int = 1) {
        _score.value = max((_score.value ?: 0) + step, 0)
    }

    private val _logo = MutableLiveData<Int>(null)
    val logo: LiveData<Int> = _logo
    fun setLogo(updatedLogo: Int) { _logo.value = updatedLogo }
}

class AwayScoreBoardViewModel : ViewModel() {
    private val _name = MutableLiveData("")
    val name: LiveData<String> = _name
    fun setName(updatedName: String) { _name.value = updatedName }

    private val _score = MutableLiveData(0)
    val score: LiveData<Int> = _score
    fun incrementScore(step: Int = 1) {
        _score.value = max((_score.value ?: 0) + step, 0)
    }

    private val _logo = MutableLiveData<Int>(null)
    val logo: LiveData<Int> = _logo
    fun setLogo(updatedLogo: Int) { _logo.value = updatedLogo }
}

