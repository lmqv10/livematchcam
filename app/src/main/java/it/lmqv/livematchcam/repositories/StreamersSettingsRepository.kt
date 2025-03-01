package it.lmqv.livematchcam.repositories

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import it.lmqv.livematchcam.factories.Sports
import it.lmqv.livematchcam.utils.KeyValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.streamersSettingsDatastore: DataStore<Preferences> by preferencesDataStore(name = "streamers_settings")

class StreamersSettingsRepository(private val context: Context) {

    private val gson = Gson()

    private val KEYS = stringPreferencesKey("StreamKeys")
    //private val SERVERS = stringPreferencesKey("StreamServers")
    private val CURRENT_KEY = stringPreferencesKey("CurrentKey")
    private val CURRENT_SERVER = stringPreferencesKey("CurrentServer")
    private val SPORT = stringPreferencesKey("Sport")

    /*val getServers: Flow<List<KeyValue<String>>> = context.streamersSettingsDatastore.data.map { preferences ->
        val jsonString = preferences[SERVERS] ?: "[{\"description\":\"YouTube\",\"key\":\"rtmp://a.rtmp.youtube.com/live2\"}]"
        if (jsonString == "") {
            emptyList()
        } else {
            val listType = object : TypeToken<List<KeyValue<String>>>() {}.type
            gson.fromJson(jsonString, listType)
        }
    }*/
    /*suspend fun setServers(servers: List<KeyValue<String>>) {
        context.streamersSettingsDatastore.edit { preferences -> preferences[SERVERS] = gson.toJson(servers) }
    }*/

    val getKeys: Flow<List<KeyValue<String>>> = context.streamersSettingsDatastore.data.map { preferences ->
        val jsonString = preferences[KEYS] ?: ("[" +
                "{\"description\":\"Custom\",\"key\":\"\"}," +
                "{\"description\":\"Lecco 2010\",\"key\":\"yyx0-at5u-b330-4avg-4kx6\"}," +
                "{\"description\":\"Picco\",\"key\":\"9vqy-cskt-f154-qxux-ep5v\"}" +
                "]")
        if (jsonString == "") {
            emptyList()
        } else {
            val listType = object : TypeToken<List<KeyValue<String>>>() {}.type
            gson.fromJson(jsonString, listType)
        }
    }
    /*suspend fun setKeys(keys: List<KeyValue<String>>) {
        context.streamersSettingsDatastore.edit { preferences -> preferences[KEYS] =  gson.toJson(keys) }
    }*/

    val getCurrentKey: Flow<String?> = context.streamersSettingsDatastore.data.map {
        preferences -> preferences[CURRENT_KEY]
    }
    suspend fun setCurrentKey(currentKey: String) {
        context.streamersSettingsDatastore.edit { preferences -> preferences[CURRENT_KEY] = currentKey }
    }

    val getCurrentServer: Flow<String?> = context.streamersSettingsDatastore.data.map {
            preferences -> preferences[CURRENT_SERVER]
    }
    suspend fun setCurrentServer(currentServer: String?) {
        context.streamersSettingsDatastore.edit { preferences -> preferences[CURRENT_SERVER] = currentServer ?: ""}
    }

    val getSport: Flow<Sports> = context.streamersSettingsDatastore.data.map { preferences ->
        var sportName = preferences[SPORT] ?: Sports.SOCCER.name
        Sports.valueOf(sportName)
    }

    suspend fun setSport(currentSport: Sports) {
        context.streamersSettingsDatastore.edit { preferences -> preferences[SPORT] = currentSport.name }
    }
}
