package it.lmqv.livematchcam.strategies

import it.lmqv.livematchcam.services.firebase.EventInfo
import it.lmqv.livematchcam.services.firebase.Match

interface IMatchSyncStrategy {
    val instanceId: String?

    suspend fun initialize(
        onMatchUpdated: (Match) -> Unit,
        onEventInfoUpdated: (EventInfo) -> Unit,
        onPresetKeys: (List<String>) -> Unit,
        onRealtimeDatabaseAvailability: (Boolean) -> Unit
    )

    fun dispose()

    fun updateMatch(match: Match)
    fun updateEventInfo(eventInfo: EventInfo)


}

