package it.lmqv.livematchcam.fragments

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.viewmodels.AwayScoreBoardViewModel
import it.lmqv.livematchcam.viewmodels.HomeScoreBoardViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

interface IScoreBoardFragment {
    interface OnUpdateCallback {
        fun refresh()
    }
    fun setOnUpdate(callback: OnUpdateCallback)
    fun getBitmapView(callback: (Bitmap) -> Unit)
    fun startTime()
    fun pauseTime()
    fun resetTime()
    fun isInPause() : Boolean
    fun togglePeriod()
}

abstract class BaseScoreBoardFragment : Fragment(), IScoreBoardFragment {

    protected var onUpdateCallback: IScoreBoardFragment.OnUpdateCallback? = null
    override fun setOnUpdate(callback: IScoreBoardFragment.OnUpdateCallback) {
        this.onUpdateCallback = callback
    }

    protected val homeTeamViewModel: HomeScoreBoardViewModel by activityViewModels()
    protected val awayTeamViewModel: AwayScoreBoardViewModel by activityViewModels()

    private var timeElapsedInSeconds = 0
    private var job: Job? = null
    private var isInPause = false

    override fun getBitmapView(callback: (Bitmap) -> Unit) {
        val view = this.view
        view?.post {
            val width = view.width
            val height = view.height
            var aspectRatio = width.toDouble() / height.toDouble()
            /*

            val screenWidth = ScreenUtils.getScreenWidth(requireContext())
            val screenHeight = ScreenUtils.getScreenHeight(requireContext())
            var factor = screenWidth.toDouble() / screenHeight.toDouble()

            //var factorHeight = 720
            //var factorWidth =  factorHeight * factor

            var scaleX = 15f //(scaleY * factor).toFloat();
            var scaleY = 15f
            */
            var resolutionAspectRatio = 1280 / 720
            Logd("${width}x${height} - ${aspectRatio} vs ${resolutionAspectRatio}")
            val scoreBoardBitmap : Bitmap
            if (width > 0 && height > 0) {
                scoreBoardBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            } else {
                val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_launcher_background)!!
                scoreBoardBitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
            }
            val canvas = Canvas(scoreBoardBitmap)
            view.draw(canvas)

            callback(scoreBoardBitmap)
        }

    }

    override fun startTime() {
        this.isInPause = false
        if (job == null || job?.isActive == false) {
            job = CoroutineScope(Dispatchers.Main).launch {
                while (isActive) {
                    //binding.matchTime.text = formatTime(timeElapsedInSeconds)
                    delay(1000)
                    if (!isInPause) {
                        timeElapsedInSeconds++
                        onUpdateCallback?.refresh()
                    }
                }
            }
        }
    }

    override fun pauseTime() {
        isInPause = true
    }

    override fun resetTime() {
        job?.cancel()
        job = null
        timeElapsedInSeconds = 0
        isInPause = false
        onUpdateCallback?.refresh()
    }

    override fun isInPause() : Boolean {
        return this.isInPause
    }

    override fun togglePeriod() {
        onUpdateCallback?.refresh()
    }
}