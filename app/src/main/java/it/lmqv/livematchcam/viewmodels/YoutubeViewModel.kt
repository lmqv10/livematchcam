package it.lmqv.livematchcam.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.api.services.youtube.model.LiveBroadcast
import com.google.api.services.youtube.model.LiveStream
import it.lmqv.livematchcam.adapters.LiveBroadcastItem
import it.lmqv.livematchcam.repositories.FirebaseDataRepository
import it.lmqv.livematchcam.repositories.MatchRepository
import it.lmqv.livematchcam.services.youtube.LiveStreamContentData
import it.lmqv.livematchcam.services.youtube.YouTubeClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class YoutubeViewModelFactory(
    private val application: Application,
    private val client: YouTubeClient
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(YoutubeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return YoutubeViewModel(application, client) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class YoutubeViewModel(
    application: Application,
    private val youTubeClient: YouTubeClient) : AndroidViewModel(application) {

    private val firebaseDataRepository = FirebaseDataRepository(application)

    fun setCurrentBoundStreamId(currentBoundStreamId: String) {
        val liveStream = _liveStreams.value.firstOrNull { x -> x.id == currentBoundStreamId }
        val ingestionInfo = liveStream?.cdn?.ingestionInfo

        val ingestionAddress = ingestionInfo?.ingestionAddress
        val streamName = ingestionInfo?.streamName!!

        firebaseDataRepository.setStreamName(streamName)
        MatchRepository.setRTMPServer("${ingestionAddress}/${streamName}")
    }

    private val _liveURL = MutableStateFlow<String?>(null)
    val liveURL: StateFlow<String?> = _liveURL

    private val _liveBroadcasts = MutableStateFlow<List<LiveBroadcast>>(mutableListOf())
    val liveBroadcasts: StateFlow<List<LiveBroadcast>> = _liveBroadcasts
    fun loadLiveBroadcast() {
        _liveBroadcasts.value = this.youTubeClient.getLiveBroadcast()
    }

    private val _currentBroadcast = MutableStateFlow<LiveBroadcastItem.EditBroadcast?>(null)
    val currentBroadcast: StateFlow<LiveBroadcastItem.EditBroadcast?> = _currentBroadcast
    fun setCurrentBroadcast(updatedBroadcast: LiveBroadcastItem.EditBroadcast) {
        _currentBroadcast.value = updatedBroadcast
        _liveURL.value = "\thttps://youtube.com/live/" + updatedBroadcast.broadcastId

        //Logd("MatchRepository::setCurrentBroadcastId::: ${updatedBroadcast.broadcastId}")
        MatchRepository.currentBroadcastId = updatedBroadcast.broadcastId
    }

    private var _liveStreams = MutableStateFlow<List<LiveStream>>(mutableListOf())
    val liveStreams: StateFlow<List<LiveStream>> = _liveStreams
    fun loadLiveStreams() {
        _liveStreams.value = this.youTubeClient.getLiveStreams()
    }

    fun addOrUpdateBroadcastEvent(
        liveStreamContentData: LiveStreamContentData,
        onComplete: (broadcastId: String) -> Unit
    ) {
        this.youTubeClient.addOrUpdateBroadcastEvent(liveStreamContentData, onComplete)
    }

    fun deleteLive(liveBroadcastId: String, onComplete: () -> Unit) {
        this.youTubeClient.deleteLive(liveBroadcastId, onComplete)
    }

    private var _liveStreamStatus = MutableStateFlow<String>("unknown")
//    fun goLive() {
//        currentBoundStreamId?.let {
//            this.youTubeClient.goLive(it) { status ->
//                _liveStreamStatus.value = status
//            }
//        }
//    }

    fun completeLive() {
        val broadcastId = MatchRepository.currentBroadcastId
        //Logd("YoutubeViewModel::broadcastId:: $broadcastId")
        this.youTubeClient.completeLive(broadcastId) { status ->
            //Logd("YoutubeViewModel::complete::Status: $status")
            _liveStreamStatus.value = status
        }
    }

}