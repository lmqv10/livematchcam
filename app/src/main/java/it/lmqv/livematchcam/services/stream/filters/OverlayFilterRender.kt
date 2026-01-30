package it.lmqv.livematchcam.services.stream.filters

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES20
import com.pedro.encoder.utils.gl.TranslateTo
import it.lmqv.livematchcam.extensions.animateAlpha
import it.lmqv.livematchcam.extensions.loadBitmapOffscreen
import it.lmqv.livematchcam.extensions.toast
import it.lmqv.livematchcam.services.stream.IVideoStreamData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class OverlayFilterRender(
    var context: Context,
    val sourceDescriptor: SourceDescriptor,
    val filterDescriptor: FilterDescriptor = FilterDescriptor(),
    val dimensionDescriptor: DimensionDescriptor = DimensionDescriptor(),
    var animationDescriptor: AnimationDescriptor = AnimationDescriptor()
) : OverlayObjectFilterRender() {

    private var sourceJob : Job
    private val sourceScope = CoroutineScope(Dispatchers.Default)

    private var targetWidth: Int = convertDpToPx(dimensionDescriptor.targetWidthDp)

    private var previousUrl:String? = null
    private var bitmap: Bitmap? = null
    private var isVisible: Boolean = false
    private var width: Int = 1
    private var height: Int = 1

    init {
        sourceJob = sourceScope.launch {
            combine(
                sourceDescriptor.url,
                sourceDescriptor.isVisible
            ) { url, visible -> Pair(url, visible) }
            .distinctUntilChanged()
            .collect { (url, visible) ->
                //Logd("OverlayFilterRender:: url $url visible $visible")
                try {
                    isVisible = visible && url.isNotEmpty()
                    if (isVisible && previousUrl != url) {
                        previousUrl = url
                        bitmap = loadBitmapOffscreen(context, url, targetWidth)
                        onBitmapAvailable()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    CoroutineScope(Dispatchers.Main).launch {
                        context.toast("Can't load image ${url}")
                    }
                }
            }
        }
    }

    override fun release() {
        super.release()
        //Logd("OverlayFilterRender::release")
        sourceJob.cancel()
    }

    override fun drawFilter() {
        super.drawFilter()
        if (!isVisible || this.bitmap == null) {
            GLES20.glUniform1f(uAlphaHandle, 0f)
        }
    }

    override fun setVideoStreamData(videoStreamData: IVideoStreamData) {
        this.width = videoStreamData.width
        this.height = videoStreamData.height
        //Logd("OverlayFilterRender::setVideoStreamData ${width}x$height")
        onBitmapAvailable()
    }

    private fun convertDpToPx(dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }

    @Synchronized
    fun onBitmapAvailable() {
        this.bitmap?.let {
            CoroutineScope(Dispatchers.Main).launch {
                val defaultScaleX = (it.width.times(100) / width).toFloat()
                val defaultScaleY = (it.height.times(100) / height).toFloat()

                val factorX = filterDescriptor.maxFactor / defaultScaleX

                val scaleX = factorX * defaultScaleX
                val scaleY = factorX * defaultScaleY

                animateAlpha(0f, animationDescriptor.targetAlpha, animationDescriptor.duration) {
                    setImage(it)
                    setScale(scaleX, scaleY)
                    translateSprite()
                }
            }
        }
    }

    private fun translateSprite() {
        var maxFactor = filterDescriptor.maxFactor
        var offset= if (streamObject.width > streamObject.height) { streamObject.height / streamObject.width * 100f } else { 0f }
        var margin = 2f

        when (filterDescriptor.translateTo) {
            TranslateTo.CENTER -> setPosition(50f - maxFactor / 2f, 50f - maxFactor / 2f)
            TranslateTo.TOP -> setPosition(50f - maxFactor / 2f, margin)
            TranslateTo.LEFT -> setPosition(margin, 50f - maxFactor / 2f)
            TranslateTo.TOP_LEFT -> setPosition(margin, margin)
            TranslateTo.RIGHT -> setPosition(100f - maxFactor - margin, 50f - maxFactor / 2f)
            TranslateTo.TOP_RIGHT -> setPosition(100f - maxFactor - margin, margin)
            TranslateTo.BOTTOM -> setPosition(50f - maxFactor / 2f, 100f - maxFactor + offset - margin)
            TranslateTo.BOTTOM_LEFT -> setPosition(margin, 100f - maxFactor - margin)
            TranslateTo.BOTTOM_RIGHT -> setPosition(100f - maxFactor - margin, 100f - maxFactor - margin)
        }
    }
}