package it.lmqv.livematchcam.viewmodels

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.services.firebase.SetScore
import it.lmqv.livematchcam.services.firebase.VolleyScore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.max

class VolleyScoreViewModel: ViewModel() {

    private var _liveScore = MutableStateFlow<VolleyScore>(VolleyScore())
    val liveScore: StateFlow<VolleyScore> = _liveScore

    private val mutex = Mutex()

    fun initScore(currentScore: VolleyScore) {
        viewModelScope.launch {
            mutex.withLock {
                Logd("VolleyScoreViewModel::initScore")
                if (_liveScore.value != currentScore) {
                    Logd("VolleyScoreViewModel::storeInitScore::$currentScore")
                    _liveScore.value = currentScore
                }
            }
        }
    }

    fun setMatchLeague(updatedLeague: String) {
        viewModelScope.launch {
            mutex.withLock {
                _liveScore.value = _liveScore.value.copy(league = updatedLeague)
            }
        }
    }

    fun incrementHomeScore() {
        viewModelScope.launch {
            mutex.withLock {
                val currentScore = _liveScore.value.sets.toMutableList().last()
                var updatedScore = currentScore.copy(home = currentScore.home.plus(1))
                updateLiveScore(updatedScore)
            }
        }
    }
    fun decrementHomeScore() {
        viewModelScope.launch {
            mutex.withLock {
                val currentScore = _liveScore.value.sets.toMutableList().last()
                var updatedScore = currentScore.copy(home = max(0, currentScore.home.minus(1)))
                updateLiveScore(updatedScore)
            }
        }
    }
    fun incrementAwayScore() {
        viewModelScope.launch {
            mutex.withLock {
                val currentScore = _liveScore.value.sets.toMutableList().last()
                var updatedScore = currentScore.copy(guest = currentScore.guest.plus(1))
                updateLiveScore(updatedScore)
            }
        }
    }
    fun decrementAwayScore() {
        viewModelScope.launch {
            mutex.withLock {
                val currentScore = _liveScore.value.sets.toMutableList().last()
                var updatedScore = currentScore.copy(guest = max(0, currentScore.guest.minus(1)))
                updateLiveScore(updatedScore)
            }
        }
    }

    fun addNewSet() {
        viewModelScope.launch {
            mutex.withLock {
                _liveScore.value = _liveScore.value.copy(
                    sets = _liveScore.value.sets.toMutableList().apply { add(SetScore()) }
                )
            }
        }
    }

    fun removeLastSet() {
        viewModelScope.launch {
            mutex.withLock {
                var updatedSets = _liveScore.value.sets.toMutableList()

                if (Build.VERSION.SDK_INT >= 35) {
                    updatedSets.removeLast()
                } else {
                    updatedSets.removeAt(updatedSets.size - 1)
                }
                _liveScore.value = _liveScore.value.copy(sets = updatedSets)
            }
        }
    }

    fun resetMatch() {
        viewModelScope.launch {
            mutex.withLock {
                _liveScore.value = VolleyScore()
            }
        }
    }

    @Synchronized
    private fun updateLiveScore(updatedScore: SetScore) {
        val updatedSets = _liveScore.value.sets.toMutableList().apply {
            if (isNotEmpty()) {
                this[lastIndex] = updatedScore
            }
        }
        Logd("VolleyScoreViewModel::updateLiveScore::$updatedSets")
        var updatedScore = _liveScore.value.copy(sets = updatedSets)
        _liveScore.value = updatedScore

    }
}