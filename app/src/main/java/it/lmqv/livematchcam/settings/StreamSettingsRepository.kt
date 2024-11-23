package it.lmqv.livematchcam.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import it.lmqv.livematchcam.utils.KeyValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.streamSettingsDatastore: DataStore<Preferences> by preferencesDataStore(name = "stream_settings")

class StreamSettingsRepository(private val context: Context) {

    private val gson = Gson()

    private val SERVERS = stringPreferencesKey("StreamServers")
    private val CURRENT_SERVER = stringPreferencesKey("CurrentStreamServer")

    private val KEYS = stringPreferencesKey("StreamKeys")
    private val CURRENT_KEY = stringPreferencesKey("CurrentStreamKey")

    val getServers: Flow<List<KeyValue<String>>> = context.streamSettingsDatastore.data.map { preferences ->
        val jsonString = preferences[SERVERS] ?: "[{\"description\":\"YouTube\",\"key\":\"rtmp://a.rtmp.youtube.com/live2\"}]"
        if (jsonString == "") {
            emptyList()
        } else {
            val listType = object : TypeToken<List<KeyValue<String>>>() {}.type
            gson.fromJson(jsonString, listType)
        }
    }
    suspend fun setServers(servers: List<KeyValue<String>>) {
        context.streamSettingsDatastore.edit { preferences ->
            preferences[SERVERS] = gson.toJson(servers)
        }
    }
    val getCurrentServer: Flow<String> = context.streamSettingsDatastore.data.map { preferences -> preferences[CURRENT_SERVER] ?: "" }
    suspend fun setCurrentServer(server: String) {
        context.streamSettingsDatastore.edit { preferences -> preferences[CURRENT_SERVER] = server }
    }

    val getKeys: Flow<List<KeyValue<String>>> = context.streamSettingsDatastore.data.map { preferences ->
        val jsonString = preferences[KEYS] ?: "[{\"description\":\"Default\",\"key\":\"yyx0-at5u-b330-4avg-4kx6\"},{\"description\":\"One-Shot\",\"key\":\"fmjw-uqav-y4ua-xd4d-3zaw\"}]"
        if (jsonString == "") {
            emptyList()
        } else {
            val listType = object : TypeToken<List<KeyValue<String>>>() {}.type
            gson.fromJson(jsonString, listType)
        }
    }
    suspend fun setKeys(keys: List<String>) {
        context.streamSettingsDatastore.edit { preferences ->
            preferences[KEYS] =  gson.toJson(keys)
        }
    }
    val getCurrentKey: Flow<String> = context.streamSettingsDatastore.data.map { preferences -> preferences[CURRENT_KEY] ?: "" }
    suspend fun setCurrentKey(key: String) {
        context.streamSettingsDatastore.edit { preferences -> preferences[CURRENT_KEY] = key }
    }

    val getEndpointStream: Flow<String> = context.streamSettingsDatastore.data.map { preferences ->
        "${preferences[CURRENT_SERVER] ?: ""}/${preferences[CURRENT_KEY] ?: ""}"
    }
}
