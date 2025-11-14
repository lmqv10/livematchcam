package it.lmqv.livematchcam.repositories

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.firebaseSettingsDatastore: DataStore<Preferences> by preferencesDataStore(name = "firebase_settings")

data class KeyDescription(
    val key: String,
    val description: String
)

class FirebaseSettingsRepository(private val context: Context) {
    private val CURRENT_KEY = stringPreferencesKey("CurrentKey")
    private val CURRENT_SERVER = stringPreferencesKey("CurrentServer")

    val getCurrentKey: Flow<String?> = context.firebaseSettingsDatastore.data.map {
        preferences -> preferences[CURRENT_KEY]
    }
    suspend fun setCurrentKey(currentKey: String) {
        context.firebaseSettingsDatastore.edit { preferences -> preferences[CURRENT_KEY] = currentKey }
    }

    val getCurrentServer: Flow<String?> = context.firebaseSettingsDatastore.data.map {
            preferences -> preferences[CURRENT_SERVER]
    }
    suspend fun setCurrentServer(currentServer: String?) {
        context.firebaseSettingsDatastore.edit { preferences -> preferences[CURRENT_SERVER] = currentServer ?: ""}
    }

//    private val KEYS = stringPreferencesKey("StreamKeys")
//    private val SERVERS = stringPreferencesKey("StreamServers")
//
//    private val gson = GsonBuilder().create()

//    val getServers: Flow<List<KeyDescription>> = context.firebaseSettingsDatastore.data.map { preferences ->
//        //val jsonString = preferences[SERVERS] ?:
//        val jsonString =
//            """
//                [
//                    { "description": "YouTube", "key": "rtmp://a.rtmp.youtube.com/live2" }
//                ]
//            """.trimIndent()
//
//        if (jsonString == "") {
//            emptyList()
//        } else {
//            try {
//                val listType = object : TypeToken<List<KeyDescription>>() {}.type
//                gson.fromJson(jsonString, listType)
//            } catch (e: Exception) {
//                e.printStackTrace()
//                emptyList()
//            }
//        }
//    }
//    suspend fun setServers(servers: List<KeyDescription>) {
//        context.firebaseSettingsDatastore.edit { preferences -> preferences[SERVERS] = gson.toJson(servers) }
//    }

//    val getKeys: Flow<List<KeyDescription>> = context.firebaseSettingsDatastore.data.map { preferences ->
//        //val jsonString = preferences[KEYS] ?:
//        val jsonString =
//            """
//                [
//                    {"description":"Serie B1", "key":"tuh3-qbgp-zzc4-3dsc-7zcc"},
//                    {"description":"Serie C", "key":"erqm-xjr2-6wt7-aae4-2kqh"},
//                    {"description":"Serie D", "key":"bxch-qv4s-y1vv-e17j-eyt3"},
//                    {"description":"2° Divisione", "key":"08rk-hcj3-skhr-kw73-aert"},
//                    {"description":"3° Divisione", "key":"z0sd-w0rg-dh5a-2ecr-crv6"},
//                    {"description":"U18 Red Eccellenza", "key":"y9ax-jj41-0emz-3b4w-b8a2"},
//                    {"description":"U18 White Territoriale", "key":"9r2g-psmj-ar77-0rh5-8ry3"},
//                    {"description":"U16 Red Eccellenza", "key":"k3uz-mpcu-rh10-a6mw-48fz"},
//                    {"description":"U16 White Territoriale", "key":"wd14-v5cu-xw8s-814g-ahp0"},
//                    {"description":"U14 Red Eccellenza", "key":"a4t3-15bm-xp94-xqdq-8k4d"},
//                    {"description":"U14 White Territoriale", "key":"hw6h-mu0j-jh44-4jd7-29ms"},
//                    {"description":"U13 Red Rossa", "key":"xh7k-z61k-m2hp-m2sc-ff54"},
//                    {"description":"U13 White Bianca", "key":"z6vu-e8pe-1d9p-km5y-0dyb"},
//                    {"description":"U12", "key":"p4f9-syzj-euq8-46ru-8h6k"}
//                ]
//            """.trimIndent()
//
//        if (jsonString == "") {
//            emptyList()
//        } else {
//            try {
//                val listType = object : TypeToken<List<KeyDescription>>() {}.type
//                gson.fromJson(jsonString, listType)
//            } catch (e: Exception) {
//                e.printStackTrace()
//                emptyList()
//            }
//        }
//    }

//    suspend fun clear() {
//        context.firebaseSettingsDatastore.edit { clear() }
//    }
}
