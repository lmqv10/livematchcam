package it.lmqv.livematchcam.services

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CounterService : Service() {
    private val binder = CounterBinder()
    private var job: Job? = null
    private var secondsCount = 0
    private var isPaused = false

    private val _counterState = MutableStateFlow<CounterState>(CounterState.Stopped)
    val counterState: StateFlow<CounterState> = _counterState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default  + SupervisorJob())

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        job?.cancel()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    inner class CounterBinder : Binder() {
        fun getService(): CounterService = this@CounterService
    }

    fun startCounter() {
        if (!isRunning()) {
            if (isPaused) {
                isPaused = false
                _counterState.value = CounterState.Running(secondsCount)
            } else {
                secondsCount = 0
                _counterState.value = CounterState.Running(secondsCount)

                job = scope.launch {
                    while (isActive) {
                        if (!isPaused) {
                            secondsCount++
                            //Logd("Counter:: $secondsCount")
                            _counterState.value = CounterState.Running(secondsCount)
                            delay(1000)
                        } else {
                            delay(100) // Small delay when paused to reduce CPU usage
                        }
                    }
                }
            }
        }
    }

    fun pauseCounter() {
        if (isRunning()) {
            isPaused = true
            _counterState.value = CounterState.Paused(secondsCount)
        }
    }

    fun stopCounter() {
        job?.cancel()
        secondsCount = 0
        isPaused = false
        _counterState.value = CounterState.Stopped
    }

    fun isRunning() : Boolean {
        return job?.isActive == true && !isPaused
    }

    sealed class CounterState {
        data object Stopped : CounterState()
        data class Running(val seconds: Int) : CounterState()
        data class Paused(val seconds: Int) : CounterState()
    }
}