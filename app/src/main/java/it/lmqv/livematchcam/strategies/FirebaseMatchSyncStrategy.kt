package it.lmqv.livematchcam.strategies

import android.content.Context
import it.lmqv.livematchcam.services.firebase.EventInfo
import it.lmqv.livematchcam.services.firebase.EventInfoData
import it.lmqv.livematchcam.services.firebase.FirebaseDataService
import it.lmqv.livematchcam.services.firebase.Match
import it.lmqv.livematchcam.services.firebase.Quadruple
import it.lmqv.livematchcam.services.firebase.ScoreFactory
import it.lmqv.livematchcam.repositories.AccountRepository
import it.lmqv.livematchcam.repositories.FirebaseDataRepository
import it.lmqv.livematchcam.repositories.MatchRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.UUID

class FirebaseMatchSyncStrategy(context: Context) : IMatchSyncStrategy {
    override val instanceId: String? = UUID.randomUUID().toString()

    private val firebaseDataRepository = FirebaseDataRepository(context)
    private val firebaseAccountRepository = AccountRepository(context)

    private lateinit var onMatchUpdated: (Match) -> Unit
    private lateinit var onEventInfoUpdated: (EventInfo) -> Unit
    private lateinit var onPresetKeys: (List<String>) -> Unit
    private lateinit var onRealtimeDatabaseAvailability: (Boolean) -> Unit

    private val syncJob = SupervisorJob()
    private val syncScope = CoroutineScope(syncJob + Dispatchers.IO)

    override suspend fun initialize(
        onMatchUpdated: (Match) -> Unit,
        onEventInfoUpdated: (EventInfo) -> Unit,
        onPresetKeys: (List<String>) -> Unit,
        onRealtimeDatabaseAvailability: (Boolean) -> Unit
    ) {
        //Logd("$instanceId :: FirebaseMatchSyncStrategy::initialize")
        this.onMatchUpdated = onMatchUpdated
        this.onEventInfoUpdated = onEventInfoUpdated
        this.onPresetKeys = onPresetKeys
        this.onRealtimeDatabaseAvailability = onRealtimeDatabaseAvailability

        syncScope.launch {
            combine(
                firebaseAccountRepository.accountName,
                firebaseAccountRepository.accountKey,
                firebaseDataRepository.streamName,
                MatchRepository.sport
            ) { accountGoogle, accountKey, streamName, sport ->
                Quadruple(accountGoogle, accountKey, streamName, sport)
            }.collect { (accountGoogle, accountKey, streamName, sport) ->
                //Logd("$instanceId :: FirebaseMatchSyncStrategy::collect:: $key")
                try {
                    FirebaseDataService.authenticateAccount(accountGoogle, accountKey, { firebaseAccount ->
                        //Logd("$instanceId :: $accountGoogle - $accountKey")
                        //Logd("$instanceId :: _isRealtimeDatabaseAvailable.value = true")
                        onPresetKeys(firebaseAccount.matches.keys.toList())

                        onRealtimeDatabaseAvailability(true)
                        //_isRealtimeDatabaseAvailable.postValue(true)

                        FirebaseDataService.attachMatchValueEventListener(streamName) { match ->
                            //Logd("$instanceId :: FirebaseMatchSyncStrategy:: Notify Match Update")
                            onMatchUpdated(match)
                        }

                        FirebaseDataService.attachEventInfoValueEventListener(streamName, sport) { eventInfo ->
                            //Logd("$instanceId :: FirebaseMatchSyncStrategy:: Notify EventInfo Update")
                            onEventInfoUpdated(eventInfo)
                        }

                    }, {
                        //_isRealtimeDatabaseAvailable.postValue(false)
                        onRealtimeDatabaseAvailability(false)
                    })
                } catch (e: Exception) {
                    e.printStackTrace()
                    //Logd("$instanceId :: _isRealtimeDatabaseAvailable.exception")
                    //_isRealtimeDatabaseAvailable.postValue(false)
                    onRealtimeDatabaseAvailability(false)
                }

            }
        }
    }

    override fun dispose() {
        //Logd("$instanceId: FirebaseMatchSyncStrategy::dispose")
        syncJob.cancel()
        FirebaseDataService.detachMatchValueEventListener()
        FirebaseDataService.detachEventInfoValueEventListener()

        //Logd("$instanceId :: _isRealtimeDatabaseAvailable.value = false")
        //_isRealtimeDatabaseAvailable.postValue(false)
        this.onRealtimeDatabaseAvailability(false)

        //Logd("$instanceId: LocalMatchSyncStrategy::reset to manual match")
        this.onMatchUpdated(Match())
        this.onEventInfoUpdated(EventInfo())
        this.onPresetKeys(emptyList<String>())
    }

    override fun updateMatch(match: Match) {
        FirebaseDataService.updateMatchValue(match)
    }

    override fun updateEventInfo(eventInfo: EventInfo) {
        var score = eventInfo.score
        val scoreMap = ScoreFactory.buildMap(score)
        val eventInfoData = EventInfoData(eventInfo.sport.name, scoreMap)

        FirebaseDataService.updateEventInfoValue(eventInfoData)
    }

//    private val _isRealtimeDatabaseAvailable = MutableLiveData(false)
//    override val isRealtimeDatabaseAvailable: Flow<Boolean> = _isRealtimeDatabaseAvailable.asFlow()
//
//    private val _presetKeys = MutableStateFlow(emptyList<String>())
//    override val presetKeys: Flow<List<String>> = _presetKeys
}