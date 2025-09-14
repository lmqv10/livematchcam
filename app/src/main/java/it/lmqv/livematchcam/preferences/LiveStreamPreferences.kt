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
    var scheduleStartTime: ZonedDateTime = ZonedDateTime.now())

data class KeyScheduleData(
    val key: String,
    val value: ScheduleData
)

suspend fun ScheduleData.toThumbnailAsset(context: Context) : ThumbnailAssets {
    return ThumbnailAssets(
        BitmapFactory.decodeFile(this.backgroundFilePath),
        context.loadBitmapFromUrl(this.logoHome),
        context.loadBitmapFromUrl(this.logoGuest),
        this.scheduleStartTime
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

    private val keyNewSchedule : String = "new_schedule"

    private val _currentKeyScheduleData = MutableStateFlow<KeyScheduleData>(KeyScheduleData(keyNewSchedule, ScheduleData()))
    val currentKeyScheduleData: StateFlow<KeyScheduleData> = _currentKeyScheduleData

    fun isEditing() : Boolean {
        return _currentKeyScheduleData.value.key != keyNewSchedule
    }

    fun set(backgroundFilePath: String? = null,
        scheduledStartTime: ZonedDateTime? = null,
        logoHome: String? = null,
        logoGuest: String? = null,
        title: String? = null,
        liveStreamId: String? = null) {

        var current = _currentKeyScheduleData.value
        //Logd("== set: ${current.key}")
        updateSchedule(current.key, backgroundFilePath, scheduledStartTime, logoHome, logoGuest, title, liveStreamId)
    }

    fun load(broadcastItem: LiveBroadcastItem.EditBroadcast) {
        //Logd("== load(broadcastItem: ${broadcastItem.broadcastId}")

        updateSchedule(broadcastItem.broadcastId,
            scheduledStartTime = broadcastItem.scheduledStartTime,
            title = broadcastItem.title,
            liveStreamId = broadcastItem.boundStreamId)
    }

    fun load() {
        //Logd("== updateSchedule: new scheduled")
        var newScheduledData = getAll().first { x -> x.key == keyNewSchedule }
        _currentKeyScheduleData.value = newScheduledData
    }

    fun getAll(): List<KeyScheduleData> {
        val json = prefs.getString(prefSchedulesKey, null) ?: return listOf(KeyScheduleData(keyNewSchedule, ScheduleData()))
        val type = object : TypeToken<List<KeyScheduleData>>() {}.type

        //Logd("== source: $json")
        //var parsed : List<KeyScheduleData> = gson.fromJson(json, type)
        //parsed.forEach { x -> Logd("== found: ${x.key} - ${x.value}") }
        return gson.fromJson(json, type)
    }

    fun cleanupMatches(broadcastsItems: List<LiveBroadcastItem>) {
        val broadcastIds = broadcastsItems
            .filterIsInstance<LiveBroadcastItem.EditBroadcast>()
            .map { it.broadcastId }
            .toSet()

        if (broadcastIds.isNotEmpty())
        {
            val schedules = getAll()
            val cleanup = schedules
                .filter { x -> x.key == keyNewSchedule || x.key in broadcastIds }

            //Logd("== cleanupMatches ${gson.toJson(cleanup)}")
            prefs.edit { putString(prefSchedulesKey, gson.toJson(cleanup)) }
        }
    }

    fun add(broadcastId: String) {
        var scheduleData =  _currentKeyScheduleData.value.value
        saveMatch(KeyScheduleData(broadcastId, scheduleData))
    }

    private fun updateSchedule(
        broadcastId: String,
        backgroundFilePath: String? = null,
        scheduledStartTime: ZonedDateTime? = null,
        logoHome: String? = null,
        logoGuest: String? = null,
        title: String? = null,
        liveStreamId: String? = null) {

        var keyScheduleData = getAll().find { x -> x.key == broadcastId }
        if(keyScheduleData != null)
        {
            var scheduleDataValue = keyScheduleData.value
            //Logd("== Source ScheduleData: $scheduleDataValue")
            var updatedScheduledData = scheduleDataValue.copy(
                backgroundFilePath = backgroundFilePath ?: scheduleDataValue.backgroundFilePath,
                scheduleStartTime = scheduledStartTime ?: scheduleDataValue.scheduleStartTime,
                logoHome = logoHome ?: scheduleDataValue.logoHome,
                logoGuest = logoGuest ?: scheduleDataValue.logoGuest,
                title = title ?: scheduleDataValue.title,
                liveStreamId = liveStreamId ?: scheduleDataValue.liveStreamId)

            //Logd("== Updated ${gson.toJson(updatedScheduledData)}")
            _currentKeyScheduleData.value = KeyScheduleData(broadcastId, updatedScheduledData)
            saveMatch(_currentKeyScheduleData.value)
        }
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