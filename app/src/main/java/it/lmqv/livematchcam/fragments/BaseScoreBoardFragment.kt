package it.lmqv.livematchcam.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.extensions.launchOnStarted
import it.lmqv.livematchcam.services.counter.CounterService
import it.lmqv.livematchcam.viewmodels.CounterViewModel

interface IScoreBoardFragment<T> where T : Fragment {
    interface OnUpdateCallback {
        fun refresh()
    }
    fun setOnUpdate(callback: OnUpdateCallback)

    fun onTickTimer(timeElapsedInSeconds: Int)
    fun nextPeriod()
}

abstract class BaseScoreBoardFragment : Fragment(), IScoreBoardFragment<BaseScoreBoardFragment> {

    protected var onUpdateCallback: IScoreBoardFragment.OnUpdateCallback? = null
    override fun setOnUpdate(callback: IScoreBoardFragment.OnUpdateCallback) {
        this.onUpdateCallback = callback
    }

    private val counterViewModel: CounterViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        launchOnStarted {
            counterViewModel.counterState.collect { state ->
                Logd("BaseScoreBoardFragment:: counterViewModel.counterState $state")
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