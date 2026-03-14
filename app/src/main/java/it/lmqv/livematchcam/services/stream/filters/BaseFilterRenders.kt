package it.lmqv.livematchcam.services.stream.filters

import android.graphics.Bitmap
import com.pedro.encoder.input.gl.render.filters.`object`.ImageObjectFilterRender
import it.lmqv.livematchcam.services.stream.IVideoStreamData

interface IOverlayObjectFilterRender {
    fun setVideoStreamData(videoStreamData: IVideoStreamData)
    fun getBitmap(): Bitmap?
    fun getOverflowRatio(): Float
    fun hide()
    fun show()
}

abstract class OverlayObjectFilterRender : ImageObjectFilterRender(), IOverlayObjectFilterRender {
    abstract override fun setVideoStreamData(videoStreamData: IVideoStreamData)

    override fun hide() {
        this.alpha = 0f
    }

    override fun show() {
        this.alpha = 1f
    }
}