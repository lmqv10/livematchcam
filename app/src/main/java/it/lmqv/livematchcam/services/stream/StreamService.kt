package it.lmqv.livematchcam.services.stream

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Range
import android.view.SurfaceView
import androidx.core.app.NotificationCompat
import com.pedro.common.ConnectChecker
import com.pedro.encoder.input.sources.OrientationForced
import com.pedro.encoder.input.sources.audio.MicrophoneSource
import com.pedro.encoder.input.sources.video.NoVideoSource
import com.pedro.encoder.input.sources.video.VideoSource
import com.pedro.library.generic.GenericStream
import com.pedro.encoder.utils.CodecUtil
import com.pedro.library.util.BitrateAdapter
import com.pedro.library.util.FpsListener
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.StreamActivity
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.extensions.Loge
import it.lmqv.livematchcam.extensions.toast
import it.lmqv.livematchcam.factories.CameraResolutionsFactory
import it.lmqv.livematchcam.factories.FiltersFactory
import it.lmqv.livematchcam.factories.sports.Sports
import it.lmqv.livematchcam.factories.VideoSourceFactory
import it.lmqv.livematchcam.preferences.CameraAPIPreferencesManager
import it.lmqv.livematchcam.preferences.ReplayPreferencesManager
import it.lmqv.livematchcam.preferences.PerformancePreferencesManager
import it.lmqv.livematchcam.repositories.StreamConfigurationRepository
import it.lmqv.livematchcam.services.firebase.Quadruple
import it.lmqv.livematchcam.services.stream.audio.AudioDeviceManager
import it.lmqv.livematchcam.services.stream.audio.AudioMonitorEffect
import it.lmqv.livematchcam.services.stream.filters.ReplayUnifiedFilterRender
import it.lmqv.livematchcam.services.stream.filters.IOverlayObjectFilterRender
import it.lmqv.livematchcam.services.replay.ReplayService
import it.lmqv.livematchcam.services.replay.ReplayState
import it.lmqv.livematchcam.services.replay.ReplayMetadata
import android.media.MediaPlayer
import android.net.Uri
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
import com.pedro.library.base.recording.RecordController.Status
import java.io.File

class StreamService: Service(),
    IVideoSourceZoomHandler,
    ConnectChecker,
    FpsListener.Callback {

    // companion object merged into SafeModeConfig above

    inner class LocalBinder : Binder() {
        val service: StreamService get() = this@StreamService
    }
    private val binder = LocalBinder()

    private val _streamingElapsedTime = MutableStateFlow<Int>(0)
    val streamingElapsedTime: StateFlow<Int> = _streamingElapsedTime

    private val _videoSourceZoomHandler = MutableStateFlow<IVideoSourceZoomHandler?>(null)
    val videoSourceZoomHandler: StateFlow<IVideoSourceZoomHandler?> = _videoSourceZoomHandler

    private val _videoCaptureFormats = MutableStateFlow<List<VideoCaptureFormat>>(listOf())
    val videoCaptureFormats: StateFlow<List<VideoCaptureFormat>> = _videoCaptureFormats

    private val _streamPerformance = MutableStateFlow(StreamPerformanceData())
    val streamPerformance: StateFlow<StreamPerformanceData> = _streamPerformance
    private var lastKnownBitrate: Long = 0
    private var previousDroppedFrames: Long = 0
    private var consecutiveRedSeconds: Int = 0
    private var consecutiveGreenSeconds: Int = 0
    private var autoRestoreCount: Int = 0

    // Safe Mode
    companion object {
        private const val CHANNEL_ID = "StreamServiceChannel"
        const val NOTIFY_ID = 230175
        const val SAFE_WIDTH = 854
        const val SAFE_HEIGHT = 480
        const val SAFE_FPS = 30
        const val SAFE_BITRATE = 1_000_000 // 1 Mbps
        const val SAFE_KEYFRAME = 2
    }
    private var safeModeActive = false
    private var savedWidth: Int = 0
    private var savedHeight: Int = 0
    private var savedFps: Int = 0
    private var savedBitrate: Int = 0
    private var currentEndpoint: String? = null

    private lateinit var genericStream: GenericStream
    private lateinit var streamConfigurationRepository: StreamConfigurationRepository
    private var notificationManager: NotificationManager? = null
    private var connectCheckerCallback: ConnectChecker? = null
    private var fpsListenerCallback: FpsListener.Callback? = null
    private var videoSourceKind: VideoSourceKind? = null


    private var prepared = false
    private var videoStreamData : IVideoStreamData = CameraVideoStreamData()
    private var audioStreamData : IAudioStreamData = AudioStreamData()
    private var microphoneSource = MicrophoneSource()

    // Audio monitoring (mic -> headphones)
    private val audioMonitorEffect = AudioMonitorEffect()
    private val _audioMonitorEnabled = MutableStateFlow(false)
    val audioMonitorEnabled: StateFlow<Boolean> = _audioMonitorEnabled
    private var selectedAudioInputDevice: AudioDeviceInfo? = null
    private var audioDeviceCallback: AudioDeviceCallback? = null

    // Replay integration
    private lateinit var replayService: ReplayService
    private var replayUnifiedFilterRender: ReplayUnifiedFilterRender? = null
    private var replayMediaPlayer: MediaPlayer? = null
    private var currentScoreboardFilter: IOverlayObjectFilterRender? = null

    private lateinit var surfaceView: SurfaceView
    private lateinit var sport: Sports

    private val streamServiceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var streamConfigurationJob : Job

    private lateinit var cameraAPIPreferencesManager: CameraAPIPreferencesManager
    private lateinit var performancePrefs: PerformancePreferencesManager
    private lateinit var replayPreferencesManager: ReplayPreferencesManager

    private var currentReplaySpeed = 0.5f

    private val bitrateAdapter = BitrateAdapter {
        genericStream.setVideoBitrateOnFly(it)
    }.apply {
        setMaxBitrate(videoStreamData.bitrate + audioStreamData.bitrate)
    }

    private var timeElapsedInSeconds = 0
    private var job: Job? = null
    private var fadeJob: Job? = null

    fun setConnectCheckerCallback(connectChecker: ConnectChecker?) {
        this.connectCheckerCallback = connectChecker
    }

    fun setFpsListenerCallback(fpsListenerCallback: FpsListener.Callback?) {
        this.fpsListenerCallback = fpsListenerCallback
    }

    override fun onCreate() {
        super.onCreate()
        Logd("StreamService :: onCreate")

        cameraAPIPreferencesManager = CameraAPIPreferencesManager(this)

        streamConfigurationRepository = StreamConfigurationRepository(applicationContext)

        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_ID, NotificationManager.IMPORTANCE_HIGH)
        notificationManager?.createNotificationChannel(channel)

        val replayPreferencesManager = ReplayPreferencesManager(baseContext)
        this.replayPreferencesManager = replayPreferencesManager
        val bufferDuration = replayPreferencesManager.getBufferDurationSeconds()
        this.currentReplaySpeed = replayPreferencesManager.getReplaySpeed()
        performancePrefs = PerformancePreferencesManager(baseContext)
        
        replayService = ReplayService(baseContext, bufferDuration)
        replayService.startRecordAction = { path ->
            if (genericStream.isStreaming && !genericStream.isRecording) {
                genericStream.startRecord(path) { status ->
                    Logd("StreamService :: record status $status")
                    if (status == Status.STOPPED) {
                        replayService.onRecordStopped()
                    }
                }
            }
        }
        replayService.stopRecordAction = {
            if (genericStream.isRecording) {
                genericStream.stopRecord()
                // Library might stop audioSource. Restore it if needed.
                ensureMicrophoneStartedForMonitor()
            }
        }

        genericStream = GenericStream(baseContext, this, NoVideoSource(), microphoneSource).apply {
            getGlInterface().autoHandleOrientation = true
            getGlInterface().forceOrientation(OrientationForced.LANDSCAPE)
            getStreamClient().setBitrateExponentialFactor(0.5f)
            getStreamClient().setReTries(10)
            getStreamClient().resizeCache(200)
            setFpsListener(this@StreamService)
        }

        // Attach the monitoring effect to the microphone pipeline
        microphoneSource.setAudioEffect(audioMonitorEffect)

        // Register audio device callback for disconnect handling
        registerAudioDeviceCallback()

        this.streamConfigurationJob = streamServiceScope.launch {
            combine(
                streamConfigurationRepository.fps,
                streamConfigurationRepository.bitrate,
                streamConfigurationRepository.videoCaptureFormat,
                streamConfigurationRepository.videoSourceKind
            ) { fps, bitrate, videoCaptureFormat, videoSourceKind -> Quadruple(fps, bitrate, videoCaptureFormat, videoSourceKind) }
            .distinctUntilChanged()
            .collect { (fps, bitrate, videoCaptureFormat,videoSourceKind) ->

                //Logd("StreamService :: streamConfigurationRepository :: ${this@StreamService.videoSourceKind} vs $videoSourceKind")
                if (this@StreamService.videoSourceKind != videoSourceKind) {
                    this@StreamService.videoSourceKind = videoSourceKind
                    var videoSource = VideoSourceFactory.get(videoSourceKind, baseContext)
                    this@StreamService.changeVideoSource(videoSource)
                }

//                Logd("StreamService :: streamConfigurationRepository :: cameraSourceParameters :: ${videoCaptureFormat}")
//                Logd("StreamService :: streamConfigurationRepository :: ${videoStreamData.height}p@${videoStreamData.fps}fps vs ${videoCaptureFormat.height}p@${fps}fps")
//                Logd("StreamService :: streamConfigurationRepository :: bitrate: ${videoStreamData.bitrate} vs ${bitrate}")

                if (this@StreamService.videoStreamData.width != videoCaptureFormat.width ||
                    this@StreamService.videoStreamData.height != videoCaptureFormat.height ||
                    this@StreamService.videoStreamData.fps != fps ||
                    this@StreamService.videoStreamData.bitrate != bitrate) {

                    this@StreamService.videoStreamData.width = videoCaptureFormat.width
                    this@StreamService.videoStreamData.height = videoCaptureFormat.height
                    this@StreamService.videoStreamData.fps = fps

                    this@StreamService.videoStreamData.bitrate = bitrate
                    bitrateAdapter.setMaxBitrate(videoStreamData.bitrate + audioStreamData.bitrate)

                    if (genericStream.isOnPreview) {
                        Logd("StreamService :: streamConfigurationRepository :: change resolution or sport during preview.. re-PreparePreview()")
                        this@StreamService.preparePreview()
                    }
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

        audioMonitorEffect.release()
        unregisterAudioDeviceCallback()

        this.stopStream()
        genericStream.release()
        cameraAPIPreferencesManager.cancel()
    }

    override fun onFps(fps: Int) {
        this.fpsListenerCallback?.onFps(fps)
    }

    override fun onConnectionStarted(url: String) {
        this.connectCheckerCallback?.onConnectionStarted(url)
        this.cameraAPIPreferencesManager.onConnectionStarted()
        this.startStreamingTimer()

        if (genericStream.isStreaming) {
            genericStream.requestKeyframe()
            Logd("StreamService :: startStream :: forced initial keyframe")
        }
    }

    override fun onConnectionSuccess() {
        this.connectCheckerCallback?.onConnectionSuccess()
    }

    override fun onNewBitrate(bitrate: Long) {
        bitrateAdapter.adaptBitrate(bitrate, genericStream.getStreamClient().hasCongestion())
        lastKnownBitrate = bitrate
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
        this.cameraAPIPreferencesManager.onDisconnect()
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

    fun getVideoFormats(): List<VideoCaptureFormat> {
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

    // --- AUDIO MONITORING ---

    fun toggleAudioMonitor(): Boolean {
        val newState = !audioMonitorEffect.isEnabled()
        if (newState) {
            audioMonitorEffect.start(
                sampleRate = audioStreamData.sampleRate,
                isStereo = audioStreamData.isStereo
            )
        } else {
            audioMonitorEffect.stop()
        }
        _audioMonitorEnabled.value = newState
        Logd("StreamService :: toggleAudioMonitor -> $newState")

        // In preview mode, the library doesn't start the microphone automatically.
        // We need to manage the microphone lifecycle if the stream/record is not active.
        if (newState) {
            ensureMicrophoneStartedForMonitor()
        } else {
            ensureMicrophoneStoppedForMonitor()
        }

        return newState
    }

    private fun ensureMicrophoneStartedForMonitor() {
        if (audioMonitorEnabled.value && !genericStream.isStreaming && !genericStream.isRecording && !microphoneSource.isRunning()) {
            try {
                val field = com.pedro.library.base.StreamBase::class.java.getDeclaredField("getMicrophoneData")
                field.isAccessible = true
                val realData = field.get(genericStream) as com.pedro.encoder.input.audio.GetMicrophoneData
                microphoneSource.start(realData)
                Logd("StreamService :: ensureMicrophoneStartedForMonitor -> Started manually")
            } catch (e: Exception) {
                Loge("StreamService :: Failed to start microphone manually for monitor: ${e.message}")
            }
        }
    }

    private fun ensureMicrophoneStoppedForMonitor() {
        if (!audioMonitorEnabled.value && !genericStream.isStreaming && !genericStream.isRecording && microphoneSource.isRunning()) {
            microphoneSource.stop()
            Logd("StreamService :: ensureMicrophoneStoppedForMonitor -> Stopped manually")
        }
    }

    fun setMonitorOutputDevice(device: AudioDeviceInfo?) {
        audioMonitorEffect.setOutputDevice(device)
    }

    fun getAvailableOutputDevices(): List<AudioDeviceInfo> {
        return AudioDeviceManager.getOutputHeadphones(baseContext)
    }

    // --- AUDIO INPUT DEVICE (USB/HDMI) ---

    fun getAvailableInputDevices(): List<AudioDeviceInfo> {
        return AudioDeviceManager.getUsbAudioInputDevices(baseContext)
    }

    fun setAudioInputDevice(device: AudioDeviceInfo?) {
        selectedAudioInputDevice = device
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            microphoneSource.setPreferredDevice(device)
        }
        val name = if (device != null) AudioDeviceManager.getDeviceDisplayName(device) else "Default Mic"
        Logd("StreamService :: setAudioInputDevice -> $name")
        CoroutineScope(Dispatchers.Main).launch {
            toast(baseContext.getString(R.string.audio_input_selected, name))
        }
    }

    fun getSelectedAudioInputDevice(): AudioDeviceInfo? {
        return selectedAudioInputDevice
    }

    // --- AUDIO DEVICE CALLBACK ---

    private fun registerAudioDeviceCallback() {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        audioDeviceCallback = object : AudioDeviceCallback() {
            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
                // If monitoring headphones were disconnected, stop monitoring
                if (audioMonitorEffect.isEnabled()) {
                    val headphones = AudioDeviceManager.getOutputHeadphones(baseContext)
                    if (headphones.isEmpty()) {
                        audioMonitorEffect.stop()
                        _audioMonitorEnabled.value = false
                        CoroutineScope(Dispatchers.Main).launch {
                            toast(baseContext.getString(R.string.headphones_disconnected_monitor_off))
                        }
                    }
                }
                // If selected USB audio input was removed, reset to default
                if (selectedAudioInputDevice != null) {
                    val stillConnected = AudioDeviceManager.getUsbAudioInputDevices(baseContext)
                        .any { it.id == selectedAudioInputDevice?.id }
                    if (!stillConnected) {
                        setAudioInputDevice(null)
                        CoroutineScope(Dispatchers.Main).launch {
                            toast(baseContext.getString(R.string.usb_audio_disconnected))
                        }
                    }
                }
            }
        }
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, Handler(Looper.getMainLooper()))
    }

    private fun unregisterAudioDeviceCallback() {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        audioDeviceCallback?.let { audioManager.unregisterAudioDeviceCallback(it) }
        audioDeviceCallback = null
    }

    fun isOnPreview(): Boolean {
        return genericStream.isOnPreview
    }

    fun isStreaming(): Boolean {
        return genericStream.isStreaming
    }

    fun startStream(endpoint: String) {
        if (!prepared) {
            CoroutineScope(Dispatchers.Main).launch {
                toast("Impossibile avviare: parametri Audio/Video non validi. (Non pronti)")
            }
            Loge("StreamService :: startStream :: failed because encoders not prepared")
            return
        }
        if (!genericStream.isStreaming) {
            currentEndpoint = endpoint
            Logd("StreamService :: startStream :: $endpoint")
            genericStream.startStream(endpoint)
            if (performancePrefs.isReplayEnabled()) {
                replayService.startRollingRecording()
            }
        }
    }

    fun stopStream() {
        if (genericStream.isStreaming) {
            replayService.stopRollingRecording()
            genericStream.stopStream()
            notificationManager?.cancel(NOTIFY_ID)

            // Library eagerly stops audioSource. Restore it if needed.
            ensureMicrophoneStartedForMonitor()
        }
        // Reset safe mode on full stop
        if (safeModeActive) {
            restoreOriginalParams()
            safeModeActive = false
        }
        currentEndpoint = null
    }

    // --- SAFE MODE ---

    fun isSafeModeActive(): Boolean = safeModeActive

    fun enableSafeMode() {
        if (safeModeActive) return
        val endpoint = currentEndpoint ?: return
        if (!genericStream.isStreaming) return

        Logd("StreamService :: enableSafeMode :: switching to ${SAFE_WIDTH}x${SAFE_HEIGHT}@${SAFE_FPS}fps ${SAFE_BITRATE/1000}kbps")
        safeModeActive = true

        CoroutineScope(Dispatchers.Main).launch {
            // Save current params
            savedWidth = videoStreamData.width
            savedHeight = videoStreamData.height
            savedFps = videoStreamData.fps
            savedBitrate = videoStreamData.bitrate

            // Stop stream and preview
            replayService.stopRollingRecording()
            genericStream.stopStream()
            this@StreamService.stopPreview()

            // Wait for resources to be released
            delay(500)

            // Reconfigure with safe params
            videoStreamData.width = SAFE_WIDTH
            videoStreamData.height = SAFE_HEIGHT
            videoStreamData.fps = SAFE_FPS
            videoStreamData.bitrate = SAFE_BITRATE
            bitrateAdapter.setMaxBitrate(SAFE_BITRATE + audioStreamData.bitrate)

            // Re-prepare and restart
            this@StreamService.prepare()
            this@StreamService.prepareFilters()
            genericStream.startPreview(this@StreamService.surfaceView, true)
            this@StreamService.startStream(endpoint)

            consecutiveRedSeconds = 0
            consecutiveGreenSeconds = 0
            _streamPerformance.value = _streamPerformance.value.copy(isSafeModeActive = true)

            toast(getString(R.string.safe_mode_toast_enabled, SAFE_HEIGHT, SAFE_FPS))
        }
    }

    fun disableSafeMode() {
        if (!safeModeActive) return
        val endpoint = currentEndpoint ?: return

        Logd("StreamService :: disableSafeMode :: restoring ${savedWidth}x${savedHeight}@${savedFps}fps")
        safeModeActive = false

        CoroutineScope(Dispatchers.Main).launch {
            // Stop stream and preview
            replayService.stopRollingRecording()
            if (genericStream.isStreaming) {
                genericStream.stopStream()
            }
            this@StreamService.stopPreview()

            // Wait for resources to be released
            delay(500)

            // Restore original params
            restoreOriginalParams()

            // Re-prepare and restart
            this@StreamService.prepare()
            this@StreamService.prepareFilters()
            genericStream.startPreview(this@StreamService.surfaceView, true)
            this@StreamService.startStream(endpoint)

            consecutiveRedSeconds = 0
            consecutiveGreenSeconds = 0
            _streamPerformance.value = _streamPerformance.value.copy(isSafeModeActive = false)

            toast(getString(R.string.safe_mode_toast_disabled))
        }
    }

    private fun restoreOriginalParams() {
        if (savedWidth > 0) {
            videoStreamData.width = savedWidth
            videoStreamData.height = savedHeight
            videoStreamData.fps = savedFps
            videoStreamData.bitrate = savedBitrate
            bitrateAdapter.setMaxBitrate(savedBitrate + audioStreamData.bitrate)
        }
    }

    fun getVideoSourceKind(): VideoSourceKind? {
        return this.videoSourceKind
    }

    fun changeVideoSource(videoSource: VideoSource) {
        try {
            if (genericStream.videoSource::class != videoSource::class) {
                Logd("StreamService :: streamConfigurationRepository :: Change VideoSource to $videoSourceKind")

                genericStream.changeVideoSource(videoSource)

                //Logd("StreamService :: set video source to camera preferences handler")
                this.cameraAPIPreferencesManager.setVideoSource(genericStream.videoSource)
                //Logd("StreamService :: initialize and notify zoomHandler")
                _videoSourceZoomHandler.value = VideoSourceZoomHandler(genericStream.videoSource)
                //Logd("StreamService :: initialize and notify videoCaptureFormats")
                _videoCaptureFormats.value = CameraResolutionsFactory.get(genericStream.videoSource)

                CoroutineScope(Dispatchers.Main).launch {
                    toast("$videoSourceKind Ready")
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
                fps = videoStreamData.fps,
                iFrameInterval = performancePrefs.getKeyframeInterval()
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
                toast("Encoder ${videoStreamData.height}p@${videoStreamData.fps}fps ${videoStreamData.bitrate / 1000}kbps")
            }
        }
    }

    fun initPreview(surfaceView: SurfaceView, sport: Sports)
    {
        this.surfaceView = surfaceView
        this.cameraAPIPreferencesManager.setSurfaceView(this.surfaceView)

        this.sport = sport
        preparePreview()
    }

    fun preparePreview()
    {
        try {
//            Logd("StreamService :: preparePreview - start")
//            Logd("StreamService :: preparePreview - genericStream.isStreaming ${genericStream.isStreaming}")
//            Logd("StreamService :: preparePreview - genericStream.isOnPreview ${genericStream.isOnPreview}")
            if (!genericStream.isStreaming) {
//                Logd("StreamService :: preparePreview - $sport")
//                Logd("StreamService :: preparePreview - $videoStreamData")
                this.stopPreview()
                this.prepare()
                this.prepareFilters()
                genericStream.startPreview(this.surfaceView, true)
                ensureMicrophoneStartedForMonitor()
            } else {
                // Logd("StreamService :: preparePreview - restart Preview while streaming")
                this.stopPreview()
                this.prepareFilters()
                genericStream.startPreview(this.surfaceView, true)
                ensureMicrophoneStartedForMonitor()
            }
            //Logd("StreamService :: preparePreview - end")
        }
        catch (e: Exception) {
            e.printStackTrace()
            Loge("StreamService :: preparePreview:: Exception ${e.message.toString()}")
        }
    }

//    fun restartPreview() {
//        try {
//            //if (genericStream.isOnPreview) {
//                Logd("StreamService :: restart Preview")
//                this.stopPreview()
//                this.prepareFilters()
//                genericStream.startPreview(this.surfaceView, true)
//            //} else {
//            //    Logd("StreamService :: Restart Preview not available")
//            //}
//        }
//        catch (e: Exception) {
//            e.printStackTrace()
//            Loge("restartPreview:: Exception ${e.message.toString()}")
//        }
//    }

    fun stopPreview() {
        if (genericStream.isOnPreview)
        {
            genericStream.stopPreview()
        }
    }

    private fun startStreamingTimer() {
        if (job == null || job?.isActive == false) {
            previousDroppedFrames = 0
            consecutiveRedSeconds = 0
            job = CoroutineScope(Dispatchers.Main).launch {
                while (isActive) {
                    _streamingElapsedTime.value = timeElapsedInSeconds

                    if (genericStream.isStreaming) {
                        val client = genericStream.getStreamClient()
                        val itemsInCache = client.getItemsInCache()
                        val cacheCapacity = client.getCacheSize()
                        val droppedVideo = client.getDroppedVideoFrames()
                        val hasCongestion = client.hasCongestion()

                        // Calculate dropped frame rate (new drops per second)
                        val droppedDelta = droppedVideo - previousDroppedFrames
                        previousDroppedFrames = droppedVideo

                        // Cache fill percentage (0-100)
                        val cacheFillPercent = if (cacheCapacity > 0) {
                            (itemsInCache * 100) / cacheCapacity
                        } else 0

                        val health = when {
                            // RED: cache fill > 50% or rapidly dropping frames
                            cacheFillPercent > 50 || droppedDelta > 5 -> NetworkHealth.RED
                            // YELLOW: congestion detected or cache fill > 20%
                            hasCongestion || cacheFillPercent > 20 -> NetworkHealth.YELLOW
                            // GREEN: all good
                            else -> NetworkHealth.GREEN
                        }

                        if (health == NetworkHealth.RED) {
                            consecutiveRedSeconds++
                            consecutiveGreenSeconds = 0
                        } else if (health == NetworkHealth.GREEN) {
                            consecutiveGreenSeconds++
                            consecutiveRedSeconds = 0
                        } else {
                            // YELLOW
                            consecutiveGreenSeconds = 0
                            consecutiveRedSeconds = 0
                        }

                        _streamPerformance.value = StreamPerformanceData(
                            currentBitrate = lastKnownBitrate,
                            droppedFrames = droppedVideo.toInt(),
                            cacheSize = itemsInCache,
                            health = health,
                            isSafeModeActive = safeModeActive
                        )

                        // Auto-trigger Safe Mode after sustained RED
                        val threshold = performancePrefs.getSafeModeThreshold()
                        if (!safeModeActive && threshold > 0 && consecutiveRedSeconds >= threshold) {
                            Logd("StreamService :: Auto-triggering Safe Mode after ${threshold}s RED")
                            enableSafeMode()
                        }

                        // Auto-disable Safe Mode after sustained GREEN (Anti-Ping-Pong strategy)
                        if (safeModeActive && threshold > 0 && consecutiveGreenSeconds >= 60) {
                            if (autoRestoreCount < 2) {
                                Logd("StreamService :: Auto-restoring Quality after 60s GREEN (Attempt ${autoRestoreCount + 1}/2)")
                                autoRestoreCount++
                                CoroutineScope(Dispatchers.Main).launch {
                                    toast(getString(R.string.safe_mode_auto_restore))
                                }
                                disableSafeMode()
                            } else if (consecutiveGreenSeconds == 60) {
                                // Inform user once that we won't auto-restore anymore
                                Logd("StreamService :: Auto-restore limit reached. Staying in Safe Mode.")
                                CoroutineScope(Dispatchers.Main).launch {
                                    toast(getString(R.string.safe_mode_unstable_stay))
                                }
                            }
                        }
                    }

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
        lastKnownBitrate = 0
        previousDroppedFrames = 0
        consecutiveRedSeconds = 0
        consecutiveGreenSeconds = 0
        _streamPerformance.value = StreamPerformanceData()
    }

    private fun prepareFilters() {
        //Logd("StreamService :: prepareFilters")
        with (genericStream.getGlInterface()) {
            setPreviewResolution(videoStreamData.width, videoStreamData.height)
            clearFilters()

            if (performancePrefs.isFiltersEnabled()) {
                // Composite: 1 GL layer merging all N overlay bitmaps (CPU Canvas)
                val compositeFilter = FiltersFactory.getCompositeFilter(applicationContext)
                compositeFilter.setVideoStreamData(videoStreamData)
                addFilter(compositeFilter)

                // Legacy: N separate GL layers (kept for internal benchmarking)
//                val filters = FiltersFactory.getFilters(applicationContext)
//                filters.forEachIndexed { index, filter ->
//                    if (filter is IOverlayObjectFilterRender) {
//                        filter.setVideoStreamData(videoStreamData)
//                    }
//                    addFilter(filter)
//                }
            } else {
                Logd("StreamService :: Performance toggle: Filters disabled")
            }

            if (performancePrefs.isScoreboardEnabled()) {
                val scoreboard = FiltersFactory.getScoreBoard(sport, applicationContext)
                scoreboard.setVideoStreamData(videoStreamData)
                addFilter(scoreboard)
                currentScoreboardFilter = scoreboard
            } else {
                Logd("StreamService :: Performance toggle: Scoreboard disabled")
            }

            if (performancePrefs.isReplayEnabled()) {
                // Add Replay Video Filter (invisible by default)
                // Add Unified Replay Filter (includes video surface and label)
                replayUnifiedFilterRender = ReplayUnifiedFilterRender()
                replayUnifiedFilterRender?.let {
                    it.setVideoStreamData(videoStreamData)
                    it.setFullScreen()
                    addFilter(it)
                }
            } else {
                Logd("StreamService :: Performance toggle: Replay disabled")
            }
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

    // --- REPLAY METHODS ---

    val replayState: StateFlow<ReplayState> get() = replayService.replayState
    val replayMetadata: StateFlow<ReplayMetadata?> get() = replayService.replayMetadata

    suspend fun prepareReplay(): Boolean {
        return replayService.prepareReplay()
    }

    fun startReplay(seekMs: Long) {
        val metadata = replayMetadata.value ?: return
        Logd("StreamService :: startReplay Overlaid at seekMs=$seekMs, file=${metadata.filePath}")

        // 1. Stop recording FIRST to avoid conflicts (rolling chunks)
        replayService.startReplay()

        // 2. Hide scoreboard and ensure replay filter is ready
        //currentScoreboardFilter?.hide()
        replayUnifiedFilterRender?.hide()

        // 3. Initialize MediaPlayer for Overlay
        try {
            stopReplayMediaPlayer() // Ensure clean state

            val mergedFile = File(metadata.filePath)
            if (mergedFile.exists()) {
                replayMediaPlayer = MediaPlayer().apply {
                    setDataSource(applicationContext, Uri.fromFile(File(metadata.filePath)))
                    replayUnifiedFilterRender?.surface?.let { setSurface(it) }
                    isLooping = false
                    setOnCompletionListener {
                        Logd("StreamService :: Replay MediaPlayer finished")
                        if (replayState.value == ReplayState.REPLAYING) {
                            stopReplay()
                        }
                    }
                    setOnInfoListener { _, what, _ ->
                        if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                            currentScoreboardFilter?.hide()

                            animateReplayAlpha(1.0f, 250)
                            true
                        } else false
                    }

                    prepare()

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        seekTo(seekMs, MediaPlayer.SEEK_CLOSEST)
                    } else {
                        seekTo(seekMs.toInt())
                    }

                    // 4. Start playback
                    start()
                    setVolume(0f, 0f)
                    if (currentReplaySpeed > 0f) {
                        playbackParams = getPlaybackParams().setSpeed(currentReplaySpeed)
                    }
                }
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    toast("File Replay missing. Please Retry")
                }
            }

            Logd("StreamService :: startReplay Overlay started and seeked to $seekMs ms")
            
        } catch (e: Exception) {
            Loge("StreamService :: Error starting Replay MediaPlayer: ${e.message}")
            CoroutineScope(Dispatchers.Main).launch {
                toast("Error starting Replay! ${e.message}")
            }
            stopReplay()
        }
    }

    private fun animateReplayAlpha(target: Float, durationMs: Long) {
        fadeJob?.cancel()
        fadeJob = streamServiceScope.launch {
            val startAlpha = replayUnifiedFilterRender?.getCurrentAlpha() ?: 0f
            val steps = 20
            val stepDuration = durationMs / steps
            val alphaDelta = (target - startAlpha) / steps

            for (i in 1..steps) {
                if (!isActive) break
                val newAlpha = startAlpha + (alphaDelta * i)
                replayUnifiedFilterRender?.updateAlpha(newAlpha)
                delay(stepDuration)
            }
            replayUnifiedFilterRender?.updateAlpha(target)
        }
    }

    private fun stopReplayMediaPlayer() {
        replayMediaPlayer?.stop()
        replayMediaPlayer?.release()
        replayMediaPlayer = null
        replayUnifiedFilterRender?.hide()
    }

    fun setReplaySpeed(speed: Float) {
        currentReplaySpeed = speed
        replayPreferencesManager.setReplaySpeed(speed)
        replayMediaPlayer?.let {
            if (it.isPlaying) {
                try {
                    if (currentReplaySpeed > 0f) {
                        it.playbackParams = it.getPlaybackParams().setSpeed(speed)
                    }
                } catch (e: Exception) {
                    Loge("StreamService :: Error updating Replay playback speed: ${e.message}")
                }
            }
        }
    }

    fun cancelReplay() {
        Logd("StreamService :: cancelReplay")
        replayService.stopReplay()
    }

    fun stopReplay() {
        if (replayState.value != ReplayState.REPLAYING) return
        Logd("StreamService :: stopReplay starting fade out and returning to live")

        streamServiceScope.launch {
            // 1. Fade Out smoothly
            animateReplayAlpha(0.0f, 400)
            delay(450) // Wait for fade to finish

            // 2. Stop and release MediaPlayer (MUST be on main thread for release typically, but streamServiceScope is IO. 
            // MediaPlayer is thread safe for release but let's be safe if UI thread is needed).
            // Actually, we are in a coroutine, so we'll switch to Main for cleanup.
            launch(Dispatchers.Main) {
                stopReplayMediaPlayer()
                
                // 3. Restore scoreboard visibility and hide Replay badge
                currentScoreboardFilter?.show()
                replayUnifiedFilterRender?.hide()

                // 4. Inform service
                replayService.stopReplay()
            }
        }
    }
}