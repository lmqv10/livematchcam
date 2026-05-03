package it.lmqv.livematchcam.services.stream

import android.app.Activity
import android.media.AudioDeviceInfo
import android.view.SurfaceView
import com.pedro.common.ConnectChecker
import com.pedro.library.util.FpsListener
import it.lmqv.livematchcam.dialogs.StartStreamingDialog
import it.lmqv.livematchcam.dialogs.StopStreamingDialog
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.extensions.hideSystemUI
import it.lmqv.livematchcam.extensions.toast
import it.lmqv.livematchcam.factories.sports.Sports
import it.lmqv.livematchcam.services.replay.ReplayMetadata
import it.lmqv.livematchcam.services.replay.ReplayState
import it.lmqv.livematchcam.viewmodels.VideoSourceKind
import kotlinx.coroutines.flow.StateFlow

class StreamServiceProxy(val activityContext: Activity, val streamService: StreamService) :
    IStreamService {
    private var endpoint: String? = null

    override fun getVideoSourceKind(): VideoSourceKind? {
        //Logd("StreamServiceProxy :: getVideoSourceKind")
        return streamService.getVideoSourceKind()
    }

    override fun initPreview(surfaceView: SurfaceView, sport: Sports) {
        Logd("StreamServiceProxy :: initPreview")
        this.streamService.initPreview(surfaceView, sport)
    }

//    override fun preparePreview() {
//        Logd("StreamServiceProxy :: preparePreview")
//        streamService.preparePreview()
//    }

    override fun stopPreview() {
        //Logd("StreamServiceProxy :: stopPreview")
        streamService.stopPreview()
    }

    override fun setEndpoint(endpoint: String?) {
        //Logd("StreamServiceProxy :: setEndpoint $endpoint")
        this.endpoint = endpoint
    }

    override fun getEndpoint() : String? {
        return this.endpoint
    }

    override fun isStreaming() : Boolean {
        return streamService.isStreaming() == true
    }

    override fun isOnPreview() : Boolean {
        return streamService.isOnPreview() == true
    }

    override fun toggleStreaming(onStartCallback: () -> Unit, onStopCallback: (Boolean) -> Unit) {
        if (this.endpoint != null) {
            if (streamService.isStreaming() == true) {
                var dialog = StopStreamingDialog(
                    activityContext,
                    { shouldEnd ->
                        streamService.stopStream()
                        onStopCallback(shouldEnd)
                        activityContext.hideSystemUI()
                    },
                    { activityContext.hideSystemUI() })
                dialog.setOnShowListener {
                    activityContext.hideSystemUI()
                }
                dialog.show()
            } else {
                activityContext.toast(this.endpoint!!)
                var dialog = StartStreamingDialog(
                    activityContext,
                    {
                        streamService.startStream(this.endpoint!!)
                        onStartCallback()
                        activityContext.hideSystemUI()
                    }, { activityContext.hideSystemUI() })
                dialog.setOnShowListener {
                    activityContext.hideSystemUI()
                }
                dialog.show()
            }
        } else {
            activityContext.toast("no RTMP server configured")
        }
    }

    override fun stopStreamWithConfirm(onConfirm: () -> Unit) {
        if (streamService.isStreaming() == true) {
            var dialog = StopStreamingDialog(
                activityContext,
                { shouldEnd ->
                    streamService.stopStream()
                    onConfirm()
                }, { })
            dialog.setOnShowListener {
                activityContext.hideSystemUI()
            }
            dialog.show()
        } else {
            onConfirm()
        }
    }

    override fun toggleMicrophoneAudio() : Boolean {
        return streamService.toggleMicrophoneAudio() == true
    }

    override fun toggleAudioMonitor(): Boolean {
        return streamService.toggleAudioMonitor()
    }

    override fun setMonitorOutputDevice(device: AudioDeviceInfo?) {
        streamService.setMonitorOutputDevice(device)
    }

    override fun getAvailableOutputDevices(): List<AudioDeviceInfo> {
        return streamService.getAvailableOutputDevices()
    }

    override val audioMonitorEnabled
        get() = streamService.audioMonitorEnabled

    override fun getAvailableInputDevices(): List<AudioDeviceInfo> {
        return streamService.getAvailableInputDevices()
    }

    override fun setAudioInputDevice(device: AudioDeviceInfo?) {
        streamService.setAudioInputDevice(device)
    }

    override fun getSelectedAudioInputDevice(): AudioDeviceInfo? {
        return streamService.getSelectedAudioInputDevice()
    }

    override fun setConnectCheckerCallback(connectChecker: ConnectChecker?) {
        streamService.setConnectCheckerCallback(connectChecker)
    }

    override fun setFpsListenerCallback(fpsListenerCallback: FpsListener.Callback?) {
        streamService.setFpsListenerCallback(fpsListenerCallback)
    }

    override fun getVideoCaptureFormats(): List<VideoCaptureFormat> {
        return streamService.getVideoFormats()
    }

//    override fun getVideoSource(): VideoSource {
//        return streamService.getVideoSource()
//    }

    override val streamingElapsedTime: StateFlow<Int>
        get() = streamService.streamingElapsedTime

    override val videoSourceZoomHandler: StateFlow<IVideoSourceZoomHandler?>
        get() = streamService.videoSourceZoomHandler

    override val videoCaptureFormats: StateFlow<List<VideoCaptureFormat>>
        get() = streamService.videoCaptureFormats

    override val streamPerformance: StateFlow<StreamPerformanceData>
        get() = streamService.streamPerformance

    override fun isSafeModeActive(): Boolean = streamService.isSafeModeActive()
    override fun enableSafeMode() = streamService.enableSafeMode()
    override fun disableSafeMode() = streamService.disableSafeMode()

    override suspend fun prepareReplay(): Boolean {
        return streamService.prepareReplay()
    }

    override fun startReplay(seekMs: Long) {
        streamService.startReplay(seekMs)
    }

    override fun setReplaySpeed(speed: Float) {
        streamService.setReplaySpeed(speed)
    }

    override fun cancelReplay() {
        streamService.cancelReplay()
    }

    override fun stopReplay() {
        streamService.stopReplay()
    }

    override val replayState: StateFlow<ReplayState>
        get() = streamService.replayState

    override val replayMetadata: StateFlow<ReplayMetadata?>
        get() = streamService.replayMetadata
}