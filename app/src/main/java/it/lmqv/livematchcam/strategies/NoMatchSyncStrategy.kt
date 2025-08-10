package it.lmqv.livematchcam.strategies

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import it.lmqv.livematchcam.firebase.EventInfo
import it.lmqv.livematchcam.firebase.Match
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class NoMatchSyncStrategy() : IMatchSyncStrategy {
    override val instanceId: String? = UUID.randomUUID().toString()

    override suspend fun initialize(
        onMatchUpdated: (Match) -> Unit,
        onEventInfoUpdated: (EventInfo) -> Unit
    ) {
        //Logd("$instanceId: NoMatchSyncStrategy::initialize")
    }

    override fun dispose() {
        //Logd("$instanceId: NoMatchSyncStrategy::dispose")
    }

    override fun updateMatch(match: Match) { }
    override fun updateEventInfo(eventInfo: EventInfo) { }

    private val _isRealtimeDatabaseAvailable = MutableLiveData(false)
    override val isRealtimeDatabaseAvailable: Flow<Boolean> = _isRealtimeDatabaseAvailable.asFlow()
}