package it.lmqv.livematchcam.factories

import android.content.Context
import com.pedro.encoder.input.sources.video.Camera2Source
import com.pedro.encoder.input.sources.video.VideoSource
import it.lmqv.livematchcam.services.stream.sources.UvcSonyCameraSource
import it.lmqv.livematchcam.viewmodels.VideoSourceKind

object VideoSourceFactory {
    fun get(videoSourceKind: VideoSourceKind, context: Context) : VideoSource {
        return when (videoSourceKind) {
            VideoSourceKind.CAMERA2 -> Camera2Source(context)
            VideoSourceKind.UVC_SONY -> UvcSonyCameraSource()
            //VideoSourceKind.UVC_CAMERA -> CameraUvcSource()
        }
    }
}