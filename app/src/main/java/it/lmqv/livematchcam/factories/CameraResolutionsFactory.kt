package it.lmqv.livematchcam.factories

import com.pedro.encoder.input.sources.video.Camera2Source
import com.pedro.encoder.input.sources.video.VideoSource
import it.lmqv.livematchcam.extensions.toCameraSourceParameters
import it.lmqv.livematchcam.extensions.toCameraSourceParametersWithFps
import it.lmqv.livematchcam.services.stream.VideoCaptureFormat
import it.lmqv.livematchcam.services.stream.sources.UvcSonyCameraSource
import kotlin.collections.filter
import kotlin.math.abs

object CameraResolutionsFactory {
    fun get(videoSource: VideoSource) : List<VideoCaptureFormat> {
//        val sourceResolutions = when (videoSource) {
//            is Camera2Source -> videoSource.getCameraResolutions(videoSource.getCameraFacing()).toCameraSourceParameters()
//            is UvcSonyCameraSource -> videoSource.getCameraResolutions().toCameraSourceParametersWithFps()
//            else -> listOf<VideoCaptureFormat>()
//        }
//
//        val targetRatio = 16f / 9f
//        val epsilon = 0.01f
//
//        var resolutions =  sourceResolutions
//            .filter { x -> abs(x.width.toFloat() / x.height.toFloat() - targetRatio) < epsilon }
//            .sortedByDescending { x -> x.width }

//        return resolutions
        return listOf(
            VideoCaptureFormat(1920, 1080),
            VideoCaptureFormat(1280, 720),
            VideoCaptureFormat(854, 480),
        )
    }
}