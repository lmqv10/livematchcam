package it.lmqv.livematchcam.factories

import android.util.Size
import com.pedro.encoder.input.sources.video.Camera2Source
import com.pedro.encoder.input.sources.video.VideoSource
import it.lmqv.livematchcam.services.stream.sources.UvcSonyCameraSource

object CameraResolutionsFactory {
    @Suppress("UNCHECKED_CAST")
    fun get(videoSource: VideoSource) : List<Size> {
        return when (videoSource) {
            is Camera2Source -> videoSource.getCameraResolutions(videoSource.getCameraFacing())
            is UvcSonyCameraSource -> videoSource.getCameraResolutions()
            else -> listOf<Size>()
        } as List<Size>
    }
}