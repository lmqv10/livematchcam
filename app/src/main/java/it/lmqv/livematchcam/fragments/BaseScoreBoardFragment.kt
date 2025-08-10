package it.lmqv.livematchcam.fragments

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.extensions.launchOnStarted
import it.lmqv.livematchcam.services.CounterService
import it.lmqv.livematchcam.viewmodels.CounterViewModel

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

    private val counterViewModel: CounterViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        launchOnStarted {
            counterViewModel.counterState.collect { state ->
                var seconds = 0
                when (state) {
                    is CounterService.CounterState.Running -> seconds = state.seconds
                    is CounterService.CounterState.Paused -> seconds = state.seconds
                    is CounterService.CounterState.Stopped -> seconds = 0
                }

                this.onTickTimer(seconds)
                onUpdateCallback?.refresh()
            }
        }
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
        counterViewModel.startCounter();
    }

    protected fun pauseTime() {
        counterViewModel.pauseCounter()
    }

    protected fun resetTime() {
        counterViewModel.stopCounter();
        onUpdateCallback?.refresh()
    }

    protected fun isStarted(): Boolean {
        return counterViewModel.isRunning()
    }

    override fun onTickTimer(timeElapsedInSeconds: Int) { }

    override fun nextPeriod() {
        onUpdateCallback?.refresh()
    }
}