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
import it.lmqv.livematchcam.services.firebase.FirebaseAccountDataContract
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
    private lateinit var onFirebaseAccountData: (FirebaseAccountDataContract) -> Unit
    //private lateinit var onRealtimeDatabaseAvailability: (Boolean) -> Unit

    private val syncJob = SupervisorJob()
    private val syncScope = CoroutineScope(syncJob + Dispatchers.IO)

    override suspend fun initialize(
        onMatchUpdated: (Match) -> Unit,
        onEventInfoUpdated: (EventInfo) -> Unit,
        onFirebaseAccountData: (FirebaseAccountDataContract) -> Unit,
        //onRealtimeDatabaseAvailability: (Boolean) -> Unit
    ) {
        //Logd("$instanceId :: FirebaseMatchSyncStrategy::initialize")
        this.onMatchUpdated = onMatchUpdated
        this.onEventInfoUpdated = onEventInfoUpdated
        this.onFirebaseAccountData = onFirebaseAccountData
        //this.onRealtimeDatabaseAvailability = onRealtimeDatabaseAvailability

        syncScope.launch {
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
                            firebaseAccount.logo,
                            firebaseAccount.name,
                            ownedStreams,
                            firebaseAccount.settings,
                            true)
                        onFirebaseAccountData(firebaseAccountDataContract)

                        //onRealtimeDatabaseAvailability(true)
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
                        onFirebaseAccountData(FirebaseAccountDataContract())
                        //onRealtimeDatabaseAvailability(false)
                    })
                } catch (e: Exception) {
                    e.printStackTrace()
                    //Logd("$instanceId :: _isRealtimeDatabaseAvailable.exception")
                    //_isRealtimeDatabaseAvailable.postValue(false)
                    onFirebaseAccountData(FirebaseAccountDataContract())
                    //onRealtimeDatabaseAvailability(false)
                }
            }
        }
        //Logd("$instanceId :: FirebaseMatchSyncStrategy::initialize::end")
    }

    override fun dispose() {
        Logd("$instanceId: FirebaseMatchSyncStrategy::dispose")
        syncJob.cancel()
        FirebaseDataService.detachMatchValueEventListener()
        FirebaseDataService.detachEventInfoValueEventListener()

        //Logd("$instanceId :: _isRealtimeDatabaseAvailable.value = false")
        //_isRealtimeDatabaseAvailable.postValue(false)
        //this.onRealtimeDatabaseAvailability(false)

        //Logd("$instanceId: LocalMatchSyncStrategy::reset to manual match")
        this.onMatchUpdated(Match())
        this.onEventInfoUpdated(EventInfo())
        this.onFirebaseAccountData(FirebaseAccountDataContract())
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
}