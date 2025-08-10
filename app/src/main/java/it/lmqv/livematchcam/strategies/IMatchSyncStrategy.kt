package it.lmqv.livematchcam.strategies

import it.lmqv.livematchcam.firebase.EventInfo
import it.lmqv.livematchcam.firebase.Match
import kotlinx.coroutines.flow.Flow

interface IMatchSyncStrategy {
    val instanceId: String?

    suspend fun initialize(
        onMatchUpdated: (Match) -> Unit,
        onEventInfoUpdated: (EventInfo) -> Unit
    )
    fun dispose()

    fun updateMatch(match: Match)
    fun updateEventInfo(eventInfo: EventInfo)

    val isRealtimeDatabaseAvailable: Flow<Boolean>
}

