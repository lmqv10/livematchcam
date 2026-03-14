package it.lmqv.livematchcam.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import it.lmqv.livematchcam.R

class ReplayPreferencesManager(private val context: Context) {

    private val KEY_REPLAY_DURATION = context.getString(R.string.replay_duration_key)
    private val KEY_REPLAY_SPEED = context.getString(R.string.replay_speed_key)
    private val KEY_QUICK_REPLAY_DURATION = context.getString(R.string.replay_quick_duration_key)
    private var sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    fun getBufferDurationSeconds(): Int {
        val durationString = sharedPreferences.getString(KEY_REPLAY_DURATION, "30")
        return durationString?.toIntOrNull() ?: 30
    }

    fun getQuickReplayDurationSeconds(): Int {
        val durationString = sharedPreferences.getString(KEY_QUICK_REPLAY_DURATION, "20")
        return durationString?.toIntOrNull() ?: 20
    }

    fun getReplaySpeed(): Float {
        val speedString = sharedPreferences.getString(KEY_REPLAY_SPEED, "0.5")
        return speedString?.toFloatOrNull() ?: 0.5f
    }

    fun setReplaySpeed(speed: Float) {
        sharedPreferences.edit().putString(KEY_REPLAY_SPEED, speed.toString()).apply()
    }
}
