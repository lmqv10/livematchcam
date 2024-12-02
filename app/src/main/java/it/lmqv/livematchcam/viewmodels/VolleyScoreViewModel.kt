package it.lmqv.livematchcam.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

enum class Set {
    SET1,
    SET2,
    SET3,
    SET4,
    SET5
}

class SetScore(var targetPoints: Int) {
    var homePoints : Int = 0
    var awayPoints : Int = 0
}

class VolleyScoreViewModel: ViewModel() {

    private val _currentSet = MutableLiveData(Set.SET1)
    val currentSet: LiveData<Set> = _currentSet
    fun setCurrentSet(updatedSet: Set) {
        _currentSet.value = updatedSet
        _currentScore.value = _score.value?.get(updatedSet)!!
    }

    private val _matchLeague = MutableLiveData("")
    val matchLeague: LiveData<String> = _matchLeague
    fun setMatchLeague(updatedSet: String) {
        _matchLeague.value = updatedSet
    }

    private val _currentScore = MutableLiveData(SetScore(25))
    val currentScore: LiveData<SetScore> = _currentScore

    private val _score = MutableLiveData(mapOf(
        Set.SET1 to SetScore(25),
        Set.SET2 to SetScore(25),
        Set.SET3 to SetScore(25),
        Set.SET4 to SetScore(25),
        Set.SET5 to SetScore(15)
    ))
    val score: LiveData<Map<Set, SetScore>> = _score

    fun incrementHomeScore() {
        val currentScoreMap = _score.value ?: return
        var setScore = currentScoreMap.get(_currentSet.value)!!
        if (setScore.homePoints < setScore.targetPoints
            || setScore.homePoints - setScore.awayPoints < 2) {
            currentScoreMap.get(_currentSet.value)!!.homePoints++
            _currentScore.value = currentScoreMap.get(_currentSet.value)!!
            this._score.value = currentScoreMap
        }
    }
    fun decrementHomeScore() {
        val currentScoreMap = _score.value ?: return
        if (currentScoreMap.get(_currentSet.value)!!.homePoints > 0) {
            currentScoreMap.get(_currentSet.value)!!.homePoints--
            _currentScore.value = currentScoreMap.get(_currentSet.value)!!
            this._score.value = currentScoreMap
        }
    }
    fun incrementAwayScore() {
        val currentScoreMap = _score.value ?: return
        var setScore = currentScoreMap.get(_currentSet.value)!!
        if (setScore.awayPoints < setScore.targetPoints
            || setScore.awayPoints - setScore.homePoints < 2) {
            currentScoreMap.get(_currentSet.value)!!.awayPoints++
            _currentScore.value = currentScoreMap.get(_currentSet.value)!!
            this._score.value = currentScoreMap
        }
    }
    fun decrementAwayScore() {
        val currentScoreMap = _score.value ?: return
        if (currentScoreMap.get(_currentSet.value)!!.awayPoints > 0) {
            currentScoreMap.get(_currentSet.value)!!.awayPoints--
            _currentScore.value = currentScoreMap.get(_currentSet.value)!!
            this._score.value = currentScoreMap
        }
    }

    fun resetMatch() {
        val currentScoreMap = _score.value ?: return
        currentScoreMap.forEach { (_, value) ->
            value.homePoints = 0
            value.awayPoints = 0
        }
        this._score.value = currentScoreMap
        this.setCurrentSet(Set.SET1)
    }
}