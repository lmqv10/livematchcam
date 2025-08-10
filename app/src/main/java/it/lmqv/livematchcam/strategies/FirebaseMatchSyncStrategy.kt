package it.lmqv.livematchcam.strategies

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import it.lmqv.livematchcam.firebase.EventInfo
import it.lmqv.livematchcam.firebase.EventInfoData
import it.lmqv.livematchcam.firebase.FirebaseDataManager
import it.lmqv.livematchcam.firebase.Match
import it.lmqv.livematchcam.firebase.Quadruple
import it.lmqv.livematchcam.firebase.ScoreFactory
import it.lmqv.livematchcam.repositories.AccountRepository
import it.lmqv.livematchcam.repositories.MatchRepository
import it.lmqv.livematchcam.repositories.StreamersSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.UUID

class FirebaseMatchSyncStrategy(context: Context) : IMatchSyncStrategy {
    override val instanceId: String? = UUID.randomUUID().toString()

    private val streamersSettingsRepository = StreamersSettingsRepository(context)
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
                streamersSettingsRepository.getCurrentKey,
                MatchRepository.sport
            ) { accountGoogle, accountKey, key, sport ->
                Quadruple(accountGoogle, accountKey, key, sport)
            }.collect { (accountGoogle, accountKey, key, sport) ->
                //Logd("$instanceId :: FirebaseMatchSyncStrategy::collect:: $key")
                FirebaseDataManager.authenticateAccount(accountGoogle, accountKey, {
                    _isRealtimeDatabaseAvailable.value = true

                    FirebaseDataManager.attachMatchValueEventListener(key) { match ->
                        //Logd("$instanceId :: FirebaseMatchSyncStrategy:: Notify Match Update")
                        onMatchUpdated(match)
                    }

                    FirebaseDataManager.attachEventInfoValueEventListener(key, sport) { eventInfo ->
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
        FirebaseDataManager.detachMatchValueEventListener()
        FirebaseDataManager.detachEventInfoValueEventListener()

        //Logd("$instanceId: LocalMatchSyncStrategy::reset to manual match")
        this.onMatchUpdated(Match())
        this.onEventInfoUpdated(EventInfo())

    }

    override fun updateMatch(match: Match) {
        FirebaseDataManager.updateMatchValue(match)
    }

    override fun updateEventInfo(eventInfo: EventInfo) {
        var score = eventInfo.score
        val scoreMap = ScoreFactory.buildMap(score)
        val eventInfoData = EventInfoData(eventInfo.sport.name, scoreMap)

        FirebaseDataManager.updateEventInfoValue(eventInfoData)
    }

    private val _isRealtimeDatabaseAvailable = MutableLiveData(false)
    override val isRealtimeDatabaseAvailable: Flow<Boolean> = _isRealtimeDatabaseAvailable.asFlow()
}