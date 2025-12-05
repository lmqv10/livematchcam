package it.lmqv.livematchcam.services.stream

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Range
import android.view.SurfaceView
import androidx.core.app.NotificationCompat
import com.pedro.common.ConnectChecker
import com.pedro.encoder.input.sources.OrientationForced
import com.pedro.encoder.input.sources.audio.MicrophoneSource
import com.pedro.encoder.input.sources.video.Camera2Source
import com.pedro.encoder.input.sources.video.NoVideoSource
import com.pedro.encoder.input.sources.video.VideoSource
import com.pedro.library.generic.GenericStream
import com.pedro.library.util.BitrateAdapter
import com.pedro.library.util.FpsListener
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.StreamActivity
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.extensions.toast
import it.lmqv.livematchcam.repositories.StreamConfigurationRepository
import it.lmqv.livematchcam.services.stream.filters.BitmapObjectFilterRender
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

    private lateinit var genericStream: GenericStream
    private lateinit var streamConfigurationRepository: StreamConfigurationRepository
    private var notificationManager: NotificationManager? = null
    private var connectCheckerCallback: ConnectChecker? = null
    //private var previewEventListenerCallback: IPreviewEventListener? = null
    private var fpsListenerCallback: FpsListener.Callback? = null
    private var zoomHandler: IVideoSourceZoomHandler? = null

    private var prepared = false
    private var videoStreamData : IVideoStreamData = CameraVideoStreamData()
    private var audioStreamData : IAudioStreamData = AudioStreamData()
    private var microphoneSource = MicrophoneSource()
    private var surfaceView: SurfaceView? = null
    private var filters: List<BitmapObjectFilterRender> = listOf()

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

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
        }

        serviceScope.launch() {
            combine(
                streamConfigurationRepository.fps,
                streamConfigurationRepository.resolution
            ) { fps, resolution -> Pair(fps, resolution) }
            .distinctUntilChanged()
            .collect { (fps, resolutionHeight) ->

                Logd("StreamService :: streamConfigurationRepository :: ${videoStreamData.height}p@${videoStreamData.fps}fps vs ${resolutionHeight}p@${fps}fps")

                if (videoStreamData.height != resolutionHeight
                    || videoStreamData.fps != fps) {

                    videoStreamData.width = if (resolutionHeight == 1080) { 1920 } else { 1280 }
                    videoStreamData.height = resolutionHeight
                    videoStreamData.fps = fps

                    if (!genericStream.isStreaming && genericStream.isOnPreview && this@StreamService.surfaceView != null) {
                        Logd("StreamService :: streamConfigurationRepository :: change resolution! .. preparePreview()")
                        this@StreamService.preparePreview(this@StreamService.surfaceView!!, this@StreamService.filters)
                    }
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
        stopStream()
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
        return this.zoomHandler?.getZoomRange() ?: Range(1f, 1f)
    }

    override fun getZoom(): Float {
        return this.zoomHandler?.getZoom() ?: 0f
    }

    override fun setZoom(level: Float) {
        this.zoomHandler?.setZoom(level)
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

    fun changeVideoSource(videoSource: VideoSource) {
        try {
            if (genericStream.videoSource::class != videoSource::class) {
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
                zoomHandler = VideoSourceZoomHandler(genericStream.videoSource)
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

    fun preparePreview(surfaceView: SurfaceView,
         filters: List<BitmapObjectFilterRender> = listOf()) {
        Logd("StreamService :: preparePreview")

        this.surfaceView = surfaceView
        this.filters = filters

        this.stopPreview()

        genericStream.getGlInterface().clearFilters()
        filters.forEachIndexed { index, filter ->
            genericStream.getGlInterface().addFilter(index, filter)
        }

        this.prepare()

        if (!genericStream.isOnPreview && this.surfaceView != null) {
            genericStream.getGlInterface().setPreviewResolution(videoStreamData.width, videoStreamData.height)

            filters.forEachIndexed { index, filter ->
                filter.setVideoStreamData(videoStreamData)
            }

            genericStream.startPreview(this.surfaceView!!, true)

            //this.onPreviewStarted()
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