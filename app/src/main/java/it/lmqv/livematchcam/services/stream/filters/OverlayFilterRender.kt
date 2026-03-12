package it.lmqv.livematchcam.services.stream.filters

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import it.lmqv.livematchcam.extensions.loadBitmapOffscreen
import it.lmqv.livematchcam.extensions.toast
import it.lmqv.livematchcam.factories.FilterPosition
import it.lmqv.livematchcam.repositories.MatchRepository
import it.lmqv.livematchcam.services.firebase.FilterOverlay
import it.lmqv.livematchcam.services.stream.IVideoStreamData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class OverlayFilterRender(
    var context: Context,
    val filterPosition: FilterPosition) : OverlayObjectFilterRender() {

    private var sourceJob : Job? = null
    private var filtersRepositoryJob : Job? = null

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var current: FilterOverlay? = null
    private var streamWidth: Int = 100
    private var streamHeight: Int = 100

    init {
        //Logd("OverlayFilterRender::$filterPosition::init")
        setImage(createBitmap(1, 1, Bitmap.Config.ARGB_8888))
    }

    private fun initializeCollectors() {
        this.filtersRepositoryJob?.cancel()
        this.filtersRepositoryJob = coroutineScope.launch {
            MatchRepository.filters.collect { filters ->
                //Logd("OverlayFilterRender::$filterPosition::filters ${filters}")
                var updated = filters
                    .firstOrNull { it.position == filterPosition }
                    ?.filter

                //Logd("OverlayFilterRender::$filterPosition::updated $updated")
                val lastUrl = current?.urls?.firstOrNull()

                if (updated != null && current != updated) {
                    current = updated
                    var url = updated.urls.firstOrNull()

//                    Logd("OverlayFilterRender::$filterPosition::url $url")
//                    Logd("OverlayFilterRender::$filterPosition::lastUrl $lastUrl")

                    if (url?.isNotEmpty() == true) {
                        try {
                            if (url != lastUrl) {
                                //Logd("OverlayFilterRender::$filterPosition::bitmap ${url}")
                                var bitmap = loadBitmapOffscreen(context, url)
                                //Logd("OverlayFilterRender::$filterPosition::bitmap.WxH ${bitmap?.width}x${bitmap?.height}")
                                setImage(bitmap)
                            }

                            scaleSprite(updated)
                            translateSprite(updated)
                            alpha = if (updated.visible) { 0.75f } else { 0f }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            CoroutineScope(Dispatchers.Main).launch {
                                context.toast("Exception: Can't load image $url")
                            }
                            alpha = 0f
                        }
                    } else {
                        alpha = 0f
                    }
                } else {
                    //Logd("OverlayFilterRender:: current != updated ?? ${current != updated}")
                    //Logd("OverlayFilterRender:: updated?.visible == true ?? ${updated?.visible == true}")
                    alpha = if (updated?.visible == true) { 0.75f } else { 0f }
                }

                //Logd("OverlayFilterRender::$filterPosition::alpha $alpha with $updated")
            }
        }
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

    override fun setVideoStreamData(videoStreamData: IVideoStreamData) {
        //Logd("OverlayFilterRender::$filterPosition::setVideoStreamData ${videoStreamData.width}w ${videoStreamData.height}h")
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

        //Logd("OverlayFilterRender::$filterPosition::translateSprite ${updated}")
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
}