package it.lmqv.livematchcam.fragments

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.utils.TimerHandler
import it.lmqv.livematchcam.viewmodels.MatchViewModel

interface IScoreBoardFragment {
    interface OnUpdateCallback {
        fun refresh()
    }
    fun setOnUpdate(callback: OnUpdateCallback)

    fun getBitmapView(callback: (Bitmap) -> Unit)

    fun onTickTimer(timeElapsedInSeconds: Int)
    fun nextPeriod()
}

abstract class BaseScoreBoardFragment : Fragment(), IScoreBoardFragment {

    protected var onUpdateCallback: IScoreBoardFragment.OnUpdateCallback? = null
    override fun setOnUpdate(callback: IScoreBoardFragment.OnUpdateCallback) {
        this.onUpdateCallback = callback
    }

    protected val matchViewModel: MatchViewModel by activityViewModels()

    private val timerHandler: TimerHandler = TimerHandler.newInstance { timeElapsedInSeconds ->
        this.onTickTimer(timeElapsedInSeconds)
        onUpdateCallback?.refresh()
    }

    override fun getBitmapView(callback: (Bitmap) -> Unit) {
        val view = this.view
        view?.post {
            val width = view.width
            val height = view.height
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

    protected fun startTime() {
        timerHandler.startTime()
    }

    protected fun pauseTime() {
        timerHandler.pauseTime()
    }

    protected fun resetTime() {
        timerHandler.resetTime()
        onUpdateCallback?.refresh()
    }

    protected fun isStarted() : Boolean {
        return timerHandler.isStarted()
    }

    override fun onTickTimer(timeElapsedInSeconds: Int) { }

    override fun nextPeriod() {
        onUpdateCallback?.refresh()
    }
}