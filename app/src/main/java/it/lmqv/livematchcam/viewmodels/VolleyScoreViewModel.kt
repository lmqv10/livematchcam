package it.lmqv.livematchcam.viewmodels

import android.os.Build
import androidx.lifecycle.ViewModel
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.services.firebase.SetScore
import it.lmqv.livematchcam.services.firebase.VolleyScore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.max

class VolleyScoreViewModel: ViewModel() {

    private var _liveScore = MutableStateFlow<VolleyScore>(VolleyScore())
    val liveScore: StateFlow<VolleyScore> = _liveScore

    fun initScore(currentScore: VolleyScore) {
        if (_liveScore.value != currentScore) {
            //Logd("VolleyScoreViewModel::initScore::$currentScore")
            _liveScore.value = currentScore
        }
    }

    fun setMatchLeague(updatedLeague: String) {
        //_matchLeague.value = updatedLeague
        _liveScore.value = _liveScore.value.copy(league = updatedLeague)
    }

    fun incrementHomeScore() {
        val currentScore = _liveScore.value.sets.toMutableList().last()
        var updatedScore = currentScore.copy(home = currentScore.home.plus(1))
        notifyUpdateScore(updatedScore)
    }
    fun decrementHomeScore() {
        val currentScore = _liveScore.value.sets.toMutableList().last()
        var updatedScore = currentScore.copy(home = max(0, currentScore.home.minus(1)))
        notifyUpdateScore(updatedScore)
    }
    fun incrementAwayScore() {
        val currentScore = _liveScore.value.sets.toMutableList().last()
        var updatedScore = currentScore.copy(guest = currentScore.guest.plus(1))
        notifyUpdateScore(updatedScore)
    }
    fun decrementAwayScore() {
        val currentScore = _liveScore.value.sets.toMutableList().last()
        var updatedScore = currentScore.copy(guest = max(0, currentScore.guest.minus(1)))
        notifyUpdateScore(updatedScore)
    }

    fun addNewSet() {
        var updatedSets = _liveScore.value.sets.toMutableList()
        updatedSets.add(SetScore())
        _liveScore.value = _liveScore.value.copy(sets = updatedSets)
    }

    fun removeLastSet() {
        var updatedSets = _liveScore.value.sets.toMutableList()

        if (Build.VERSION.SDK_INT >= 35) {
            updatedSets.removeLast()
        } else {
            updatedSets.removeAt(updatedSets.size - 1)
        }
        _liveScore.value = _liveScore.value.copy(sets = updatedSets)
    }

    fun resetMatch() {
        _liveScore.value = VolleyScore()
    }

    private fun notifyUpdateScore(updatedScore: SetScore) {
        val updatedSets = _liveScore.value.sets.toMutableList().apply {
            if (isNotEmpty()) {
                this[lastIndex] = updatedScore
            }
        }
        Logd("VolleyScoreViewModel::notifyUpdateSetScore::$updatedSets")

        var updatedScore = _liveScore.value.copy(sets = updatedSets)
        Logd("VolleyScoreViewModel::notifyUpdateScore::$updatedScore")

        _liveScore.value = updatedScore

        /*_liveScore.value = _liveScore.value.let { liveScore ->
            val updatedSets = liveScore.sets.apply {
                if (isNotEmpty()) {
                    this[lastIndex] = updatedScore
                }
            }
            liveScore.copy(sets = updatedSets)
        }*/
    }
}