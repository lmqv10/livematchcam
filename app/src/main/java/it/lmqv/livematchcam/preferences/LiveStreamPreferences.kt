package it.lmqv.livematchcam.preferences

import android.content.Context
import androidx.core.content.edit
import java.util.Calendar

class LiveStreamPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("live_stream_prefs", Context.MODE_PRIVATE)

    private val key_id: String = "id"
    private val key_description: String = "description"
    private val key_background: String = "background"
    private val key_logo_home: String = "logo_home"
    private val key_logo_guest: String = "logo_guest"
    private val key_date = "key_date"

    fun getId(): String? {
        return prefs.getString(key_id, null)
    }

    fun setId(id: String) {
        prefs.edit { putString(key_id, id) }
    }

    fun getDescription(): String? {
        return prefs.getString(key_description, null)
    }
    fun setDescription(description: String) {
        prefs.edit { putString(key_description, description) }
    }

    fun getBackground(): String? {
        return prefs.getString(key_background, null)
    }
    fun setBackground(background: String) {
        prefs.edit { putString(key_background, background) }
    }

    fun getLogoHome(): String {
        return prefs.getString(key_logo_home, null) ?: ""
    }
    fun setLogoHome(logoHome: String) {
        prefs.edit { putString(key_logo_home, logoHome) }
    }

    fun getLogoGuest(): String {
        return prefs.getString(key_logo_guest, null) ?: ""
    }
    fun setLogoGuest(logoGuest: String) {
        prefs.edit { putString(key_logo_guest, logoGuest) }
    }

    fun setDate(date: Calendar) {
        prefs.edit {
            putLong(key_date, date.timeInMillis)
        }
    }

    fun getDate(): Calendar {
        val timeInMillisecond = prefs.getLong(key_date, -1L)
        return if (timeInMillisecond != -1L) {
            Calendar.getInstance().apply { timeInMillis = timeInMillisecond }
        } else {
            Calendar.getInstance()
        }
    }
}
