package it.lmqv.livematchcam.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TimerHandler(private val onTickCallback: (Int) -> Unit) {
    private var timeElapsedInSeconds = 0
    private var job: Job? = null
    private var isInPause = true

    companion object {
        fun newInstance(onTickCallback: (Int) -> Unit) = TimerHandler(onTickCallback)
    }

    fun startTime() {
        this.isInPause = false
        if (job == null || job?.isActive == false) {
            job = CoroutineScope(Dispatchers.Main).launch {
                while (isActive) {
                    delay(1000)
                    if (!isInPause) {
                        timeElapsedInSeconds++
                        onTickCallback(timeElapsedInSeconds)
                    }
                }
            }
        }
    }

    fun pauseTime() {
        isInPause = true
    }

    fun resetTime() {
        job?.cancel()
        job = null
        timeElapsedInSeconds = 0
        isInPause = true
        onTickCallback(timeElapsedInSeconds)
    }

    fun isStarted() : Boolean {
        return !this.isInPause
    }
}