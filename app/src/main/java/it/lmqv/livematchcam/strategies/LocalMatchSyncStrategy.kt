package it.lmqv.livematchcam.strategies

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import it.lmqv.livematchcam.services.firebase.EventInfo
import it.lmqv.livematchcam.services.firebase.Match
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID

class LocalMatchSyncStrategy(context: Context) : IMatchSyncStrategy {
    override val instanceId: String? = UUID.randomUUID().toString()

    //override val isRealtimeDatabaseAvailable: Boolean = false
    //override var presetKeys: List<String> = emptyList()

    private lateinit var onMatchUpdated: (Match) -> Unit
    private lateinit var onEventInfoUpdated: (EventInfo) -> Unit

    //private val streamersSettingsRepository = ServerSettingsRepository(context)

    //private var syncJob = SupervisorJob()
    //private val syncScope = CoroutineScope(syncJob + Dispatchers.IO)

    override suspend fun initialize(
        onMatchUpdated: (Match) -> Unit,
        onEventInfoUpdated: (EventInfo) -> Unit,
        onPresetKeys: (List<String>) -> Unit,
        onRealtimeDatabaseAvailability: (Boolean) -> Unit
    ) {
        //Logd("$instanceId: LocalMatchSyncStrategy::initialize::start")
        this.onMatchUpdated = onMatchUpdated
        this.onEventInfoUpdated = onEventInfoUpdated

        /*syncScope.launch {
            streamersSettingsRepository.getSport.collectLatest { sport ->
                Logd("$instanceId: LocalMatchSyncStrategy::collectLatest::$sport")
                var defaultScore = ScoreFactory.getInitialScore(sport)
                val eventInfo = EventInfo(sport, defaultScore)
                Logd("$instanceId: LocalMatchSyncStrategy::onEventInfoUpdated::$eventInfo")
                //onEventInfoUpdated(eventInfo)
            }
        }*/
        //Logd("$instanceId: LocalMatchSyncStrategy::initialize::end")
    }

    override fun dispose() {
        //Logd("$instanceId: LocalMatchSyncStrategy::dispose")
        //syncJob.cancel()
    }

    override fun updateMatch(match: Match) {
        //Logd("$instanceId: LocalMatchSyncStrategy::updateMatch")
        this.onMatchUpdated(match)
    }
    override fun updateEventInfo(eventInfo: EventInfo) {
        //Logd("$instanceId: LocalMatchSyncStrategy::updateEventInfo")
        /*syncScope.launch {
            Logd("$instanceId: LocalMatchSyncStrategy::setSport")
            streamersSettingsRepository.setSport(eventInfo.sport)
        }*/
        this.onEventInfoUpdated(eventInfo)
    }

//    private val _isRealtimeDatabaseAvailable = MutableLiveData(false)
//    override val isRealtimeDatabaseAvailable: Flow<Boolean> = _isRealtimeDatabaseAvailable.asFlow()
//
//    private val _presetKeys = MutableStateFlow(emptyList<String>())
//    override val presetKeys: Flow<List<String>> = _presetKeys
}