package it.lmqv.livematchcam.strategies

import it.lmqv.livematchcam.services.firebase.EventInfo
import it.lmqv.livematchcam.services.firebase.FilterOverlayEvent
import it.lmqv.livematchcam.services.firebase.Match
import it.lmqv.livematchcam.services.firebase.ScoreboardOverlay
import java.util.UUID

class NoMatchSyncStrategy() : IMatchSyncStrategy {
    override val instanceId: String? = UUID.randomUUID().toString()

    override suspend fun initialize(syncDataListenerContract: SyncDataListenerContract) {
        //Logd("$instanceId: NoMatchSyncStrategy::initialize")
    }

    override fun dispose() {
        //Logd("$instanceId: NoMatchSyncStrategy::dispose")
    }

    override fun updateMatch(match: Match) { }
    override fun updateEventInfo(eventInfo: EventInfo) { }
    override fun updateFilter(filter: FilterOverlayEvent) { }
    override fun updateScoreboard(scoreboard: ScoreboardOverlay) { }
}