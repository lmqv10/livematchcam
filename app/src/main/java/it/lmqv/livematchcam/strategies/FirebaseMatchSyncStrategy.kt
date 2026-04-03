package it.lmqv.livematchcam.strategies

import android.content.Context
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.services.firebase.EventInfo
import it.lmqv.livematchcam.services.firebase.EventInfoData
import it.lmqv.livematchcam.services.firebase.FirebaseDataService
import it.lmqv.livematchcam.services.firebase.Match
import it.lmqv.livematchcam.services.firebase.Quadruple
import it.lmqv.livematchcam.services.firebase.ScoreFactory
import it.lmqv.livematchcam.repositories.AccountRepository
import it.lmqv.livematchcam.repositories.FirebaseDataRepository
import it.lmqv.livematchcam.repositories.MatchRepository
import it.lmqv.livematchcam.services.firebase.FilterOverlayEvent
import it.lmqv.livematchcam.services.firebase.FirebaseAccountDataContract
import it.lmqv.livematchcam.services.firebase.Schedule
import it.lmqv.livematchcam.services.firebase.ScoreboardOverlay
import it.lmqv.livematchcam.services.firebase.listeners.OverlaysValueListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.UUID

class FirebaseMatchSyncStrategy(context: Context) :
    IMatchSyncStrategy {
    override val instanceId: String? = UUID.randomUUID().toString()

    private val firebaseDataRepository = FirebaseDataRepository(context)
    private val firebaseAccountRepository = AccountRepository(context)

    private lateinit var syncDataListenerContract: SyncDataListenerContract

    private var syncJob : Job? = null
    private val syncScope = CoroutineScope(Dispatchers.IO)

    override suspend fun initialize(
        syncDataListenerContract: SyncDataListenerContract
    ) {
        //Logd("$instanceId :: FirebaseMatchSyncStrategy::initialize")
        this.syncDataListenerContract = syncDataListenerContract

        syncJob?.cancel()
        syncJob = syncScope.launch {
            combine(
                firebaseAccountRepository.accountName,
                firebaseAccountRepository.accountKey,
                firebaseDataRepository.streamName,
                MatchRepository.sport
            ) { accountName, accountKey, streamName, sport ->
                Quadruple(accountName, accountKey, streamName, sport)
            }.collect { (accountName, accountKey, streamName, sport) ->
                Logd("$instanceId :: FirebaseMatchSyncStrategy::collect:: $accountKey")
                try {
                    FirebaseDataService.authenticateAccount(accountName, accountKey, { firebaseAccount ->
                        Logd("$instanceId :: FirebaseMatchSyncStrategy :: $accountName - $accountKey")
                        //Logd("$instanceId :: _isRealtimeDatabaseAvailable.value = true")

                        var ownedStreams =
                            firebaseAccount.streams
                            .filter { x ->
                                firebaseAccount.admin == accountName ||
                                x.owners.contains(accountName)
                            }
                        var firebaseAccountDataContract = FirebaseAccountDataContract(
                            firebaseAccount.guid,
                            firebaseAccount.logo,
                            firebaseAccount.name,
                            ownedStreams,
                            firebaseAccount.settings,
                            true)
                        syncDataListenerContract.onChangeFirebaseAccount(firebaseAccountDataContract)

                        FirebaseDataService.attachMatchValueEventListener(streamName) { match ->
                            //Logd("$instanceId :: FirebaseMatchSyncStrategy:: Notify Match Update")
                            syncDataListenerContract.onChangeMatch(match)
                        }

                        FirebaseDataService.attachEventInfoValueEventListener(streamName, sport) { eventInfo ->
                            //Logd("$instanceId :: FirebaseMatchSyncStrategy:: Notify EventInfo Update")
                            syncDataListenerContract.onChangeEventInfo(eventInfo)
                        }

                        FirebaseDataService.attachSchedulesValueEventListener(streamName) {
                            syncDataListenerContract.onChangeSchedules(it)
                        }

                        FirebaseDataService.attachOverlaysValueEventListener(streamName, object : OverlaysValueListener {
//                            override fun onChangeFilter(filter: FilterOverlayEvent) {
//                                Logd("$instanceId :: FirebaseMatchSyncStrategy:: onChangeFilter $filter")
//                                syncDataListenerContract.onChangeFilter(filter)
//                            }

                            override fun onChangeScoreboard(scoreboard: ScoreboardOverlay) {
                                Logd("$instanceId :: FirebaseMatchSyncStrategy:: scoreboard $scoreboard")
                                syncDataListenerContract.onChangeScoreboard(scoreboard)
                            }

                            override fun onChangeFilters(filters: List<FilterOverlayEvent>) {
                                Logd("$instanceId :: FirebaseMatchSyncStrategy:: filters $filters")
                                syncDataListenerContract.onChangeFilters(filters)
                            }
                        })
                    }, {
                        syncDataListenerContract.onChangeFirebaseAccount(FirebaseAccountDataContract())
                    })
                } catch (e: Exception) {
                    e.printStackTrace()
                    syncDataListenerContract.onChangeFirebaseAccount(FirebaseAccountDataContract())
                }
            }
        }
        //Logd("$instanceId :: FirebaseMatchSyncStrategy::initialize::end")
    }

    override fun dispose() {
        Logd("$instanceId: FirebaseMatchSyncStrategy::dispose")
        syncJob?.cancel()
        FirebaseDataService.detachMatchValueEventListener()
        FirebaseDataService.detachEventInfoValueEventListener()
        FirebaseDataService.detachSchedulesValueEventListener()
        FirebaseDataService.detachOverlaysValueEventListener()

        //Logd("$instanceId: LocalMatchSyncStrategy::reset to manual match")
        syncDataListenerContract.onChangeMatch(Match())
        syncDataListenerContract.onChangeEventInfo(EventInfo())
        syncDataListenerContract.onChangeSchedules(listOf<Schedule>())
        syncDataListenerContract.onChangeScoreboard(ScoreboardOverlay())
        syncDataListenerContract.onChangeFilters(listOf())
        syncDataListenerContract.onChangeFirebaseAccount(FirebaseAccountDataContract())
    }

    @Synchronized
    override fun updateMatch(match: Match) {
        FirebaseDataService.updateMatchValue(match)
    }

    @Synchronized
    override fun updateEventInfo(eventInfo: EventInfo) {
        var score = eventInfo.score
        val scoreMap = ScoreFactory.buildMap(score)
        val eventInfoData = EventInfoData(eventInfo.sport.name, scoreMap)

        FirebaseDataService.updateEventInfoValue(eventInfoData)
    }

    @Synchronized
    override fun updateFilter(filter: FilterOverlayEvent) {
        FirebaseDataService.updateFilterValue(filter)
    }

    override fun updateScoreboard(scoreboard: ScoreboardOverlay) {
        FirebaseDataService.updateScoreboardValue(scoreboard)
    }
}