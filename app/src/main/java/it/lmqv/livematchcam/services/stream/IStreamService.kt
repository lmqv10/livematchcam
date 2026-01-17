package it.lmqv.livematchcam.services.stream

import android.util.Range
import android.view.SurfaceView
import com.pedro.common.ConnectChecker
import com.pedro.library.util.FpsListener
import it.lmqv.livematchcam.factories.sports.Sports
import it.lmqv.livematchcam.viewmodels.VideoSourceKind
import kotlinx.coroutines.flow.StateFlow

interface IVideoSourceZoomHandler {
    fun getZoomRange(): Range<Float>
    fun getZoom(): Float
    fun setZoom(level: Float)
}

interface IStreamService {
    fun getVideoSourceKind(): VideoSourceKind?

    fun initPreview(surfaceView: SurfaceView, sport: Sports)
    //fun preparePreview()
    fun stopPreview()

    fun setEndpoint(endpoint: String?)
    fun getEndpoint(): String?

    fun isStreaming(): Boolean
    fun isOnPreview(): Boolean


    fun toggleStreaming(onStartCallback: () -> Unit, onStopCallback: (Boolean) -> Unit)
    fun stopStreamWithConfirm(onConfirm: () -> Unit)

    fun toggleMicrophoneAudio(): Boolean

    fun setConnectCheckerCallback(connectChecker: ConnectChecker?)
    fun setFpsListenerCallback(fpsListenerCallback: FpsListener.Callback?)

    fun getVideoCaptureFormats(): List<VideoCaptureFormat>
    //fun getCameraSourceParameters(): List<CameraSourceParameters>
    //fun getVideoSource(): VideoSource

    val streamingElapsedTime: StateFlow<Int>
    val videoSourceZoomHandler: StateFlow<IVideoSourceZoomHandler?>
    val videoCaptureFormats: StateFlow<List<VideoCaptureFormat>>
}
