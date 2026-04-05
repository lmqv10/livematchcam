package it.lmqv.livematchcam.strategies

import android.content.Context
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.repositories.AccountRepository
import it.lmqv.livematchcam.services.firebase.EventInfo
import it.lmqv.livematchcam.services.firebase.FilterOverlayEvent
import it.lmqv.livematchcam.services.firebase.FirebaseAccountDataContract
import it.lmqv.livematchcam.services.firebase.FirebaseAuthService
import it.lmqv.livematchcam.services.firebase.FirebaseDataService
import it.lmqv.livematchcam.services.firebase.Match
import it.lmqv.livematchcam.services.firebase.ScoreboardOverlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.UUID

class LocalMatchSyncStrategy(context: Context) : IMatchSyncStrategy {
    override val instanceId: String? = UUID.randomUUID().toString()

    private val firebaseAccountRepository = AccountRepository(context)

    private lateinit var syncDataListenerContract: SyncDataListenerContract

    private var syncJob : Job? = null
    private val syncScope = CoroutineScope(Dispatchers.IO)

    override suspend fun initialize(
        syncDataListenerContract: SyncDataListenerContract
    ) {
        //Logd("$instanceId: LocalMatchSyncStrategy::initialize::start")
        this.syncDataListenerContract = syncDataListenerContract

        syncJob?.cancel()
        syncJob = syncScope.launch {
            combine(
                firebaseAccountRepository.accountName,
                firebaseAccountRepository.accountKey
            ) { accountName, accountKey ->
                Pair(accountName, accountKey)
            }.collect { (accountName, accountKey) ->
                Logd("$instanceId :: LocalMatchSyncStrategy::collect:: $accountKey")
                try {
                    val result = FirebaseAuthService.authenticateAccount(accountName, accountKey)

                    result.onSuccess { firebaseAccount ->
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
                        syncDataListenerContract.onChangeFirebaseAccount(firebaseAccountDataContract)
                    }

                    result.onFailure {
                        syncDataListenerContract.onChangeFirebaseAccount(FirebaseAccountDataContract())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    //Logd("$instanceId :: _isRealtimeDatabaseAvailable.exception")
                    syncDataListenerContract.onChangeFirebaseAccount(FirebaseAccountDataContract())
                }
            }
        }
        //Logd("$instanceId: LocalMatchSyncStrategy::initialize::end")
    }

    override fun dispose() {
        Logd("$instanceId: LocalMatchSyncStrategy::dispose")
        syncJob?.cancel()
    }

    override fun updateMatch(match: Match) {
        //Logd("$instanceId: LocalMatchSyncStrategy::updateMatch")
        syncDataListenerContract.onChangeMatch(match)
    }
    override fun updateEventInfo(eventInfo: EventInfo) {
        //Logd("$instanceId: LocalMatchSyncStrategy::updateEventInfo")
        syncDataListenerContract.onChangeEventInfo(eventInfo)
    }
    override fun updateFilter(filter: FilterOverlayEvent) {
//        val current = it.lmqv.livematchcam.repositories.MatchRepository.filters.value.toMutableList()
//        val index = current.indexOfFirst { it.position == filter.position }
//        if (index >= 0) current[index] = filter else current.add(filter)
        syncDataListenerContract.onChangeFilters(listOf())
    }

    override fun updateScoreboard(scoreboard: ScoreboardOverlay) {
        syncDataListenerContract.onChangeScoreboard(scoreboard)
    }
}
