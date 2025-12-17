package it.lmqv.livematchcam.services.stream

import android.app.Activity
import android.view.SurfaceView
import com.pedro.common.ConnectChecker
import com.pedro.library.util.FpsListener
import it.lmqv.livematchcam.dialogs.StartStreamingDialog
import it.lmqv.livematchcam.dialogs.StopStreamingDialog
import it.lmqv.livematchcam.extensions.hideSystemUI
import it.lmqv.livematchcam.extensions.toast
import it.lmqv.livematchcam.factories.Sports
import it.lmqv.livematchcam.viewmodels.VideoSourceKind
import kotlinx.coroutines.flow.StateFlow

class StreamServiceProxy(val activityContext: Activity, val streamService: StreamService) :
    IStreamService {
    private var endpoint: String? = null

    override fun getVideoSourceKind(): VideoSourceKind? {
        //Logd("StreamServiceProxy :: getVideoSourceKind")
        return streamService.getVideoSourceKind()
    }

    override fun preparePreview(surfaceView: SurfaceView, sport: Sports) {
        //Logd("StreamServiceProxy :: preparePreview")
        streamService.preparePreview(surfaceView, sport)
    }

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

    override fun setConnectCheckerCallback(connectChecker: ConnectChecker?) {
        streamService.setConnectCheckerCallback(connectChecker)
    }

    override fun setFpsListenerCallback(fpsListenerCallback: FpsListener.Callback?) {
        streamService.setFpsListenerCallback(fpsListenerCallback)
    }

    override val streamingElapsedTime: StateFlow<Int>
        get() = streamService.streamingElapsedTime

    override val videoSourceZoomHandler: StateFlow<IVideoSourceZoomHandler?>
        get() = streamService.videoSourceZoomHandler

}