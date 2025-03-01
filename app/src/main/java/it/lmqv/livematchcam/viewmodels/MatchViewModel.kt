package it.lmqv.livematchcam.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import it.lmqv.livematchcam.extensions.toArgbHex
import it.lmqv.livematchcam.factories.Sports
import it.lmqv.livematchcam.factories.SportsFactory
import it.lmqv.livematchcam.firebase.FirebaseDataManager
import it.lmqv.livematchcam.firebase.IScore
import it.lmqv.livematchcam.firebase.Match
import it.lmqv.livematchcam.firebase.Quadruple
import it.lmqv.livematchcam.firebase.ScoreFactory
import it.lmqv.livematchcam.repositories.AccountRepository
import it.lmqv.livematchcam.repositories.StreamersSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MatchViewModel(application: Application) : AndroidViewModel(application) {
    //val instanceId: String? =  UUID.randomUUID().toString()

    //private var currentKey: String? = null
    private val firebaseDataManager = FirebaseDataManager.getInstance()
    private var streamersSettingsRepository = StreamersSettingsRepository(application)
    private var sportsFactory = SportsFactory

    private var firebaseAccountRepository: AccountRepository = AccountRepository(application)
    private var currentMatch = Match()

    private val _homeTeam = MutableLiveData(currentMatch.homeTeam)
    val homeTeam: LiveData<String> = _homeTeam

    private val _guestTeam = MutableLiveData(currentMatch.guestTeam)
    val guestTeam: LiveData<String> = _guestTeam

    private val _homeColorHex = MutableLiveData(currentMatch.homeColorHex)
    val homeColorHex: LiveData<String> = _homeColorHex

    private val _guestColorHex = MutableLiveData(currentMatch.guestColorHex)
    val guestColorHex: LiveData<String> = _guestColorHex

    private val _type = MutableLiveData(currentMatch.type)
    val type: LiveData<String> = _type
    fun setType(updatedType: String) {
        val sport: Sports = enumValues<Sports>().find { it.name == updatedType } ?: Sports.SOCCER
        val updatedMatch = currentMatch.copy(type = sport.name)
        applyMatchChanges(updatedMatch)
    }

    private val _score = MutableLiveData<IScore?>(null)
    val score: LiveData<IScore?> = _score
    fun setScore(updatedScore: IScore?) {
        if (updatedScore != null) {
            applyScoreChanges(updatedScore)
        }
    }

    fun setHomeTeam(updatedTeam: String) {
        val updatedMatch = currentMatch.copy(homeTeam = updatedTeam)
        applyMatchChanges(updatedMatch)
    }
    fun setGuestTeam(updatedTeam: String) {
        val updatedMatch = currentMatch.copy(guestTeam = updatedTeam)
        applyMatchChanges(updatedMatch)
    }
    fun setHomeColorHex(updatedColorInt: Int) {
        val updatedMatch = currentMatch.copy(homeColorHex = updatedColorInt.toArgbHex())
        applyMatchChanges(updatedMatch)
    }
    fun setGuestColorHex(updatedColorInt: Int) {
        val updatedMatch = currentMatch.copy(guestColorHex = updatedColorInt.toArgbHex())
        applyMatchChanges(updatedMatch)
    }

    private val _isRealtimeDatabaseAvailable = MutableLiveData(false)
    val isRealtimeDatabaseAvailable: LiveData<Boolean> = _isRealtimeDatabaseAvailable

    init {
        //Logd("MatchViewModel:: init")
        /*viewModelScope.launch {
            streamersSettingsRepository.getSport.collect {
                _type.value = it.name
                sportsFactory.set(it)
            }
        }*/

        viewModelScope.launch {
            combine(
                streamersSettingsRepository.getSport,
                firebaseAccountRepository.accountGoogle,
                firebaseAccountRepository.accountKey,
                streamersSettingsRepository.getCurrentKey)
            { sport, accountGoogle, accountKey, currentKey -> Quadruple(sport, accountGoogle, accountKey, currentKey) }
            .collect { (sport, accountGoogle, accountKey, currentKey) ->
                //Logd("MatchViewModel:: $sport | $accountGoogle | $accountKey | $currentKey")

                if (_type.value != sport.name) {
                    sportsFactory.set(sport)
                    _type.value = sport.name
                }

                firebaseDataManager.authenticateAccount(accountGoogle, accountKey, {
                    _isRealtimeDatabaseAvailable.value = true
                    firebaseDataManager.attachMatchValueEventListener(currentKey, sport) { match, score ->
                        //Logd("MatchViewModel:: onDataChangeCallback")
                        notifyMatchChanges(match)
                        notifyScoreChanges(score)
                    }
                },
                {
                    _isRealtimeDatabaseAvailable.value = false
                })

            }
        }
    }

    /*override fun onCleared() {
        super.onCleared()
        Logd("MatchViewModel:: onCleared")
    }*/

    private fun applyMatchChanges(updatedMatch: Match) {
        if (_isRealtimeDatabaseAvailable.value == true) {
            firebaseDataManager.updateMatchValue(updatedMatch)
        } else {
            notifyMatchChanges(updatedMatch)
        }
    }

    private fun applyScoreChanges(updatedScore: IScore) {
        if (_isRealtimeDatabaseAvailable.value == true) {
            val updatedScoreMap = ScoreFactory.getInstance().buildMap(updatedScore)
            firebaseDataManager.updateScoreValue(updatedScoreMap)
        } else {
            notifyScoreChanges(updatedScore)
        }
    }

    private fun notifyScoreChanges(score: IScore) {
        if (_score.value != score) {
            _score.value = score
        }
    }

    private fun notifyMatchChanges(updatedMatch: Match) {
        currentMatch = updatedMatch

        if (_homeTeam.value != currentMatch.homeTeam) {
            _homeTeam.value = currentMatch.homeTeam
        }
        if (_guestTeam.value != currentMatch.guestTeam) {
            _guestTeam.value = currentMatch.guestTeam
        }
        if (_homeColorHex.value != currentMatch.homeColorHex) {
            _homeColorHex.value = currentMatch.homeColorHex
        }
        if (_guestColorHex.value != currentMatch.guestColorHex) {
            _guestColorHex.value = currentMatch.guestColorHex
        }
        if (_type.value != currentMatch.type) {
            val sport: Sports = enumValues<Sports>().find { it.name == currentMatch.type } ?: Sports.SOCCER
            viewModelScope.launch(Dispatchers.IO) {
                streamersSettingsRepository.setSport(sport)
            }
        }
    }
}