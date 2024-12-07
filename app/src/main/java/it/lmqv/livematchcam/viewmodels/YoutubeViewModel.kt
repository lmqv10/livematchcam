package it.lmqv.livematchcam.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.google.api.services.youtube.model.LiveBroadcast
import com.google.api.services.youtube.model.LiveStream
import it.lmqv.livematchcam.adapters.BroadcastItem
import it.lmqv.livematchcam.repositories.StreamersSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class YoutubeViewModel(application: Application) : AndroidViewModel(application) {
    private var streamersSettingsRepository = StreamersSettingsRepository(application)

    private val _streamURL = MutableStateFlow<String?>(null)
    val streamURL: StateFlow<String?> = _streamURL
    //fun setStreamURL(streamURL: String) { _streamURL.value = streamURL }

    //private val _currentBoundStreamId = MutableStateFlow<String?>(null)
    //val currentBoundStreamId: StateFlow<String?> = _currentBoundStreamId
    suspend fun setCurrentBoundStreamId(currentBoundStreamId: String?) {
        //_currentBoundStreamId.value = currentBoundStreamId
        val liveStream = _liveStreams.value.firstOrNull { x -> x.id == currentBoundStreamId }
        val ingestionInfo = liveStream?.cdn?.ingestionInfo
        val ingestionAddress = ingestionInfo?.ingestionAddress
        val streamName = ingestionInfo?.streamName!!

        streamersSettingsRepository.setCurrentKey(streamName)
        _streamURL.value = "${ingestionAddress}/${streamName}"
    }
    private val _liveURL = MutableStateFlow<String?>(null)
    val liveURL: StateFlow<String?> = _liveURL
    /*fun getLiveStreamByCurrentId() : LiveStream? {
        return liveStreams.value.firstOrNull { x -> x.id == _currentBoundStreamId.value }
    }*/

    private val _liveBroadcasts = MutableStateFlow<List<LiveBroadcast>>(mutableListOf())
    val liveBroadcasts: StateFlow<List<LiveBroadcast>> = _liveBroadcasts
    fun setLiveBroadcasts(liveBroadcasts: List<LiveBroadcast>) { _liveBroadcasts.value = liveBroadcasts }

    private val _currentBroadcast = MutableStateFlow<BroadcastItem?>(null)
    val currentBroadcast: StateFlow<BroadcastItem?> = _currentBroadcast
    fun setCurrentBroadcast(updatedBroadcast: BroadcastItem) {
        _currentBroadcast.value = updatedBroadcast
        _liveURL.value = "\thttps://youtube.com/live/" + updatedBroadcast.id
    }

    private val _liveStreams = MutableStateFlow<List<LiveStream>>(mutableListOf())
    //val liveStreams: StateFlow<List<LiveStream>> = _liveStreams
    fun setLiveStreams(liveStreams: List<LiveStream>) { _liveStreams.value = liveStreams }
}