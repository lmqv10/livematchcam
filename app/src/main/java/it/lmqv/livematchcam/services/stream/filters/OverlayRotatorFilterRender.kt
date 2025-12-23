//package it.lmqv.livematchcam.services.stream.filters
//
//import android.content.Context
//import android.graphics.Bitmap
//import android.opengl.GLES20
//import it.lmqv.livematchcam.extensions.Logd
//import it.lmqv.livematchcam.extensions.animateAlpha
//import it.lmqv.livematchcam.services.stream.IVideoStreamData
//import it.lmqv.livematchcam.utils.BitmapRotator
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.Job
//import kotlinx.coroutines.flow.combine
//import kotlinx.coroutines.flow.distinctUntilChanged
//import kotlinx.coroutines.launch
//import kotlin.math.min
//
//class OverlayRotatorFilterRender(
//    var context: Context,
//    val sourceDescriptor: SourceDescriptor,
//    val filterDescriptor: FilterDescriptor = FilterDescriptor(),
//    val rotatorDescriptor: RotatorDescriptor = RotatorDescriptor(),
//    var animationDescriptor: AnimationDescriptor = AnimationDescriptor(),
//) : OverlayObjectFilterRender(),
//    BitmapRotator.BitmapRotationListener {
//
//    private var sourceJob : Job
//    private val sourceScope = CoroutineScope(Dispatchers.Default)
//
//    private var bitmapRotator: BitmapRotator = BitmapRotator(context, rotatorDescriptor)
//    private var isVisible: Boolean = false
//    private var width: Int = 1
//    private var height: Int = 1
//
//    init {
//        bitmapRotator.setBitmapRotationListener(this)
//
//        sourceJob = sourceScope.launch {
//            combine(
//                sourceDescriptor.url,
//                sourceDescriptor.isVisible
//            ) { url, visible -> Pair(url, visible) }
//            .distinctUntilChanged()
//            .collect { (url, visible) ->
//                Logd("OverlayRotatorFilterRender:: url $url visible $visible")
//
////                var urls = listOf<String>(
////                    "https://www.nowpadova.com/images/2020/01/23/AVIST_large.jpg",
////                    "https://avisbiella.it/wp-content/uploads/2022/01/Logo_AVIS.png"
////                )
////                bitmapRotator.setUrls(urls)
//
//                bitmapRotator.setUrls(listOf(url))
//                isVisible = visible && url.isNotEmpty()
//                if (!isVisible) {
//                    stop()
//                } else {
//                    start()
//                }
//            }
//        }
//    }
//
//    override fun release() {
//        super.release()
//        Logd("OverlayRotatorFilterRender::release")
//        sourceJob.cancel()
//        stop()
//    }
//
//    fun start() {
//        //Logd("OverlayRotatorFilterRender:: bitmapRotator.start()")
//        bitmapRotator.start()
//    }
//
//    fun stop() {
//        //Logd("OverlayRotatorFilterRender:: bitmapRotator.stop()")
//        bitmapRotator.stop()
//        //Logd("OverlayRotatorFilterRender:: hide")
//        //this.animateAlpha(alpha, 0f, animationDescriptor.duration)
//    }
//
//    override fun drawFilter() {
//        super.drawFilter()
//        if (!isVisible) {
//            GLES20.glUniform1f(uAlphaHandle, 0f)
//        }
//    }
//
//    override fun setVideoStreamData(videoStreamData: IVideoStreamData) {
//        //Logd("OverlayRotatorFilterRender :: setVideoStreamData ${videoStreamData.width}x${videoStreamData.height}")
//        this.width = videoStreamData.width
//        this.height = videoStreamData.height
//    }
//
//    override fun onBitmapAvailable(bitmap: Bitmap) {
//        CoroutineScope(Dispatchers.Main).launch {
//            if (this@OverlayRotatorFilterRender.isVisible) {
//                //Logd("OverlayRotatorFilterRender:: onBitmapAvailable ${bitmap.width}x${bitmap.height}")
//                //Logd("OverlayRotatorFilterRender:: previewSize ${width}x${height}")
//
//                val defaultScaleX = (bitmap.width.times(100) / width).toFloat()
//                val defaultScaleY = (bitmap.height.times(100) / height).toFloat()
//
//                val factorX = filterDescriptor.maxFactor / defaultScaleX
//
//                val scaleX = factorX * defaultScaleX
//                val scaleY = factorX * defaultScaleY
//
//                val startAlpha = min(alpha, animationDescriptor.targetAlpha)
//                val duration = animationDescriptor.duration
//
//                //Logd("OverlayRotatorFilterRender:: setImage ${bitmap}")
//
//                this@OverlayRotatorFilterRender.animateAlpha(startAlpha, 0f, duration) {
//                    setImage(bitmap)
//                    setScale(scaleX, scaleY)
//                    //setPosition(100f - padding - scaleX, 0f + padding)
//                    setPosition(filterDescriptor.translateTo)
//                    animateAlpha(0f, animationDescriptor.targetAlpha, duration)
//                }
////        } else {
////            Logd("OverlayRotatorFilterRender:: image transparent")
////            val transparentBitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)
////            transparentBitmap.eraseColor(Color.TRANSPARENT)
////            setImage(transparentBitmap)
//            }
//        }
//    }
//}