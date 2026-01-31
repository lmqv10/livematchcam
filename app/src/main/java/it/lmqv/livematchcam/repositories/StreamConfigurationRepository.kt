package it.lmqv.livematchcam.repositories

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.GsonBuilder
import it.lmqv.livematchcam.services.stream.VideoCaptureFormat
import it.lmqv.livematchcam.viewmodels.VideoSourceKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.streamConfigurationDataStore: DataStore<Preferences> by preferencesDataStore(name = "streamConfiguration")

class StreamConfigurationRepository(private val context: Context) {

    private val gson = GsonBuilder().create()

    private val VIDEO_SOURCE_KIND = stringPreferencesKey("VideoSourceKind")
    private val STREAM_FPS = intPreferencesKey("StreamFps")
    private val STREAM_BITRATE = intPreferencesKey("StreamBitrate")
    private val VIDEO_CAPTURE_FORMATS = stringPreferencesKey("VideoCaptureFormats")

    val fps: Flow<Int> = context.streamConfigurationDataStore.data.map { preferences -> preferences[STREAM_FPS] ?: 30 }
    suspend fun setFps(updatedFps: Int) {
        context.streamConfigurationDataStore.edit { preferences -> preferences[STREAM_FPS] = updatedFps }
    }

    val bitrate: Flow<Int> = context.streamConfigurationDataStore.data.map { preferences -> preferences[STREAM_BITRATE] ?: (6000 * 1000) }
    suspend fun setBitrate(updatedBitrate: Int) {
        context.streamConfigurationDataStore.edit { preferences -> preferences[STREAM_BITRATE] = updatedBitrate }
    }

    val videoCaptureFormat: Flow<VideoCaptureFormat> = context.streamConfigurationDataStore.data.map { preferences ->
        var jsonString = preferences[VIDEO_CAPTURE_FORMATS] ?: ""
        if (jsonString.isEmpty()) {
            VideoCaptureFormat() // default
        } else {
            gson.fromJson(jsonString, VideoCaptureFormat::class.java)
        }
    }

    suspend fun setVideoCaptureFormat(updatedVideoCaptureFormat: VideoCaptureFormat) {
        context.streamConfigurationDataStore.edit { preferences ->
            var jsonString = gson.toJson(updatedVideoCaptureFormat)
            preferences[VIDEO_CAPTURE_FORMATS] = jsonString
        }
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