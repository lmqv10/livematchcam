package it.lmqv.livematchcam.strategies

import it.lmqv.livematchcam.services.firebase.EventInfo
import it.lmqv.livematchcam.services.firebase.FirebaseAccountDataContract
import it.lmqv.livematchcam.services.firebase.Match
import it.lmqv.livematchcam.services.firebase.Stream
import java.util.UUID

class NoMatchSyncStrategy() : IMatchSyncStrategy {
    override val instanceId: String? = UUID.randomUUID().toString()


    override suspend fun initialize(
        onMatchUpdated: (Match) -> Unit,
        onEventInfoUpdated: (EventInfo) -> Unit,
        onFirebaseAccountData: (FirebaseAccountDataContract) -> Unit,
        //onRealtimeDatabaseAvailability: (Boolean) -> Unit
    ) {
        //Logd("$instanceId: NoMatchSyncStrategy::initialize")
    }

    override fun dispose() {
        //Logd("$instanceId: NoMatchSyncStrategy::dispose")
    }

    override fun updateMatch(match: Match) { }
    override fun updateEventInfo(eventInfo: EventInfo) { }

//    private val _isRealtimeDatabaseAvailable = MutableLiveData(false)
//    override val isRealtimeDatabaseAvailable: Flow<Boolean> = _isRealtimeDatabaseAvailable.asFlow()
//
//    private val _presetKeys = MutableStateFlow(emptyList<String>())
//    override val presetKeys: Flow<List<String>> = _presetKeys
}