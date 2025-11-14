package it.lmqv.livematchcam.strategies

import it.lmqv.livematchcam.services.firebase.EventInfo
import it.lmqv.livematchcam.services.firebase.FirebaseAccountDataContract
import it.lmqv.livematchcam.services.firebase.Match
import it.lmqv.livematchcam.services.firebase.Stream

interface IMatchSyncStrategy {

    //TODO use in initialize
//    interface FirebaseMatchSyncListener {
//        fun onMatchUpdated(match: Match)
//        fun onEventInfoUpdated(eventInfo: EventInfo)
//        fun onPresetKeys(presetKeys: List<Stream>)
//        fun onRealtimeDatabaseAvailability(available: Boolean)
//    }

    val instanceId: String?

    //TODO change initialize with ListenerInterface
    suspend fun initialize(

        //listener: FirebaseMatchSyncListener,
        onMatchUpdated: (Match) -> Unit,
        onEventInfoUpdated: (EventInfo) -> Unit,
        onFirebaseAccountData: (FirebaseAccountDataContract) -> Unit,
        //onRealtimeDatabaseAvailability: (Boolean) -> Unit
    )

    fun dispose()

    fun updateMatch(match: Match)
    fun updateEventInfo(eventInfo: EventInfo)


}

