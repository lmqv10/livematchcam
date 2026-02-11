package it.lmqv.livematchcam.services.stream.filters

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.opengl.GLES20
import androidx.preference.PreferenceManager
import com.pedro.encoder.utils.gl.TranslateTo
import it.lmqv.livematchcam.extensions.Logd
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
    private var width: Int = 100
    private var height: Int = 100
    private var maxFactor: Float = 18f

    private var sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val preferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            key?.let {
                handlePreferenceKey(it)
            }
        }

    init {
        this.sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        this.handlePreferenceKey(filterDescriptor.preferencesSizeKey)

        sourceJob = sourceScope.launch {
            combine(
                sourceDescriptor.url,
                sourceDescriptor.isVisible
            ) { currentUrl, visible -> Pair(currentUrl, visible) }
            .distinctUntilChanged()
            .collect { (currentUrl, visible) ->
                try {
                    Logd("OverlayFilterRender:: url $currentUrl visible $visible")
                    isVisible = visible && currentUrl.isNotEmpty()
                    if (isVisible && previousUrl != currentUrl) {
                        previousUrl = currentUrl
                        bitmap = loadBitmapOffscreen(context, currentUrl, targetWidth)
                        this@OverlayFilterRender.scaleSprite()
                        this@OverlayFilterRender.setImage(bitmap)
                        this@OverlayFilterRender.translateSprite()
                    } else {
                        Logd("OverlayFilterRender:: load bitmap not required")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    CoroutineScope(Dispatchers.Main).launch {
                        context.toast("Can't load image $currentUrl")
                    }
                }
            }
        }
    }

    override fun release() {
        super.release()
        sourceJob.cancel()
        this.sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    override fun drawFilter() {
        super.drawFilter()
        var targetAlpha = if (!isVisible || this.bitmap == null) { 0f } else { animationDescriptor.targetAlpha }
        GLES20.glUniform1f(uAlphaHandle, targetAlpha)
    }

    override fun setVideoStreamData(videoStreamData: IVideoStreamData) {
        Logd("OverlayFilterRender::setVideoStreamData ${videoStreamData.width}x${videoStreamData.height}")
        this.width = videoStreamData.width
        this.height = videoStreamData.height
    }

    private fun convertDpToPx(dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }

    private fun scaleSprite() {
        this.bitmap?.let {
            Logd("OverlayFilterRender::scaleSprite ${it.width}x${it.height} overlay ${width}x${height}")

            val defaultScaleX = (it.width.times(100) / width).toFloat()
            val defaultScaleY = (it.height.times(100) / height).toFloat()

            val factorX = this@OverlayFilterRender.maxFactor / defaultScaleX

            val scaleX = factorX * defaultScaleX
            val scaleY = factorX * defaultScaleY

            setScale(scaleX, scaleY)
        }
    }

    private fun translateSprite() {
        var factor = this.maxFactor
        var offset= if (streamObject.width > streamObject.height) { streamObject.height / streamObject.width * 100f } else { 0f }
        var margin = 2f

        when (filterDescriptor.translateTo) {
            TranslateTo.CENTER -> setPosition(50f - factor / 2f, 50f - factor / 2f)
            TranslateTo.TOP -> setPosition(50f - factor / 2f, margin)
            TranslateTo.LEFT -> setPosition(margin, 50f - factor / 2f)
            TranslateTo.TOP_LEFT -> setPosition(margin, margin)
            TranslateTo.RIGHT -> setPosition(100f - factor - margin, 50f - factor / 2f)
            TranslateTo.TOP_RIGHT -> setPosition(100f - factor - margin, margin)
            TranslateTo.BOTTOM -> setPosition(50f - factor / 2f, 100f - factor + offset - margin)
            TranslateTo.BOTTOM_LEFT -> setPosition(margin, 100f - factor - margin)
            TranslateTo.BOTTOM_RIGHT -> setPosition(100f - factor - margin, 100f - factor - margin)
        }
    }

    private fun handlePreferenceKey(key: String) {
        if (key == filterDescriptor.preferencesSizeKey) {
            this.maxFactor = sharedPreferences.getString(filterDescriptor.preferencesSizeKey, filterDescriptor.defaultSize.toString())?.toFloatOrNull() ?: filterDescriptor.defaultSize.toFloat()
            try {
                scaleSprite()
                translateSprite()
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    context.toast("OverlayFilterRender::handlePreferenceKey ${e.message.toString()}")
                }
            }
        }
    }
}