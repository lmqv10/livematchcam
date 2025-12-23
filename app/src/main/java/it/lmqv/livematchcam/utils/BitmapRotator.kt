//package it.lmqv.livematchcam.utils
//
//import android.content.Context
//import android.graphics.Bitmap
//import it.lmqv.livematchcam.extensions.Logd
//import it.lmqv.livematchcam.extensions.Loge
//import java.io.IOException
//import it.lmqv.livematchcam.extensions.loadBitmapOffscreen
//import it.lmqv.livematchcam.services.stream.filters.RotatorDescriptor
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.Job
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.launch
//
//class BitmapRotator(
//    private val context: Context,
//    private val rotatorDescriptor: RotatorDescriptor = RotatorDescriptor()
//) {
//    interface BitmapRotationListener {
//        fun onBitmapAvailable(bitmap: Bitmap)
//    }
//
//    private var targetWidth: Int = convertDpToPx(rotatorDescriptor.targetWidthDp)
//
//    private val bitmapCache = mutableMapOf<String, Bitmap>()
//    private var urls: List<String> = emptyList()
//    private var currentIndex = 0
//    private var listener: BitmapRotationListener? = null
//    private var job: Job? = null
//    private val scope = CoroutineScope(Dispatchers.Main)
//
//    fun setBitmapRotationListener(listener: BitmapRotationListener) {
//        this.listener = listener
//    }
//
//    @Synchronized
//    fun setUrls(urlList: List<String>) {
//        //Logd("BitmapRotator:: setUrls...")
//        if (this.urls != urlList)
//        {
//            this.urls = urlList
//            bitmapCache.clear()
//            currentIndex = 0
//            preloadBitmaps()
//        }
//    }
//
//    @Synchronized
//    fun start() {
//        Logd("BitmapRotator:: START")
//        if (urls.isEmpty()) throw IllegalStateException("no urls defined")
//
//        if (job?.isActive == true) return
//
////        job = scope.launch {
////            while (job?.isActive == true) {
////                try {
////                    Logd("BitmapRotator:: running...")
////                    val bitmaps = synchronized(bitmapCache) {
////                        bitmapCache.values.toList()
////                    }
////
////                    if (this@BitmapRotator.urls.size == bitmaps.size) {
////                        Logd("BitmapRotator:: show $currentIndex")
////                        listener?.onBitmapAvailable(
////                            bitmaps[currentIndex].copy(Bitmap.Config.ARGB_8888, true)
////                        )
////                        currentIndex = (currentIndex + 1) % bitmaps.size
////
////                        if (this@BitmapRotator.urls.size == 1) {
////                            Logd("BitmapRotator:: only 1 and loaded .. cancel")
////                            job?.cancel()
////                        } else {
////                            Logd("BitmapRotator:: wait for next ${rotatorDescriptor.intervalMillis}")
////                            delay(rotatorDescriptor.intervalMillis)
////                        }
////                    } else {
////                        Logd("BitmapRotator:: check preloading cache...")
////                        delay(250)
////                    }
////                } catch (e: Exception) {
////                    e.printStackTrace()
////                    Loge("BitmapRotator:: Exception ${e.message.toString()}")
////                }
////            }
////        }
//    }
//
//    @Synchronized
//    fun stop() {
//        Logd("BitmapRotator:: STOP")
//        if (job?.isActive == true) job?.cancel()
//    }
//
//    private fun preloadBitmaps() {
//        //Logd("BitmapRotator:: preloadBitmaps")
//        for (url in urls) {
//            CoroutineScope(Dispatchers.IO).launch {
//                try {
//                    if (url.isNotEmpty()) {
//                        val bitmap = loadBitmapOffscreen(context, url, targetWidth)
//                        if (bitmap != null) {
//                            listener?.onBitmapAvailable(
//                                bitmap
//                            )
////                        synchronized(bitmapCache) {
////                            bitmapCache[url] = bitmap
////                        }
//                        }
//                    } else {
////                    synchronized(bitmapCache) {
////                        bitmapCache.remove(url)
////                    }
//                    }
//                } catch (e: Exception) {
//                    e.printStackTrace()
////                    synchronized(bitmapCache) {
////                        bitmapCache.remove(url)
////                    }
//                }
//            }
//        }
//    }
//
//    private fun convertDpToPx(dp: Int): Int {
//        val density = context.resources.displayMetrics.density
//        return (dp * density).toInt()
//    }
//}
