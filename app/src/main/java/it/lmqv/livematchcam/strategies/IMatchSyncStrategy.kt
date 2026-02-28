package it.lmqv.livematchcam.strategies

import it.lmqv.livematchcam.services.firebase.EventInfo
import it.lmqv.livematchcam.services.firebase.FilterOverlayEvent
import it.lmqv.livematchcam.services.firebase.FirebaseAccountDataContract
import it.lmqv.livematchcam.services.firebase.Match
import it.lmqv.livematchcam.services.firebase.Schedule
import it.lmqv.livematchcam.services.firebase.ScoreboardOverlay

interface SyncDataListenerContract {
    fun onChangeMatch(match: Match)
    fun onChangeEventInfo(eventInfo: EventInfo)
    fun onChangeSchedules(schedules: List<Schedule>)
    fun onChangeScoreboard(scoreboard: ScoreboardOverlay)
    fun onChangeFilters(filters: List<FilterOverlayEvent>)
    //fun onChangeFilter(filter: FilterOverlayEvent)
    fun onChangeFirebaseAccount(account: FirebaseAccountDataContract)
}

interface IMatchSyncStrategy {

    val instanceId: String?

    suspend fun initialize(syncDataListenerContract: SyncDataListenerContract)

    fun dispose()

    fun updateMatch(match: Match)
    fun updateEventInfo(eventInfo: EventInfo)
    fun updateFilter(filter: FilterOverlayEvent)
    fun updateScoreboard(scoreboard: ScoreboardOverlay)
}

