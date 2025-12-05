package it.lmqv.livematchcam.services.stream.filters

import android.graphics.Bitmap
import android.graphics.Canvas
import android.opengl.GLES20
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.fragment.app.Fragment
import com.pedro.encoder.utils.gl.ImageStreamObject
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.fragments.BaseScoreBoardFragment
import it.lmqv.livematchcam.fragments.IScoreBoardFragment
import it.lmqv.livematchcam.services.stream.IVideoStreamData

class ScoreBoardFilterRender(
    var scoreBoardFragment: IScoreBoardFragment<BaseScoreBoardFragment>,
    val filterDescriptor: FilterDescriptor = FilterDescriptor()
) : BitmapObjectFilterRender(),
    IScoreBoardFragment.OnUpdateCallback {

    //private var previewVideoStreamData : IVideoStreamData? = null

    init {
        streamObject = ImageStreamObject()
        scoreBoardFragment.setOnUpdate(this)
    }

    override fun drawFilter() {
        super.drawFilter()
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, streamObjectTextureId[0])
        GLES20.glUniform1f(uAlphaHandle, if (streamObjectTextureId[0] == -1) 0f else alpha)
    }

    override fun setVideoStreamData(videoStreamData: IVideoStreamData) {
//        if (previewVideoStreamData == null
//            || previewVideoStreamData?.width != videoStreamData.width
//            || previewVideoStreamData?.height != videoStreamData.height) {
//            previewVideoStreamData = videoStreamData

            Logd("ScoreBoardFilterRender :: setVideoStreamData ${videoStreamData.width}x${videoStreamData.height}")
            loadBitmapFromView { bitmap ->
                val defaultScaleX = (bitmap.width * 100 / videoStreamData.width).toFloat()
                val defaultScaleY = (bitmap.height * 100 / videoStreamData.height).toFloat()

                val factorX = filterDescriptor.maxFactor / defaultScaleX
                val scaleX = factorX * defaultScaleX
                val scaleY = factorX * defaultScaleY
                setImage(bitmap)
                setScale(scaleX, scaleY)
                //val position = filterDescriptor.position
                //setPosition(position.x, position.y)
                setPosition(filterDescriptor.translateTo)
            }
//        } else {
//            Logd("ScoreBoardFilterRender :: setVideoStreamData no changes")
//        }
    }

    override fun refresh() {
        loadBitmapFromView { bitmap ->
            setImage(bitmap)
        }
    }

    private fun loadBitmapFromView(callback: (Bitmap) -> Unit) {
        val view = (this.scoreBoardFragment as Fragment).view
        view?.post {
            val width = view.width
            val height = view.height
            val scoreBoardBitmap : Bitmap
            if (width > 0 && height > 0) {
                scoreBoardBitmap = createBitmap(view.width, view.height)
            } else {
                val drawable = ContextCompat.getDrawable(view.context, R.drawable.preview_missing)!!
                scoreBoardBitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
            }
            val canvas = Canvas(scoreBoardBitmap)
            view.draw(canvas)

            callback(scoreBoardBitmap)
        }
    }
}
