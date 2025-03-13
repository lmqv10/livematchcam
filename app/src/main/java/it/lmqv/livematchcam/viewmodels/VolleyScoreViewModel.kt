package it.lmqv.livematchcam.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import it.lmqv.livematchcam.firebase.SetScore
import it.lmqv.livematchcam.firebase.VolleyScore
import kotlin.math.max

class VolleyScoreViewModel: ViewModel() {

    private val _liveScore = MutableLiveData<VolleyScore?>(null)

    val liveScore: LiveData<VolleyScore?> = _liveScore
    fun initScore(currentScore: VolleyScore) {
        if (_liveScore.value != currentScore) {
            _liveScore.value = currentScore
        }
    }

    //private val _currentSet = MutableLiveData(Set.SET1)
    //val currentSet: LiveData<Set> = _currentSet
    /*fun startSet() {
        //_currentSet.value = updatedSet
        //_currentScore.value = _score.value?.get(updatedSet)!!
        var currentSet = updatedSet.ordinal + 1
        _liveScore.value = _liveScore.value?.copy(currentSet = currentSet)
    }*/

    //private val _matchLeague = MutableLiveData("")
    //val matchLeague: LiveData<String> = _matchLeague
    fun setMatchLeague(updatedLeague: String) {
        //_matchLeague.value = updatedLeague
        _liveScore.value = _liveScore.value?.copy(league = updatedLeague)
    }


    //private val _currentScore = MutableLiveData(SetScore(25))
    //val currentScore: LiveData<SetScore> = _currentScore
    /*private val _score = MutableLiveData(mapOf(
        Set.SET1 to SetScore(25),
        Set.SET2 to SetScore(25),
        Set.SET3 to SetScore(25),
        Set.SET4 to SetScore(25),
        Set.SET5 to SetScore(15)
    ))
    val score: LiveData<Map<Set, SetScore>> = _score*/

    fun incrementHomeScore() {
        val currentScore = _liveScore.value?.sets?.last() ?: SetScore()
        var updatedScore = currentScore.copy(home = currentScore.home.plus(1))
        notifyUpdateScore(updatedScore)
    }
    fun decrementHomeScore() {
        val currentScore = _liveScore.value?.sets?.last() ?: SetScore()
        var updatedScore = currentScore.copy(home = max(0, currentScore.home.minus(1)))
        notifyUpdateScore(updatedScore)
    }
    fun incrementAwayScore() {
        val currentScore = _liveScore.value?.sets?.last() ?: SetScore()
        var updatedScore = currentScore.copy(guest = currentScore.guest.plus(1))
        notifyUpdateScore(updatedScore)
    }
    fun decrementAwayScore() {
        val currentScore = _liveScore.value?.sets?.last() ?: SetScore()
        var updatedScore = currentScore.copy(guest = max(0, currentScore.guest.minus(1)))
        notifyUpdateScore(updatedScore)
    }

    fun addNewSet() {
        var updatedSets = _liveScore.value?.sets
        updatedSets?.add(SetScore())
        _liveScore.value = updatedSets?.let { _liveScore.value?.copy(sets = it) }
    }

    fun removeLastSet() {
        var updatedSets = _liveScore.value?.sets
        updatedSets?.removeLast()
        _liveScore.value = updatedSets?.let { _liveScore.value?.copy(sets = it) }
    }

    fun resetMatch() {
        _liveScore.value = VolleyScore()
    }

    private fun notifyUpdateScore(updatedScore: SetScore) {
        _liveScore.value = _liveScore.value?.let { liveScore ->
            val updatedSets = liveScore.sets.apply {
                if (isNotEmpty()) {
                    this[lastIndex] = updatedScore
                }
            }
            liveScore.copy(sets = updatedSets)
        }
    }
}