package it.lmqv.livematchcam.services.stream.filters

import android.graphics.Bitmap
import com.pedro.encoder.input.gl.render.filters.`object`.ImageObjectFilterRender
import it.lmqv.livematchcam.services.stream.IVideoStreamData

interface IOverlayObjectFilterRender {
    fun setVideoStreamData(videoStreamData: IVideoStreamData)
    fun getBitmap(): Bitmap?
    fun getOverflowRatio(): Float
}

abstract class OverlayObjectFilterRender : ImageObjectFilterRender(), IOverlayObjectFilterRender {
    abstract override fun setVideoStreamData(videoStreamData: IVideoStreamData)
}