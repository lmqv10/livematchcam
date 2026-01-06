package it.lmqv.livematchcam.services.stream

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Range
import android.util.Size
import android.view.SurfaceView
import androidx.core.app.NotificationCompat
import com.pedro.common.ConnectChecker
import com.pedro.encoder.input.gl.render.filters.`object`.BaseObjectFilterRender
import com.pedro.encoder.input.sources.OrientationForced
import com.pedro.encoder.input.sources.audio.MicrophoneSource
import com.pedro.encoder.input.sources.video.Camera2Source
import com.pedro.encoder.input.sources.video.NoVideoSource
import com.pedro.encoder.input.sources.video.VideoSource
import com.pedro.encoder.input.video.CameraHelper
import com.pedro.library.generic.GenericStream
import com.pedro.library.util.BitrateAdapter
import com.pedro.library.util.FpsListener
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.StreamActivity
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.extensions.Loge
import it.lmqv.livematchcam.extensions.toast
import it.lmqv.livematchcam.factories.CameraResolutionsFactory
import it.lmqv.livematchcam.factories.FiltersFactory
import it.lmqv.livematchcam.factories.Sports
import it.lmqv.livematchcam.factories.VideoSourceFactory
import it.lmqv.livematchcam.repositories.MatchRepository
import it.lmqv.livematchcam.repositories.StreamConfigurationRepository
import it.lmqv.livematchcam.services.firebase.Quadruple
import it.lmqv.livematchcam.services.stream.filters.IScoreboardViewFilterRender
import it.lmqv.livematchcam.services.stream.filters.IOverlayObjectFilterRender
import it.lmqv.livematchcam.utils.OptionItem
import it.lmqv.livematchcam.viewmodels.VideoSourceKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

//interface IPreviewEventListener {
//    fun onPreviewStarted()
//}

class StreamService: Service(),
    IVideoSourceZoomHandler,
    //IPreviewEventListener,
    ConnectChecker,
    FpsListener.Callback {

    companion object {
        private const val CHANNEL_ID = "StreamServiceChannel"
        const val NOTIFY_ID = 230175
    }

    inner class LocalBinder : Binder() {
        val service: StreamService get() = this@StreamService
    }
    private val binder = LocalBinder()

    private val _streamingElapsedTime = MutableStateFlow<Int>(0)
    val streamingElapsedTime: StateFlow<Int> = _streamingElapsedTime

    private val _videoSourceZoomHandler = MutableStateFlow<IVideoSourceZoomHandler?>(null)
    val videoSourceZoomHandler: StateFlow<IVideoSourceZoomHandler?> = _videoSourceZoomHandler

    private lateinit var genericStream: GenericStream
    private lateinit var streamConfigurationRepository: StreamConfigurationRepository
    private var notificationManager: NotificationManager? = null
    private var connectCheckerCallback: ConnectChecker? = null
    //private var previewEventListenerCallback: IPreviewEventListener? = null
    private var fpsListenerCallback: FpsListener.Callback? = null
    private var videoSourceKind: VideoSourceKind? = null
    private var sport: Sports? = null

    private var prepared = false
    private var videoStreamData : IVideoStreamData = CameraVideoStreamData()
    private var audioStreamData : IAudioStreamData = AudioStreamData()
    private var microphoneSource = MicrophoneSource()
    private lateinit var surfaceView: SurfaceView
    //private var filters: List<OverlayObjectFilterRender> = listOf()

    private val streamServiceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var streamConfigurationJob : Job
    private var matchRepositoryJob : Job? = null
    private var scoreRepositoryJob : Job? = null

    private val bitrateAdapter = BitrateAdapter {
        genericStream.setVideoBitrateOnFly(it)
    }.apply {
        setMaxBitrate(videoStreamData.bitrate + audioStreamData.bitrate)
    }

    private var timeElapsedInSeconds = 0
    private var job: Job? = null

//    fun setPreviewEventListenerCallback(previewEventListener: IPreviewEventListener?) {
//        this.previewEventListenerCallback = previewEventListener
//    }

    fun setConnectCheckerCallback(connectChecker: ConnectChecker?) {
        this.connectCheckerCallback = connectChecker
    }

    fun setFpsListenerCallback(fpsListenerCallback: FpsListener.Callback?) {
        this.fpsListenerCallback = fpsListenerCallback
    }

    override fun onCreate() {
        super.onCreate()
        Logd("StreamService :: onCreate")
        streamConfigurationRepository = StreamConfigurationRepository(applicationContext)

        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_ID, NotificationManager.IMPORTANCE_HIGH)
        notificationManager?.createNotificationChannel(channel)

        genericStream = GenericStream(baseContext, this, NoVideoSource(), microphoneSource).apply {
            getGlInterface().autoHandleOrientation = true
            getGlInterface().forceOrientation(OrientationForced.LANDSCAPE)
            getStreamClient().setBitrateExponentialFactor(0.5f)
            getStreamClient().setReTries(10)
            setFpsListener(this@StreamService)
        }

        this.streamConfigurationJob = streamServiceScope.launch {
            combine(
                MatchRepository.sport,
                streamConfigurationRepository.fps,
                streamConfigurationRepository.resolution,
                streamConfigurationRepository.videoSourceKind
            ) { sport, fps, resolution, videoSourceKind -> Quadruple(sport, fps, resolution, videoSourceKind) }
            .distinctUntilChanged()
            .collect { (sport, fps, resolutionHeight, videoSourceKind) ->

                Logd("StreamService :: streamConfigurationRepository :: ${this@StreamService.videoSourceKind} vs $videoSourceKind")
                if (this@StreamService.videoSourceKind != videoSourceKind) {
                    this@StreamService.videoSourceKind = videoSourceKind
                    var videoSource = VideoSourceFactory.get(videoSourceKind, baseContext)
                    this@StreamService.changeVideoSource(videoSource)
                }

                Logd("StreamService :: streamConfigurationRepository :: ${videoStreamData.height}p@${videoStreamData.fps}fps vs ${resolutionHeight}p@${fps}fps")
                if (!genericStream.isStreaming && genericStream.isOnPreview &&
                    (this@StreamService.sport != sport || videoStreamData.height != resolutionHeight || videoStreamData.fps != fps)) {
                    videoStreamData.width = if (resolutionHeight == 1080) { 1920 } else { 1280 }
                    videoStreamData.height = resolutionHeight
                    videoStreamData.fps = fps
                    this@StreamService.sport = sport
                    Logd("StreamService :: streamConfigurationRepository :: change resolution or sport! .. preparePreview()")
                    this@StreamService.preparePreview(this@StreamService.surfaceView, sport)
                } else {
                    Logd("StreamService :: streamConfigurationRepository :: No changes")
                }
            }
        }

        keepAliveTrick()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logd("StreamService :: onStartCommand")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        Logd("StreamService :: onBind")
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Logd("StreamService :: onUnbind")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        Logd("StreamService :: onDestroy")
        this.streamConfigurationJob.cancel()
        this.matchRepositoryJob?.cancel()
        this.scoreRepositoryJob?.cancel()
        this.stopStream()
        genericStream.release()
    }

//    override fun onPreviewStarted() {
//        this.previewEventListenerCallback?.onPreviewStarted()
//    }

    override fun onFps(fps: Int) {
        this.fpsListenerCallback?.onFps(fps)
    }

    override fun onConnectionStarted(url: String) {
        this.connectCheckerCallback?.onConnectionStarted(url)
        this.startStreamingTimer()
    }

    override fun onConnectionSuccess() {
        this.connectCheckerCallback?.onConnectionSuccess()
    }

    override fun onNewBitrate(bitrate: Long) {
        bitrateAdapter.adaptBitrate(bitrate, genericStream.getStreamClient().hasCongestion())
        this.connectCheckerCallback?.onNewBitrate(bitrate)
    }

    override fun onConnectionFailed(reason: String) {
        this.connectCheckerCallback?.onConnectionFailed(reason)
        if (genericStream.getStreamClient().reTry(5000, reason, null)) {
            CoroutineScope(Dispatchers.Main).launch {
                toast("Retry: $reason")
            }
        } else {
            genericStream.stopStream()
            CoroutineScope(Dispatchers.Main).launch {
                toast("Failed: $reason")
            }
        }

    }

    override fun onDisconnect() {
        this.connectCheckerCallback?.onDisconnect()
        this.stopStreamingTimer()
    }

    override fun onAuthError() {
        this.connectCheckerCallback?.onAuthError()
    }

    override fun onAuthSuccess() {
        this.connectCheckerCallback?.onAuthSuccess()
    }

    override fun getZoomRange(): Range<Float> {
        return this.videoSourceZoomHandler.value?.getZoomRange() ?: Range(1f, 1f)
    }

    override fun getZoom(): Float {
        return this.videoSourceZoomHandler.value?.getZoom() ?: 0f
    }

    override fun setZoom(level: Float) {
        this.videoSourceZoomHandler.value?.setZoom(level)
    }

    fun getCameraResolutions() : List<Size> {
        return CameraResolutionsFactory.get(genericStream.videoSource)
    }

    fun toggleMicrophoneAudio() : Boolean {
        return if (microphoneSource.isMuted()) {
            microphoneSource.unMute()
            false
        } else {
            microphoneSource.mute()
            true
        }
    }

    fun isOnPreview(): Boolean {
        return genericStream.isOnPreview
    }

    fun isStreaming(): Boolean {
        return genericStream.isStreaming
    }

    fun startStream(endpoint: String) {
        if (!genericStream.isStreaming) {
            genericStream.startStream(endpoint)
        }
    }

    fun stopStream() {
        if (genericStream.isStreaming) {
            genericStream.stopStream()
            notificationManager?.cancel(NOTIFY_ID)
        }
    }

    fun getVideoSourceKind(): VideoSourceKind? {
        return this.videoSourceKind
    }

    fun changeVideoSource(videoSource: VideoSource) {
        try {
            if (genericStream.videoSource::class != videoSource::class) {
                Logd("StreamService :: streamConfigurationRepository :: Change VideoSource to $videoSourceKind")

                when (videoSource) {
                    is Camera2Source -> {
                        with(videoSource) {
                            enableVideoStabilization()
                            enableOpticalVideoStabilization()
                            enableAutoFocus()
                            enableAutoExposure()
                            disableFaceDetection()
                        }
                    }
                }

                genericStream.changeVideoSource(videoSource)

                Logd("StreamService :: initialize and notify zoomHandler")
                _videoSourceZoomHandler.value = VideoSourceZoomHandler(genericStream.videoSource)

                CoroutineScope(Dispatchers.Main).launch {
                    toast("Ready for $videoSourceKind")
                }
            }
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            CoroutineScope(Dispatchers.Main).launch {
                toast("changeVideoSource :: Exception :: ${e.message.toString()}")
            }
        }
    }

    fun prepare() {
        Logd("StreamService :: prepare ${videoStreamData.width}x${videoStreamData.height}p@${videoStreamData.fps}fps")

        prepared = try {
            genericStream.prepareVideo(
                width = videoStreamData.width,
                height = videoStreamData.height,
                bitrate = videoStreamData.bitrate,
                rotation = videoStreamData.rotation,
                fps = videoStreamData.fps
            ) &&
            genericStream.prepareAudio(
                sampleRate = audioStreamData.sampleRate,
                isStereo = audioStreamData.isStereo,
                bitrate = audioStreamData.bitrate,
                echoCanceler = audioStreamData.echoCanceler,
                noiseSuppressor = audioStreamData.noiseSuppressor
            )

            true
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            false
        }

        CoroutineScope(Dispatchers.Main).launch {
            if (!prepared) {
                toast("Invalid audio or video parameters, prepare failed")
            } else {
                toast("Ready for ${videoStreamData.width}x${videoStreamData.height}p@${videoStreamData.fps}fps")
            }
        }
    }

    fun preparePreview(surfaceView: SurfaceView, sport: Sports) {
        try {
            Logd("StreamService :: preparePreview $sport")
            this.stopPreview()

            this.surfaceView = surfaceView

            genericStream.getGlInterface().setPreviewResolution(videoStreamData.width, videoStreamData.height)
            genericStream.getGlInterface().clearFilters()

            var filters = FiltersFactory.get(sport, applicationContext)
            filters.forEachIndexed { index, filter ->
                //Logd("StreamService :: addFilter $filter")
                genericStream.getGlInterface().addFilter(index, filter)
                if (filter is IOverlayObjectFilterRender) {
                    //Logd("StreamService :: filter $filter setVideoStreamData $videoStreamData")
                    filter.setVideoStreamData(videoStreamData)
                }
            }
            this.prepareMatchDataListeners(filters)

            this.prepare()
            genericStream.startPreview(this.surfaceView, true)


            (genericStream.videoSource as Camera2Source)
                .getCameraResolutions((genericStream.videoSource as Camera2Source).getCameraFacing())
        }
        catch (e: Exception) {
            e.printStackTrace()
            Loge("preparePreview:: Exception ${e.message.toString()}")
        }
    }

    fun stopPreview() {
        if (genericStream.isOnPreview) genericStream.stopPreview()
    }

    private fun startStreamingTimer() {
        if (job == null || job?.isActive == false) {
            job = CoroutineScope(Dispatchers.Main).launch {
                while (isActive) {
                    _streamingElapsedTime.value = timeElapsedInSeconds
                    delay(1000)
                    timeElapsedInSeconds++
                }
            }
        }
    }

    private fun stopStreamingTimer() {
        job?.cancel()
        job = null
        timeElapsedInSeconds = 0
        _streamingElapsedTime.value = timeElapsedInSeconds
    }

    private fun prepareMatchDataListeners(filters: List<BaseObjectFilterRender>) {
        this.matchRepositoryJob?.cancel()
        this.scoreRepositoryJob?.cancel()

        var scoreBoardFilterInstance = filters.firstOrNull { it is IScoreboardViewFilterRender }
        if (scoreBoardFilterInstance != null) {
            var scoreBoardFilterRender = scoreBoardFilterInstance as IScoreboardViewFilterRender
            this.matchRepositoryJob = this.streamServiceScope.launch {
                MatchRepository.match.collect { match ->
                    //Logd("StreamService :: MatchRepository.match.collect :: $match")
                    scoreBoardFilterRender.match(match)
                }
            }
            this.scoreRepositoryJob = this.streamServiceScope.launch {
                MatchRepository.score.collect { score ->
                    //Logd("StreamService :: MatchRepository.score.collect :: $score")
                    scoreBoardFilterRender.score(score)
                }
            }
        //} else {
        //    Logd("StreamService :: prepareListeners :: not required")
        }
    }

    private fun keepAliveTrick() {
        val notificationIntent = Intent(this, StreamActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.switch_icon)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_text))
            .setContentIntent(pendingIntent)
            .setSilent(true)
            .setOngoing(false)
            .build()
        startForeground(1, notification)
    }
}