package it.lmqv.livematchcam.services.stream.filters

import android.content.Context
import android.graphics.Canvas
import android.view.View
import androidx.core.graphics.createBitmap
import androidx.viewbinding.ViewBinding
import com.pedro.encoder.utils.gl.ImageStreamObject
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.extensions.Loge
import it.lmqv.livematchcam.services.firebase.IScore
import it.lmqv.livematchcam.services.firebase.Match
import it.lmqv.livematchcam.services.stream.IVideoStreamData

interface IScoreboardViewFilterRender {
    fun match(match: Match)
    fun score(score: IScore)
}

abstract class ScoreboardViewFilterRender<T>(
    val applicationContext: Context,
    val filterDescriptor: FilterDescriptor = FilterDescriptor()
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
        Logd("ScoreboardViewFilterRender::release")
        _binding = null
    }

    override fun setVideoStreamData(videoStreamData: IVideoStreamData) {
        if (_binding != null) {
            Logd("ScoreboardViewFilterRender::setVideoStreamData $videoStreamData")
            this.width = videoStreamData.width
            this.height = videoStreamData.height

            binding.root.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            binding.root.layout(0, 0, binding.root.measuredWidth, binding.root.measuredHeight)
            var scale = this.width * filterDescriptor.maxFactor / binding.root.measuredWidth

            setScale(scale, scale)
            setPosition(filterDescriptor.translateTo)
            render()
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

    abstract override fun match(match: Match)
    abstract override fun score(score: IScore)
}
