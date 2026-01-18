package it.lmqv.livematchcam.services.stream.filters

import android.content.Context
import android.graphics.Canvas
import androidx.core.graphics.createBitmap
import androidx.viewbinding.ViewBinding
import com.pedro.encoder.utils.gl.ImageStreamObject
import com.pedro.encoder.utils.gl.TranslateTo
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.extensions.Loge
import it.lmqv.livematchcam.extensions.wrapLayout
import it.lmqv.livematchcam.services.firebase.IScore
import it.lmqv.livematchcam.services.firebase.Match
import it.lmqv.livematchcam.services.stream.IVideoStreamData

interface IScoreboardViewFilterRender {
    fun match(match: Match)
    fun score(score: IScore)
}

abstract class ScoreboardViewFilterRender<T>(
    val applicationContext: Context,
    val filterDescriptor: FilterDescriptor = FilterDescriptor(),
) : OverlayObjectFilterRender(), IScoreboardViewFilterRender where T : ViewBinding {

    internal var _binding: T? = null
    internal val binding get() = _binding!!

    private var width: Int = 0
    private var height: Int = 0

    init {
        streamObject = ImageStreamObject()
    }

    override fun release() {
        super.release()
        _binding = null
    }

    override fun setVideoStreamData(videoStreamData: IVideoStreamData) {
        if (_binding != null) {
            Logd("ScoreboardViewFilterRender::setVideoStreamData $videoStreamData")
            this.width = videoStreamData.width
            this.height = videoStreamData.height
            this.scaleSprite()
            this.translateSprite()
            this.render()
        }
    }

    @Synchronized
    protected fun render() {
        try {
            //Logd("ScoreboardViewRenderer::render ${width}x$height")
            if (_binding != null && width > 0 && height > 0) {
                val bitmap = createBitmap(width, height)
                val canvas = Canvas(bitmap)
                binding.root.draw(canvas)
                setImage(bitmap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Loge("ScoreboardViewRenderer::render Exception:: ${e.message.toString()}")
        }
    }

    private fun scaleSprite() {
        binding.root.wrapLayout()
        var scale = this.width * filterDescriptor.maxFactor / binding.root.measuredWidth
        setScale(scale, scale)
    }

    private fun translateSprite() {
        var maxFactor = filterDescriptor.maxFactor
        var offset= if (streamObject.width > streamObject.height) { streamObject.height / streamObject.width * 100f } else { 0f }

        when (filterDescriptor.translateTo) {
            TranslateTo.CENTER -> setPosition(50f - maxFactor / 2f, 50f - maxFactor / 2f)
            TranslateTo.TOP -> setPosition(50f - maxFactor / 2f, 0f)
            TranslateTo.LEFT -> setPosition(0f, 50f - maxFactor / 2f)
            TranslateTo.TOP_LEFT -> setPosition(0f, 0f)
            TranslateTo.RIGHT -> setPosition(100f - maxFactor,50f - maxFactor / 2f)
            TranslateTo.TOP_RIGHT -> setPosition(100f - maxFactor, 0f)
            TranslateTo.BOTTOM -> setPosition(50f - maxFactor / 2f, 100f - maxFactor + offset)
            TranslateTo.BOTTOM_LEFT -> setPosition(0f, 100f - maxFactor)
            TranslateTo.BOTTOM_RIGHT -> setPosition(100f - maxFactor, 100f - maxFactor)
        }
    }

    abstract override fun match(match: Match)
    abstract override fun score(score: IScore)
}
