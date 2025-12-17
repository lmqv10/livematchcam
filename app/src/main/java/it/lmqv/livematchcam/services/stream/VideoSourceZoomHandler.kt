package it.lmqv.livematchcam.services.stream

import android.util.Range
import com.pedro.encoder.input.sources.video.Camera1Source
import com.pedro.encoder.input.sources.video.Camera2Source
import com.pedro.encoder.input.sources.video.VideoSource

class VideoSourceZoomHandler(val videoSource: VideoSource): IVideoSourceZoomHandler {

    override fun getZoomRange(): Range<Float> {
        return when (videoSource) {
            is Camera1Source -> {
                @Suppress("UNCHECKED_CAST")
                videoSource.getZoomRange() as Range<Float>
            }
            is Camera2Source -> {
                videoSource.getZoomRange()
            }
            else -> Range(1f, 1f)
        }
    }

    override fun getZoom(): Float {
        return when (videoSource) {
            is Camera1Source -> {
                videoSource.getZoom().toFloat()
            }
            is Camera2Source -> {
                videoSource.getZoom()
            }
            else -> 1f
        }
    }

    override fun setZoom(level: Float) {
        when (videoSource) {
            is Camera1Source -> { videoSource.setZoom(level.toInt()) }
            is Camera2Source -> { videoSource.setZoom(level) }
        }
    }
}