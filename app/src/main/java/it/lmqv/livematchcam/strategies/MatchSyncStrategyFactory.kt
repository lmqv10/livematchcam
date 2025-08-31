package it.lmqv.livematchcam.strategies

import android.content.Context
import it.lmqv.livematchcam.utils.SyncStrategy

class MatchSyncStrategyFactory {
    private var localMatchSyncStrategy: LocalMatchSyncStrategy? = null
    private var firebaseMatchSyncStrategy: FirebaseMatchSyncStrategy? = null

    fun get(context: Context, syncStrategy: SyncStrategy): IMatchSyncStrategy {
        //Logd("MatchSyncStrategyFactory::get::$syncStrategy")

        return when (syncStrategy) {
            SyncStrategy.LOCAL -> {
                localMatchSyncStrategy ?: LocalMatchSyncStrategy(context).also {
                    localMatchSyncStrategy = it
                }
            }
            SyncStrategy.FIREBASE -> {
                firebaseMatchSyncStrategy ?: FirebaseMatchSyncStrategy(context).also {
                    firebaseMatchSyncStrategy = it
                }
            }
        }
    }
}