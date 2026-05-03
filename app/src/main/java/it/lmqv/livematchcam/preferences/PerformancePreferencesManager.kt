package it.lmqv.livematchcam.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import it.lmqv.livematchcam.R

class PerformancePreferencesManager(context: Context) {
    private val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val ctx = context

    fun isReplayEnabled(): Boolean {
        val key = ctx.getString(R.string.enable_replay_key)
        return sharedPreferences.getBoolean(key, false)
    }

    fun isFiltersEnabled(): Boolean {
        val key = ctx.getString(R.string.enable_filters_key)
        return sharedPreferences.getBoolean(key, true)
    }

    fun isScoreboardEnabled(): Boolean {
        val key = ctx.getString(R.string.enable_scoreboard_key)
        return sharedPreferences.getBoolean(key, true)
    }

    fun getKeyframeInterval(): Int {
        val key = ctx.getString(R.string.keyframe_interval_key)
        return sharedPreferences.getString(key, "1")?.toIntOrNull() ?: 1
    }

    fun getSafeModeThreshold(): Int {
        val key = ctx.getString(R.string.safe_mode_threshold_key)
        return sharedPreferences.getString(key, "10")?.toIntOrNull() ?: 10
    }
}
