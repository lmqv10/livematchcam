package it.lmqv.livematchcam.services.stream

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.view.SurfaceView
import androidx.core.content.ContextCompat
import com.pedro.encoder.input.sources.video.VideoSource
import it.lmqv.livematchcam.dialogs.StartStreamingDialog
import it.lmqv.livematchcam.dialogs.StopStreamingDialog
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.extensions.hideSystemUI
import it.lmqv.livematchcam.extensions.toast
import it.lmqv.livematchcam.services.stream.filters.BitmapObjectFilterRender

class StreamServiceConnector(val activityContext: Activity) : ServiceConnection {

    private var streamService: StreamService? = null
    private var bound = false
    private var endpoint: String? = null

    private var onServiceConnected : (StreamService) -> Unit = {}
    fun setOnServiceConnected(callback: (StreamService) -> Unit) {
        this.onServiceConnected = callback
    }

    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
        Logd("StreamServiceConnector :: onServiceConnected")
        val localBinder = binder as StreamService.LocalBinder
        streamService = localBinder.service
        this.onServiceConnected(streamService!!)
        bound = true
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        Logd("StreamServiceConnector :: onServiceDisconnected")
        streamService?.stopPreview()
        bound = false
        streamService = null
    }

    fun changeVideoSource(videoSource: VideoSource) {
        Logd("StreamServiceConnector :: changeVideoSource")
        streamService?.changeVideoSource(videoSource)
    }

    fun preparePreview(surfaceView: SurfaceView, filters: List<BitmapObjectFilterRender> = listOf()) {
        Logd("StreamServiceConnector :: preparePreview")
        streamService?.preparePreview(surfaceView, filters)
    }

    fun setEndpoint(endpoint: String?) {
        this.endpoint = endpoint
    }

    fun getEndpoint() : String? {
        return this.endpoint
    }

    fun bindService() {
        Logd("StreamServiceConnector :: bindService")
        val intent = Intent(activityContext, StreamService::class.java)
        ContextCompat.startForegroundService(activityContext, intent)
        activityContext.bindService(intent, this, Context.BIND_AUTO_CREATE)
    }

    fun unbindService() {
        Logd("StreamServiceConnector :: stopPreview ??")
        streamService?.stopPreview()
        if (bound) {
            Logd("StreamServiceConnector :: unbindService")
            activityContext.unbindService(this)
            bound = false
        }
    }

    fun stopService() {
        Logd("StreamServiceConnector :: stopService")
        val intent = Intent(activityContext, StreamService::class.java)
        activityContext.stopService(intent)
    }

    fun isStreaming() : Boolean {
        return streamService?.isStreaming() == true
    }

    fun isOnPreview() : Boolean {
        return streamService?.isOnPreview() == true
    }

    fun toggleStreaming(onStartCallback: () -> Unit, onStopCallback: (Boolean) -> Unit) {
        if (this.endpoint != null) {
            if (streamService?.isStreaming() == true) {
                var dialog = StopStreamingDialog(
                    activityContext,
                    { shouldEnd ->
                        streamService?.stopStream()
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
                        streamService?.startStream(this.endpoint!!)
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

    fun stopStreamWithConfirm(onConfirm: () -> Unit) {
        if (streamService?.isStreaming() == true) {
            var dialog = StopStreamingDialog(
                activityContext,
                { shouldEnd ->
                    streamService?.stopStream()
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

    fun toggleMicrophoneAudio() : Boolean {
        return streamService?.toggleMicrophoneAudio() == true
    }
}