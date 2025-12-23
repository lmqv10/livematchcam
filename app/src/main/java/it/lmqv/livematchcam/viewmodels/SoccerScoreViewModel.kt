package it.lmqv.livematchcam.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.lmqv.livematchcam.services.firebase.SoccerScore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.max

enum class Command {
    START_TIME,
    PAUSE,
    RESET_TIME,

    ZOOM_IN,
    ZOOM_DEFAULT,
    ZOOM_OUT
}

class SoccerScoreViewModel: ViewModel() {

    private var _liveScore = MutableStateFlow<SoccerScore>(SoccerScore())
    val liveScore: StateFlow<SoccerScore> = _liveScore

    private val mutex = Mutex()

    @Synchronized
    fun initScore(currentScore: SoccerScore) {
        viewModelScope.launch {
            mutex.withLock {
                if (_liveScore.value != currentScore) {
                    _liveScore.value = currentScore
                }
            }
        }
    }

    fun incrementHomeScore(step: Int = 1) {
        viewModelScope.launch {
            mutex.withLock {
                //Logd("SoccerScoreViewModel::incrementHomeScore::$step")
                var scoreHome = max(_liveScore.value.home.plus(step), 0)
                _liveScore.value = _liveScore.value.copy(home = scoreHome)
            }
        }
    }

    fun incrementGuestScore(step: Int = 1) {
        viewModelScope.launch {
            mutex.withLock {
                //Logd("SoccerScoreViewModel::incrementGuestScore::$step")
                var scoreGuest = max(_liveScore.value.away.plus(step), 0)
                _liveScore.value = _liveScore.value.copy(away = scoreGuest)
            }
        }
    }

    fun nextPeriod() {
        viewModelScope.launch {
            mutex.withLock {
                var nextPeriod = if (_liveScore.value.period == "1T") "2T" else "1T"
                //Logd("SoccerScoreViewModel::incrementGuestScore::$nextPeriod")
                _liveScore.value = _liveScore.value.copy(period = nextPeriod)
            }
        }
    }

    fun setCommand(command: Command) {
        viewModelScope.launch {
            mutex.withLock {
                _liveScore.value = _liveScore.value.copy(command = command.name)
            }
        }
    }
}