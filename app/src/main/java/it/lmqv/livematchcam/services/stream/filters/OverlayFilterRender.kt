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
import it.lmqv.livematchcam.factories.FilterPosition
import it.lmqv.livematchcam.repositories.MatchRepository
import it.lmqv.livematchcam.services.firebase.FilterOverlay
import it.lmqv.livematchcam.services.firebase.FilterOverlayEvent
import it.lmqv.livematchcam.services.stream.IVideoStreamData
import it.lmqv.livematchcam.services.stream.filters.ScoreboardViewFilterRender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class OverlayFilterRender(
    var context: Context,
    val filterPosition: FilterPosition
//    val sourceDescriptor: SourceDescriptor,
//    val filterDescriptor: FilterDescriptor = FilterDescriptor(),
//    var animationDescriptor: AnimationDescriptor = AnimationDescriptor()
) : OverlayObjectFilterRender() {

    private var sourceJob : Job? = null
    private var filtersRepositoryJob : Job? = null

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var current: FilterOverlay? = null
    //private var bitmap: Bitmap? = null
    private var isVisible: Boolean = false
    private var streamWidth: Int = 100
    private var streamHeight: Int = 100

//    private var sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
//    private val preferenceChangeListener =
//        SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
//            key?.let {
//                handlePreferenceKey(it)
//            }
//        }

//    init {
//        this.sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
//        this.handlePreferenceKey(filterDescriptor.preferencesSizeKey)
//    }

    private fun initializeCollectors() {
        this.filtersRepositoryJob?.cancel()
        this.filtersRepositoryJob = coroutineScope.launch {
            MatchRepository.filters.collect { filters ->
                var updated = filters
                    .firstOrNull { it.position == this@OverlayFilterRender.filterPosition }
                    ?.filter

                if (updated != null && current != updated) {
                    var url = updated.urls.firstOrNull()
                    val lastUrl = current?.urls?.firstOrNull()

                    if (url?.isNotEmpty() == true) {
                        try {
                            var bitmap : Bitmap? = null
                            if (url != lastUrl) {
                                bitmap = loadBitmapOffscreen(context, url)
                                if (bitmap != null) {
                                    this@OverlayFilterRender.setImage(bitmap)
                                }
                            }

                            this@OverlayFilterRender.scaleSprite(updated)
                            this@OverlayFilterRender.translateSprite(updated)
                            isVisible = (bitmap != null || url == lastUrl) && updated.visible
                        } catch (e: Exception) {
                            e.printStackTrace()
                            CoroutineScope(Dispatchers.Main).launch {
                                context.toast("Can't load image $url")
                            }
                            isVisible = false
                        }
                    } else {
                        isVisible = false
                    }
                } else {
                    //Logd("OverlayFilterRender:: current != updated ?? ${current != updated}")
                    //Logd("OverlayFilterRender:: updated?.visible == true ?? ${updated?.visible == true}")
                    isVisible = updated?.visible == true
                }

                current = updated
            }
        }

//        this.sourceJob?.cancel()
//        this.sourceJob =  coroutineScope.launch {
//            combine(
//                sourceDescriptor.url,
//                sourceDescriptor.isVisible
//            ) { currentUrl, visible -> Pair(currentUrl, visible) }
//            .distinctUntilChanged()
//            .collect { (currentUrl, visible) ->
//                try {
//                    Logd("OverlayFilterRender:: url $currentUrl visible $visible")
//                    isVisible = visible && currentUrl.isNotEmpty()
//                    if (isVisible && previousUrl != currentUrl) {
//                        previousUrl = currentUrl
//                        //bitmap = loadBitmapOffscreen(context, currentUrl, targetWidth)
//                        bitmap = loadBitmapOffscreen(context, currentUrl)
//                        this@OverlayFilterRender.scaleSprite()
//                        this@OverlayFilterRender.setImage(bitmap)
//                        this@OverlayFilterRender.translateSprite()
//                    } else {
//                        Logd("OverlayFilterRender:: load bitmap not required")
//                    }
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                    CoroutineScope(Dispatchers.Main).launch {
//                        context.toast("Can't load image $currentUrl")
//                    }
//                }
//            }
//        }
    }

    override fun release() {
        super.release()
        //Logd("OverlayFilterRender:: release")
        this.sourceJob?.cancel()
        this.filtersRepositoryJob?.cancel()
        this.current = null
        //this.bitmap = null
//        this.sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    override fun drawFilter() {
        super.drawFilter()
        //var targetAlpha = if (!isVisible || this.bitmap == null) { 0f } else { 0.75f }
        var targetAlpha = if (!isVisible) { 0f } else { 0.75f }
        GLES20.glUniform1f(uAlphaHandle, targetAlpha)
    }

    override fun setVideoStreamData(videoStreamData: IVideoStreamData) {
        Logd("OverlayFilterRender::setVideoStreamData $filterPosition")
        //Logd("OverlayFilterRender::setVideoStreamData ${videoStreamData.width}w ${videoStreamData.height}h")
        this.streamWidth = videoStreamData.width
        this.streamHeight = videoStreamData.height

        this.initializeCollectors()
    }

    private fun scaleSprite(updated: FilterOverlay) {
        updated.let { filter ->
            if (streamObject != null && streamWidth > 0 && streamHeight > 0) {
                var scaleFactor = filter.size.toFloat()
                //Logd("OverlayFilterRender::scaleSprite ${streamObject.width}x${streamObject.height} overlay ${streamWidth}x${streamHeight}")

                val defaultScaleX = (streamObject.width.times(100).div(streamWidth)).toFloat()
                val defaultScaleY = (streamObject.height.times(100).div(streamHeight)).toFloat()

                val factorX = scaleFactor.div(defaultScaleX)

                val scaleX = factorX.times(defaultScaleX)
                val scaleY = factorX.times(defaultScaleY)

                setScale(scaleX, scaleY)
            }
        }
    }

    private fun translateSprite(updated: FilterOverlay) {
        var scaleX = scale.x
        var scaleY = scale.y
        var margin = 1f

        when (updated.position) {
            FilterPosition.CENTER -> setPosition(50f - scaleX / 2f, 50f - scaleY / 2f)
            FilterPosition.TOP -> setPosition(50f - scaleX / 2f, margin)
            //FilterPosition.LEFT -> setPosition(margin, 50f - scaleY / 2f)
            FilterPosition.TOP_LEFT -> setPosition(margin, margin)
            //FilterPosition.RIGHT -> setPosition(100f - scaleX - margin, 50f - scaleY / 2f)
            FilterPosition.TOP_RIGHT -> setPosition(100f - scaleX - margin, margin)
            FilterPosition.BOTTOM -> setPosition(50f - scaleX / 2f, 100f - scaleY - margin)
            FilterPosition.BOTTOM_LEFT -> setPosition(margin, 100f - scaleY - margin)
            FilterPosition.BOTTOM_RIGHT -> setPosition(100f - scaleX - margin, 100f - scaleY - margin)
        }
    }

//    private fun handlePreferenceKey(key: String) {
//        if (key == filterDescriptor.preferencesSizeKey) {
//            this.maxFactor = sharedPreferences.getString(filterDescriptor.preferencesSizeKey, filterDescriptor.defaultSize.toString())?.toFloatOrNull() ?: filterDescriptor.defaultSize.toFloat()
//            try {
//                scaleSprite()
//                translateSprite()
//            } catch (e: Exception) {
//                CoroutineScope(Dispatchers.Main).launch {
//                    context.toast("OverlayFilterRender::handlePreferenceKey ${e.message.toString()}")
//                }
//            }
//        }
//    }
}