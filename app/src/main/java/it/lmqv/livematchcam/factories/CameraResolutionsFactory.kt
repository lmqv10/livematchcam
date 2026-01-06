package it.lmqv.livematchcam.factories

import android.util.Size
import com.pedro.encoder.input.sources.video.Camera2Source
import com.pedro.encoder.input.sources.video.VideoSource
import it.lmqv.livematchcam.services.stream.sources.UvcSonyCameraSource
import kotlin.collections.filter
import kotlin.math.abs

object CameraResolutionsFactory {
    @Suppress("UNCHECKED_CAST")
    fun get(videoSource: VideoSource) : List<Size> {
        val sourceResolutions = when (videoSource) {
            is Camera2Source -> videoSource.getCameraResolutions(videoSource.getCameraFacing())
            is UvcSonyCameraSource -> videoSource.getCameraResolutions()
            else -> listOf<Size>()
        } as List<Size>

        val targetRatio = 16f / 9f
        val epsilon = 0.01f

        var resolutions =  sourceResolutions
            .filter { x -> abs(x.width.toFloat() / x.height.toFloat() - targetRatio) < epsilon }
            .sortedByDescending { x -> x.width }

        return resolutions
    }
}