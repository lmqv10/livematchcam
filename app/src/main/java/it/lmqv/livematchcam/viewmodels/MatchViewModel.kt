package it.lmqv.livematchcam.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.extensions.toArgbHex
import it.lmqv.livematchcam.firebase.FirebaseDataManager
import it.lmqv.livematchcam.firebase.Match
import it.lmqv.livematchcam.repositories.AccountRepository
import it.lmqv.livematchcam.repositories.StreamersSettingsRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.max

class MatchViewModel(application: Application) : AndroidViewModel(application) {
    private val firebaseDataManager = FirebaseDataManager.getInstance()
    private var streamersSettingsRepository = StreamersSettingsRepository(application)
    private var firebaseAccountRepository: AccountRepository = AccountRepository(application)

    val instanceId: String? =  UUID.randomUUID().toString()
    private var currentKey: String? = null
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

    //private val _score = MutableLiveData(currentMatch.score)
    //val score: LiveData<Any?> = _score

    fun setHomeTeam(updatedTeam: String) {
        val updatedMatch = currentMatch.copy(homeTeam = updatedTeam)
        firebaseDataManager.updateMatchValue(updatedMatch)
    }
    fun setGuestTeam(updatedTeam: String) {
        val updatedMatch = currentMatch.copy(guestTeam = updatedTeam)
        firebaseDataManager.updateMatchValue(updatedMatch)
    }
    fun setHomeColorHex(updatedColorInt: Int) {
        val updatedMatch = currentMatch.copy(homeColorHex = updatedColorInt.toArgbHex())
        firebaseDataManager.updateMatchValue(updatedMatch)
    }
    fun setGuestColorHex(updatedColorInt: Int) {
        val updatedMatch = currentMatch.copy(guestColorHex = updatedColorInt.toArgbHex())
        firebaseDataManager.updateMatchValue(updatedMatch)
    }

    private val _homeScore = MutableLiveData(0)
    val homeScore: LiveData<Int> = _homeScore
    fun incrementHomeScore(step: Int = 1) {
        _homeScore.value = max((_homeScore.value ?: 0) + step, 0)
    }

    private val _guestScore = MutableLiveData(0)
    val guestScore: LiveData<Int> = _guestScore
    fun incrementGuestScore(step: Int = 1) {
        _guestScore.value = max((_guestScore.value ?: 0) + step, 0)
    }

    private fun notifyChanges(updatedMatch: Match) {
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
            _type.value = currentMatch.type
        }
        /*if (_score.value != currentMatch.score) {
            _score.value = currentMatch.score
        }*/
    }

    /*fun detach() {
        Logd("MatchViewModel::detach")
        //firebaseDataManager.removeMatchValueEventListener()
    }*/

    init {
        Logd("MatchViewModel:: init")
        viewModelScope.launch {
            combine(
                firebaseAccountRepository.accountKey,
                streamersSettingsRepository.getCurrentKey
            ){ accountKey, key -> Pair(accountKey, key) }
            .collect { (firebaseAccountKey, key) ->
                //Logd("MatchViewModel:: $firebaseAccountKey $key")
                if (currentKey != key) {
                    currentKey = key

                    if (!firebaseAccountKey.isNullOrEmpty() && currentKey != null) {
                        //Logd("MatchViewModel:: INITIALIZE $firebaseAccountKey $currentKey")
                        firebaseDataManager
                            //.initialize(firebaseAccountKey)
                            .attachMatchValueEventListener(firebaseAccountKey, currentKey!!) { match ->
                                Logd("MatchViewModel:: onDataChangeCallback")
                                //_match.value = match
                                notifyChanges(match)
                            }
                    } else {
                        Logd("MatchViewModel:: Called??")
                        //firebaseDataManager.removeMatchValueEventListener()
                    }
                }
            }
        }

    }

    /*override fun onCleared() {
        super.onCleared()
        Logd("MatchViewModel:: onCleared")
        //firebaseDataManager.removeMatchValueEventListener()
    }*/
}

/*
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
*/
