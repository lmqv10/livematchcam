package it.lmqv.livematchcam.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlin.math.max

/*class TeamsScoreBoardViewModel : ViewModel() {
    private val _homeTeam = MutableLiveData("")
    val homeTeam: LiveData<String> = _homeTeam
    fun setHomeTeam(updatedTeam: String) { _homeTeam.value = updatedTeam }

    private val _awayTeam = MutableLiveData("")
    val awayTeam: LiveData<String> = _awayTeam
    fun setAwayTeam(updatedTeam: String) { _awayTeam.value = updatedTeam }

    private val _homeLogo = MutableLiveData<Int>(null)
    val homeLogo: LiveData<Int> = _homeLogo
    fun setHomeLogo(updatedLogo: Int) { _homeLogo.value = updatedLogo }

    private val _awayLogo = MutableLiveData<Int>(null)
    val awayLogo: LiveData<Int> = _awayLogo
    fun setAwayLogo(updatedLogo: Int) { _awayLogo.value = updatedLogo }
}*/

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

