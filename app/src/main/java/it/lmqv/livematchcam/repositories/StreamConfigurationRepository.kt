package it.lmqv.livematchcam.repositories

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import it.lmqv.livematchcam.viewmodels.VideoSourceKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.streamConfigurationDataStore: DataStore<Preferences> by preferencesDataStore(name = "streamConfiguration")

class StreamConfigurationRepository(private val context: Context) {

    private val VIDEO_SOURCE_KIND = stringPreferencesKey("VideoSourceKind")
    private val STREAM_FPS = intPreferencesKey("StreamFps")
    private val STREAM_RESOLUTION = intPreferencesKey("StreamResolution")

    val fps: Flow<Int> = context.streamConfigurationDataStore.data.map { preferences -> preferences[STREAM_FPS] ?: 30 }
    suspend fun setFps(updatedFps: Int) {
        context.streamConfigurationDataStore.edit { preferences -> preferences[STREAM_FPS] = updatedFps }
    }

    val resolution: Flow<Int> = context.streamConfigurationDataStore.data.map { preferences -> preferences[STREAM_RESOLUTION] ?: 1080 }
    suspend fun setResolution(updatedResolution: Int) {
        context.streamConfigurationDataStore.edit { preferences -> preferences[STREAM_RESOLUTION] = updatedResolution }
    }

    val videoSourceKind: Flow<VideoSourceKind> =
        context.streamConfigurationDataStore.data.map { preferences ->
            val value = preferences[VIDEO_SOURCE_KIND]
            value?.let {
                runCatching { VideoSourceKind.valueOf(it) }.getOrNull()
            } ?: VideoSourceKind.CAMERA2 // default
        }

    suspend fun setVideoSourceKind(kind: VideoSourceKind) {
        context.streamConfigurationDataStore.edit { preferences ->
            preferences[VIDEO_SOURCE_KIND] = kind.name
        }
    }
}