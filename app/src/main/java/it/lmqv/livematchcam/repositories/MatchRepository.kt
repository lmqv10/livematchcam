package it.lmqv.livematchcam.repositories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.extensions.toArgbHex
import it.lmqv.livematchcam.factories.Sports
import it.lmqv.livematchcam.services.firebase.IScore
import it.lmqv.livematchcam.services.firebase.Match
import it.lmqv.livematchcam.strategies.IMatchSyncStrategy
import it.lmqv.livematchcam.extensions.Loge
import it.lmqv.livematchcam.services.firebase.EventInfo
import it.lmqv.livematchcam.services.firebase.FirebaseAccountDataContract
import it.lmqv.livematchcam.services.firebase.ScoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID

object MatchRepository {
    val instanceId: String? = UUID.randomUUID().toString()

    fun init() {
        //Logd("$instanceId :: MatchRepository::init::start")
        CoroutineScope(Dispatchers.IO).launch {
            MatchSyncStrategyRepository.syncStrategy.collectLatest { strategy ->
                Logd("$instanceId :: MatchRepository::onSyncStrategy::initialize >> ${strategy}")
                syncStrategy = strategy
                syncStrategy.initialize(
                    onMatchUpdated = { match -> notifyMatchChanges(match) },
                    onEventInfoUpdated = { eventInfo -> notifyEventInfoChanges(eventInfo) },
                    onFirebaseAccountData = { firebaseAccountDataContract -> _firebaseAccountData.value = firebaseAccountDataContract },
                )
                //Logd("$instanceId :: MatchRepository::onSyncStrategy::isRealtimeDatabaseAvailable >> ${isRealtimeDatabaseAvailable.last()}")
            }
        }
        //Logd("$instanceId :: MatchRepository::init::end")
    }

    private lateinit var syncStrategy: IMatchSyncStrategy

    private var currentMatch = Match()

    private var _match = MutableStateFlow<Match>(currentMatch)
    var match: StateFlow<Match> = _match

    private var currentEventInfo = EventInfo()

//    private var _isRealtimeDatabaseAvailable = MutableStateFlow<Boolean>(false)
//    var isRealtimeDatabaseAvailable = _isRealtimeDatabaseAvailable

    private var _firebaseAccountData = MutableStateFlow<FirebaseAccountDataContract>(FirebaseAccountDataContract())
    var firebaseAccountData: StateFlow<FirebaseAccountDataContract>  = _firebaseAccountData

    private val _homeTeam = MutableStateFlow(currentMatch.homeTeam)
    val homeTeam: StateFlow<String> = _homeTeam

    private val _guestTeam = MutableStateFlow(currentMatch.guestTeam)
    val guestTeam: StateFlow<String> = _guestTeam

    private val _homeLogo = MutableStateFlow(currentMatch.homeLogo)
    val homeLogo: StateFlow<String> = _homeLogo

    private val _guestLogo = MutableStateFlow(currentMatch.guestLogo)
    val guestLogo: StateFlow<String> = _guestLogo

    private val _homePrimaryColorHex = MutableStateFlow(currentMatch.homePrimaryColorHex)
    val homePrimaryColorHex: StateFlow<String> = _homePrimaryColorHex

    private val _homeSecondaryColorHex = MutableStateFlow(currentMatch.homeSecondaryColorHex)
    val homeSecondaryColorHex: StateFlow<String> = _homeSecondaryColorHex

    private val _guestPrimaryColorHex = MutableStateFlow(currentMatch.guestPrimaryColorHex)
    val guestPrimaryColorHex: StateFlow<String> = _guestPrimaryColorHex

    private val _guestSecondaryColorHex = MutableStateFlow(currentMatch.guestSecondaryColorHex)
    val guestSecondaryColorHex: StateFlow<String> = _guestSecondaryColorHex

    private val _spotBannerURL = MutableStateFlow(currentMatch.spotBannerURL)
    val spotBannerURL: StateFlow<String> = _spotBannerURL

    private val _spotBannerVisible = MutableStateFlow(currentMatch.spotBannerVisible)
    val spotBannerVisible: StateFlow<Boolean> = _spotBannerVisible

    private val _mainBannerURL = MutableStateFlow(currentMatch.mainBannerURL)
    val mainBannerURL: StateFlow<String> = _mainBannerURL

    private val _mainBannerVisible = MutableStateFlow(currentMatch.mainBannerVisible)
    val mainBannerVisible: StateFlow<Boolean> = _mainBannerVisible

    private val _RTMPServerURI = MutableStateFlow<String?>(null)
    val RTMPServerURI: StateFlow<String?> = _RTMPServerURI
    fun setRTMPServer(serverUri: String?) {
        _RTMPServerURI.value = serverUri
    }

    var currentBroadcastId: String = ""

    private var _sport = MutableStateFlow<Sports>(currentEventInfo.sport)
    val sport: StateFlow<Sports> = _sport
    @Synchronized
    fun setSport(updatedSport: Sports) {
        if (currentEventInfo.sport != updatedSport) {
            Logd("$instanceId :: MatchRepository::setSport:: $updatedSport")
            var score = ScoreFactory.getInitialScore(updatedSport)
            Logd("$instanceId :: MatchRepository::SetInitialScore::$score")
            applyEventInfoChanges(EventInfo(updatedSport, score))
        }
    }

    private var _score = MutableStateFlow<IScore>(currentEventInfo.score)
    var score: StateFlow<IScore> = _score
    @Synchronized
    fun setScore(updatedScore: IScore) {
        try {
            if (currentEventInfo.score != updatedScore) {
                Logd("$instanceId :: MatchRepository::setScore:: $updatedScore")
                val updatedEventInfo = currentEventInfo.copy(score = updatedScore)
                applyEventInfoChanges(updatedEventInfo)
            } else {
                Logd("$instanceId :: MatchRepository::setScore:: no update required")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Loge("MatchRepository::Exception::setScore:: ${e.message.toString()}")
        }
    }

    fun setHomeLogo(updatedLogo: String) {
        applyMatchChanges(currentMatch.copy(homeLogo = updatedLogo))
    }

    fun setGuestLogo(updatedLogo: String) {
        applyMatchChanges(currentMatch.copy(guestLogo = updatedLogo))
    }

    fun setHomeTeam(updatedTeam: String) {
        applyMatchChanges(currentMatch.copy(homeTeam = updatedTeam))
    }

    fun setGuestTeam(updatedTeam: String) {
        applyMatchChanges(currentMatch.copy(guestTeam = updatedTeam))
    }

    fun setHomePrimaryColorHex(updatedColorInt: Int) {
        applyMatchChanges(currentMatch.copy(homePrimaryColorHex = updatedColorInt.toArgbHex()))
    }

    fun setHomeSecondaryColorHex(updatedColorInt: Int) {
        applyMatchChanges(currentMatch.copy(homeSecondaryColorHex = updatedColorInt.toArgbHex()))
    }

    fun setGuestPrimaryColorHex(updatedColorInt: Int) {
        applyMatchChanges(currentMatch.copy(guestPrimaryColorHex = updatedColorInt.toArgbHex()))
    }

    fun setGuestSecondaryColorHex(updatedColorInt: Int) {
        applyMatchChanges(currentMatch.copy(guestSecondaryColorHex = updatedColorInt.toArgbHex()))
    }
    fun setSpotBannerURL(spotBannerURL: String) {
        applyMatchChanges(currentMatch.copy(spotBannerURL = spotBannerURL))
    }

    fun setMainBannerURL(mainBannerURL: String) {
        applyMatchChanges(currentMatch.copy(mainBannerURL = mainBannerURL))
    }

    fun setSpotBannerVisible(spotBannerVisible: Boolean) {
        applyMatchChanges(currentMatch.copy(spotBannerVisible = spotBannerVisible))
    }

    fun setMainBannerVisible(mainBannerVisible: Boolean) {
        applyMatchChanges(currentMatch.copy(mainBannerVisible = mainBannerVisible))
    }

    @Synchronized
    private fun applyMatchChanges(updatedMatch: Match) {
        Logd("$instanceId :: MatchRepository::applyMatchChanges:: $updatedMatch")
        syncStrategy.updateMatch(updatedMatch)
    }

    @Synchronized
    private fun applyEventInfoChanges(updatedEventInfo: EventInfo) {
        Logd("$instanceId :: MatchRepository::applyEventInfoChanges:: $updatedEventInfo")
        syncStrategy.updateEventInfo(updatedEventInfo)
    }

    @Synchronized
    private fun notifyMatchChanges(updatedMatch: Match) {
        try {
            //Logd("$instanceId :: MatchRepository::notifyMatchChanges:: $updatedMatch")
            currentMatch = updatedMatch
            _match.value = updatedMatch

            if (_homeTeam.value != currentMatch.homeTeam) {
                _homeTeam.value = currentMatch.homeTeam
            }
            if (_guestTeam.value != currentMatch.guestTeam) {
                _guestTeam.value = currentMatch.guestTeam
            }
            if (_homeLogo.value != currentMatch.homeLogo) {
                _homeLogo.value = currentMatch.homeLogo
            }
            if (_guestLogo.value != currentMatch.guestLogo) {
                _guestLogo.value = currentMatch.guestLogo
            }

            if (_homePrimaryColorHex.value != currentMatch.homePrimaryColorHex) {
                _homePrimaryColorHex.value = currentMatch.homePrimaryColorHex
            }
            if (_homeSecondaryColorHex.value != currentMatch.homeSecondaryColorHex) {
                _homeSecondaryColorHex.value = currentMatch.homeSecondaryColorHex
            }

            if (_guestPrimaryColorHex.value != currentMatch.guestPrimaryColorHex) {
                _guestPrimaryColorHex.value = currentMatch.guestPrimaryColorHex
            }
            if (_guestSecondaryColorHex.value != currentMatch.guestSecondaryColorHex) {
                _guestSecondaryColorHex.value = currentMatch.guestSecondaryColorHex
            }

            if (_spotBannerURL.value != currentMatch.spotBannerURL) {
                _spotBannerURL.value = currentMatch.spotBannerURL
            }
            if (_spotBannerVisible.value != currentMatch.spotBannerVisible) {
                _spotBannerVisible.value = currentMatch.spotBannerVisible
            }
            if (_mainBannerURL.value != currentMatch.mainBannerURL) {
                _mainBannerURL.value = currentMatch.mainBannerURL
            }
            if (_mainBannerVisible.value != currentMatch.mainBannerVisible) {
                _mainBannerVisible.value = currentMatch.mainBannerVisible
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Loge("$instanceId :: MatchRepository::Exception::notifyMatchChanges:: ${e.message.toString()}")
        }
    }

    @Synchronized
    private fun notifyEventInfoChanges(updatedEventInfo: EventInfo) {
        try {
            Logd("$instanceId :: MatchRepository::notifyEventInfoChanges:: $updatedEventInfo")
            currentEventInfo = updatedEventInfo

            if (_sport.value != currentEventInfo.sport) {
                _sport.value = currentEventInfo.sport
            }

            if (_score.value != currentEventInfo.score) {
                _score.value = currentEventInfo.score
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Loge("$instanceId :: MatchRepository::Exception::notifyEventInfoChanges:: ${e.message.toString()}")
        }
    }
}