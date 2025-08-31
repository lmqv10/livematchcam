package it.lmqv.livematchcam.repositories

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.serverSettingsDatastore: DataStore<Preferences> by preferencesDataStore(name = "server_settings")

class ServerSettingsRepository(private val context: Context) {
    private val CURRENT_KEY = stringPreferencesKey("CurrentKey")
    private val CURRENT_SERVER = stringPreferencesKey("CurrentServer")

    val getCurrentKey: Flow<String?> = context.serverSettingsDatastore.data.map {
        preferences -> preferences[CURRENT_KEY]
    }
    suspend fun setCurrentKey(currentKey: String) {
        context.serverSettingsDatastore.edit { preferences -> preferences[CURRENT_KEY] = currentKey }
    }

    val getCurrentServer: Flow<String?> = context.serverSettingsDatastore.data.map {
            preferences -> preferences[CURRENT_SERVER]
    }
    suspend fun setCurrentServer(currentServer: String?) {
        context.serverSettingsDatastore.edit { preferences -> preferences[CURRENT_SERVER] = currentServer ?: ""}
    }
}
