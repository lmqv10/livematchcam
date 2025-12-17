package it.lmqv.livematchcam.services.stream.filters

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES20
import com.pedro.encoder.input.gl.render.filters.`object`.ImageObjectFilterRender
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.extensions.animateAlpha
import it.lmqv.livematchcam.services.stream.IVideoStreamData
import it.lmqv.livematchcam.utils.BitmapRotator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class BitmapRotatorFilterRender(
    var context: Context,
    val sourceDescriptor: SourceDescriptor,
    val filterDescriptor: FilterDescriptor = FilterDescriptor(),
    val rotatorDescriptor: RotatorDescriptor = RotatorDescriptor(),
    var animationDescriptor: AnimationDescriptor = AnimationDescriptor()
) : BitmapObjectFilterRender(),
    BitmapRotator.BitmapRotationListener {

    private var sourceJob : Job
    private val sourceScope = CoroutineScope(Dispatchers.Default)

    private var bitmapRotator: BitmapRotator = BitmapRotator(context, rotatorDescriptor)
    private var isVisible: Boolean = false
    private var width: Int = 1
    private var height: Int = 1

    init {
        bitmapRotator.setBitmapRotationListener(this)

        sourceJob = sourceScope.launch {
            combine(
                sourceDescriptor.url,
                sourceDescriptor.isVisible
            ) { url, visible -> Pair(url, visible)
            }.collect { (url, visible) ->
                bitmapRotator.setUrls(listOf(url))
                isVisible = visible
                if (!isVisible) {
                    stop()
                } else {
                    start()
                }
            }
        }
    }

    override fun release() {
        super.release()
        Logd("BitmapRotatorFilterRender::release")
        sourceJob.cancel()
    }

    fun start() {
        Logd("BitmapRotatorFilterRender:: bitmapRotator.start()")
        bitmapRotator.start()
    }

    fun stop() {
        Logd("BitmapRotatorFilterRender:: bitmapRotator.stop()")
        bitmapRotator.stop()
    }

    override fun drawFilter() {
        super.drawFilter()
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, streamObjectTextureId[0])
        GLES20.glUniform1f(uAlphaHandle, if (streamObjectTextureId[0] == -1 || !isVisible) 0f else alpha)
    }

    override fun setVideoStreamData(videoStreamData: IVideoStreamData) {
        Logd("BitmapRotatorFilterRender :: setVideoStreamData ${videoStreamData.width}x${videoStreamData.height}")
        this.width = videoStreamData.width
        this.height = videoStreamData.height

        if (this.isVisible) {
            start()
        }
    }

    override fun onBitmapAvailable(bitmap: Bitmap) {
        val defaultScaleX = (bitmap.width.times(100) / width).toFloat()
        val defaultScaleY = (bitmap.height.times(100) / height).toFloat()

        val factorX = filterDescriptor.maxFactor / defaultScaleX

        val scaleX = factorX * defaultScaleX
        val scaleY = factorX * defaultScaleY

        val targetAlpha = animationDescriptor.targetAlpha
        val duration = animationDescriptor.duration
        (this as ImageObjectFilterRender).animateAlpha(targetAlpha, 0f, duration) {
            setImage(bitmap)
            setScale(scaleX, scaleY)
            //setPosition(100f - padding - scaleX, 0f + padding)
            setPosition(filterDescriptor.translateTo)
            animateAlpha(0f, targetAlpha, duration)
        }
    }
}