package it.lmqv.livematchcam.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import it.lmqv.livematchcam.firebase.SoccerScore
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
    private val _liveScore = MutableLiveData<SoccerScore?>(null)
    val liveScore: LiveData<SoccerScore?> = _liveScore
    fun initScore(currentScore: SoccerScore) {
        if (_liveScore.value != currentScore) {
            _liveScore.value = currentScore

        }
    }

    fun incrementHomeScore(step: Int = 1) {
        var scoreHome = max(_liveScore.value?.home?.plus(step) ?: 0, 0)
        _liveScore.value = _liveScore.value?.copy(home = scoreHome)
    }

    fun incrementGuestScore(step: Int = 1) {
        var scoreGuest = max(_liveScore.value?.away?.plus(step) ?: 0, 0)
        _liveScore.value = _liveScore.value?.copy(away = scoreGuest)
    }

    fun nextPeriod() {
        var nextPeriod = if (_liveScore.value?.period == "1T") "2T" else "1T"
        _liveScore.value = _liveScore.value?.copy(period = nextPeriod)
    }

    fun setCommand(command: Command) {
        _liveScore.value = _liveScore.value?.copy(command = command.name)
    }
}