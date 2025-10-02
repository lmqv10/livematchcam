package it.lmqv.livematchcam.services.youtube

import android.content.Context
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.Channel
import com.google.api.services.youtube.model.LiveBroadcast
import com.google.api.services.youtube.model.LiveBroadcastContentDetails
import com.google.api.services.youtube.model.LiveBroadcastSnippet
import com.google.api.services.youtube.model.LiveBroadcastStatus
import com.google.api.services.youtube.model.LiveStream
import com.google.api.services.youtube.model.MonitorStreamInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.time.ZonedDateTime
import java.util.Date

data class LiveStreamContentData (
    var thumbnail: File,
    var title: String,
    var scheduledStartTime: ZonedDateTime,
    var liveStreamId: String,
    var liveBroadcastId: String?,
)

object YouTubeFactory {
    fun getInstance(context: Context, accountName: String?) : YouTube {
        return YouTube
            .Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                GoogleAccountCredential
                    .usingOAuth2(context, listOf("https://www.googleapis.com/auth/youtube"))
                    .apply {
                        selectedAccountName = accountName
                    })
            .setApplicationName("LiveMatchCam")
            .build()
    }
}

object YouTubeClientProvider {
    private var client: YouTubeClient? = null

    fun initialize(
        context: Context,
        accountName: String?,
        notify: (message: String) -> Unit,
    ) {
        client = YouTubeClient.create(context, accountName, notify)
    }

    fun get(): YouTubeClient {
        return client ?: throw IllegalStateException("YouTubeClient not initialized. Call initialize() first.")
    }
}

class YouTubeClient private constructor(
    private val youTube: YouTube,
    private val notify: (message: String) -> Unit = { },
) {

    companion object {
        fun create(
            context: Context,
            accountName: String?,
            notify: (message: String) -> Unit,
        ): YouTubeClient {
            var youTube = YouTubeFactory.getInstance(context, accountName)
            return YouTubeClient(youTube, notify)
        }
    }

    fun getChannels() : List<Channel> {
        try {
            val response = this.youTube
                .channels()
                .list("id,snippet")
                .apply { mine = true }
                .execute()
            return response.items
        }
        catch (e: Exception) {
            e.printStackTrace()
            notify(e.message.toString())
            return listOf()
        }
    }

    fun getLiveStreams() : List<LiveStream> {
        try {
            val response = this.youTube
                .liveStreams()
                .list("id,snippet,cdn,status")
                .apply { mine = true }
                .execute()
            return response.items
        }
        catch (e: Exception) {
            e.printStackTrace()
            notify(e.message.toString())
            return listOf()
        }
    }

    fun getLiveBroadcast() : List<LiveBroadcast> {
        try {
            val response = this.youTube
                .liveBroadcasts()
                .list("id,snippet,contentDetails,status")
                .apply { mine = true }
                .execute()

            return response.items
        }
        catch (e: Exception) {
            e.printStackTrace()
            notify(e.message.toString())
            return listOf()
        }
    }

//    fun goLive(liveBroadcastId: String, onComplete : (status: String) -> Unit) {
//        try {
//            val response = this.youTube.liveBroadcasts()
//                .transition("live", liveBroadcastId, "status")
//                .execute()
//            notify(response.status.lifeCycleStatus)
//            onComplete(response.status.lifeCycleStatus)
//        }
//        catch (e: Exception) {
//            e.printStackTrace()
//            notify(e.message.toString())
//        }
//    }

    fun completeLive(liveBroadcastId: String, onComplete : (status: String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                notify("Send end stream")

                val response = youTube.liveBroadcasts()
                    .transition("complete", liveBroadcastId, "status")
                    .execute()

                notify("Stream status ${response.status.lifeCycleStatus}")
                onComplete(response.status.lifeCycleStatus)
            } catch (e: Exception) {
                e.printStackTrace()
                notify(e.message.toString())
            }
        }
    }

    fun deleteLive(liveBroadcastId: String, onComplete : () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                youTube.liveBroadcasts()
                    .delete(liveBroadcastId)
                    .execute()

                notify("Match deleted successfully")

                onComplete()
            } catch (e: Exception) {
                e.printStackTrace()
                notify(e.message.toString())
            }
        }
    }


    fun addOrUpdateBroadcastEvent(liveStreamContentData: LiveStreamContentData, onComplete : (broadcastId: String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                notify("Start create Event")

                var eventScheduledStartTime = liveStreamContentData.scheduledStartTime
                val now = ZonedDateTime.now()
                var googleTimeZoneScheduledStartTime = if (now.isBefore(eventScheduledStartTime)) { eventScheduledStartTime } else { now.plusMinutes(1) }
                val googleScheduledStartTime = DateTime(Date.from(googleTimeZoneScheduledStartTime.toInstant()))

                val liveBroadcast = LiveBroadcast().apply {
                    snippet = LiveBroadcastSnippet().apply {
                        title = liveStreamContentData.title
                        scheduledStartTime = googleScheduledStartTime
                    }
                    status = LiveBroadcastStatus().apply {
                        privacyStatus = "unlisted"
                    }
                    kind = "youtube#liveBroadcast"
                    contentDetails = LiveBroadcastContentDetails().apply {
                        enableClosedCaptions = false
                        enableEmbed = false
                        enableAutoStart = true
                        monitorStream = MonitorStreamInfo().apply {
                            enableMonitorStream = true
                            enableAutoStart = true
                            enableDvr = true
                            broadcastStreamDelayMs = 0
                        }
                    }
                }

                var broadcastId: String
                if (liveStreamContentData.liveBroadcastId == null) {
                    val broadcastResponse = youTube
                        .liveBroadcasts()
                        .insert("snippet,status,contentDetails", liveBroadcast)
                        .execute()

                    broadcastId = broadcastResponse.id
                    notify("Match Created $broadcastId")
                } else {
                    broadcastId = liveStreamContentData.liveBroadcastId!!

                    youTube
                        .liveBroadcasts()
                        .update("snippet,status,contentDetails", liveBroadcast.apply { id = broadcastId })
                        .execute()
                    notify("Match updated $broadcastId")
                }

                val bindRequest = youTube
                    .liveBroadcasts()
                    .bind(broadcastId, "id,contentDetails")

                var liveStreamId = liveStreamContentData.liveStreamId
                bindRequest.streamId = liveStreamId
                bindRequest.execute()

                notify("Binding Stream Key $liveStreamId")

                val mediaContent = FileContent("image/jpeg", liveStreamContentData.thumbnail)
                val thumbnailSet = youTube
                    .thumbnails()
                    .set(broadcastId, mediaContent)

                thumbnailSet.execute()

                notify("Thumbnail updated")

                notify("Match Event updated successfully")

                onComplete(broadcastId)
            } catch (e: Exception) {
                e.printStackTrace()
                notify(e.message.toString())
            }
        }
    }
}
