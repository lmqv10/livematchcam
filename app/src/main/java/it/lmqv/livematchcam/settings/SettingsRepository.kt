package it.lmqv.livematchcam.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private val AUTO_ZOOM_ENABLED =  booleanPreferencesKey("AutoZoomEnabled")

    private val LEFT_DEGREE = intPreferencesKey("LeftDegree")
    private val RIGHT_DEGREE =  intPreferencesKey("RightDegree")

    private val INITIAL_ZOOM =  floatPreferencesKey("InitialZoom")
    private val ZOOM_OFFSET =  floatPreferencesKey("ZoomOffset")

    val autoZoomEnabled: Flow<Boolean> = context.settingsDataStore.data.map { preferences -> preferences[AUTO_ZOOM_ENABLED] ?: true }
    suspend fun setAutoZoom(enabled: Boolean) {
        context.settingsDataStore.edit { preferences -> preferences[AUTO_ZOOM_ENABLED] = enabled }
    }

    val leftDegree: Flow<Int> = context.settingsDataStore.data.map { preferences -> preferences[LEFT_DEGREE] ?: 35 }
    suspend fun setLeftDegree(degree: Int) {
        context.settingsDataStore.edit { preferences -> preferences[LEFT_DEGREE] = degree }
    }

    val rightDegree: Flow<Int> = context.settingsDataStore.data.map { preferences -> preferences[RIGHT_DEGREE] ?: 35 }
    suspend fun setRightDegree(degree: Int) {
        context.settingsDataStore.edit { preferences -> preferences[RIGHT_DEGREE] = degree }
    }

    val initialZoom: Flow<Float> = context.settingsDataStore.data.map { preferences -> preferences[INITIAL_ZOOM] ?: 1.0f }
    suspend fun setInitialZoom(level: Float) {
        context.settingsDataStore.edit { preferences -> preferences[INITIAL_ZOOM] = level }
    }

    val zoomOffset: Flow<Float> = context.settingsDataStore.data.map { preferences -> preferences[ZOOM_OFFSET] ?: 0.3f }
    suspend fun setZoomOffset(offset: Float) {
        context.settingsDataStore.edit { preferences -> preferences[ZOOM_OFFSET] = offset }
    }
}