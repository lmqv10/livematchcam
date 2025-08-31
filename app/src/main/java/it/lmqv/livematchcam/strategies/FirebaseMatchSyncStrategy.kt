package it.lmqv.livematchcam.strategies

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.UUID

class FirebaseMatchSyncStrategy(context: Context) : IMatchSyncStrategy {
    override val instanceId: String? = UUID.randomUUID().toString()

    private val firebaseDataRepository = FirebaseDataRepository(context)
    private val firebaseAccountRepository = AccountRepository(context)

    private lateinit var onMatchUpdated: (Match) -> Unit
    private lateinit var onEventInfoUpdated: (EventInfo) -> Unit

    private val syncJob = SupervisorJob()
    private val syncScope = CoroutineScope(syncJob + Dispatchers.IO)

    override suspend fun initialize(
        onMatchUpdated: (Match) -> Unit,
        onEventInfoUpdated: (EventInfo) -> Unit
    ) {
        //Logd("$instanceId :: FirebaseMatchSyncStrategy::initialize")
        this.onMatchUpdated = onMatchUpdated
        this.onEventInfoUpdated = onEventInfoUpdated

        syncScope.launch {
            combine(
                firebaseAccountRepository.accountGoogle,
                firebaseAccountRepository.accountKey,
                firebaseDataRepository.streamName,
                MatchRepository.sport
            ) { accountGoogle, accountKey, streamName, sport ->
                Quadruple(accountGoogle, accountKey, streamName, sport)
            }.collect { (accountGoogle, accountKey, streamName, sport) ->
                //Logd("$instanceId :: FirebaseMatchSyncStrategy::collect:: $key")
                FirebaseDataService.authenticateAccount(accountGoogle, accountKey, {
                    _isRealtimeDatabaseAvailable.value = true

                    FirebaseDataService.attachMatchValueEventListener(streamName) { match ->
                        //Logd("$instanceId :: FirebaseMatchSyncStrategy:: Notify Match Update")
                        onMatchUpdated(match)
                    }

                    FirebaseDataService.attachEventInfoValueEventListener(streamName, sport) { eventInfo ->
                        //Logd("$instanceId :: FirebaseMatchSyncStrategy:: Notify EventInfo Update")
                        onEventInfoUpdated(eventInfo)
                    }

                }, {
                    //Logd("$instanceId: FirebaseMatchSyncStrategy::auth Failed")
                    _isRealtimeDatabaseAvailable.value = false
                })
            }
        }
    }

    override fun dispose() {
        //Logd("$instanceId: FirebaseMatchSyncStrategy::dispose")
        syncJob.cancel()
        _isRealtimeDatabaseAvailable.value = false
        FirebaseDataService.detachMatchValueEventListener()
        FirebaseDataService.detachEventInfoValueEventListener()

        //Logd("$instanceId: LocalMatchSyncStrategy::reset to manual match")
        this.onMatchUpdated(Match())
        this.onEventInfoUpdated(EventInfo())

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

    private val _isRealtimeDatabaseAvailable = MutableLiveData(false)
    override val isRealtimeDatabaseAvailable: Flow<Boolean> = _isRealtimeDatabaseAvailable.asFlow()
}