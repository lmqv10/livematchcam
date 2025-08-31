package it.lmqv.livematchcam.preferences

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.content.edit
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import it.lmqv.livematchcam.adapters.LiveBroadcastItem
import it.lmqv.livematchcam.extensions.Loge
import it.lmqv.livematchcam.extensions.loadBitmapFromUrl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.lang.reflect.Type
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.String

data class ThumbnailAssets(
    var background: Bitmap? = null,
    var logoHome: Bitmap? = null,
    var logoGuest: Bitmap? = null,
    var calendar: ZonedDateTime = ZonedDateTime.now())


data class ScheduleData(
    var backgroundFilePath: String = "",
    var logoHome: String = "",
    var logoGuest: String = "",
    var title: String = "",
    var liveStreamId: String? = null,
    var scheduleTime: ZonedDateTime = ZonedDateTime.now())

data class KeyScheduleData(
    val key: String,
    val value: ScheduleData
)

suspend fun ScheduleData.toThumbnailAsset(context: Context) : ThumbnailAssets {
    return ThumbnailAssets(
        BitmapFactory.decodeFile(this.backgroundFilePath),
        context.loadBitmapFromUrl(this.logoHome),
        context.loadBitmapFromUrl(this.logoGuest),
        this.scheduleTime
    )
}

class SchedulesPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("schedules_prefs", Context.MODE_PRIVATE)

    private val prefSchedulesKey: String = "schedules"

    private val zonedDateTimeAdapter = object : JsonSerializer<ZonedDateTime>, JsonDeserializer<ZonedDateTime> {
        override fun serialize(src: ZonedDateTime?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
            return JsonPrimitive(src?.format(DateTimeFormatter.ISO_ZONED_DATE_TIME))
        }

        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): ZonedDateTime {
            return ZonedDateTime.parse(json?.asString, DateTimeFormatter.ISO_ZONED_DATE_TIME)
        }
    }

    private val gson = GsonBuilder()
        .registerTypeAdapter(ZonedDateTime::class.java, zonedDateTimeAdapter)
        .create()

    private val newScheduleKey : String = "new_schedule"

    private val _currentSchedule = MutableStateFlow<KeyScheduleData>(KeyScheduleData(newScheduleKey, ScheduleData()))
    val currentSchedule: StateFlow<KeyScheduleData> = _currentSchedule

    fun set(backgroundFilePath: String? = null,
        scheduleTime: ZonedDateTime? = null,
        logoHome: String? = null,
        logoGuest: String? = null,
        title: String? = null,
        liveStreamId: String? = null) {

        var current = _currentSchedule.value
        var currentSchedule = current.value
        var updated = currentSchedule.copy(
            backgroundFilePath = backgroundFilePath ?: currentSchedule.backgroundFilePath,
            scheduleTime = scheduleTime ?: currentSchedule.scheduleTime,
            logoHome = logoHome ?: currentSchedule.logoHome,
            logoGuest = logoGuest ?: currentSchedule.logoGuest,
            title = title ?: currentSchedule.title,
            liveStreamId = liveStreamId ?: currentSchedule.liveStreamId,
        )
        _currentSchedule.value = KeyScheduleData(current.key, updated)
        saveMatch(_currentSchedule.value)
    }

    fun isEditing() : Boolean {
        return _currentSchedule.value.key != newScheduleKey
    }

    fun load(broadcastItem: LiveBroadcastItem.EditBroadcast) {
        try {
            var scheduleData = getAll().find { x -> x.key == broadcastItem.broadcastId }
            var currentSchedule = _currentSchedule.value.value
            var updatedScheduledData = ScheduleData(
                backgroundFilePath = scheduleData?.value?.backgroundFilePath ?: currentSchedule.backgroundFilePath,
                scheduleTime = broadcastItem.scheduledStartTime, // ?: scheduleData?.value?.scheduleTime ?: currentSchedule.scheduleTime,
                logoHome = scheduleData?.value?.logoHome ?: currentSchedule.logoHome,
                logoGuest = scheduleData?.value?.logoGuest ?: currentSchedule.logoGuest,
                title = scheduleData?.value?.title ?: currentSchedule.title,
                liveStreamId = scheduleData?.value?.liveStreamId ?: currentSchedule.liveStreamId)

            //Logd("== Created $updatedScheduledData -> ${gson.toJson(updatedScheduledData)}")
            _currentSchedule.value = KeyScheduleData(broadcastItem.broadcastId, updatedScheduledData)
            saveMatch(_currentSchedule.value)
        } catch (e: Exception) {
            e.printStackTrace()
            Loge(e.message.toString())
        }
    }

    fun load() {
        try {
            var newScheduledData = getAll().first { x -> x.key == newScheduleKey }
            //Logd("== Loaded Add $newScheduledData -> ${gson.toJson(newScheduledData)}")
            _currentSchedule.value = newScheduledData
        } catch (e: Exception) {
            e.printStackTrace()
            Loge(e.message.toString())
        }
    }

    fun getAll(): List<KeyScheduleData> {
        val json = prefs.getString(prefSchedulesKey, null) ?: return emptyList()
        val type = object : TypeToken<List<KeyScheduleData>>() {}.type
        return gson.fromJson(json, type)
    }

    fun cleanupMatches(broadcastsItems: List<LiveBroadcastItem>) {

        val broadcastIds = broadcastsItems
            .filterIsInstance<LiveBroadcastItem.EditBroadcast>()
            .map { it.broadcastId }
            .toSet()

        if (broadcastIds.size > 0)
        {
            val schedules = getAll()
            val cleanup = schedules
                .filter { x -> x.key == newScheduleKey || x.key in broadcastIds }

            //Logd("== cleanupMatches ${gson.toJson(cleanup)}")

            prefs.edit { putString(prefSchedulesKey, gson.toJson(cleanup)) }
        }
    }

    /*fun clear() {
        prefs.edit { clear() }
    }*/

    fun add(broadcastId: String) {
        var scheduleData =  _currentSchedule.value.value
        saveMatch(KeyScheduleData(broadcastId, scheduleData))
    }

    private fun saveMatch(item: KeyScheduleData) {
        val matches = getAll().toMutableList()
        matches.removeAll { it.key == item.key }
        matches.add(0, item)
        val json = gson.toJson(matches)
        //Logd("== Save $json")
        prefs.edit { putString(prefSchedulesKey, json) }
    }
}