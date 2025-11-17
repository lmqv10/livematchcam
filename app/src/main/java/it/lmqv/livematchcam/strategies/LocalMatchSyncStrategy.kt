package it.lmqv.livematchcam.strategies

import android.content.Context
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.repositories.AccountRepository
import it.lmqv.livematchcam.services.firebase.EventInfo
import it.lmqv.livematchcam.services.firebase.FirebaseAccountDataContract
import it.lmqv.livematchcam.services.firebase.FirebaseDataService
import it.lmqv.livematchcam.services.firebase.Match
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.UUID

class LocalMatchSyncStrategy(context: Context) : IMatchSyncStrategy {
    override val instanceId: String? = UUID.randomUUID().toString()

    private val firebaseAccountRepository = AccountRepository(context)

    private lateinit var onMatchUpdated: (Match) -> Unit
    private lateinit var onEventInfoUpdated: (EventInfo) -> Unit

    private var syncJob = SupervisorJob()
    private val syncScope = CoroutineScope(syncJob + Dispatchers.IO)

    override suspend fun initialize(
        onMatchUpdated: (Match) -> Unit,
        onEventInfoUpdated: (EventInfo) -> Unit,
        onFirebaseAccountData: (FirebaseAccountDataContract) -> Unit,
        //onRealtimeDatabaseAvailability: (Boolean) -> Unit
    ) {
        //Logd("$instanceId: LocalMatchSyncStrategy::initialize::start")
        this.onMatchUpdated = onMatchUpdated
        this.onEventInfoUpdated = onEventInfoUpdated

        syncScope.launch {
            combine(
                firebaseAccountRepository.accountName,
                firebaseAccountRepository.accountKey
            ) { accountName, accountKey ->
                Pair(accountName, accountKey)
            }.collect { (accountName, accountKey) ->
                Logd("$instanceId :: LocalMatchSyncStrategy::collect:: $accountKey")
                try {
                    FirebaseDataService.authenticateAccount(accountName, accountKey, { firebaseAccount ->
                        Logd("$instanceId :: LocalMatchSyncStrategy :: $accountName - $accountKey")
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
                            firebaseAccount.settings)
                        onFirebaseAccountData(firebaseAccountDataContract)
                    }, {
                        onFirebaseAccountData(FirebaseAccountDataContract())
                    })
                    //onRealtimeDatabaseAvailability(false)
                } catch (e: Exception) {
                    e.printStackTrace()
                    //Logd("$instanceId :: _isRealtimeDatabaseAvailable.exception")
                    onFirebaseAccountData(FirebaseAccountDataContract())
                    //onRealtimeDatabaseAvailability(false)
                }
            }
        }
        //Logd("$instanceId: LocalMatchSyncStrategy::initialize::end")
    }

    override fun dispose() {
        Logd("$instanceId: LocalMatchSyncStrategy::dispose")
        syncJob.cancel()
    }

    override fun updateMatch(match: Match) {
        //Logd("$instanceId: LocalMatchSyncStrategy::updateMatch")
        this.onMatchUpdated(match)
    }
    override fun updateEventInfo(eventInfo: EventInfo) {
        //Logd("$instanceId: LocalMatchSyncStrategy::updateEventInfo")
        this.onEventInfoUpdated(eventInfo)
    }
}