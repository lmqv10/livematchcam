package it.lmqv.livematchcam.repositories

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

val Context.firebaseDataStore: DataStore<Preferences> by preferencesDataStore(name = "firebase_data")

class FirebaseDataRepository(private val context: Context) {

    private val keyStreamName =  stringPreferencesKey("streamName")

    val streamName: Flow<String> = context.firebaseDataStore.data.map { preferences -> preferences[keyStreamName] ?: "" }
    fun setStreamName(updatedStreamName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            context.firebaseDataStore.edit { preferences -> preferences[keyStreamName] = updatedStreamName }
        }
    }
}
