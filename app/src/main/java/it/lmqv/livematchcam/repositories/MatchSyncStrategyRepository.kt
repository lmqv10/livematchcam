package it.lmqv.livematchcam.repositories

import android.content.Context
import it.lmqv.livematchcam.utils.SyncStrategy
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.strategies.IMatchSyncStrategy
import it.lmqv.livematchcam.strategies.MatchSyncStrategyFactory
import it.lmqv.livematchcam.strategies.NoMatchSyncStrategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object MatchSyncStrategyRepository {

    private var currentSyncStrategyKind : SyncStrategy? = null

    private val _syncStrategy = MutableStateFlow<IMatchSyncStrategy>(NoMatchSyncStrategy())
    var syncStrategy: StateFlow<IMatchSyncStrategy> = _syncStrategy.asStateFlow()

    fun initialize(context: Context, syncStrategy: SyncStrategy) {
        try {
            if (currentSyncStrategyKind == null || currentSyncStrategyKind != syncStrategy) {
                //Logd("MatchViewModel::onSyncStrategy:: change strategy:: $currentSyncStrategyKind to $syncStrategy")
                this.currentSyncStrategyKind = syncStrategy

                this._syncStrategy.value.dispose()
                this._syncStrategy.value = MatchSyncStrategyFactory().get(context, syncStrategy)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}