package it.lmqv.livematchcam.services.stream.filters

import com.pedro.encoder.input.gl.render.filters.`object`.ImageObjectFilterRender
import it.lmqv.livematchcam.services.stream.IVideoStreamData

abstract class BitmapObjectFilterRender : ImageObjectFilterRender() {
    abstract fun setVideoStreamData(videoStreamData: IVideoStreamData)
}
